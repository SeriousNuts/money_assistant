package com.example.myapplication;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.WriteBatch;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** One entry point for both the offline demo and the authenticated Firestore ledger. */
public final class BillRepository {
    public interface Result<T> { void done(T value, Exception error); }
    private static final long FRESH_MS = 5 * 60 * 1000L;
    private static boolean configured;
    private final Context context;
    private final FirebaseFirestore cloud;
    private final DemoLedger demo;

    public BillRepository(Context context) {
        this.context = context.getApplicationContext();
        demo = new DemoLedger(this.context);
        if (DemoSession.active(context)) {
            cloud = null; // Never touch Firebase for the demo account.
        } else {
            cloud = FirebaseFirestore.getInstance();
            if (!configured) {
                cloud.setFirestoreSettings(new FirebaseFirestoreSettings.Builder()
                        .setCacheSizeBytes(100L * 1024 * 1024).setPersistenceEnabled(true).build());
                configured = true;
            }
        }
    }

    private String uid() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) throw new IllegalStateException("Войдите в аккаунт");
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    private DocumentReference ref(String id) { return cloud.collection(uid()).document(id); }

    private long fresh(String key) {
        return context.getSharedPreferences("cache-freshness", 0).getLong(uid() + ":" + key, 0);
    }

    private void markFresh(String key) {
        context.getSharedPreferences("cache-freshness", 0).edit()
                .putLong(uid() + ":" + key, System.currentTimeMillis()).apply();
    }

    public void list(boolean refresh, Result<List<BillDocument>> callback) {
        if (cloud == null) {
            try { callback.done(demo.read(), null); } catch (IOException e) { callback.done(null, e); }
            return;
        }
        Task<QuerySnapshot> cache = cloud.collection(uid()).whereEqualTo("typo", "Chart").get(Source.CACHE);
        cache.addOnCompleteListener(result -> {
            QuerySnapshot saved = result.isSuccessful() ? result.getResult() : null;
            int previousCount = context.getSharedPreferences("cache-freshness", 0)
                    .getInt(uid() + ":bills:count", -1);
            boolean complete = saved != null && previousCount == saved.size() && fresh("bills") > 0;
            if (complete) decode(saved, callback);
            if (!refresh && complete && System.currentTimeMillis() - fresh("bills") < FRESH_MS) return;
            cloud.collection(uid()).whereEqualTo("typo", "Chart").get(Source.SERVER)
                    .addOnSuccessListener(snapshot -> {
                        markFresh("bills");
                        context.getSharedPreferences("cache-freshness", 0).edit()
                                .putInt(uid() + ":bills:count", snapshot.size()).apply();
                        decode(snapshot, callback);
                    }).addOnFailureListener(error -> {
                        if (!complete && saved != null) decode(saved, callback);
                        else if (!complete) callback.done(null, error);
                    });
        });
    }

    private void decode(QuerySnapshot snapshot, Result<List<BillDocument>> callback) {
        List<BillDocument> bills = new ArrayList<>();
        int[] remaining = {snapshot.size()};
        if (remaining[0] == 0) { callback.done(bills, null); return; }
        for (DocumentSnapshot document : snapshot.getDocuments()) {
            BillDocument bill;
            try {
                String data = document.getString("ledgerJson");
                bill = data == null ? new BillDocument() : BillDocument.fromJson(new JSONObject(data));
            } catch (JSONException ignored) { bill = new BillDocument(); }
            bill.id = document.getId();
            if (bill.name.isEmpty()) bill.name = document.getString("Chartname") == null ? "Без названия" : document.getString("Chartname");
            if (document.getTimestamp("createdAt") != null) bill.createdAt = document.getTimestamp("createdAt").toDate().getTime();
            BillDocument current = bill;
            // 1.2 stored members in separate documents and had no ledgerJson field.
            cloud.collection(uid()).whereEqualTo("ParentChart", bill.id).get(Source.CACHE)
                    .addOnCompleteListener(cached -> {
                        int expected = 0;
                        try { expected = Integer.parseInt(document.getString("NumberOfPeople")); }
                        catch (Exception ignored) { expected = current.people.size(); }
                        if (cached.isSuccessful() && cached.getResult().size() == expected) {
                            addBill(bills, current, cached.getResult(), remaining, callback); return;
                        }
                        cloud.collection(uid()).whereEqualTo("ParentChart", current.id).get(Source.SERVER)
                                .addOnSuccessListener(remote -> addBill(bills, current, remote, remaining, callback))
                                .addOnFailureListener(error -> addBill(bills, current,
                                        cached.isSuccessful() ? cached.getResult() : null, remaining, callback));
                    });
        }
    }

    private void addBill(List<BillDocument> bills, BillDocument bill, QuerySnapshot members,
                         int[] remaining, Result<List<BillDocument>> callback) {
        if (members != null && !members.isEmpty()) {
            bill.people.clear();
            List<DocumentSnapshot> sorted = new ArrayList<>(members.getDocuments());
            sorted.sort((a, b) -> {
                int x = suffix(a.getId()), y = suffix(b.getId());
                return Integer.compare(x, y);
            });
            for (DocumentSnapshot member : sorted) {
                Long amount = member.getLong("amountMinor");
                bill.people.add(new BillDocument.Person(member.getString("name") == null ? "Участник" : member.getString("name"),
                        member.getString("phone") == null ? "" : member.getString("phone"),
                        amount == null ? 0 : amount, Boolean.TRUE.equals(member.getBoolean("paid"))));
            }
        }
        bills.add(bill);
        if (--remaining[0] == 0) {
            bills.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));
            callback.done(bills, null);
        }
    }

    private static int suffix(String id) {
        try { return Integer.parseInt(id.substring(id.lastIndexOf('_') + 1)); }
        catch (NumberFormatException e) { return Integer.MAX_VALUE; }
    }

    public void save(BillDocument bill, Result<Void> callback) {
        if (bill.name.trim().isEmpty() || bill.people.isEmpty() || bill.people.size() > 450) {
            callback.done(null, new IllegalArgumentException("Введите название и добавьте участников")); return;
        }
        try { bill.total(); } catch (RuntimeException e) { callback.done(null, e); return; }
        if (cloud == null) {
            try {
                List<BillDocument> bills = demo.read();
                int index = indexOf(bills, bill.id);
                if (index >= 0) {
                    BillDocument previous = bills.get(index);
                    for (int i = 0; i < Math.min(previous.people.size(), bill.people.size()); i++)
                        bill.people.get(i).paid = previous.people.get(i).paid;
                    bills.set(index, bill);
                } else bills.add(bill);
                demo.write(bills);
                callback.done(null, null);
            } catch (IOException e) { callback.done(null, e); }
            return;
        }
        try {
                Map<String, Object> chart = new HashMap<>();
                chart.put("typo", "Chart");
                chart.put("Chartname", bill.name);
                chart.put("fullAmount", Money.format(bill.total()));
                chart.put("NumberOfPeople", String.valueOf(bill.people.size()));
                chart.put("percent", "0");
                chart.put("createdAt", new java.util.Date(bill.createdAt));
                chart.put("totalMinor", bill.total());
                chart.put("paidMinor", bill.paid());
                chart.put("ledgerJson", bill.toJson().toString());
                List<Map<String, Object>> participants = new ArrayList<>();
                for (int i = 0; i < bill.people.size(); i++) {
                    BillDocument.Person p = bill.people.get(i);
                    Map<String, Object> member = new HashMap<>();
                    member.put("ParentChart", bill.id);
                    member.put("name", p.name);
                    member.put("phone", p.phone);
                    member.put("amountMinor", p.amount);
                    member.put("paid", p.paid);
                    member.put("date", p.paid ? "Оплачено" : "Не оплачено");
                    participants.add(member);
                }
                cloud.runTransaction(transaction -> {
                    // Atomically check existence: retries or concurrent saves never reset payments.
                    if (transaction.get(ref(bill.id)).exists()) return null;
                    transaction.set(ref(bill.id), chart);
                    for (int i = 0; i < participants.size(); i++)
                        transaction.set(ref(bill.id + "_" + i), participants.get(i));
                    return null;
                }).addOnSuccessListener(v -> { markFresh("bills"); callback.done(null, null); })
                        .addOnFailureListener(e -> callback.done(null, e));
        } catch (JSONException e) { callback.done(null, e); }
    }

    public void mark(BillDocument bill, int person, boolean paid, Result<Void> callback) {
        if (person < 0 || person >= bill.people.size()) { callback.done(null, new IllegalArgumentException("Участник не найден")); return; }
        if (cloud == null) {
            try {
                List<BillDocument> bills = demo.read();
                int index = indexOf(bills, bill.id);
                if (index < 0) throw new IOException("Счёт удалён");
                bills.get(index).people.get(person).paid = paid;
                demo.write(bills);
                callback.done(null, null);
            } catch (IOException e) { callback.done(null, e); }
            return;
        }
        cloud.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(ref(bill.id));
            DocumentSnapshot member = transaction.get(ref(bill.id + "_" + person));
            if (!snapshot.exists() || !member.exists()) throw new IllegalStateException("Счёт удалён");
            try {
                boolean previous = Boolean.TRUE.equals(member.getBoolean("paid"));
                long amount = member.getLong("amountMinor") == null ? 0L : member.getLong("amountMinor");
                long total = snapshot.getLong("totalMinor") == null ? 0L : snapshot.getLong("totalMinor");
                long before = snapshot.getLong("paidMinor") == null ? 0L : snapshot.getLong("paidMinor");
                long next = before + (previous == paid ? 0 : paid ? amount : -amount);
                if (next < 0 || next > total) throw new IllegalStateException("Некорректная сумма оплаты");
                Map<String, Object> changes = new HashMap<>();
                changes.put("paidMinor", next);
                changes.put("percent", total == 0 ? "0" : String.valueOf(next * 100 / total));
                if (snapshot.getString("ledgerJson") != null) {
                    BillDocument latest = BillDocument.fromJson(new JSONObject(snapshot.getString("ledgerJson")));
                    latest.people.get(person).paid = paid;
                    changes.put("ledgerJson", latest.toJson().toString());
                }
                transaction.update(ref(bill.id), changes);
                transaction.update(ref(bill.id + "_" + person), "paid", paid,
                        "date", paid ? "Оплачено" : "Не оплачено");
            } catch (JSONException e) { throw new IllegalStateException(e); }
            return null;
        }).addOnSuccessListener(v -> { markFresh("bills"); callback.done(null, null); })
                .addOnFailureListener(e -> callback.done(null, e));
    }

    public void delete(BillDocument bill, Result<Void> callback) {
        if (cloud == null) {
            try {
                List<BillDocument> bills = demo.read();
                bills.removeIf(item -> bill.id.equals(item.id));
                demo.write(bills);
                removePhoto(bill.id);
                callback.done(null, null);
            } catch (IOException e) { callback.done(null, e); }
            return;
        }
        cloud.collection(uid()).whereEqualTo("ParentChart", bill.id).limit(451).get(Source.SERVER)
                .addOnSuccessListener(members -> {
                    if (members.size() > 450) { callback.done(null, new IllegalStateException("Слишком много участников")); return; }
                    WriteBatch batch = cloud.batch();
                    batch.delete(ref(bill.id));
                    for (DocumentSnapshot member : members) batch.delete(member.getReference());
                    batch.commit().addOnSuccessListener(v -> { markFresh("bills"); removePhoto(bill.id); callback.done(null, null); })
                            .addOnFailureListener(e -> callback.done(null, e));
                }).addOnFailureListener(e -> callback.done(null, e));
    }

    private int indexOf(List<BillDocument> bills, String id) {
        for (int i = 0; i < bills.size(); i++) if (bills.get(i).id.equals(id)) return i;
        return -1;
    }

    private void removePhoto(String id) {
        // Only app-created files in private storage; cloud has never received a photo.
        java.io.File photo = new java.io.File(new java.io.File(context.getFilesDir(), "receipts"), id + ".jpg");
        if (photo.exists() && !photo.delete()) android.util.Log.w("BillRepository", "Could not remove local receipt");
    }
}

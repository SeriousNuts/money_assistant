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
            Long revision = document.getLong("revision");
            if (revision != null) bill.revision = revision;
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
            List<BillDocument.Person> saved = new ArrayList<>(bill.people);
            bill.people.clear();
            List<DocumentSnapshot> sorted = new ArrayList<>(members.getDocuments());
            sorted.sort((a, b) -> {
                int x = suffix(a.getId()), y = suffix(b.getId());
                return Integer.compare(x, y);
            });
            for (DocumentSnapshot member : sorted) {
                Long amount = member.getLong("amountMinor");
                BillDocument.Person person = new BillDocument.Person(member.getString("name") == null ? "Участник" : member.getString("name"),
                        member.getString("phone") == null ? "" : member.getString("phone"),
                        amount == null ? 0 : amount, Boolean.TRUE.equals(member.getBoolean("paid")));
                int index = bill.people.size();
                person.id = member.getString("personId") == null ? bill.id + ":" + index : member.getString("personId");
                if (index < saved.size() && saved.get(index).amount == person.amount)
                    person.manualAmount = saved.get(index).manualAmount;
                bill.people.add(person);
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
            synchronized (DemoLedger.class) { try {
                List<BillDocument> bills = demo.read();
                int index = indexOf(bills, bill.id);
                if (index >= 0) throw new IOException("Счёт уже существует; откройте его для редактирования");
                bills.add(bill);
                demo.write(bills);
                callback.done(null, null);
            } catch (IOException e) { callback.done(null, e); } }
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
                chart.put("revision", bill.revision);
                chart.put("ledgerJson", bill.toJson().toString());
                List<Map<String, Object>> participants = new ArrayList<>();
                for (int i = 0; i < bill.people.size(); i++) {
                    BillDocument.Person p = bill.people.get(i);
                    Map<String, Object> member = new HashMap<>();
                    member.put("ParentChart", bill.id);
                    member.put("name", p.name);
                    member.put("personId", p.id);
                    member.put("phone", p.phone);
                    member.put("amountMinor", p.amount);
                    member.put("paid", p.paid);
                    member.put("date", p.paid ? "Оплачено" : "Не оплачено");
                    participants.add(member);
                }
                cloud.runTransaction(transaction -> {
                    // Atomically check existence: retries or concurrent saves never reset payments.
                    if (transaction.get(ref(bill.id)).exists()) throw new IllegalStateException("Счёт уже существует");
                    transaction.set(ref(bill.id), chart);
                    for (int i = 0; i < participants.size(); i++)
                        transaction.set(ref(bill.id + "_" + i), participants.get(i));
                    return null;
                }).addOnSuccessListener(v -> { markFresh("bills"); callback.done(null, null); })
                        .addOnFailureListener(e -> callback.done(null, e));
        } catch (JSONException e) { callback.done(null, e); }
    }

    /** Update only the version the editor opened; existing member IDs and payments stay intact. */
    public void update(BillDocument edited, Result<Void> callback) {
        if (edited.name.trim().isEmpty() || edited.people.isEmpty() || edited.people.size() > 450) {
            callback.done(null, new IllegalArgumentException("Введите название и добавьте участников")); return;
        }
        try { edited.total(); } catch (RuntimeException e) { callback.done(null, e); return; }
        if (cloud == null) {
            synchronized (DemoLedger.class) { try {
                List<BillDocument> bills = demo.read();
                int index = indexOf(bills, edited.id);
                if (index < 0) throw new IOException("Счёт удалён");
                BillDocument previous = bills.get(index);
                mergeExisting(previous, edited);
                BillDocument updated = BillDocument.fromJson(edited.toJson());
                updated.revision = previous.revision + 1;
                bills.set(index, updated);
                demo.write(bills);
                callback.done(null, null);
            } catch (IOException | JSONException | RuntimeException e) { callback.done(null, e); } }
            return;
        }
        try {
            String draft = edited.toJson().toString();
            cloud.runTransaction(transaction -> {
                DocumentSnapshot snapshot = transaction.get(ref(edited.id));
                if (!snapshot.exists()) throw new IllegalStateException("Счёт удалён");
                long revision = snapshot.getLong("revision") == null ? 0 : snapshot.getLong("revision");
                if (revision != edited.revision) throw new IllegalStateException("Счёт изменён на другом устройстве. Обновите и повторите правку");
                int count;
                try { count = Integer.parseInt(snapshot.getString("NumberOfPeople")); }
                catch (Exception e) { throw new IllegalStateException("Невозможно проверить участников счёта", e); }
                if (count < 0 || count > 450 || edited.people.size() < count)
                    throw new IllegalStateException("Нельзя удалить участников при редактировании");
                List<DocumentSnapshot> members = new ArrayList<>();
                for (int i = 0; i < count; i++) members.add(transaction.get(ref(edited.id + "_" + i)));
                BillDocument updated;
                try { updated = BillDocument.fromJson(new JSONObject(draft)); }
                catch (JSONException e) { throw new IllegalStateException(e); }
                for (int i = 0; i < count; i++) {
                    DocumentSnapshot member = members.get(i);
                    if (!member.exists()) throw new IllegalStateException("Не найден участник счёта");
                    String id = member.getString("personId") == null ? edited.id + ":" + i : member.getString("personId");
                    if (!id.equals(updated.people.get(i).id)) throw new IllegalStateException("Нельзя переставить участников счёта");
                    updated.people.get(i).paid = Boolean.TRUE.equals(member.getBoolean("paid"));
                }
                updated.createdAt = snapshot.getTimestamp("createdAt") == null
                        ? edited.createdAt : snapshot.getTimestamp("createdAt").toDate().getTime();
                updated.revision = revision + 1;
                Map<String, Object> changes = new HashMap<>();
                changes.put("Chartname", updated.name);
                changes.put("NumberOfPeople", String.valueOf(updated.people.size()));
                changes.put("fullAmount", Money.format(updated.total()));
                changes.put("totalMinor", updated.total());
                changes.put("paidMinor", updated.paid());
                changes.put("percent", updated.total() == 0 ? "0" : String.valueOf(updated.paid() * 100 / updated.total()));
                changes.put("revision", updated.revision);
                try { changes.put("ledgerJson", updated.toJson().toString()); }
                catch (JSONException e) { throw new IllegalStateException(e); }
                transaction.update(ref(edited.id), changes);
                for (int i = 0; i < updated.people.size(); i++) {
                    BillDocument.Person p = updated.people.get(i);
                    Map<String, Object> fields = new HashMap<>();
                    fields.put("ParentChart", edited.id);
                    fields.put("personId", p.id);
                    fields.put("name", p.name);
                    fields.put("phone", p.phone);
                    fields.put("amountMinor", p.amount);
                    fields.put("paid", p.paid);
                    fields.put("date", p.paid ? "Оплачено" : "Не оплачено");
                    transaction.set(ref(edited.id + "_" + i), fields);
                }
                return null;
            }).addOnSuccessListener(v -> { markFresh("bills"); callback.done(null, null); })
                    .addOnFailureListener(e -> callback.done(null, e));
        } catch (JSONException e) { callback.done(null, e); }
    }

    private static void mergeExisting(BillDocument previous, BillDocument edited) {
        if (previous.revision != edited.revision)
            throw new IllegalStateException("Счёт изменён. Обновите и повторите правку");
        if (edited.people.size() < previous.people.size())
            throw new IllegalStateException("Нельзя удалить участников при редактировании");
        for (int i = 0; i < previous.people.size(); i++) {
            BillDocument.Person before = previous.people.get(i), after = edited.people.get(i);
            if (!before.id.equals(after.id)) throw new IllegalStateException("Нельзя переставить участников счёта");
            after.paid = before.paid;
        }
        edited.createdAt = previous.createdAt;
    }

    public void mark(BillDocument bill, int person, boolean paid, Result<Void> callback) {
        if (person < 0 || person >= bill.people.size()) { callback.done(null, new IllegalArgumentException("Участник не найден")); return; }
        if (cloud == null) {
            synchronized (DemoLedger.class) { try {
                List<BillDocument> bills = demo.read();
                int index = indexOf(bills, bill.id);
                if (index < 0) throw new IOException("Счёт удалён");
                if (person >= bills.get(index).people.size()
                        || !bill.people.get(person).id.equals(bills.get(index).people.get(person).id))
                    throw new IOException("Состав участников изменён. Обновите счёт");
                bills.get(index).people.get(person).paid = paid;
                bills.get(index).revision++;
                demo.write(bills);
                callback.done(null, null);
            } catch (IOException e) { callback.done(null, e); } }
            return;
        }
        cloud.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(ref(bill.id));
            DocumentSnapshot member = transaction.get(ref(bill.id + "_" + person));
            if (!snapshot.exists() || !member.exists()) throw new IllegalStateException("Счёт удалён");
            String memberId = member.getString("personId") == null
                    ? bill.id + ":" + person : member.getString("personId");
            if (!memberId.equals(bill.people.get(person).id))
                throw new IllegalStateException("Состав участников изменён. Обновите счёт");
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
                long revision = snapshot.getLong("revision") == null ? 0 : snapshot.getLong("revision");
                changes.put("revision", revision + 1);
                if (snapshot.getString("ledgerJson") != null) {
                    BillDocument latest = BillDocument.fromJson(new JSONObject(snapshot.getString("ledgerJson")));
                    latest.people.get(person).paid = paid;
                    latest.revision = revision + 1;
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
            synchronized (DemoLedger.class) { try {
                List<BillDocument> bills = demo.read();
                bills.removeIf(item -> bill.id.equals(item.id));
                demo.write(bills);
                removePhotos(bill);
                callback.done(null, null);
            } catch (IOException e) { callback.done(null, e); } }
            return;
        }
        cloud.collection(uid()).whereEqualTo("ParentChart", bill.id).limit(451).get(Source.SERVER)
                .addOnSuccessListener(members -> {
                    if (members.size() > 450) { callback.done(null, new IllegalStateException("Слишком много участников")); return; }
                    WriteBatch batch = cloud.batch();
                    batch.delete(ref(bill.id));
                    for (DocumentSnapshot member : members) batch.delete(member.getReference());
                    batch.commit().addOnSuccessListener(v -> { markFresh("bills"); removePhotos(bill); callback.done(null, null); })
                            .addOnFailureListener(e -> callback.done(null, e));
                }).addOnFailureListener(e -> callback.done(null, e));
    }

    private int indexOf(List<BillDocument> bills, String id) {
        for (int i = 0; i < bills.size(); i++) if (bills.get(i).id.equals(id)) return i;
        return -1;
    }

    private void removePhotos(BillDocument bill) {
        // Only app-created files in private storage; cloud has never received a photo.
        for (ReceiptDocument receipt : bill.receipts) ReceiptPhotos.remove(context, receipt);
        ReceiptPhotos.remove(context, ReceiptDocument.legacy(bill.id));
    }
}

package com.example.myapplication;

import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.WriteBatch;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Client-side deletion of the existing per-UID Firestore collection, followed by Firebase Auth.
 * A trusted backend is still needed to guarantee cleanup against concurrent devices/subcollections.
 */
final class AccountDeletion {
    private static final int PAGE_SIZE = 250;
    private final Context context;
    private final FirebaseAuth auth;
    private final FirebaseUser user;
    private final FirebaseFirestore cloud;
    private final String uid;
    private final BillDraftStore drafts;
    private final List<BillDocument> photos = new ArrayList<>();
    private final BillRepository.Result<Void> callback;
    private int deletedDocuments;

    private AccountDeletion(Context context, FirebaseAuth auth, FirebaseUser user,
                            BillRepository.Result<Void> callback) {
        this.context = context.getApplicationContext();
        this.auth = auth;
        this.user = user;
        this.cloud = FirebaseFirestore.getInstance();
        this.uid = user.getUid();
        this.drafts = new BillDraftStore(context);
        this.callback = callback;
    }

    static void start(Context context, String password, BillRepository.Result<Void> callback) {
        if (DemoSession.active(context)) {
            callback.done(null, new IllegalStateException("Демоаккаунт не является облачным аккаунтом")); return;
        }
        if (FirebaseApp.getApps(context).isEmpty()) {
            callback.done(null, new IllegalStateException("Облачный аккаунт недоступен")); return;
        }
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            callback.done(null, new IllegalStateException("Войдите в облачный аккаунт")); return;
        }
        if (password == null || password.isEmpty()) {
            callback.done(null, new IllegalArgumentException("Введите пароль аккаунта")); return;
        }
        AccountDeletion deletion = new AccountDeletion(context, auth, user, callback);
        user.reauthenticate(EmailAuthProvider.getCredential(user.getEmail(), password))
                .addOnSuccessListener(ignored -> deletion.deletePage())
                .addOnFailureListener(error -> callback.done(null, error));
    }

    private void deletePage() {
        if (auth.getCurrentUser() != user) {
            callback.done(null, new IllegalStateException("Сменился аккаунт. Повторите удаление")); return;
        }
        cloud.collection(uid).limit(PAGE_SIZE).get(Source.SERVER)
                .addOnSuccessListener(this::deleteBatch)
                .addOnFailureListener(error -> callback.done(null, error));
    }

    private void deleteBatch(QuerySnapshot page) {
        if (page.isEmpty()) {
            // Preflight before deleting Auth: otherwise corrupt local drafts become inaccessible.
            final List<BillDraftStore.Draft> localDrafts;
            try { localDrafts = drafts.list(); }
            catch (IOException error) { callback.done(null, partial(error)); return; }
            user.delete().addOnSuccessListener(ignored -> {
                Exception localError = removeLocalData(localDrafts);
                auth.signOut();
                callback.done(null, localError);
            }).addOnFailureListener(error -> callback.done(null, partial(error)));
            return;
        }
        if (auth.getCurrentUser() != user) {
            callback.done(null, new IllegalStateException("Сменился аккаунт. Повторите удаление")); return;
        }
        WriteBatch batch = cloud.batch();
        for (DocumentSnapshot document : page.getDocuments()) {
            if ("Chart".equals(document.getString("typo"))) try {
                String json = document.getString("ledgerJson");
                if (json != null) photos.add(BillDocument.fromJson(new JSONObject(json)));
            } catch (Exception e) {
                Log.w("AccountDeletion", "Could not inspect receipt files", e);
            }
            batch.delete(document.getReference());
        }
        batch.commit().addOnSuccessListener(ignored -> {
            deletedDocuments += page.size();
            deletePage();
        }).addOnFailureListener(error -> callback.done(null, partial(error)));
    }

    private Exception partial(Exception cause) {
        return deletionError(deletedDocuments, cause);
    }

    static Exception deletionError(int deletedDocuments, Exception cause) {
        return deletedDocuments == 0 ? cause : new IllegalStateException(
                "Удалена часть облачных данных, аккаунт сохранён. Повторите операцию при подключении к сети.", cause);
    }

    private Exception removeLocalData(List<BillDraftStore.Draft> localDrafts) {
        boolean failed = false;
        for (BillDocument bill : photos) {
            for (ReceiptDocument receipt : bill.receipts) failed |= !removePhoto(receipt);
            failed |= !removePhoto(ReceiptDocument.legacy(bill.id));
        }
        for (BillDraftStore.Draft draft : localDrafts) {
            try { drafts.remove(draft.bill.id); }
            catch (IOException e) { Log.w("AccountDeletion", "Local draft", e); failed = true; }
            if (draft.stagedPhoto != null && !draft.bill.receipts.isEmpty()) try {
                File file = new File(draft.stagedPhoto);
                if (ReceiptPhotos.isStage(context, draft.bill.receipts.get(0), file)
                        && file.exists() && !file.delete()) failed = true;
            } catch (IOException e) { Log.w("AccountDeletion", "Draft photo", e); failed = true; }
        }
        if (!context.getSharedPreferences("cache-freshness", Context.MODE_PRIVATE).edit().clear().commit())
            failed = true;
        if (!context.getSharedPreferences("receipt-ai", Context.MODE_PRIVATE).edit().clear().commit())
            failed = true;
        // Firestore's offline persistence may still contain data. A trusted backend must handle
        // concurrent devices and nested collections; do not describe this as complete erasure.
        return failed ? new IllegalStateException("Аккаунт удалён, но часть локальных файлов или кэша осталась. Очистите данные приложения в настройках Android.") : null;
    }

    private boolean removePhoto(ReceiptDocument receipt) {
        try {
            File file = ReceiptPhotos.photo(context, receipt);
            return !file.exists() || file.delete();
        } catch (IOException e) { Log.w("AccountDeletion", "Receipt photo", e); return false; }
    }
}

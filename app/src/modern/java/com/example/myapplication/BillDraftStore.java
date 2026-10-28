package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Unsaved bills are private to the current account and never appear in the paid ledger. */
final class BillDraftStore {
    static final class Draft {
        final BillDocument bill;
        final String enteredTotal, stagedPhoto;
        final boolean share;

        Draft(BillDocument bill, String enteredTotal, boolean share, String stagedPhoto) {
            this.bill = bill;
            this.enteredTotal = enteredTotal;
            this.share = share;
            this.stagedPhoto = stagedPhoto;
        }

        JSONObject toJson() throws JSONException {
            return new JSONObject().put("bill", bill.toJson()).put("enteredTotal", enteredTotal)
                    .put("share", share).put("stagedPhoto", stagedPhoto);
        }

        static Draft fromJson(String json) throws JSONException {
            JSONObject data = new JSONObject(json);
            return new Draft(BillDocument.fromJson(data.getJSONObject("bill")),
                    data.optString("enteredTotal", ""), data.optBoolean("share", false),
                    data.isNull("stagedPhoto") ? null : data.optString("stagedPhoto", null));
        }
    }

    private final SharedPreferences prefs;
    private final String prefix;

    BillDraftStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences("unfinished-bills", Context.MODE_PRIVATE);
        if (DemoSession.active(context)) prefix = "demo:";
        else if (!FirebaseApp.getApps(context).isEmpty() && FirebaseAuth.getInstance().getCurrentUser() != null)
            prefix = "account:" + FirebaseAuth.getInstance().getCurrentUser().getUid() + ":";
        else prefix = "guest:";
    }

    void save(Draft draft) throws IOException {
        try {
            if (!prefs.edit().putString(prefix + draft.bill.id, draft.toJson().toString()).commit())
                throw new IOException("Не удалось сохранить черновик счёта");
        } catch (JSONException e) { throw new IOException("Не удалось записать черновик", e); }
    }

    Draft get(String id) throws IOException {
        String json = prefs.getString(prefix + id, null);
        if (json == null) return null;
        try { return Draft.fromJson(json); }
        catch (JSONException e) { throw new IOException("Повреждён черновик счёта. Он не удалён.", e); }
    }

    List<Draft> list() throws IOException {
        List<Draft> drafts = new ArrayList<>();
        for (java.util.Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (!entry.getKey().startsWith(prefix)) continue;
            try { drafts.add(Draft.fromJson((String) entry.getValue())); }
            catch (JSONException | ClassCastException e) {
                throw new IOException("Не удалось прочитать черновик. Данные не удалены.", e);
            }
        }
        drafts.sort((a, b) -> Long.compare(b.bill.createdAt, a.bill.createdAt));
        return drafts;
    }

    void remove(String id) throws IOException {
        if (!prefs.edit().remove(prefix + id).commit()) throw new IOException("Не удалось удалить черновик");
    }
}

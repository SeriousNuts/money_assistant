package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.File;

/** Compact receipt overview; an unsaved receipt is never added to the stored bill. */
public final class ReceiptOverviewActivity extends AppCompatActivity {
    private static final int EDIT = 301;
    private BillRepository repository;
    private BillDocument bill;
    private LinearLayout list;
    private boolean saving;
    private BillDocument pendingDraft;
    private ReceiptDocument pendingRemove, pendingEdit, pendingPhoto;
    private String pendingStage, photoStage;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        repository = new BillRepository(this);
        LinearLayout root = Ui.page(this);
        Ui.toolbar(this, root, "Чеки", true, null);
        LinearLayout body = Ui.scroll(this, root);
        Ui.add(this, body, Ui.headline(this, "Чеки счёта", 34), 8);
        Ui.add(this, body, Ui.label(this, "Добавляйте фото и позиции в каждый чек отдельно."), 8);
        list = Ui.column(this);
        Ui.add(this, body, list, 16);
        Ui.add(this, Ui.footer(this, root), Ui.button(this, "＋  Добавить чек", this::addReceipt), 0);
        if (state != null && state.getString("pendingDraft") != null) try {
            pendingDraft = BillDocument.fromJson(new JSONObject(state.getString("pendingDraft")));
            pendingStage = state.getString("pendingStage");
            String removedId = state.getString("pendingRemove"), editedId = state.getString("pendingEdit");
            for (ReceiptDocument item : pendingDraft.receipts)
                if (item.id.equals(editedId)) pendingEdit = item;
            if (removedId != null) pendingRemove = ReceiptDocument.fromJson(new JSONObject(state.getString("removedJson")));
        } catch (Exception e) { Ui.error(this, e); }
        load();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        if (pendingDraft != null) try { state.putString("pendingDraft", pendingDraft.toJson().toString()); }
        catch (Exception ignored) { }
        state.putString("pendingStage", pendingStage);
        if (pendingEdit != null) state.putString("pendingEdit", pendingEdit.id);
        if (pendingRemove != null) try {
            state.putString("pendingRemove", pendingRemove.id);
            state.putString("removedJson", pendingRemove.toJson().toString());
        } catch (Exception ignored) { }
        super.onSaveInstanceState(state);
    }

    private void load() {
        repository.list(true, (items, error) -> {
            if (items == null) { Ui.error(this, error); return; }
            for (BillDocument item : items) if (item.id.equals(getIntent().getStringExtra("bill"))) {
                bill = item;
                // Pre-1.4.2 bills may have a photo but no marked positions.
                if (bill.receipts.isEmpty() && new File(getFilesDir(), "receipts/" + bill.id + ".jpg").exists())
                    bill.primaryReceipt();
                render();
                return;
            }
            Ui.error(this, new IllegalStateException("Счёт не найден"));
        });
    }

    private void render() {
        list.removeAllViews();
        if (pendingDraft != null) {
            Ui.add(this, list, Ui.label(this, "Изменения не сохранены. Проверьте соединение и повторите попытку."), 5);
            Ui.add(this, list, Ui.secondary(this, "Повторить сохранение", () -> save(
                    pendingDraft, pendingRemove, pendingEdit, pendingStage)), 5);
            Ui.add(this, list, Ui.link(this, "Отменить несохранённые изменения", this::discardPending), 5);
        }
        if (pendingPhoto != null) Ui.add(this, list, Ui.secondary(this,
                "Повторить сохранение фото", this::commitPendingPhoto), 5);
        if (bill.receipts.isEmpty()) Ui.add(this, list,
                Ui.label(this, "Пока нет чеков. Нажмите «Добавить чек»."), 5);
        for (ReceiptDocument receipt : bill.receipts) {
            LinearLayout card = Ui.card(this, list);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) card.getLayoutParams();
            params.topMargin = Ui.dp(this, 9);
            card.setLayoutParams(params);
            long amount = 0;
            for (ReceiptLine line : receipt.lines) amount = Math.addExact(amount, line.amount);
            Ui.add(this, card, Ui.text(this, receipt.name, 17, true), 0);
            Ui.add(this, card, Ui.label(this, receipt.lines.size() + " позиций · " + Money.format(amount)), 5);
            LinearLayout actions = Ui.row(this);
            TextView open = Ui.link(this, "Открыть", () -> openReceipt(bill, receipt));
            Ui.addRow(this, actions, open, 1);
            TextView delete = Ui.link(this, "Удалить", () -> confirmDelete(receipt));
            Ui.addRow(this, actions, delete, 0);
            Ui.add(this, card, actions, 9);
        }
    }

    private void addReceipt() {
        if (saving || bill == null || pendingDraft != null || pendingPhoto != null) return;
        try {
            BillDocument draft = BillDocument.fromJson(bill.toJson());
            ReceiptDocument receipt = ReceiptDocument.create(bill.id);
            draft.receipts.add(receipt);
            openReceipt(draft, receipt);
        } catch (Exception e) { Ui.error(this, e); }
    }

    private void openReceipt(BillDocument draft, ReceiptDocument receipt) {
        if (saving || pendingDraft != null || pendingPhoto != null) return;
        try {
            startActivityForResult(new Intent(this, ReceiptActivity.class)
                    .putExtra("draft", draft.toJson().toString())
                    .putExtra("receiptId", receipt.id), EDIT);
        } catch (Exception e) { Ui.error(this, e); }
    }

    private void confirmDelete(ReceiptDocument receipt) {
        new AlertDialog.Builder(this).setTitle("Удалить чек?")
                .setMessage("Фото и позиции чека «" + receipt.name + "» будут удалены. Ручные долги и оплаты останутся.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Удалить", (d, w) -> {
                    if (saving || pendingDraft != null || pendingPhoto != null) return;
                    try {
                        BillDocument draft = BillDocument.fromJson(bill.toJson());
                        draft.receipts.removeIf(item -> item.id.equals(receipt.id));
                        draft.lines = draft.receipts.isEmpty() ? new java.util.ArrayList<>() : draft.receipts.get(0).lines;
                        draft.recalculateAmounts();
                        save(draft, receipt, null, null);
                    } catch (Exception e) { Ui.error(this, e); }
                }).show();
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != EDIT || result != RESULT_OK || data == null || saving) return;
        try {
            BillDocument draft = BillDocument.fromJson(new JSONObject(data.getStringExtra("draft")));
            ReceiptDocument edited = null;
            for (ReceiptDocument item : draft.receipts)
                if (item.id.equals(data.getStringExtra("receiptId"))) edited = item;
            if (edited == null || bill == null || !bill.id.equals(draft.id)) throw new IllegalStateException("Чек не найден");
            save(draft, null, edited, data.getStringExtra("stagedPhoto"));
        } catch (Exception e) { Ui.error(this, e); }
    }

    private void save(BillDocument draft, ReceiptDocument removed, ReceiptDocument edited, String stage) {
        saving = true;
        pendingDraft = draft;
        pendingRemove = removed;
        pendingEdit = edited;
        pendingStage = stage;
        repository.update(draft, (v, error) -> {
            saving = false;
            if (error != null) { Ui.error(this, error); render(); return; }
            pendingDraft = null;
            pendingRemove = null;
            pendingEdit = null;
            pendingStage = null;
            if (removed != null) ReceiptPhotos.remove(this, removed);
            if (edited != null && stage != null) {
                pendingPhoto = edited;
                photoStage = stage;
                commitPendingPhoto();
            }
            load();
        });
    }

    private void commitPendingPhoto() {
        if (pendingPhoto == null) return;
        try {
            ReceiptPhotos.commit(this, pendingPhoto, photoStage);
            pendingPhoto = null;
            photoStage = null;
            render();
        } catch (Exception e) { Ui.error(this, e); render(); }
    }

    private void discardPending() {
        if (pendingStage != null && pendingEdit != null) try {
            File photo = new File(pendingStage);
            if (ReceiptPhotos.isStage(this, pendingEdit, photo) && photo.exists()
                    && !photo.delete()) android.util.Log.w("ReceiptOverview", "Could not remove draft photo");
        } catch (Exception e) { android.util.Log.w("ReceiptOverview", "Could not clean draft photo", e); }
        pendingDraft = null;
        pendingRemove = null;
        pendingEdit = null;
        pendingStage = null;
        render();
    }
}

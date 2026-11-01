package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;

public final class StartMenu extends BaseActivity {
    private BillRepository repository;
    private BillDraftStore draftStore;
    private LinearLayout cards, total, filters, draftsView;
    private TextView cacheStatus;
    private List<BillDocument> bills = new ArrayList<>();
    private int filter;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) filter = state.getInt("filter", 0);
        repository = new BillRepository(this);
        draftStore = new BillDraftStore(this);
        LinearLayout root = Ui.page(this);
        Ui.toolbar(this, root, "money assistant", false, () -> {
            View anchor = root.findViewWithTag("toolbarMenu");
            PopupMenu menu = new PopupMenu(this, anchor);
            menu.getMenu().add(getString(R.string.local_device)).setEnabled(false);
            menu.getMenu().add(getString(R.string.language)).setOnMenuItemClickListener(item -> {
                Ui.chooseLanguage(this); return true;
            });
            menu.getMenu().add(getString(R.string.legal_menu)).setOnMenuItemClickListener(item -> {
                startActivity(new Intent(this, LegalActivity.class)); return true;
            });
            menu.show();
        });
        LinearLayout body = Ui.scroll(this, root);
        Ui.add(this, body, Ui.headline(this, getString(R.string.bills_title), 36), 8);
        Ui.add(this, body, Ui.label(this, getString(R.string.bills_subtitle)), 10);

        total = Ui.card(this, body);
        LinearLayout.LayoutParams summaryLayout = (LinearLayout.LayoutParams) total.getLayoutParams();
        summaryLayout.topMargin = Ui.dp(this, 24);
        total.setLayoutParams(summaryLayout);

        HorizontalScrollView scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setFillViewport(false);
        filters = Ui.row(this);
        scroller.addView(filters, new HorizontalScrollView.LayoutParams(-2, -2));
        Ui.add(this, body, scroller, 24);

        LinearLayout statusRow = Ui.row(this);
        cacheStatus = Ui.label(this, getString(R.string.local_device));
        Ui.addRow(this, statusRow, cacheStatus, 1);
        Ui.addRow(this, statusRow, Ui.link(this, getString(R.string.refresh_action), () -> load(true)), 0);
        Ui.add(this, body, statusRow, 10);

        draftsView = Ui.column(this);
        Ui.add(this, body, draftsView, 8);

        cards = Ui.column(this);
        Ui.add(this, body, cards, 0);
        Ui.add(this, Ui.footer(this, root), Ui.button(this, getString(R.string.new_bill), () ->
                startActivity(new Intent(this, AfterContact.class))), 0);
        render();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("filter", filter);
        super.onSaveInstanceState(state);
    }

    @Override protected void onResume() { super.onResume(); load(false); showDrafts(); }

    private void showDrafts() {
        draftsView.removeAllViews();
        try {
            List<BillDraftStore.Draft> drafts = draftStore.list();
            if (drafts.isEmpty()) return;
            Ui.add(this, draftsView, Ui.text(this, getString(R.string.drafts_title), 19, true), 14);
            for (BillDraftStore.Draft draft : drafts) {
                LinearLayout card = Ui.card(this, draftsView);
                LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) card.getLayoutParams();
                p.topMargin = Ui.dp(this, 8);
                card.setLayoutParams(p);
                Ui.add(this, card, Ui.text(this, draft.bill.name.isEmpty() ? getString(R.string.draft_unnamed) : draft.bill.name, 16, true), 0);
                Ui.add(this, card, Ui.label(this, getString(R.string.draft_people, draft.bill.people.size())), 4);
                LinearLayout actions = Ui.row(this);
                Ui.addRow(this, actions, Ui.link(this, getString(R.string.resume_action), () -> startActivity(
                        new Intent(this, AfterContact.class).putExtra("draftId", draft.bill.id))), 1);
                Ui.addRow(this, actions, Ui.link(this, getString(R.string.delete_action), () -> confirmDeleteDraft(draft)), 0);
                Ui.add(this, card, actions, 5);
            }
        } catch (Exception e) { Ui.error(this, e); }
    }

    private void confirmDeleteDraft(BillDraftStore.Draft draft) {
        new AlertDialog.Builder(this).setTitle(R.string.draft_delete_title)
                .setMessage(R.string.draft_delete_message)
                .setNegativeButton(R.string.cancel_action, null)
                .setPositiveButton(R.string.delete_action, (d, w) -> {
                    try {
                        draftStore.remove(draft.bill.id);
                        if (draft.stagedPhoto != null && !draft.bill.receipts.isEmpty()) {
                            java.io.File stage = new java.io.File(draft.stagedPhoto);
                            if (ReceiptPhotos.isStage(this, draft.bill.receipts.get(0), stage)
                                    && stage.exists() && !stage.delete())
                                android.util.Log.w("StartMenu", "Could not remove draft receipt photo");
                        }
                        showDrafts();
                    } catch (Exception e) { Ui.error(this, e); }
                }).show();
    }

    private void load(boolean force) {
        repository.list(force, (found, error) -> {
            if (found != null) {
                bills = found;
                cacheStatus.setText(R.string.local_device);
                render();
            } else {
                cacheStatus.setText(R.string.no_connection);
                Ui.error(this, error);
            }
        });
    }

    private void renderFilters() {
        filters.removeAllViews();
        String[] names = {getString(R.string.filter_all), getString(R.string.filter_open),
                getString(R.string.filter_closed)};
        for (int i = 0; i < names.length; i++) {
            final int choice = i;
            TextView chip = Ui.chip(this, names[i], filter == i, () -> { filter = choice; render(); });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
            if (i != 0) p.leftMargin = Ui.dp(this, 8);
            filters.addView(chip, p);
        }
    }

    private void render() {
        total.removeAllViews(); cards.removeAllViews(); renderFilters();
        long owed = 0;
        int open = 0, shown = 0;
        for (BillDocument bill : bills) {
            long remaining = Math.max(0, bill.total() - bill.paid());
            owed += remaining;
            if (remaining > 0) open++;
        }
        Ui.add(this, total, Ui.kicker(this, getString(R.string.remaining_total)), 0);
        Ui.add(this, total, Ui.headline(this, Money.format(owed), 42), 12);
        Ui.add(this, total, Ui.label(this, bills.isEmpty() ? getString(R.string.start_bill) :
                open == 0 ? getString(R.string.all_paid) : getString(R.string.open_count, open)), 12);

        for (BillDocument bill : bills) {
            boolean closed = bill.total() > 0 && bill.paid() >= bill.total();
            if ((filter == 1 && closed) || (filter == 2 && !closed)) continue;
            shown++;
            LinearLayout card = Ui.card(this, cards);
            LinearLayout.LayoutParams cardParams = (LinearLayout.LayoutParams) card.getLayoutParams();
            cardParams.topMargin = shown == 1 ? 0 : Ui.dp(this, 12);
            card.setLayoutParams(cardParams);
            card.setClickable(true);
            card.setOnClickListener(v -> open(bill));
            LinearLayout header = Ui.row(this);
            TextView name = Ui.text(this, bill.name, 17, true);
            name.setMaxLines(2);
            Ui.addRow(this, header, name, 1);
            TextView more = Ui.text(this, "⋮", 23, false);
            more.setGravity(android.view.Gravity.CENTER);
            more.setContentDescription(getString(R.string.bill_menu, bill.name));
            header.addView(more, new LinearLayout.LayoutParams(Ui.dp(this, 32), Ui.dp(this, 36)));
            more.setOnClickListener(v -> {
                PopupMenu menu = new PopupMenu(this, more);
                menu.getMenu().add(getString(R.string.delete_bill)).setOnMenuItemClickListener(item -> { confirmDelete(bill); return true; });
                menu.show();
            });
            Ui.add(this, card, header, 0);
            Ui.add(this, card, Ui.label(this, getString(R.string.people_count, bill.people.size())), 6);
            Ui.add(this, card, Ui.headline(this, Money.format(bill.total()), 32), 21);
            View bar = Ui.progress(this, bill.paid(), bill.total());
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(-1, Ui.dp(this, 4));
            barParams.topMargin = Ui.dp(this, 20);
            card.addView(bar, barParams);
            TextView percent = Ui.label(this, getString(R.string.paid_percent,
                    bill.total() == 0 ? 0 : bill.paid() * 100 / bill.total()));
            percent.setTextSize(12);
            percent.setGravity(android.view.Gravity.RIGHT);
            Ui.add(this, card, percent, 4);
        }
        if (shown == 0) Ui.add(this, cards, Ui.label(this, getString(bills.isEmpty() ?
                R.string.bills_empty : R.string.bills_filter_empty)), 24);
    }

    private void open(BillDocument bill) {
        Intent intent = new Intent(this, MainWindow.class);
        intent.putExtra("bill", bill.id);
        startActivity(intent);
    }

    private void confirmDelete(BillDocument bill) {
        new AlertDialog.Builder(this).setTitle(R.string.delete_bill_title)
                .setMessage(getString(R.string.delete_bill_message, bill.name))
                .setNegativeButton(R.string.cancel_action, null).setPositiveButton(R.string.delete_action, (d, w) ->
                        repository.delete(bill, (v, error) -> {
                            if (error != null) Ui.error(this, error);
                            else load(true);
                        })).show();
    }
}

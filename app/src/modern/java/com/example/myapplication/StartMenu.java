package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public final class StartMenu extends AppCompatActivity {
    private BillRepository repository;
    private LinearLayout cards, total, filters;
    private TextView cacheStatus;
    private List<BillDocument> bills = new ArrayList<>();
    private int filter;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) filter = state.getInt("filter", 0);
        repository = new BillRepository(this);
        LinearLayout root = Ui.page(this);
        Ui.toolbar(this, root, "money assistant", false, () -> {
            View anchor = root.findViewWithTag("toolbarMenu");
            PopupMenu menu = new PopupMenu(this, anchor);
            menu.getMenu().add(DemoSession.active(this) ? "Тестовый аккаунт" : "Личный аккаунт").setEnabled(false);
            menu.getMenu().add("Выйти").setOnMenuItemClickListener(item -> { signOut(); return true; });
            menu.show();
        });
        LinearLayout body = Ui.scroll(this, root);
        Ui.add(this, body, Ui.headline(this, "Общие счета", 36), 8);
        Ui.add(this, body, Ui.label(this, "Меньше расчётов. Больше хороших встреч."), 10);

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
        cacheStatus = Ui.label(this, DemoSession.active(this) ? "Демо · на устройстве" : "На устройстве");
        Ui.addRow(this, statusRow, cacheStatus, 1);
        Ui.addRow(this, statusRow, Ui.link(this, "Обновить", () -> load(true)), 0);
        Ui.add(this, body, statusRow, 10);

        cards = Ui.column(this);
        Ui.add(this, body, cards, 0);
        Ui.add(this, Ui.footer(this, root), Ui.button(this, "＋  Новый счёт", () ->
                startActivity(new Intent(this, AfterContact.class))), 0);
        render();
    }

    private void signOut() {
        DemoSession.set(this, false);
        if (!FirebaseApp.getApps(this).isEmpty()) FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("filter", filter);
        super.onSaveInstanceState(state);
    }

    @Override protected void onResume() { super.onResume(); load(false); }

    private void load(boolean force) {
        repository.list(force, (found, error) -> {
            if (found != null) {
                bills = found;
                cacheStatus.setText(DemoSession.active(this) ? "Демо · на устройстве" : "На устройстве");
                render();
            } else {
                cacheStatus.setText("Нет сохранённых счетов или связи");
                Ui.error(this, error);
            }
        });
    }

    private void renderFilters() {
        filters.removeAllViews();
        String[] names = {"Все", "В процессе", "Закрытые"};
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
        Ui.add(this, total, Ui.kicker(this, "ОСТАЛОСЬ СОБРАТЬ"), 0);
        Ui.add(this, total, Ui.headline(this, Money.format(owed), 42), 12);
        Ui.add(this, total, Ui.label(this, bills.isEmpty() ? "Начните с первого общего счёта" :
                open == 0 ? "Все счета закрыты. Вы в расчёте." : "Незакрытых счетов: " + open), 12);

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
            more.setContentDescription("Меню счёта " + bill.name);
            header.addView(more, new LinearLayout.LayoutParams(Ui.dp(this, 32), Ui.dp(this, 36)));
            more.setOnClickListener(v -> {
                PopupMenu menu = new PopupMenu(this, more);
                menu.getMenu().add("Удалить счёт").setOnMenuItemClickListener(item -> { confirmDelete(bill); return true; });
                menu.show();
            });
            Ui.add(this, card, header, 0);
            Ui.add(this, card, Ui.label(this, "Участников: " + bill.people.size()), 6);
            Ui.add(this, card, Ui.headline(this, Money.format(bill.total()), 32), 21);
            View bar = Ui.progress(this, bill.paid(), bill.total());
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(-1, Ui.dp(this, 4));
            barParams.topMargin = Ui.dp(this, 20);
            card.addView(bar, barParams);
            TextView percent = Ui.label(this, bill.total() == 0 ? "0% оплачено" :
                    bill.paid() * 100 / bill.total() + "% оплачено");
            percent.setTextSize(12);
            percent.setGravity(android.view.Gravity.RIGHT);
            Ui.add(this, card, percent, 4);
        }
        if (shown == 0) Ui.add(this, cards, Ui.label(this, bills.isEmpty() ?
                "Здесь пока нет счетов. Создайте первый!" : "Нет счетов для выбранного фильтра"), 24);
    }

    private void open(BillDocument bill) {
        Intent intent = new Intent(this, MainWindow.class);
        intent.putExtra("bill", bill.id);
        startActivity(intent);
    }

    private void confirmDelete(BillDocument bill) {
        new AlertDialog.Builder(this).setTitle("Удалить счёт?")
                .setMessage(bill.name + " и все его участники будут удалены")
                .setNegativeButton("Отмена", null).setPositiveButton("Удалить", (d, w) ->
                        repository.delete(bill, (v, error) -> {
                            if (error != null) Ui.error(this, error);
                            else load(true);
                        })).show();
    }
}

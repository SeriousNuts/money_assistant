package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public final class MainWindow extends AppCompatActivity {
    private BillDocument bill;
    private BillRepository repository;
    private LinearLayout body;
    private boolean updating;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        repository = new BillRepository(this);
        LinearLayout root = Ui.page(this);
        Ui.toolbar(this, root, "Расчёт", true, null);
        body = Ui.scroll(this, root);
        LinearLayout footer = Ui.footer(this, root);
        Ui.add(this, footer, Ui.button(this, "⇧  Поделиться расчётом", this::share), 0);
        TextView all = Ui.link(this, "Все счета", this::finish);
        all.setGravity(Gravity.CENTER);
        Ui.add(this, footer, all, 3);
        load(false);
    }

    private void load(boolean refresh) {
        repository.list(refresh, (items, error) -> {
            if (items == null) { Ui.error(this, error); return; }
            for (BillDocument item : items) if (item.id.equals(getIntent().getStringExtra("bill"))) {
                bill = item;
                render();
                return;
            }
            Ui.error(this, new IllegalStateException("Счёт не найден"));
        });
    }

    private void render() {
        body.removeAllViews();
        Ui.add(this, body, Ui.headline(this, bill.name, 35), 7);
        LinearLayout card = Ui.card(this, body);
        LinearLayout.LayoutParams cardParams = (LinearLayout.LayoutParams) card.getLayoutParams();
        cardParams.topMargin = Ui.dp(this, 20);
        card.setLayoutParams(cardParams);
        Ui.add(this, card, Ui.kicker(this, "СУММА СЧЁТА"), 0);
        Ui.add(this, card, Ui.headline(this, Money.format(bill.total()), 42), 12);
        View progress = Ui.progress(this, bill.paid(), bill.total());
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, Ui.dp(this, 4));
        progressParams.topMargin = Ui.dp(this, 26);
        card.addView(progress, progressParams);
        Ui.add(this, card, Ui.text(this, "Оплачено   " + Money.format(bill.paid()), 15, false), 15);
        Ui.add(this, card, Ui.text(this, "Осталось   " + Money.format(bill.total() - bill.paid()), 15, false), 4);

        LinearLayout statusRow = Ui.row(this);
        Ui.addRow(this, statusRow,
                Ui.label(this, DemoSession.active(this) ? "Демо · на устройстве" : "На устройстве"), 1);
        Ui.addRow(this, statusRow, Ui.link(this, "Обновить", () -> load(true)), 0);
        Ui.add(this, body, statusRow, 12);
        Ui.add(this, body, Ui.text(this, "Участники", 18, true), 20);
        Ui.add(this, body, Ui.label(this, "Нажмите на участника, чтобы отметить оплату."), 6);

        for (int i = 0; i < bill.people.size(); i++) {
            int index = i;
            BillDocument.Person person = bill.people.get(i);
            LinearLayout line = Ui.row(this);
            line.setMinimumHeight(Ui.dp(this, 86));
            line.setOnClickListener(v -> confirmMark(index));
            TextView avatar = Ui.avatar(this, person.name);
            line.addView(avatar, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));
            LinearLayout details = Ui.column(this);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
            p.leftMargin = Ui.dp(this, 12);
            line.addView(details, p);
            Ui.add(this, details, Ui.text(this, person.name, 16, true), 0);
            TextView status = Ui.label(this, person.paid ? "Оплачено" : "Ожидает оплаты");
            status.setTextSize(13);
            if (person.paid) status.setTextColor(Ui.accent(this));
            Ui.add(this, details, status, 6);
            LinearLayout amount = Ui.column(this);
            amount.setGravity(Gravity.RIGHT);
            Ui.addRow(this, amount, Ui.text(this, Money.format(person.amount), 15, true), 0);
            if (person.paid) {
                TextView check = Ui.text(this, "✓", 22, false);
                check.setTextColor(Ui.accent(this));
                check.setGravity(Gravity.RIGHT);
                Ui.add(this, amount, check, 4);
            }
            line.addView(amount, new LinearLayout.LayoutParams(-2, -2));
            Ui.add(this, body, line, 6);
        }
    }

    private void confirmMark(int index) {
        if (updating) return;
        BillDocument.Person person = bill.people.get(index);
        new AlertDialog.Builder(this)
                .setTitle(person.paid ? "Отменить оплату?" : "Отметить оплату?")
                .setMessage(person.name + ": " + Money.format(person.amount))
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Подтвердить", (dialog, which) -> {
                    if (updating) return;
                    updating = true;
                    repository.mark(bill, index, !person.paid, (v, error) -> {
                        updating = false;
                        if (error != null) Ui.error(this, error);
                        else load(false);
                    });
                }).show();
    }

    private void share() {
        if (bill == null) { Ui.error(this, new IllegalStateException("Сначала загрузите счёт")); return; }
        StringBuilder text = new StringBuilder(bill.name + " · " + Money.format(bill.total()) + "\n");
        for (BillDocument.Person person : bill.people)
            text.append(person.name).append(": ").append(Money.format(person.amount))
                    .append(person.paid ? " ✓" : "").append('\n');
        Intent share = new Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, text.toString());
        startActivity(Intent.createChooser(share, "Отправить расчёт"));
    }
}

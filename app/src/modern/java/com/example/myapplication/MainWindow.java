package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public final class MainWindow extends AppCompatActivity {
    private BillDocument bill;
    private BillRepository repository;
    private LinearLayout body;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        repository = new BillRepository(this);
        body = Ui.scroll(this, Ui.page(this));
        repository.list(false, (items, error) -> {
            if (items == null) { Ui.error(this, error); return; }
            for (BillDocument item : items) if (item.id.equals(getIntent().getStringExtra("bill"))) {
                bill = item; render(); return;
            }
        });
    }

    private void render() {
        body.removeAllViews();
        Ui.add(this, body, Ui.label(this, "ДЕТАЛИ СЧЁТА"), 0);
        Ui.add(this, body, Ui.text(this, bill.name, 30, true), 10);
        LinearLayout summary = Ui.card(this, body);
        Ui.add(this, summary, Ui.label(this, "ОПЛАЧЕНО"), 0);
        Ui.add(this, summary, Ui.text(this, Money.format(bill.paid()) + " из " + Money.format(bill.total()), 20, true), 8);
        Ui.add(this, summary, Ui.label(this, "Осталось " + Money.format(bill.total() - bill.paid())), 6);
        Ui.add(this, body, Ui.text(this, "Участники", 22, true), 24);
        for (int i = 0; i < bill.people.size(); i++) {
            int index = i;
            BillDocument.Person person = bill.people.get(i);
            LinearLayout card = Ui.card(this, body);
            Ui.add(this, card, Ui.text(this, person.name + " · " + Money.format(person.amount), 17, true), 0);
            Ui.add(this, card, Ui.label(this, person.paid ? "Оплачено" : "Ожидает оплаты"), 6);
            Ui.add(this, card, Ui.secondary(this, person.paid ? "Отменить оплату" : "Отметить оплату", () -> {
                repository.mark(bill, index, !person.paid, (v, error) -> {
                    if (error != null) Ui.error(this, error);
                    else { person.paid = !person.paid; render(); }
                });
            }), 8);
        }
        Ui.add(this, body, Ui.secondary(this, "Поделиться расчётом", () -> {
            StringBuilder text = new StringBuilder(bill.name + " · " + Money.format(bill.total()) + "\n");
            for (BillDocument.Person person : bill.people)
                text.append(person.name).append(": ").append(Money.format(person.amount))
                        .append(person.paid ? " ✓" : "").append('\n');
            Intent share = new Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, text.toString());
            startActivity(Intent.createChooser(share, "Отправить расчёт"));
        }), 24);
    }
}

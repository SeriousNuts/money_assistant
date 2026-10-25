package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.FirebaseApp;

import java.util.ArrayList;
import java.util.List;

public final class StartMenu extends AppCompatActivity {
    private BillRepository repository;
    private LinearLayout cards, total;
    private List<BillDocument> bills = new ArrayList<>();
    private int filter;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        repository = new BillRepository(this);
        LinearLayout root = Ui.page(this), body = Ui.scroll(this, root);
        Ui.add(this, body, Ui.label(this, DemoSession.active(this) ? "ЛОКАЛЬНЫЙ ДЕМОРЕЖИМ" : "МОИ СЧЕТА"), 4);
        Ui.add(this, body, Ui.text(this, "Ваши счета", 32, true), 10);
        total = Ui.card(this, body);
        LinearLayout choices = Ui.card(this, body);
        Ui.add(this, choices, Ui.secondary(this, "Все", () -> { filter = 0; render(); }), 0);
        Ui.add(this, choices, Ui.secondary(this, "В процессе", () -> { filter = 1; render(); }), 6);
        Ui.add(this, choices, Ui.secondary(this, "Закрытые", () -> { filter = 2; render(); }), 6);
        cards = Ui.column(this);
        Ui.add(this, body, cards, 12);
        Ui.add(this, root, Ui.button(this, "＋  Создать счёт", () ->
                startActivity(new Intent(this, AfterContact.class))), 8);
        Ui.add(this, root, Ui.secondary(this, "Выйти", () -> {
            DemoSession.set(this, false);
            if (!FirebaseApp.getApps(this).isEmpty()) FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, MainActivity.class)); finish();
        }), 4);
    }

    @Override protected void onResume() { super.onResume(); load(false); }

    private void load(boolean force) {
        repository.list(force, (found, error) -> {
            if (found != null) { bills = found; render(); }
            else Ui.error(this, error);
        });
    }

    private void render() {
        total.removeAllViews(); cards.removeAllViews();
        long owed = 0;
        for (BillDocument bill : bills) owed += bill.total() - bill.paid();
        Ui.add(this, total, Ui.label(this, "ОСТАЛОСЬ СОБРАТЬ"), 0);
        Ui.add(this, total, Ui.text(this, Money.format(owed), 27, true), 8);
        Ui.add(this, cards, Ui.secondary(this, "↻  Обновить", () -> load(true)), 0);
        for (BillDocument bill : bills) {
            boolean closed = bill.paid() == bill.total();
            if ((filter == 1 && closed) || (filter == 2 && !closed)) continue;
            LinearLayout card = Ui.card(this, cards);
            Ui.add(this, card, Ui.text(this, bill.name, 20, true), 0);
            Ui.add(this, card, Ui.label(this, Money.format(bill.paid()) + " из " + Money.format(bill.total())
                    + " · " + (closed ? "Закрыт" : "В процессе")), 6);
            Ui.add(this, card, Ui.secondary(this, "Открыть", () -> {
                Intent intent = new Intent(this, MainWindow.class);
                intent.putExtra("bill", bill.id);
                startActivity(intent);
            }), 12);
            Ui.add(this, card, Ui.secondary(this, "Удалить…", () -> new AlertDialog.Builder(this)
                    .setMessage("Удалить счёт «" + bill.name + "»?")
                    .setNegativeButton("Отмена", null).setPositiveButton("Удалить", (d, w) ->
                            repository.delete(bill, (v, error) -> {
                                if (error != null) Ui.error(this, error); else load(true);
                            })).show()), 6);
        }
        if (bills.isEmpty()) Ui.add(this, cards, Ui.label(this, "Здесь пока нет счетов. Создайте первый!"), 25);
    }
}

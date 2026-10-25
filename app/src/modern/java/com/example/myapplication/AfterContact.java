package com.example.myapplication;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public final class AfterContact extends AppCompatActivity {
    private static final int PICK_CONTACT = 100;
    private static final int EDIT_RECEIPT = 101;
    private BillDocument bill = new BillDocument();
    private BillRepository repository;
    private LinearLayout people;
    private EditText title, total;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        repository = new BillRepository(this);
        if (state != null) try {
            bill = BillDocument.fromJson(new JSONObject(state.getString("draft")));
        } catch (JSONException | NullPointerException ignored) { }
        LinearLayout root = Ui.page(this), body = Ui.scroll(this, root);
        Ui.add(this, body, Ui.label(this, "НОВЫЙ СЧЁТ"), 2);
        Ui.add(this, body, Ui.text(this, "Кто за что платит?", 29, true), 8);
        title = Ui.input(this, "Название счёта");
        title.setText(bill.name);
        Ui.add(this, body, title, 18);
        total = Ui.input(this, "Общая сумма, ₽");
        total.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        Ui.add(this, body, total, 12);
        Ui.add(this, body, Ui.secondary(this, "Разделить поровну", this::splitEvenly), 16);
        Ui.add(this, body, Ui.secondary(this, "＋  Добавить участника", () -> addPerson("", "")), 8);
        Ui.add(this, body, Ui.secondary(this, "Из контактов", () -> {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            startActivityForResult(intent, PICK_CONTACT);
        }), 8);
        Ui.add(this, body, Ui.secondary(this, "Фото и разметка чека", () -> {
            try {
                captureDraft();
                if (bill.people.isEmpty()) throw new IllegalArgumentException("Сначала добавьте участников");
                Intent intent = new Intent(this, ReceiptActivity.class);
                intent.putExtra("draft", bill.toJson().toString());
                startActivityForResult(intent, EDIT_RECEIPT);
            } catch (Exception e) { Ui.error(this, e); }
        }), 8);
        people = Ui.column(this);
        Ui.add(this, body, people, 16);
        Ui.add(this, root, Ui.button(this, "Сохранить счёт", this::save), 8);
        render();
    }

    private void captureDraft() {
        bill.name = title.getText().toString().trim();
        for (int i = 0; i < bill.people.size(); i++) {
            EditText amount = people.findViewWithTag("amount-" + i);
            if (amount != null && !amount.getText().toString().trim().isEmpty())
                bill.people.get(i).amount = Money.parse(amount.getText().toString());
        }
    }

    private void render() {
        people.removeAllViews();
        Ui.add(this, people, Ui.text(this, "Участники · " + bill.people.size(), 21, true), 4);
        for (int i = 0; i < bill.people.size(); i++) {
            final int index = i;
            BillDocument.Person person = bill.people.get(i);
            LinearLayout card = Ui.card(this, people);
            Ui.add(this, card, Ui.text(this, person.name, 17, true), 0);
            EditText amount = Ui.input(this, "Доля, ₽");
            amount.setTag("amount-" + index);
            amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            if (person.amount > 0) amount.setText(Money.format(person.amount).replace(" ₽", ""));
            Ui.add(this, card, amount, 6);
            Ui.add(this, card, Ui.secondary(this, "Убрать", () -> {
                try { captureDraft(); } catch (IllegalArgumentException e) { Ui.error(this, e); return; }
                bill.people.remove(index);
                bill.lines.clear(); // Indices in receipt markup no longer refer to these participants.
                render();
            }), 8);
        }
        if (!bill.lines.isEmpty()) Ui.add(this, people,
                Ui.label(this, "Позиции чека: " + bill.lines.size() + " · доли рассчитаны автоматически"), 12);
    }

    private void addPerson(String name, String phone) {
        if (!name.isEmpty()) {
            try { captureDraft(); } catch (IllegalArgumentException e) { Ui.error(this, e); return; }
            bill.people.add(new BillDocument.Person(name, phone, 0, false));
            bill.lines.clear();
            if (!total.getText().toString().trim().isEmpty()) {
                try {
                    List<Long> shares = Money.split(Money.parse(total.getText().toString()), bill.people.size());
                    for (int i = 0; i < shares.size(); i++) bill.people.get(i).amount = shares.get(i);
                } catch (IllegalArgumentException e) { Ui.error(this, e); }
            }
            render(); return;
        }
        EditText input = Ui.input(this, "Имя участника");
        new AlertDialog.Builder(this).setTitle("Добавить участника").setView(input)
                .setNegativeButton("Отмена", null).setPositiveButton("Добавить", (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    if (value.isEmpty()) Ui.error(this, new IllegalArgumentException("Введите имя"));
                    else addPerson(value, "");
                }).show();
    }

    private void splitEvenly() {
        try {
            captureDraft();
            if (bill.people.isEmpty()) throw new IllegalArgumentException("Сначала добавьте участников");
            long amount = Money.parse(total.getText().toString());
            List<Long> shares = Money.split(amount, bill.people.size());
            for (int i = 0; i < shares.size(); i++) bill.people.get(i).amount = shares.get(i);
            bill.lines.clear();
            render();
        } catch (RuntimeException e) { Ui.error(this, e); }
    }

    private void save() {
        try {
            captureDraft();
            if (bill.name.isEmpty() || bill.people.isEmpty() || bill.people.size() > 450)
                throw new IllegalArgumentException("Введите название и добавьте участников");
            for (int i = 0; i < bill.people.size(); i++) {
                EditText field = people.findViewWithTag("amount-" + i);
                bill.people.get(i).amount = Money.parse(field.getText().toString());
            }
            if (bill.total() == 0) throw new IllegalArgumentException("Сумма должна быть больше нуля");
            if (!total.getText().toString().trim().isEmpty() && bill.total() != Money.parse(total.getText().toString()))
                throw new IllegalArgumentException("Сумма долей не совпадает с общей суммой");
            repository.save(bill, (v, error) -> { if (error != null) Ui.error(this, error); else finish(); });
        } catch (RuntimeException e) { Ui.error(this, e); }
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        try { captureDraft(); state.putString("draft", bill.toJson().toString()); }
        catch (Exception ignored) { }
        super.onSaveInstanceState(state);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null) return;
        if (request == EDIT_RECEIPT) {
            try {
                bill = BillDocument.fromJson(new JSONObject(data.getStringExtra("draft")));
                List<Long> shares = ReceiptCalculator.allocate(bill.people.size(), bill.lines);
                for (int i = 0; i < shares.size(); i++) bill.people.get(i).amount = shares.get(i);
                total.setText(Money.format(bill.total()).replace(" ₽", ""));
                render();
            } catch (Exception e) { Ui.error(this, e); }
        } else if (request == PICK_CONTACT && data.getData() != null) {
            try (Cursor cursor = getContentResolver().query(data.getData(),
                    new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) addPerson(cursor.getString(0), cursor.getString(1));
            } catch (Exception e) { Ui.error(this, e); }
        }
    }
}

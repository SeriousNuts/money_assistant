package com.example.myapplication;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

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
    private TextView summary, displayedTotal;
    private CheckBox shareAfterSave;
    private boolean saving;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        repository = new BillRepository(this);
        if (state != null) try {
            bill = BillDocument.fromJson(new JSONObject(state.getString("draft")));
        } catch (JSONException | NullPointerException ignored) { }

        LinearLayout root = Ui.page(this);
        Ui.toolbar(this, root, "Новый счёт", true, null);
        LinearLayout body = Ui.scroll(this, root);
        title = Ui.input(this, "Название счёта");
        title.setText(bill.name);
        Ui.add(this, body, title, 8);

        LinearLayout amountCard = Ui.card(this, body);
        LinearLayout.LayoutParams amountParams = (LinearLayout.LayoutParams) amountCard.getLayoutParams();
        amountParams.topMargin = Ui.dp(this, 20);
        amountCard.setLayoutParams(amountParams);
        Ui.add(this, amountCard, Ui.kicker(this, "СУММА СЧЁТА"), 0);
        total = Ui.input(this, "0,00");
        total.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        total.setTypeface(android.graphics.Typeface.create("serif", android.graphics.Typeface.NORMAL));
        total.setTextSize(36);
        total.setPadding(0, 0, 0, 0);
        total.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        total.setMinHeight(Ui.dp(this, 58));
        total.setMaxWidth(Ui.dp(this, 240));
        if (state != null) total.setText(state.getString("enteredTotal", ""));
        LinearLayout moneyRow = Ui.row(this);
        moneyRow.addView(total, new LinearLayout.LayoutParams(-2, Ui.dp(this, 58)));
        LinearLayout.LayoutParams currencyParams = new LinearLayout.LayoutParams(-2, -2);
        currencyParams.leftMargin = Ui.dp(this, 6);
        moneyRow.addView(Ui.headline(this, "₽", 36), currencyParams);
        Ui.add(this, amountCard, moneyRow, 8);
        Ui.add(this, amountCard, Ui.secondary(this, "↳  Разделить поровну", this::splitEvenly), 12);

        summary = Ui.label(this, "");
        Ui.add(this, body, summary, 12);
        total.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateSummary(); }
            @Override public void afterTextChanged(Editable s) { }
        });

        Ui.add(this, body, Ui.text(this, "Участники", 18, true), 21);
        LinearLayout actions = Ui.row(this);
        TextView add = Ui.link(this, "＋  Добавить", () -> addPerson("", ""));
        Ui.addRow(this, actions, add, 1);
        TextView contacts = Ui.link(this, "Из контактов", () -> {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            startActivityForResult(intent, PICK_CONTACT);
        });
        contacts.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        Ui.addRow(this, actions, contacts, 0);
        Ui.add(this, body, actions, 2);

        people = Ui.column(this);
        Ui.add(this, body, people, 7);
        Ui.add(this, body, Ui.secondary(this, "▧  Фото и разметка чека", () -> {
            try {
                captureDraft();
                if (bill.people.isEmpty()) throw new IllegalArgumentException("Сначала добавьте участников");
                Intent intent = new Intent(this, ReceiptActivity.class);
                intent.putExtra("draft", bill.toJson().toString());
                startActivityForResult(intent, EDIT_RECEIPT);
            } catch (Exception e) { Ui.error(this, e); }
        }), 16);

        LinearLayout footer = Ui.footer(this, root);
        shareAfterSave = new CheckBox(this);
        shareAfterSave.setText("Поделиться после сохранения");
        shareAfterSave.setTextColor(Ui.subdued(this));
        shareAfterSave.setTextSize(14);
        if (state != null) shareAfterSave.setChecked(state.getBoolean("share", false));
        Ui.add(this, footer, shareAfterSave, 0);
        Ui.add(this, footer, Ui.button(this, "Сохранить счёт", this::save), 6);
        render();
    }

    private void captureDraft() { bill.name = title.getText().toString().trim(); }

    private void updateSummary() {
        if (summary == null || total == null) return;
        long shares = bill.total();
        String note = "Участников: " + bill.people.size() + " · Доли: " + Money.format(shares);
        if (!total.getText().toString().trim().isEmpty()) {
            try {
                long expected = Money.parse(total.getText().toString());
                note += "\nСчёт: " + Money.format(expected);
                if (shares > 0 && shares != expected) note += " · Проверьте сумму долей";
            } catch (IllegalArgumentException ignored) { note += "\nУкажите сумму в рублях"; }
        }
        summary.setText(note);
    }

    private void render() {
        people.removeAllViews();
        for (int i = 0; i < bill.people.size(); i++) {
            int index = i;
            BillDocument.Person person = bill.people.get(i);
            LinearLayout row = Ui.row(this);
            row.setMinimumHeight(Ui.dp(this, 76));
            row.setOnClickListener(v -> editPerson(index));
            TextView avatar = Ui.avatar(this, person.name);
            row.addView(avatar, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));
            LinearLayout text = Ui.column(this);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, -2, 1);
            textParams.leftMargin = Ui.dp(this, 12);
            row.addView(text, textParams);
            Ui.add(this, text, Ui.text(this, person.name, 16, true), 0);
            Ui.add(this, text, Ui.text(this, Money.format(person.amount), 14, false), 1);
            TextView edit = Ui.text(this, "✎", 23, false);
            edit.setGravity(Gravity.CENTER);
            edit.setContentDescription("Изменить " + person.name);
            row.addView(edit, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 48)));
            edit.setOnClickListener(v -> editPerson(index));
            Ui.add(this, people, row, 0);
        }
        if (bill.people.isEmpty()) Ui.add(this, people,
                Ui.label(this, "Добавьте себя и друзей, чтобы разделить счёт."), 8);
        if (!bill.lines.isEmpty()) Ui.add(this, people,
                Ui.label(this, "Позиции чека: " + bill.lines.size() + " · доли рассчитаны автоматически"), 10);
        updateSummary();
    }

    private void editPerson(int index) {
        BillDocument.Person person = bill.people.get(index);
        LinearLayout form = Ui.column(this);
        form.setPadding(Ui.dp(this, 20), Ui.dp(this, 8), Ui.dp(this, 20), 0);
        EditText name = Ui.input(this, "Имя участника");
        name.setText(person.name);
        Ui.add(this, form, name, 0);
        EditText amount = Ui.input(this, "Доля, ₽");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amount.setText(Money.format(person.amount).replace(" ₽", ""));
        Ui.add(this, form, amount, 10);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Участник")
                .setView(form).setNegativeButton("Отмена", null)
                .setNeutralButton("Удалить", (d, w) -> {
                    bill.people.remove(index);
                    bill.lines.clear(); // Position assignee indices no longer match.
                    if (!total.getText().toString().trim().isEmpty() && !bill.people.isEmpty()) splitEvenly();
                    else render();
                }).setPositiveButton("Сохранить", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                String renamed = name.getText().toString().trim();
                if (renamed.isEmpty()) throw new IllegalArgumentException("Введите имя");
                long value = Money.parse(amount.getText().toString());
                person.name = renamed;
                if (value != person.amount) bill.lines.clear();
                person.amount = value;
                dialog.dismiss(); render();
            } catch (IllegalArgumentException e) { Ui.error(this, e); }
        }));
        dialog.show();
    }

    private void addPerson(String name, String phone) {
        if (!name.isEmpty()) {
            if (bill.people.size() >= 450) { Ui.error(this, new IllegalArgumentException("Не более 450 участников")); return; }
            captureDraft();
            bill.people.add(new BillDocument.Person(name, phone, 0, false));
            bill.lines.clear();
            if (!total.getText().toString().trim().isEmpty()) splitEvenly();
            else render();
            return;
        }
        EditText input = Ui.input(this, "Имя участника");
        LinearLayout wrapper = Ui.column(this);
        wrapper.setPadding(Ui.dp(this, 20), Ui.dp(this, 12), Ui.dp(this, 20), 0);
        Ui.add(this, wrapper, input, 0);
        new AlertDialog.Builder(this).setTitle("Добавить друга").setView(wrapper)
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
        if (saving) return;
        try {
            captureDraft();
            if (bill.name.isEmpty() || bill.people.isEmpty() || bill.people.size() > 450)
                throw new IllegalArgumentException("Введите название и добавьте участников");
            if (bill.total() == 0) throw new IllegalArgumentException("Сумма должна быть больше нуля");
            if (!total.getText().toString().trim().isEmpty() && bill.total() != Money.parse(total.getText().toString()))
                throw new IllegalArgumentException("Сумма долей не совпадает с общей суммой");
            saving = true;
            repository.save(bill, (v, error) -> {
                saving = false;
                if (error != null) { Ui.error(this, error); return; }
                if (shareAfterSave.isChecked()) share();
                finish();
            });
        } catch (RuntimeException e) { Ui.error(this, e); }
    }

    private void share() {
        StringBuilder text = new StringBuilder(bill.name + " · " + Money.format(bill.total()));
        for (BillDocument.Person person : bill.people)
            text.append('\n').append(person.name).append(": ").append(Money.format(person.amount));
        startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, text.toString()), "Поделиться счётом"));
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        try { captureDraft(); state.putString("draft", bill.toJson().toString()); }
        catch (Exception ignored) { }
        state.putString("enteredTotal", total.getText().toString());
        state.putBoolean("share", shareAfterSave.isChecked());
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

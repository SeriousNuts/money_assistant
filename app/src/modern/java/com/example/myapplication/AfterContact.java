package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;

public final class AfterContact extends AppCompatActivity {
    static final int PICK_CONTACT = 100;
    private static final int EDIT_RECEIPT = 101;
    private BillDocument bill = new BillDocument();
    private BillRepository repository;
    private BillDraftStore draftStore;
    private LinearLayout people;
    private EditText title, total;
    private TextView summary;
    private CheckBox shareAfterSave;
    private boolean saving;
    private boolean editing;
    private boolean saved;
    private String stagedPhoto;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        repository = new BillRepository(this);
        draftStore = new BillDraftStore(this);
        editing = getIntent().hasExtra("editDraft");
        if (state != null) {
            saved = state.getBoolean("saved");
            stagedPhoto = state.getString("stagedPhoto");
        }
        if (state != null || editing) try {
            bill = BillDocument.fromJson(new JSONObject(state != null
                    ? state.getString("draft") : getIntent().getStringExtra("editDraft")));
        } catch (JSONException | NullPointerException e) { Ui.error(this, e); finish(); return; }
        BillDraftStore.Draft restoredDraft = null;
        if (state == null && !editing && getIntent().hasExtra("draftId")) try {
            restoredDraft = draftStore.get(getIntent().getStringExtra("draftId"));
            if (restoredDraft == null) throw new IllegalStateException("Черновик не найден");
            bill = restoredDraft.bill;
            stagedPhoto = restoredDraft.stagedPhoto;
        } catch (Exception e) { Ui.error(this, e); finish(); return; }

        LinearLayout root = Ui.page(this);
        Ui.toolbar(this, root, editing ? "Редактировать счёт" : "Новый счёт", true, null);
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
        else if (restoredDraft != null) total.setText(restoredDraft.enteredTotal);
        else if (editing) total.setText(Money.format(bill.total()).replace(" ₽", ""));
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
            if (bill.people.size() >= 450) {
                Ui.error(this, new IllegalArgumentException("Не более 450 участников")); return;
            }
            startActivityForResult(new Intent(this, ContactSelectionActivity.class)
                    .putExtra("available", 450 - bill.people.size()), PICK_CONTACT);
        });
        contacts.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        Ui.addRow(this, actions, contacts, 0);
        Ui.add(this, body, actions, 2);

        people = Ui.column(this);
        Ui.add(this, body, people, 7);
        if (editing) Ui.add(this, body, Ui.label(this,
                "Чеки сохраняются при изменении счёта. Для правки фото откройте экран чеков после сохранения."), 16);
        else Ui.add(this, body, Ui.secondary(this, "▧  Фото и разметка чека", () -> {
            try {
                captureDraft();
                if (bill.people.isEmpty()) throw new IllegalArgumentException("Сначала добавьте участников");
                Intent intent = new Intent(this, ReceiptActivity.class);
                intent.putExtra("draft", bill.toJson().toString());
                intent.putExtra("draftPhoto", stagedPhoto);
                intent.putExtra("preserveOnBack", true);
                startActivityForResult(intent, EDIT_RECEIPT);
            } catch (Exception e) { Ui.error(this, e); }
        }), 16);

        LinearLayout footer = Ui.footer(this, root);
        shareAfterSave = new CheckBox(this);
        shareAfterSave.setText("Поделиться после сохранения");
        shareAfterSave.setTextColor(Ui.subdued(this));
        shareAfterSave.setTextSize(14);
        if (state != null) shareAfterSave.setChecked(state.getBoolean("share", false));
        else if (restoredDraft != null) shareAfterSave.setChecked(restoredDraft.share);
        Ui.add(this, footer, shareAfterSave, 0);
        Ui.add(this, footer, Ui.button(this, editing ? "Сохранить изменения" : "Сохранить счёт", this::save), 6);
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
        if (bill.hasReceiptLines()) Ui.add(this, people,
                Ui.label(this, "Чеков: " + bill.receipts.size() + " · доли: ручные + позиции чеков"), 10);
        updateSummary();
    }

    private void editPerson(int index) {
        BillDocument.Person person = bill.people.get(index);
        LinearLayout form = Ui.column(this);
        form.setPadding(Ui.dp(this, 20), Ui.dp(this, 8), Ui.dp(this, 20), 0);
        EditText name = Ui.input(this, "Имя участника");
        name.setText(person.name);
        Ui.add(this, form, name, 0);
        EditText amount = Ui.input(this, bill.hasReceiptLines() ? "Ручная доля, ₽" : "Доля, ₽");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amount.setText(Money.format(person.manualAmount).replace(" ₽", ""));
        Ui.add(this, form, amount, 10);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Участник")
                .setView(form).setNegativeButton("Отмена", null)
                .setNeutralButton("Удалить", (d, w) -> {
                    if (editing || bill.hasReceiptLines()) {
                        Ui.error(this, new IllegalStateException(editing
                                ? "Удаление участника сохранённого счёта недоступно: оно может изменить назначения позиций и оплаты"
                                : "Сначала удалите позиции чеков: они назначены участникам"));
                        return;
                    }
                    bill.people.remove(index);
                    if (!total.getText().toString().trim().isEmpty() && !bill.people.isEmpty()) splitEvenly();
                    else render();
                }).setPositiveButton("Сохранить", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                String renamed = name.getText().toString().trim();
                if (renamed.isEmpty()) throw new IllegalArgumentException("Введите имя");
                long value = Money.parse(amount.getText().toString());
                String previousName = person.name;
                long previousManual = person.manualAmount;
                try {
                    person.name = renamed;
                    person.manualAmount = value;
                    bill.recalculateAmounts();
                } catch (RuntimeException e) {
                    person.name = previousName;
                    person.manualAmount = previousManual;
                    throw e;
                }
                if (editing || bill.hasReceiptLines()) total.setText(Money.format(bill.total()).replace(" ₽", ""));
                dialog.dismiss(); render();
            } catch (RuntimeException e) { Ui.error(this, e); }
        }));
        dialog.show();
    }

    private void addPerson(String name, String phone) {
        if (!name.isEmpty()) {
            if (bill.people.size() >= 450) { Ui.error(this, new IllegalArgumentException("Не более 450 участников")); return; }
            if (editing) { promptDebt(new BillDocument.Person(name, phone, 0, false), null); return; }
            captureDraft();
            bill.people.add(new BillDocument.Person(name, phone, 0, false));
            if (!total.getText().toString().trim().isEmpty() && !bill.hasReceiptLines()) splitEvenly();
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

    private void promptDebt(BillDocument.Person person, Runnable after) {
        EditText input = Ui.input(this, "Сколько должен, ₽");
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        LinearLayout form = Ui.column(this);
        form.setPadding(Ui.dp(this, 20), Ui.dp(this, 12), Ui.dp(this, 20), 0);
        Ui.add(this, form, Ui.label(this, person.name), 0);
        Ui.add(this, form, input, 8);
        Runnable next = () -> { if (after != null) new android.os.Handler(getMainLooper()).post(after); };
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Новый участник")
                .setView(form).setNegativeButton("Отмена", (d, w) -> next.run())
                .setNeutralButton("Добавить сейчас, указать позже", (d, w) -> {
                    addWithDebt(person, 0);
                    next.run();
                }).setPositiveButton("Добавить", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                long amount = Money.parse(input.getText().toString());
                addWithDebt(person, amount);
                dialog.dismiss();
                next.run();
            } catch (RuntimeException e) { Ui.error(this, e); }
        }));
        dialog.show();
    }

    private void addWithDebt(BillDocument.Person person, long amount) {
        if (bill.people.size() >= 450) { Ui.error(this, new IllegalArgumentException("Не более 450 участников")); return; }
        person.manualAmount = amount;
        person.amount = amount;
        bill.people.add(person);
        try { bill.recalculateAmounts(); bill.total(); }
        catch (RuntimeException e) { bill.people.remove(person); throw e; }
        total.setText(Money.format(bill.total()).replace(" ₽", ""));
        render();
    }

    private void promptNextContact(List<BillDocument.Person> additions, int index) {
        if (index < additions.size())
            promptDebt(additions.get(index), () -> promptNextContact(additions, index + 1));
    }

    private void splitEvenly() {
        try {
            captureDraft();
            if (bill.people.isEmpty()) throw new IllegalArgumentException("Сначала добавьте участников");
            long amount = Money.parse(total.getText().toString());
            List<Long> shares = Money.split(amount, bill.people.size());
            if (bill.hasReceiptLines()) throw new IllegalStateException("Нельзя разделить общую сумму при наличии позиций чеков: измените ручные доли участников");
            for (int i = 0; i < shares.size(); i++) {
                bill.people.get(i).manualAmount = shares.get(i);
                bill.people.get(i).amount = shares.get(i);
            }
            render();
        } catch (RuntimeException e) { Ui.error(this, e); }
    }

    private void save() {
        if (saving) return;
        try {
            if (saved) { commitPhoto(); return; }
            captureDraft();
            if (bill.name.isEmpty() || bill.people.isEmpty() || bill.people.size() > 450)
                throw new IllegalArgumentException("Введите название и добавьте участников");
            if (!editing && bill.total() == 0) throw new IllegalArgumentException("Сумма должна быть больше нуля");
            if (!total.getText().toString().trim().isEmpty() && bill.total() != Money.parse(total.getText().toString()))
                throw new IllegalArgumentException("Сумма долей не совпадает с общей суммой");
            saving = true;
            BillRepository.Result<Void> done = (v, error) -> {
                saving = false;
                if (error != null) { Ui.error(this, error); return; }
                saved = true;
                try { commitPhoto(); } catch (Exception e) { Ui.error(this, e); }
            };
            if (editing) repository.update(bill, done);
            else repository.save(bill, done);
        } catch (Exception e) { Ui.error(this, e); }
    }

    private void commitPhoto() throws Exception {
        if (stagedPhoto != null) {
            ReceiptPhotos.commit(this, bill.primaryReceipt(), stagedPhoto);
            stagedPhoto = null;
        }
        if (!editing) draftStore.remove(bill.id);
        if (shareAfterSave.isChecked()) share();
        finish();
    }

    private void saveDraft() throws Exception {
        captureDraft();
        draftStore.save(new BillDraftStore.Draft(bill, total.getText().toString(),
                shareAfterSave.isChecked(), stagedPhoto));
    }

    @Override protected void onPause() {
        if (!editing && !saved && title != null && shareAfterSave != null) try {
            saveDraft();
        } catch (Exception e) { Ui.error(this, e); }
        super.onPause();
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
        state.putBoolean("saved", saved);
        state.putString("stagedPhoto", stagedPhoto);
        super.onSaveInstanceState(state);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null) return;
        if (request == EDIT_RECEIPT) {
            try {
                String oldStage = stagedPhoto;
                bill = BillDocument.fromJson(new JSONObject(data.getStringExtra("draft")));
                stagedPhoto = data.getStringExtra("stagedPhoto");
                total.setText(Money.format(bill.total()).replace(" ₽", ""));
                render();
                saveDraft();
                if (oldStage != null && !oldStage.equals(stagedPhoto)) try {
                    java.io.File old = new java.io.File(oldStage);
                    if (ReceiptPhotos.isStage(this, bill.primaryReceipt(), old) && old.exists() && !old.delete())
                        android.util.Log.w("AfterContact", "Could not remove superseded draft photo");
                } catch (Exception e) { android.util.Log.w("AfterContact", "Could not clean draft photo", e); }
            } catch (Exception e) { Ui.error(this, e); }
        } else if (request == PICK_CONTACT) {
            ArrayList<String> names = data.getStringArrayListExtra(ContactSelectionActivity.NAMES);
            ArrayList<String> phones = data.getStringArrayListExtra(ContactSelectionActivity.PHONES);
            if (names == null || phones == null || names.size() != phones.size()) return;
            ArrayList<BillDocument.Person> additions = new ArrayList<>();
            for (int i = 0; i < names.size(); i++) {
                if (names.get(i) == null || names.get(i).trim().isEmpty()) continue;
                String name = names.get(i).trim();
                String phone = phones.get(i) == null ? "" : phones.get(i);
                boolean exists = false;
                for (BillDocument.Person person : bill.people)
                    if (person.name.equals(name) && person.phone.equals(phone)) { exists = true; break; }
                for (BillDocument.Person person : additions)
                    if (person.name.equals(name) && person.phone.equals(phone)) { exists = true; break; }
                if (!exists) additions.add(new BillDocument.Person(name, phone, 0, false));
            }
            if (additions.isEmpty()) return;
            if (bill.people.size() + additions.size() > 450) {
                Ui.error(this, new IllegalArgumentException("Не более 450 участников")); return;
            }
            captureDraft();
            if (editing) promptNextContact(additions, 0);
            else {
                bill.people.addAll(additions);
                if (!total.getText().toString().trim().isEmpty() && !bill.hasReceiptLines()) splitEvenly();
                else render();
            }
        }
    }
}

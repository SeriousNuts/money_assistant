package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Multi-select contacts locally; the editor receives only the chosen names and phone numbers. */
public final class ContactSelectionActivity extends AppCompatActivity {
    static final String NAMES = "selectedNames", PHONES = "selectedPhones";
    private static final int READ_CONTACTS = 1;
    private final List<Contact> contacts = new ArrayList<>(), visible = new ArrayList<>();
    private final LinkedHashMap<Long, Contact> selected = new LinkedHashMap<>();
    private final Set<Long> restored = new HashSet<>();
    private ContactAdapter adapter;
    private EditText search;
    private TextView message;
    private android.widget.Button done;
    private int available;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        available = Math.max(0, Math.min(450, getIntent().getIntExtra("available", 450)));
        if (state != null && state.getLongArray("selectedIds") != null)
            for (long id : state.getLongArray("selectedIds")) restored.add(id);

        LinearLayout root = Ui.page(this);
        Ui.toolbar(this, root, "Из контактов", true, null);
        LinearLayout column = Ui.column(this);
        column.setPadding(Ui.dp(this, 24), Ui.dp(this, 8), Ui.dp(this, 24), 0);
        root.addView(column, new LinearLayout.LayoutParams(-1, -2));
        Ui.add(this, column, Ui.headline(this, "Выберите участников", 30), 8);
        Ui.add(this, column, Ui.label(this, "Отметьте нескольких людей и нажмите «Добавить»."), 8);
        search = Ui.input(this, "Поиск по имени или номеру");
        Ui.add(this, column, search, 16);
        message = Ui.label(this, "Загружаем контакты…");
        Ui.add(this, column, message, 12);
        message.setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
                requestContacts();
        });

        ListView list = new ListView(this);
        list.setDividerHeight(0);
        list.setBackgroundColor(Ui.background(this));
        adapter = new ContactAdapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> toggle(visible.get(position)));
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(-1, 0, 1);
        listParams.leftMargin = Ui.dp(this, 16);
        listParams.rightMargin = Ui.dp(this, 16);
        root.addView(list, listParams);

        done = Ui.button(this, "Добавить (0)", this::complete);
        Ui.add(this, Ui.footer(this, root), done, 0);
        updateButton();
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        if (state != null) search.setText(state.getString("search", ""));
        requestContacts();
    }

    private void requestContacts() {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
            loadContacts();
        else {
            message.setText("Разрешите доступ к контактам, чтобы выбрать участников. Нажмите здесь для повторного запроса.");
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, READ_CONTACTS);
        }
    }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request == READ_CONTACTS) {
            if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) loadContacts();
            else message.setText("Доступ не предоставлен. Нажмите здесь, чтобы попробовать снова.");
        }
    }

    private void loadContacts() {
        message.setText("Загружаем контакты…");
        new Thread(() -> {
            List<Contact> loaded = new ArrayList<>();
            try (Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                            ContactsContract.CommonDataKinds.Phone.NUMBER},
                    null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY + " ASC")) {
                if (cursor == null) throw new IllegalStateException("Не удалось прочитать контакты");
                Set<Long> seen = new HashSet<>();
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    String name = cursor.getString(1);
                    if (name != null && !name.trim().isEmpty() && seen.add(id))
                        loaded.add(new Contact(id, name.trim(), cursor.getString(2)));
                }
            } catch (Exception e) {
                runOnUiThread(() -> { if (!isFinishing() && !isDestroyed()) message.setText("Контакты недоступны: " + e.getMessage()); });
                return;
            }
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                contacts.clear(); contacts.addAll(loaded);
                for (Contact contact : contacts) if (restored.contains(contact.id)) selected.put(contact.id, contact);
                restored.clear();
                filter();
                updateButton();
            });
        }).start();
    }

    private void filter() {
        String query = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        visible.clear();
        for (Contact contact : contacts)
            if (contact.name.toLowerCase(Locale.ROOT).contains(query) || contact.phone.contains(query))
                visible.add(contact);
        adapter.notifyDataSetChanged();
        message.setText(contacts.isEmpty() ? "Нет контактов с номером телефона" :
                visible.isEmpty() ? "Ничего не найдено" : "Выбрано: " + selected.size());
    }

    private void toggle(Contact contact) {
        if (selected.containsKey(contact.id)) selected.remove(contact.id);
        else if (selected.size() < available) selected.put(contact.id, contact);
        else { Ui.error(this, new IllegalArgumentException("Можно выбрать не более " + available + " участников")); return; }
        adapter.notifyDataSetChanged();
        updateButton();
        message.setText("Выбрано: " + selected.size());
    }

    private void updateButton() {
        done.setText("Добавить (" + selected.size() + ")");
        done.setEnabled(!selected.isEmpty());
    }

    private void complete() {
        ArrayList<String> names = new ArrayList<>(), phones = new ArrayList<>();
        for (Contact contact : selected.values()) { names.add(contact.name); phones.add(contact.phone); }
        setResult(RESULT_OK, new Intent().putStringArrayListExtra(NAMES, names)
                .putStringArrayListExtra(PHONES, phones));
        finish();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        long[] ids = new long[selected.size() + restored.size()];
        int i = 0;
        for (Long id : selected.keySet()) ids[i++] = id;
        for (Long id : restored) ids[i++] = id;
        state.putLongArray("selectedIds", ids);
        state.putString("search", search.getText().toString());
        super.onSaveInstanceState(state);
    }

    private static final class Contact {
        final long id;
        final String name, phone;
        Contact(long id, String name, String phone) {
            this.id = id; this.name = name; this.phone = phone == null ? "" : phone;
        }
    }

    private final class ContactAdapter extends BaseAdapter {
        @Override public int getCount() { return visible.size(); }
        @Override public Contact getItem(int position) { return visible.get(position); }
        @Override public long getItemId(int position) { return getItem(position).id; }

        @Override public View getView(int position, View recycled, ViewGroup parent) {
            LinearLayout row;
            Row views;
            if (recycled == null) {
                row = Ui.row(ContactSelectionActivity.this);
                row.setMinimumHeight(Ui.dp(ContactSelectionActivity.this, 72));
                row.setPadding(Ui.dp(ContactSelectionActivity.this, 8), 0, Ui.dp(ContactSelectionActivity.this, 8), 0);
                TextView avatar = Ui.avatar(ContactSelectionActivity.this, "");
                row.addView(avatar, new LinearLayout.LayoutParams(Ui.dp(ContactSelectionActivity.this, 44), Ui.dp(ContactSelectionActivity.this, 44)));
                LinearLayout info = Ui.column(ContactSelectionActivity.this);
                LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, -2, 1);
                infoParams.leftMargin = Ui.dp(ContactSelectionActivity.this, 12);
                row.addView(info, infoParams);
                TextView name = Ui.text(ContactSelectionActivity.this, "", 16, true);
                TextView phone = Ui.label(ContactSelectionActivity.this, "");
                Ui.add(ContactSelectionActivity.this, info, name, 0);
                Ui.add(ContactSelectionActivity.this, info, phone, 3);
                CheckBox check = new CheckBox(ContactSelectionActivity.this);
                check.setFocusable(false);
                check.setClickable(false);
                check.setContentDescription("Выбрать контакт");
                row.addView(check, new LinearLayout.LayoutParams(Ui.dp(ContactSelectionActivity.this, 48), Ui.dp(ContactSelectionActivity.this, 48)));
                views = new Row(avatar, name, phone, check);
                row.setTag(views);
            } else {
                row = (LinearLayout) recycled;
                views = (Row) row.getTag();
            }
            Contact contact = getItem(position);
            views.avatar.setText(contact.name.substring(0, contact.name.offsetByCodePoints(0, 1)).toUpperCase(Locale.ROOT));
            views.name.setText(contact.name);
            views.phone.setText(contact.phone);
            views.check.setChecked(selected.containsKey(contact.id));
            row.setContentDescription(contact.name + ", " + contact.phone + (selected.containsKey(contact.id) ? ", выбран" : ", не выбран"));
            return row;
        }
    }

    private static final class Row {
        final TextView avatar, name, phone;
        final CheckBox check;
        Row(TextView avatar, TextView name, TextView phone, CheckBox check) {
            this.avatar = avatar; this.name = name; this.phone = phone; this.check = check;
        }
    }
}

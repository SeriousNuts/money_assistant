package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Multi-select contacts locally; the editor receives only the chosen names and phone numbers. */
public final class ContactSelectionActivity extends AppCompatActivity {
    static final String NAMES = "selectedNames", PHONES = "selectedPhones";
    private static final int READ_CONTACTS = 1;
    private final List<ContactDirectory.Contact> contacts = new ArrayList<>(), visible = new ArrayList<>();
    private final LinkedHashMap<Long, ContactDirectory.Contact> selected = new LinkedHashMap<>();
    private final Set<Long> restored = new HashSet<>();
    private ContactAdapter adapter;
    private EditText search;
    private TextView message;
    private android.widget.Button done;
    private int available;
    private int loadGeneration;
    private boolean permissionRequested;
    private boolean loading, loadedOnce;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        available = Math.max(0, Math.min(450, getIntent().getIntExtra("available", 450)));
        permissionRequested = state != null && state.getBoolean("permissionRequested");
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
        message.setOnClickListener(v -> requestContacts(true));

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
        requestContacts(false);
    }

    private void requestContacts(boolean userInitiated) {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
            loadContacts();
        else {
            loadGeneration++;
            if (permissionRequested && !shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                message.setText("Доступ к контактам отключён. Нажмите здесь, чтобы открыть настройки приложения и разрешить доступ.");
                if (userInitiated) startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName())));
            } else {
                message.setText("Разрешите доступ к контактам. После разрешения отметьте нужных людей в списке.");
                permissionRequested = true;
                requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, READ_CONTACTS);
            }
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (message != null && checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                && !loading && !loadedOnce) loadContacts();
    }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request == READ_CONTACTS) {
            if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) loadContacts();
            else message.setText("Доступ не предоставлен. Нажмите здесь, чтобы повторить запрос или открыть настройки приложения.");
        }
    }

    private void loadContacts() {
        message.setText("Загружаем контакты…");
        loading = true;
        final int generation = ++loadGeneration;
        new Thread(() -> {
            List<ContactDirectory.Contact> loaded;
            try {
                loaded = ContactDirectory.read(getContentResolver());
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (generation != loadGeneration || isFinishing() || isDestroyed()) return;
                    loading = false;
                    message.setText("Контакты недоступны: " + e.getMessage() + ". Нажмите здесь, чтобы повторить.");
                });
                return;
            }
            runOnUiThread(() -> {
                if (generation != loadGeneration || isFinishing() || isDestroyed()) return;
                loading = false;
                loadedOnce = true;
                contacts.clear(); contacts.addAll(loaded);
                restored.addAll(selected.keySet());
                selected.clear();
                for (ContactDirectory.Contact contact : contacts) if (restored.contains(contact.id)) selected.put(contact.id, contact);
                restored.clear();
                filter();
                updateButton();
            });
        }).start();
    }

    private void filter() {
        String query = search.getText().toString().trim();
        visible.clear();
        for (ContactDirectory.Contact contact : contacts)
            if (contact.matches(query))
                visible.add(contact);
        adapter.notifyDataSetChanged();
        if (!loadedOnce) return; // Keep permission, loading or error feedback visible while searching.
        message.setText(contacts.isEmpty() ? "Контактов нет. Проверьте, что они сохранены на устройстве или синхронизированы." :
                visible.isEmpty() ? "Ничего не найдено · выбрано: " + selected.size() :
                        "Контактов: " + contacts.size() + " · выбрано: " + selected.size());
    }

    private void toggle(ContactDirectory.Contact contact) {
        if (selected.containsKey(contact.id)) selected.remove(contact.id);
        else if (selected.size() < available) selected.put(contact.id, contact);
        else { Ui.error(this, new IllegalArgumentException("Можно выбрать не более " + available + " участников")); return; }
        adapter.notifyDataSetChanged();
        updateButton();
        message.setText("Контактов: " + contacts.size() + " · выбрано: " + selected.size());
    }

    private void updateButton() {
        done.setText("Добавить (" + selected.size() + ")");
        done.setEnabled(!selected.isEmpty());
    }

    private void complete() {
        ArrayList<String> names = new ArrayList<>(), phones = new ArrayList<>();
        for (ContactDirectory.Contact contact : selected.values()) { names.add(contact.name); phones.add(contact.phone); }
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
        state.putBoolean("permissionRequested", permissionRequested);
        super.onSaveInstanceState(state);
    }

    private final class ContactAdapter extends BaseAdapter {
        @Override public int getCount() { return visible.size(); }
        @Override public ContactDirectory.Contact getItem(int position) { return visible.get(position); }
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
            ContactDirectory.Contact contact = getItem(position);
            views.avatar.setText(contact.name.substring(0, contact.name.offsetByCodePoints(0, 1)).toUpperCase(Locale.ROOT));
            views.name.setText(contact.name);
            views.phone.setText(contact.phone.isEmpty() ? "Без номера" : contact.phone);
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

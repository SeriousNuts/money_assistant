package com.example.myapplication;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/** Reads all visible contacts, including those without a phone number. */
final class ContactDirectory {
    static final class Contact {
        final long id;
        final String name, phone;

        Contact(long id, String name, String phone) {
            this.id = id;
            this.name = name;
            this.phone = phone == null ? "" : phone;
        }

        boolean matches(String query) {
            String needle = query.trim().toLowerCase(Locale.ROOT);
            if (name.toLowerCase(Locale.ROOT).contains(needle) || phone.toLowerCase(Locale.ROOT).contains(needle)) return true;
            String digits = needle.replaceAll("[^0-9]", "");
            return !digits.isEmpty() && phone.replaceAll("[^0-9]", "").contains(digits);
        }
    }

    static List<Contact> read(ContentResolver resolver) {
        Cursor people = resolver.query(ContactsContract.Contacts.CONTENT_URI,
                new String[]{ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY},
                null, null, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " ASC");
        if (people == null) throw new IllegalStateException("Не удалось прочитать контакты");
        Cursor phones = null;
        try {
            phones = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                            ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, null);
            if (phones == null) throw new IllegalStateException("Не удалось прочитать номера");
            return merge(people, phones);
        } finally {
            people.close();
            if (phones != null) phones.close();
        }
    }

    static List<Contact> merge(Cursor people, Cursor phones) {
        LinkedHashMap<Long, String> numbers = new LinkedHashMap<>();
        while (phones.moveToNext()) {
            long id = phones.getLong(0);
            String phone = phones.getString(1);
            if (!numbers.containsKey(id) && phone != null && !phone.trim().isEmpty())
                numbers.put(id, phone.trim());
        }
        List<Contact> result = new ArrayList<>();
        while (people.moveToNext()) {
            String name = people.getString(1);
            if (name != null && !name.trim().isEmpty())
                result.add(new Contact(people.getLong(0), name.trim(), numbers.get(people.getLong(0))));
        }
        return result;
    }

    private ContactDirectory() { }
}

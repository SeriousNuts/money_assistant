package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;

import java.util.ArrayList;

public class ContactsList extends AppCompatActivity {
    RecyclerView ContactView;
    ContactsAdapter ContactsAdapter;
    ContentResolver contentResolver;
    String phone = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts_list);
        ContactView = findViewById(R.id.ContactView);
        ContactView.setLayoutManager(new LinearLayoutManager(this));
        ContactsAdapter = new ContactsAdapter();
        ContactView.setAdapter(ContactsAdapter);
        contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, new String[]{
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.HAS_PHONE_NUMBER,
                ContactsContract.Contacts.PHOTO_ID
                },null,null,ContactsContract.Contacts._ID);
        ArrayList<Contact>contacts = new ArrayList<>();
        if (cursor != null && cursor.moveToFirst()){
            do{
                long id = cursor.getLong(0);
                String name = cursor.getString(1);
                String hasNumber = cursor.getString(2);
                if (Integer.parseInt(hasNumber) != 0) {
                    Cursor pCur;
                    pCur = this.getContentResolver().query(
                            ContactsContract.CommonDataKinds
                                    .Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds
                                    .Phone.CONTACT_ID + " = ?",
                            new String[]{String.valueOf(id)},
                            null);
                    while (pCur.moveToNext()) {
                        phone = pCur.getString(
                                pCur.getColumnIndex(
                                        ContactsContract.
                                                CommonDataKinds.
                                                Phone.NUMBER));
                    }
                }
                long imageId = cursor.getLong(3);
                Contact contact = new Contact(name, phone,id,imageId);
                contacts.add(contact);
            }while (cursor.moveToNext());
        }
        if (cursor != null) {cursor.close();}
        ContactsAdapter.setContacts(contacts);

    }
}
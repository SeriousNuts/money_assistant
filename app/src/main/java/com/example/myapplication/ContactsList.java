package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.util.ArrayList;

public class ContactsList extends AppCompatActivity {
    RecyclerView ContactView;
    ContactsAdapter ContactsAdapter;
    ContentResolver contentResolver;
    String phone = "";
    ArrayList<Contact>ChooseContact = new ArrayList<>();
    ArrayList<ImageView>Avatars = new ArrayList<>();
    ArrayList<Contact>contacts = new ArrayList<>();
    Button choosenContactButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts_list);
        ContactView = findViewById(R.id.ContactView);
        choosenContactButton = findViewById(R.id.contactsChoosenButt);
        ContactView.setLayoutManager(new LinearLayoutManager(this));
        ContactsAdapter = new ContactsAdapter();
        ContactView.setAdapter(ContactsAdapter);
        contentResolver = getContentResolver();
        @SuppressLint("UseCompatLoadingForDrawables") final Drawable checkCircle = this.getResources().getDrawable(R.drawable.ic_baseline_check_circle_outline_24);
        @SuppressLint("UseCompatLoadingForDrawables") final Drawable oneXML = this.getResources().getDrawable(R.drawable.one);
        Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, new String[]{
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.HAS_PHONE_NUMBER,
                ContactsContract.Contacts.PHOTO_ID
                },null,null,ContactsContract.Contacts._ID);

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
                else {
                    phone = "No number";
                }
                long imageId = cursor.getLong(3);
                Contact contact = new Contact(name, phone,imageId,id);
                contacts.add(contact);
            }while (cursor.moveToNext());
        }
        if (cursor != null) {cursor.close();}
        ContactsAdapter.setOnItemListenerListener(new ContactsAdapter.OnItemListener() {
            ImageView checked= null;
            @Override
            public void OnItemClickListener(View view, int position) {

                ImageView imageView = (ImageView)view.findViewById(R.id.checkImage);
                if (imageView.getTag()!=checkCircle) {
                    imageView.setTag(checkCircle);
                    imageView.setImageDrawable(checkCircle);
                    ChooseContact.add(contacts.get(view.getId()));
                    Avatars.add(imageView);
                }
                else
                {
                    imageView.setImageDrawable(oneXML);
                    imageView.setTag(oneXML);
                    ChooseContact.remove(contacts.get(position));

                    Avatars.remove(imageView);
                }

            }

            @Override
            public void OnItemLongClickListener(View view, int position) {

            }
        });
        ContactsAdapter.setContacts(contacts);
        choosenContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent IntentAddUsers = new Intent(ContactsList.this, MainWindow.class);
                IntentAddUsers.putExtra("ChoosenContacts",ChooseContact );
                startActivity(IntentAddUsers);

            }
        });
    }
}
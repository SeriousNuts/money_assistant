package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class AfterContact extends AppCompatActivity {
private RecyclerView choosenContactsList;
public ArrayList<Contact>ContactsArray = new ArrayList<>();
private choosenContactsArapter ContAdapter;
private int countOfContacts;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_after_contact);
        choosenContactsList= findViewById(R.id.contactsRecycler);
        choosenContactsList.setLayoutManager(new LinearLayoutManager(this));
        choosenContactsArapter choosenContactsArapter = new choosenContactsArapter();
        choosenContactsList.addItemDecoration(new Cardview_item_decor.SpacesItemDecoration(10));
        choosenContactsList.setAdapter(choosenContactsArapter);


        //ArrayList arrayFromContacts = (ArrayList) getIntent().getParcelableArrayListExtra("ChoosenContacts");
        Bundle choosenContacts = getIntent().getExtras();
        if (choosenContacts!=null){

            ContactsArray = (ArrayList<Contact>) choosenContacts.getSerializable("ChoosenContacts");

        }
         else {
            Toast.makeText(AfterContact.this, "список контактов пуст", Toast.LENGTH_SHORT).show();
        }
        choosenContactsArapter.setChoosenContacts(ContactsArray);





    }
}
package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;

public class AfterContact extends AppCompatActivity {
private RecyclerView choosenContactsList;
private choosenContactsArapter ContAdapter;
private int countOfContacts;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_after_contact);
        choosenContactsList= findViewById(R.id.contactsRecycler);
        LinearLayoutManager layoutManager= new LinearLayoutManager(this);
        ArrayList<Contact> arrayFromContacts = (ArrayList) getIntent().getSerializableExtra("ChoosenContacts");
        if (arrayFromContacts!=null){
            for (int j = 0; j < arrayFromContacts.size(); j++) {countOfContacts++;}
        }
        choosenContactsArapter choosenContactsArapter = new choosenContactsArapter(this,arrayFromContacts,countOfContacts);
        choosenContactsList.setAdapter(choosenContactsArapter);
        choosenContactsList.setLayoutManager(layoutManager);



    }
}
package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import static com.example.myapplication.R.id.bottom_menu;

public class StartMenu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_menu);
        Button createEventButton = findViewById(R.id.createEvent);


        createEventButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int permissionStatus = ContextCompat.checkSelfPermission(StartMenu.this, Manifest.permission.READ_CONTACTS);
                if(permissionStatus == PackageManager.PERMISSION_GRANTED) {
                    Intent contactIntent = new Intent(StartMenu.this, ContactsList.class);
                    startActivity(contactIntent);
                }
                else {
                    ActivityCompat.requestPermissions(StartMenu.this, new String[] {Manifest.permission.READ_CONTACTS,
                                    Manifest.permission.WRITE_CONTACTS},
                            1);
                }
            }
        });
    }
}
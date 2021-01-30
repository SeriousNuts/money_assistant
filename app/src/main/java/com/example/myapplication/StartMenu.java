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

import com.cuberto.liquid_swipe.LiquidPager;
import com.example.myapplication.ViewPager.ViewPager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import static com.example.myapplication.R.id.Pager;
import static com.example.myapplication.R.id.bottom_menu;

public class StartMenu extends AppCompatActivity {
LiquidPager pager;
ViewPager viewPager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_menu);
        pager = findViewById(R.id.pager);
        viewPager = new ViewPager(getSupportFragmentManager(),10);
        pager.setAdapter(viewPager);


    }
}
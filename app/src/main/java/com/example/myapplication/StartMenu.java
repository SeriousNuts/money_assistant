package com.example.myapplication;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.cuberto.liquid_swipe.LiquidPager;
import com.example.myapplication.ViewPager.ViewPager;
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
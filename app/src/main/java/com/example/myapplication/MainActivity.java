package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.ArrayList;

import static com.example.myapplication.R.id.sign_in;
import static com.example.myapplication.R.id.sign_up;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private LinearLayout mDotsLayout;
    private TextView[] mDots;
    private EditText Password;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ViewPager mSlidePageViewer = (ViewPager) findViewById(R.id.Pager);
        mDotsLayout = (LinearLayout) findViewById(R.id.LinearLayout);
        authorization_slider_layout sliderAdapter = new authorization_slider_layout(this);
        mSlidePageViewer.setAdapter(sliderAdapter);
        addDotsInditcator(0);
        mSlidePageViewer.addOnPageChangeListener(ViewListener);
        }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case sign_in:
                EditText email = (EditText) findViewById(R.id.email_toggle);
                Password = (EditText) findViewById(R.id.password_toggle);
                        //вход
                FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
                firebaseAuth.signInWithEmailAndPassword(email.getText().toString(),Password.getText().toString()).addOnCompleteListener(
                        new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            Intent intentMainWindows = new Intent(MainActivity.this, MainWindow.class);
                            startActivity(intentMainWindows);
                        }
                        else{
                            Toast.makeText(MainActivity.this,"Authentication failed."+ task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                break;
            case sign_up:
                Intent intentSignUpActitvity = new Intent(MainActivity.this,SignUpActivity.class);
                startActivity(intentSignUpActitvity);

            default:
                break;
        }
    }

    private void addDotsInditcator(int position) {
        mDots = new TextView[3];
        mDotsLayout.removeAllViews();
        for (int i = 0; i < mDots.length; i++) {
            mDots[i] = new TextView(this);
            mDots[i].setText(Html.fromHtml("&#8226"));
            mDots[i].setTextSize(35);
            mDots[i].setTextColor(getResources().getColor(R.color.colorPrimaryDark));

            mDotsLayout.addView(mDots[i]);
        }
        if (mDots.length > 0) {
            mDots[position].setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        }
    }

    private String to_hex(EditText password) {
        //crypting password
        return BCrypt.hashpw(Password.toString(), BCrypt.gensalt(12));
    }

    ViewPager.OnPageChangeListener ViewListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            addDotsInditcator(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

}


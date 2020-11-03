package com.example.myapplication;

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

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.ArrayList;





public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private ViewPager mSlidePageViewer;
    private LinearLayout mDotsLayout;
    private TextView[] mDots;
    private authorization_slider_layout sliderAdapter;
    private EditText Phone;
    private EditText Password;
    private Button SignInButton;
    private Button SignUpButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mSlidePageViewer = (ViewPager) findViewById(R.id.Pager);
        mDotsLayout = (LinearLayout) findViewById(R.id.LinearLayout);
        SignInButton = (Button) findViewById(R.id.sign_in);
        sliderAdapter = new authorization_slider_layout(this);
        mSlidePageViewer.setAdapter(sliderAdapter);
        addDotsInditcator(0);
        mSlidePageViewer.addOnPageChangeListener(ViewListener);

        }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.sign_in:
                Intent intent;
                intent = new Intent(MainActivity.this, MainWindow.class);
                startActivity(intent);
                break;
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

   /* public void onClickSignIn(View view) throws SQLException {ЭТОТ МЕТОД ДОЛЖЕН БЫТЬ ПЕРЕНЕСЁН В КНОПКУ SIGN IN
        Phone = (EditText) findViewById(R.id.phone_toggle);
        Password = (EditText) findViewById(R.id.password_toggle);
        SignInButton = (Button) findViewById(R.id.sign_in);
        DataBase date = new DataBase();
        if (date.signIn_user(Password.toString(), Phone.toString())) {
            //вход
            openActivity2();
            System.out.print("success");

        } else
            System.out.printf("wrong");
    }*/

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


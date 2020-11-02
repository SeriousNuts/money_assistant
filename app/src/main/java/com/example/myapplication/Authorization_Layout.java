package com.example.myapplication;

import android.text.Html;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;

import androidx.viewpager.widget.ViewPager;

public class Authorization_Layout extends MainActivity {
    private ViewPager mSlidePageViewer;
    private LinearLayout mDotsLayout;
    private TextView[] mDots;
    private authorization_slider_layout sliderAdapter;
    private EditText Phone;
    private EditText Password;
    private ListView SignInButton;
    private TextView SignUpButton;


    public void setup_authorization(){

        mSlidePageViewer = (ViewPager) findViewById(R.id.Pager);
        mDotsLayout = (LinearLayout) findViewById(R.id.LinearLayout);
        SignInButton = (ListView) findViewById(R.id.sign_in);

        sliderAdapter = new authorization_slider_layout();
        mSlidePageViewer.setAdapter(sliderAdapter);
        addDotsInditcator();

    }
    private void addDotsInditcator(){
        mDots = new TextView[3];
        for (int i = 0; i < mDots.length;i++)
        {
            mDots[i] = new TextView(this);
            mDots[i].setText(Html.fromHtml("&#8226"));
            mDots[i].setTextSize(35);
            mDots[i].setTextColor(getResources().getColor(R.color.colorPrimaryDark));

            mDotsLayout.addView(mDots[i]);
        }
    }
    private String to_hex(EditText password)
    {
        //crypting password
        return BCrypt.hashpw(Password.toString(), BCrypt.gensalt(12));
    }

    public void onClickSignIn(ListView view) throws SQLException {
        Phone = (EditText) findViewById(R.id.phone_toggle);
        Password = (EditText) findViewById(R.id.password_toggle);
        SignInButton = (ListView) findViewById(R.id.sign_in);
        DataBase date =  new DataBase();
        if (date.signIn_user(Password.toString(), Phone.toString()))
        {
            //вход
        }
        else
            System.out.printf("wrong");
    }
}

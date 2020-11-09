package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import static android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL;
import static android.view.View.AUTOFILL_HINT_PHONE;
import static com.example.myapplication.R.id.EnterName;
import static com.example.myapplication.R.id.buttonAdd;



public class ChartGenerator extends AppCompatActivity implements View.OnClickListener{
LinearLayout llMain;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart_generator);
        Button Add;
        Add=(Button) findViewById(R.id.buttonAdd);
        llMain=(LinearLayout) findViewById(R.id.llMain);



       // String crtN = String.valueOf(ChartName.getText().toString());
    }



    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case EnterName:
                EditText chartName = (EditText) findViewById(R.id.ChartName);
                EditText UserOne = (EditText) findViewById(R.id.User1);
                EditText UserTwo = (EditText) findViewById(R.id.User2);
                EditText UserThree = (EditText) findViewById(R.id.User3);
                EditText UserFour = (EditText) findViewById(R.id.User4);
                EditText UserFive = (EditText) findViewById(R.id.User5);
               Intent intent1 = new Intent(ChartGenerator.this, MainWindow.class);
                Bundle Name= new Bundle();
                Bundle User1= new Bundle();
                Bundle User2= new Bundle();
                Bundle User3= new Bundle();
                Bundle User4= new Bundle();
                Bundle User5= new Bundle();
                //имя диаграммы
                Name.putString("Chart Name",chartName.getText().toString());
                //имена пользователей
                User1.putString("User1",UserOne.getText().toString());
                User2.putString("User2",UserTwo.getText().toString());
                User3.putString("User3",UserThree.getText().toString());
                User4.putString("User4",UserFour.getText().toString());
                User5.putString("User5",UserFive.getText().toString());
                //передача всех данных в MainWindows


                intent1.putExtras(Name);
                intent1.putExtras(User1);
                intent1.putExtras(User2);
                intent1.putExtras(User3);
                intent1.putExtras(User4);
                intent1.putExtras(User5);
                startActivity(intent1);
                break;
            case buttonAdd:

                LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                int valuePos= Gravity.CENTER_HORIZONTAL;
                param.gravity=valuePos;
                int ValueView = TYPE_NUMBER_FLAG_DECIMAL;
                EditText User=new EditText(this);
                EditText Value = new EditText(this);
                Value.setInputType(TYPE_NUMBER_FLAG_DECIMAL);
                Intent intent = new Intent(ChartGenerator.this, MainWindow.class);
                Bundle value= new Bundle();
                value.putString("Value", Value.getText().toString());
                intent.putExtras(value);
                llMain.addView(Value,param);


                break;
            case R.id.logout:
                FirebaseAuth.getInstance().signOut();
                Intent intentLogout = new Intent(ChartGenerator.this, MainActivity.class);
                startActivity(intentLogout);
                finish();
                break;
            default:
                break;
        }


    }

}
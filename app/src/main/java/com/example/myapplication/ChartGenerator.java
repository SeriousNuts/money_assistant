package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import static com.example.myapplication.R.id.EnterName;



public class ChartGenerator extends AppCompatActivity  implements View.OnClickListener{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart_generator);
Button Name;
Name=(Button) findViewById(EnterName);




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

                EditText ValueOne =(EditText) findViewById(R.id.Value1);
                EditText ValueTwo =(EditText) findViewById(R.id.Value2);
                EditText ValueThree =(EditText) findViewById(R.id.Value3);
                EditText ValueFour =(EditText) findViewById(R.id.Value4);
                EditText ValueFive =(EditText) findViewById(R.id.Value5);

                //Toast.makeText(ChartGenerator.this, "Имя вашей диаграммы: "+ chartName.getText().toString(), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(ChartGenerator.this, MainWindow.class);
                Bundle Name= new Bundle();

                Bundle User1= new Bundle();
                Bundle User2= new Bundle();
                Bundle User3= new Bundle();
                Bundle User4= new Bundle();
                Bundle User5= new Bundle();
                Bundle Value1=new Bundle();
                Bundle Value2=new Bundle();
                Bundle Value3=new Bundle();
                Bundle Value4=new Bundle();
                Bundle Value5=new Bundle();

                //значения пользователей
                Value1.putString("ValueOne",ValueOne.getText().toString());
                Value2.putString("ValueTwo",ValueTwo.getText().toString());
                Value3.putString("ValueThree",ValueThree.getText().toString());
                Value4.putString("ValueFour",ValueFour.getText().toString());
                Value5.putString("ValueFive",ValueFive.getText().toString());
                //имя диаграммы
                Name.putString("Chart Name",chartName.getText().toString());
                //имена пользователей
                User1.putString("User1",UserOne.getText().toString());
                User2.putString("User2",UserTwo.getText().toString());
                User3.putString("User3",UserThree.getText().toString());
                User4.putString("User4",UserFour.getText().toString());
                User5.putString("User5",UserFive.getText().toString());
                //передача всех данных в MainWindows
                intent.putExtras(Name);
                intent.putExtras(User1);
                intent.putExtras(User2);
                intent.putExtras(User3);
                intent.putExtras(User4);
                intent.putExtras(User5);
                intent.putExtras(Value1);
                intent.putExtras(Value2);
                intent.putExtras(Value3);
                intent.putExtras(Value4);
                intent.putExtras(Value5);
                startActivity(intent);
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
package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import static android.text.InputType.TYPE_CLASS_NUMBER;
import static com.example.myapplication.R.*;
import static com.example.myapplication.R.id.EnterName;
import static com.example.myapplication.R.id.buttonAdd;


public class ChartGenerator extends AppCompatActivity implements View.OnClickListener{
LinearLayout llMain;
ArrayList<String>NumbersofEditText = new ArrayList<>();
ArrayList<String>EditTexts = new ArrayList<>();
int i = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_chart_generator);
        llMain=(LinearLayout) findViewById(id.llMain);
    }


    private void scanCode(){
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setCaptureActivity(QRScanner.class);
        integrator.setOrientationLocked(false);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
        integrator.setPrompt("Scanning code");
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null){
            if(result.getContents() != null){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(result.getContents());
                builder.setTitle("Результат сканирования");
                builder.setPositiveButton("Отсканировать ещё", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                            scanCode();
                    }
                }).setNegativeButton("Закончить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
            else{
                Toast.makeText(this, "Ничего не отсканировано", Toast.LENGTH_SHORT).show();
            }
        }
        else{
            super.onActivityResult(requestCode,resultCode, data);
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case EnterName:
                EditText chartName = (EditText) findViewById(id.ChartName);
                EditText UserOne = (EditText) findViewById(id.User1);
                EditText UserTwo = (EditText) findViewById(id.User2);
                EditText UserThree = (EditText) findViewById(id.User3);
                EditText UserFour = (EditText) findViewById(id.User4);
                EditText UserFive = (EditText) findViewById(id.User5);
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
               for(int i = 0; i <NumbersofEditText.size();i++ ){
                   EditText ed = (EditText) findViewById(i+1);
                   if(!ed.getText().toString().equals("")) {
                       EditTexts.add(ed.getText().toString());
                   }
               }
                intent1.putExtra("list",EditTexts);
                startActivity(intent1);
                EditTexts.clear();
                break;
            case buttonAdd:
                i++;
                LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                param.gravity= Gravity.CENTER_HORIZONTAL;
                final EditText editText = new EditText(this);
                editText.setId(i);
                NumbersofEditText.add(Integer.toString(i));//запись id едиттекстов
                editText.setInputType(TYPE_CLASS_NUMBER);
                llMain.addView(editText, param);
                break;
            case id.logout:
                FirebaseAuth.getInstance().signOut();
                Intent intentLogout = new Intent(ChartGenerator.this, MainActivity.class);
                startActivity(intentLogout);
                finish();
                break;
            case id.qr_scanner:
                scanCode();
            default:
                break;
        }


    }
}
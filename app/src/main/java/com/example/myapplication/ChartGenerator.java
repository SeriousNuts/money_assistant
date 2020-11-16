package com.example.myapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;

import static android.text.InputType.TYPE_CLASS_NUMBER;
import static android.text.InputType.TYPE_TEXT_VARIATION_PERSON_NAME;
import static com.example.myapplication.R.id;
import static com.example.myapplication.R.id.EnterName;
import static com.example.myapplication.R.id.buttonAdd;
import static com.example.myapplication.R.layout;


public class ChartGenerator extends AppCompatActivity implements View.OnClickListener{
LinearLayout llMain;
    LinearLayout llMain2;
ArrayList<String>NumbersofEditText = new ArrayList<>();
ArrayList<String>EditTexts = new ArrayList<>();
    ArrayList<String>NumbersofEditText2 = new ArrayList<>();
    ArrayList<String>EditTexts2 = new ArrayList<>();

int i = 0;
int d=0;
    int x= 10;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_chart_generator);
        llMain=(LinearLayout) findViewById(id.llMain);
        llMain2=(LinearLayout) findViewById(id.llMain2);
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

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case EnterName:
                EditText chartName = (EditText) findViewById(id.ChartName);
               Intent intent1 = new Intent(ChartGenerator.this, MainWindow.class);
                Bundle Name= new Bundle();
                //имя диаграммы
                Name.putString("Chart Name",chartName.getText().toString());
                intent1.putExtras(Name);
                for (int z=0; z<NumbersofEditText2.size();z++) {

                        EditText ed2 = (EditText) findViewById(Integer.parseInt(NumbersofEditText2.get(z)));
                        EditTexts2.add(ed2.getText().toString());
                }

                intent1.putExtra("users",EditTexts2);


               for(int i = 0; i <NumbersofEditText.size();i++ ){
                   EditText ed = (EditText) findViewById(Integer.parseInt(NumbersofEditText.get(i)));
                   if(!ed.getText().toString().equals("")) {
                       EditTexts.add(ed.getText().toString());
                   }

               }
                intent1.putExtra("list",EditTexts);
                startActivity(intent1);
                EditTexts.clear();
                EditTexts2.clear();


                break;
            case buttonAdd:
                i++;
                LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                param.gravity= Gravity.CENTER_HORIZONTAL;
                final EditText editText = new EditText(this);
                editText.setId(i);
                editText.setHint("Сумма");
                NumbersofEditText.add(Integer.toString(i));
                //запись id едиттекстов
                editText.setInputType(TYPE_CLASS_NUMBER);
                d=d+x;
                LinearLayout.LayoutParams param2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                param2.gravity = Gravity.CENTER_HORIZONTAL;
                final EditText Users = new EditText(this);
                Users.setId(d);
                Users.setHint("Имя");
                Users.setInputType(TYPE_TEXT_VARIATION_PERSON_NAME);
                NumbersofEditText2.add(Integer.toString(d));
                llMain2.addView(Users, param2);
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
                break;
            case id.progress_bars:
                Intent progresIntent = new Intent(this, ProgressBars.class);
                startActivity(progresIntent);
                break;
            default:
                break;
        }


    }
}
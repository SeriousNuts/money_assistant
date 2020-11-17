package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
import static android.view.Gravity.*;
import static com.example.myapplication.R.id;
import static com.example.myapplication.R.id.EnterName;
import static com.example.myapplication.R.id.buttonAdd;
import static com.example.myapplication.R.layout;


public class ChartGenerator extends AppCompatActivity implements View.OnClickListener {
    LinearLayout llMain2;
    ArrayList<String> NumbersofNameEditText = new ArrayList<>();
    ArrayList<String> EditTexts = new ArrayList<>();
    ArrayList<String> NumbersofSummEditText = new ArrayList<>();
    ArrayList<String> EditTexts2 = new ArrayList<>();

    int i = 0;
    int d = 0;
    int x = 50;
    int buttonId;
    int Checkid = 0;
    int Checkid2 = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_chart_generator);
        llMain2 = findViewById(id.llMain2);
    }

    private void scanCode() {
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
        if (result != null) {
            if (result.getContents() != null) {
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
            } else {
                Toast.makeText(this, "Ничего не отсканировано", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @SuppressLint("ResourceType")
    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case EnterName:
                EditText chartName = findViewById(id.ChartName);
                Intent intent1 = new Intent(ChartGenerator.this, MainWindow.class);
                Bundle Name = new Bundle();
                //имя диаграммы
                Name.putString("Chart Name", chartName.getText().toString());
                intent1.putExtras(Name);
                //пользователи
                for (int z = 0; z < NumbersofNameEditText.size(); z++) {
                    EditText ed2 = findViewById(Integer.parseInt(NumbersofNameEditText.get(z)));
                    EditTexts2.add(ed2.getText().toString());

                }

                intent1.putExtra("users", EditTexts2);

                //значения
                for (int i = 0; i < NumbersofSummEditText.size(); i++) {
                    EditText ed = findViewById(Integer.parseInt(NumbersofSummEditText.get(i)));
                    if (!ed.getText().toString().equals("")) {
                        EditTexts.add(ed.getText().toString());
                    } else {
                        Toast.makeText(ChartGenerator.this, "сумма не может быть пустой", Toast.LENGTH_SHORT).show();

                    }

                }
                intent1.putExtra("list", EditTexts);
                startActivity(intent1);
                EditTexts.clear();
                EditTexts2.clear();


                break;
            case buttonAdd:
                Checkid2 = Checkid2 + x;
                i++;
                LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                param.gravity = RELATIVE_LAYOUT_DIRECTION;
                final EditText editText = new EditText(this);
                editText.setId(i);
                NumbersofSummEditText.add(Integer.toString(i));
                editText.setHint("Сумма");
                //запись id едиттекстов
                editText.setInputType(TYPE_CLASS_NUMBER);
                d = d + x;
                final LinearLayout.LayoutParams param2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                param2.gravity = RIGHT;
                final EditText Users = new EditText(this);
                Users.setId(d);
                NumbersofNameEditText.add(Integer.toString(d));
                Users.setHint("Имя");
                Users.setInputType(TYPE_TEXT_VARIATION_PERSON_NAME);
                buttonId++;
                final Button deleteField = new Button(this);
                deleteField.setId(buttonId);
                deleteField.setText("Удалить");
                llMain2.addView(Users, param2);
                llMain2.addView(editText, param);
                llMain2.addView(deleteField);
                deleteField.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Checkid++;
                        Checkid2++;
                        NumbersofNameEditText.remove(Integer.toString(Users.getId()));
                        NumbersofSummEditText.remove(Integer.toString(editText.getId()));
                        llMain2.removeView(Users);
                        llMain2.removeView(editText);
                        llMain2.removeView(deleteField);
                    }

                });

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
// ArrayList<Integer> positions = new ArrayList<Integer>();
// positions.add(3); // add some sample values positions.add(6);
// add some sample values positions.add(1);
// add some sample values positions.add(2);
// add some sample values positions.add(6);
// for(int i=0;i<positions.size();i++) { if(positions.get(i) == 6) { positions.remove(i); } }
// Log.i("========== After Remove ",":: "+positions.toString());
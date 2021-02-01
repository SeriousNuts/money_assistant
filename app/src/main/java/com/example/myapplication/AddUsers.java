package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static android.text.InputType.TYPE_CLASS_NUMBER;
import static android.text.InputType.TYPE_TEXT_VARIATION_PERSON_NAME;
import static android.text.InputType.TYPE_TEXT_VARIATION_PHONETIC;
import static android.view.Gravity.RELATIVE_LAYOUT_DIRECTION;
import static android.view.Gravity.RIGHT;
import static com.example.myapplication.R.id.ChartName;
import static com.example.myapplication.R.id.EnterName;
import static com.example.myapplication.R.id.bottom_menu;
import static com.example.myapplication.R.id.nav_add;
import static com.example.myapplication.R.id.nav_exit;
import static com.example.myapplication.R.id.percent;


public class AddUsers extends Fragment {


    ArrayList<String> NumbersofNameEditText = new ArrayList<>();
    ArrayList<String> EditTexts = new ArrayList<>();
    ArrayList<String> NumbersofSummEditText = new ArrayList<>();
    ArrayList<String> EditTexts2 = new ArrayList<>();
    ArrayList<String> NumbersofPhonesEditText = new ArrayList<>();
    ArrayList<String> EditTexts3 = new ArrayList<>();
    int i = 0;
    int d = 0;
    int x = 50;
    int f = 1000;
    int g = 0;
    int buttonId;

    CheckBox notifyALL;
    //смски


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View RootView = inflater.inflate(R.layout.fragment_add_users, container, false);



        notifyALL = (CheckBox) RootView.findViewById(R.id.notifyAll);

        Button EnterBut = RootView.findViewById(EnterName);
        BottomNavigationView bottomMenu = RootView.findViewById(bottom_menu);
        bottomMenu.setOnNavigationItemSelectedListener(navListener);

        EnterBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {



                final EditText chartName = RootView.findViewById(R.id.ChartName);
                Intent intent1 = new Intent(getActivity(), MainWindow.class);
                Bundle Name = new Bundle();
                //имя диаграммы
                //пользователи
                for (int z = 0; z < NumbersofNameEditText.size(); z++) {
                    EditText ed2 = getView().findViewById(Integer.parseInt(NumbersofNameEditText.get(z)));
                    EditTexts2.add(ed2.getText().toString());
                }

                intent1.putExtra("users", EditTexts2);
                //значения
                for (int i = 0; i < NumbersofSummEditText.size(); i++) {
                    EditText ed = getView().findViewById(Integer.parseInt(NumbersofSummEditText.get(i)));
                    if (!ed.getText().toString().equals("")) {
                        EditTexts.add(ed.getText().toString());
                    } else {
                        // Toast.makeText(ChartGenerator.this, "сумма не может быть пустой", Toast.LENGTH_SHORT).show();

                    }

                }
                intent1.putExtra("list", EditTexts);


                ///////



               // if (!chartname.equals("")) {





               // } else {
                    Toast.makeText(getActivity(), "Введите имя дигарммы", Toast.LENGTH_SHORT).show();
               // }
                if (notifyALL.isChecked()) {

                    for (int h = 0; h < NumbersofPhonesEditText.size(); h++) {
                        EditText ed3 = RootView.findViewById(Integer.parseInt(NumbersofPhonesEditText.get(h)));
                        if (!ed3.getText().toString().equals("")) {
                            EditTexts3.add(ed3.getText().toString());
                        }

                        EditText ed2 = RootView.findViewById(Integer.parseInt(NumbersofNameEditText.get(h)));
                        EditText ed = RootView.findViewById(Integer.parseInt(NumbersofSummEditText.get(h)));
                      //  smsManager.sendTextMessage(String.valueOf(EditTexts3), chartName.getText().toString(), ed2.getText().toString() + ",вы должны мне -" + ed.getText().toString() + " рублей.", sent_pi, deliver_pi);
                        EditTexts3.clear();
                        Name.putString("Chart Name", chartName.getText().toString());
                        intent1.putExtras(Name);
                    }
                }


                startActivity(intent1);
                //EditTexts.clear();
                //EditTexts2.clear();

            }
        });
        return RootView;
    }


    private BottomNavigationView.OnNavigationItemSelectedListener navListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @SuppressLint("NonConstantResourceId")
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                /*
                case nav_contacts:
                    int permissionStatus = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_CONTACTS);
                    if(permissionStatus == PackageManager.PERMISSION_GRANTED) {
                        Intent contactIntent = new Intent(getActivity(), ContactsList.class);
                        startActivity(contactIntent);
                    }
                    else {
                        ActivityCompat.requestPermissions(getActivity(), new String[] {Manifest.permission.READ_CONTACTS,
                                Manifest.permission.WRITE_CONTACTS},
                                1);
                    }
                    break;
                 */
                case nav_exit:
                    FirebaseAuth.getInstance().signOut();
                    Intent intentLogout = new Intent(getActivity(), MainActivity.class);
                    Toast.makeText(getActivity(),"exit", Toast.LENGTH_SHORT).show();
                    startActivity(intentLogout);
                    getActivity().finish();
                    break;
                    /*
                case nav_qr:

                    break;
                     */
                /*
                case nav_progress:
                    Intent progresIntent = new Intent(getActivity(), ChartProgressActivity.class);
                    startActivity(progresIntent);
                    break;

                 */
                case nav_add:
                    final LinearLayout llMain2 = getView().findViewById(R.id.llMain2);
                    i++;
                    LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    param.gravity = RELATIVE_LAYOUT_DIRECTION;
                    final EditText editText = new EditText(getActivity());
                    editText.setId(i);
                    NumbersofSummEditText.add(Integer.toString(i));
                    editText.setHint("Сумма");
                    //запись id едиттекстов
                    editText.setInputType(TYPE_CLASS_NUMBER);


                    d = d + x;
                    final LinearLayout.LayoutParams param2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    param2.gravity = RIGHT;
                    final EditText Users = new EditText(getActivity());
                    Users.setId(d);
                    NumbersofNameEditText.add(Integer.toString(d));
                    Users.setHint("Имя");
                    Users.setInputType(TYPE_TEXT_VARIATION_PERSON_NAME);
                    buttonId++;

                    g = g + f;
                    final LinearLayout.LayoutParams param3 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    param3.gravity = RIGHT;
                    final EditText Phones = new EditText(getActivity());
                    Phones.setId(g);
                    Phones.setHint("Номер телефона");
                    Phones.setInputType(TYPE_TEXT_VARIATION_PHONETIC);
                    NumbersofPhonesEditText.add(Integer.toString(g));

                    final Button deleteField = new Button(getActivity());
                    deleteField.setId(buttonId);
                    deleteField.setText("Удалить");
                    llMain2.addView(Users, param2);
                    llMain2.addView(editText, param);
                    llMain2.addView(Phones, param3);
                    llMain2.addView(deleteField);

                    deleteField.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            NumbersofNameEditText.remove(Integer.toString(Users.getId()));
                            NumbersofSummEditText.remove(Integer.toString(editText.getId()));
                            NumbersofPhonesEditText.remove(Integer.toString(Phones.getId()));
                            llMain2.removeView(Users);
                            llMain2.removeView(editText);
                            llMain2.removeView(Phones);
                            llMain2.removeView(deleteField);
                        }

                    });
                    break;
            }
            return true;
        }
    };





}
package com.example.myapplication;

import android.Manifest;
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
    FirebaseUser payment;
    FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    CheckBox notifyALL;
    //смски
    String SENT_SMS = "SENT_SMS";
    String DELIVER_SMS = "DELIVER_SMS";
    Intent sent_intent = new Intent(SENT_SMS);
    Intent deliver_intent = new Intent(DELIVER_SMS);
    PendingIntent sent_pi, deliver_pi;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View RootView = inflater.inflate(R.layout.fragment_add_users, container, false);

        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.SEND_SMS}, 1);

        notifyALL = (CheckBox) RootView.findViewById(R.id.notifyAll);

        Button EnterBut = RootView.findViewById(EnterName);
        BottomNavigationView bottomMenu = RootView.findViewById(bottom_menu);
        bottomMenu.setOnNavigationItemSelectedListener(navListener);
        sent_pi = PendingIntent.getBroadcast(getActivity(), 0, sent_intent, 0);
        deliver_pi = PendingIntent.getBroadcast(getActivity(), 0, deliver_intent, 0);
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
                payment = FirebaseAuth.getInstance().getCurrentUser();
                String PaymentKey = payment.getUid();
                EditText chartname = RootView.findViewById(ChartName);
                if (!chartname.equals("")) {
                    String Chartname = chartname.getText().toString();
                    Date currentTime = Calendar.getInstance().getTime();
                    EditText EditText;
                    Chart chart = new Chart(Chartname, "", Integer.toString(NumbersofNameEditText.size()), "0", "Chart");
                    int fullAmount = 0;
                    Map<String, Object> paymentsMap = new HashMap<>();
                    paymentsMap.put("Chart", chart);
                    String date = currentTime.toString();
                    for (int j = 0; j < NumbersofSummEditText.size(); j++) {
                        EditText = RootView.findViewById(Integer.parseInt(NumbersofSummEditText.get(j)));
                        String amount = EditText.getText().toString();
                        fullAmount = fullAmount + Integer.parseInt(amount);
                    }
                    for (int i = 0; i < NumbersofNameEditText.size(); i++) {
                        EditText = RootView.findViewById(Integer.parseInt(NumbersofNameEditText.get(i)));
                        String name = EditText.getText().toString();
                        EditText = RootView.findViewById(Integer.parseInt(NumbersofSummEditText.get(i)));
                        String amount = EditText.getText().toString();
                        String id = Integer.toString(i);
                        int percentPayment = (Integer.parseInt(amount) * 100 / fullAmount);
                        Payment paymentCh = new Payment(Chartname, id, name, date, Integer.parseInt(amount), percentPayment);


                        firebaseFirestore.collection(PaymentKey).add(paymentCh);
                        Toast.makeText(getActivity(), "Добавлено", Toast.LENGTH_SHORT).show();
                    }
                    firebaseFirestore.collection(PaymentKey).document(Chartname).set(chart);
                } else {
                    Toast.makeText(getActivity(), "Введите имя дигарммы", Toast.LENGTH_SHORT).show();
                }
                if (notifyALL.isChecked()) {
                    SmsManager smsManager = SmsManager.getDefault();
                    for (int h = 0; h < NumbersofPhonesEditText.size(); h++) {
                        EditText ed3 = RootView.findViewById(Integer.parseInt(NumbersofPhonesEditText.get(h)));
                        if (!ed3.getText().toString().equals("")) {
                            EditTexts3.add(ed3.getText().toString());
                        }

                        EditText ed2 = RootView.findViewById(Integer.parseInt(NumbersofNameEditText.get(h)));
                        EditText ed = RootView.findViewById(Integer.parseInt(NumbersofSummEditText.get(h)));
                        smsManager.sendTextMessage(String.valueOf(EditTexts3), chartName.getText().toString(), ed2.getText().toString() + ",вы должны мне -" + ed.getText().toString() + " рублей.", sent_pi, deliver_pi);
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

    @Override
    public void onResume() {

        super.onResume();

        requireActivity().registerReceiver(sentReceiver, new IntentFilter(SENT_SMS));

        requireActivity().registerReceiver(deliverReceiver, new IntentFilter(DELIVER_SMS));

    }


    @Override
    public void onStop() {

        super.onStop();

        requireActivity().unregisterReceiver(sentReceiver);

        requireActivity().unregisterReceiver(deliverReceiver);

    }

    BroadcastReceiver sentReceiver = new BroadcastReceiver() {

        @Override

        public void onReceive(Context context, Intent intent) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    Toast.makeText(context, "Sented", Toast.LENGTH_LONG).show();
                    break;
                default:
                    Toast.makeText(context, "Error S", Toast.LENGTH_LONG).show();
                    break;
            }
        }

    };


    BroadcastReceiver deliverReceiver = new BroadcastReceiver() {

        @Override

        public void onReceive(Context context, Intent intent) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    Toast.makeText(context, "Delivered", Toast.LENGTH_LONG).show();
                    break;
                default:
                    Toast.makeText(context, "Error D", Toast.LENGTH_LONG).show();
                    break;
            }
        }

    };
    private BottomNavigationView.OnNavigationItemSelectedListener navListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
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
                    scanCode();
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

    private void scanCode() {
        IntentIntegrator integrator = new IntentIntegrator(getActivity());
        integrator.setCaptureActivity(QRScanner.class);
        integrator.setOrientationLocked(false);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
        integrator.setPrompt("Scanning code");
        integrator.initiateScan();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
                        getActivity().finish();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                Toast.makeText(getActivity(), "Ничего не отсканировано", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}
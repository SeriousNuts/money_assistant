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
import android.graphics.Color;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.Helper.MyButtonClickListener;
import com.example.myapplication.Helper.MySwipeHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.text.InputType.TYPE_CLASS_NUMBER;

public class AfterContact extends AppCompatActivity   {

    private RecyclerView choosenContactsList;
    public ArrayList<Contact>ContactsArray = new ArrayList<>();
    public ArrayList<String> NumberOfPhones = new ArrayList<>();
    int Checker = 0;
    Double T_A_var;
   // double ValueForEveryUser;

    
    Long phone;

    String SENT_SMS = "SENT_SMS";
    String DELIVER_SMS = "DELIVER_SMS";
    Intent sent_intent = new Intent(SENT_SMS);
    Intent deliver_intent = new Intent(DELIVER_SMS);
    PendingIntent sent_pi, deliver_pi;
    
    FloatingActionButton Button;

    ChoosenContactsArapter choosenContactsArapter = new ChoosenContactsArapter();

    FirebaseUser payment;
    FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_after_contact);
        
        ActivityCompat.requestPermissions(AfterContact.this, new String[]{Manifest.permission.SEND_SMS}, 1);
        sent_pi = PendingIntent.getBroadcast(AfterContact.this, 0, sent_intent, 0);
        deliver_pi = PendingIntent.getBroadcast(AfterContact.this, 0, deliver_intent, 0);
        choosenContactsList= findViewById(R.id.contactsRecycler);

        choosenContactsList.setLayoutManager(new LinearLayoutManager(this));

        choosenContactsList.addItemDecoration(new Cardview_item_decor.SpacesItemDecoration(10));
        choosenContactsList.setAdapter(choosenContactsArapter);
        Button=findViewById(R.id.floatingActionButton);
        BottomNavigationView AfterContactMenu= findViewById(R.id.afterContact_menu);
        AfterContactMenu.setOnNavigationItemSelectedListener(navListener);

        Bundle choosenContacts = getIntent().getExtras();
        if (choosenContacts!=null){

            ContactsArray = (ArrayList<Contact>) choosenContacts.getSerializable("ChoosenContacts");

        }
         else {
            Toast.makeText(AfterContact.this, "список контактов пуст", Toast.LENGTH_SHORT).show();
        }
        choosenContactsArapter.setChoosenContacts(ContactsArray);
        MySwipeHelper swipeHelper = new MySwipeHelper(this, choosenContactsList,300) {
            @Override
            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buffer) {
                buffer.add(new MyButton(AfterContact.this,
                        "Удалить",
                        30,
                        0,
                        Color.parseColor("#8B0000"), new  MyButtonClickListener(){
                    @Override
                    public void onClick(int pos) {
                        ContactsArray.remove(pos);
                        if(T_A_var!=null && ContactsArray.size() !=0) {
                            double AmountForEveryUser = (T_A_var / ContactsArray.size());
                            choosenContactsArapter.setAmountForEveryUserValue(AmountForEveryUser);
                        }
                        choosenContactsArapter.notifyItemRemoved(pos);
                        Toast.makeText(AfterContact.this, "Удалено!", Toast.LENGTH_SHORT).show();
                    }
                }));
            }
        };
        Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SmsSender();
                DataSender();
                Intent MainWindowIntent = new Intent(AfterContact.this,MainWindow.class);
                startActivity(MainWindowIntent);
            }
        });
    }
    //Навигационная панель
    private BottomNavigationView.OnNavigationItemSelectedListener navListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @SuppressLint("NonConstantResourceId")
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()){
                case R.id.Notify_item:
                    if(Checker == 0){
                        NotifyDialogBuilder();
                    }
                    else {
                        NotifyDialogBuilderExiter();
                    }
                    break;
                case R.id.totalAmount_item:
                    TotalAmountDialogBuilder();

                    break;
                case R.id.QR_Scanner_item:
                    scanCode();
                    break;
            }
            return true;
        }
    };
    //Сканнер кодов
    private void scanCode() {
        IntentIntegrator integrator = new IntentIntegrator(AfterContact.this);
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
                AlertDialog.Builder builder = new AlertDialog.Builder(AfterContact.this);
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
                        AfterContact.this.finish();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                Toast.makeText(AfterContact.this, "Ничего не отсканировано", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    //Диалоговое окно для общей суммы
    public void TotalAmountDialogBuilder(){
            final EditText totalAmount = new EditText(AfterContact.this);
            totalAmount.setHint("Введите общую сумму");
            totalAmount.setInputType(TYPE_CLASS_NUMBER);
            AlertDialog.Builder TotalAmountDialog = new AlertDialog.Builder(AfterContact.this);
            TotalAmountDialog.setTitle("Общая сумма.");
            TotalAmountDialog.setMessage("Введите общую сумму,которая разделится на всех.");
            TotalAmountDialog.setView(totalAmount);

            TotalAmountDialog.setPositiveButton("Продолжить", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
              String T_A =  totalAmount.getText().toString();
              T_A_var= Double.parseDouble(T_A);
              if(T_A_var!=null) {
                  double AmountForEveryUser = (T_A_var / ContactsArray.size());
                  Intent intentForAdapter = new Intent(AfterContact.this,ChoosenContactsArapter.class);
                  intentForAdapter.putExtra("AmountForEveryUser",AmountForEveryUser);

                  choosenContactsArapter.setAmountForEveryUserValue(AmountForEveryUser);
              }
              else{
                  Toast.makeText(AfterContact.this,"Введите сумму",Toast.LENGTH_LONG).show();
              }
                }
            });
            TotalAmountDialog.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            TotalAmountDialog.create().show();

    }
    //Диалоговое окно для того что бы уведомлять всех
    private void NotifyDialogBuilder(){
        
        AlertDialog.Builder NotifyAllDialog = new AlertDialog.Builder(AfterContact.this);
        NotifyAllDialog.setTitle("Уведомлять всех.");
        NotifyAllDialog.setMessage("Вы действительно хотите отправлять каждому участнику события смс с просьбой оплатить долг?\n" +
                "Содержание смс можно изменить в настройках.");
        NotifyAllDialog.setPositiveButton("Да", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                 Checker=1;
                
            }
        });
        NotifyAllDialog.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        NotifyAllDialog.create().show();
    }
    private void NotifyDialogBuilderExiter(){
        AlertDialog.Builder NotifyAllDialogExiter = new AlertDialog.Builder(AfterContact.this);
        NotifyAllDialogExiter.setTitle("Уведомлять всех.");
        NotifyAllDialogExiter.setMessage("Вы уже выбрали функцию уведомления участников.\n" +
                "Хотите ее отменить?");
        NotifyAllDialogExiter.setPositiveButton("Да", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Checker=0;

            }
        });
        NotifyAllDialogExiter.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        NotifyAllDialogExiter.create().show();
    }
    @Override
    public void onResume() {

        super.onResume();

        AfterContact.this.registerReceiver(sentReceiver, new IntentFilter(SENT_SMS));

        AfterContact.this.registerReceiver(deliverReceiver, new IntentFilter(DELIVER_SMS));

    }


    @Override
    public void onStop() {

        super.onStop();

        AfterContact.this.unregisterReceiver(sentReceiver);

        AfterContact.this.unregisterReceiver(deliverReceiver);

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
    public void SmsSender(){
        if (Checker!=0){
            for(int i=0;i<ContactsArray.size();i++) {
                NumberOfPhones.add(String.valueOf(ContactsArray.get(i).number));
            }
            for(int j=0;j<NumberOfPhones.size();j++){
                phone = Long.parseLong(NumberOfPhones.get(j));
                if (!phone.equals("")) {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(String.valueOf(phone),null,"Test Message", sent_pi, deliver_pi);
                }
            }
        }
    }
    public void DataSender(){
        ///
        //работает
        payment = FirebaseAuth.getInstance().getCurrentUser();
        String PaymentKey = payment.getUid();
        EditText chartname =findViewById(R.id.Chart_nameEditText);
        String Chartname = chartname.getText().toString();
        Date currentTime = Calendar.getInstance().getTime();
        EditText EditText;
        Chart chart = new Chart(Chartname, "", Integer.toString(ContactsArray.size()), "0", "Chart");
        int fullAmount = 0;
        Map<String, Object> paymentsMap = new HashMap<>();
        paymentsMap.put("Chart", chart);
        String date = currentTime.toString();

/*
        ///
        //то что не работает
        for (int j = 0; j < NumbersofSummEditText.size(); j++) {
            EditText =findViewById(Integer.parseInt(NumbersofSummEditText.get(j)));
            String amount = EditText.getText().toString();
            fullAmount = fullAmount + Integer.parseInt(amount);
        }
        for (int i = 0; i < NumbersofNameEditText.size(); i++) {
            EditText =findViewById(Integer.parseInt(NumbersofNameEditText.get(i)));
            String name = EditText.getText().toString();
            EditText = findViewById(Integer.parseInt(NumbersofSummEditText.get(i)));
            String amount = EditText.getText().toString();
            String id = Integer.toString(i);
            int percentPayment = (Integer.parseInt(amount) * 100 / fullAmount);
            Payment paymentCh = new Payment(Chartname, id, name, date, Integer.parseInt(amount), percentPayment);


            firebaseFirestore.collection(PaymentKey).add(paymentCh);
            Toast.makeText(AfterContact.this, "Добавлено", Toast.LENGTH_SHORT).show();
        }
        firebaseFirestore.collection(PaymentKey).document(Chartname).set(chart);


 */

    }
}
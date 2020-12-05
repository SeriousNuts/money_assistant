package com.example.myapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import static android.view.Gravity.RELATIVE_LAYOUT_DIRECTION;
import static android.view.Gravity.RIGHT;
import static com.example.myapplication.R.id.ChartName;
import static com.example.myapplication.R.id.EnterName;
import static com.example.myapplication.R.id.buttonAdd;
import static com.example.myapplication.R.id.progress_bars;
import static com.example.myapplication.R.id.qr_scanner;

public class AddUsers extends Fragment {


    ArrayList<String> NumbersofNameEditText = new ArrayList<>();
    ArrayList<String> EditTexts = new ArrayList<>();
    ArrayList<String> NumbersofSummEditText = new ArrayList<>();
    ArrayList<String> EditTexts2 = new ArrayList<>();
    int i = 0;
    int d = 0;
    int x = 50;
    int buttonId;
    FirebaseUser payment;
    FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View RootView = inflater.inflate(R.layout.fragment_add_users, container, false);
        Button EnterBut = RootView.findViewById(EnterName);
        Button addBut = RootView.findViewById(buttonAdd);
        Button qr = RootView.findViewById(qr_scanner);
        Button logout = RootView.findViewById(R.id.logout);
        Button progressBars = RootView.findViewById(progress_bars);

        EnterBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText chartName = RootView.findViewById(R.id.ChartName);
                Intent intent1 = new Intent(getActivity(), MainWindow.class);
                Bundle Name = new Bundle();
                //имя диаграммы
                Name.putString("Chart Name", chartName.getText().toString());
                intent1.putExtras(Name);
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
                    for (int i = 0; i < NumbersofNameEditText.size(); i++) {
                        EditText = RootView.findViewById(Integer.parseInt(NumbersofNameEditText.get(i)));
                        String name = EditText.getText().toString();
                        EditText = RootView.findViewById(Integer.parseInt(NumbersofSummEditText.get(i)));
                        String amount = EditText.getText().toString();
                        fullAmount = fullAmount + Integer.parseInt(amount);
                        String percent = "0%";
                        String id = Integer.toString(i);
                        Payment paymentCh = new Payment(Chartname, id, name, date, amount, percent);

                        //chart.payments.put("Payment " + i, paymentCh);
                        //paymentsMap.put("Payment" + i, paymentCh);
                        firebaseFirestore.collection(PaymentKey).add(paymentCh);
                        Toast.makeText(getActivity(), "Добавлено", Toast.LENGTH_SHORT).show();
                    }
                    chart.fullAmount = fullAmount + " rub";
                    firebaseFirestore.collection(PaymentKey).document(Chartname).set(chart);
                }
                else{
                    Toast.makeText(getActivity(), "Введите имя дигарммы",Toast.LENGTH_SHORT).show();
                }
                startActivity(intent1);

            }
        });


        addBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                final Button deleteField = new Button(getActivity());
                deleteField.setId(buttonId);
                deleteField.setText("Удалить");
                llMain2.addView(Users, param2);
                llMain2.addView(editText, param);
                llMain2.addView(deleteField);
                deleteField.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        NumbersofNameEditText.remove(Integer.toString(Users.getId()));
                        NumbersofSummEditText.remove(Integer.toString(editText.getId()));
                        llMain2.removeView(Users);
                        llMain2.removeView(editText);
                        llMain2.removeView(deleteField);
                    }

                });
            }
        });

        qr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanCode();
            }
        });
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intentLogout = new Intent(getActivity(), MainActivity.class);
                startActivity(intentLogout);
                getActivity().finish();
            }
        });
        progressBars.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent progresIntent = new Intent(getActivity(), ChartProgressActivity.class);
                startActivity(progresIntent);
            }
        });
        return RootView;
    }

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
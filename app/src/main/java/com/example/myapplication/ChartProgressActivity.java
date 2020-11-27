package com.example.myapplication;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ChartProgressActivity extends AppCompatActivity {
    RecyclerView recyclerViewChart;
    FirebaseUser payment = FirebaseAuth.getInstance().getCurrentUser();
    String PaymentKey = payment.getUid();
    FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    Query queryChart = firebaseFirestore.collection(PaymentKey).whereEqualTo("typo", "Chart");
    String chartname;
    private RecycleViewChartAdapter firestoreRecyclerAdapter;
    int ViewPosition;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart_progress);

        recyclerViewChart = findViewById(R.id.RecyclerViewChart);

        FirestoreRecyclerOptions<Chart> options =
                new FirestoreRecyclerOptions.Builder<Chart>()
                        .setQuery(queryChart, Chart.class)
                        .build();

        recyclerViewChart.setLayoutManager(new LinearLayoutManager(this));
        firestoreRecyclerAdapter = new RecycleViewChartAdapter(options);
        firestoreRecyclerAdapter.setOnItemListenerListener(new RecycleViewChartAdapter.OnItemListener() {
            @Override
            public void OnItemClickListener(View view, int position) {
                chartname = firestoreRecyclerAdapter.getItem(position).Chartname;
                Intent chartToProgressBars = new Intent(ChartProgressActivity.this, ProgressBars.class);
                Bundle bundleChartName = new Bundle();
                //имя диаграммы
                bundleChartName.putString("chartname", chartname);
                chartToProgressBars.putExtras(bundleChartName);
                startActivity(chartToProgressBars);
                //Toast.makeText(ChartProgressActivity.this, "click", Toast.LENGTH_LONG).show();
            }

            @Override
            public void OnItemLongClickListener(View view, int position) {
                ViewPosition = position;
                    ShowAlertDialog();
            }
        });

        recyclerViewChart.setAdapter(firestoreRecyclerAdapter);

    }
    @Override
    public void onStop(){
        super.onStop();
        firestoreRecyclerAdapter.stopListening();
    }

    @Override
    public void onStart(){
        super.onStart();
        firestoreRecyclerAdapter.startListening();
    }

    public void ShowAlertDialog(){
        EditDeleteDialog dialog = new EditDeleteDialog();
        dialog.show(getSupportFragmentManager(),"Delete");
    }
    public void okClicked() {
        chartname = firestoreRecyclerAdapter.getItem(ViewPosition).Chartname;
        final String PaymentKey = payment.getUid();
        firebaseFirestore.collection(PaymentKey).document(chartname)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(ChartProgressActivity.this, "Deleted",Toast.LENGTH_SHORT).show();
                        DeleteDocument(chartname);
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(ChartProgressActivity.this, e.toString(),Toast.LENGTH_SHORT).show();
            }
        });
        firestoreRecyclerAdapter.notifyDataSetChanged();
    }
    public void DeleteDocument(String chartname){
        final String PaymentKey = payment.getUid();
        firebaseFirestore.collection(PaymentKey).document(chartname)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(ChartProgressActivity.this, "Deleted",Toast.LENGTH_SHORT).show();
                        firestoreRecyclerAdapter.notifyDataSetChanged();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(ChartProgressActivity.this, e.toString(),Toast.LENGTH_SHORT).show();
            }
        });
    }
}
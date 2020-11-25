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
    FirebaseUser payment;
    Dialog EditDelete;
    Query queryChart;
    FirebaseFirestore firebaseFirestore;
    String chartname;
    private RecycleViewChartAdapter firestoreRecyclerAdapter;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart_progress);

        payment = FirebaseAuth.getInstance().getCurrentUser();
        final String PaymentKey = payment.getUid();
        firebaseFirestore = FirebaseFirestore.getInstance();
        queryChart = firebaseFirestore.collection(PaymentKey);
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
            }

            @Override
            public void OnItemLongClickListener(View view, int position) {
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
    public void setDialog(){
        EditDelete = new Dialog(ChartProgressActivity.this);
        EditDelete.setContentView(R.layout.edit_delete_dialog);
        EditDelete.show();
    }
    public void ShowAlertDialog(){
        EditDeleteDialog dialog = new EditDeleteDialog();
        dialog.show(getSupportFragmentManager(),"Delete");
    }
    public void okClicked(int which) {
        chartname = firestoreRecyclerAdapter.getItem(which).Chartname;
        payment = FirebaseAuth.getInstance().getCurrentUser();
        final String PaymentKey = payment.getUid();
        firebaseFirestore.collection(PaymentKey).document(chartname)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(ChartProgressActivity.this, "Deleted",Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(ChartProgressActivity.this, e.toString(),Toast.LENGTH_SHORT).show();
            }
        });
    }
}
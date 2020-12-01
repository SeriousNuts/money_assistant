package com.example.myapplication;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ProgressBars extends AppCompatActivity {
    RecyclerView recyclerView;
    FirebaseUser payment = FirebaseAuth.getInstance().getCurrentUser();
    String PaymentKey = payment.getUid();
    Query queryPayments;
    FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    RecycleViewAdapter firestoreRecyclerAdapter;
    String chartname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intentReceived= getIntent();
        setContentView(R.layout.activity_progress_bars);
        Bundle bundleChartName=intentReceived.getExtras();
        if(bundleChartName != null){
            chartname=bundleChartName.getString("chartname");
        }
        queryPayments = firebaseFirestore.collection(PaymentKey).whereEqualTo("ParentChart",chartname);


        recyclerView = findViewById(R.id.PaymentResView);


        FirestoreRecyclerOptions<Payment> options =
                new FirestoreRecyclerOptions.Builder<Payment>()
                        .setQuery(queryPayments, Payment.class)
                        .build();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        firestoreRecyclerAdapter = new RecycleViewAdapter(options);
        firestoreRecyclerAdapter.setOnItemListenerListener(new RecycleViewAdapter.OnItemListener() {
            @Override
            public void OnItemClickListener(View view, int position) {

            }

            @Override
            public void OnItemLongClickListener(View view, int position) {

            }
        });
        recyclerView.setAdapter(firestoreRecyclerAdapter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        firestoreRecyclerAdapter.stopListening();
    }

    @Override
    protected void onStart() {
        super.onStart();
        firestoreRecyclerAdapter.startListening();
    }

}
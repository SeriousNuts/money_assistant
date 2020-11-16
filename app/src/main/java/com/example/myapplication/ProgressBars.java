package com.example.myapplication;

import android.os.Bundle;
import android.widget.ArrayAdapter;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ProgressBars extends AppCompatActivity {
    int i = 0;
    ArrayList<Payment> listDataPayment = new ArrayList<>();
    RecyclerView recyclerView;
    RecycleViewAdapter recyclerViewAdapter;
    private DatabaseReference mDataBase;
    private String PAYMENT_KEY = "PAYMENT";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress_bars);
        init();

        FirebaseRecyclerOptions<Payment> options =
                new FirebaseRecyclerOptions.Builder<Payment>()
                        .setQuery(mDataBase, Payment.class)
                        .build();
        recyclerViewAdapter = new RecycleViewAdapter(options);
        recyclerView.setAdapter(recyclerViewAdapter);
    }
    @Override
    protected void onStart(){
        super.onStart();
        recyclerViewAdapter.startListening();
    }
    @Override
    protected void onStop(){
        super.onStop();
        recyclerViewAdapter.stopListening();
    }
    public void init(){
        recyclerView = findViewById(R.id.PaymentResView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        listDataPayment = new ArrayList<>();
        mDataBase = FirebaseDatabase.getInstance().getReference(PAYMENT_KEY);
    }
}
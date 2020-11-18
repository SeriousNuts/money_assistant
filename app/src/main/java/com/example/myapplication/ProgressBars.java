package com.example.myapplication;

import android.os.Bundle;
import android.widget.ArrayAdapter;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    ArrayList<Payment> listDataPayment = new ArrayList<>();
    RecyclerView recyclerView;
    RecycleViewAdapter recyclerViewAdapter;
    FirebaseUser payment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        listDataPayment = new ArrayList<>();

        payment = FirebaseAuth.getInstance().getCurrentUser();
        String PaymentKey = payment.getUid();

        setContentView(R.layout.activity_progress_bars);
        recyclerView = findViewById(R.id.PaymentResView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        FirebaseRecyclerOptions<Payment> options =
                new FirebaseRecyclerOptions.Builder<Payment>()
                        .setQuery(FirebaseDatabase.getInstance().getReference(PaymentKey).child("Payments"), Payment.class)
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

}
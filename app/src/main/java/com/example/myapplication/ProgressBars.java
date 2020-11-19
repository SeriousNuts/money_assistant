package com.example.myapplication;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ProgressBars extends AppCompatActivity {
    ArrayList<Payment> listDataPayment = new ArrayList<>();
    RecyclerView recyclerView;
    RecycleViewAdapter recyclerViewAdapter;
    FirebaseUser payment;
    Dialog EditDelete;
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
        recyclerViewAdapter.setOnItemListenerListener(new RecycleViewAdapter.OnItemListener() {
            @Override
            public void OnItemClickListener(View view, int position) {
                Toast.makeText(ProgressBars.this,"click work",Toast.LENGTH_SHORT).show();
                Log.v("  ", "click");

            }

            @Override
            public void OnItemLongClickListener(View view, int position) {
                setDialog();
            }
        });

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
    public void setDialog(){
        EditDelete = new Dialog(ProgressBars.this);
        EditDelete.setTitle("Удалить или изменить");
        EditDelete.setContentView(R.layout.edit_delete_dialog);
        EditDelete.show();
    }
}
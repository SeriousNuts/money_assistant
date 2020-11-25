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
    FirebaseUser payment;
    Dialog EditDelete;
    Query query;
    FirebaseFirestore firebaseFirestore;
    FirestoreRecyclerAdapter firestoreRecyclerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        payment = FirebaseAuth.getInstance().getCurrentUser();
        String PaymentKey = payment.getUid();
        firebaseFirestore = FirebaseFirestore.getInstance();
        setContentView(R.layout.activity_progress_bars);

        recyclerView = findViewById(R.id.PaymentResView);


        FirestoreRecyclerOptions<Payment> options =
                new FirestoreRecyclerOptions.Builder<Payment>()
                        .setQuery(query, Payment.class)
                        .build();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        firestoreRecyclerAdapter = new FirestoreRecyclerAdapter<Payment, PaymentsViewHolder>(options) {
            @NonNull
            @Override
            public PaymentsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.one_payment_row, parent, false);
                //view.setOnClickListener(new RecycleViewAdapter.RV_ItemListener());
                //view.setOnLongClickListener(new RecycleViewAdapter.RV_ItemListener());
                return new PaymentsViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull PaymentsViewHolder holder, int position, @NonNull Payment payment) {
                holder.parentChartName.setText(payment.ParentChart);
                holder.name.setText(payment.name);
                holder.date.setText(payment.date);
                holder.amount.setText(payment.amount);
                holder.percent.setText(payment.percent);
                holder.itemView.setId(position);
            }
        };
        //recyclerView.setHasFixedSize(true);

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

    private static class PaymentsViewHolder extends RecyclerView.ViewHolder {
        TextView name, date, amount, percent, parentChartName;

        public PaymentsViewHolder(@NonNull View itemview) {
            super(itemview);
            parentChartName = itemView.findViewById(R.id.parentChartName);
            name = itemView.findViewById(R.id.name);
            date = itemView.findViewById(R.id.date);
            amount = itemView.findViewById(R.id.amount);
            percent = itemView.findViewById(R.id.percent);
        }
    }

    public void setDialog() {
        EditDelete = new Dialog(ProgressBars.this);
        EditDelete.setTitle("Удалить или изменить");
        EditDelete.setContentView(R.layout.edit_delete_dialog);
        EditDelete.show();
    }
}
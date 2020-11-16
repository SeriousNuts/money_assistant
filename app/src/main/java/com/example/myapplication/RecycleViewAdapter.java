package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RecycleViewAdapter extends FirebaseRecyclerAdapter<Payment,RecycleViewAdapter.myviewholder> {

    public RecycleViewAdapter(@NonNull FirebaseRecyclerOptions<Payment> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull myviewholder holder, int position, @NonNull Payment payment) {
        holder.name.setText(payment.name);
        holder.date.setText(payment.date);
        holder.amount.setText(payment.amount);
        holder.percent.setText(payment.percent);
    }

    @NonNull
    @Override
    public myviewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.one_payment_row,parent,false);
        return new myviewholder(view);
    }

    class myviewholder extends RecyclerView.ViewHolder{
        TextView name, date, amount, percent;
        public myviewholder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            date = itemView.findViewById(R.id.date);
            amount = itemView.findViewById(R.id.amount);
            percent = itemView.findViewById(R.id.percent);
        }
    }
}

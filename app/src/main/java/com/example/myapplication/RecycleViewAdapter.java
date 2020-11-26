package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class RecycleViewAdapter extends FirestoreRecyclerAdapter<Payment, RecycleViewAdapter.myviewholder> {
    private OnItemListener onItemListener;

    public RecycleViewAdapter(@NonNull FirestoreRecyclerOptions<Payment> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull final myviewholder holder, final int position, @NonNull Payment payment) {
        holder.parentChartName.setText(payment.ParentChart);
        holder.name.setText(payment.name);
        holder.date.setText(payment.date);
        holder.amount.setText(payment.amount);
        holder.percent.setText(payment.percent);
        holder.itemView.setId(position);
    }

    @NonNull
    @Override
    public myviewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.one_payment_row,parent,false);
        view.setOnClickListener(new RV_ItemListener());
        view.setOnLongClickListener(new RV_ItemListener());
        return new myviewholder(view);
    }

    static class myviewholder extends RecyclerView.ViewHolder{
        TextView name, date, amount, percent, parentChartName;
        public myviewholder(@NonNull final View itemView) {
            super(itemView);
            parentChartName = itemView.findViewById(R.id.parentChartName);
            name = itemView.findViewById(R.id.name);
            date = itemView.findViewById(R.id.date);
            amount = itemView.findViewById(R.id.amount);
            percent = itemView.findViewById(R.id.percent);


        }
    }
    public  interface OnItemListener{
        void OnItemClickListener(View view, int position);
        void OnItemLongClickListener(View view, int position);
    }
    class RV_ItemListener implements View.OnClickListener, View.OnLongClickListener{

        @Override
        public void onClick(View view) {
           if (onItemListener != null){
                onItemListener.OnItemClickListener(view, view.getId());
            }
        }
        @Override
        public boolean onLongClick(View view) {
            if (onItemListener != null){
                onItemListener.OnItemLongClickListener(view,view.getId());
            }
            return true;
        }
    }
    public void setOnItemListenerListener(OnItemListener listener){
        this.onItemListener = listener;
    }
}



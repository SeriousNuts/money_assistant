package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.database.DatabaseReference;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RecycleViewChartAdapter extends FirestoreRecyclerAdapter<Chart, RecycleViewChartAdapter.ChartViewHolder> {
    private OnItemListener onItemListener;

    public RecycleViewChartAdapter(@NonNull FirestoreRecyclerOptions<Chart> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull final ChartViewHolder holder, final int position, @NonNull Chart chart) {
        holder.chartName.setText(chart.Chartname);
        holder.fullAmount.setText(chart.fullAmount);
        holder.numberOfPeople.setText(chart.NumberOfPeople);
        holder.percent.setText(chart.percent);
        holder.itemView.setId(position);
    }

    @NonNull
    @Override
    public ChartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.one_row_chart_row,parent,false);
        view.setOnClickListener(new RV_ItemListener());
        view.setOnLongClickListener(new RV_ItemListener());
        return new ChartViewHolder(view);
    }

    static class ChartViewHolder extends RecyclerView.ViewHolder{
        TextView chartName, fullAmount, numberOfPeople, percent;
        public ChartViewHolder(@NonNull final View itemView) {
            super(itemView);
            chartName = itemView.findViewById(R.id.RecycleViewChartName);
            fullAmount = itemView.findViewById(R.id.fullAmount);
            numberOfPeople = itemView.findViewById(R.id.numberOfPeople);
            percent = itemView.findViewById(R.id.RecycleViewChartPercent);

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

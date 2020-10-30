package com.example.myapplication;

import android.os.Bundle;

import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.charts.Pie;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {
    AnyChartView anyChartView;
    String[] Users = {"User1", "User2", "User3", "User4"};
    int[] Credits = {1500, 2000, 3300, 800};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        anyChartView = findViewById(R.id.any_char_view);

        setupPieChart();
    }
    public void setupPieChart(){
        Pie pie = AnyChart.pie();
        List<DataEntry> dataEntries = new ArrayList<>();
        for (int i = 0; i < Users.length; i++ )
            dataEntries.add(new ValueDataEntry(Users[i], Credits[i]));
        pie.data(dataEntries);
        pie.title("credits");
        anyChartView.setChart(pie);
    }
}

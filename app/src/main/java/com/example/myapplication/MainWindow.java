package com.example.myapplication;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;





public class MainWindow extends AppCompatActivity {
    PieChart pieChart;
    private Object TextWatcher;
    float valueUsers[]={40.5f,13.2f,60.7f,40.8f,20f};
//String Users[]={s}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.main_window);





        setContentView(R.layout.main_window);
        pieChart= findViewById(R.id.piechart);
        //процентные значения
        pieChart.setUsePercentValues(false);
        pieChart.getLegend().setEnabled(true);
        pieChart.setSoundEffectsEnabled(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDragDecelerationEnabled(true);
        //коэф трения/торможения
        pieChart.setDragDecelerationFrictionCoef(150f);
        //прозрачный круг
        pieChart.setTransparentCircleRadius(100f);
        //бул рисовать окно посередине или нет
        pieChart.setDrawHoleEnabled(true);
        //задаем текст в центре
        pieChart.setCenterText("Сбор денег");
        //размер текста в центре
        pieChart.setCenterTextSize(10f);
        //шрифт текста в центре
        pieChart.setCenterTextTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));
        //радиус окна в центре
        pieChart.setHoleRadius(30f);
        //цвет текста в центре
        pieChart.setCenterTextColor(Color.WHITE);
        //цвет самого окна(может не работать)
        pieChart.setHoleColor(Color.TRANSPARENT);
        //закругленные концы
        pieChart.setDrawRoundedSlices(false);
        //измменить шрифт юзеров
        pieChart.setDrawEntryLabels(true);
        pieChart.setEntryLabelTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));
        setupPieChart();

    }



    private void setupPieChart() {
        ArrayList<PieEntry> yValues= new ArrayList<>();
        for (int i=0; i< valueUsers.length;i++) {
            yValues.add(new PieEntry(valueUsers[i], "users"));

        }
        PieDataSet dataSet = new PieDataSet(yValues,"Users");
        dataSet.setUsingSliceColorAsValueLineColor(true);
        dataSet.setFormSize(8f);
        dataSet.setValueLineColor(Color.DKGRAY);
        dataSet.setSliceSpace(5f);
        dataSet.setAutomaticallyDisableSliceSpacing(true);
        dataSet.setSelectionShift(8f);
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        PieData data= new PieData((dataSet));
        data.setValueTextSize(10f);
        data.setValueTextColor(Color.DKGRAY);
        dataSet.notifyDataSetChanged();
        pieChart.invalidate();
        pieChart.notifyDataSetChanged();
        pieChart.setData(data);
        pieChart.animateY(2000);
    }

}
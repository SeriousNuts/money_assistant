package com.example.myapplication;


import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.Scanner;


public class MainActivity extends AppCompatActivity {
    Scanner scan = new Scanner(System.in);
    PieChart pieChart;
    private Object CinCetnerValue;


    class CinCetnerValue {

        float center = scan.nextFloat();

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {


        String users;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pieChart=(PieChart) findViewById(R.id.piechart);
        //процентные значения
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDragDecelerationEnabled(true);
        //коэф трения/торможения
        pieChart.setDragDecelerationFrictionCoef(150f);
        //прозрачный круг
        pieChart.setTransparentCircleRadius(100f);
        //бул рисовать окно посередине или нет
        pieChart.setDrawHoleEnabled(true);
        //задаем текст в центре
        pieChart.setCenterText("Sbor deneg");
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
//        Float.valueOf(edittext.getText.toString())
//        value = Float.valueOf(edittext.getText.toString())



//список пользователей
        ArrayList<PieEntry> yValues= new ArrayList<>();

        yValues.add(new PieEntry(210f,"Ilushka"));
        yValues.add(new PieEntry(359f,"Ura"));
        yValues.add(new PieEntry(519f,"Danilka"));
        yValues.add(new PieEntry(700f,"Misha"));
        yValues.add(new PieEntry(315f,"Sasha"));


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


        pieChart.setData(data);

    }
}


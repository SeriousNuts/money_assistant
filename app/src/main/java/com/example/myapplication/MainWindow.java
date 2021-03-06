package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;


public class MainWindow extends FragmentActivity {
    PieChart pieChart;
    String pieChartName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_window);
        Fragment Us = new AddedUsers_progress();
        FragmentTransaction ft= getSupportFragmentManager().beginTransaction();
        ft.add(R.id.addedUsers,Us);
        ft.commit();

//Добавление Пользовательских Данных
        Intent intentReceived= getIntent();
        Bundle name=intentReceived.getExtras();
        if(name != null){
            pieChartName=name.getString("Chart Name");
        }
        else {
            pieChartName="";
        }
      Toast.makeText(MainWindow.this,pieChartName,Toast.LENGTH_SHORT).show();
/////////////////////////////////////////////////////////
        pieChart= findViewById(R.id.piechart);
        //процентные значени
        pieChart.setUsePercentValues(false);
        pieChart.getLegend().setEnabled(true);
        pieChart.setSoundEffectsEnabled(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDragDecelerationEnabled(true);
        //коэф трения/торможения
        pieChart.setDragDecelerationFrictionCoef(120f);
        //прозрачный круг
        pieChart.setTransparentCircleRadius(40f);
        pieChart.setTransparentCircleColor(Color.LTGRAY);
        //бул рисовать окно посередине или нет
        pieChart.setDrawHoleEnabled(true);
        //задаем текст в центре
        pieChart.setCenterText(pieChartName);
        //размер текста в центре
        pieChart.setCenterTextSize(10f);
        //шрифт текста в центре
        pieChart.setCenterTextTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));
        //радиус окна в центре
        pieChart.setHoleRadius(25f);
        //цвет текста в центре
        pieChart.setCenterTextColor(Color.WHITE);
        //цвет самого окна(может не работать)
        pieChart.setHoleColor(Color.TRANSPARENT);
        //закругленные концы
        pieChart.setDrawRoundedSlices(false);
        //измменить шрифт юзеров
        pieChart.setDrawEntryLabels(true);
        pieChart.setEntryLabelTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));


        // ArrayList<String> values = name.getStringArrayList("list");
        ArrayList<String> arrayFrom;
        arrayFrom = getIntent().getStringArrayListExtra("users");
        //ArrayList<String>  arrayFromIntent=getIntent().getStringArrayListExtra("list");

        ArrayList<String> arrayFromIntent = (ArrayList) getIntent().getStringArrayListExtra("list");

        //users
        ArrayList<PieEntry> yValues= new ArrayList<>();
        //кусок кода для заполнения диаграммы напрямую из контактов
/*
        if (arrayFromContacts!=null){
            for (int j = 0; j < arrayFromContacts.size(); j++) {
                yValues.add(new PieEntry(Float.parseFloat(arrayFromContacts.get(j).name), arrayFromContacts.get(j).id));
            }
        }
        */
        //////
        if(arrayFromIntent != null) {
            for (int i = 0; i < arrayFromIntent.size(); i++) {

                yValues.add(new PieEntry(Float.parseFloat(arrayFromIntent.get(i)), arrayFrom.get(i)));
            }
        }


//дата сеты
        PieDataSet dataSet = new PieDataSet(yValues,pieChartName);
        dataSet.setUsingSliceColorAsValueLineColor(true);
        dataSet.setFormSize(8f);
        dataSet.setValueLineColor(Color.DKGRAY);
        dataSet.setSliceSpace(5f);
        dataSet.setAutomaticallyDisableSliceSpacing(true);
        dataSet.setSelectionShift(8f);
        dataSet.setColors(ColorTemplate.PASTEL_COLORS);
        PieData data= new PieData((dataSet));
        data.setValueTextSize(10f);
        data.setValueTextColor(Color.DKGRAY);
        dataSet.notifyDataSetChanged();
        pieChart.invalidate();
        pieChart.notifyDataSetChanged();
        pieChart.setData(data);
        pieChart.animateY(1500);





    }

}
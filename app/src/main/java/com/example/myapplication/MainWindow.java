package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
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
import android.widget.Toast;

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
    private TextView ChartName;
    private TextView UserOne;
    private TextView UserTwo;
    private TextView UserThree;
    private TextView UserFour;
    private TextView UserFive;
    private TextView ValueUs1;
    private TextView ValueUs2;
    private TextView ValueUs3;
    private TextView ValueUs4;
    private TextView ValueUs5;
    String User1;
    String User2;
    String User3;
    String User4;
    String User5;
    String Value;
    String Value2;
    String Value3;
    String Value4;
    String Value5;
    String pieChartName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_window);
//Добавление Пользовательских Данных
        //Имя Диаграммы
        ChartName = (TextView) findViewById(R.id.ChartName);
        Intent intentReceived= getIntent();
        Bundle name=intentReceived.getExtras();
        pieChartName=name.getString("Chart Name");
        ChartName.setText(pieChartName);
        Toast.makeText(MainWindow.this,pieChartName,Toast.LENGTH_SHORT).show();
        //User1
        UserOne=(TextView) findViewById(R.id.UserOne);
        Intent intentOne= getIntent();
        Bundle user1=intentOne.getExtras();
        User1=user1.getString("User1");
        UserOne.setText(User1);
        //User2
        UserTwo=(TextView) findViewById(R.id.UserTwo);
        Intent intentTwo= getIntent();
        Bundle user2=intentTwo.getExtras();
        User2=user2.getString("User2");
        UserTwo.setText(User2);
        //User3
        UserThree=(TextView) findViewById(R.id.UserThree);
        Intent intentThree= getIntent();
        Bundle user3=intentThree.getExtras();
        User3=user3.getString("User3");
        UserThree.setText(User3);
        //User4
        UserFour=(TextView) findViewById(R.id.UserFour);
        Intent intentFour= getIntent();
        Bundle user4=intentFour.getExtras();
        User4=user4.getString("User4");
        UserFour.setText(User4);
        //User5
        UserFive=(TextView) findViewById(R.id.UserFive);
        Intent intentFive= getIntent();
        Bundle user5=intentFive.getExtras();
        User5=user5.getString("User5");
        UserFive.setText(User5);
//////////////////////////////////////////////////////////

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
        pieChart.setCenterText(pieChartName);
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

        ArrayList<PieEntry> yValues= new ArrayList<>();
        ArrayList<String> arrayFromIntent = (ArrayList<String>) getIntent().getSerializableExtra("list");
        //users
        for (int i =0; i < arrayFromIntent.size(); i++) {
            ValueUs1=(TextView) findViewById(R.id.ValueUs1);
            if (arrayFromIntent.get(i) == null){i++;}
            yValues.add(new PieEntry(Float.parseFloat(arrayFromIntent.get(i)), User1));
            Toast.makeText(MainWindow.this, Value, Toast.LENGTH_SHORT).show();
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
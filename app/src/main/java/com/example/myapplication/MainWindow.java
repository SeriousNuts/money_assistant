package com.example.myapplication;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;





public class MainWindow extends AppCompatActivity {
    PieChart pieChart;
    private Object TextWatcher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.main_window);





        setContentView(R.layout.main_window);
        pieChart= findViewById(R.id.piechart);
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





//список пользователей
        final ArrayList<PieEntry> yValues= new ArrayList<>();
        final EditText editText = findViewById(R.id.editText);
        final TextView textView = findViewById(R.id.textView);
        editText.addTextChangedListener(new  TextWatcher() {


            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                textView.setText(s);



            }

            @Override
            public void afterTextChanged(Editable s) {

            }


        });


        String s = String.valueOf(editText.getText().toString());
        yValues.add(new PieEntry(210f, s));
        yValues.add(new PieEntry(359f, s));
        yValues.add(new PieEntry(519f, s));





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






    }

}

package com.example.myapplication;

import java.util.Map;

public class Payment extends  Chart{
    public String ParentChart, id, name, date;
    public int amount, Paypercent;

    Payment(){}

    public Payment(String parentChart, String id, String name, String date, int amount, int percent) {
        ParentChart = parentChart;
        this.id = id;
        this.name = name;
        this.date = date;
        this.amount = amount;
        this.Paypercent = percent;
    }
    public int percentCalculation(int fullAmount){
        Paypercent = amount / fullAmount;
        return  Paypercent;
    }
}

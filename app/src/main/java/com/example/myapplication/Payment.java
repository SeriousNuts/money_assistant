package com.example.myapplication;

import java.util.Map;

public class Payment{
    public String ParentChart, id, name, date, amount, percent;

    Payment(){}

    public Payment(String parentChart, String id, String name, String date, String amount, String percent) {
        ParentChart = parentChart;
        this.id = id;
        this.name = name;
        this.date = date;
        this.amount = amount;
        this.percent = percent;
    }
}

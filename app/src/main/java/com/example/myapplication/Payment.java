package com.example.myapplication;

public class Payment {
    public String id, name, date, amount, percent;

    Payment(){}

    public Payment(String id, String name, String date, String amount, String percent) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.amount = amount;
        this.percent = percent;
    }
}

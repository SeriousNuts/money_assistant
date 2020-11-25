package com.example.myapplication;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Chart {
    String Chartname = "";
    String fullAmount = "";
    String NumberOfPeople = "";
    String percent = "";
    Map<String, Object>payments = new HashMap<>();
    public Chart() {
    }

    public Chart(String chartname, String fullAmount, String numberOfPeople, String percent) {
        Chartname = chartname;
        this.fullAmount = fullAmount;
        NumberOfPeople = numberOfPeople;
        this.percent = percent;
    }

    public String getChartname() {
        return Chartname;
    }

    public void setChartname(String chartname) {
        Chartname = chartname;
    }

    public String getFullAmount() {
        return fullAmount;
    }

    public void setFullAmount(String fullAmount) {
        this.fullAmount = fullAmount;
    }

    public String getNumberOfPeople() {
        return NumberOfPeople;
    }

    public void setNumberOfPeople(String numberOfPeople) {
        NumberOfPeople = numberOfPeople;
    }

    public String getPercent() {
        return percent;
    }

    public void setPercent(String percent) {
        this.percent = percent;
    }

    public Map<String, Object> getPayments() {
        return payments;
    }

    public void setPayments(Map<String, Object> payments) {
        this.payments = payments;
    }
}



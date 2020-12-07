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
    String typo = "";
    public Chart() {
    }

    public Chart(String chartname, String fullAmount, String numberOfPeople, String percent, String typo) {
        Chartname = chartname;
        this.fullAmount = fullAmount + " rub";
        NumberOfPeople = numberOfPeople;
        this.percent = percent;
        this.typo = typo;
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

    public String getTypo() {
        return typo;
    }

    public void setTypo(String typo) {
        this.typo = typo;
    }
}



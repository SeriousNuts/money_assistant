package com.example.myapplication;

import android.net.Uri;

public class User {
    String name;
    String phone;
    Uri avatar;
    Double amount;
    Long id;

    public User(String name, String phone, Uri avatar, Double amount, Long id) {
        this.name = name;
        this.phone = phone;
        this.avatar = avatar;
        this.amount = amount;
        this.id = id;
    }
}


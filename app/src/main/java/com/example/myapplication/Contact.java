package com.example.myapplication;

public class Contact {
    String name;
    String number;
    long image_id;
    long id;

    public Contact() {
    }

    public Contact(String name, String number, long image_id, long id) {
        this.name = name;
        this.number = number;
        this.image_id = image_id;
        this.id = id;
    }
}

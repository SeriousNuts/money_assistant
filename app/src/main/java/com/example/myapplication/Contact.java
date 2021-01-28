package com.example.myapplication;

import java.io.Serializable;

public class Contact implements Serializable {
    String name;
    String number;
    Long image_id;
    Double amount;
    long id;

    public Contact() {
    }

    public Contact(String name, String number, Long image_id, long id) {
        this.name = name;
        this.number = number;
        this.image_id = image_id;
        this.id = id;
    }
}

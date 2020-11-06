package com.example.myapplication;

import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;

public class User {
    String email;
    String hashpassword;
    public User( String phone, String hashpassword){
        this.email = email;
        this.hashpassword = hashpassword;
    }
    public String getEmail(){return  email;}
    public String getHashpassword(){return hashpassword;}
    }


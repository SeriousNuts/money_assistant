package com.example.myapplication;

import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;

public class User {
    String username;
    String phone;
    String hashpassword;
    public User(String username, String phone, String hashpassword){
        this.username = username;
        this.phone = phone;
        this.hashpassword = hashpassword;
    }
    public String getUsername(){return  username;}
    public String getPhone(){return  phone;}
    public String getHashpassword(){return hashpassword;}
    public void SaveUser(){
        final String TAG = "Condition: ";
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        Map<String, Object> user  = new HashMap<>();
        user.put("username", username);
        user.put("phone", phone);
        user.put("password", hashpassword);
        database.collection("users")
                .add(user)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });



    }

}

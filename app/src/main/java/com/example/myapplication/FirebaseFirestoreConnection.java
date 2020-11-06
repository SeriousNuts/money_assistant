/*package com.example.myapplication;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import java.io.FileInputStream;

import javax.annotation.PostConstruct;

public class FirebaseFirestoreConnection {

    @PostConstruct
    public void initialize(){
        try {
            FileInputStream serviceAccount =
                    new FileInputStream("./app/FirebaseService.json");

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://moneyassistant-1604315426885.firebaseio.com")
                    .build();

            FirebaseApp.initializeApp(options);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}*/

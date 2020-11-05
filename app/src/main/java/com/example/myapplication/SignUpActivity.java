package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.mindrot.jbcrypt.BCrypt;

import static com.example.myapplication.R.id.AddDataBase;

public class SignUpActivity extends AppCompatActivity implements View.OnClickListener {
    EditText PhoneNumber;
    EditText Password;
    EditText ReplyPassword;
    EditText Username;
    Button SignUpButton;
    String HashPassword;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        SignUpButton = findViewById(AddDataBase);
    }

        @Override
        public void onClick(View view){
            switch (view.getId()){
                case AddDataBase:
                    PhoneNumber = findViewById(R.id.phoneSignUp);
                    Password = findViewById(R.id.passwordSignUp);
                    ReplyPassword = findViewById(R.id.passwordRepitSIgnUP);
                    Username = findViewById(R.id.usernameSignUp);
                    String PasswordCheck = Password.getText().toString();
                    String ReplyPasswordCheck = ReplyPassword.getText().toString();

                    if (PasswordCheck == ReplyPasswordCheck){
                        HashPassword = BCrypt.hashpw(Password.toString(), BCrypt.gensalt(12));
                        User user = new User(Username.toString(), PhoneNumber.toString(), HashPassword);
                        user.SaveUser();
                        Intent intentMainWindow = new Intent(SignUpActivity.this, MainWindow.class);
                        startActivity(intentMainWindow);
                    }
                    break;
                default:
                    break;
            }
        }

}
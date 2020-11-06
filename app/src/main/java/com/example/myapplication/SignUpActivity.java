package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.mindrot.jbcrypt.BCrypt;

import java.util.HashMap;
import java.util.Map;

import static com.example.myapplication.R.id.AddDataBase;
import static com.example.myapplication.R.id.password_toggle;

public class SignUpActivity extends AppCompatActivity implements View.OnClickListener {
    EditText email;
    EditText Password;
    EditText ReplyPassword;
    EditText Username;
    Button SignUpButton;
    String HashPassword;
    TextView SignUpBar;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        SignUpButton = findViewById(AddDataBase);

    }

        @Override
        public void onClick(View view){//произвести рефакторинг функции сделать доавбление в базу данных в отдельном методе
            switch (view.getId()){
                case AddDataBase:
                    email = findViewById(R.id.emailSignUp);
                    Password = findViewById(R.id.passwordSignUp);
                    ReplyPassword = findViewById(R.id.passwordRepitSIgnUP);
                    Username = findViewById(R.id.usernameSignUp);
                    SignUpBar = findViewById(R.id.signUpBar);
                    String PasswordCheck = Password.getText().toString();
                    String ReplyPasswordCheck = ReplyPassword.getText().toString();
                    if (PasswordCheck.equals(ReplyPasswordCheck)){
                        auth = FirebaseAuth.getInstance();
                        auth.createUserWithEmailAndPassword(email.getText().toString(), Password.getText().toString())
                                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        if (task.isSuccessful()) {
                                            Intent intentSignup = new Intent(SignUpActivity.this, MainWindow.class);
                                            startActivity(intentSignup);
                                        } else {
                                            // If sign up fails, display a message to the user.
                                            Toast.makeText(SignUpActivity.this, "Authentication failed."+ task.getException().getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }
                    else{
                        Toast.makeText(SignUpActivity.this,"Пароли не совпадают",Toast.LENGTH_SHORT).show();
                    }

                    break;
                default:
                    break;
            }
        }

}
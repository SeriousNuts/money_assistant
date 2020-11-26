package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import static com.example.myapplication.R.id.AddDataBase;

public class SignUpActivity extends AppCompatActivity implements View.OnClickListener {
    EditText email;
    EditText Password;
    EditText ReplyPassword;
    EditText Username;
    Button SignUpButton;
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
                                            finish();
                                        } else {
                                            // If sign up fails, display a message to the user.
                                            Toast.makeText(SignUpActivity.this, "Authentication failed."+ task.getException().
                                                    getMessage(), Toast.LENGTH_LONG).show();
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
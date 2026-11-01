package com.example.myapplication;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.FirebaseApp;

public final class SignUpActivity extends BaseActivity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout body = Ui.scroll(this, Ui.page(this));
        Ui.add(this, body, Ui.text(this, getString(R.string.signup_title), 30, true), 12);
        EditText email = Ui.input(this, getString(R.string.email_hint));
        email.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        Ui.add(this, body, email, 28);
        EditText password = Ui.input(this, getString(R.string.signup_password));
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        Ui.add(this, body, password, 16);
        Ui.add(this, body, Ui.button(this, getString(R.string.signup_button), () -> {
            if (email.getText().toString().trim().isEmpty() || password.length() < 6) {
                Ui.error(this, new IllegalArgumentException("Укажите почту и пароль от 6 символов")); return;
            }
            if (FirebaseApp.getApps(this).isEmpty()) {
                Ui.error(this, new IllegalStateException("Firebase не настроен: доступен только деморежим")); return;
            }
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email.getText().toString().trim(),
                    password.getText().toString()).addOnSuccessListener(result -> {
                DemoSession.set(this, false);
                startActivity(new android.content.Intent(this, StartMenu.class));
                finish();
            }).addOnFailureListener(e -> Ui.error(this, e));
        }), 24);
        Ui.add(this, body, Ui.link(this, getString(R.string.legal_menu), () ->
                startActivity(new android.content.Intent(this, LegalActivity.class))), 16);
    }
}

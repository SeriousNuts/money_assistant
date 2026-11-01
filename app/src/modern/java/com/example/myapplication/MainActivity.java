package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.FirebaseApp;

public final class MainActivity extends BaseActivity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (!LegalActivity.accepted(this)) {
            startActivity(new Intent(this, LegalActivity.class).putExtra("required", true));
            finish();
            return;
        }
        if (DemoSession.active(this) || (!FirebaseApp.getApps(this).isEmpty()
                && FirebaseAuth.getInstance().getCurrentUser() != null)) {
            openBills(); return;
        }
        LinearLayout root = Ui.page(this), body = Ui.scroll(this, root);
        Ui.add(this, body, Ui.label(this, "MONEY ASSISTANT"), 12);
        Ui.add(this, body, Ui.text(this, getString(R.string.main_title), 32, true), 12);
        Ui.add(this, body, Ui.label(this, getString(R.string.main_subtitle)), 8);
        EditText email = Ui.input(this, getString(R.string.email_hint));
        email.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        Ui.add(this, body, email, 40);
        EditText password = Ui.input(this, getString(R.string.password_hint));
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        Ui.add(this, body, password, 12);
        Ui.add(this, body, Ui.button(this, getString(R.string.sign_in), () -> {
            String login = email.getText().toString().trim();
            String secret = password.getText().toString();
            if (DemoSession.EMAIL.equals(login) && DemoSession.PASSWORD.equals(secret)) {
                DemoSession.set(this, true); openBills(); return;
            }
            if (login.isEmpty() || secret.isEmpty()) {
                Ui.error(this, new IllegalArgumentException("Заполните логин и пароль")); return;
            }
            if (FirebaseApp.getApps(this).isEmpty()) {
                Ui.error(this, new IllegalStateException("Firebase не настроен: доступен только деморежим")); return;
            }
            FirebaseAuth.getInstance().signInWithEmailAndPassword(login, secret)
                    .addOnSuccessListener(result -> { DemoSession.set(this, false); openBills(); })
                    .addOnFailureListener(e -> Ui.error(this, e));
        }), 24);
        Ui.add(this, body, Ui.secondary(this, getString(R.string.try_demo), () -> {
            DemoSession.set(this, true); openBills();
        }), 12);
        Ui.add(this, body, Ui.secondary(this, getString(R.string.create_account), () ->
                startActivity(new Intent(this, SignUpActivity.class))), 12);
        Ui.add(this, body, Ui.secondary(this, getString(R.string.language), () -> Ui.chooseLanguage(this)), 12);
        Ui.add(this, body, Ui.link(this, getString(R.string.legal_menu), () ->
                startActivity(new Intent(this, LegalActivity.class))), 12);
        Ui.add(this, body, Ui.label(this, getString(R.string.demo_private)), 24);
    }

    private void openBills() {
        startActivity(new Intent(this, StartMenu.class));
        finish();
    }
}

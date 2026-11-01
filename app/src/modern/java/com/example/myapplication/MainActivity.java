package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;

public final class MainActivity extends BaseActivity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        // This release runs locally: no sign-in, registration or Firebase request on launch.
        DemoSession.set(this, true);
        if (!LegalActivity.accepted(this)) {
            startActivity(new Intent(this, LegalActivity.class).putExtra("required", true));
            finish();
            return;
        }
        startActivity(new Intent(this, StartMenu.class));
        finish();
    }
}

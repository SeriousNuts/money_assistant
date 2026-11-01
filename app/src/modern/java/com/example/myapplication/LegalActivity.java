package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Both legal drafts remain available in the app; the first-run gate records a local acknowledgement. */
public final class LegalActivity extends BaseActivity {
    // New third-party OCR provider changes where receipt photos may be sent.
    private static final int AGREEMENT_VERSION = 2;
    private boolean termsOpened, privacyOpened;

    static boolean accepted(android.content.Context context) {
        return context.getSharedPreferences("legal-acceptance", MODE_PRIVATE)
                .getInt("version", 0) == AGREEMENT_VERSION;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        boolean required = getIntent().getBooleanExtra("required", false);
        if (state != null) {
            termsOpened = state.getBoolean("termsOpened");
            privacyOpened = state.getBoolean("privacyOpened");
        }
        LinearLayout root = Ui.page(this);
        Ui.toolbar(this, root, getString(R.string.terms_title), !required, null);
        LinearLayout body = Ui.scroll(this, root);
        Ui.add(this, body, Ui.label(this, getString(R.string.legal_notice)), 12);
        Ui.add(this, body, Ui.secondary(this, getString(R.string.language), () -> Ui.chooseLanguage(this)), 12);
        Ui.add(this, body, Ui.secondary(this, getString(R.string.terms_title),
                () -> showDocument(R.raw.terms, R.string.terms_title, true)), 24);
        Ui.add(this, body, Ui.secondary(this, getString(R.string.privacy_title),
                () -> showDocument(R.raw.privacy, R.string.privacy_title, false)), 12);
        if (required) {
            CheckBox agree = new CheckBox(this);
            agree.setText(R.string.accept_terms);
            agree.setTextColor(Ui.ink(this));
            Ui.add(this, body, agree, 24);
            Ui.add(this, Ui.footer(this, root), Ui.button(this, getString(R.string.continue_action), () -> {
                if (!termsOpened || !privacyOpened || !agree.isChecked()) {
                    Ui.error(this, new IllegalStateException(getString(R.string.legal_required)));
                    return;
                }
                if (!getSharedPreferences("legal-acceptance", MODE_PRIVATE).edit()
                        .putInt("version", AGREEMENT_VERSION).commit()) {
                    Ui.error(this, new IllegalStateException("Could not save acceptance"));
                    return;
                }
                startActivity(new Intent(this, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            }), 0);
        }
    }

    private void showDocument(int resource, int title, boolean terms) {
        try (InputStream stream = getResources().openRawResource(resource);
             ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = stream.read(buffer)) != -1) bytes.write(buffer, 0, count);
            TextView content = Ui.text(this, new String(bytes.toByteArray(), StandardCharsets.UTF_8), 15, false);
            content.setPadding(Ui.dp(this, 20), Ui.dp(this, 12), Ui.dp(this, 20), Ui.dp(this, 12));
            ScrollView scroll = new ScrollView(this);
            scroll.addView(content);
            if (terms) termsOpened = true;
            else privacyOpened = true;
            new AlertDialog.Builder(this).setTitle(title).setView(scroll)
                    .setPositiveButton(getString(R.string.continue_action), null).show();
        } catch (Exception e) { Ui.error(this, e); }
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("termsOpened", termsOpened);
        state.putBoolean("privacyOpened", privacyOpened);
        super.onSaveInstanceState(state);
    }
}

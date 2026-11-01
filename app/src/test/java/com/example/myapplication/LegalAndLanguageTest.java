package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class LegalAndLanguageTest {
    private Context context;

    @Before public void reset() {
        context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("legal-acceptance", Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences("app-language", Context.MODE_PRIVATE).edit().clear().commit();
        DemoSession.set(context, false);
    }

    @Test public void firstLaunchRequiresBothDocumentsAndExplicitAgreement() {
        MainActivity entry = Robolectric.buildActivity(MainActivity.class).create().start().resume().get();
        Intent next = Shadows.shadowOf(entry).getNextStartedActivity();
        assertEquals(LegalActivity.class.getName(), next.getComponent().getClassName());
        LegalActivity legal = Robolectric.buildActivity(LegalActivity.class, next).create().start().resume().get();
        View root = legal.findViewById(android.R.id.content);
        findClickable(root, "Лицензионное соглашение").performClick();
        ((AlertDialog) ShadowDialog.getLatestDialog()).getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        findClickable(root, "Политика конфиденциальности").performClick();
        ((AlertDialog) ShadowDialog.getLatestDialog()).getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        find(root, "Продолжить").performClick();
        assertFalse(LegalActivity.accepted(context));
        ((CheckBox) find(root, "Я прочитал")).setChecked(true);
        find(root, "Продолжить").performClick();
        assertTrue(LegalActivity.accepted(context));
    }

    @Test public void englishLocaleHasEnglishLegalDocumentAndMainScreen() throws Exception {
        context.getSharedPreferences("app-language", Context.MODE_PRIVATE).edit().putString("language", "en").commit();
        Context english = AppLanguage.wrap(context);
        assertEquals("Privacy policy", english.getString(R.string.privacy_title));
        try (InputStream policy = english.getResources().openRawResource(R.raw.privacy)) {
            byte[] heading = new byte[40];
            assertTrue(policy.read(heading) > 0);
            assertTrue(new String(heading, StandardCharsets.UTF_8).startsWith("DRAFT MONEY ASSISTANT"));
        }
        context.getSharedPreferences("legal-acceptance", Context.MODE_PRIVATE).edit().putInt("version", 1).commit();
        MainActivity entry = Robolectric.buildActivity(MainActivity.class).create().start().resume().get();
        assertNotNull(find(entry.findViewById(android.R.id.content), "Split bills with ease"));
    }

    private TextView find(View root, String text) {
        if (root instanceof TextView && ((TextView) root).getText().toString().contains(text))
            return (TextView) root;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = find(group.getChildAt(i), text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private TextView findClickable(View root, String text) {
        if (root instanceof TextView && root.isClickable()
                && ((TextView) root).getText().toString().contains(text)) return (TextView) root;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = findClickable(group.getChildAt(i), text);
                if (found != null) return found;
            }
        }
        return null;
    }
}

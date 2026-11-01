package com.example.myapplication;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ReceiptProviderTest {
    @Test public void buildDefaultIsOverriddenByExplicitLocalChoice() {
        Context context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("receipt-ocr-provider", Context.MODE_PRIVATE).edit().clear().commit();
        assertEquals(context.getString(R.string.default_online_ocr_provider), ReceiptProvider.selected(context).id);
        ReceiptProvider alternate = ReceiptProvider.selected(context) == ReceiptProvider.GEMINI
                ? ReceiptProvider.CLOUD : ReceiptProvider.GEMINI;
        assertTrue(ReceiptProvider.select(context, alternate));
        assertEquals(alternate, ReceiptProvider.selected(context));
    }
}

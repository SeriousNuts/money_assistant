package com.example.myapplication;

import android.content.Context;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class ReceiptAiKeyDeviceTest {
    @Test public void encryptsAndDeletesUserSuppliedKey() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();
        // Never overwrite a key someone entered into an existing emulator installation.
        assumeTrue(ReceiptAiKey.load(context) == null);
        try {
            ReceiptAiKey.save(context, "test-placeholder-not-a-real-key");
            assertEquals("test-placeholder-not-a-real-key", ReceiptAiKey.load(context));
            String stored = context.getSharedPreferences("receipt-ai", Context.MODE_PRIVATE).getString("key", "");
            assertFalse(stored.contains("test-placeholder"));
        } finally { ReceiptAiKey.clear(context, ReceiptProvider.GEMINI); }
        assertNull(ReceiptAiKey.load(context));
    }

    @Test public void cloudAndGeminiKeysStaySeparate() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();
        assumeTrue(ReceiptAiKey.load(context, ReceiptProvider.GEMINI) == null
                && ReceiptAiKey.load(context, ReceiptProvider.CLOUD) == null);
        try {
            ReceiptAiKey.save(context, ReceiptProvider.GEMINI, "fake-gemini-key");
            ReceiptAiKey.save(context, ReceiptProvider.CLOUD, "fake-cloud-key");
            assertEquals("fake-cloud-key", ReceiptAiKey.load(context, ReceiptProvider.CLOUD));
            assertFalse(context.getSharedPreferences("receipt-ai", Context.MODE_PRIVATE)
                    .getString("cloud-key", "").contains("fake-cloud-key"));
            ReceiptAiKey.clear(context, ReceiptProvider.CLOUD);
            assertNull(ReceiptAiKey.load(context, ReceiptProvider.CLOUD));
            assertEquals("fake-gemini-key", ReceiptAiKey.load(context, ReceiptProvider.GEMINI));
        } finally {
            ReceiptAiKey.clear(context, ReceiptProvider.CLOUD);
            ReceiptAiKey.clear(context, ReceiptProvider.GEMINI);
        }
    }
}

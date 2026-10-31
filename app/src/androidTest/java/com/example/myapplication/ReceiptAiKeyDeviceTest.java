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
        } finally { ReceiptAiKey.clear(context); }
        assertNull(ReceiptAiKey.load(context));
    }
}

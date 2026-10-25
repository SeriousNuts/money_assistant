package com.example.myapplication;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class DemoLedgerTest {
    private Context context;
    private File file;
    @Before public void setup() {
        context = RuntimeEnvironment.getApplication();
        file = new File(context.getFilesDir(), "demo-ledger-v1.bin");
        if (file.exists() && !file.delete()) throw new AssertionError("test sandbox is not writable");
    }
    @Test public void examplePersistsAndDoesNotComeBackAfterDeletion() throws Exception {
        DemoLedger store = new DemoLedger(context);
        List<BillDocument> bills = store.read();
        assertEquals(1, bills.size());
        assertEquals(10000, bills.get(0).total());
        store.write(new java.util.ArrayList<>());
        assertTrue(new DemoLedger(context).read().isEmpty());
    }
    @Test public void paymentsSurviveReopening() throws Exception {
        DemoLedger store = new DemoLedger(context);
        List<BillDocument> bills = store.read();
        bills.get(0).people.get(0).paid = true;
        store.write(bills);
        assertTrue(new DemoLedger(context).read().get(0).people.get(0).paid);
    }
    @Test public void binaryLedgerFromPreviousApkMigrates() throws Exception {
        DemoLedger previous = new DemoLedger(context);
        Field field = DemoLedger.class.getDeclaredField("documents");
        field.setAccessible(true);
        Map<String, Map<String, Object>> documents = new LinkedHashMap<>();
        Map<String, Object> bill = new HashMap<>();
        bill.put("typo", "Chart"); bill.put("Chartname", "Старый счёт");
        documents.put("demo-dinner", bill);
        Map<String, Object> person = new HashMap<>();
        person.put("name", "Анна"); person.put("amountMinor", 1234L); person.put("paid", true);
        documents.put("demo-dinner_0", person);
        field.set(previous, documents);
        try (ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(file))) {
            stream.writeObject(previous);
        }
        List<BillDocument> restored = new DemoLedger(context).read();
        assertEquals(1, restored.size());
        assertEquals("demo-dinner", restored.get(0).id);
        assertTrue(restored.get(0).people.get(0).paid);
        assertEquals(1234L, restored.get(0).total());
        assertEquals(1, new DemoLedger(context).read().size());
    }
    @Test public void sessionUsesPreviousApkPreferenceName() {
        context.getSharedPreferences("demo_session", 0).edit().putBoolean("active", true).commit();
        assertTrue(DemoSession.active(context));
        DemoSession.set(context, false);
        assertFalse(DemoSession.active(context));
    }
}

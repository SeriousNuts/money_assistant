package com.example.myapplication;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ReceiptMigrationTest {
    private ReceiptLine line(long amount, int... assignees) {
        ReceiptLine item = new ReceiptLine();
        item.title = "Ужин";
        item.amount = amount;
        for (int index : assignees) item.assignees.add(index);
        return item;
    }

    @Test public void legacyLineAndPhotoMigrateWithoutChangingPaymentsOrDebt() throws Exception {
        String old = "{\"id\":\"dinner\",\"name\":\"Ужин\",\"people\":["
                + "{\"name\":\"Анна\",\"amountMinor\":51,\"paid\":true},"
                + "{\"name\":\"Борис\",\"amountMinor\":50,\"paid\":false}],"
                + "\"lines\":[{\"title\":\"Еда\",\"amountMinor\":101,\"assignees\":[0,1]}]}";
        BillDocument restored = BillDocument.fromJson(new JSONObject(old));
        assertEquals(1, restored.receipts.size());
        assertEquals("dinner.jpg", restored.receipts.get(0).photoFile);
        assertEquals(101, restored.total());
        assertEquals(51, restored.paid());
        assertEquals(0, restored.people.get(0).manualAmount);
        assertEquals(1, restored.lines.size());
        BillDocument reopened = BillDocument.fromJson(restored.toJson());
        assertEquals(1, reopened.receipts.size());
        assertEquals(1, reopened.lines.size());
        assertEquals(101, reopened.total());
        assertTrue(reopened.people.get(0).paid);
    }

    @Test public void twoReceiptsAddToManualSharesAndKeepPaidStatus() throws Exception {
        BillDocument bill = new BillDocument();
        bill.people.add(new BillDocument.Person("Анна", "", 200, true));
        bill.people.add(new BillDocument.Person("Борис", "", 300, false));
        bill.primaryReceipt().lines.add(line(101, 0, 1));
        ReceiptDocument second = ReceiptDocument.create(bill.id);
        second.name = "Десерт";
        second.lines.add(line(400, 1));
        bill.receipts.add(second);
        bill.recalculateAmounts();
        assertEquals(251, bill.people.get(0).amount);
        assertEquals(750, bill.people.get(1).amount);
        assertEquals(251, bill.paid());
        BillDocument restored = BillDocument.fromJson(bill.toJson());
        assertEquals(2, restored.receipts.size());
        assertEquals(1, restored.lines.size());
        assertEquals("Десерт", restored.receipts.get(1).name);
        assertFalse(restored.receipts.get(0).photoFile.equals(restored.receipts.get(1).photoFile));
        assertEquals(200, restored.people.get(0).manualAmount);
        assertEquals(300, restored.people.get(1).manualAmount);
        restored.receipts.get(1).lines.clear();
        restored.recalculateAmounts();
        assertEquals(601, restored.total());
        assertTrue(restored.people.get(0).paid);
    }

    @Test public void legacyManualMismatchPreservesOriginalAmounts() throws Exception {
        BillDocument legacy = new BillDocument();
        legacy.people.add(new BillDocument.Person("Анна", "", 500, true));
        legacy.lines.add(line(200, 0));
        JSONObject old = legacy.toJson();
        old.remove("receipts");
        old.getJSONArray("people").getJSONObject(0).remove("manualMinor");
        BillDocument restored = BillDocument.fromJson(old);
        assertEquals(500, restored.total());
        assertEquals(300, restored.people.get(0).manualAmount);
        restored.recalculateAmounts();
        assertEquals(500, restored.paid());
    }

    @Test public void invalidReceiptCannotChangeExistingAmounts() {
        BillDocument bill = new BillDocument();
        bill.people.add(new BillDocument.Person("Анна", "", 100, true));
        bill.primaryReceipt().lines.add(line(10, 0));
        ReceiptDocument bad = ReceiptDocument.create(bill.id);
        bad.lines.add(line(50, 9));
        bill.receipts.add(bad);
        try { bill.recalculateAmounts(); fail("invalid assignee accepted"); }
        catch (IllegalArgumentException expected) { }
        assertEquals(100, bill.people.get(0).amount);
        assertTrue(bill.people.get(0).paid);
    }

    @Test public void overflowNeverPartiallyChangesAnyParticipantOrPayment() {
        BillDocument bill = new BillDocument();
        bill.people.add(new BillDocument.Person("Анна", "", 100, true));
        bill.people.add(new BillDocument.Person("Борис", "", Money.MAX_MINOR, false));
        bill.primaryReceipt().lines.add(line(1, 0));
        bill.receipts.get(0).lines.add(line(1, 1));
        try { bill.recalculateAmounts(); fail("overflow accepted"); }
        catch (IllegalArgumentException expected) { }
        assertEquals(100, bill.people.get(0).amount);
        assertEquals(Money.MAX_MINOR, bill.people.get(1).amount);
        assertTrue(bill.people.get(0).paid);
    }

    @Test public void receiptPhotoMustBeAppPrivateFileName() throws Exception {
        try {
            ReceiptDocument.fromJson(new JSONObject("{\"id\":\"x\",\"photoFile\":\"../secret.jpg\"}"));
            fail("path traversal accepted");
        } catch (JSONException expected) { }
    }
}

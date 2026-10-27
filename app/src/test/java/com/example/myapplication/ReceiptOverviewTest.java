package com.example.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "w320dp-h640dp")
public class ReceiptOverviewTest {
    private Context context;
    private BillDocument bill;

    @Before public void setup() throws Exception {
        context = RuntimeEnvironment.getApplication();
        DemoSession.set(context, true);
        bill = new BillDocument();
        bill.id = "receipts-example";
        bill.name = "Ужин";
        bill.people.add(new BillDocument.Person("Анна", "", 1200, true));
        bill.people.add(new BillDocument.Person("Борис", "", 800, false));
        new DemoLedger(context).write(Collections.singletonList(bill));
    }

    private ReceiptLine line(String title, long amount, int... assignees) {
        ReceiptLine line = new ReceiptLine();
        line.title = title;
        line.amount = amount;
        for (int index : assignees) line.assignees.add(index);
        return line;
    }

    @Test public void twoReceiptsPersistWithSeparatePhotosAndDeletionKeepsManualDebtAndPayment() throws Exception {
        ReceiptOverviewActivity overview = Robolectric.buildActivity(ReceiptOverviewActivity.class,
                new Intent(context, ReceiptOverviewActivity.class).putExtra("bill", bill.id))
                .create().start().resume().get();
        BillDocument draft = BillDocument.fromJson(bill.toJson());
        draft.primaryReceipt().lines.add(line("Обед", 200, 0, 1));
        ReceiptDocument second = ReceiptDocument.create(bill.id);
        second.name = "Десерт";
        second.lines.add(line("Торт", 300, 1));
        draft.receipts.add(second);
        draft.recalculateAmounts();
        File stage = ReceiptPhotos.stage(context, second);
        try (FileOutputStream out = new FileOutputStream(stage)) { out.write(new byte[]{1, 2, 3}); }
        overview.onActivityResult(301, Activity.RESULT_OK, new Intent()
                .putExtra("draft", draft.toJson().toString()).putExtra("receiptId", second.id)
                .putExtra("stagedPhoto", stage.getPath()));
        BillDocument saved = new DemoLedger(context).read().get(0);
        assertEquals(2, saved.receipts.size());
        assertEquals(1300, saved.people.get(0).amount);
        assertEquals(1200, saved.people.get(1).amount);
        assertTrue(saved.people.get(0).paid);
        assertTrue(ReceiptPhotos.photo(context, second).isFile());
        assertFalse(stage.exists());

        Method confirm = ReceiptOverviewActivity.class.getDeclaredMethod("confirmDelete", ReceiptDocument.class);
        confirm.setAccessible(true);
        confirm.invoke(overview, second);
        ((AlertDialog) ShadowDialog.getLatestDialog()).getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        BillDocument after = new DemoLedger(context).read().get(0);
        assertEquals(1, after.receipts.size());
        assertEquals(1300, after.people.get(0).amount);
        assertEquals(900, after.people.get(1).amount);
        assertTrue(after.people.get(0).paid);
        assertFalse(ReceiptPhotos.photo(context, second).exists());
    }

    @Test public void positionsUseFilteredCompactListInsteadOfOneCardPerItem() throws Exception {
        bill.primaryReceipt();
        for (int i = 0; i < 150; i++) bill.lines.add(line("Позиция " + i, 10, 0));
        Intent intent = new Intent(context, ReceiptActivity.class)
                .putExtra("draft", bill.toJson().toString()).putExtra("receiptId", "legacy");
        ReceiptActivity editor = Robolectric.buildActivity(ReceiptActivity.class, intent).create().start().resume().get();
        View root = editor.findViewById(android.R.id.content);
        ListView positions = findList(root);
        assertNotNull(positions);
        assertEquals(150, positions.getAdapter().getCount());
        EditText search = findInput(root, "Поиск по позициям");
        search.setText("Позиция 149");
        assertEquals(1, positions.getAdapter().getCount());
    }

    @Test public void failedSaveKeepsOriginalPhotoAndOffersRetryWithoutDeletingDraft() throws Exception {
        ReceiptDocument receipt = bill.primaryReceipt();
        File old = ReceiptPhotos.photo(context, receipt);
        writeImage(old, android.graphics.Color.RED);
        byte[] before = Files.readAllBytes(old.toPath());
        new DemoLedger(context).write(Collections.singletonList(bill));
        ReceiptOverviewActivity overview = Robolectric.buildActivity(ReceiptOverviewActivity.class,
                new Intent(context, ReceiptOverviewActivity.class).putExtra("bill", bill.id))
                .create().start().resume().get();
        BillDocument draft = BillDocument.fromJson(bill.toJson());
        draft.lines.add(line("Торт", 100, 0));
        draft.recalculateAmounts();
        File stage = ReceiptPhotos.stage(context, receipt);
        writeImage(stage, android.graphics.Color.BLUE);
        // A payment on another screen makes this editor's revision stale.
        new BillRepository(context).mark(bill, 1, true, (v, error) -> assertEquals(null, error));
        overview.onActivityResult(301, Activity.RESULT_OK, new Intent()
                .putExtra("draft", draft.toJson().toString()).putExtra("receiptId", receipt.id)
                .putExtra("stagedPhoto", stage.getPath()));
        assertEquals(0, new DemoLedger(context).read().get(0).lines.size());
        assertTrue(new DemoLedger(context).read().get(0).people.get(1).paid);
        assertTrue(Arrays.equals(before, Files.readAllBytes(old.toPath())));
        assertTrue(stage.exists());
        assertNotNull(findText(overview.findViewById(android.R.id.content), "Повторить сохранение"));
    }

    @Test public void cancelledPhotoReplacementKeepsOldPhotoAndRotationKeepsDraft() throws Exception {
        ReceiptDocument receipt = bill.primaryReceipt();
        File old = ReceiptPhotos.photo(context, receipt);
        writeImage(old, android.graphics.Color.RED);
        byte[] before = Files.readAllBytes(old.toPath());
        Intent intent = new Intent(context, ReceiptActivity.class)
                .putExtra("draft", bill.toJson().toString()).putExtra("receiptId", receipt.id);
        ActivityController<ReceiptActivity> first = Robolectric.buildActivity(ReceiptActivity.class, intent)
                .create().start().resume();
        File next = new File(ReceiptPhotos.directory(context), "incoming-photo.jpg");
        writeImage(next, android.graphics.Color.BLUE);
        Method stage = ReceiptActivity.class.getDeclaredMethod("stagePhoto", File.class);
        stage.setAccessible(true);
        stage.invoke(first.get(), next);
        File staged = ReceiptPhotos.stage(context, receipt);
        assertTrue(staged.exists());
        assertTrue(Arrays.equals(before, Files.readAllBytes(old.toPath())));
        Bundle saved = new Bundle();
        first.pause().saveInstanceState(saved).stop().destroy();
        ActivityController<ReceiptActivity> second = Robolectric.buildActivity(ReceiptActivity.class, intent)
                .create(saved).start().resume();
        assertTrue(staged.exists());
        second.get().finish();
        second.pause().stop().destroy();
        assertFalse(staged.exists());
        assertTrue(Arrays.equals(before, Files.readAllBytes(old.toPath())));
    }

    private void writeImage(File path, int color) throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(color);
        try (FileOutputStream output = new FileOutputStream(path)) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output));
        }
        bitmap.recycle();
    }

    private ListView findList(View root) {
        if (root instanceof ListView) return (ListView) root;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                ListView found = findList(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private EditText findInput(View root, String hint) {
        if (root instanceof EditText && hint.equals(((EditText) root).getHint())) return (EditText) root;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                EditText found = findInput(group.getChildAt(i), hint);
                if (found != null) return found;
            }
        }
        return null;
    }

    private TextView findText(View root, String part) {
        if (root instanceof TextView && ((TextView) root).getText().toString().contains(part)) return (TextView) root;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = findText(group.getChildAt(i), part);
                if (found != null) return found;
            }
        }
        return null;
    }
}

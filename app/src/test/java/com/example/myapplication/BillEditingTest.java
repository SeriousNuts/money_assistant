package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "w320dp-h640dp")
public class BillEditingTest {
    private Context context;
    private BillRepository repository;
    private BillDocument original;

    @Before public void setup() throws Exception {
        context = RuntimeEnvironment.getApplication();
        DemoSession.set(context, true);
        original = new BillDocument();
        original.id = "editing-example";
        original.name = "Ужин";
        original.people.add(new BillDocument.Person("Анна", "", 1200, true));
        original.people.add(new BillDocument.Person("Михаил", "", 800, false));
        new DemoLedger(context).write(Collections.singletonList(original));
        repository = new BillRepository(context);
    }

    private BillDocument read() throws Exception { return new DemoLedger(context).read().get(0); }

    private Exception update(BillDocument draft) {
        AtomicReference<Exception> error = new AtomicReference<>();
        repository.update(draft, (v, e) -> error.set(e));
        return error.get();
    }

    @Test public void editPreservesPaidFlagsAndAppendsNewPersonAfterReopening() throws Exception {
        BillDocument draft = read();
        draft.name = "Другой ужин";
        draft.people.get(0).name = "Анна П.";
        draft.people.get(0).paid = false; // A stale editor must not undo an already recorded payment.
        draft.people.add(new BillDocument.Person("Сергей", "", 0, false));
        assertEquals(null, update(draft));
        BillDocument saved = read();
        assertEquals("Другой ужин", saved.name);
        assertEquals(3, saved.people.size());
        assertEquals(0, saved.people.get(2).amount);
        assertTrue(saved.people.get(0).paid);
        assertEquals(1200, saved.paid());
        assertEquals(1, saved.revision);
        saved.people.get(2).manualAmount = 375;
        saved.recalculateAmounts();
        assertEquals(null, update(saved));
        assertEquals(2375, read().total());
        assertTrue(read().people.get(0).paid);
    }

    @Test public void paymentWhileEditorOpenRejectsOutdatedEdit() throws Exception {
        BillDocument draft = read();
        AtomicReference<Exception> markError = new AtomicReference<>();
        repository.mark(read(), 1, true, (v, e) -> markError.set(e));
        assertEquals(null, markError.get());
        draft.name = "Устаревшее название";
        assertNotNull(update(draft));
        assertEquals("Ужин", read().name);
        assertTrue(read().people.get(1).paid);
        assertEquals(2, read().people.size());
    }

    @Test public void cannotReorderOrOverwriteExistingBillWithCreate() throws Exception {
        BillDocument draft = read();
        Collections.swap(draft.people, 0, 1);
        assertNotNull(update(draft));
        AtomicReference<Exception> error = new AtomicReference<>();
        repository.save(original, (v, e) -> error.set(e));
        assertNotNull(error.get());
        assertEquals(1, new DemoLedger(context).read().size());
        assertTrue(read().people.get(0).paid);
    }

    @Test public void editorAsksDebtForEachSelectedContact() throws Exception {
        Intent intent = new Intent(context, AfterContact.class).putExtra("editDraft", read().toJson().toString());
        AfterContact editor = Robolectric.buildActivity(AfterContact.class, intent).create().start().resume().get();
        View root = editor.findViewById(android.R.id.content);
        assertNotNull(findText(root, "Редактировать"));
        Intent contacts = new Intent()
                .putStringArrayListExtra(ContactSelectionActivity.NAMES,
                        new ArrayList<>(Arrays.asList("Борис", "Вера")))
                .putStringArrayListExtra(ContactSelectionActivity.PHONES,
                        new ArrayList<>(Arrays.asList("123", "456")));
        editor.onActivityResult(AfterContact.PICK_CONTACT, android.app.Activity.RESULT_OK, contacts);
        AlertDialog first = (AlertDialog) ShadowDialog.getLatestDialog();
        assertNotNull(findText(first.getWindow().getDecorView(), "Борис"));
        first.getButton(AlertDialog.BUTTON_NEUTRAL).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        AlertDialog second = (AlertDialog) ShadowDialog.getLatestDialog();
        assertNotNull(findText(second.getWindow().getDecorView(), "Вера"));
        findInput(second.getWindow().getDecorView(), "Сколько должен, ₽").setText("4,25");
        second.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        assertNotNull(findText(root, "Борис"));
        assertNotNull(findText(root, "Вера"));
        findText(root, "Сохранить изменения").performClick();
        BillDocument saved = read();
        assertEquals(4, saved.people.size());
        assertEquals(0, saved.people.get(2).amount);
        assertEquals(425, saved.people.get(3).amount);
        assertTrue(saved.people.get(0).paid);
        assertFalse(saved.people.get(3).paid);
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
}

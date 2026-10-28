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
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "w320dp-h640dp")
public class BillDraftStoreTest {
    private Context context;
    private BillDraftStore drafts;

    @Before public void setup() throws Exception {
        context = RuntimeEnvironment.getApplication();
        DemoSession.set(context, true);
        context.getSharedPreferences("unfinished-bills", Context.MODE_PRIVATE).edit().clear().commit();
        drafts = new BillDraftStore(context);
        new DemoLedger(context).write(Collections.emptyList());
    }

    @Test public void storesIncompleteBillSeparatelyAndRestoresFormAfterBack() throws Exception {
        ActivityController<AfterContact> controller = Robolectric.buildActivity(AfterContact.class).create().start().resume();
        AfterContact editor = controller.get();
        View root = editor.findViewById(android.R.id.content);
        findInput(root, "Название счёта").setText("Праздник");
        findInput(root, "0,00").setText("90,00");
        findText(root, "＋  Добавить").performClick();
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        findInput(dialog.getWindow().getDecorView(), "Имя участника").setText("Анна");
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        editor.finish();
        controller.pause().stop().destroy();
        assertEquals(1, drafts.list().size());
        assertTrue(new DemoLedger(context).read().isEmpty());

        StartMenu home = Robolectric.buildActivity(StartMenu.class).create().start().resume().get();
        assertNotNull(findText(home.findViewById(android.R.id.content), "Незавершённые счета"));
        findText(home.findViewById(android.R.id.content), "Продолжить").performClick();
        Intent resume = Shadows.shadowOf(home).getNextStartedActivity();
        AfterContact restored = Robolectric.buildActivity(AfterContact.class, resume).create().start().resume().get();
        View reopened = restored.findViewById(android.R.id.content);
        assertEquals("Праздник", findInput(reopened, "Название счёта").getText().toString());
        assertEquals("90,00", findInput(reopened, "0,00").getText().toString());
        assertNotNull(findText(reopened, "Анна"));
        findText(reopened, "Сохранить счёт").performClick();
        assertTrue(drafts.list().isEmpty());
        assertEquals(1, new DemoLedger(context).read().size());
    }

    @Test public void differentAccountDoesNotSeeDemoDraft() throws Exception {
        BillDocument bill = new BillDocument();
        drafts.save(new BillDraftStore.Draft(bill, "", false, null));
        DemoSession.set(context, false);
        assertTrue(new BillDraftStore(context).list().isEmpty());
        DemoSession.set(context, true);
        assertEquals(bill.id, new BillDraftStore(context).list().get(0).bill.id);
    }

    private TextView findText(View root, String text) {
        if (root instanceof TextView && ((TextView) root).getText().toString().contains(text)) return (TextView) root;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = findText(group.getChildAt(i), text);
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

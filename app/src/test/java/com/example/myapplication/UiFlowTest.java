package com.example.myapplication;

import android.content.Intent;
import android.app.Activity;
import android.widget.TextView;
import android.view.View;
import android.widget.EditText;
import android.os.Looper;

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

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "w320dp-h640dp")
public class UiFlowTest {
    @Before public void demo() throws Exception {
        DemoSession.set(RuntimeEnvironment.getApplication(), true);
        BillDocument sample = new BillDocument();
        sample.id = "demo-dinner";
        sample.name = "Ужин с друзьями · пример";
        for (long share : Money.split(10000, 3))
            sample.people.add(new BillDocument.Person("Участник", "", share, false));
        new DemoLedger(RuntimeEnvironment.getApplication()).write(Collections.singletonList(sample));
    }

    @Test public void homeShowsLocalAccountsOnCompactDisplay() {
        StartMenu home = Robolectric.buildActivity(StartMenu.class).create().start().resume().get();
        android.view.View root = home.findViewById(android.R.id.content);
        assertNotNull(root);
        assertNotNull(findText(root, "Общие счета"));
        assertNotNull(findText(root, "Новый счёт"));
        assertNotNull(findText(root, "В процессе"));
        assertNotNull(findText(root, "Закрытые"));
    }

    @Test public void editorOffersReceiptAndEqualSplitWithoutFirebase() {
        AfterContact editor = Robolectric.buildActivity(AfterContact.class).create().start().resume().get();
        android.view.View root = editor.findViewById(android.R.id.content);
        assertNotNull(findText(root, "Фото и разметка чека"));
        assertNotNull(findText(root, "Разделить поровну"));
    }

    @Test public void filtersKeepBillsOutOfClosedAndRestoreOnAll() {
        StartMenu home = Robolectric.buildActivity(StartMenu.class).create().start().resume().get();
        View root = home.findViewById(android.R.id.content);
        assertNotNull(findText(root, "Ужин с друзьями"));
        findText(root, "Закрытые").performClick();
        assertNotNull(findText(root, "Нет счетов для выбранного фильтра"));
        findText(root, "Все").performClick();
        assertNotNull(findText(root, "Ужин с друзьями"));
    }

    @Test public void editingParticipantsPreservesBillTitle() {
        AfterContact editor = Robolectric.buildActivity(AfterContact.class).create().start().resume().get();
        View root = editor.findViewById(android.R.id.content);
        EditText title = findInput(root, "Название счёта");
        title.setText("Пятничный ужин");
        findText(root, "Добавить").performClick();
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        findInput(dialog.getWindow().getDecorView(), "Имя участника").setText("Анна");
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertNotNull(findText(root, "Анна"));
        assertEquals("Пятничный ужин", title.getText().toString());
    }

    @Test public void multipleContactsAreAddedTogetherWithoutDuplicatingExistingPeople() {
        AfterContact editor = Robolectric.buildActivity(AfterContact.class).create().start().resume().get();
        View root = editor.findViewById(android.R.id.content);
        findInput(root, "Название счёта").setText("Ужин");
        findInput(root, "0,00").setText("90,00");
        Intent selection = new Intent()
                .putStringArrayListExtra(ContactSelectionActivity.NAMES,
                        new java.util.ArrayList<>(java.util.Arrays.asList("Анна", "Михаил")))
                .putStringArrayListExtra(ContactSelectionActivity.PHONES,
                        new java.util.ArrayList<>(java.util.Arrays.asList("+79991111111", "+79992222222")));
        editor.onActivityResult(AfterContact.PICK_CONTACT, Activity.RESULT_OK, selection);
        assertNotNull(findText(root, "Анна"));
        assertNotNull(findText(root, "Михаил"));
        assertNotNull(findText(root, "Участников: 2 · Доли: 90,00 ₽"));
        assertEquals("Ужин", findInput(root, "Название счёта").getText().toString());
        editor.onActivityResult(AfterContact.PICK_CONTACT, Activity.RESULT_OK, selection);
        assertNotNull(findText(root, "Участников: 2 · Доли: 90,00 ₽"));
    }

    @Test public void receiptEditorAcceptsDraftWithoutPhoto() throws Exception {
        BillDocument bill = new BillDocument();
        bill.people.add(new BillDocument.Person("Анна", "", 0, false));
        Intent intent = new Intent(RuntimeEnvironment.getApplication(), ReceiptActivity.class)
                .putExtra("draft", bill.toJson().toString());
        ReceiptActivity activity = Robolectric.buildActivity(ReceiptActivity.class, intent)
                .create().start().resume().get();
        assertNotNull(findText(activity.findViewById(android.R.id.content), "Сфотографировать чек"));
    }

    @Test @Config(sdk = 28, qualifiers = "w393dp-h852dp-night")
    public void darkThemeUsesReferencePalette() {
        StartMenu home = Robolectric.buildActivity(StartMenu.class).create().start().resume().get();
        assertTrue(Ui.dark(home));
        assertEquals(0xff1d1e1b, Ui.background(home));
        assertEquals(0xff282925, Ui.surface(home));
    }

    private TextView findText(android.view.View root, String part) {
        if (root instanceof TextView && ((TextView) root).getText().toString().contains(part)) return (TextView) root;
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = findText(group.getChildAt(i), part);
                if (found != null) return found;
            }
        }
        return null;
    }

    private EditText findInput(android.view.View root, String hint) {
        if (root instanceof EditText && hint.equals(((EditText) root).getHint())) return (EditText) root;
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                EditText found = findInput(group.getChildAt(i), hint);
                if (found != null) return found;
            }
        }
        return null;
    }
}

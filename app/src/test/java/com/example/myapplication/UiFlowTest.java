package com.example.myapplication;

import android.content.Intent;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "w320dp-h640dp")
public class UiFlowTest {
    @Before public void demo() { DemoSession.set(RuntimeEnvironment.getApplication(), true); }

    @Test public void homeShowsLocalAccountsOnCompactDisplay() {
        StartMenu home = Robolectric.buildActivity(StartMenu.class).create().start().resume().get();
        android.view.View root = home.findViewById(android.R.id.content);
        assertNotNull(root);
        assertNotNull(findText(root, "Ваши счета"));
        assertNotNull(findText(root, "Создать счёт"));
    }

    @Test public void editorOffersReceiptAndEqualSplitWithoutFirebase() {
        AfterContact editor = Robolectric.buildActivity(AfterContact.class).create().start().resume().get();
        android.view.View root = editor.findViewById(android.R.id.content);
        assertNotNull(findText(root, "Фото и разметка чека"));
        assertNotNull(findText(root, "Разделить поровну"));
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
}

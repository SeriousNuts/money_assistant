package com.example.myapplication;

import android.database.MatrixCursor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ContactDirectoryTest {
    @Test public void includesContactsWithoutPhonesAndKeepsOneRowPerPerson() {
        MatrixCursor people = new MatrixCursor(new String[]{"_id", "display_name"});
        people.addRow(new Object[]{10, " Анна "});
        people.addRow(new Object[]{11, "Михаил"});
        people.addRow(new Object[]{12, "  "});
        MatrixCursor phones = new MatrixCursor(new String[]{"contact_id", "number"});
        phones.addRow(new Object[]{10, "+7 (999) 111-22-33"});
        phones.addRow(new Object[]{10, "89001112233"});
        List<ContactDirectory.Contact> found = ContactDirectory.merge(people, phones);
        assertEquals(2, found.size());
        assertEquals(10, found.get(0).id);
        assertEquals("Анна", found.get(0).name);
        assertEquals("+7 (999) 111-22-33", found.get(0).phone);
        assertEquals("", found.get(1).phone);
        assertTrue(found.get(0).matches("АнН"));
        assertTrue(found.get(0).matches("999111"));
        assertTrue(found.get(1).matches("мих"));
        assertFalse(found.get(1).matches("999"));
    }
}

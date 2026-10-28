package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Real native OCR, not a mock: ensures the bundled Russian model loads on Android. */
@RunWith(AndroidJUnit4.class)
public class ReceiptOcrDeviceTest {
    @Test public void readsRussianProductAndIgnoresSummary() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();
        Bitmap image = Bitmap.createBitmap(1100, 400, Bitmap.Config.ARGB_8888);
        image.eraseColor(Color.WHITE);
        Canvas canvas = new Canvas(image);
        Paint print = new Paint(Paint.ANTI_ALIAS_FLAG);
        print.setColor(Color.BLACK);
        print.setTypeface(android.graphics.Typeface.MONOSPACE);
        print.setTextSize(55);
        canvas.drawText("МОЛОКО", 40, 125, print);
        canvas.drawText("79,50", 760, 125, print);
        canvas.drawText("ИТОГО", 40, 260, print);
        canvas.drawText("79,50", 760, 260, print);
        try {
            List<ReceiptOcrParser.Fragment> fragments = ReceiptOcrEngine.recognize(context, image);
            List<ReceiptLine> found = ReceiptOcrParser.parse(fragments, image.getWidth(), image.getHeight());
            StringBuilder debug = new StringBuilder();
            for (ReceiptOcrParser.Fragment fragment : fragments)
                debug.append('[').append(fragment.text).append(" @ ").append(fragment.confidence).append("] ");
            assertEquals(debug.toString(), 1, found.size());
            assertTrue(found.get(0).title.contains("МОЛОКО"));
            assertEquals(7950, found.get(0).amount);
        } finally { image.recycle(); }
    }
}

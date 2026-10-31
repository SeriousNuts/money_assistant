package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.json.JSONObject;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/** Optional local-only OCR check. Photos must be staged privately on the emulator, never in Git/assets. */
@RunWith(AndroidJUnit4.class)
public class ReceiptPrivateSampleDeviceTest {
    @Test public void inspectLocalSamplesWithoutLoggingReceiptText() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();
        File folder = new File(context.getFilesDir(), "private-ocr");
        assumeTrue(new File(folder, "sample-1.jpg").isFile()
                && new File(folder, "sample-2.jpg").isFile());
        for (int i = 1; i <= 2; i++) {
            Bitmap bitmap = BitmapFactory.decodeFile(new File(folder, "sample-" + i + ".jpg").getAbsolutePath());
            assertNotNull(bitmap);
            try {
                JSONObject request = GeminiReceiptClient.request(bitmap); // Serialize actual photo locally, no network.
                JSONObject image = request.getJSONArray("contents").getJSONObject(0)
                        .getJSONArray("parts").getJSONObject(1).getJSONObject("inline_data");
                assertEquals("image/jpeg", image.getString("mime_type"));
                assertTrue(image.getString("data").length() > 100);
                inspect(context, bitmap, i + "-full");
                int left = Math.round(bitmap.getWidth() * (i == 1 ? .12f : .16f));
                int top = Math.round(bitmap.getHeight() * (i == 1 ? .36f : .48f));
                int right = Math.round(bitmap.getWidth() * .87f);
                int bottom = Math.round(bitmap.getHeight() * (i == 1 ? .67f : .67f));
                Bitmap crop = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top);
                try { inspect(context, crop, i + "-crop"); }
                finally { crop.recycle(); }
            } finally { bitmap.recycle(); }
        }
    }

    private static void inspect(Context context, Bitmap photo, String name) throws Exception {
        List<ReceiptOcrParser.Fragment> fragments = ReceiptOcrEngine.recognize(context, photo);
        List<ReceiptLine> lines = ReceiptOcrParser.parse(fragments, photo.getWidth(), photo.getHeight());
        StringBuilder amounts = new StringBuilder();
        for (ReceiptLine line : lines) {
            if (amounts.length() > 0) amounts.append(',');
            amounts.append(line.amount); // minor currency units only; never print private text/QR/IDs
        }
        System.out.println("LOCAL_OCR sample=" + name + " fragments=" + fragments.size()
                + " suggestions=" + lines.size() + " amountsMinor=" + amounts);
    }
}

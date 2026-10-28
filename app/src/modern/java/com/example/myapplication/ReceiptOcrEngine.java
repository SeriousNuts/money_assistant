package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;

import com.googlecode.tesseract.android.ResultIterator;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Runs offline on a worker thread. Ships Russian and English language data in the APK. */
final class ReceiptOcrEngine {
    private static final String[] LANGUAGES = {"rus", "eng"};
    private static final long[] MODEL_SIZES = {3861738, 4113088};

    static List<ReceiptOcrParser.Fragment> recognize(Context context, Bitmap photo) throws Exception {
        File home = new File(context.getFilesDir(), "tesseract");
        File data = new File(home, "tessdata");
        if (!data.exists() && !data.mkdirs()) throw new java.io.IOException("Не удалось подготовить OCR");
        for (int i = 0; i < LANGUAGES.length; i++) {
            File model = new File(data, LANGUAGES[i] + ".traineddata");
            if (model.length() == MODEL_SIZES[i]) continue;
            File temporary = new File(data, LANGUAGES[i] + ".tmp-" + java.util.UUID.randomUUID());
            try {
                try (InputStream input = context.getAssets().open("tessdata/" + LANGUAGES[i] + ".traineddata");
                     FileOutputStream output = new FileOutputStream(temporary)) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                    output.getFD().sync();
                }
                Files.move(temporary.toPath(), model.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } finally {
                if (temporary.exists() && !temporary.delete())
                    android.util.Log.w("ReceiptOcrEngine", "Could not remove partial OCR model");
            }
        }

        Bitmap prepared = prepare(photo);
        TessBaseAPI tess = new TessBaseAPI();
        try {
            if (!tess.init(home.getAbsolutePath(), "rus+eng"))
                throw new IllegalStateException("Не удалось загрузить русский OCR");
            tess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);
            tess.setImage(prepared);
            tess.getUTF8Text(); // Start recognition before requesting line positions.
            ResultIterator iterator = tess.getResultIterator();
            List<ReceiptOcrParser.Fragment> rows = new ArrayList<>();
            if (iterator != null) try {
                iterator.begin();
                do {
                    String text = iterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE);
                    Rect box = iterator.getBoundingRect(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE);
                    float confidence = iterator.confidence(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE);
                    if (text != null && box != null && confidence >= 38)
                        rows.add(new ReceiptOcrParser.Fragment(text,
                                Math.round(box.left * (float) photo.getWidth() / prepared.getWidth()),
                                Math.round(box.top * (float) photo.getHeight() / prepared.getHeight()),
                                Math.round(box.right * (float) photo.getWidth() / prepared.getWidth()),
                                Math.round(box.bottom * (float) photo.getHeight() / prepared.getHeight()),
                                confidence));
                } while (iterator.next(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE));
            } finally { iterator.delete(); }
            return rows;
        } finally {
            tess.recycle();
            prepared.recycle();
        }
    }

    private static Bitmap prepare(Bitmap original) {
        float scale = Math.min(2f, Math.min(2000f / original.getWidth(), 2800f / original.getHeight()));
        int width = Math.max(1, Math.round(original.getWidth() * scale));
        int height = Math.max(1, Math.round(original.getHeight() * scale));
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        ColorMatrix grayscale = new ColorMatrix();
        grayscale.setSaturation(0);
        // Improve thermal-print contrast without throwing away faint gray letters.
        ColorMatrix contrast = new ColorMatrix(new float[]{
                1.35f, 0, 0, 0, -32, 0, 1.35f, 0, 0, -32,
                0, 0, 1.35f, 0, -32, 0, 0, 0, 1, 0});
        grayscale.postConcat(contrast);
        paint.setColorFilter(new ColorMatrixColorFilter(grayscale));
        new Canvas(result).drawBitmap(original, null, new Rect(0, 0, width, height), paint);
        return result;
    }

    private ReceiptOcrEngine() { }
}

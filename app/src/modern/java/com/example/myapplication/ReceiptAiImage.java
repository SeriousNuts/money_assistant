package com.example.myapplication;

import android.graphics.Bitmap;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Limit each explicitly submitted image before encoding it for either provider. */
final class ReceiptAiImage {
    static String jpegBase64(Bitmap bitmap) throws IOException {
        if (bitmap == null || bitmap.isRecycled() || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0)
            throw new IOException("Фото недоступно");
        float scale = Math.min(1f, Math.min(2000f / bitmap.getWidth(), 2800f / bitmap.getHeight()));
        Bitmap smaller = scale < 1 ? Bitmap.createScaledBitmap(bitmap,
                Math.max(1, Math.round(bitmap.getWidth() * scale)),
                Math.max(1, Math.round(bitmap.getHeight() * scale)), true) : bitmap;
        ByteArrayOutputStream image = new ByteArrayOutputStream();
        try {
            if (!smaller.compress(Bitmap.CompressFormat.JPEG, 82, image))
                throw new IOException("Не удалось подготовить фото");
        } finally { if (smaller != bitmap) smaller.recycle(); }
        if (image.size() > 4 * 1024 * 1024) throw new IOException("Фото слишком большое для отправки");
        return Base64.encodeToString(image.toByteArray(), Base64.NO_WRAP);
    }

    private ReceiptAiImage() { }
}

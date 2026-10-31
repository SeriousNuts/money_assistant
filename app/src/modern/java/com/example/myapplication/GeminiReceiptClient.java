package com.example.myapplication;

import android.graphics.Bitmap;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Explicit, user-initiated cloud recognition. Never called during offline photo capture. */
final class GeminiReceiptClient {
    private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent";
    private static final String PROMPT = "Извлеки из фотографии кассового чека только реально видимые товары или услуги "
            + "и итоговую стоимость КАЖДОЙ позиции в рублях. Читай кириллицу. "
            + "Если количество больше одного, возьми стоимость всей позиции, а не цену за единицу. "
            + "Не добавляй ИТОГО, налог, скидку, сдачу, адрес, банковскую операцию или выдуманные товары. "
            + "Если текст неразборчив, пропусти позицию. Для каждой позиции верни name и price как строку "
            + "в рублях с двумя знаками после запятой. Не считай сумму чека позицией.";

    static List<ReceiptLine> recognize(String apiKey, Bitmap bitmap) throws Exception {
        return recognize(apiKey, bitmap, new URL(ENDPOINT));
    }

    static List<ReceiptLine> recognize(String apiKey, Bitmap bitmap, URL endpoint) throws Exception {
        if (apiKey == null || apiKey.trim().isEmpty()) throw new IllegalArgumentException("Нужен API-ключ Gemini");
        HttpURLConnection connection = (HttpURLConnection) endpoint.openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("x-goog-api-key", apiKey);
        try {
            byte[] request = request(bitmap).toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(request.length);
            try (OutputStream stream = connection.getOutputStream()) { stream.write(request); }
            int status = connection.getResponseCode();
            if (status == 429) throw new IOException("Бесплатный лимит Gemini исчерпан. Повторите позже или используйте офлайн-режим.");
            if (status == 401 || status == 403) throw new IOException("Проверьте API-ключ Gemini и доступность модели для вашего аккаунта.");
            if (status != 200) throw new IOException("Gemini недоступен (HTTP " + status + "). Повторите позже или используйте офлайн-режим.");
            try (InputStream stream = connection.getInputStream()) {
                ByteArrayOutputStream response = new ByteArrayOutputStream();
                byte[] chunk = new byte[8192];
                int count;
                while ((count = stream.read(chunk)) != -1) {
                    if (response.size() + count > 1024 * 1024) throw new IOException("Ответ Gemini слишком большой");
                    response.write(chunk, 0, count);
                }
                return parseResponse(new JSONObject(response.toString("UTF-8")));
            }
        } finally { connection.disconnect(); }
    }

    static JSONObject request(Bitmap bitmap) throws Exception {
        int width = bitmap.getWidth(), height = bitmap.getHeight();
        float scale = Math.min(1f, Math.min(2000f / width, 2800f / height));
        Bitmap smaller = scale < 1 ? Bitmap.createScaledBitmap(bitmap,
                Math.max(1, Math.round(width * scale)), Math.max(1, Math.round(height * scale)), true) : bitmap;
        ByteArrayOutputStream image = new ByteArrayOutputStream();
        try {
            if (!smaller.compress(Bitmap.CompressFormat.JPEG, 82, image)) throw new IOException("Не удалось подготовить фото");
        } finally { if (smaller != bitmap) smaller.recycle(); }
        if (image.size() > 4 * 1024 * 1024) throw new IOException("Фото слишком большое для отправки");
        JSONObject photo = new JSONObject().put("mime_type", "image/jpeg")
                .put("data", Base64.encodeToString(image.toByteArray(), Base64.NO_WRAP));
        JSONObject item = new JSONObject().put("type", "OBJECT").put("properties", new JSONObject()
                .put("name", new JSONObject().put("type", "STRING"))
                .put("price", new JSONObject().put("type", "STRING")))
                .put("required", new JSONArray().put("name").put("price"));
        JSONObject schema = new JSONObject().put("type", "OBJECT")
                .put("properties", new JSONObject().put("items", new JSONObject().put("type", "ARRAY").put("items", item)))
                .put("required", new JSONArray().put("items"));
        return new JSONObject().put("contents", new JSONArray().put(new JSONObject()
                        .put("parts", new JSONArray().put(new JSONObject().put("text", PROMPT))
                                .put(new JSONObject().put("inline_data", photo)))))
                .put("generationConfig", new JSONObject().put("responseMimeType", "application/json")
                        .put("responseSchema", schema).put("maxOutputTokens", 8192));
    }

    static List<ReceiptLine> parseResponse(JSONObject response) throws Exception {
        JSONArray candidates = response.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) throw new IOException("Gemini не вернул позиций. Проверьте фото.");
        JSONArray parts = candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts");
        for (int i = 0; i < parts.length(); i++) {
            String text = parts.getJSONObject(i).optString("text", "");
            if (!text.trim().isEmpty()) return parseItems(new JSONObject(text));
        }
        throw new IOException("Gemini не вернул текст. Попробуйте другое фото.");
    }

    static List<ReceiptLine> parseItems(JSONObject json) throws Exception {
        List<ReceiptLine> lines = new ArrayList<>();
        JSONArray items = json.getJSONArray("items");
        for (int i = 0; i < items.length() && lines.size() < 300; i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            String name = item.optString("name", "").trim().replaceAll("\\s+", " ");
            if (name.length() < 2 || name.length() > 80 || name.matches("(?iu)^(итог|всего|сумма|ндс|скидка|сдача|оплата|total|tax|change|discount).*")) continue;
            long price;
            try { price = Money.parse(item.optString("price", "")); }
            catch (IllegalArgumentException ignored) { continue; }
            if (price <= 0) continue;
            ReceiptLine line = new ReceiptLine();
            line.title = name;
            line.amount = price;
            // Cloud model returns an item list, not reliable image coordinates. Edit via the list.
            line.autoDetected = true;
            lines.add(line);
        }
        return lines;
    }

    private GeminiReceiptClient() { }
}

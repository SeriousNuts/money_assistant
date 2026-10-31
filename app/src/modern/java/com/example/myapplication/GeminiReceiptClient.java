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
            + "Если на фото видны и кассовый чек, и платёжный слип терминала, учитывай только товары кассового чека; "
            + "не дублируй суммы из слипа. Номер заказа и строки НДС не являются товарами. "
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
            if (status != 200) throw apiError(status, connection.getErrorStream());
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

    /** Return a useful diagnosis without ever showing the server's raw response (which may contain secrets). */
    static IOException apiError(int status, InputStream errorStream) {
        String category = "", reason = "", detail = "";
        if (errorStream != null) try (InputStream stream = errorStream) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int length;
            while (bytes.size() < 16384 && (length = stream.read(chunk, 0, Math.min(chunk.length, 16384 - bytes.size()))) != -1)
                bytes.write(chunk, 0, length);
            JSONObject error = new JSONObject(bytes.toString("UTF-8")).optJSONObject("error");
            if (error != null) {
                category = error.optString("status", "");
                detail = error.optString("message", "");
                JSONArray details = error.optJSONArray("details");
                if (details != null) for (int i = 0; i < details.length(); i++) {
                    JSONObject entry = details.optJSONObject(i);
                    if (entry != null && entry.has("reason")) { reason = entry.optString("reason"); break; }
                }
            }
        } catch (Exception ignored) { /* Never expose raw error bodies, request data or API keys. */ }
        if ("API_KEY_INVALID".equals(reason) || "API_KEY_INVALID".equals(category)
                || detail.contains("API key not valid"))
            return new IOException("Gemini отклонил API-ключ (HTTP " + status
                    + "). Отзовите опубликованный ключ и создайте новый в Google AI Studio.");
        if (status == 400 && "FAILED_PRECONDITION".equals(category))
            return new IOException("Бесплатный тариф Gemini недоступен для этого проекта или региона (HTTP 400). Проверьте условия в AI Studio.");
        if (status == 429) return new IOException("Лимит Gemini исчерпан (HTTP 429). Повторите позже или используйте офлайн-режим.");
        if (status == 401 || status == 403)
            return new IOException("Нет доступа к Gemini (HTTP " + status + "). Проверьте ключ и настройки проекта в AI Studio.");
        if (status == 404)
            return new IOException("Выбранная модель Gemini недоступна проекту (HTTP 404). Нужна проверка доступных моделей.");
        if (status == 400 && "INVALID_ARGUMENT".equals(category))
            return new IOException("Gemini отклонил запрос (HTTP 400, INVALID_ARGUMENT). "
                    + (detail.contains("responseSchema") || detail.contains("response_schema")
                    ? "Сервер не принял формат ответа модели. Обновите приложение."
                    : "Возможна ошибка ключа или параметров модели. Проверьте доступ в AI Studio."));
        return new IOException("Gemini недоступен (HTTP " + status + "). Повторите позже или используйте офлайн-режим.");
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
            if (name.length() < 2 || name.length() > 80 || name.matches("(?iu)^(итог|всего|сумма|ндс|скидка|сдача|оплата|под сбербанк|терминал|комиссия|плат[её]ж|номер заказа|заказ в пакете|total|tax|change|discount).*")) continue;
            int letters = 0;
            for (int j = 0; j < name.length(); j++) if (Character.isLetter(name.charAt(j))) letters++;
            if (letters < 2) continue;
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

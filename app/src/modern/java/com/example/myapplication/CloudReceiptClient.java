package com.example.myapplication;

import android.graphics.Bitmap;

import org.json.JSONArray;
import org.json.JSONException;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Cloud.ru Foundation Models: explicitly submitted photo -> Markdown -> reviewable suggestions. */
final class CloudReceiptClient {
    private static final String ENDPOINT = "https://foundation-models.api.cloud.ru/v1/chat/completions";
    private static final String MODEL = "deepseek-ai/DeepSeek-OCR-2";
    private static final String PROMPT = "<image><|grounding|>Convert the document to markdown.";
    private static final Pattern PRICE = Pattern.compile("(?<![\\p{L}\\d])(?:\\d{1,7}|\\d{1,3}(?:[ \\u00a0\\u202f]\\d{3})+)[,.]\\d{2}(?!\\d)");

    static List<ReceiptLine> recognize(String apiKey, Bitmap bitmap) throws Exception {
        return recognize(apiKey, bitmap, new URL(ENDPOINT));
    }

    static List<ReceiptLine> recognize(String apiKey, Bitmap bitmap, URL endpoint) throws Exception {
        if (apiKey == null || apiKey.trim().isEmpty())
            throw new IllegalArgumentException("Нужен API-ключ Cloud.ru");
        HttpURLConnection connection = (HttpURLConnection) endpoint.openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
        try {
            byte[] request = request(bitmap).toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(request.length);
            try (OutputStream stream = connection.getOutputStream()) { stream.write(request); }
            int status = connection.getResponseCode();
            if (status != 200) throw apiError(status);
            try (InputStream stream = connection.getInputStream()) {
                ByteArrayOutputStream response = new ByteArrayOutputStream();
                byte[] chunk = new byte[8192];
                int count;
                while ((count = stream.read(chunk)) != -1) {
                    if (response.size() + count > 1024 * 1024) throw new IOException("Ответ Cloud.ru слишком большой");
                    response.write(chunk, 0, count);
                }
                try { return parseResponse(new JSONObject(response.toString("UTF-8"))); }
                catch (JSONException error) { throw new IOException("Некорректный ответ Cloud.ru", error); }
            }
        } finally { connection.disconnect(); }
    }

    static JSONObject request(Bitmap bitmap) throws Exception {
        JSONObject image = new JSONObject().put("url", "data:image/jpeg;base64," + ReceiptAiImage.jpegBase64(bitmap));
        JSONArray content = new JSONArray().put(new JSONObject().put("type", "text").put("text", PROMPT))
                .put(new JSONObject().put("type", "image_url").put("image_url", image));
        return new JSONObject().put("model", MODEL).put("max_tokens", 2500).put("temperature", 0.5)
                .put("messages", new JSONArray().put(new JSONObject().put("role", "user").put("content", content)));
    }

    static List<ReceiptLine> parseResponse(JSONObject response) throws Exception {
        JSONArray choices = response.optJSONArray("choices");
        if (choices == null || choices.length() == 0)
            throw new IOException("Cloud.ru не вернул результат распознавания");
        JSONObject message = choices.optJSONObject(0) == null ? null : choices.optJSONObject(0).optJSONObject("message");
        Object content = message == null ? null : message.opt("content");
        String markdown = content instanceof String ? (String) content : "";
        if (markdown.trim().isEmpty()) throw new IOException("Cloud.ru не вернул текст чека");
        return parseMarkdown(markdown);
    }

    /** Only rows with explicit kopecks are suggested; ambiguous OCR is left for manual entry. */
    static List<ReceiptLine> parseMarkdown(String markdown) throws Exception {
        List<ReceiptLine> suggestions = new ArrayList<>();
        String[] rows = markdown.split("\\r?\\n");
        for (String row : rows) {
            String normalized = row.trim();
            if (normalized.isEmpty() || normalized.startsWith("```") || normalized.startsWith("#")) continue;
            String name, price;
            if (normalized.startsWith("|") && normalized.endsWith("|")) {
                String[] columns = normalized.substring(1, normalized.length() - 1).split("\\|", -1);
                if (columns.length < 2) continue;
                name = columns[0].trim();
                if (name.matches("\\d+[.)]?") && columns.length > 2) name = columns[1].trim();
                price = columns[columns.length - 1].trim();
            } else {
                Matcher amount = PRICE.matcher(normalized);
                int start = -1;
                price = "";
                while (amount.find()) { start = amount.start(); price = amount.group(); }
                if (start < 0) continue;
                name = normalized.substring(0, start).replaceFirst("^[*+\\-\\s]*(?:\\d+[.)]\\s*)?", "").trim();
            }
            Matcher amount = PRICE.matcher(price);
            String value = null;
            while (amount.find()) value = amount.group();
            if (value == null) continue;
            name = name.replaceAll("^[\\s*#_`|]+|[\\s*#_`|:·.\\-–—]+$", "").trim();
            if (name.matches("(?iu)^(итог|всего|сумма|ндс|инн|чек|кассир|телефон|дата|время|фискальн|фн|фд|фпд|оплачено|total|tax|order|payment).*")) continue;
            if (name.length() < 2) continue;
            suggestions.addAll(GeminiReceiptClient.parseItems(new JSONObject().put("items", new JSONArray()
                    .put(new JSONObject().put("name", name).put("price", value.replaceAll("[ \\u00a0\\u202f]", ""))))));
            if (suggestions.size() >= 300) break;
        }
        return suggestions;
    }

    static IOException apiError(int status) {
        if (status == 401 || status == 403)
            return new IOException("Cloud.ru отклонил ключ или доступ к модели (HTTP " + status + "). Проверьте ключ и права проекта.");
        if (status == 429)
            return new IOException("Лимит Cloud.ru исчерпан (HTTP 429). Повторите позже или используйте офлайн-режим.");
        return new IOException("Cloud.ru недоступен (HTTP " + status + "). Проверьте доступность модели или используйте офлайн-режим.");
    }

    private CloudReceiptClient() { }
}

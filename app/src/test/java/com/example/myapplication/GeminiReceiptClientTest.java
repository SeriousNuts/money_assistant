package com.example.myapplication;

import android.graphics.Bitmap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class GeminiReceiptClientTest {
    @Test public void requestSendsJpegAndAsksForStructuredItems() throws Exception {
        Bitmap photo = Bitmap.createBitmap(40, 80, Bitmap.Config.ARGB_8888);
        JSONObject request = GeminiReceiptClient.request(photo);
        JSONArray parts = request.getJSONArray("contents").getJSONObject(0).getJSONArray("parts");
        assertTrue(parts.getJSONObject(0).getString("text").contains("кассового чека"));
        JSONObject data = parts.getJSONObject(1).getJSONObject("inline_data");
        assertEquals("image/jpeg", data.getString("mime_type"));
        assertTrue(data.getString("data").length() > 30);
        assertEquals("application/json", request.getJSONObject("generationConfig").getString("responseMimeType"));
        photo.recycle();
    }

    @Test public void rejectsTotalsAndInvalidPricesButPreservesItemsForReview() throws Exception {
        JSONObject items = new JSONObject().put("items", new JSONArray()
                .put(new JSONObject().put("name", "Молоко 2 шт").put("price", "159,00"))
                .put(new JSONObject().put("name", "ИТОГО").put("price", "159,00"))
                .put(new JSONObject().put("name", "Неясная цена").put("price", "не видно"))
                .put(new JSONObject().put("name", "Хлеб").put("price", "49.90")));
        List<ReceiptLine> result = GeminiReceiptClient.parseItems(items);
        assertEquals(2, result.size());
        assertEquals("Молоко 2 шт", result.get(0).title);
        assertEquals(15900L, result.get(0).amount);
        assertEquals(4990L, result.get(1).amount);
        assertTrue(result.get(0).autoDetected);
        assertTrue(result.get(0).assignees.isEmpty());
        assertEquals(0f, result.get(0).right, 0f);
    }

    @Test public void parsesGeminiResponseWithoutSendingAnything() throws Exception {
        JSONObject part = new JSONObject().put("text", "{\"items\":[{\"name\":\"Чай\",\"price\":\"100.00\"}]}");
        JSONObject candidate = new JSONObject().put("content", new JSONObject().put("parts", new JSONArray().put(part)));
        JSONObject response = new JSONObject().put("candidates", new JSONArray().put(candidate));
        assertEquals(10000L, GeminiReceiptClient.parseResponse(response).get(0).amount);
    }

    @Test public void postsImageWithKeyHeaderAndReadsModelResponse() throws Exception {
        String json = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"items\\\":[{\\\"name\\\":\\\"Кофе\\\",\\\"price\\\":\\\"129.50\\\"}]}\"}]}}]}";
        ByteArrayOutputStream sent = new ByteArrayOutputStream();
        AtomicReference<HttpURLConnection> connection = new AtomicReference<>();
        URL endpoint = new URL(null, "https://example.invalid/scan", new URLStreamHandler() {
            @Override protected URLConnection openConnection(URL url) {
                HttpURLConnection stub = new HttpURLConnection(url) {
                    @Override public void connect() { }
                    @Override public void disconnect() { }
                    @Override public boolean usingProxy() { return false; }
                    @Override public int getResponseCode() { return 200; }
                    @Override public OutputStream getOutputStream() { return sent; }
                    @Override public InputStream getInputStream() {
                        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
                    }
                };
                connection.set(stub);
                return stub;
            }
        });
        Bitmap photo = Bitmap.createBitmap(40, 80, Bitmap.Config.ARGB_8888);
        try {
            List<ReceiptLine> items = GeminiReceiptClient.recognize("test-key", photo, endpoint);
            assertEquals(1, items.size());
            assertEquals(12950L, items.get(0).amount);
            assertEquals("test-key", connection.get().getRequestProperty("x-goog-api-key"));
            assertTrue(new JSONObject(sent.toString("UTF-8")).has("contents"));
        } finally { photo.recycle(); }
    }
}

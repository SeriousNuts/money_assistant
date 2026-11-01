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
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class CloudReceiptClientTest {
    @Test public void requestUsesCloudModelAndJpegDataUrlWithoutKey() throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(40, 80, Bitmap.Config.ARGB_8888);
        try {
            JSONObject request = CloudReceiptClient.request(bitmap);
            assertEquals("deepseek-ai/DeepSeek-OCR-2", request.getString("model"));
            assertEquals(2500, request.getInt("max_tokens"));
            JSONArray content = request.getJSONArray("messages").getJSONObject(0).getJSONArray("content");
            assertEquals("<image><|grounding|>Convert the document to markdown.",
                    content.getJSONObject(0).getString("text"));
            assertTrue(content.getJSONObject(1).getJSONObject("image_url").getString("url")
                    .startsWith("data:image/jpeg;base64,"));
            assertFalse(request.toString().contains("test-secret"));
        } finally { bitmap.recycle(); }
    }

    @Test public void parsesOnlyExplicitItemPricesAndSkipsTotalsSlipAndOrder() throws Exception {
        String markdown = "# Кассовый чек\n| № | Товар | Кол-во | Сумма |\n| --- | --- | --- | --- |\n"
                + "| 1 | Молоко | 2 | 159,00 ₽ |\n| ИТОГО | | | 208,90 ₽ |\n"
                + "- Хлеб 49.90 ₽\nНомер заказа 2464,00\nПОД СБЕРБАНК 208,90\n"
                + "ИНН 123456789 99,00\nДата 12.12.2026\nНеразборчивая цена 245\n";
        JSONObject response = new JSONObject().put("choices", new JSONArray().put(new JSONObject()
                .put("message", new JSONObject().put("content", markdown))));
        List<ReceiptLine> lines = CloudReceiptClient.parseResponse(response);
        assertEquals(2, lines.size());
        assertEquals("Молоко", lines.get(0).title);
        assertEquals(15900, lines.get(0).amount);
        assertEquals(4990, lines.get(1).amount);
        assertTrue(lines.get(0).autoDetected);
        assertTrue(lines.get(0).assignees.isEmpty());
    }

    @Test public void postsWithBearerHeaderAndLeavesModelTextForReview() throws Exception {
        String response = "{\"choices\":[{\"message\":{\"content\":\"Чай 100,00 ₽\"}}]}";
        ByteArrayOutputStream sent = new ByteArrayOutputStream();
        AtomicReference<HttpURLConnection> connection = new AtomicReference<>();
        URL endpoint = new URL(null, "https://example.invalid/chat/completions", new URLStreamHandler() {
            @Override protected URLConnection openConnection(URL url) {
                HttpURLConnection stub = new HttpURLConnection(url) {
                    @Override public void connect() { }
                    @Override public void disconnect() { }
                    @Override public boolean usingProxy() { return false; }
                    @Override public int getResponseCode() { return 200; }
                    @Override public OutputStream getOutputStream() { return sent; }
                    @Override public InputStream getInputStream() {
                        return new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8));
                    }
                };
                connection.set(stub);
                return stub;
            }
        });
        Bitmap bitmap = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888);
        try {
            assertEquals(10000, CloudReceiptClient.recognize("test-secret", bitmap, endpoint).get(0).amount);
            assertEquals("Bearer test-secret", connection.get().getRequestProperty("Authorization"));
            assertFalse(sent.toString("UTF-8").contains("test-secret"));
        } finally { bitmap.recycle(); }
    }

    @Test public void errorsNeverIncludeServerBodyOrKey() {
        assertFalse(CloudReceiptClient.apiError(401).getMessage().contains("secret"));
        assertTrue(CloudReceiptClient.apiError(429).getMessage().contains("Лимит"));
    }

    @Test public void unpricedLinesRemainForManualReview() throws Exception {
        assertTrue(CloudReceiptClient.parseMarkdown("Товар без цены\nНомер заказа 2464\nИТОГО 100,00 ₽")
                .isEmpty());
        try {
            CloudReceiptClient.parseResponse(new JSONObject().put("choices", new JSONArray()));
            org.junit.Assert.fail("Missing response must not be accepted");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("не вернул"));
        }
    }
}

package com.example.myapplication;

import android.content.Context;

/** Channel-specific default only; user choice is stored locally and never sends a photo. */
enum ReceiptProvider {
    GEMINI("gemini", "Google Gemini"), CLOUD("cloud", "Cloud.ru · DeepSeek-OCR-2");

    final String id, label;

    ReceiptProvider(String id, String label) { this.id = id; this.label = label; }

    static ReceiptProvider selected(Context context) {
        String fallback = context.getString(R.string.default_online_ocr_provider);
        String saved = context.getSharedPreferences("receipt-ocr-provider", Context.MODE_PRIVATE)
                .getString("provider", fallback);
        return CLOUD.id.equals(saved) ? CLOUD : GEMINI;
    }

    static boolean select(Context context, ReceiptProvider provider) {
        return context.getSharedPreferences("receipt-ocr-provider", Context.MODE_PRIVATE)
                .edit().putString("provider", provider.id).commit();
    }
}

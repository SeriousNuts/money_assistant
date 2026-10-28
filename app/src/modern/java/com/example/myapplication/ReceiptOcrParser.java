package com.example.myapplication;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Conservative OCR suggestions: only rows with a product name AND a trailing price. */
final class ReceiptOcrParser {
    static final class Fragment {
        final String text;
        final int left, top, right, bottom;

        Fragment(String text, int left, int top, int right, int bottom) {
            this.text = text;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }

    private static final Pattern PRICE = Pattern.compile("(?<![0-9.,])([0-9]{1,7}(?:[.,][0-9]{2})?)\\s*[₽рp]?\\s*$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern SUMMARY = Pattern.compile(
            "(?iu).*(итог|всего|сумма|скидк|сдач|оплат|налич|карта|ндс|налог|касса|терминал|чек|дата|время|total|subtotal|change|tax|cash|discount).*"
    );

    static List<ReceiptLine> parse(List<Fragment> fragments, int width, int height) {
        List<ReceiptLine> positions = new ArrayList<>();
        if (width < 1 || height < 1) return positions;
        List<Fragment> sorted = new ArrayList<>(fragments);
        sorted.sort(Comparator.comparingInt(f -> (f.top + f.bottom) / 2));
        List<Fragment> row = new ArrayList<>();
        int rowTop = 0, rowBottom = 0;
        for (Fragment part : sorted) {
            if (part.text == null || part.text.trim().isEmpty() || part.right <= part.left || part.bottom <= part.top) continue;
            if (!row.isEmpty() && Math.abs((part.top + part.bottom) - (rowTop + rowBottom))
                    > Math.max(part.bottom - part.top, rowBottom - rowTop)) {
                append(row, width, height, positions);
                row.clear();
            }
            if (row.isEmpty()) { rowTop = part.top; rowBottom = part.bottom; }
            else { rowTop = Math.min(rowTop, part.top); rowBottom = Math.max(rowBottom, part.bottom); }
            row.add(part);
        }
        append(row, width, height, positions);
        return positions;
    }

    private static void append(List<Fragment> row, int width, int height, List<ReceiptLine> result) {
        if (row.isEmpty() || result.size() >= 300) return;
        row.sort(Comparator.comparingInt(f -> f.left));
        StringBuilder text = new StringBuilder();
        int left = width, top = height, right = 0, bottom = 0;
        for (Fragment part : row) {
            if (text.length() > 0) text.append(' ');
            text.append(part.text.trim());
            left = Math.min(left, part.left); top = Math.min(top, part.top);
            right = Math.max(right, part.right); bottom = Math.max(bottom, part.bottom);
        }
        Matcher match = PRICE.matcher(text);
        if (!match.find()) return;
        String title = text.substring(0, match.start()).replaceAll("[.·:_-]+$", "").trim();
        if (title.length() < 2 || SUMMARY.matcher(title).matches()) return;
        int letters = 0;
        for (int i = 0; i < title.length(); i++) if (Character.isLetter(title.charAt(i))) letters++;
        if (letters < 2) return;
        long amount;
        try { amount = Money.parse(match.group(1)); } catch (IllegalArgumentException e) { return; }
        if (amount == 0) return;
        ReceiptLine line = new ReceiptLine();
        line.title = title.replaceAll("\\s+", " ");
        line.amount = amount;
        line.left = clamp((float) left / width); line.top = clamp((float) top / height);
        line.right = clamp((float) right / width); line.bottom = clamp((float) bottom / height);
        if (line.right <= line.left || line.bottom <= line.top) return;
        line.autoDetected = true;
        result.add(line);
    }

    private static float clamp(float value) { return Math.max(0, Math.min(1, value)); }

    private ReceiptOcrParser() { }
}

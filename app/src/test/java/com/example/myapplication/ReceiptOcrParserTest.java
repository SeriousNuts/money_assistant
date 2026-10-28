package com.example.myapplication;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReceiptOcrParserTest {
    private ReceiptOcrParser.Fragment part(String text, int left, int top, int right, int bottom) {
        return new ReceiptOcrParser.Fragment(text, left, top, right, bottom);
    }

    @Test public void groupsPriceAndTitleAndMarksTheirCombinedRectangle() {
        List<ReceiptLine> found = ReceiptOcrParser.parse(Arrays.asList(
                part("79,50", 75, 24, 95, 34), part("Молоко 1л", 10, 23, 65, 34),
                part("Хлеб 20.00", 10, 45, 95, 56), part("ИТОГО 99,50", 10, 70, 95, 81)), 100, 100);
        assertEquals(2, found.size());
        assertEquals("Молоко 1л", found.get(0).title);
        assertEquals(7950, found.get(0).amount);
        assertEquals(0.1f, found.get(0).left, 0.001f);
        assertEquals(0.95f, found.get(0).right, 0.001f);
        assertEquals(0.34f, found.get(0).bottom, 0.001f);
        assertTrue(found.get(0).autoDetected);
        assertEquals(2000, found.get(1).amount);
    }

    @Test public void ignoresSummaryZeroAndNumberOnlyRows() {
        List<ReceiptLine> result = ReceiptOcrParser.parse(Arrays.asList(
                part("Итого 100.00", 1, 1, 90, 10),
                part("Карта 100.00", 1, 20, 90, 29),
                part("12345 20.00", 1, 40, 90, 49),
                part("Товар 0.00", 1, 60, 90, 69)), 100, 100);
        assertTrue(result.isEmpty());
        assertTrue(ReceiptOcrParser.parse(Collections.emptyList(), 0, 100).isEmpty());
    }

    @Test public void rejectsMalformedAmountsAndKeepsDistinctRows() {
        List<ReceiptLine> result = ReceiptOcrParser.parse(Arrays.asList(
                part("Кофе 12,345", 1, 1, 90, 10),
                part("Чай 40,00", 1, 20, 90, 29),
                part("Пирог 80,50", 1, 40, 90, 49)), 100, 100);
        assertEquals(2, result.size());
        assertEquals("Чай", result.get(0).title);
        assertEquals(8050, result.get(1).amount);
    }

    @Test public void excludesReceiptFooterCodesDatesAndLowConfidenceNoise() {
        List<ReceiptLine> result = ReceiptOcrParser.parse(Arrays.asList(
                part("ИНН поставщика 4400005628", 1, 1, 90, 10),
                part("ИТОГ =8135.00", 1, 20, 90, 29),
                part("Кассир Светлана 8135.00", 1, 40, 90, 49),
                part("КОЛЬЦО 94014700 3802.00", 1, 60, 90, 69),
                new ReceiptOcrParser.Fragment("Фанта 99.00", 1, 80, 90, 89, 12),
                part("Кофе 149.00", 1, 100, 90, 109)), 100, 120);
        assertEquals(1, result.size());
        assertEquals("Кофе", result.get(0).title);
    }
}

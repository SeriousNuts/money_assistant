package com.example.myapplication;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MoneyTest {
    @Test public void equalSplitKeepsEveryKopeck() {
        assertEquals(Arrays.asList(3334L, 3333L, 3333L), Money.split(10000, 3));
    }
    @Test public void oneKopeckGoesToFirst() {
        assertEquals(Arrays.asList(1L, 0L, 0L), Money.split(1, 3));
    }
    @Test public void commaAndDot() {
        assertEquals(3334L, Money.parse("33,34"));
        assertEquals(3334L, Money.parse("33.34"));
    }
    @Test public void formatZero() { assertEquals("0,00 ₽", Money.format(0)); }
    @Test public void zeroIsAllowedForIndividualShares() { assertEquals(0, Money.parse("0")); }
    @Test public void rejectEmptyAndNegative() {
        invalid(""); invalid("-1"); invalid("  ");
    }
    @Test public void rejectOverprecisionAndHugeAmount() {
        invalid("1,001"); invalid("99999999999999999999999");
    }
    @Test public void rejectMissingPeople() {
        try { Money.split(100, 0); fail("accepted an empty list"); } catch (IllegalArgumentException expected) { }
    }
    private void invalid(String value) {
        try { Money.parse(value); fail("accepted " + value); } catch (IllegalArgumentException expected) { }
    }
}

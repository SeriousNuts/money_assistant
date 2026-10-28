package com.example.myapplication;

import java.util.ArrayList;
import java.util.List;

/** All amounts are non-negative integer kopecks. No binary floating-point arithmetic. */
public final class Money {
    public static final long MAX_MINOR = 99_999_999_999L;

    private Money() { }

    public static long parse(String input) {
        String value = input == null ? "" : input.trim().replace(',', '.');
        if (!value.matches("[0-9]+(\\.[0-9]{1,2})?")) {
            throw new IllegalArgumentException("Введите сумму в рублях, например 33,34");
        }
        String[] parts = value.split("\\.");
        try {
            long rubles = Long.parseLong(parts[0]);
            int kopecks = parts.length == 1 ? 0 : Integer.parseInt((parts[1] + "0").substring(0, 2));
            if (rubles > MAX_MINOR / 100 ||
                    (rubles == MAX_MINOR / 100 && kopecks > MAX_MINOR % 100))
                throw new IllegalArgumentException("Слишком большая сумма");
            long minor = rubles * 100 + kopecks;
            check(minor);
            return minor;
        } catch (ArithmeticException | NumberFormatException e) {
            throw new IllegalArgumentException("Слишком большая сумма", e);
        }
    }

    public static void check(long minor) {
        if (minor < 0 || minor > MAX_MINOR) throw new IllegalArgumentException("Недопустимая сумма");
    }

    public static String format(long minor) {
        check(minor);
        return (minor / 100) + "," + (minor % 100 < 10 ? "0" : "") + (minor % 100) + " ₽";
    }

    public static List<Long> split(long minor, int people) {
        check(minor);
        if (minor == 0) throw new IllegalArgumentException("Введите положительную сумму");
        if (people <= 0 || people > 450) throw new IllegalArgumentException("Добавьте от 1 до 450 участников");
        List<Long> shares = new ArrayList<>();
        for (int i = 0; i < people; i++) shares.add(minor / people + (i < minor % people ? 1 : 0));
        return shares;
    }

    public static long sum(List<Long> amounts) {
        long result = 0;
        for (Long amount : amounts) {
            if (amount == null) throw new IllegalArgumentException("Не задана сумма");
            check(amount);
            result = Math.addExact(result, amount);
        }
        check(result);
        return result;
    }
}

package com.example.myapplication;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ReceiptCalculator {
    private ReceiptCalculator() { }

    /** The order of selected participants determines who receives an extra kopeck. */
    public static List<Long> allocate(int participantCount, List<ReceiptLine> lines) {
        if (participantCount < 1 || participantCount > 450) throw new IllegalArgumentException("Добавьте от 1 до 450 участников");
        List<Long> totals = new ArrayList<>();
        for (int i = 0; i < participantCount; i++) totals.add(0L);
        for (ReceiptLine line : lines) {
            Money.check(line.amount);
            if (line.assignees.isEmpty()) throw new IllegalArgumentException("Выберите участника для каждой позиции");
            Set<Integer> unique = new HashSet<>();
            for (int index : line.assignees) {
                if (index < 0 || index >= participantCount || !unique.add(index))
                    throw new IllegalArgumentException("Неверный участник позиции");
            }
            List<Long> shares = Money.split(line.amount, line.assignees.size());
            for (int i = 0; i < shares.size(); i++) {
                int index = line.assignees.get(i);
                totals.set(index, Math.addExact(totals.get(index), shares.get(i)));
                Money.check(totals.get(index));
            }
        }
        return totals;
    }

    public static List<Long> allocateAll(int participantCount, List<ReceiptDocument> receipts) {
        List<Long> totals = new ArrayList<>();
        for (int i = 0; i < participantCount; i++) totals.add(0L);
        for (ReceiptDocument receipt : receipts) {
            List<Long> shares = allocate(participantCount, receipt.lines);
            for (int i = 0; i < shares.size(); i++) {
                long total = Math.addExact(totals.get(i), shares.get(i));
                Money.check(total);
                totals.set(i, total);
            }
        }
        return totals;
    }
}

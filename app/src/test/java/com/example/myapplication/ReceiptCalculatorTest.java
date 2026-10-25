package com.example.myapplication;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ReceiptCalculatorTest {
    private ReceiptLine line(long amount, Integer... assignees) {
        ReceiptLine item = new ReceiptLine();
        item.title = "Еда";
        item.amount = amount;
        item.assignees.addAll(Arrays.asList(assignees));
        return item;
    }
    @Test public void sharedPositionDistributesRemainderExactly() {
        assertEquals(Arrays.asList(3334L, 3333L, 3333L),
                ReceiptCalculator.allocate(3, Collections.singletonList(line(10000, 0, 1, 2))));
    }
    @Test public void personalAndSharedItemsAddUp() {
        assertEquals(Arrays.asList(5101L, 4900L), ReceiptCalculator.allocate(2,
                Arrays.asList(line(201, 0, 1), line(5000, 0), line(4800, 1))));
    }
    @Test public void onlySelectedParticipantsPay() {
        assertEquals(Arrays.asList(0L, 51L, 50L),
                ReceiptCalculator.allocate(3, Collections.singletonList(line(101, 1, 2))));
    }
    @Test public void zeroLinesYieldZeroShares() {
        assertEquals(Arrays.asList(0L, 0L), ReceiptCalculator.allocate(2, new ArrayList<>()));
    }
    @Test public void rejectMissingOrDuplicateAssignee() {
        invalid(2, line(100)); invalid(2, line(100, 0, 0));
    }
    @Test public void rejectOutOfBoundsAndNegativeAmount() {
        invalid(2, line(100, 2)); invalid(2, line(-1, 0));
    }
    private void invalid(int people, ReceiptLine item) {
        try { ReceiptCalculator.allocate(people, Collections.singletonList(item)); fail("accepted invalid markup"); }
        catch (IllegalArgumentException expected) { }
    }
}

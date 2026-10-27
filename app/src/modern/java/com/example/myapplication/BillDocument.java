package com.example.myapplication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BillDocument {
    public String id = UUID.randomUUID().toString();
    public String name = "";
    public long createdAt = System.currentTimeMillis();
    public long revision;
    public final List<Person> people = new ArrayList<>();
    public final List<ReceiptDocument> receipts = new ArrayList<>();
    // Compatibility for the existing single-receipt editor; shares the first receipt's list.
    public List<ReceiptLine> lines = new ArrayList<>();

    public ReceiptDocument primaryReceipt() {
        if (receipts.isEmpty()) {
            ReceiptDocument receipt = ReceiptDocument.legacy(id);
            receipt.lines.addAll(lines);
            receipts.add(receipt);
        }
        lines = receipts.get(0).lines;
        return receipts.get(0);
    }

    public boolean hasReceiptLines() {
        if (receipts.isEmpty()) return !lines.isEmpty();
        for (ReceiptDocument receipt : receipts) if (!receipt.lines.isEmpty()) return true;
        return false;
    }

    /** Calculate first, then update amounts together; never touch paid flags. */
    public void recalculateAmounts() {
        if (receipts.isEmpty() && !lines.isEmpty()) primaryReceipt();
        List<Long> allocated = ReceiptCalculator.allocateAll(people.size(), receipts);
        List<Long> totals = new ArrayList<>();
        for (int i = 0; i < people.size(); i++) {
            long amount = Math.addExact(people.get(i).manualAmount, allocated.get(i));
            Money.check(amount);
            totals.add(amount);
        }
        for (int i = 0; i < people.size(); i++) people.get(i).amount = totals.get(i);
    }

    public long total() {
        List<Long> amounts = new ArrayList<>();
        for (Person person : people) amounts.add(person.amount);
        return Money.sum(amounts);
    }

    public long paid() {
        List<Long> amounts = new ArrayList<>();
        for (Person person : people) if (person.paid) amounts.add(person.amount);
        return Money.sum(amounts);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject().put("id", id).put("name", name)
                .put("createdAt", createdAt).put("revision", revision);
        JSONArray participants = new JSONArray();
        for (Person person : people) participants.put(new JSONObject().put("name", person.name)
                .put("personId", person.id)
                .put("phone", person.phone).put("amountMinor", person.amount)
                .put("manualMinor", person.manualAmount).put("paid", person.paid));
        json.put("people", participants);
        if (receipts.isEmpty() && !lines.isEmpty()) primaryReceipt();
        JSONArray documents = new JSONArray();
        for (ReceiptDocument receipt : receipts) documents.put(receipt.toJson());
        json.put("receipts", documents);
        JSONArray receipt = new JSONArray();
        for (ReceiptLine line : lines) receipt.put(line.toJson());
        // A 1.4.1 client can still read the first receipt, but new clients use receipts.
        return json.put("lines", receipt);
    }

    public static BillDocument fromJson(JSONObject json) throws JSONException {
        BillDocument bill = new BillDocument();
        bill.id = json.getString("id");
        bill.name = json.optString("name", "");
        bill.createdAt = json.optLong("createdAt", bill.createdAt);
        bill.revision = json.optLong("revision", 0);
        JSONArray people = json.optJSONArray("people");
        if (people != null) for (int i = 0; i < people.length(); i++) {
            JSONObject p = people.getJSONObject(i);
            Person person = new Person(p.getString("name"), p.optString("phone", ""),
                    p.getLong("amountMinor"), p.optBoolean("paid", false));
            person.id = p.optString("personId", bill.id + ":" + i);
            if (p.has("manualMinor")) person.manualAmount = p.getLong("manualMinor");
            bill.people.add(person);
        }
        JSONArray documents = json.optJSONArray("receipts");
        if (documents != null) for (int i = 0; i < documents.length(); i++)
            bill.receipts.add(ReceiptDocument.fromJson(documents.getJSONObject(i)));
        if (!bill.receipts.isEmpty()) bill.lines = bill.receipts.get(0).lines;
        else {
            JSONArray lines = json.optJSONArray("lines");
            if (lines != null) for (int i = 0; i < lines.length(); i++) bill.lines.add(ReceiptLine.fromJson(lines.getJSONObject(i)));
            if (!bill.lines.isEmpty()) bill.primaryReceipt();
        }
        // Old bills stored only final amounts. Recover manual shares without changing totals.
        if (people != null) {
            List<Long> allocated = null;
            try { if (!bill.receipts.isEmpty()) allocated = ReceiptCalculator.allocateAll(bill.people.size(), bill.receipts); }
            catch (RuntimeException ignored) { /* Preserve old totals even if old markup is invalid. */ }
            for (int i = 0; i < people.length(); i++) if (!people.getJSONObject(i).has("manualMinor")) {
                Person person = bill.people.get(i);
                if (allocated != null && person.amount >= allocated.get(i))
                    person.manualAmount = person.amount - allocated.get(i);
            }
        }
        return bill;
    }

    public static final class Person {
        public String id = UUID.randomUUID().toString();
        public String name, phone;
        public long amount;
        public long manualAmount;
        public boolean paid;

        public Person(String name, String phone, long amount, boolean paid) {
            this.name = name;
            this.phone = phone;
            this.amount = amount;
            this.manualAmount = amount;
            this.paid = paid;
        }
    }
}

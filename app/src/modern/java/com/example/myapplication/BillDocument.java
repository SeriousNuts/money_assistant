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
    public final List<Person> people = new ArrayList<>();
    public final List<ReceiptLine> lines = new ArrayList<>();

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
        JSONObject json = new JSONObject().put("id", id).put("name", name).put("createdAt", createdAt);
        JSONArray participants = new JSONArray();
        for (Person person : people) participants.put(new JSONObject().put("name", person.name)
                .put("phone", person.phone).put("amountMinor", person.amount).put("paid", person.paid));
        json.put("people", participants);
        JSONArray receipt = new JSONArray();
        for (ReceiptLine line : lines) receipt.put(line.toJson());
        return json.put("lines", receipt);
    }

    public static BillDocument fromJson(JSONObject json) throws JSONException {
        BillDocument bill = new BillDocument();
        bill.id = json.getString("id");
        bill.name = json.optString("name", "");
        bill.createdAt = json.optLong("createdAt", bill.createdAt);
        JSONArray people = json.optJSONArray("people");
        if (people != null) for (int i = 0; i < people.length(); i++) {
            JSONObject p = people.getJSONObject(i);
            bill.people.add(new Person(p.getString("name"), p.optString("phone", ""),
                    p.getLong("amountMinor"), p.optBoolean("paid", false)));
        }
        JSONArray lines = json.optJSONArray("lines");
        if (lines != null) for (int i = 0; i < lines.length(); i++) bill.lines.add(ReceiptLine.fromJson(lines.getJSONObject(i)));
        return bill;
    }

    public static final class Person {
        public String name, phone;
        public long amount;
        public boolean paid;

        public Person(String name, String phone, long amount, boolean paid) {
            this.name = name;
            this.phone = phone;
            this.amount = amount;
            this.paid = paid;
        }
    }
}

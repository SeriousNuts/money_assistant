package com.example.myapplication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Normalized rectangle coordinates keep markup aligned at different screen sizes. */
public final class ReceiptLine {
    public String title;
    public long amount;
    public float left, top, right, bottom;
    public final List<Integer> assignees = new ArrayList<>();

    public JSONObject toJson() throws JSONException {
        JSONArray selected = new JSONArray();
        for (int index : assignees) selected.put(index);
        return new JSONObject().put("title", title).put("amountMinor", amount)
                .put("left", left).put("top", top).put("right", right).put("bottom", bottom)
                .put("assignees", selected);
    }

    public static ReceiptLine fromJson(JSONObject json) throws JSONException {
        ReceiptLine line = new ReceiptLine();
        line.title = json.getString("title");
        line.amount = json.getLong("amountMinor");
        line.left = (float) json.optDouble("left");
        line.top = (float) json.optDouble("top");
        line.right = (float) json.optDouble("right");
        line.bottom = (float) json.optDouble("bottom");
        JSONArray people = json.getJSONArray("assignees");
        for (int i = 0; i < people.length(); i++) line.assignees.add(people.getInt(i));
        return line;
    }
}

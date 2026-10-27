package com.example.myapplication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A receipt has its own markup and app-private photo (never an external URI). */
public final class ReceiptDocument {
    public String id, name, photoFile;
    public final List<ReceiptLine> lines = new ArrayList<>();

    private ReceiptDocument(String id, String name, String photoFile) {
        this.id = id;
        this.name = name;
        this.photoFile = photoFile;
    }

    public static ReceiptDocument legacy(String billId) {
        return new ReceiptDocument("legacy", "Чек 1", billId + ".jpg");
    }

    public static ReceiptDocument create(String billId) {
        String id = UUID.randomUUID().toString();
        return new ReceiptDocument(id, "Новый чек", billId + "-" + id + ".jpg");
    }

    public JSONObject toJson() throws JSONException {
        JSONArray positions = new JSONArray();
        for (ReceiptLine line : lines) positions.put(line.toJson());
        return new JSONObject().put("id", id).put("name", name)
                .put("photoFile", photoFile).put("lines", positions);
    }

    public static ReceiptDocument fromJson(JSONObject json) throws JSONException {
        String photo = json.optString("photoFile", "");
        // Never allow a stored document to point outside files/receipts.
        if (photo.contains("/") || photo.contains("\\") || photo.equals(".") || photo.equals(".."))
            throw new JSONException("Некорректное имя файла чека");
        ReceiptDocument receipt = new ReceiptDocument(json.getString("id"), json.optString("name", "Чек"), photo);
        JSONArray positions = json.optJSONArray("lines");
        if (positions != null) for (int i = 0; i < positions.length(); i++)
            receipt.lines.add(ReceiptLine.fromJson(positions.getJSONObject(i)));
        return receipt;
    }
}

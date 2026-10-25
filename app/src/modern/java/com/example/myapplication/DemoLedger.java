package com.example.myapplication;

import android.content.Context;
import android.util.AtomicFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Private on-device ledger. A deletion never recreates the example on next login. */
public final class DemoLedger implements Serializable {
    // Matches the serialized DemoLedger bundled in the 1.2 APK.
    private static final long serialVersionUID = 1L;
    private Map<String, Map<String, Object>> documents;
    private transient AtomicFile file;

    public DemoLedger(Context context) {
        file = new AtomicFile(new File(context.getFilesDir(), "demo-ledger-v1.bin"));
    }

    public synchronized List<BillDocument> read() throws IOException {
        if (!file.getBaseFile().exists() && !new File(file.getBaseFile().getPath() + ".bak").exists()) {
            BillDocument sample = new BillDocument();
            sample.id = "demo-dinner";
            sample.name = "Ужин с друзьями · пример";
            List<Long> shares = Money.split(10000, 3);
            String[] names = {"Вы (тестовый пользователь)", "Анна", "Михаил"};
            for (int i = 0; i < 3; i++) sample.people.add(new BillDocument.Person(names[i], "", shares.get(i), false));
            List<BillDocument> first = new ArrayList<>();
            first.add(sample);
            write(first);
            return first;
        }
        try {
            byte[] data = file.readFully();
            if (data.length > 1 && (data[0] & 0xff) == 0xac && (data[1] & 0xff) == 0xed) {
                // Convert the previous AtomicFile/ObjectOutputStream format once, without
                // recreating deleted demo bills or discarding users' payment history.
                List<BillDocument> migrated;
                try (ObjectInputStream old = new ObjectInputStream(file.openRead())) {
                    DemoLedger legacy = (DemoLedger) old.readObject();
                    migrated = legacyBills(legacy.documents);
                } catch (ClassNotFoundException e) { throw new IOException("Невозможно прочитать старый демосчёт", e); }
                write(migrated);
                return migrated;
            }
            JSONArray json = new JSONArray(new String(data, StandardCharsets.UTF_8));
            List<BillDocument> bills = new ArrayList<>();
            for (int i = 0; i < json.length(); i++) bills.add(BillDocument.fromJson(json.getJSONObject(i)));
            return bills;
        } catch (JSONException e) {
            throw new IOException("Повреждены локальные данные. Файл не перезаписан.", e);
        }
    }

    private static List<BillDocument> legacyBills(Map<String, Map<String, Object>> documents) throws IOException {
        if (documents == null) throw new IOException("Некорректный файл демосчёта");
        List<BillDocument> bills = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> entry : documents.entrySet()) {
            Map<String, Object> chart = entry.getValue();
            if (!"Chart".equals(chart.get("typo"))) continue;
            BillDocument bill = new BillDocument();
            bill.id = entry.getKey();
            bill.name = String.valueOf(chart.get("Chartname"));
            for (int i = 0; i < 450; i++) {
                Map<String, Object> p = documents.get(bill.id + "_" + i);
                if (p == null) break;
                bill.people.add(new BillDocument.Person(String.valueOf(p.get("name")),
                        String.valueOf(p.get("phone") == null ? "" : p.get("phone")),
                        ((Number) p.get("amountMinor")).longValue(), Boolean.TRUE.equals(p.get("paid"))));
            }
            bills.add(bill);
        }
        return bills;
    }

    public synchronized void write(List<BillDocument> bills) throws IOException {
        JSONArray json = new JSONArray();
        try {
            for (BillDocument bill : bills) json.put(bill.toJson());
        } catch (JSONException e) {
            throw new IOException(e);
        }
        byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
        FileOutputStream stream = null;
        try {
            stream = file.startWrite();
            stream.write(bytes);
            file.finishWrite(stream);
        } catch (IOException e) {
            if (stream != null) file.failWrite(stream);
            throw e;
        }
    }
}

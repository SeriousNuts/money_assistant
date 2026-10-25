package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** The original receipt stays in private app storage; only markup and shares go to the bill. */
public final class ReceiptActivity extends AppCompatActivity {
    private static final int CAMERA = 201, PICK_IMAGE = 202;
    private BillDocument bill;
    private ReceiptImageView image;
    private LinearLayout positions;
    private File photo, pending;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            bill = BillDocument.fromJson(new JSONObject(state == null
                    ? getIntent().getStringExtra("draft") : state.getString("draft")));
            File directory = new File(getFilesDir(), "receipts");
            if (!directory.exists() && !directory.mkdirs()) throw new IllegalStateException("Не удалось создать папку чека");
            photo = new File(directory, bill.id + ".jpg");
            if (state != null && state.getString("pending") != null) pending = new File(state.getString("pending"));
        } catch (Exception e) { Ui.error(this, e); finish(); return; }
        LinearLayout root = Ui.page(this), body = Ui.scroll(this, root);
        Ui.add(this, body, Ui.label(this, "РАЗМЕТКА ЧЕКА · БЕЗ OCR"), 0);
        Ui.add(this, body, Ui.text(this, "Отметьте позиции", 28, true), 9);
        Ui.add(this, body, Ui.label(this, "Выделите область пальцем, введите сумму и выберите людей."), 8);
        Ui.add(this, body, Ui.secondary(this, "Сфотографировать чек", this::takePhoto), 18);
        Ui.add(this, body, Ui.secondary(this, "Выбрать изображение", this::choosePhoto), 8);
        image = new ReceiptImageView(this);
        image.setOnSelection((left, top, right, bottom) -> edit(null, left, top, right, bottom));
        image.setOnExisting(this::editExisting);
        Ui.add(this, body, image, 15);
        positions = Ui.column(this);
        Ui.add(this, body, positions, 12);
        Ui.add(this, root, Ui.button(this, "Перенести доли в счёт", this::apply), 8);
        showPhoto();
        render();
    }

    private void takePhoto() {
        try {
            pending = new File(photo.getParentFile(), bill.id + "-capture.jpg");
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".files", pending);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            if (intent.resolveActivity(getPackageManager()) == null) {
                Ui.error(this, new IllegalStateException("Приложение камеры не найдено")); return;
            }
            startActivityForResult(intent, CAMERA);
        } catch (Exception e) { Ui.error(this, e); }
    }

    private void choosePhoto() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*")
                .addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK) return;
        try {
            if (request == CAMERA) {
                if (pending == null || !pending.exists() || pending.length() == 0) throw new IllegalStateException("Фото не получено");
                replacePhoto(pending);
            } else if (request == PICK_IMAGE && data != null && data.getData() != null) {
                File temp = new File(photo.getParentFile(), bill.id + "-import.jpg");
                try {
                    try (InputStream input = getContentResolver().openInputStream(data.getData());
                         OutputStream output = new FileOutputStream(temp)) {
                        if (input == null) throw new IllegalStateException("Изображение недоступно");
                        byte[] buffer = new byte[8192];
                        int count;
                        long length = 0;
                        while ((count = input.read(buffer)) != -1) {
                            length += count;
                            if (length > 20L * 1024 * 1024) throw new IllegalArgumentException("Фото больше 20 МБ");
                            output.write(buffer, 0, count);
                        }
                    }
                    replacePhoto(temp);
                } finally {
                    if (temp.exists() && !temp.delete()) android.util.Log.w("ReceiptActivity", "Could not remove partial import");
                }
            } else return;
            bill.lines.clear(); // Markup on an older image cannot match a new one.
            showPhoto(); render();
        } catch (Exception e) { Ui.error(this, e); }
    }

    private void replacePhoto(File source) throws Exception {
        Files.move(source.toPath(), photo.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private void showPhoto() {
        if (!photo.exists()) { image.setVisibility(View.GONE); return; }
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(photo.getPath(), options);
            if (options.outWidth <= 0 || options.outHeight <= 0) throw new IllegalArgumentException("Это не изображение");
            options.inSampleSize = 1;
            while (options.outWidth / options.inSampleSize > 1800 || options.outHeight / options.inSampleSize > 2400)
                options.inSampleSize *= 2;
            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeFile(photo.getPath(), options);
            if (bitmap == null) throw new IllegalArgumentException("Не удалось открыть изображение");
            int orientation = new ExifInterface(photo.getPath()).getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            Matrix rotation = new Matrix();
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotation.postRotate(90);
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotation.postRotate(180);
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotation.postRotate(270);
            if (!rotation.isIdentity()) {
                Bitmap corrected = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotation, true);
                bitmap.recycle();
                bitmap = corrected;
            }
            image.setImage(bitmap, bill.lines);
            image.setVisibility(View.VISIBLE);
        } catch (Exception e) { Ui.error(this, e); image.setVisibility(View.GONE); }
    }

    private void editExisting(ReceiptLine line) { edit(line, line.left, line.top, line.right, line.bottom); }

    private void edit(ReceiptLine existing, float left, float top, float right, float bottom) {
        LinearLayout form = Ui.column(this);
        form.setPadding(Ui.dp(this, 20), 0, Ui.dp(this, 20), 0);
        EditText title = Ui.input(this, "Позиция чека");
        EditText amount = Ui.input(this, "Сумма в рублях");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (existing != null) {
            title.setText(existing.title);
            amount.setText(Money.format(existing.amount).replace(" ₽", ""));
        }
        Ui.add(this, form, title, 2); Ui.add(this, form, amount, 8);
        List<CheckBox> checkboxes = new ArrayList<>();
        for (int i = 0; i < bill.people.size(); i++) {
            CheckBox check = new CheckBox(this);
            check.setText(bill.people.get(i).name);
            check.setTextColor(Ui.ink(this));
            check.setChecked(existing != null && existing.assignees.contains(i));
            Ui.add(this, form, check, 4);
            checkboxes.add(check);
        }
        AlertDialog.Builder dialog = new AlertDialog.Builder(this).setTitle("Позиция чека").setView(form)
                .setNegativeButton("Отмена", null);
        if (existing != null) dialog.setNeutralButton("Удалить", (d, w) -> {
            bill.lines.remove(existing); render();
        });
        dialog.setPositiveButton("Сохранить", null);
        AlertDialog shown = dialog.create();
        shown.setOnShowListener(v -> shown.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
            try {
                String name = title.getText().toString().trim();
                long value = Money.parse(amount.getText().toString());
                if (name.isEmpty() || value == 0) throw new IllegalArgumentException("Введите название и положительную сумму");
                ReceiptLine line = new ReceiptLine();
                line.title = name; line.amount = value;
                line.left = left; line.top = top; line.right = right; line.bottom = bottom;
                for (int i = 0; i < checkboxes.size(); i++) if (checkboxes.get(i).isChecked()) line.assignees.add(i);
                if (line.assignees.isEmpty()) throw new IllegalArgumentException("Выберите хотя бы одного участника");
                int position = existing == null ? bill.lines.size() : bill.lines.indexOf(existing);
                if (existing != null) bill.lines.set(position, line);
                else bill.lines.add(line);
                try { ReceiptCalculator.allocate(bill.people.size(), bill.lines); }
                catch (RuntimeException error) {
                    if (existing != null) bill.lines.set(position, existing);
                    else bill.lines.remove(position);
                    throw error;
                }
                shown.dismiss(); render();
            } catch (Exception e) { Ui.error(this, e); }
        }));
        shown.show();
    }

    private void render() {
        positions.removeAllViews();
        Ui.add(this, positions, Ui.text(this, "Позиции · " + bill.lines.size(), 20, true), 0);
        for (ReceiptLine line : bill.lines) {
            LinearLayout card = Ui.card(this, positions);
            Ui.add(this, card, Ui.text(this, line.title + " · " + Money.format(line.amount), 16, true), 0);
            Ui.add(this, card, Ui.secondary(this, "Изменить", () -> editExisting(line)), 5);
        }
        if (image != null) image.invalidate();
    }

    private void apply() {
        try {
            if (bill.lines.isEmpty()) throw new IllegalArgumentException("Добавьте хотя бы одну позицию чека");
            List<Long> amounts = ReceiptCalculator.allocate(bill.people.size(), bill.lines);
            for (int i = 0; i < amounts.size(); i++) bill.people.get(i).amount = amounts.get(i);
            setResult(RESULT_OK, new Intent().putExtra("draft", bill.toJson().toString()));
            finish();
        } catch (Exception e) { Ui.error(this, e); }
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        try { state.putString("draft", bill.toJson().toString()); } catch (Exception ignored) { }
        if (pending != null) state.putString("pending", pending.getPath());
        super.onSaveInstanceState(state);
    }
}

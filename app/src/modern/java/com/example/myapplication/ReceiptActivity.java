package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The original receipt stays in private app storage; only markup and shares go to the bill. */
public final class ReceiptActivity extends AppCompatActivity {
    private static final int CAMERA = 201, PICK_IMAGE = 202;
    private BillDocument bill;
    private ReceiptDocument receipt;
    private ReceiptImageView image;
    private ListView positions;
    private TextView count;
    private TextView ocrStatus;
    private EditText search;
    private final List<ReceiptLine> visible = new ArrayList<>();
    private BaseAdapter adapter;
    private File photo, pending, staged;
    private String inheritedStage;
    private boolean applied;
    private boolean recognizing;
    private int ocrGeneration;
    private TextRecognizer recognizer;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            bill = BillDocument.fromJson(new JSONObject(state == null
                    ? getIntent().getStringExtra("draft") : state.getString("draft")));
            String selected = getIntent().getStringExtra("receiptId");
            if (selected != null) for (ReceiptDocument item : bill.receipts)
                if (item.id.equals(selected)) { receipt = item; break; }
            if (receipt == null) {
                if (selected != null) throw new IllegalStateException("Чек не найден");
                receipt = bill.primaryReceipt();
            }
            photo = ReceiptPhotos.photo(this, receipt);
            inheritedStage = state == null ? getIntent().getStringExtra("draftPhoto") : state.getString("inheritedStage");
            if (state != null && state.getString("pending") != null) pending = new File(state.getString("pending"));
            if (state != null && state.getString("staged") != null) staged = new File(state.getString("staged"));
            else if (inheritedStage != null && ReceiptPhotos.isStage(this, receipt, new File(inheritedStage)))
                staged = new File(inheritedStage);
        } catch (Exception e) { Ui.error(this, e); finish(); return; }
        LinearLayout root = Ui.page(this);
        Ui.toolbar(this, root, receipt.name, true, null);
        LinearLayout body = Ui.scroll(this, root);
        Ui.add(this, body, Ui.kicker(this, "РАЗМЕТКА ЧЕКА"), 8);
        Ui.add(this, body, Ui.headline(this, "Отметьте позиции", 34), 10);
        Ui.add(this, body, Ui.label(this, "После фото позиции появятся автоматически. Проверьте названия, суммы и участников; при необходимости выделите области пальцем."), 8);
        Ui.add(this, body, Ui.secondary(this, "✎  Название чека", this::rename), 9);
        Ui.add(this, body, Ui.secondary(this, "Сфотографировать чек", this::takePhoto), 18);
        Ui.add(this, body, Ui.secondary(this, "Выбрать изображение", this::choosePhoto), 8);
        Ui.add(this, body, Ui.secondary(this, "Повторить распознавание", () -> {
            if (!receipt.lines.isEmpty()) new AlertDialog.Builder(this).setTitle("Заменить позиции?")
                    .setMessage("Текущая разметка будет заменена результатами распознавания. Фото останется прежним.")
                    .setNegativeButton("Отмена", null).setPositiveButton("Распознать", (d, w) -> recognize()).show();
            else recognize();
        }), 8);
        ocrStatus = Ui.label(this, "Распознавание работает на устройстве. Для русских названий возможны ошибки; проверьте результат.");
        Ui.add(this, body, ocrStatus, 8);
        image = new ReceiptImageView(this);
        image.setOnSelection((left, top, right, bottom) -> edit(null, left, top, right, bottom));
        image.setOnExisting(this::editExisting);
        Ui.add(this, body, image, 15);
        count = Ui.text(this, "", 18, true);
        Ui.add(this, body, count, 14);
        search = Ui.input(this, "Поиск по позициям");
        Ui.add(this, body, search, 8);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { render(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        positions = new ListView(this);
        positions.setDividerHeight(0);
        adapter = new BaseAdapter() {
            @Override public int getCount() { return visible.size(); }
            @Override public ReceiptLine getItem(int position) { return visible.get(position); }
            @Override public long getItemId(int position) { return position; }
            @Override public View getView(int position, View recycled, ViewGroup parent) {
                TextView row = recycled instanceof TextView ? (TextView) recycled : Ui.text(ReceiptActivity.this, "", 16, false);
                row.setMinimumHeight(Ui.dp(ReceiptActivity.this, 52));
                ReceiptLine line = getItem(position);
                row.setText((line.autoDetected ? "◇ " : "") + line.title + "  ·  " + Money.format(line.amount));
                row.setContentDescription("Изменить позицию " + line.title);
                return row;
            }
        };
        positions.setAdapter(adapter);
        positions.setOnItemClickListener((parent, view, position, id) -> editExisting(visible.get(position)));
        Ui.add(this, body, positions, 8);
        positions.getLayoutParams().height = Ui.dp(this, 280);
        Ui.add(this, Ui.footer(this, root), Ui.button(this, "Перенести доли в счёт", this::apply), 0);
        showPhoto();
        render();
    }

    private void rename() {
        EditText name = Ui.input(this, "Название чека");
        name.setText(receipt.name);
        LinearLayout form = Ui.column(this);
        form.setPadding(Ui.dp(this, 20), Ui.dp(this, 12), Ui.dp(this, 20), 0);
        Ui.add(this, form, name, 0);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Название чека").setView(form)
                .setNegativeButton("Отмена", null).setPositiveButton("Сохранить", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String value = name.getText().toString().trim();
            if (value.isEmpty()) { Ui.error(this, new IllegalArgumentException("Введите название чека")); return; }
            receipt.name = value;
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void takePhoto() {
        try {
            pending = new File(photo.getParentFile(), receipt.photoFile + "-capture.jpg");
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
                stagePhoto(pending);
            } else if (request == PICK_IMAGE && data != null && data.getData() != null) {
                File temp = new File(photo.getParentFile(), receipt.photoFile + "-import.jpg");
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
                    stagePhoto(temp);
                } finally {
                    if (temp.exists() && !temp.delete()) android.util.Log.w("ReceiptActivity", "Could not remove partial import");
                }
            } else return;
            receipt.lines.clear(); // Markup on an older image cannot match a new one.
            showPhoto(); render();
            recognize();
        } catch (Exception e) { Ui.error(this, e); }
    }

    private void stagePhoto(File source) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(source.getPath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IllegalArgumentException("Это не изображение");
        File destination = ReceiptPhotos.newStage(this, receipt);
        java.nio.file.Files.move(source.toPath(), destination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        if (staged != null && !staged.getPath().equals(inheritedStage) && staged.exists() && !staged.delete())
            android.util.Log.w("ReceiptActivity", "Could not remove superseded draft photo");
        staged = destination;
    }

    private void showPhoto() {
        File shown = staged != null && staged.exists() ? staged : photo;
        if (!shown.exists()) { image.setVisibility(View.GONE); return; }
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(shown.getPath(), options);
            if (options.outWidth <= 0 || options.outHeight <= 0) throw new IllegalArgumentException("Это не изображение");
            options.inSampleSize = 1;
            while (options.outWidth / options.inSampleSize > 1800 || options.outHeight / options.inSampleSize > 2400)
                options.inSampleSize *= 2;
            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeFile(shown.getPath(), options);
            if (bitmap == null) throw new IllegalArgumentException("Не удалось открыть изображение");
            int orientation = new ExifInterface(shown.getPath()).getAttributeInt(ExifInterface.TAG_ORIENTATION,
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
            image.setImage(bitmap, receipt.lines);
            image.setVisibility(View.VISIBLE);
        } catch (Exception e) { Ui.error(this, e); image.setVisibility(View.GONE); }
    }

    private void recognize() {
        Bitmap shown = image.currentBitmap();
        if (shown == null || shown.isRecycled() || image.getVisibility() != View.VISIBLE) {
            Ui.error(this, new IllegalStateException("Сначала сфотографируйте чек")); return;
        }
        if (recognizer == null) recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        float scale = Math.min(1f, Math.min(1200f / shown.getWidth(), 1800f / shown.getHeight()));
        Bitmap input = scale < 1 ? Bitmap.createScaledBitmap(shown,
                Math.max(1, Math.round(shown.getWidth() * scale)),
                Math.max(1, Math.round(shown.getHeight() * scale)), true)
                : shown.copy(Bitmap.Config.ARGB_8888, false);
        final int generation = ++ocrGeneration;
        recognizing = true;
        ocrStatus.setText("Распознаём позиции на устройстве…");
        recognizer.process(InputImage.fromBitmap(input, 0)).addOnSuccessListener(result -> {
            if (generation != ocrGeneration || isFinishing() || isDestroyed()) return;
            List<ReceiptOcrParser.Fragment> fragments = new ArrayList<>();
            for (Text.TextBlock block : result.getTextBlocks())
                for (Text.Line line : block.getLines()) {
                    Rect box = line.getBoundingBox();
                    if (box != null) fragments.add(new ReceiptOcrParser.Fragment(line.getText(),
                            box.left, box.top, box.right, box.bottom));
                }
            List<ReceiptLine> found = ReceiptOcrParser.parse(fragments, input.getWidth(), input.getHeight());
            if (found.isEmpty()) {
                ocrStatus.setText("Позиции не найдены. Добавьте их вручную или повторите съёмку.");
                return;
            }
            for (ReceiptLine line : found)
                for (int i = 0; i < bill.people.size(); i++) line.assignees.add(i);
            List<ReceiptLine> previous = new ArrayList<>(receipt.lines);
            receipt.lines.clear();
            receipt.lines.addAll(found);
            try {
                bill.recalculateAmounts();
                ocrStatus.setText("Предложено позиций: " + found.size() + ". По умолчанию разделены между всеми. Проверьте суммы, названия и участников.");
                render();
            } catch (RuntimeException e) {
                receipt.lines.clear(); receipt.lines.addAll(previous);
                ocrStatus.setText("Результат не применён: " + e.getMessage());
                render();
            }
        }).addOnFailureListener(e -> {
            if (generation == ocrGeneration && !isFinishing() && !isDestroyed())
                ocrStatus.setText("Не удалось распознать чек: " + e.getMessage() + ". Разметьте его вручную.");
        }).addOnCompleteListener(task -> { input.recycle(); if (generation == ocrGeneration) recognizing = false; });
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
            receipt.lines.remove(existing); render();
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
                int position = existing == null ? receipt.lines.size() : receipt.lines.indexOf(existing);
                if (existing != null) receipt.lines.set(position, line);
                else receipt.lines.add(line);
                try { bill.recalculateAmounts(); }
                catch (RuntimeException error) {
                    if (existing != null) receipt.lines.set(position, existing);
                    else receipt.lines.remove(position);
                    throw error;
                }
                shown.dismiss(); render();
            } catch (Exception e) { Ui.error(this, e); }
        }));
        shown.show();
    }

    private void render() {
        if (positions == null) return;
        count.setText("Позиции · " + receipt.lines.size());
        String query = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        visible.clear();
        for (ReceiptLine line : receipt.lines)
            if (line.title.toLowerCase(Locale.ROOT).contains(query)) visible.add(line);
        adapter.notifyDataSetChanged();
        if (image != null) image.invalidate();
    }

    private void apply() {
        try {
            if (recognizing) throw new IllegalStateException("Дождитесь окончания распознавания");
            boolean unreviewed = false;
            for (ReceiptLine line : receipt.lines) if (line.autoDetected) { unreviewed = true; break; }
            if (unreviewed) {
                new AlertDialog.Builder(this).setTitle("Проверьте распознанные позиции")
                        .setMessage("Названия и суммы могут быть неточными. Все найденные позиции пока назначены всем участникам. Вы проверили расчёт?")
                        .setNegativeButton("Проверить", null)
                        .setPositiveButton("Перенести", (d, w) -> applyConfirmed()).show();
                return;
            }
            applyConfirmed();
        } catch (Exception e) { Ui.error(this, e); }
    }

    private void applyConfirmed() {
        try {
            if (receipt.lines.isEmpty() && (staged == null || !staged.exists()) && !photo.exists())
                throw new IllegalArgumentException("Добавьте фото или хотя бы одну позицию чека");
            bill.recalculateAmounts();
            setResult(RESULT_OK, new Intent().putExtra("draft", bill.toJson().toString())
                    .putExtra("receiptId", receipt.id)
                    .putExtra("stagedPhoto", staged == null ? null : staged.getPath()));
            applied = true;
            finish();
        } catch (Exception e) { Ui.error(this, e); }
    }

    @Override public void finish() {
        if (!applied && bill != null && receipt != null && getIntent().getBooleanExtra("preserveOnBack", false)) try {
            setResult(RESULT_OK, new Intent().putExtra("draft", bill.toJson().toString())
                    .putExtra("receiptId", receipt.id)
                    .putExtra("stagedPhoto", staged == null ? null : staged.getPath()));
            applied = true;
        } catch (Exception e) { Ui.error(this, e); }
        super.finish();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        try { state.putString("draft", bill.toJson().toString()); } catch (Exception ignored) { }
        if (pending != null) state.putString("pending", pending.getPath());
        if (staged != null) state.putString("staged", staged.getPath());
        state.putString("inheritedStage", inheritedStage);
        super.onSaveInstanceState(state);
    }

    @Override protected void onDestroy() {
        ocrGeneration++;
        if (recognizer != null) recognizer.close();
        if (isFinishing() && !applied) {
            if (staged != null && !staged.getPath().equals(inheritedStage)
                    && staged.exists() && !staged.delete()) android.util.Log.w("ReceiptActivity", "Could not remove draft photo");
            if (pending != null && pending.exists() && !pending.delete()) android.util.Log.w("ReceiptActivity", "Could not remove camera draft");
        }
        super.onDestroy();
    }
}

package com.example.myapplication;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Only app-owned receipt files are touched, and only after saving the document. */
final class ReceiptPhotos {
    static File directory(Context context) throws IOException {
        File folder = new File(context.getFilesDir(), "receipts");
        if (!folder.exists() && !folder.mkdirs()) throw new IOException("Не удалось создать папку чеков");
        return folder;
    }

    static File photo(Context context, ReceiptDocument receipt) throws IOException {
        if (receipt.photoFile.isEmpty() || receipt.photoFile.contains("/") || receipt.photoFile.contains("\\"))
            throw new IOException("Некорректное имя файла чека");
        return new File(directory(context), receipt.photoFile);
    }

    static File stage(Context context, ReceiptDocument receipt) throws IOException {
        return new File(directory(context), receipt.photoFile + ".stage");
    }

    static File newStage(Context context, ReceiptDocument receipt) throws IOException {
        return new File(directory(context), receipt.photoFile + ".stage-" + java.util.UUID.randomUUID());
    }

    static boolean isStage(Context context, ReceiptDocument receipt, File file) throws IOException {
        String name = file.getName();
        return (name.equals(receipt.photoFile + ".stage") ||
                name.matches(java.util.regex.Pattern.quote(receipt.photoFile + ".stage-") + "[a-f0-9-]{36}"))
                && directory(context).getCanonicalFile().equals(file.getCanonicalFile().getParentFile());
    }

    static void commit(Context context, ReceiptDocument receipt, String stagePath) throws IOException {
        if (stagePath == null) return;
        File expected = new File(stagePath);
        if (!isStage(context, receipt, expected) || !expected.isFile())
            throw new IOException("Черновик фотографии недоступен");
        Files.move(expected.toPath(), photo(context, receipt).toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    static void remove(Context context, ReceiptDocument receipt) {
        try {
            File photo = photo(context, receipt);
            if (photo.exists() && !photo.delete()) android.util.Log.w("ReceiptPhotos", "Could not remove receipt photo");
        } catch (IOException e) { android.util.Log.w("ReceiptPhotos", "Invalid receipt photo", e); }
    }

    private ReceiptPhotos() { }
}

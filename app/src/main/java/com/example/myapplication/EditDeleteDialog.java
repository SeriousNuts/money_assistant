package com.example.myapplication;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import static android.content.Intent.getIntent;

public class EditDeleteDialog extends DialogFragment {
    String chartname;
    public EditDeleteDialog() {
    }
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        chartname = this.getArguments().getString("chartname");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Удаление")
                .setMessage("Диаграмма " + chartname + " будет удалена без возможности восстановления")
                .setPositiveButton("Да, удалить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((ChartProgressActivity) getActivity()).okClicked();
                    }
                }).setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        return builder.create();
    }

}

package com.example.myapplication;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class Ui {
    private Ui() { }

    public static int dp(Activity a, float size) { return (int) (size * a.getResources().getDisplayMetrics().density + .5f); }
    public static boolean dark(Activity a) {
        return (a.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }
    public static int background(Activity a) { return Color.parseColor(dark(a) ? "#181818" : "#F7F5F1"); }
    public static int surface(Activity a) { return Color.parseColor(dark(a) ? "#262626" : "#FFFFFF"); }
    public static int ink(Activity a) { return Color.parseColor(dark(a) ? "#F4F1ED" : "#282725"); }
    public static int subdued(Activity a) { return Color.parseColor(dark(a) ? "#BDB9B4" : "#6F6A65"); }
    public static GradientDrawable shape(Activity a, boolean button) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(button ? ink(a) : surface(a));
        drawable.setCornerRadius(dp(a, 18));
        if (!button) drawable.setStroke(dp(a, 1), dark(a) ? 0xff42413e : 0xffe9e4dc);
        return drawable;
    }
    public static LinearLayout column(Activity a) {
        LinearLayout layout = new LinearLayout(a);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }
    public static LinearLayout page(Activity a) {
        LinearLayout root = column(a);
        root.setBackgroundColor(background(a));
        a.setContentView(root);
        return root;
    }
    public static LinearLayout scroll(Activity a, LinearLayout parent) {
        ScrollView scroll = new ScrollView(a);
        scroll.setFillViewport(true);
        parent.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout body = column(a);
        body.setPadding(dp(a, 20), dp(a, 25), dp(a, 20), dp(a, 28));
        scroll.addView(body);
        return body;
    }
    public static TextView text(Activity a, String value, int size, boolean bold) {
        TextView view = new TextView(a);
        view.setText(value);
        view.setTextColor(ink(a));
        view.setTextSize(size);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }
    public static TextView label(Activity a, String value) {
        TextView text = text(a, value, 14, false);
        text.setTextColor(subdued(a));
        return text;
    }
    public static void add(Activity a, LinearLayout parent, View child, int marginTop) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(a, marginTop);
        parent.addView(child, p);
    }
    public static LinearLayout card(Activity a, LinearLayout parent) {
        LinearLayout card = column(a);
        card.setPadding(dp(a, 18), dp(a, 16), dp(a, 18), dp(a, 16));
        card.setBackground(shape(a, false));
        add(a, parent, card, 12);
        return card;
    }
    public static Button button(Activity a, String title, Runnable click) {
        Button button = new Button(a);
        button.setText(title);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTextColor(background(a));
        button.setBackground(shape(a, true));
        button.setMinHeight(dp(a, 50));
        button.setOnClickListener(v -> click.run());
        return button;
    }
    public static Button secondary(Activity a, String title, Runnable click) {
        Button button = button(a, title, click);
        button.setTextColor(ink(a));
        button.setBackground(shape(a, false));
        return button;
    }
    public static EditText input(Activity a, String hint) {
        EditText view = new EditText(a);
        view.setHint(hint);
        view.setTextColor(ink(a));
        view.setHintTextColor(subdued(a));
        view.setTextSize(16);
        view.setSingleLine(true);
        return view;
    }
    public static void error(Activity a, Exception e) {
        Toast.makeText(a, e == null ? "Не удалось выполнить операцию" : e.getMessage(), Toast.LENGTH_LONG).show();
    }
}

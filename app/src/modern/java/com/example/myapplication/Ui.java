package com.example.myapplication;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
import java.util.Locale;

/** Spacing, typography and surfaces shared by the light/dark screens of design 1.2. */
public final class Ui {
    private Ui() { }

    public static int dp(Activity a, float size) { return (int) (size * a.getResources().getDisplayMetrics().density + .5f); }
    public static boolean dark(Activity a) {
        return (a.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }
    public static int background(Activity a) { return dark(a) ? 0xff1d1e1b : 0xfff7f6f2; }
    public static int surface(Activity a) { return dark(a) ? 0xff282925 : Color.WHITE; }
    public static int ink(Activity a) { return dark(a) ? 0xffecebe4 : 0xff272723; }
    public static int subdued(Activity a) { return dark(a) ? 0xffbbbbb4 : 0xff7c7c7a; }
    public static int border(Activity a) { return dark(a) ? 0xff40413c : 0xffe6e3dc; }
    public static int accent(Activity a) { return dark(a) ? 0xff68b38a : 0xff208257; }

    public static GradientDrawable shape(Activity a, int color, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(a, radius));
        if (stroke != 0) drawable.setStroke(dp(a, 1), stroke);
        return drawable;
    }

    public static LinearLayout column(Activity a) {
        LinearLayout layout = new LinearLayout(a);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    public static LinearLayout row(Activity a) {
        LinearLayout layout = new LinearLayout(a);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    public static LinearLayout page(Activity a) {
        LinearLayout root = column(a);
        root.setBackgroundColor(background(a));
        a.getWindow().setStatusBarColor(background(a));
        a.getWindow().setNavigationBarColor(background(a));
        a.getWindow().getDecorView().setSystemUiVisibility(dark(a) ? 0 :
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        a.setContentView(root);
        return root;
    }

    public static LinearLayout scroll(Activity a, LinearLayout parent) {
        ScrollView scroll = new ScrollView(a);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        parent.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout body = column(a);
        body.setPadding(dp(a, 24), dp(a, 8), dp(a, 24), dp(a, 20));
        scroll.addView(body);
        return body;
    }

    public static TextView text(Activity a, String value, int size, boolean bold) {
        TextView view = new TextView(a);
        view.setText(value);
        view.setTextColor(ink(a));
        view.setTextSize(size);
        if (bold) view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    public static TextView headline(Activity a, String value, int size) {
        TextView title = text(a, value, size, false);
        title.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        title.setIncludeFontPadding(false);
        return title;
    }

    public static TextView label(Activity a, String value) {
        TextView text = text(a, value, 14, false);
        text.setTextColor(subdued(a));
        return text;
    }

    public static TextView kicker(Activity a, String value) {
        TextView text = label(a, value);
        text.setTextSize(11);
        text.setLetterSpacing(.15f);
        return text;
    }

    public static void add(Activity a, LinearLayout parent, View child, int marginTop) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(a, marginTop);
        parent.addView(child, p);
    }

    public static void addRow(Activity a, LinearLayout parent, View child, int weight) {
        parent.addView(child, new LinearLayout.LayoutParams(weight == 0 ? -2 : 0, -2, weight));
    }

    public static LinearLayout card(Activity a, LinearLayout parent) {
        LinearLayout card = column(a);
        card.setPadding(dp(a, 24), dp(a, 22), dp(a, 24), dp(a, 22));
        card.setBackground(shape(a, surface(a), 24, border(a)));
        add(a, parent, card, 16);
        return card;
    }

    public static Button button(Activity a, String title, Runnable click) {
        return buttonStyle(a, title, click, true);
    }

    public static Button secondary(Activity a, String title, Runnable click) {
        return buttonStyle(a, title, click, false);
    }

    private static Button buttonStyle(Activity a, String title, Runnable click, boolean primary) {
        Button button = new Button(a);
        button.setText(title);
        button.setAllCaps(false);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setTextSize(15);
        button.setTextColor(primary ? background(a) : ink(a));
        button.setBackground(shape(a, primary ? ink(a) : surface(a), 32, primary ? 0 : border(a)));
        button.setMinHeight(dp(a, 56));
        button.setPadding(dp(a, 14), dp(a, 8), dp(a, 14), dp(a, 8));
        button.setElevation(0);
        button.setStateListAnimator(null);
        button.setOnClickListener(v -> click.run());
        return button;
    }

    public static TextView link(Activity a, String value, Runnable click) {
        TextView view = label(a, value);
        view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setMinHeight(dp(a, 48));
        view.setOnClickListener(v -> click.run());
        return view;
    }

    public static TextView chip(Activity a, String value, boolean selected, Runnable click) {
        TextView chip = text(a, value, 14, false);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(a, 13), 0, dp(a, 13), 0);
        chip.setMinHeight(dp(a, 40));
        chip.setTextColor(selected ? background(a) : subdued(a));
        chip.setBackground(shape(a, selected ? ink(a) : (dark(a) ? 0xff32342d : 0xffeeece5), 24, 0));
        chip.setOnClickListener(v -> click.run());
        return chip;
    }

    public static LinearLayout toolbar(Activity a, LinearLayout parent, String title, boolean back, Runnable menu) {
        LinearLayout bar = row(a);
        bar.setPadding(dp(a, back ? 14 : 16), 0, dp(a, 10), 0);
        parent.addView(bar, new LinearLayout.LayoutParams(-1, dp(a, 56)));
        if (back) {
            TextView arrow = text(a, "←", 25, false);
            arrow.setGravity(Gravity.CENTER);
            arrow.setContentDescription("Назад");
            bar.addView(arrow, new LinearLayout.LayoutParams(dp(a, 40), -1));
            arrow.setOnClickListener(v -> a.finish());
        }
        TextView brand = text(a, title, 20, true);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -1, 1);
        if (back) p.leftMargin = dp(a, 8);
        bar.addView(brand, p);
        if (menu != null) {
            TextView more = text(a, "⋮", 24, false);
            more.setGravity(Gravity.CENTER);
            more.setContentDescription("Дополнительные действия");
            more.setTag("toolbarMenu");
            bar.addView(more, new LinearLayout.LayoutParams(dp(a, 36), -1));
            more.setOnClickListener(v -> menu.run());
        }
        return bar;
    }

    public static LinearLayout footer(Activity a, LinearLayout root) {
        LinearLayout footer = column(a);
        footer.setPadding(dp(a, 24), dp(a, 8), dp(a, 24), dp(a, 16));
        footer.setBackgroundColor(background(a));
        root.addView(footer, new LinearLayout.LayoutParams(-1, -2));
        return footer;
    }

    public static EditText input(Activity a, String hint) {
        EditText view = new EditText(a);
        view.setHint(hint);
        view.setTextColor(ink(a));
        view.setHintTextColor(subdued(a));
        view.setTextSize(16);
        view.setSingleLine(true);
        view.setMinHeight(dp(a, 54));
        view.setPadding(dp(a, 16), 0, dp(a, 16), 0);
        view.setBackground(shape(a, surface(a), 16, border(a)));
        return view;
    }

    public static View progress(Activity a, long paid, long total) {
        View line = new View(a) {
            final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas canvas) {
                paint.setColor(dark(a) ? 0xff33342f : 0xfff3f2ef);
                canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
                paint.setColor(accent(a));
                float fraction = total > 0 ? Math.min(1f, (float) paid / (float) total) : 0;
                canvas.drawRect(0, 0, getWidth() * fraction, getHeight(), paint);
            }
        };
        line.setContentDescription("Оплачено " + (total == 0 ? 0 : paid * 100 / total) + "%");
        return line;
    }

    public static TextView avatar(Activity a, String name) {
        TextView avatar = text(a, name == null || name.trim().isEmpty() ? "·" :
                name.trim().substring(0, name.trim().offsetByCodePoints(0, 1)).toUpperCase(Locale.ROOT), 18, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(shape(a, dark(a) ? 0xff33342f : 0xffeeece5, 24, 0));
        return avatar;
    }

    public static void error(Activity a, Exception e) {
        Toast.makeText(a, e == null ? "Не удалось выполнить операцию" : e.getMessage(), Toast.LENGTH_LONG).show();
    }
}

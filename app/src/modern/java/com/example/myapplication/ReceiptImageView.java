package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.List;

/** Touch selections are stored relative to the original bitmap, not the screen. */
public final class ReceiptImageView extends View {
    public interface Selection { void selected(float left, float top, float right, float bottom); }
    public interface Existing { void selected(ReceiptLine line); }
    private Bitmap bitmap;
    private List<ReceiptLine> lines;
    private Selection selection;
    private Existing existing;
    private float x0, y0, x1, y1;
    private boolean dragging;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public ReceiptImageView(Context context) {
        super(context);
        setContentDescription("Чек: выделите позицию пальцем или нажмите на ранее отмеченную");
    }
    public void setImage(Bitmap image, List<ReceiptLine> markup) {
        if (bitmap != null && bitmap != image) bitmap.recycle();
        bitmap = image; lines = markup; requestLayout(); invalidate();
    }
    public void setOnSelection(Selection listener) { selection = listener; }
    public void setOnExisting(Existing listener) { existing = listener; }
    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int height = bitmap == null ? width : (int) ((long) width * bitmap.getHeight() / bitmap.getWidth());
        setMeasuredDimension(width, Math.max(1, height));
    }
    @Override protected void onDraw(Canvas canvas) {
        if (bitmap == null) return;
        paint.setColor(Color.WHITE); paint.setStyle(Paint.Style.FILL);
        canvas.drawBitmap(bitmap, null, new RectF(0, 0, getWidth(), getHeight()), paint);
        paint.setColor(0xffb46d3d); paint.setStrokeWidth(4);
        paint.setStyle(Paint.Style.STROKE);
        for (ReceiptLine line : lines) canvas.drawRect(line.left * getWidth(), line.top * getHeight(),
                line.right * getWidth(), line.bottom * getHeight(), paint);
        if (dragging) canvas.drawRect(x0, y0, x1, y1, paint);
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (bitmap == null) return false;
        float x = Math.max(0, Math.min(getWidth(), event.getX()));
        float y = Math.max(0, Math.min(getHeight(), event.getY()));
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                x0 = x1 = x; y0 = y1 = y; dragging = true; invalidate(); return true;
            case MotionEvent.ACTION_MOVE:
                x1 = x; y1 = y; invalidate(); return true;
            case MotionEvent.ACTION_UP:
                dragging = false;
                getParent().requestDisallowInterceptTouchEvent(false);
                if (Math.abs(x - x0) < 12 && Math.abs(y - y0) < 12) {
                    performClick();
                    float nx = x / getWidth(), ny = y / getHeight();
                    for (int i = lines.size() - 1; i >= 0; i--) {
                        ReceiptLine line = lines.get(i);
                        if (nx >= line.left && nx <= line.right && ny >= line.top && ny <= line.bottom) {
                            if (existing != null) existing.selected(line);
                            invalidate(); return true;
                        }
                    }
                }
                if (selection != null) selection.selected(Math.min(x, x0) / getWidth(),
                        Math.min(y, y0) / getHeight(), Math.max(x, x0) / getWidth(), Math.max(y, y0) / getHeight());
                invalidate(); return true;
            case MotionEvent.ACTION_CANCEL:
                dragging = false; getParent().requestDisallowInterceptTouchEvent(false); invalidate(); return true;
            default: return true;
        }
    }
    @Override public boolean performClick() {
        super.performClick();
        return true;
    }
}

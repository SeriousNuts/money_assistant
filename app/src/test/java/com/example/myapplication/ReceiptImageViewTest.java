package com.example.myapplication;

import android.graphics.Bitmap;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ReceiptImageViewTest {
    @Test public void photoDoesNotOpenPositionEditorUnlessManualSelectionIsEnabled() {
        FrameLayout parent = new FrameLayout(RuntimeEnvironment.getApplication());
        ReceiptImageView view = new ReceiptImageView(parent.getContext());
        parent.addView(view);
        view.setImage(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888), new ArrayList<>());
        parent.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY));
        parent.layout(0, 0, 400, 400);
        AtomicInteger selected = new AtomicInteger();
        view.setOnSelection((left, top, right, bottom) -> selected.incrementAndGet());
        assertFalse(touch(view, MotionEvent.ACTION_DOWN, 40, 40));
        touch(view, MotionEvent.ACTION_UP, 40, 40);
        assertEquals(0, selected.get());

        view.setSelectionEnabled(true);
        touch(view, MotionEvent.ACTION_DOWN, 40, 40);
        touch(view, MotionEvent.ACTION_UP, 40, 40);
        assertEquals(0, selected.get()); // A tap is not a new rectangle.
        touch(view, MotionEvent.ACTION_DOWN, 40, 40);
        touch(view, MotionEvent.ACTION_MOVE, 160, 100);
        touch(view, MotionEvent.ACTION_UP, 160, 100);
        assertEquals(1, selected.get());
        assertFalse(view.isSelectionEnabled());
    }

    private boolean touch(ReceiptImageView view, int action, float x, float y) {
        MotionEvent event = MotionEvent.obtain(0, 10, action, x, y, 0);
        try { return view.onTouchEvent(event); }
        finally { event.recycle(); }
    }
}

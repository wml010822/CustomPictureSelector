package com.luck.lib.camerax;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

public class FaceOverlayView extends View {
    private Paint transparentPaint;
    private Paint overlayPaint;
    private RectF ovalRect;

    public FaceOverlayView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        transparentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        transparentPaint.setColor(Color.TRANSPARENT);
        transparentPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        overlayPaint.setColor(Color.parseColor("#4D000000")); // 30% 透明度的黑色

        ovalRect = new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float ovalWidth = w * 0.7f;
        float ovalHeight = h * 0.5f;
        float left = (w - ovalWidth) / 2f;
        float top = (h - ovalHeight) / 2f - (h * 0.1f);
        ovalRect.set(left, top, left + ovalWidth, top + ovalHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(overlayPaint.getColor());
        canvas.drawOval(ovalRect, transparentPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 返回 false 表示不处理任何触摸事件
        return false;
    }
}


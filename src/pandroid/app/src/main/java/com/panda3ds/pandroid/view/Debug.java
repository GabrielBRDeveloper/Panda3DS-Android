package com.panda3ds.pandroid.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.panda3ds.pandroid.view.renderer.ConsoleRenderer;

public class Debug extends androidx.appcompat.widget.AppCompatTextView {

    private ConsoleRenderer renderer;
    private long lastUpdate = 0;

    public Debug(@NonNull Context context) {
        this(context,null);
    }

    public Debug(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public Debug(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        invalidate();
        int padding = Math.round(getResources().getDisplayMetrics().density*10);
        setPadding(padding, padding, padding, padding);
        setTextSize(TypedValue.COMPLEX_UNIT_PX, padding*1.4f);
        setShadowLayer(padding/4.0f, 0,0, Color.BLACK);
        postDelayed(this::update, 100);
    }


    private void update(){
        setText(String.format(
                "fps: %s\n" +
                        "backend: %s\n" +
                        "refresh: %s ms",
                (renderer != null ? renderer.getFps() : 0),
                (renderer != null ? renderer.getBackendName() : ""),
                (System.currentTimeMillis()-lastUpdate)
        ));
        lastUpdate = System.currentTimeMillis();
        postDelayed(this::update, 100);
    }

    public void setRenderer(ConsoleRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }
}

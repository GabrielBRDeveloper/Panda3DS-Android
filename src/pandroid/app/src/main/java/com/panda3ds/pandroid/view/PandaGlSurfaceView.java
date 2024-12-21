package com.panda3ds.pandroid.view;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Debug;
import android.view.Surface;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import com.panda3ds.pandroid.math.Vector2;
import com.panda3ds.pandroid.view.controller.TouchEvent;
import com.panda3ds.pandroid.view.controller.nodes.TouchScreenNodeImpl;
import com.panda3ds.pandroid.view.renderer.ConsoleRenderer;

public class PandaGlSurfaceView extends GLSurfaceView implements TouchScreenNodeImpl {
	final PandaGlRenderer renderer;
	private int width;
	private int height;

	public PandaGlSurfaceView(Context context, String romPath) {
		super(context);
		setEGLContextClientVersion(3);
		if (Debug.isDebuggerConnected()) {
			setDebugFlags(DEBUG_LOG_GL_CALLS);
		}
		renderer = new PandaGlRenderer(getContext(), romPath);
		setRenderer(renderer);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		super.surfaceCreated(holder);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			holder.getSurface().setFrameRate(120.0f, Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE, Surface.CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS);
		} else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
			holder.getSurface().setFrameRate(120.0f, Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE);
		}
	}

	public ConsoleRenderer getRenderer() { return renderer; }

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		width = getMeasuredWidth();
		height = getMeasuredHeight();
	}

	@NonNull
	@Override
	public Vector2 getSize() {
		return new Vector2(width, height);
	}

	@Override
	public void onTouch(TouchEvent event) {
		onTouchScreenPress(renderer, event);
	}
}
package com.won983212.vaultapp.ui.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ResizableRecyclerView extends RecyclerView implements ScaleGestureDetector.OnScaleGestureListener {
    private final ScaleGestureDetector mDetector;

    public ResizableRecyclerView(@NonNull Context context) {
        super(context);
        mDetector = new ScaleGestureDetector(context, this);
    }

    public ResizableRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mDetector = new ScaleGestureDetector(context, this);
    }

    public ResizableRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mDetector = new ScaleGestureDetector(context, this);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        mDetector.onTouchEvent(e);
        return super.onTouchEvent(e);
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        GridLayoutManager layoutManager = (GridLayoutManager) getLayoutManager();
        int span = layoutManager.getSpanCount();
        float factor = detector.getScaleFactor();

        if (factor > 1.7f) {
            if (span > 2)
                animateRecyclerLayoutChange(layoutManager, span - 1);
        } else if (factor < 0.6f) {
            if (span < 4)
                animateRecyclerLayoutChange(layoutManager, span + 1);
        }
    }

    private void animateRecyclerLayoutChange(GridLayoutManager layoutManager, int layoutSpanCount) {
        TransitionManager.beginDelayedTransition(this);
        layoutManager.setSpanCount(layoutSpanCount);
        getAdapter().notifyDataSetChanged();
    }
}

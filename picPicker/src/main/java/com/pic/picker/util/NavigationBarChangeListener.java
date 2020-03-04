package com.pic.picker.util;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;


public class NavigationBarChangeListener implements ViewTreeObserver.OnGlobalLayoutListener {

    public static final int ORIENTATION_VERTICAL = 1;
    public static final int ORIENTATION_HORIZONTAL = 2;

    private Rect rect;
    private View rootView;
    private boolean isShowNavigationBar = false;
    private int orientation;
    private OnSoftInputStateChangeListener listener;

    public NavigationBarChangeListener(View rootView, int orientation) {
        this.rootView = rootView;
        this.orientation = orientation;
        rect = new Rect();
    }

    @Override
    public void onGlobalLayout() {
        rect.setEmpty();
        rootView.getWindowVisibleDisplayFrame(rect);
        int heightDiff = 0;
        if (orientation == ORIENTATION_VERTICAL) {
            heightDiff = rootView.getHeight() - (rect.bottom - rect.top);
        } else if (orientation == ORIENTATION_HORIZONTAL) {
            heightDiff = rootView.getWidth() - (rect.right - rect.left);
        }
        int navigationBarHeight = Utils.hasVirtualNavigationBar(rootView.getContext()) ? Utils.getNavigationBarHeight(rootView.getContext()) : 0;
        if (heightDiff >= navigationBarHeight && heightDiff < navigationBarHeight * 2) {
            if (!isShowNavigationBar && listener != null) {
                listener.onNavigationBarShow(orientation, heightDiff);
            }
            isShowNavigationBar = true;
        } else {
            if (isShowNavigationBar && listener != null) {
                listener.onNavigationBarHide(orientation);
            }
            isShowNavigationBar = false;
        }
    }

    public void setListener(OnSoftInputStateChangeListener listener) {
        this.listener = listener;
    }

    public interface OnSoftInputStateChangeListener {
        void onNavigationBarShow(int orientation, int height);

        void onNavigationBarHide(int orientation);
    }

    public static NavigationBarChangeListener with(View rootView) {
        return with(rootView, ORIENTATION_VERTICAL);
    }

    public static NavigationBarChangeListener with(Activity activity) {
        return with(activity.findViewById(android.R.id.content), ORIENTATION_VERTICAL);
    }

    public static NavigationBarChangeListener with(View rootView, int orientation) {
        NavigationBarChangeListener changeListener = new NavigationBarChangeListener(rootView, orientation);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(changeListener);
        return changeListener;
    }

    public static NavigationBarChangeListener with(Activity activity, int orientation) {
        return with(activity.findViewById(android.R.id.content), orientation);
    }
}

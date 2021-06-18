package com.dadoutek.uled.widget;

import android.content.Context;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;

import top.defaults.colorpicker.ColorPickerView;

public class MyColorPickerView extends ColorPickerView {

    public boolean isDispatchTouchEvent() {
        return isDispatchTouchEvent;
    }

    public void setDispatchTouchEvent(boolean dispatchTouchEvent) {
        isDispatchTouchEvent = dispatchTouchEvent;
    }

    private boolean isDispatchTouchEvent = true;

    public MyColorPickerView(Context context) {
        super(context);
    }

    public MyColorPickerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MyColorPickerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if(isDispatchTouchEvent)
        {
           return super.dispatchTouchEvent(ev);
        }else{
            return true;
        }
    }
}

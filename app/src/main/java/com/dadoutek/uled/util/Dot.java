package com.dadoutek.uled.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.dadoutek.uled.R;

public class Dot extends View {
    public Dot(Context context) {
        super(context);
        mPaint.setColor(getResources().getColor(R.color.white));
    }

    public Dot(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mPaint.setColor(getResources().getColor(R.color.white));
    }

    public Dot(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPaint.setColor(getResources().getColor(R.color.white));
    }

    public Dot(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mPaint.setColor(getResources().getColor(R.color.white));
    }

    //Paint.ANTI_ALIAS_FLAG是使位图抗锯齿的标志
    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int radius = Math.min(width, height) / 2;
        mPaint.setShadowLayer(45, 0, 0, getContext().getResources().getColor(R.color.white));
        //canvas.drawCircle(width / 2, height / 2, radius, mPaint);
        RectF rect = new RectF();
        rect.left = 0;
        rect.right = width;
        rect.top = 0 ;
        rect.bottom = height;
        canvas.drawRoundRect(rect,5,5,mPaint);
    }

    public void setChecked(boolean checked,int color) {
        if (checked) {
            if(color == -1){
                mPaint.setColor(getResources().getColor(R.color.white));
            }else{
                mPaint.setColor(color);
            }

        } else {
            mPaint.setColor(getResources().getColor(R.color.white));
        }
        invalidate();
    }
}

package com.dadoutek.uled.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.app.hubert.guide.util.LogUtil;
import com.app.hubert.guide.util.ScreenUtils;
import com.dadoutek.uled.R;

/**
 * 创建者     ZCL
 * 创建时间   2019/10/22 17:52
 * 描述
 * <p>
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
public class RecyclerGridDecoration extends RecyclerView.ItemDecoration {

    private int[] attrs = new int[]{
            android.R.attr.listDivider
    };
    private Drawable divider;
    private int mColumn;
    private Context mContext;
    private int mTotalCount;
    private boolean isAllScreen = true;

    public RecyclerGridDecoration(Context context, int column) {
        super();
        mContext = context;
        TypedArray typedArray = context.obtainStyledAttributes(attrs);
        divider = typedArray.getDrawable(0);
        mColumn = column;
    }

    //bottomLine是否占满屏幕
    public void isAllScreen(boolean isAllScreen) {
        this.isAllScreen = isAllScreen;
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDraw(c, parent, state);
        drawLine(parent, c);
    }

    private void drawLine(RecyclerView parent, Canvas c) {
        Paint paint = new Paint();
        paint.setColor(mContext.getResources().getColor(R.color.gray_e));
        //设置画笔的宽度，这里的宽度需要和getItemOffsets()方法中的
        //left、top、right、bottom值的关系处理好，否则显示的效果会不理想
        paint.setStrokeWidth(ScreenUtils.dp2px(mContext, 1));
        //获得RecyclerView中总条目数量
        int childCount = parent.getChildCount();
        mTotalCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            //假设如果有一个header，就需要if判断i==0的情况，不需要去画线。
            // 几个header就需要去处理几次，即continue几次
//            if (i == 0) {
//                //如果是第一个item，我们就不用画边框了
//                continue;
//            }
            //获取每个子View，画上边框
            View childView = parent.getChildAt(i);
            //先获得子View在屏幕上的位置和它自身的宽高，利用这些参数去画线
            float x = childView.getX();
            float y = childView.getY();
            float width = childView.getWidth();
            float height = childView.getHeight();

            if (i % mColumn == 0) {
                View view = parent.getChildAt(i);
                float x1 = view.getX();
                float y1 = view.getY();
                //top
                c.drawLine(x1, y1, ScreenUtils.getScreenWidth(mContext), y1, paint);
            }

            if ((i + 1) % mColumn != 0) {
                //right
                c.drawLine(x + width, y, x + width, y + height, paint);
            }

            int remainder = mTotalCount % mColumn;
            int temp = mTotalCount - i;
            if (isAllScreen) {
                //不满足list.size()%column==0，bottomLine全屏
                if ((temp <= remainder) || (temp <= mColumn && remainder == 0)) {
                    //bottom
                    c.drawLine(x, y + height, ScreenUtils.getScreenWidth(mContext), y + height, paint);
                    //right
//                    c.drawLine(x + width, y, x + width, y + height + divider.getIntrinsicHeight() / 2, paint);
                    LogUtil.d("isAllScreen = true draw line x = " + (x + width));
                }
            } else {//不满足list.size()%column==0，bottomLine不全屏
                if ((temp <= remainder) || (temp <= mColumn && remainder == 0)) {
                    //bottom
                    c.drawLine(x, y + height, x + width, y + height, paint);

                    //right
//                    c.drawLine(x + width, y, x + width, y + height + divider.getIntrinsicHeight() / 2, paint);

                    LogUtil.d("draw line x = " + (x + width));
                }
            }
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State
            state) {
        int left = 0;
        int top = 0;
        int right = 0;
        int bottom = 0;
        int position = parent.getChildLayoutPosition(view);
        int remainder = mTotalCount % mColumn;
        int temp = mTotalCount - position;
        //这里判断，是为了list.size()%column==0且是最后一列
        //或者最后一列有余数，在这两种情况下，才需要去画bottomLine
//        if ((temp <= remainder) || (temp <= mColumn && remainder == 0)) {
//            //设置bottom留的边框值，这样画的bottomLine才能看见
//            bottom = divider.getIntrinsicHeight();
//        }
        outRect.set(left, top, right, bottom);
    }
}
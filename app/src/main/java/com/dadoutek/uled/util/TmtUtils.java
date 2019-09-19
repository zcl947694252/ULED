package com.dadoutek.uled.util;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dadoutek.uled.R;

/**
 * 创建者     ZCL
 * 创建时间   2016/8/29 11:25
 * 描述	      ${TODO}
 * <p/>
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${TODO}
 */
public class TmtUtils {
    public static void midToast(Context context, String str) {
        Toast toast = Toast.makeText(context, str, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);  //设置显示位置
        TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
        v.setTextColor(context.getResources().getColor(R.color.blue_background));     //设置字体颜色
        toast.show();
    }  public static void midToastW(Context context, String str) {
        Toast toast = Toast.makeText(context, str, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);  //设置显示位置
        TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
        v.setTextColor(context.getResources().getColor(R.color.white));     //设置字体颜色
        toast.show();
    }

    public static void midToastLong(Context context, String str) {
        Toast toast = Toast.makeText(context, str, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);  //设置显示位置
        TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
        v.setTextSize(15);
        v.setTextColor(context.getResources().getColor(R.color.blue_background));     //设置字体颜色
        toast.show();
    }

    public static void midToastMe(Context context, String str, String str2) {
        Toast toast = Toast.makeText(context, str, Toast.LENGTH_SHORT);

        LinearLayout view = (LinearLayout) toast.getView();
        view.setOrientation(LinearLayout.VERTICAL);
        //view.setGravity(Gravity.HORIZONTAL_GRAVITY_MASK);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        //此处相当于布局文件中的Android:layout_gravity属性
        lp.gravity = Gravity.CENTER;
        view.setLayoutParams(lp);

        TextView text = new TextView(context);
        //设置内容
        text.setText(str2);
        text.setTextColor(context.getResources().getColor(R.color.blue_background));

        //加入到第二位
        view.addView(text, 1);

        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG);

        TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
        v.setTextColor(Color.RED);     //设置字体颜色

        toast.show();
    }


}

package com.dadoutek.uled.widget;

import android.app.Dialog;
import android.content.Context;
public class BaseUpDateDialog extends Dialog {
    private int res;
    public BaseUpDateDialog(Context context, int theme, int res) {
        super(context, theme);
        // TODO 自动生成的构造函数存根
        setContentView(res);
        this.res = res;
        setCanceledOnTouchOutside(false);
    }

}
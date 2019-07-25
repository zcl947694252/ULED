package com.dadoutek.uled.widget;

import android.app.Dialog;
import android.content.Context;
public class BaseUpDateDialog extends Dialog {
    private int res;
    public BaseUpDateDialog(Context context, int theme, int res) {
        super(context, theme);
        setContentView(res);
        this.res = res;
        setCanceledOnTouchOutside(false);
    }

}
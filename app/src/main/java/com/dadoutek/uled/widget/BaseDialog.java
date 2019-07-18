package com.dadoutek.uled.widget;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

import butterknife.ButterKnife;

/**
 * Created by lsm on 2016/9/29.
 */

public abstract class BaseDialog extends Dialog {
    protected Activity activity;

    public BaseDialog(Context context, int themeResId) {
        super(context, themeResId);
        this.activity = (Activity) context;
    }

    public BaseDialog(Context context) {
        super(context);
        this.activity = (Activity) context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(activity).inflate(setLayoutId(), null);
        setContentView(view);
        ButterKnife.bind(this, view);
        initBase();
    }

    protected abstract int setLayoutId();

    protected abstract void initBase();
}

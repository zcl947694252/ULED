package com.dadoutek.uled.util;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.widget.EditText;

import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.R;

public class InputRGBColorDialog extends AlertDialog implements View.OnClickListener {

    private EditText redEditText;

    private EditText greenEditText;

    private EditText blueEditText;

    private ConstraintLayout okBtn;

    private ConstraintLayout cancelBtn;

    private String redStr;

    private String greenStr;

    private String blueStr;

    private RGBColorListener listener;

    private Context mContext;


    public InputRGBColorDialog(Context context, int style, String red, String green, String blue, RGBColorListener mListener) {
        super(context, style);
        this.redStr = red;
        this.greenStr = green;
        this.blueStr = blue;
        this.listener = mListener;
        this.mContext = context;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.input_rgb_color);

        redEditText = (EditText) findViewById(R.id.rgb_red);
        greenEditText = (EditText) findViewById(R.id.rgb_green);
        blueEditText = (EditText) findViewById(R.id.rgb_blue);



        okBtn = (ConstraintLayout) findViewById(R.id.okBtn);
        cancelBtn = (ConstraintLayout) findViewById(R.id.cancelBtn);

        redEditText.setText(redStr);
        greenEditText.setText(greenStr);
        blueEditText.setText(blueStr);

        okBtn.setOnClickListener(this);
        cancelBtn.setOnClickListener(this);
    }

    @SuppressLint("ShowToast")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.okBtn:
                String red = redEditText.getText().toString().trim();
                String green = greenEditText.getText().toString().trim();
                String blue = blueEditText.getText().toString().trim();

                if(red.equals("")){
                    ToastUtils.showShort(mContext.getString(R.string.cannot_rgb_null));
                    return;
                }

                if(green.equals("")){
                    ToastUtils.showShort(mContext.getString(R.string.cannot_rgb_null));
                    return;
                }

                if(blue.equals("")){
                    ToastUtils.showShort(mContext.getString(R.string.cannot_rgb_null));
                    return;
                }

                if(Integer.parseInt(red) >255){
                    ToastUtils.showShort(mContext.getString(R.string.rgb_max));
                    return;
                }

                if(Integer.parseInt(green) >255){
                    ToastUtils.showShort(mContext.getString(R.string.rgb_max));
                    return;
                }

                if(Integer.parseInt(blue) >255){
                    ToastUtils.showShort(mContext.getString(R.string.rgb_max));
                    return;
                }

                listener.RGBColorFinished(red,green,blue);
                dismiss();
                break;

            case R.id.cancelBtn:
                dismiss();
                break;

            default:
                break;
        }
    }

    public interface RGBColorListener {
        void RGBColorFinished(String red, String green, String blue);
    }
}

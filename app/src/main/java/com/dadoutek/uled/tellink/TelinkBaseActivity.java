package com.dadoutek.uled.tellink;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.util.ActivityUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TelinkBaseActivity extends AppCompatActivity {

    protected Toast toast;
    protected boolean foreground = false;
    private Dialog loadDialog;

    @Override
    @SuppressLint("ShowToast")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        foreground = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        foreground = false;
    }


    @Override
    protected void onResume() {
        super.onResume();
        foreground = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        this.toast.cancel();
        this.toast = null;
    }

    public void showToast(CharSequence s) {

        if (this.toast != null) {
            this.toast.setView(this.toast.getView());
            this.toast.setDuration(Toast.LENGTH_LONG);
            this.toast.setText(s);
            this.toast.show();
        }
    }

    protected void saveLog(String log) {
        ((TelinkLightApplication) getApplication()).saveLog(log);
    }

    public void showLoadingDialog(String content) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View v = inflater.inflate(R.layout.dialogview, null);

        LinearLayout layout = (LinearLayout) v.findViewById(R.id.dialog_view);
        TextView tvContent = (TextView) v.findViewById(R.id.tvContent);
        tvContent.setText(content);


//        ImageView spaceshipImage = (ImageView) v.findViewById(R.id.img);
//
//        @SuppressLint("ResourceType") Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(this,
//                R.animator.load_animation);

//        spaceshipImage.startAnimation(hyperspaceJumpAnimation);

        if (loadDialog == null) {
            loadDialog = new Dialog(this,
                    R.style.FullHeightDialog);
        }
        //loadDialog没显示才把它显示出来
        if (!loadDialog.isShowing()) {
            loadDialog.setCancelable(false);
            loadDialog.setCanceledOnTouchOutside(false);
            loadDialog.setContentView(layout);
            if(!this.isDestroyed()){
                loadDialog.show();
            }
        }
    }

    public void hideLoadingDialog() {
        if (loadDialog != null) {
            loadDialog.dismiss();
        }
    }

    public boolean compileExChar(String str) {
        return StringUtils.compileExChar(str);
    }
}

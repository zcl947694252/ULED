package com.dadoutek.uled.light;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.SharedPreferencesHelper;
import com.dadoutek.uled.othersview.MainActivity;
import com.dadoutek.uled.othersview.SplashActivity;
import com.dadoutek.uled.base.TelinkBaseActivity;
import com.dadoutek.uled.tellink.TelinkLightService;
import com.dadoutek.uled.util.SharedPreferencesUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class EmptyAddActivity extends TelinkBaseActivity {


    @BindView(R.id.tip)
    TextView tip;
    @BindView(R.id.img_add)
    ImageView imgAdd;
    @BindView(R.id.change_account)
    Button changeAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_empty_add);
        ButterKnife.bind(this);
    }

    @OnClick({R.id.tip, R.id.img_add, R.id.change_account})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.tip:
//                developerMode();
                break;
            case R.id.img_add:
                Intent intent = new Intent(EmptyAddActivity.this, DeviceScanningNewActivity.class);
                startActivity(intent);
                finish();
                break;
            case R.id.change_account:
                exitLogin();
                break;
        }
    }

    private void exitLogin() {
        SharedPreferencesHelper.putBoolean(this, Constant.IS_LOGIN, false);
        TelinkLightService instance = TelinkLightService.Instance();
        if (instance!=null)
            instance.idleMode(true);
        ActivityUtils.finishAllActivities(true);
        ActivityUtils.startActivity(SplashActivity.class);
    }

    long[] mHints = new long[6];//初始全部为0

    private void developerMode() {
        //将mHints数组内的所有元素左移一个位置
        System.arraycopy(mHints, 1, mHints, 0, mHints.length - 1);
        //获得当前系统已经启动的时间
        mHints[mHints.length - 1] = SystemClock.uptimeMillis();
        if (SystemClock.uptimeMillis() - mHints[0] <= 1000) {
            ToastUtils.showLong(R.string.developer_mode);
            SharedPreferencesUtils.setDeveloperModel(true);
            Intent intent = new Intent(EmptyAddActivity.this, MainActivity.class);
            startActivity(intent);
        }
    }
}

package com.dadoutek.uled.othersview;

import android.content.Intent;
import android.os.Bundle;

import com.blankj.utilcode.util.ActivityUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.SharedPreferencesHelper;
import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.dadoutek.uled.tellink.TelinkMeshErrorDealActivity;
import com.dadoutek.uled.user.LoginActivity;
import com.telink.bluetooth.TelinkLog;

/**
 * Created by hejiajun on 2018/3/22.
 */

public class SplashActivity extends TelinkMeshErrorDealActivity {

    private TelinkLightApplication mApplication;
    boolean mIsFirstData = true;
    boolean mIsLogging = false;
    public static final String IS_FIRST_LAUNCH = "IS_FIRST_LAUNCH";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void init() {
        this.mApplication = (TelinkLightApplication) this.getApplication();
        //this.mApplication.doDestroy();
        TelinkLog.onDestroy();
        this.mApplication.doInit();

        //判断是否是第一次使用app，启动导航页
        mIsFirstData = SharedPreferencesHelper.getBoolean(SplashActivity.this, IS_FIRST_LAUNCH, true);
        mIsLogging = SharedPreferencesHelper.getBoolean(SplashActivity.this, Constant.IS_LOGIN, false);


        if (mIsLogging) {
            ActivityUtils.startActivityForResult(this, MainActivity.class, 0);
        } else {
            gotoLoginSetting(false);
        }
    }

    private void gotoLoginSetting(Boolean isFrist) {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        intent.putExtra(IS_FIRST_LAUNCH, isFrist);
        startActivityForResult(intent, 0);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == RESULT_FIRST_USER) {
            finish();
        }
    }

    @Override
    protected void onLocationEnable() {}
}

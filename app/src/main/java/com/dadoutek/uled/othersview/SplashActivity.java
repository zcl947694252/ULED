package com.dadoutek.uled.othersview;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;

import com.dadoutek.uled.R;
import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.dadoutek.uled.tellink.TelinkMeshErrorDealActivity;
import com.dadoutek.uled.light.EmptyAddActivity;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DbModel.DBUtils;
import com.dadoutek.uled.model.SharedPreferencesHelper;
import com.dadoutek.uled.user.LoginActivity;
import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;

import io.reactivex.functions.Consumer;

/**
 * Created by hejiajun on 2018/3/22.
 */

public class SplashActivity extends TelinkMeshErrorDealActivity {

    private static final int REQ_MESH_SETTING = 0x01;
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
        this.mApplication.doInit();

        //判断是否是第一次使用app，启动导航页
        mIsFirstData = SharedPreferencesHelper.getBoolean(SplashActivity.this, IS_FIRST_LAUNCH, true);
        mIsLogging = SharedPreferencesHelper.getBoolean(SplashActivity.this, Constant.IS_LOGIN, false);

//        if (mIsFirstData) {
//            initMesh();
//            initGroupData();
//            gotoLoginSetting(true);
//            //把是否是第一次进入设为false
////            SharedPreferencesHelper.putBoolean(SplashActivity.this, IS_FIRST_LAUNCH, false);
//        } else {
        if (mIsLogging) {
            if(DBUtils.INSTANCE.getAllLight()!=null&& DBUtils.INSTANCE.getAllLight().size()==0){
                startActivity(new Intent(this,EmptyAddActivity.class));
                finish();
            }else{
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        } else {
            gotoLoginSetting(false);
        }
//        }
    }

    private void gotoLoginSetting(Boolean isFrist) {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        intent.putExtra(IS_FIRST_LAUNCH, isFrist);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onLocationEnable() {
    }




}

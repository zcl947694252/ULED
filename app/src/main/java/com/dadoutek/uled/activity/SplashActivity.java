package com.dadoutek.uled.activity;

import android.content.Intent;
import android.os.Bundle;

import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.TelinkMeshErrorDealActivity;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.Mesh;
import com.dadoutek.uled.util.DataManager;
import com.dadoutek.uled.util.FileSystem;
import com.dadoutek.uled.model.SharedPreferencesHelper;

/**
 * Created by hejiajun on 2018/3/22.
 */

public class SplashActivity extends TelinkMeshErrorDealActivity {

    private static final int REQ_MESH_SETTING = 0x01;
    private TelinkLightApplication mApplication;
    boolean mIsFirstData = true;
    public static final String IS_FIRST_LAUNCH = "IS_FIRST_LAUNCH";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        init();
    }

    private void init() {
        this.mApplication = (TelinkLightApplication) this.getApplication();
        this.mApplication.doInit();

        //判断是否是第一次使用app，启动导航页
        mIsFirstData = SharedPreferencesHelper.getBoolean(SplashActivity.this, IS_FIRST_LAUNCH, true);

        if (mIsFirstData) {
            initMesh();
            initGroupData();
            gotoMeshSetting();
            //把是否是第一次进入设为false
            SharedPreferencesHelper.putBoolean(SplashActivity.this, IS_FIRST_LAUNCH, false);
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    /**
     * 初始化分组数据
     */
    private void initGroupData() {
        Mesh mesh = mApplication.getMesh();
        DataManager dataManager = new DataManager(this, mesh.name, mesh.password);
        dataManager.creatGroup(true, 0);//初始化自动创建16个分组
    }

    @Override
    protected void onLocationEnable() {
    }

    private void initMesh() {
        Mesh mesh = (Mesh) FileSystem.readAsObject(this, Constant.NEW_MESH_NAME + "." + Constant.NEW_MESH_PASSWORD);

        if (mesh == null) {
            mesh = new Mesh();
            mesh.name = Constant.NEW_MESH_NAME;
            mesh.password = Constant.NEW_MESH_PASSWORD;
        }

        mesh.factoryName = Constant.DEFAULT_MESH_FACTORY_NAME;
        mesh.factoryPassword = Constant.DEFAULT_MESH_FACTORY_PASSWORD;

        if (mesh.saveOrUpdate(this)) {
            this.mApplication.setupMesh(mesh);
            SharedPreferencesHelper.saveMeshName(this, mesh.name);
            SharedPreferencesHelper.saveMeshPassword(this, mesh.password);
        }
    }


    private void gotoMeshSetting() {
        startActivityForResult(new Intent(this, AddMeshActivity.class), REQ_MESH_SETTING);
    }

    /**
     * 进入引导流程，也就是进入DeviceActivity。
     */
    private void gotoDeviceScanning() {
        //首次进入APP才进入引导流程
        Intent intent = new Intent(SplashActivity.this, DeviceScanningActivity.class);
        intent.putExtra("isInit", true);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //退出MeshSetting后进入DeviceScanning
        if (requestCode == REQ_MESH_SETTING) {
            gotoDeviceScanning();
        }
    }
}

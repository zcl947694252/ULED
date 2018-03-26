package com.dadou.bluetooth.light.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.dadou.bluetooth.light.R;
import com.dadou.bluetooth.light.TelinkBaseActivity;
import com.dadou.bluetooth.light.TelinkLightApplication;
import com.dadou.bluetooth.light.TelinkLightService;
import com.dadou.bluetooth.light.TelinkMeshErrorDealActivity;
import com.dadou.bluetooth.light.model.Constent;
import com.dadou.bluetooth.light.model.Mesh;
import com.dadou.bluetooth.light.model.SharedPreferencesHelper;
import com.dadou.bluetooth.light.util.FileSystem;

/**
 * Created by hejiajun on 2018/3/22.
 */

public class SplashActivity extends TelinkMeshErrorDealActivity {

    private TelinkLightApplication mApplication;
    private static final int LOCATION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
//        TelinkLightService.Instance().idleMode(true);//配置mesh前设置为空闲模式
        this.mApplication = (TelinkLightApplication) this.getApplication();
        initMesh();
        this.mApplication.doInit();
        checkPermission();
    }

    @Override
    protected void onLocationEnable() {

    }

    private void initMesh() {
        Mesh mesh = (Mesh) FileSystem.readAsObject(this, Constent.NEW_MESH_FACTORY_NAME + "." + Constent.NEW_MESH_FACTORY_PASSWORD);

        if (mesh == null) {
            mesh = new Mesh();
            mesh.name = Constent.NEW_MESH_FACTORY_NAME;
            mesh.password = Constent.NEW_MESH_FACTORY_PASSWORD;
        }

        mesh.factoryName = Constent.DEFAULT_MESH_FACTORY_NAME;
        mesh.factoryPassword = Constent.DEFAULT_MESH_FACTORY_PASSWORD;

        if (mesh.saveOrUpdate(this)) {
            this.mApplication.setupMesh(mesh);
            SharedPreferencesHelper.saveMeshName(this, mesh.name);
            SharedPreferencesHelper.saveMeshPassword(this, mesh.password);
        }
    }

    private void checkPermission(){

        if (ContextCompat.checkSelfPermission(SplashActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {// 没有权限。
            if (ActivityCompat.shouldShowRequestPermissionRationale(SplashActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)) {

                ActivityCompat.requestPermissions(SplashActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_CODE);
            } else {
                // 申请授权。
                ActivityCompat.requestPermissions(SplashActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_CODE);
            }
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                    Intent intent = new Intent(SplashActivity.this, DeviceScanningActivity.class);
                    intent.putExtra("isInit",true);
                    startActivity(intent);
                    finish();
            }

        }, 2500);
    }
}

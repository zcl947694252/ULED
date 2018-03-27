package com.dadoutek.uled.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.TelinkMeshErrorDealActivity;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.Mesh;
import com.dadoutek.uled.util.FileSystem;
import com.dadoutek.uled.model.SharedPreferencesHelper;

/**
 * Created by hejiajun on 2018/3/22.
 */

public class SplashActivity extends TelinkMeshErrorDealActivity {

    private TelinkLightApplication mApplication;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        this.mApplication = (TelinkLightApplication) this.getApplication();
        initMesh();
        this.mApplication.doInit();
        goToNextView();
    }

    @Override
    protected void onLocationEnable() {

    }

    private void initMesh() {
        Mesh mesh = (Mesh) FileSystem.readAsObject(this, Constant.NEW_MESH_FACTORY_NAME + "." + Constant.NEW_MESH_FACTORY_PASSWORD);

        if (mesh == null) {
            mesh = new Mesh();
            mesh.name = Constant.NEW_MESH_FACTORY_NAME;
            mesh.password = Constant.NEW_MESH_FACTORY_PASSWORD;
        }

        mesh.factoryName = Constant.DEFAULT_MESH_FACTORY_NAME;
        mesh.factoryPassword = Constant.DEFAULT_MESH_FACTORY_PASSWORD;

        if (mesh.saveOrUpdate(this)) {
            this.mApplication.setupMesh(mesh);
            SharedPreferencesHelper.saveMeshName(this, mesh.name);
            SharedPreferencesHelper.saveMeshPassword(this, mesh.password);
        }
    }

    private void goToNextView(){
        Intent intent = new Intent(SplashActivity.this, DeviceScanningActivity.class);
        intent.putExtra("isInit",true);
        startActivity(intent);
        finish();
    }
}

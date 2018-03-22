package com.telink.bluetooth.light.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.telink.bluetooth.light.R;
import com.telink.bluetooth.light.TelinkBaseActivity;
import com.telink.bluetooth.light.TelinkLightApplication;
import com.telink.bluetooth.light.TelinkLightService;
import com.telink.bluetooth.light.model.Constent;
import com.telink.bluetooth.light.model.Mesh;
import com.telink.bluetooth.light.model.SharedPreferencesHelper;
import com.telink.bluetooth.light.util.FileSystem;

/**
 * Created by hejiajun on 2018/3/22.
 */

public class SplashActivity extends TelinkBaseActivity {

    private TelinkLightApplication mApplication;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        TelinkLightService.Instance().idleMode(true);//配置mesh前设置为空闲模式
        this.mApplication = (TelinkLightApplication) this.getApplication();
        initMesh();
        jumpNextSet();
    }

    private void jumpNextSet() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent=new Intent(SplashActivity.this,DeviceScanningActivity.class);
                intent.putExtra("isInit",true);
                startActivity(intent);
            }
        },1000);
    }

    private void initMesh() {
        Mesh mesh = (Mesh) FileSystem.readAsObject(this, Constent.DEFAULT_MESH_FACTORY_NAME + "." + Constent.DEFAULT_MESH_FACTORY_PASSWORD);

        if (mesh == null) {
            mesh = new Mesh();
            mesh.name = Constent.DEFAULT_MESH_FACTORY_NAME;
            mesh.password = Constent.DEFAULT_MESH_FACTORY_PASSWORD;
        }

        mesh.factoryName = Constent.DEFAULT_MESH_FACTORY_NAME;
        mesh.factoryPassword = Constent.DEFAULT_MESH_FACTORY_PASSWORD;

        if (mesh.saveOrUpdate(this)) {
            this.mApplication.setupMesh(mesh);
            SharedPreferencesHelper.saveMeshName(this, mesh.name);
            SharedPreferencesHelper.saveMeshPassword(this, mesh.password);
        }
    }
}

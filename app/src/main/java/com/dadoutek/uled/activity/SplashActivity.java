package com.dadoutek.uled.activity;

import android.content.Intent;
import android.os.Bundle;

import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.TelinkMeshErrorDealActivity;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.Mesh;
import com.dadoutek.uled.model.SharedPreferencesHelper;
import com.dadoutek.uled.util.DataManager;
import com.dadoutek.uled.util.FileSystem;

/**
 * Created by hejiajun on 2018/3/22.
 */

public class SplashActivity extends TelinkMeshErrorDealActivity {

    private static final int REQ_MESH_SETTING = 0x01;
    private TelinkLightApplication mApplication;
    boolean mIsFirstData = true;
    boolean mIsLogging= false;
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
        mIsLogging=SharedPreferencesHelper.getBoolean(SplashActivity.this, Constant.IS_LOGIN, false);

        if (mIsFirstData) {
            initMesh();
            initGroupData();
            gotoLoginSetting(true);
            //把是否是第一次进入设为false
//            SharedPreferencesHelper.putBoolean(SplashActivity.this, IS_FIRST_LAUNCH, false);
        } else {
            if(mIsLogging){
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }else{
                gotoLoginSetting(false);
            }
        }
    }

    private void gotoLoginSetting(Boolean isFrist) {
        Intent intent=new Intent(SplashActivity.this,LoginActivity.class);
        intent.putExtra(IS_FIRST_LAUNCH,isFrist);
        startActivity(intent);
    }

    /**
     * 初始化分组数据
     */
    private void initGroupData() {
        Mesh mesh = mApplication.getMesh();
        DataManager dataManager = new DataManager(this, mesh.name, mesh.password);
        dataManager.createAllLightControllerGroup();//初始化自动创建
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

}

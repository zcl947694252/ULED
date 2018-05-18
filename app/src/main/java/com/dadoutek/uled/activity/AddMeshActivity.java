package com.dadoutek.uled.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkBaseActivity;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.TelinkLightService;
import com.dadoutek.uled.model.Groups;
import com.dadoutek.uled.model.Lights;
import com.dadoutek.uled.model.Mesh;
import com.dadoutek.uled.model.SharedPreferencesHelper;
import com.dadoutek.uled.qrcode.QRCodeShareActivity;
import com.dadoutek.uled.util.DBManager;
import com.dadoutek.uled.util.DataManager;
import com.dadoutek.uled.util.FileSystem;

public final class AddMeshActivity extends TelinkBaseActivity {

    private ImageView backView;
    //    private Button btn_save_filter;
    private Button btnSave;
    private Button btnShare, btnClear;

    private TelinkLightApplication mApplication;
    private boolean canBeSave = false;
    private String mOldMeshName;
    private String mOldMeshPwd;
    private String mNewMeshName;
    private String mNewMeshPwd;

    private OnClickListener clickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v == backView) {
                finish();
            } else if (v == btnSave) {
                saveData();
            } else if (v == btnShare) {
                startActivity(new Intent(AddMeshActivity.this, QRCodeShareActivity.class));
            } else if (v == btnClear) {
                if (mApplication.getMesh().devices != null) {
                    mApplication.getMesh().devices.clear();
                    mApplication.getMesh().saveOrUpdate(AddMeshActivity.this);

                    DataManager dataManager = new DataManager(AddMeshActivity.this, mNewMeshName, mNewMeshPwd);
                    dataManager.createAllLightControllerGroup();

                    dataManager.updateGroup(Groups.getInstance());
                    finish();
                }
            }
        }
    };

    private void saveData() {
        saveMesh();
        if (canBeSave) {
            //如果用户更改了控制名称或密码，那么就清空保存的组和灯
            if (!mNewMeshName.equals(mOldMeshName) || !mNewMeshPwd.equals(mOldMeshPwd)) {
                clearData();
                setResult(RESULT_OK);
            } else {
                setResult(RESULT_CANCELED);
            }
            finish();
        }
    }

    /**
     * 清除组和灯的数据
     */
    private void clearData() {
        Groups groups = Groups.getInstance();
        Lights lights = Lights.getInstance();

        groups.clear();
        lights.clear();

        DataManager dataManager = new DataManager(this, mNewMeshName, mNewMeshPwd);

        dataManager.createAllLightControllerGroup();//初始化自动创建16个分组
//        groups.add(dataManager.getGroups().get());
        lights.add(dataManager.getLights().get());

        //非本地保存的暂时清空数据库 2018-5-10 hjj
        DBManager.getInstance().deleteAllData();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.activity_add_mesh);

        this.mApplication = (TelinkLightApplication) this.getApplication();

        this.backView = (ImageView) this
                .findViewById(R.id.img_header_menu_left);
        this.backView.setOnClickListener(this.clickListener);

        this.btnSave = (Button) this.findViewById(R.id.btn_save);
        this.btnSave.setOnClickListener(this.clickListener);

        this.btnShare = (Button) findViewById(R.id.btn_share);
        this.btnShare.setOnClickListener(this.clickListener);

        this.btnClear = (Button) findViewById(R.id.btn_clear);
        this.btnClear.setOnClickListener(this.clickListener);

        TelinkLightService.Instance().idleMode(true);

        updateGUI();


    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void updateGUI() {

        if (this.mApplication.isEmptyMesh())
            return;

        EditText txtMeshName = (EditText) this.findViewById(R.id.txt_mesh_name);
        EditText txtPassword = (EditText) this
                .findViewById(R.id.txt_mesh_password);

        EditText txtFactoryMeshName = (EditText) this
                .findViewById(R.id.txt_factory_name);
        EditText txtFactoryPassword = (EditText) this
                .findViewById(R.id.txt_factory_password);

        Mesh mesh = this.mApplication.getMesh();

        txtMeshName.setText(mesh.name);
        txtPassword.setText(mesh.password);
        txtFactoryMeshName.setText(mesh.factoryName);
        txtFactoryPassword.setText(mesh.factoryPassword);

        mOldMeshName = mesh.name;
        mOldMeshPwd = mesh.password;

    }

    @SuppressLint("ShowToast")
    private void saveMesh() {

        EditText txtMeshName = (EditText) this.findViewById(R.id.txt_mesh_name);
        EditText txtPassword = (EditText) this
                .findViewById(R.id.txt_mesh_password);

        EditText txtFactoryMeshName = (EditText) this
                .findViewById(R.id.txt_factory_name);
        EditText txtFactoryPassword = (EditText) this
                .findViewById(R.id.txt_factory_password);
        //EditText otaText = (EditText) this.findViewById(R.id.ota_device);

        mNewMeshName = txtMeshName.getText().toString().trim();
        mNewMeshPwd = txtPassword.getText().toString().trim();

        String factoryName = txtFactoryMeshName.getText().toString().trim();
        String factoryPwd = txtFactoryPassword.getText().toString().trim();

        if (mNewMeshName.equals(mNewMeshPwd)) {
            showToast(getString(R.string.add_mesh_save_tip1));
            canBeSave = false;
            return;
        }

        if (mNewMeshName.equals(factoryName)) {
            showToast(getString(R.string.add_mesh_save_tip2));
            canBeSave = false;
            return;
        }

        if (mNewMeshName.length() > 16 || mNewMeshPwd.length() > 16 || factoryName.length() > 16 || factoryPwd.length() > 16) {
            showToast(getString(R.string.add_mesh_save_tip3));
            canBeSave = false;
            return;
        }
        if (compileExChar(mNewMeshName) || compileExChar(mNewMeshPwd)) {
//            showToast(getString(R.string.add_mesh_save_tip4));
            Toast.makeText(AddMeshActivity.this, getString(R.string.add_mesh_save_tip5), Toast.LENGTH_LONG).show();
            canBeSave = false;
            return;
        }


        canBeSave = true;
        Mesh mesh = (Mesh) FileSystem.readAsObject(this, mNewMeshName + "." + mNewMeshPwd);

        if (mesh == null) {
            mesh = new Mesh();
            mesh.name = mNewMeshName;
            mesh.password = mNewMeshPwd;
        }

        mesh.factoryName = factoryName;
        mesh.factoryPassword = factoryPwd;

        if (mesh.saveOrUpdate(this)) {
            this.mApplication.setupMesh(mesh);
            SharedPreferencesHelper.saveMeshName(this, mesh.name);
            SharedPreferencesHelper.saveMeshPassword(this, mesh.password);
//            this.showToast("Save Mesh Success");
        }
    }

}

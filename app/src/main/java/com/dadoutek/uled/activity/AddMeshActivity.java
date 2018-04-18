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
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.model.Mesh;
import com.dadoutek.uled.util.FileSystem;
import com.dadoutek.uled.TelinkBaseActivity;
import com.dadoutek.uled.TelinkLightService;
import com.dadoutek.uled.model.SharedPreferencesHelper;
import com.dadoutek.uled.qrcode.QRCodeShareActivity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AddMeshActivity extends TelinkBaseActivity {

    private ImageView backView;
    //    private Button btn_save_filter;
    private Button btnSave;
    private Button btnShare, btnClear;

    private TelinkLightApplication mApplication;
    private boolean canBeSave=false;

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
                }
            }
        }
    };

    private void saveData() {
        saveMesh();
        if(canBeSave){
            setResult(RESULT_OK);
            finish();
        }
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateGUI();
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

        String newfactoryName = txtMeshName.getText().toString().trim();
        String newfactoryPwd = txtPassword.getText().toString().trim();

        String factoryName = txtFactoryMeshName.getText().toString().trim();
        String factoryPwd = txtFactoryPassword.getText().toString().trim();

        if (newfactoryName.equals(newfactoryPwd)) {
            showToast(getString(R.string.add_mesh_save_tip1));
            canBeSave=false;
            return;
        }

        if (newfactoryName.equals(factoryName)) {
            showToast(getString(R.string.add_mesh_save_tip2));
            canBeSave=false;
            return;
        }

        if (newfactoryName.length() > 16 || newfactoryPwd.length() > 16 || factoryName.length() > 16 || factoryPwd.length() > 16) {
            showToast(getString(R.string.add_mesh_save_tip3));
            canBeSave=false;
            return;
        }
        if (compileExChar(newfactoryName) || compileExChar(newfactoryPwd)) {
//            showToast(getString(R.string.add_mesh_save_tip4));
            Toast.makeText(AddMeshActivity.this, getString(R.string.add_mesh_save_tip5), Toast.LENGTH_LONG).show();
            canBeSave=false;
            return;
        }


        canBeSave=true;
        Mesh mesh = (Mesh) FileSystem.readAsObject(this, newfactoryName + "." + newfactoryPwd);

        if (mesh == null) {
            mesh = new Mesh();
            mesh.name = newfactoryName;
            mesh.password = newfactoryPwd;
        }

        mesh.factoryName = factoryName;
        mesh.factoryPassword = factoryPwd;

        if (mesh.saveOrUpdate(this)) {
            this.mApplication.setupMesh(mesh);
            SharedPreferencesHelper.saveMeshName(this, mesh.name);
            SharedPreferencesHelper.saveMeshPassword(this, mesh.password);
            this.showToast("Save Mesh Success");
        }
    }

    /**

     * @prama: str 要判断是否包含特殊字符的目标字符串

     */

    private boolean compileExChar(String str){

        String limitEx="[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";

        Pattern pattern = Pattern.compile(limitEx);
        Matcher m = pattern.matcher(str);

        if( m.find()){
            return true;
        }
       return false;
    }

}

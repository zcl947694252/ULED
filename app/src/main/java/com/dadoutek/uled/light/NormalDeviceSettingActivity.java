package com.dadoutek.uled.light;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.communicate.Commander;
import com.dadoutek.uled.intf.OtaPrepareListner;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DbModel.DbLight;
import com.dadoutek.uled.ota.OTAUpdateActivity;
import com.dadoutek.uled.tellink.TelinkBaseActivity;
import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.dadoutek.uled.util.DataManager;
import com.dadoutek.uled.util.OtaPrepareUtils;
import com.telink.TelinkApplication;

public final class NormalDeviceSettingActivity extends TelinkBaseActivity {

    private ImageView backView;
    private TextView tvOta;
    TextView txtTitle;
    private NormalDeviceSettingFragment settingFragment;
    private String localVersion;

    private DbLight light;
    private int gpAddress;
    private String fromWhere;
    private TelinkLightApplication mApplication;
    private DataManager dataManager;
    private OnClickListener clickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v == backView) {
                finish();
            } else if (v == tvOta) {
                if(checkPermission()){
                    OtaPrepareUtils.instance().gotoUpdateView(NormalDeviceSettingActivity.this,localVersion,otaPrepareListner);
                }
            }
        }
    };

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private Boolean checkPermission() {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
            return false;
        } else {
            return true;
        }
    }

    OtaPrepareListner otaPrepareListner=new OtaPrepareListner() {
        @Override
        public void startGetVersion() {
            showLoadingDialog(getString(R.string.verification_version));
        }

        @Override
        public void getVersionSuccess(String s) {
            ToastUtils.showLong(R.string.verification_version_success);
            hideLoadingDialog();
        }

        @Override
        public void getVersionFail() {
            ToastUtils.showLong(R.string.verification_version_fail);
            hideLoadingDialog();
        }



        @Override
        public void downLoadFileSuccess() {
            transformView();
        }

        @Override
        public void downLoadFileFail(String message) {
            ToastUtils.showLong(R.string.download_pack_fail);
        }
    };

    private void transformView() {
        Intent intent = new Intent(NormalDeviceSettingActivity.this, OTAUpdateActivity.class);
        intent.putExtra(Constant.UPDATE_LIGHT, light);
        startActivity(intent);
        finish();
    }

    private void getVersion() {
        int dstAdress = 0;
        if (TelinkApplication.getInstance().getConnectDevice() != null) {
            Commander.INSTANCE.getDeviceVersion(light.getMeshAddr(), (s) -> {
                localVersion = s;
                if (txtTitle != null) {
                    if(OtaPrepareUtils.instance().checkSupportOta(localVersion)){
                        txtTitle.setVisibility(View.VISIBLE);
                        txtTitle.setText(localVersion);
                        light.version=localVersion;
                        tvOta.setVisibility(View.VISIBLE);
                    }else{
                        txtTitle.setVisibility(View.GONE);
                        tvOta.setVisibility(View.GONE);
                    }
                }
                return null;
            }, () -> {
                if (txtTitle != null) {
                    txtTitle.setVisibility(View.GONE);
                    tvOta.setVisibility(View.GONE);
                }
                return null;
            });
        } else {
            dstAdress = 0;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.activity_device_setting);
        initView();
        getVersion();
    }

    private void initView() {
        this.light = (DbLight) this.getIntent().getExtras().get(Constant.LIGHT_ARESS_KEY);
        this.fromWhere = this.getIntent().getStringExtra(Constant.LIGHT_REFRESH_KEY);
        this.gpAddress = this.getIntent().getIntExtra(Constant.GROUP_ARESS_KEY, 0);
        mApplication = (TelinkLightApplication) this.getApplication();
        dataManager = new DataManager(this, mApplication.getMesh().getName(), mApplication.getMesh().getPassword());
        txtTitle = (TextView) this
                .findViewById(R.id.txt_header_title);
        txtTitle.setText("");
        this.backView = (ImageView) this
                .findViewById(R.id.img_header_menu_left);
        this.backView.setOnClickListener(this.clickListener);

        this.tvOta = (TextView) this
                .findViewById(R.id.tv_ota);
        this.tvOta.setOnClickListener(this.clickListener);

        this.settingFragment = (NormalDeviceSettingFragment) this
                .getSupportFragmentManager().findFragmentById(
                        R.id.device_setting_fragment);

        if (fromWhere != null && !fromWhere.isEmpty()) {
            this.settingFragment.fromWhere = fromWhere;
            this.settingFragment.gpAddress = gpAddress;
        }
        this.settingFragment.light = light;
    }
}

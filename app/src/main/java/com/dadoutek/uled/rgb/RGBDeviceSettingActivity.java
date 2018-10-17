package com.dadoutek.uled.rgb;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
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
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.telink.TelinkApplication;
import io.reactivex.disposables.CompositeDisposable;

public final class RGBDeviceSettingActivity extends TelinkBaseActivity {

    private ImageView backView;
    private TextView tvOta;
    TextView txtTitle;
    private RGBDeviceSettingFragment settingFragment;
    private String localVersion;

    private DbLight light;
    private int gpAddress;
    private String fromWhere;
    private TelinkLightApplication mApplication;
    private DataManager dataManager;
    private CompositeDisposable mDisposable = new CompositeDisposable();
    private RxPermissions mRxPermission;
    private OnClickListener clickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v == backView) {
                finish();
            } else if (v == tvOta) {
                checkPermission();
            }
        }
    };

    private void checkPermission(){
        mDisposable.add(
                mRxPermission.request(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe(granted -> {
                    if (granted) {
                        OtaPrepareUtils.instance().gotoUpdateView(RGBDeviceSettingActivity.this,localVersion,otaPrepareListner);
                    } else {
                        ToastUtils.showLong(R.string.update_permission_tip);
                    }
                }));
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
        Intent intent = new Intent(RGBDeviceSettingActivity.this, OTAUpdateActivity.class);
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
                    txtTitle.setVisibility(View.VISIBLE);
                    txtTitle.setText(localVersion);
                    if(OtaPrepareUtils.instance().checkSupportOta(localVersion)){
                        light.version=localVersion;
                        tvOta.setVisibility(View.VISIBLE);
                    }else{
//                        txtTitle.setVisibility(View.GONE);
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
        this.setContentView(R.layout.activity_rgb_device_setting);
        initView();
        getVersion();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDisposable.dispose();
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

        this.settingFragment = (RGBDeviceSettingFragment) this
                .getSupportFragmentManager().findFragmentById(
                        R.id.device_setting_rgb_fragment);

        if (fromWhere != null && !fromWhere.isEmpty()) {
            this.settingFragment.fromWhere = fromWhere;
            this.settingFragment.gpAddress = gpAddress;
        }
        this.settingFragment.light = light;
        mRxPermission = new RxPermissions(this);
    }
}

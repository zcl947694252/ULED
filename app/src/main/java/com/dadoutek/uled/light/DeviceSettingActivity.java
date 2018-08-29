package com.dadoutek.uled.light;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.dadoutek.uled.R;
import com.dadoutek.uled.communicate.Commander;
import com.dadoutek.uled.group.LightGroupingActivity;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.DbModel.DbLight;
import com.dadoutek.uled.ota.OTAUpdateActivity;
import com.dadoutek.uled.ota.OtaActivity;
import com.dadoutek.uled.ota.OtaDeviceListActivity;
import com.dadoutek.uled.tellink.TelinkBaseActivity;
import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.dadoutek.uled.tellink.TelinkLightService;
import com.dadoutek.uled.util.DataManager;
import com.telink.TelinkApplication;
import com.telink.util.Event;
import com.telink.util.EventListener;

public final class DeviceSettingActivity extends TelinkBaseActivity {

    private ImageView backView;
    private TextView tvOta;
    TextView txtTitle;
    private DeviceSettingFragment settingFragment;

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
                TelinkLightService.Instance().idleMode(true);
                Intent intent=new Intent(DeviceSettingActivity.this, OTAUpdateActivity.class);
                intent.putExtra(Constant.UPDATE_LIGHT,light);
                startActivity(intent);
                finish();
            }
        }
    };

    private void getVersion() {
        int dstAdress = 0;
        if (TelinkApplication.getInstance().getConnectDevice() != null) {
            Commander.INSTANCE.getDeviceVersion(light.getMeshAddr(), (s) -> {
                String version = s;
                if (txtTitle != null) {
                    txtTitle.setVisibility(View.VISIBLE);
                    txtTitle.setText(version);
                    tvOta.setVisibility(View.VISIBLE);
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

        this.settingFragment = (DeviceSettingFragment) this
                .getSupportFragmentManager().findFragmentById(
                        R.id.device_setting_fragment);

        if (fromWhere != null && !fromWhere.isEmpty()) {
            this.settingFragment.fromWhere = fromWhere;
            this.settingFragment.gpAddress = gpAddress;
        }
        this.settingFragment.light = light;
    }
}

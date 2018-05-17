package com.dadoutek.uled.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkBaseActivity;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.fragments.DeviceSettingFragment;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.Light;
import com.dadoutek.uled.model.Lights;
import com.dadoutek.uled.util.DataManager;

public final class DeviceSettingActivity extends TelinkBaseActivity {

    private ImageView backView;
    private ImageView editView;
    private DeviceSettingFragment settingFragment;

    private int meshAddress;
    private int gpAddress;
    private String fromWhere;
    private TelinkLightApplication mApplication;
    private DataManager dataManager;
    private OnClickListener clickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v == backView) {
                finish();
            } else if (v == editView) {
                Intent intent = new Intent(DeviceSettingActivity.this,
                        DeviceGroupingActivity.class);
                intent.putExtra("meshAddress", meshAddress);
                startActivity(intent);
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.activity_device_setting);

        this.meshAddress = this.getIntent().getIntExtra(Constant.LIGHT_ARESS_KEY, 0);
        this.fromWhere = this.getIntent().getStringExtra(Constant.LIGHT_REFRESH_KEY);
        this.gpAddress = this.getIntent().getIntExtra(Constant.GROUP_ARESS_KEY, 0);

        mApplication = (TelinkLightApplication) this.getApplication();
        dataManager = new DataManager(this, mApplication.getMesh().name, mApplication.getMesh().password);

        Light light = Lights.getInstance().getByMeshAddress(meshAddress);

        if (light != null) {
            TextView txtTitle = (TextView) this
                    .findViewById(R.id.txt_header_title);
            txtTitle.setText(dataManager.getLightName(light));
        }

        this.backView = (ImageView) this
                .findViewById(R.id.img_header_menu_left);
        this.backView.setOnClickListener(this.clickListener);

        this.editView = (ImageView) this
                .findViewById(R.id.img_header_menu_right);
        this.editView.setOnClickListener(this.clickListener);

        this.settingFragment = (DeviceSettingFragment) this
                .getFragmentManager().findFragmentById(
                        R.id.device_setting_fragment);

        if (fromWhere != null && !fromWhere.isEmpty()) {
//            editView.setVisibility(View.GONE);
            this.settingFragment.fromWhere = fromWhere;
            this.settingFragment.gpAddress = gpAddress;
        }
        this.settingFragment.meshAddress = meshAddress;
    }
}

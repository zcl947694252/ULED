package com.dadoutek.uled.light;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.dadoutek.uled.group.DeviceGroupingActivity;
import com.dadoutek.uled.model.DbModel.DbLight;
import com.dadoutek.uled.R;
import com.dadoutek.uled.tellink.TelinkBaseActivity;
import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.util.DataManager;

public final class DeviceSettingActivity extends TelinkBaseActivity {

    private ImageView backView;
    private ImageView editView;
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
            } else if (v == editView) {
                Intent intent = new Intent(DeviceSettingActivity.this,
                        DeviceGroupingActivity.class);
                intent.putExtra("light", light);
                intent.putExtra("gpAddress", gpAddress);
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

        this.light = (DbLight) this.getIntent().getExtras().get(Constant.LIGHT_ARESS_KEY);
        this.fromWhere = this.getIntent().getStringExtra(Constant.LIGHT_REFRESH_KEY);
        this.gpAddress = this.getIntent().getIntExtra(Constant.GROUP_ARESS_KEY, 0);

        mApplication = (TelinkLightApplication) this.getApplication();
        dataManager = new DataManager(this, mApplication.getMesh().getName(), mApplication.getMesh().getPassword());

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
                .getSupportFragmentManager().findFragmentById(
                        R.id.device_setting_fragment);

        if (fromWhere != null && !fromWhere.isEmpty()) {
//            editView.setVisibility(View.GONE);
            this.settingFragment.fromWhere = fromWhere;
            this.settingFragment.gpAddress = gpAddress;
        }
        this.settingFragment.light = light;
    }
}

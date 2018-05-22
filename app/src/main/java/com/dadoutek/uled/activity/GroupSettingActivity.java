package com.dadoutek.uled.activity;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.dadoutek.uled.DbModel.DbGroup;
import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkBaseActivity;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.fragments.GroupSettingFragment;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.Group;
import com.dadoutek.uled.util.DataManager;

public final class GroupSettingActivity extends TelinkBaseActivity {

    private ImageView backView;
    private GroupSettingFragment settingFragment;
    private TelinkLightApplication mApplication;

    private DbGroup group;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

//		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.activity_group_setting);

        this.mApplication = (TelinkLightApplication) this.getApplication();
        this.group = (DbGroup) this.getIntent().getExtras().get("group");

        if (group != null) {
            TextView txtTitle = (TextView) this
                    .findViewById(R.id.txt_header_title);
            txtTitle.setText(group.getName());
        }

        this.backView = (ImageView) this
                .findViewById(R.id.img_header_menu_left);
        this.backView.setOnClickListener(this.clickListener);

        this.settingFragment = (GroupSettingFragment) this.getSupportFragmentManager()
                .findFragmentById(R.id.group_setting_fragment);

        this.settingFragment.group = group;
    }

    private OnClickListener clickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v == backView) {
                setResult(Constant.RESULT_OK);
                finish();
            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            setResult(Constant.RESULT_OK);
            finish();
            return false;
        } else {
            return super.onKeyDown(keyCode, event);
        }

    }
}

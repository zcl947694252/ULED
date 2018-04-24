package com.dadoutek.uled.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.fragments.GroupSettingFragment;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.Group;
import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkBaseActivity;
import com.dadoutek.uled.model.Groups;
import com.dadoutek.uled.util.DataManager;

public final class GroupSettingActivity extends TelinkBaseActivity {

	private ImageView backView;
	private GroupSettingFragment settingFragment;
	private TelinkLightApplication mApplication;

	private int groupAddress;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

//		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setContentView(R.layout.activity_group_setting);

		this.mApplication = (TelinkLightApplication) this.getApplication();
		this.groupAddress = this.getIntent().getIntExtra("groupAddress", 0);
		DataManager dataManager = new DataManager(this, mApplication.getMesh().name, mApplication.getMesh().password);

		Group group = dataManager.getGroup(groupAddress,GroupSettingActivity.this);

		if (group != null) {
			TextView txtTitle = (TextView) this
					.findViewById(R.id.txt_header_title);
			txtTitle.setText(group.name);
		}

		this.backView = (ImageView) this
				.findViewById(R.id.img_header_menu_left);
		this.backView.setOnClickListener(this.clickListener);

		this.settingFragment = (GroupSettingFragment) this.getFragmentManager()
				.findFragmentById(R.id.group_setting_fragment);

		this.settingFragment.groupAddress = groupAddress;
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
		}else {
			return super.onKeyDown(keyCode, event);
		}

	}
}

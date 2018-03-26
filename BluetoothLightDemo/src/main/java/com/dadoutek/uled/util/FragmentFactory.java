package com.dadoutek.uled.util;

import android.app.Fragment;

import com.dadoutek.uled.R;
import com.dadoutek.uled.fragments.DeviceListFragment;
import com.dadoutek.uled.fragments.GroupListFragment;

public abstract class FragmentFactory {

	public static Fragment createFragment(int id) {

		Fragment fragment = null;

		if (id == R.id.tab_devices) {
			fragment = new DeviceListFragment();
		} else if (id == R.id.tab_groups) {
			fragment = new GroupListFragment();
		} else if (id == R.id.tab_account) {
		}

		return fragment;
	}
}

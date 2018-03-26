package com.dadou.bluetooth.light.util;

import android.app.Fragment;

import com.dadou.bluetooth.light.R;
import com.dadou.bluetooth.light.fragments.DeviceListFragment;
import com.dadou.bluetooth.light.fragments.GroupListFragment;

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

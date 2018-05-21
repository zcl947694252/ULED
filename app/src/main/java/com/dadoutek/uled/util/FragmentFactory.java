package com.dadoutek.uled.util;

import android.app.Fragment;

public abstract class FragmentFactory {

    public static Fragment createFragment(int id) {

        Fragment fragment = null;

//        if (id == R.id.tab_devices) {
//            fragment = new DeviceListFragment();
//        } else if (id == R.id.tab_groups) {
//            fragment = new GroupListFragment();
//        } else if (id == R.id.tab_account) {
//            fragment = new MeFragment();
//        } else if (id == R.id.tab_scene) {
//            fragment = new SceneFragment();
//        }

        return fragment;
    }
}

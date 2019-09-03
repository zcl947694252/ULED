package com.dadoutek.uled.util;

import android.content.Context;

import com.blankj.utilcode.util.ToastUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.model.DbModel.DbUser;
import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.SharedPreferencesHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hejiajun on 2018/5/22.
 */

public class SharedPreferencesUtils {

    public static void saveCurrentUseRegion(long id) {
        SharedPreferencesHelper.putLong(TelinkLightApplication.Companion.getApp(),
                Constant.CURRENT_USE_REGION_KEY, id);
    }

    public static long getCurrentUseRegion() {
        return SharedPreferencesHelper.getLong(TelinkLightApplication.Companion.getApp(),
                Constant.CURRENT_USE_REGION_KEY, -1);
    }

    public static void saveCurrentLightVsersion(String vsersion) {
        SharedPreferencesHelper.putString(TelinkLightApplication.Companion.getApp(),
                Constant.CURRENT_LIGHT_VSERSION_KEY, vsersion);
    }

    public static String getCurrentLightVersion() {
        return SharedPreferencesHelper.getString(TelinkLightApplication.Companion.getApp(),
                Constant.CURRENT_LIGHT_VSERSION_KEY, "");
    }

    public static void saveCurrentUserList(String account) {
        List<String> userList = getCurrentUserList();
        if (userList == null) {
            userList = new ArrayList<>();
        }
        if (!userList.contains(account)) {
            userList.add(account);
        }
        SharedPreferencesHelper.putObject(TelinkLightApplication.Companion.getApp(),
                Constant.CURRENT_USE_LIST_KEY, userList);
    }

    public static List<String> getCurrentUserList() {
        List<String> list = (List<String>) SharedPreferencesHelper.getObject(TelinkLightApplication.Companion.getApp(),
                Constant.CURRENT_USE_LIST_KEY);
        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    //true表示用于已登陆此版本
    public static void setUserLogin(boolean model) {
        SharedPreferencesHelper.putBoolean(TelinkLightApplication.Companion.getApp(),
                Constant.USER_LOGIN, model);
    }

    //true表示用户在删除模式
    public static void setDelete(boolean model) {
        SharedPreferencesHelper.putBoolean(TelinkLightApplication.Companion.getApp(),
                Constant.IS_DELETE, model);
    }

    //true表示处于开发者模式，false标书用户模式
    public static void setDeveloperModel(boolean model) {
        SharedPreferencesHelper.putBoolean(TelinkLightApplication.Companion.getApp(),
                Constant.IS_DEVELOPER_MODE, model);
    }

    //开关状态是否是所有组
    public static void setAllLightModel(boolean model){
        SharedPreferencesHelper.putBoolean(TelinkLightApplication.Companion.getApp(),
                Constant.IS_ALL_LIGHT_MODE, model);
    }

    //true表示处于已连接，false表示未连接
    public static void setBluetoothState(boolean model) {
        SharedPreferencesHelper.putBoolean(TelinkLightApplication.Companion.getApp(),
                Constant.IS_BLUETOOTH_STATE,model);
    }

    public static boolean isDeveloperModel() {
        return SharedPreferencesHelper.getBoolean(TelinkLightApplication.Companion.getApp(),
                Constant.IS_DEVELOPER_MODE, false);
    }

    public static boolean getConnectState(Context context) {
        if (!SharedPreferencesHelper.getBoolean(context, Constant.CONNECT_STATE_SUCCESS_KEY, false)) {
            ToastUtils.showLong(context.getString(R.string.device_not_connected));
            return false;
        }
        return true;
    }

    public static void saveLastUser(String info) {
        SharedPreferencesHelper.putString(TelinkLightApplication.Companion.getApp(),
                Constant.USER_INFO, info);
    }

    public static String getLastUser() {
        return SharedPreferencesHelper.getString(TelinkLightApplication.Companion.getApp(),
                Constant.USER_INFO, "");
    }

    public static void saveUpdateFilePath(String path) {
        SharedPreferencesHelper.putString(TelinkLightApplication.Companion.getApp(),
                Constant.UPDATE_FILE_ADRESS, path);
    }

    public static String getUpdateFilePath() {
        return SharedPreferencesHelper.getString(TelinkLightApplication.Companion.getApp(),
                Constant.UPDATE_FILE_ADRESS, "");
    }

    public static void setShowGuideAgain(boolean model) {
        SharedPreferencesHelper.putBoolean(TelinkLightApplication.Companion.getApp(),
                Constant.IS_SHOWGUIDE_AGAIN, model);
    }

    public static boolean isShowGuideAgain() {
        return SharedPreferencesHelper.getBoolean(TelinkLightApplication.Companion.getApp(),
                Constant.IS_SHOWGUIDE_AGAIN, false);
    }
}

package com.dadoutek.uled.util;

import com.dadoutek.uled.model.Constants;
import com.dadoutek.uled.model.SharedPreferencesHelper;
import com.dadoutek.uled.tellink.TelinkLightApplication;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hejiajun on 2018/5/22.
 */

public class SharedPreferencesUtils {

    public static void saveCurrentUseRegionID(long id) {
        SharedPreferencesHelper.putLong(TelinkLightApplication.Companion.getApp(),
                Constants.CURRENT_USE_REGION_KEY, id);
    }

    public static long getCurrentUseRegionId() {
        return SharedPreferencesHelper.getLong(TelinkLightApplication.Companion.getApp(),
                Constants.CURRENT_USE_REGION_KEY, -1);
    }

    public static void saveCurrentLightVsersion(String vsersion) {
        SharedPreferencesHelper.putString(TelinkLightApplication.Companion.getApp(),
                Constants.CURRENT_LIGHT_VSERSION_KEY, vsersion);
    }

    public static String getCurrentLightVersion() {
        return SharedPreferencesHelper.getString(TelinkLightApplication.Companion.getApp(),
                Constants.CURRENT_LIGHT_VSERSION_KEY, "");
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
                Constants.CURRENT_USE_LIST_KEY, userList);
    }


    public static List<String> getCurrentUserList() {
        List<String> list = (List<String>) SharedPreferencesHelper.getObject(TelinkLightApplication.Companion.getApp(),
                Constants.CURRENT_USE_LIST_KEY);
        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    //true表示用于已登陆此版本
    public static void setUserLogin(boolean model) {
        SharedPreferencesHelper.putBoolean(TelinkLightApplication.Companion.getApp(),
                Constants.USER_LOGIN, model);
    }

    //true表示用户在删除模式
    public static void setDelete(boolean model) {
        SharedPreferencesHelper.putBoolean(TelinkLightApplication.Companion.getApp(),
                Constants.IS_DELETE, model);
    }

    //true表示处于开发者模式，false标书用户模式
    public static void setDeveloperModel(boolean model) {
        SharedPreferencesHelper.putBoolean(TelinkLightApplication.Companion.getApp(), Constants.IS_DEVELOPER_MODE, model);
    }

    //开关状态是否是所有组
    public static void setAllLightModel(boolean model){
        SharedPreferencesHelper.putBoolean(TelinkLightApplication.Companion.getApp(),
                Constants.IS_ALL_LIGHT_MODE, model);
    }

    //true表示处于已连接，false表示未连接
    public static void setBluetoothState(boolean model) {
        SharedPreferencesHelper.putBoolean(TelinkLightApplication.Companion.getApp(),
                Constants.IS_BLUETOOTH_STATE,model);
    }

    public static boolean isDeveloperModel() {
        return SharedPreferencesHelper.getBoolean(TelinkLightApplication.Companion.getApp(),
                Constants.IS_DEVELOPER_MODE, false);
    }


    public static void saveLastUser(String info) {
        SharedPreferencesHelper.putString(TelinkLightApplication.Companion.getApp(),
                Constants.USER_INFO, info);
    }

    public static String getLastUser() {
        return SharedPreferencesHelper.getString(TelinkLightApplication.Companion.getApp(),
                Constants.USER_INFO, "");
    }

    public static void saveUpdateFilePath(String path) {
        SharedPreferencesHelper.putString(TelinkLightApplication.Companion.getApp(),
                Constants.UPDATE_FILE_ADRESS, path);
    }

    public static String getUpdateFilePath() {
        return SharedPreferencesHelper.getString(TelinkLightApplication.Companion.getApp(),
                Constants.UPDATE_FILE_ADRESS, "");
    }


    public static void saveRegionNameList( List<String> list) {
        SharedPreferencesHelper.putObject(TelinkLightApplication.Companion.getApp(),
                Constants.REGION_LIST, list);
    }

    public static List<String> getRegionNameList() {
        List list = (List<String>)SharedPreferencesHelper.getObject(TelinkLightApplication.Companion.getApp(),
                Constants.REGION_LIST);
        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }
}

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

    public static void saveCurrentUseRegion(long id){
        SharedPreferencesHelper.putLong(TelinkLightApplication.getInstance(),
                Constant.CURRENT_USE_REGION_KEY,id);
    }

    public static long getCurrentUseRegion(){
        return SharedPreferencesHelper.getLong(TelinkLightApplication.getInstance(),
                Constant.CURRENT_USE_REGION_KEY,-1);
    }

    public static void saveCurrentLightVsersion(String vsersion){
        SharedPreferencesHelper.putString(TelinkLightApplication.getInstance(),
                Constant.CURRENT_LIGHT_VSERSION_KEY,vsersion);
    }

    public static String getCurrentLightVersion(){
        return SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(),
                Constant.CURRENT_LIGHT_VSERSION_KEY,"");
    }

    public static void saveCurrentUserList(String account){
        List<String> userList=getCurrentUserList();
        if(userList==null){
            userList=new ArrayList<>();
        }
        if(!userList.contains(account)){
            userList.add(account);
        }
        SharedPreferencesHelper.putObject(TelinkLightApplication.getInstance(),
                Constant.CURRENT_USE_LIST_KEY,userList);
    }

    public static List<String> getCurrentUserList(){
        List<String> list=(List<String>) SharedPreferencesHelper.getObject(TelinkLightApplication.getInstance(),
                Constant.CURRENT_USE_LIST_KEY);
        if(list==null){
            list=new ArrayList<>();
        }
        return list;
    }

    //true表示处于开发者模式，false标书用户模式
    public static void setDeveloperModel(boolean model){
        SharedPreferencesHelper.putBoolean(TelinkLightApplication.getInstance(),
                Constant.IS_DEVELOPER_MODE,model);
    }

    public static boolean isDeveloperModel(){
        return SharedPreferencesHelper.getBoolean(TelinkLightApplication.getInstance(),
                Constant.IS_DEVELOPER_MODE,false);
    }

    public static boolean getConnectState(Context context) {
        if (!SharedPreferencesHelper.getBoolean(context, Constant.CONNECT_STATE_SUCCESS_KEY, false)) {
            ToastUtils.showLong(context.getString(R.string.device_not_connected));
            return false;
        }
        return true;
    }

    public static void saveLastUser(String info){
         SharedPreferencesHelper.putString(TelinkLightApplication.getInstance(),
                Constant.USER_INFO,info);
    }

    public static String getLastUser(){
        return SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(),
                Constant.USER_INFO,"");
    }
}

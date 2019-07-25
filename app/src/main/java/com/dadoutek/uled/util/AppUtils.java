package com.dadoutek.uled.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.app.hubert.guide.NewbieGuide;
import com.dadoutek.uled.model.SharedPreferencesHelper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by hejiajun on 2018/4/17.
 */

public class AppUtils {

    private AppUtils() {
        /* cannot be instantiated */
        throw new UnsupportedOperationException("cannot be instantiated");
    }

    /**
     * 获取应用程序名称
     */
    public static String getAppName(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    context.getPackageName(), 0);
            int labelRes = packageInfo.applicationInfo.labelRes;
            return context.getResources().getString(labelRes);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * [获取应用程序版本名称信息]
     * @param context
     * @return 当前应用的版本名称
     */
    public static String getVersionName(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    context.getPackageName(), 0);
            return packageInfo.versionName;

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    //手机CPU信息
    public static boolean isExynosSoc() {
        String str1 = "/proc/cpuinfo";
        String str2 ;
        try {
            FileReader fr = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(fr, 8192);

            while (true) {
                str2 = localBufferedReader.readLine();
                if (str2 == null) {
                    return false;
                } else if (str2.contains("Exynos")) {
                    localBufferedReader.close();
                    return true;
                }
            }

        } catch (IOException ignored) {
            return false;
        }
    }

    public static void checkShowGuideAgain(Context context,String tag){
        if(SharedPreferencesHelper.getBoolean(context,tag,false)){
            NewbieGuide.resetLabel(context,tag);
            SharedPreferencesHelper.putBoolean(context,tag,false);
        }
    }
}

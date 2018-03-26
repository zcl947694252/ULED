package com.telink.bluetooth.light.model;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by kee on 2017/12/22.
 */

public class SharedPreferencesHelper {
    private static final String FILE_NAME = "com.telink.bluetooth.light.SharedPreferences";

    private static final String KEY_MESH_NAME = "com.telink.bluetooth.light.mesh_name";
    private static final String KEY_MESH_PASSWORD = "com.telink.bluetooth.light.mesh_password";

    public static String getMeshName(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_MESH_NAME, null);
    }

    public static String getMeshPassword(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_MESH_PASSWORD, null);
    }

    public static void saveMeshName(Context context, String name) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_MESH_NAME, name)
                .apply();
    }

    public static void saveMeshPassword(Context context, String pwd) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_MESH_PASSWORD, pwd)
                .apply();
    }

    private static final String dbName = "zeroner_info";

    public static SharedPreferences getSharePre(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(dbName,
                Context.MODE_PRIVATE);
        return preferences;
    }

    public static void putString(Context context, String key, String value) {
        SharedPreferences preferences = getSharePre(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value).apply();
    }

    public static String getString(Context context, String key, String defValue) {
        SharedPreferences preferences = getSharePre(context);
        return preferences.getString(key, defValue);
    }

    public static void putBoolean(Context context, String key, boolean value) {
        SharedPreferences preferences = getSharePre(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key, value).apply();
    }

    public static boolean getBoolean(Context context, String key,
                                     boolean defValue) {
        SharedPreferences preferences = getSharePre(context);
        return preferences.getBoolean(key, defValue);
    }

    public static void putInt(Context context, String key, int value) {
        SharedPreferences preferences = getSharePre(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(key, value).apply();
    }

    public static int getInt(Context context, String key, int defValue) {
        SharedPreferences preferences = context.getSharedPreferences(dbName,
                Context.MODE_PRIVATE);
        return preferences.getInt(key, defValue);
    }

    public static void putLong(Context context, String key, long value) {
        SharedPreferences preferences = getSharePre(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(key, value).apply();
    }

    public static long getLong(Context context, String key, long defValue) {
        SharedPreferences preferences = getSharePre(context);
        return preferences.getLong(key, defValue);
    }

    public static void putFloat(Context context, String key, float value){
        SharedPreferences preferences = getSharePre(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putFloat(key, value).apply();
    }

    public static float getFloat(Context context, String key, float defValue){
        SharedPreferences preferences = getSharePre(context);
        return preferences.getFloat(key, defValue);
    }

    public static void removeKeys(Context context, String... keys) {
        SharedPreferences preferences = getSharePre(context);
        SharedPreferences.Editor editor = preferences.edit();
        for (int i = 0; i < keys.length; i++) {
            editor.remove(keys[i]);
        }
        editor.apply();
    }

    public static void removeKey(Context context, String key) {
        SharedPreferences preferences = getSharePre(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(key);
        editor.apply();
    }
}

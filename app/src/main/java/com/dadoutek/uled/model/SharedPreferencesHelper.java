package com.dadoutek.uled.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

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

    public static void putFloat(Context context, String key, float value) {
        SharedPreferences preferences = getSharePre(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putFloat(key, value).apply();
    }

    public static float getFloat(Context context, String key, float defValue) {
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

    public static boolean putObject(Context context, String key, Object value) {
        // TODO Auto-generated method stub
        SharedPreferences share = PreferenceManager
                .getDefaultSharedPreferences(context);
        if (value == null) {
            SharedPreferences.Editor editor = share.edit().remove(key);
            return editor.commit();
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(baos);
            oos.writeObject(value);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        // 将对象放到OutputStream中
        // 将对象转换成byte数组，并将其进行base64编码
        String objectStr = new String(Base64.encode(baos.toByteArray(),
                Base64.DEFAULT));
        try {
            baos.close();
            oos.close();
        } catch (IOException e) {
// TODO Auto-generated catch block
            e.printStackTrace();
        }
        SharedPreferences.Editor editor = share.edit();
// 将编码后的字符串写到base64.xml文件中
        editor.putString(key, objectStr);
        return editor.commit();
    }

    public static Object getObject(Context context, String key) {
        SharedPreferences sharePre = PreferenceManager
                .getDefaultSharedPreferences(context);
        try {
            String wordBase64 = sharePre.getString(key, "");
// 将base64格式字符串还原成byte数组
            if (wordBase64 == null || wordBase64.equals("")) { // 不可少，否则在下面会报java.io.StreamCorruptedException
                return null;
            }
            byte[] objBytes = Base64.decode(wordBase64.getBytes(),
                    Base64.DEFAULT);
            ByteArrayInputStream bais = new ByteArrayInputStream(objBytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
// 将byte数组转换成product对象
            Object obj = ois.readObject();
            bais.close();
            ois.close();
            return obj;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

package com.dadoutek.uled.gateway.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Map;

/**
 * 创建者     ZCL
 * 创建时间   2020/3/10 18:43
 * 描述
 * <p>
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
public class GsonUtil {
    private static Gson gson = new Gson();

    private GsonUtil() {
    }

    /**
     * 转成json
     */
    public static String beanToString(Object object) {
        String gsonString = null;
        if (gson != null) {
            gsonString = gson.toJson(object);
        }
        return gsonString;
    }

    /**
     * 转成bean
     */
    public static <T> T stringToBean(String gsonString, Class<T> cls) {
        T t = null;
        if (gson != null) {
            t = gson.fromJson(gsonString, cls);
        }
        return t;
    }

    /**
     * 转成list
     */
    public static <T> ArrayList<T> stringToList(String gsonString, Class<T> cls) {
        ArrayList<T> list = new ArrayList<>();
        if (gson != null) {
            JsonArray array = new JsonParser().parse(gsonString).getAsJsonArray();
            for (final JsonElement elem : array) {
                list.add(gson.fromJson(elem, cls));
            }
        }
        return list;
    }

    /**
     * 转成list, 有可能造成类型擦除
     */
    public static <T> ArrayList<T> stringToList(String gsonString) {
        ArrayList<T> list = null;
        if (gson != null) {
            list = gson.fromJson(gsonString, new TypeToken<ArrayList<T>>() {
            }.getType());
        }
        return list;
    }

    /**
     * 转成map的
     */
    public static <T> Map<String, T> stringToMaps(String gsonString, Class<T> cls) {
        Map<String, T> map = null;
        if (gson != null) {
            map = gson.fromJson(gsonString, new TypeToken<Map<String, T>>() {
            }.getType());
        }
        return map;
    }
}

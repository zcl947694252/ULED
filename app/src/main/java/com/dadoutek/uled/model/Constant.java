package com.dadoutek.uled.model;

import android.util.Config;

import com.dadoutek.uled.BuildConfig;

import java.io.Serializable;

/**
 * Created by hejiajun on 2018/3/22.
 */

public class Constant implements Serializable {

//    public static final String NEW_MESH_NAME="987654";
//    public static final String NEW_MESH_PASSWORD="123";
//
//    public static final String DEFAULT_MESH_FACTORY_NAME="TestLocas";
//    public static final String DEFAULT_MESH_FACTORY_PASSWORD="123";

    public static final int MAX_GROUP_COUNT = 255;

    public static final String NEW_MESH_NAME = "dadou";
    public static final String NEW_MESH_PASSWORD = "123";

    public static final String DEFAULT_MESH_FACTORY_NAME = BuildConfig.DEBUG?"test5":"dadousmart";
    public static final String DEFAULT_MESH_FACTORY_PASSWORD = "123";

    public static final String GROUPS_KEY = "LastGroups";
    public static final String CURRENT_SELECT_SCENE = "CURRENT_SELECT_SCENE";
    public static final String SCENE_KEY = "SCENE_LAST";
    public static final String LIGHTS_KEY = "LastLights";
    public static final String DEFAULT_GROUP_ID = "DEFAULT_GROUP_ID";

    public static final String GROUPS_KEY_ALL = "GROUPS_KEY_ALL";

    public static final String OUT_OF_MESH_NAME = BuildConfig.DEBUG?"test5":"out_of_mesh";

    public static final int RESULT_OK = 1;
    public static final String LIGHT_ARESS_KEY = "LIGHT_ARESS_KEY";
    public static final String GROUP_ARESS_KEY = "GROUP_ARESS_KEY";
    public static final String LIGHT_REFRESH_KEY = "LIGHT_REFRESH_KEY";
    public static final String LIGHT_REFRESH_KEY_OK = "LIGHT_REFRESH_KEY_OK";

    //标志链接是否成功
    public static final String CONNECT_STATE_SUCCESS_KEY = "CONNECT_STATE_SUCCESS_KEY";

    //测试账号设置
    public static String TESTACCOUNT = "xxxx";

    //本地保存是否当前手机登陆过
    public static final String IS_LOGIN = "IS_LOGIN";

    //网络请求服务器测试地址
//    public static final String BASE_URL = "http://101.132.137.180/";

    //网络请求服务器正式地址
    public static final String BASE_URL = "https://mqtt.beesmartnet.com/";

    //用于标记当前数据库名的本地存储
    public static String DB_NAME_KEY = "DB_NAME_KEY";

    //用于标记当前数据库名的本地存储
    public static String DB_TOken_KEY = "DB_NAME_KEY";

    //当前使用的区域默认为0
    public static long CURRENT_USE_REGION_ID = 0;

    //当前使用的区域默认为0
    public static String CURRENT_USE_REGION_KEY = "CURRENT_USE_REGION_KEY";

    //当前使用的区域默认为0
    public static String CURRENT_LIGHT_VSERSION_KEY = "CURRENT_LIGHT_VSERSION_KEY";

    //当前使用过此手机登录的用户列表
    public static String CURRENT_USE_LIST_KEY = "CURRENT_USE_LIST_KEY";

    //是否是开发者模式
    public static String IS_DEVELOPER_MODE = "IS_DEVELOPER_MODE";

    //用于标记数据库改变增加
    public static final String DB_ADD = "DB_ADD";
    //用于标记数据库改变删除
    public static final String DB_DELETE = "DB_DELETE";
    //用于标记数据库改变修改
    public static final String DB_UPDATE = "DB_UPDATE";
    //用户类型
    public static String USER_TYPE = "USER_TYPE";
    //用户类型老用户
    public static String USER_TYPE_OLD = "OLD_USER";
    //用户类型新用户
    public static String USER_TYPE_NEW = "NEW_USER";
    //用户类型新用户
    public static int CTROL_PASSWORD_REGION = 1000000000;

    public static String LIGHT_STATE_KEY = "LIGHT_STATE_KEY";

    //是否在一键恢复出厂设置
    public static String DELETEING = "DELETEING";

    //管理权限本地保存key
    public static String ME_FUNCTION = "ME_FUNCTION";

    //管理权限本地保存key
    public static String OLD_INDEX_DATA = "oldIndexData";

    //是否注册跳过验证码（目前用来测试）
    public static boolean TEST_REGISTER = false;
}

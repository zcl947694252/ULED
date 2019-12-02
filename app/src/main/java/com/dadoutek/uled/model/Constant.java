package com.dadoutek.uled.model;

import java.io.Serializable;

/**
 * Created by hejiajun on 2018/3/22.
 *
 * 12-2号版本再次上传时候记得改成dadousmart
 */

public class Constant implements Serializable {

    public static final Boolean isDebug = true;
    //public static final String DEFAULT_MESH_FACTORY_NAME = "dadourd";
    public static final String DEFAULT_MESH_FACTORY_NAME = "dadousmart";
    //public static final String DEFAULT_MESH_FACTORY_NAME = "dadoutek";

    //虚拟主机号。测试服:/smartlight/test 正式服:/smartlight 服务器已做处理暂时不必添加转换
    // val WS_DEBUG_HOST = "/smartlight/test" 服务器已做处理暂时不必添加转换
    public static String WS_HOST = "/smartlight";

    //网络请求服务器java域名地址
    public static final String BASE_URL_JAVA = "https://dev.dadoutek.com/smartlight_java/";
    //网络请求服务器测试地址
    public static final String BASE_DEBUG_URL = "http://47.107.227.130/smartlight_test/";
    //长连接请求服务器域名地址
    public static String WS_BASE_URL = "ws://dev.dadoutek.com/smartlight_java/websocket-endpoint";
    //长连接测试请求服务器域名地址
    public static String WS_BASE_URL_DEBUG = "ws://dev.dadoutek.com/smartlight_test/websocket-endpoint";

    /**
     * 上线必改  正式服url stomp正式服url  dadousmart正式服  倒计是为11
     */
    public static final long downTime = isDebug ? 2 : 11;
    public static final String BASE_URL = isDebug ? BASE_DEBUG_URL : BASE_URL_JAVA;
    public static String WS_STOMP_URL = isDebug ? WS_BASE_URL_DEBUG : WS_BASE_URL;

    public static final String DEFAULT_MESH_FACTORY_PASSWORD = "123";
    //单点登录key
    public static final String LOGIN_STATE_KEY = "LOGIN_STATE_KEY";

    public static final int MAX_GROUP_COUNT = 250;
    public static final int MAX_VALUE = 100;
    public static final int MAX_SCROLL_DELAY_VALUE = 40;
    public static boolean isTeck = false;
    //public static final String DEFAULT_MESH_FACTORY_NAME = isTeck?"dadoutek":"dadousmart";

    public static final String GROUPS_KEY = "LastGroups";
    public static final String COLOR_NODE_KEY = "COLOR_NODE_KEY";
    public static final String CURRENT_SELECT_SCENE = "CURRENT_SELECT_SCENE";
    public static final String IS_CHANGE_SCENE = "IS_CHANGE_SCENE";
    public static final String IS_CHANGE_COLOR = "IS_CHANGE_COLOR";
    public static final String SCENE_KEY = "SCENE_LAST";
    public static final String GRADIENT_KEY = "GRADIENT_KEY";
    public static final String LIGHTS_KEY = "LastLights";
    public static final String DEFAULT_GROUP_ID = "DEFAULT_GROUP_ID";

    public static final String GROUPS_KEY_ALL = "GROUPS_KEY_ALL";

    public static final String OUT_OF_MESH_NAME = "out_of_mesh";

    public static final String PIR_SWITCH_MESH_NAME = DEFAULT_MESH_FACTORY_NAME;
    //public static final String PIR_SWITCH_MESH_NAME = "dadourd";

    public static final int RESULT_OK = 1;
    public static final String LIGHT_ARESS_KEY = "LIGHT_ARESS_KEY";
    public static final String GROUP_ARESS_KEY = "GROUP_ARESS_KEY";
    public static final String LIGHT_REFRESH_KEY = "LIGHT_REFRESH_KEY";
    public static final String LIGHT_REFRESH_KEY_OK = "LIGHT_REFRESH_KEY_OK";
    public static final String CURTAINS_ARESS_KEY = "CURTAINS_ARESS_KEY";
    public static final String CURTAINS_KEY = "CURTAINS_KEY";
    public static final String LIGHT_KEY = "LIGHT_KEY";
    public static final String RGB_LIGHT_KEY = "RGB_LIGHT_KEY";

    //标志链接是否成功
    public static final String CONNECT_STATE_SUCCESS_KEY = "CONNECT_STATE_SUCCESS_KEY";

    //测试账号设置
    public static String TESTACCOUNT = "xxxx";

    //本地保存是否当前手机登陆过
    public static final String IS_LOGIN = "IS_LOGIN";

    //网络请求服务器正式地址
    //public static final String BASE_URL = "http://47.107.227.130/smartlight/";


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

    //是否是所有组
    public static String IS_ALL_LIGHT_MODE = "IS_ALL_LIGHT_MODE";

    //是否重置引导显示
    public static String IS_SHOWGUIDE_AGAIN = "IS_DEVELOPER_MODE";

    //是否连接成功
    public static String IS_BLUETOOTH_STATE = "IS_BLUETOOTH_STATE";

    //用户信息
    public static String USER_INFO = "USER_INFO";

    //用户名
    public static final String USER_NAME = "NAME";

    //ps
    public static final String USER_PS = "PS";
    public static final String NOT_SHOW = "NOT_SHOW";

    //用户登录此版本
    public static String USER_LOGIN = "USER_LOGIN";

    //用户在删除模式
    public static String IS_DELETE = "IS_DELETE";

    //开发者模式
    public static String DEVELOPER_MODEL = "DEVELOPER_MODEL";

    //用户信息
    public static String UPDATE_FILE_ADRESS = "UPDATE_FILE_ADRESS";

    //用于标记数据库改变增加
    public static final String DB_ADD = "DB_ADD";
    //用于标记数据库改变删除
    public static final String DB_DELETE = "DB_DELETE";
    //用于标记数据库改变修改
    public static final String DB_UPDATE = "DB_UPDATE";
    //用户类型
    public static String USER_TYPE = "USER_TYPE";
    //    //用户类型老用户
//    public static String USER_TYPE_OLD = "OLD_USER";
    //用户类型老用户（2018-7-23：取消新老用户的标识，全部标记为新用户数据类型）
    public static String USER_TYPE_OLD = "NEW_USER";
    //用户类型新用户
    public static String USER_TYPE_NEW = "NEW_USER";
    //用户类型新用户
    public static int CTROL_PASSWORD_REGION = 1000000000;

    public static String LIGHT_STATE_KEY = "LIGHT_STATE_KEY";

    //是否在一键恢复出厂设置
    public static String DELETEING = "DELETEING";

    //管理权限本地保存key
    public static String ME_FUNCTION = "ME_FUNCTION";

    public static String DEVICE_TYPE = "DEVICE_TYPE";

    //管理权限本地保存key
    public static String OLD_INDEX_DATA = "oldIndexData";

    //管理权限本地保存key
    public static int SWITCH_PIR_ADDRESS = 0xFF;

    //是否注册跳过验证码（目前用来测试）
    public static boolean TEST_REGISTER = false;

    public static String UPDATE_LIGHT = "UPDATE_LIGHT";

    public static String FIRMWARE_TYPE_LIGHT = "FIRMWARE_TYPE_LIGHT";
    public static String FIRMWARE_TYPE_CONTROLLER = "FIRMWARE_TYPE_CONTROLLER";

    public static String PRESET_COLOR = "PRESET_COLOR";

    public static int LIGHT = 1;
    public static int CONTROLLER = 2;

    //l有频闪;ln无频闪;lns无频闪单调光;ln不带s调光调色

    //🈶频闪
    public static int LIGHT_TYPE_STROBE = 1;
    //无频闪单调光
    public static int LIGHT_TYPE_NO_STROBOSCOPIC_MONOTONE_LIGHT = 2;
    //无频闪调光调色
    public static int LIGHT_TYPE_NO_STROBO_DIMMING = 3;
    //无频闪costdown
    public static int LIGHT_TYPE_NO_STROBO_COSTDOWN = 4;
    //无频闪costdown双调光
    public static int LIGHT_TYPE_NO_STROBO_COSTDOWN_DUAL_DIMMING = 5;
    //无频闪costdown48转36V
    public static int LIGHT_TYPE_NO_STROBO_COSTDOWN_48_TO_36V = 6;

    //无频闪单调光
    public static int CONTROLLER_TYPE_NO_STROBOSCOPIC_MONOTONE_LIGHT = 1;
    //无频闪调光调色
    public static int CONTROLLER_TYPE_NO_STROBO_DIMMING = 2;
    //RGB控制器
    public static int CONTROLLER_TYPE_RGB = 3;

    public static int OTA_SUPPORT_LOWEST_VERSION = 206;

    public static int TURN_ON_THE_LIGHT_AFTER_PASSING = 0;
    public static int TURN_OFF_THE_LIGHT_AFTER_PASSING = 1;

    public static int VENDOR_ID = 0x0211;
    public static String IS_SCAN_RGB_LIGHT = "IS_SCAN_RGB_LIGHT";
    public static String IS_SCAN_CURTAIN = "IS_SCAN_CURTAIN";
    public static String TYPE_GROUP = "TYPE_GROUP";
    public static String TYPE_LIGHT = "TYPE_LIGHT";
    public static String TYPE_CURTAIN = "TYPE_CURTAIN";
    public static String TYPE_VIEW = "TYPE_VIEW";
    public static String TYPE_VIEW_ADDRESS = "TYPE_VIEW_ADDRESS";
    public static String TYPE_USER = "TYPE_USER";
    public static String TYPE_REGISTER = "TYPE_REGISTER";
    public static String TYPE_FORGET_PASSWORD = "TYPE_FORGET_PASSWORD";
    public static String TYPE_VERIFICATION_CODE = "TYPE_VERIFICATION_CODE";
    public static String TYPE_LOGIN = "TYPE_LOGIN";

    //导航页标签
    public static String TAG_GroupListFragment = "GroupListFragment";
    public static String TAG_SceneFragment = "SceneFragment";
    public static String TAG_SceneFragment1 = "SceneFragment1";
    public static String TAG_SceneFragment2 = "SceneFragment2";
    public static String TAG_DeviceScanningNewActivity = "DeviceScanningNewActivity";
    public static String TAG_SetSceneAct = "TAG_SetSceneAct";
    public static String TAG_LightsOfGroupActivity = "LightsOfGroupActivity";
    public static String TAG_ConfigSensorAct = "TAG_ConfigSensorAct";
    //    public static String TAG_GroupListFragment = "GroupListFragment";

    public static final int INSTALL_NORMAL_LIGHT = 0;
    public static final int INSTALL_RGB_LIGHT = 1;
    public static final int INSTALL_SWITCH = 2;
    public static final int INSTALL_SENSOR = 3;
    public static final int INSTALL_CURTAIN = 4;
    public static final int INSTALL_CONNECTOR = 5;

    public static final int INSTALL_LIGHT_OF_CW = 10;
    public static final int INSTALL_LIGHT_OF_RGB = 11;
    public static final int INSTALL_CURTAIN_OF = 12;
    public static final int INSTALL_RELAY_OF = 13;

    //分组标识


    //默认分组
    public static final Long DEVICE_TYPE_DEFAULT_ALL = 0L;
    //默认分组  1是所有灯的分组，-1是默认分组
    public static final Long DEVICE_TYPE_DEFAULT = -1L;
    //所有灯的分组
    public static final Long DEVICE_TYPE_NO = 1L;


    //普通灯分组
    public static final Long DEVICE_TYPE_LIGHT_NORMAL = Long.valueOf(0x04);
    //RGB分组
    public static final Long DEVICE_TYPE_LIGHT_RGB = Long.valueOf(0x06);
    //窗帘分组
    public static final Long DEVICE_TYPE_CURTAIN = Long.valueOf(0x10);
    //连接器
    public static final Long DEVICE_TYPE_CONNECTOR = Long.valueOf(0x05);

    //升级标记t
    public static final String UP_VERSION = "UP_VERSION";

    //网络授权分享人数
    public static final String SHARE_PERSON = "SHARE_PERSON";

    public static final String OTA_MAC = "OTA_MAC";
    public static final String OTA_MES_Add = "OTA_MES_Add";

    //是否是新生成的移交码
    public static final String IS_NEW_TRANSFER_CODE = "IS_NEW_TRANSFER_CODE";
    //是否是新生成的授权码
    public static final String IS_NEW_AUTHOR_CODE = "IS_NEW_AUTHOR_CODE";


    //是否显示区域弹框
    public static final String IS_SHOW_REGION_DIALOG = "IS_SHOW_REGION_DIALOG";
    //区域列表key
    public static final String REGION_LIST = "REGION_LIST";
    //区域授权列表key
    public static final String REGION_AUTHORIZE_LIST = "REGION_AUTHORIZE_LIST";

    //区域列别
    public static final int REGION_TYPE = 1;
    //授权区域列别
    public static final int REGION_AUTHORIZE_TYPE = 2;
    //判断是否是telbase类
    public static boolean isTelBase = true;
    //进行OTA的设备类型
    public static final String OTA_TYPE = "OTA_TYPE";

    //进行OTA的设备类型为开关
    public static final int SWITCH_TYPE = 3;

    //Stomp传递标识
    public static final String STOMP = "LOGIN_OUT";
    //rxbus 登出传递标识
    public static final String LOGIN_OUT = "LOGIN_OUT";
    //rxbus 解析码传递标识
    public static final String PARSE_CODE = "PARSE_CODE";
    //rxbus 取消码传递标识
    public static final String CANCEL_CODE = "CANCEL_CODE";

    //OTA 升级版本
    public static final String OTA_VERSION = "OTA_VERSION";
    //是否是重新配置
    public static final String ISCONFIRM = "isConfirm";
    public static boolean isCreat = false;
    public static final String DEVICE_NUM = "DEVICE_NUM";
}

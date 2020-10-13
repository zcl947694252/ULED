package com.dadoutek.uled.model;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * Created by hejiajun on 2018/3/22.
 * dadoutek2018
 */
public class Constant implements Serializable {
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
    public static String WS_BASE_URL_DEBUG = "ws://dev.dadoutek.com/smartlight_test/websocket" +
            "-endpoint";
    public static Boolean isDebug = true;
    //public static final String DEFAULT_MESH_FACTORY_NAME = "dadourd";
    //public static final String DEFAULT_MESH_FACTORY_NAME = "dadousmart";

    /**
     * 上线必改  正式服url stomp正式服url  dadousmart正式服  倒计是为11
     * 测试mqtt服务器地址
     * tcp://47.107.227.130:1885
     * 正式mqtt服务器地址
     * tcp://smart.dadoutek.com:1883
     */
    public static String HOST =isDebug ? "47.107.227.130":"smart.dadoutek.com";
    public static String HOST2 =isDebug ? "tcp://47.107.227.130":"tcp://smart.dadoutek.com";
    public static int PORT = isDebug ?1885:1883;

    public static final long downTime = isDebug ? 3 : 11;
    public static final String BASE_URL = isDebug ? BASE_DEBUG_URL : BASE_URL_JAVA;
    public static String WS_STOMP_URL = isDebug ? WS_BASE_URL_DEBUG : WS_BASE_URL;
    public static  String DEFAULT_MESH_FACTORY_NAME = isDebug ?"dadoutek":"dadousmart";
    //public static String DEFAULT_MESH_FACTORY_NAME = "dadoutek3";

    public static final String DEFAULT_MESH_FACTORY_PASSWORD = "123";
    //单点登录key
    public static final String LOGIN_STATE_KEY = "LOGIN_STATE_KEY";

    public static final int MAX_GROUP_COUNT = 250;
    public static final int MAX_VALUE = 100;
    public static final int MAX_SCROLL_DELAY_VALUE = 40;

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
    public static final String IS_FIRST_CONFIG_DOUBLE_SWITCH = "IS_FIRST_CONFIG_DOUBLE_SWITCH";
    public static String PIR_SWITCH_MESH_NAME = DEFAULT_MESH_FACTORY_NAME;
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
    //版本信息名
    public static String UPDATE_FILE_NAME = "UPDATE_FILE_NAME";

    //用于标记数据库改变增加
    public static final String DB_ADD = "DB_ADD";
    //用于标记数据库改变删除
    public static final String DB_DELETE = "DB_DELETE";
    //用于标记数据库改变修改
    public static final String DB_UPDATE = "DB_UPDATE";
    //用户类型
    public static String USER_TYPE = "USER_TYPE";
    //用户类型老用户（2018-7-23：取消新老用户的标识，全部标记为新用户数据类型）
    public static String USER_TYPE_OLD = "NEW_USER";
    //用户类型新用户
    public static String USER_TYPE_NEW = "NEW_USER";
    //用户类型新用户


    //管理权限本地保存key
    public static String ME_FUNCTION = "ME_FUNCTION";

    public static String DEVICE_TYPE = "DEVICE_TYPE";


    //管理权限本地保存key
    public static int SWITCH_PIR_ADDRESS = 0xFF;

    //是否注册跳过验证码（目前用来测试）
    public static boolean TEST_REGISTER = false;

    public static String UPDATE_LIGHT = "UPDATE_LIGHT";

    public static String PRESET_COLOR = "PRESET_COLOR";

    public static int OTA_SUPPORT_LOWEST_VERSION = 206;

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
    public static String TAG_SceneFragment = "SceneFragment";

    public static final int INSTALL_NORMAL_LIGHT = 0;
    public static final int INSTALL_RGB_LIGHT = 1;
    public static final int INSTALL_SWITCH = 2;
    public static final int INSTALL_SENSOR = 3;
    public static final int INSTALL_CURTAIN = 4;
    public static final int INSTALL_CONNECTOR = 5;
    public static final int INSTALL_GATEWAY = 6;
    public static final int INSTALL_ROUTER = 7;

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

    //灯的相关分组
    public static final Long DEVICE_TYPE_LIGHT = 2L;
    //灯的相关分组 不带所有组
    public static final Long DEVICE_TYPE_LIGHT_SW = 3L;

    //普通灯分组
    public static final Long DEVICE_TYPE_LIGHT_NORMAL = Long.valueOf(0x04);
    //RGB分组
    public static final Long DEVICE_TYPE_LIGHT_RGB = Long.valueOf(0x06);
    //窗帘分组
    public static final Long DEVICE_TYPE_CURTAIN = Long.valueOf(0x10);
    //连接器
    public static final Long DEVICE_TYPE_CONNECTOR = Long.valueOf(0x05);
    //网关
    public static final Long DEVICE_TYPE_GATE_WAY = Long.valueOf(0x07);

    //升级标记t
    public static final String UP_VERSION = "UP_VERSION";

    //网络授权分享人数
    public static final String SHARE_PERSON = "SHARE_PERSON";

    public static final String OTA_MAC = "OTA_MAC";
    public static final String OTA_MES_Add = "OTA_MES_Add";


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
    public static final String IS_TECK = "IS_TECK";
    public static final String EIGHT_SWITCH_TYPE = "EIGHT_SWITCH_TYPE";

    public static final String GW_COMMEND_CODE = "GW_COMMEND_CODE";
    /**
     * 网关设置wifi连接标识
     */
    public static final int GW_WIFI_VOIP = 0X10;
    /**
     * 网关开关业务标识
     */
    public static final int GW_SWITCH_VOIP = 0X11;
    /**
     * 网关用户恢复业务标识 文档上写的是恢复出厂
     */
    public static final int GW_RESET_USER_VOIP = 0X1C;
    /**
     * 网关恢复出厂业务标识
     */
    public static final int GW_RESET_VOIP = 0X12;
    /**
     * 网关定时场景标签头下发
     */
    public static final int GW_CONFIG_TIMER_LABEL_VOIP = 0X13;
    /**
     * 网关定时场景时间下发
     */
    public static final int GW_CONFIG_TIMER_TASK_VOIP = 0X14;
    /**
     * 网关删除定时场景标签:
     */
    public static final int GW_DELETE_TIMER_LABEL_VOIP = 0X15;
    /**
     * 网关删除定时场景时间
     */
    public static final int GW_DELETE_TIMER_TASK_VOIP = 0X16;
    /**
     * w网关循环场景标签头下发:
     */
    public static final int GW_CONFIG_TIME_PERIVODE_LABEL_VOIP = 0X17;
    /**
     * 网关循环时间段下发业务标识
     */
    public static final int GW_CONFIG_TIME_PERIVODE_TASK_VOIP = 0X18;
    /**
     * 网关删除循环场景标签业务标识
     */
    public static final int GW_DELETE_TIME_PERIVODE_LABEL_VOIP = 0X19;
    /**
     * 网关删除循环场景时间段业务标识
     */
    public static final int GW_DELETE_TIME_PERIVODE_TASK_VOIP = 0X1A;
    /**
     * 网关设置时区和时间连接标识
     */
    public static final int GW_TIME_ZONE_VOIP = 0X1B;
    /**
     * 开关网关
     */
    public static final int GW_GATT_SWITCH = 1;

    /**
     * 开关网关标签
     */
    public static final int GW_GATT_LABEL_SWITCH = 2;
    /**
     * 删除网关标签
     */
    public static final int GW_GATT_DELETE_LABEL = 3;
    /**
     * 删除网关标签的task时间
     */
    public static final int GW_GATT_DELETE_LABEL_TASK = 4;

    /**
     * 保存网关标签头
     */
    public static final int GW_GATT_SAVE_LABEL_HEAD = 5;


    /**
     * 保存网关定时选择时间标签头
     */
    public static final int GW_GATT_CHOSE_TIME_LABEL_HEAD = 6;

    /**
     * 保存网关定时模式task任务
     */
    public static final int GW_GATT_SAVE_TIMER_TASK_TIME = 7;

    /**
     * 保存网关循环模式选择时间标签头
     */
    public static final int GW_GATT_CHOSE_TIME_PEROIDES_LABEL_HEAD = 8;

    /**
     * 保存网关x循环模式task任务
     */
    public static final int GW_GATT_SAVE_TIMER_PERIODES_TASK_TIME = 9;
    /**
     * 是否是重新配置网关wifi
     */
    public static final String IS_GW_CONFIG_WIFI = "IS_GW_CONFIG_WIFI";
    /**
     * 0b0000 0001 and 0b1111 1111 相同为1 不同为0  等于0b0000 0001
     */
    public static final int SATURDAY = 1 << 6;
    public static final int FRIDAY = 1 << 5;
    public static final int THURSDAY = 1 << 4;
    public static final int WEDNESDAY = 1 << 3;
    public static final int TUESDAY = 1 << 2;
    public static final int MONDAY = 1 << 1;
    public static final int SUNDAY = 1 << 0;
    /**
     * 通过服务器转发命令到网关CMD
     */
    public static final int CMD_MQTT_CONTROL = 2500;
    /**
     * Y远程控制业务标识
     */
    public static final int SER_ID_GROUP_ALLON = 0x51;
    public static final int SER_ID_GROUP_ALLOFF = 0x52;
    public static final int SER_ID_GROUP_ON = 0x53;
    public static final int SER_ID_GROUP_OFF = 0x54;

    public static final int SER_ID_LIGHT_ON = 0x55;
    public static final int SER_ID_LIGHT_OFF = 0x56;
    public static final int SER_ID_RGBLIGHT_ON = 0x57;
    public static final int SER_ID_RGBLIGHT_OFF = 0x58;
    public static final int SER_ID_SCENE_ON = 0x59;
    public static final int SER_ID_CURTAIN_ON = 0x60;
    public static final int SER_ID_CURTAIN_OFF = 0x61;
    public static final int SER_ID_RELAY_ON = 0x62;
    public static final int SER_ID_RELAY_OFF = 0x63;
    public static final int SER_ID_GATEWAY_ON = 0x64;
    public static final int SER_ID_GATEWAY_OFF = 0x65;
    public static final int SER_ID_SENSOR_ON = 0x66;
    public static final int SER_ID_SENSOR_OFF = 0x67;

    public static final String LAST_MESS_ADDR="LAST_MESS_ADDR";
    /**
     * 网页锚点
     */
    public static final String WB_TYPE="wb_type";
    /**
     * 网页锚点
     */
    public static final String ROUTE_MODE="ROUTE_MODE";
    public static  Boolean IS_ROUTE_MODE=false;
    public static  Boolean IS_OPEN_AUXFUN=false;
    @Nullable
    public static final String ONE_QR="one_qr";
    @Nullable//-1：全部失败；0：全部成功；1：部分成功
    public static final int ALL_SUCCESS =0;
    public static final int ALL_FAILE =-1;
    public static final int SOME_SUCCESS =1;
    /**
     * 路由器闲置状态
     */
    public static final int ROUTER_IDLE =0;
    /**
     * 路由器正在扫描 有未确认扫描数据
     */
    public static final int ROUTER_SCANNING =1;
    /**
     * 扫描结束，有未确认扫描数据
     */
    public static final int ROUTER_SCAN_END =2;
    /**
     * 路由器OTA中
     */
    public static final int ROUTER_OTA_ING =3;
    public static  long SCAN_SERID =0;
    public static String OTA_TIME ="OTA_TIME";
}
package com.dadoutek.uled.model;

import java.io.Serializable;

/**
 * Created by hejiajun on 2018/3/27.
 */

public class Cmd implements Serializable {
    public static final int routeOTAFinish = 3018;
    public static final int routeOTAing = 3023;
    public static final int routeUpdateDeviceVersion = 3007;
    public static final int routeUpdateScenes = 3008;
    public static final int routeDeleteScenes = 3006;
    public static final int routeAddScenes = 3005;
    public static final int routeApplyScenes =4006;
    public static final int routeDeleteGroup = 3004;
    public static final int routeGroupingDevice = 3003;
    public static final int routeScanDeviceInfo = 3002;
    public static final int routeStopScan = 3019;
    public static final int routeStartScann = 3001;
    public static final int tzRouteInAccount = 3000;
    public static final int tzRouteConfigWifi = 3022;


    public static final int gwControlCallback = 2500;
    public static final int gwCreateCallback = 2000;
    public static final int gwON = 701;
    public static final int gwStatus = 700;

    public static final int unbindRegion = 3;
    public static final int parseQR = 2;
    public static final int singleLogin = 1;
    public static final int tzRouteAddGradient =3009;
    public static final int tzRouteDelGradient =3010;
    public static final int tzRouteUpdateGradient =3011;
    public static final int tzRouteConnectSwSe =3012;
    public static final int tzRouteConfigDoubleSw =3014;
    public static final int tzRouteConfigNormalSw =3013;
    public static final int tzRouteConfigEightSw =3016;
    public static final int tzRouteConfigSceneSw =3015;
    public static final int tzRouteConfigEightSesonr =3017;
    public static final int tzRouteResetFactoryBySelf =3021;
    public static final int tzRouteOpenOrClose =4000;
    public static final int tzRouteConfigBri =4002;
    public static final int tzRouteConfigTem =4004;
    public static final int tzRouteConfigRgb =4008;
    public static final int tzRouteResetFactory =4007;
    public static final int tzRouteConfigWhite =4010;
    public static final int tzRouteSysGradientApply =4012;
    public static final int tzRouteGradientApply =4014;
    public static final int tzRouteGradientStop =4016;
    public static final int tzRouteSlowUPSlowDownSw =4019;
    public static final int tzRouteSlowUPSlowDownSpeed =4020;
    public static final int tzRouteUserReset =4021;
    public static final int tzRouteSafeLock =4022;
}

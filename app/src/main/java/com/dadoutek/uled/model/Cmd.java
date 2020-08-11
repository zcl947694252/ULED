package com.dadoutek.uled.model;

import java.io.Serializable;

/**
 * Created by hejiajun on 2018/3/27.
 */

public class Cmd implements Serializable {
    public static final int routeUpdateDeviceVersion = 3007;
    public static final int routeUpdateScenes = 3008;
    public static final int routeDeleteScenes = 3006;
    public static final int routeAddScenes = 3005;
    public static final int routeDeleteGroup = 3004;
    public static final int routeGroupingDevice = 3003;
    public static final int routeScanDeviceInfo = 3002;
    public static final int routeStartScann = 3001;
    public static final int routeInAccount = 3000;


    public static final int gwControlCallback = 2500;
    public static final int gwCreateCallback = 2000;
    public static final int gwON = 701;
    public static final int gwStatus = 700;

    public static final int unbindRegion = 3;
    public static final int parseQR = 2;
    public static final int singleLogin = 1;
}

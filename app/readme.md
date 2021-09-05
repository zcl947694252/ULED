Constant 的文件中以下两行需要修改：
    public static Boolean isDebug = false;
    // 显示测试控件
    public static final Boolean isShow = false;
    
MqttService.kt的文件中 这里需要修改：
    //        host = "${Constant.HOST2}:${Constant.PORT}"
    
    
NetworkFactory.java中这里需要修改：
        if (null == api /*|| SharedPreferencesUtils.getTestType()*/) {
        
        
        
        
RequestInterface.java 类存了我们所有的网络接口


TelinkLightApplication 类中 我修改了此处地址 mesh.password = pwd // mesh.password = name chown changed it


触摸开关：触摸调光调色、双组开关、场景开关 
光能开关：调光调色、场景开光、单调光



四键： four_switch1
六键： six_switch1
八键： eight_switch1
光能单调光： light_only1
光能场景： light_scene1
光能调光调色： light_color1
触摸单调光： touch_only_light1
触摸场景：touch_scene1
触摸调光调色： touch_light_color1


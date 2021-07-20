Constant 的文件中以下两行需要修改：
    public static Boolean isDebug = false;
    // 显示测试控件
    public static final Boolean isShow = false;
    
MqttService.kt的文件中 这里需要修改：
    //        host = "${Constant.HOST2}:${Constant.PORT}"
    
    
NetworkFactory.java中这里需要修改：
        if (null == api /*|| SharedPreferencesUtils.getTestType()*/) {
        
        
        
        
RequestInterface.java 类存了我们所有的网络接口
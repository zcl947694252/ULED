package com.dadoutek.uled.othersview

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.KeyEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import com.blankj.utilcode.util.LogUtils

import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.model.Constant
import kotlinx.android.synthetic.main.toolbar.*
import java.util.*

class InstructionsForUsActivity : TelinkBaseActivity() {
    /**
    冷暖灯(单色温灯，双色温灯)  #normal-light
    全彩灯(RGB灯，RGBW灯)  #color-light
    光能场景开关  #solar-scene-switch
    光能调光调色开关  #solar-normal-switch
    触摸板调光调色开关  #touch-double-group-switch
    触摸板单调光双组开关  #touch-normal-switch
    触摸板场景开关  #touch-scene-switch
    调光调色四组八键开关  #eight-key-switch-mode-one
    场景八键开关  #eight-key-switch-mode-two
    单调光六组八键开关  #eight-key-switch-mode-three
    场景模式传感器  #sensor-scene-mode
    组模式传感器  #sensor-group-mode
    窗帘  #curtain
    接收器  #relay
    网关定时模式  #gateway-timing-mode
    网关循环模式  #gateway-loop-mode
    冷暖灯控制  #control-normal-light
    全彩灯颜色模式控制  #control-color-light-normal
    全彩灯内置渐变控制 #control-color-light-built-in-gradient
    全彩灯自定义渐变控制  #control-color-light-custom-gradient
    窗帘控制  #control-curtain
    冷暖灯群组控制  #control-normal-group
    彩灯群组控制  #control-color-light-group
    连接器群组控制  #control-relay-group
    窗帘群组控制  #control-curtain-group
    场景控制  #control-scene
    传感器控制  #control-sensor
    开关控制  #control-switch
    网关控制  #control-gateway
    分享区域  #region-authorize
    移交区域  #region-transfer
    移交账号  #account-transfer
    用户复位 #user-reset
    安全锁  #safe-lock
    上传数据  #upload-data
    灯电源设备复位  #light-reset
    开关设备复位  #switch-reset
    传感器设备复位  #sensor-reset
    窗帘设备复位  #curtain-reset
     */
    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instructions_for_us)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbarTv.text  = getString(R.string.instructions_for_use)

        webView = findViewById(R.id.webView)
        val webSettings = webView!!.settings
        //设置WebView属性，能够执行Javascript脚本
        webSettings.javaScriptEnabled = true
        //设置可以访问文件
        webSettings.allowFileAccess = true
        //加载需要显示的网页
        //支持自动适配
        webSettings.useWideViewPort = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.loadWithOverviewMode = true
        webSettings.setSupportZoom(true)  //支持放大缩小
        webSettings.allowFileAccess = true // 允许访问文件
        webSettings.saveFormData = true
        webSettings.setGeolocationEnabled(true)
        webSettings.domStorageEnabled = true
        webView!!.clearCache(true)


 /*       if (isZh(this)) {
            webView!!.loadUrl("https://dev.dadoutek.com/static/README2.0/index.html")
        } else {
            webView!!.loadUrl("http://www.dadoutek.com/app/README/index.html?lang=1")
        }*/

        var webIndex = intent.getStringExtra(Constant.WB_TYPE)

        val config: Configuration = resources.configuration
        when {
            config.locale.language.contains("zh") -> webView!!.loadUrl("https://dev.dadoutek.com/static/README2.0/index.html?lang=0$webIndex")
            config.locale.language.contains("en") -> webView!!.loadUrl("https://dev.dadoutek.com/static/README2.0/index.html?lang=1$webIndex")
            config.locale.language.contains("ru") -> webView!!.loadUrl("https://dev.dadoutek.com/static/README2.0/index.html?lang=2$webIndex")
        }

        //设置Web视图
//        webView!!.webViewClient = webViewClient()
    }


    override//设置回退 覆盖Activity类的onKeyDown(int keyCoder,KeyEvent event)方法
    fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView!!.canGoBack()) {
            webView!!.goBack() //goBack()表示返回WebView的上一页面
            return true
        }
        finish()//结束退出程序
        return false
    }

    private fun isZh(context: Context): Boolean {
        val locale = context.resources.configuration.locale
        val language = locale.language
        return language.endsWith("zh")
    }
}

package com.dadoutek.uled.rgb

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.model.Constants
import com.dadoutek.uled.model.dbModel.DbColorNode
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.InputRGBColorDialog
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import io.reactivex.Observable
import kotlinx.android.synthetic.main.activity_select_color_gradient.*
import kotlinx.android.synthetic.main.activity_select_color_gradient.color_b
import kotlinx.android.synthetic.main.activity_select_color_gradient.color_g
import kotlinx.android.synthetic.main.activity_select_color_gradient.color_picker
import kotlinx.android.synthetic.main.activity_select_color_gradient.color_r
import kotlinx.android.synthetic.main.activity_select_color_gradient.rgb_sbBrightness
import kotlinx.android.synthetic.main.activity_select_color_gradient.sbBrightness_add
import kotlinx.android.synthetic.main.activity_select_color_gradient.sbBrightness_less
import kotlinx.android.synthetic.main.activity_select_color_gradient.sbBrightness_num
import kotlinx.android.synthetic.main.activity_select_color_gradient.rgb_white_seekbar
import kotlinx.android.synthetic.main.activity_select_color_gradient.sb_w_bright_add
import kotlinx.android.synthetic.main.activity_select_color_gradient.sb_w_bright_less
import kotlinx.android.synthetic.main.activity_select_color_gradient.sb_w_bright_num
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.defaults.colorpicker.ColorObserver
import java.util.concurrent.TimeUnit

class SelectColorGradientAct : TelinkBaseActivity(), View.OnClickListener {
    private var colorNow: Int = 0
    private var sendProgress: Int = 0
    private var colorNode: DbColorNode? = null
    private var stopTracking = false

    internal var downTime: Long = 0//Button被按下时的时间
    internal var thisTime: Long = 0//while每次循环时的时间
    internal var onBtnTouch = false//Button是否被按下
    internal var tvValue = 0//TextView中的值

    private var redColor: Int? = null
    private var greenColor: Int? = null
    private var blueColor: Int? = null

    private var isColor: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_color_gradient)
        initData()
        initView()
    }

    private fun initData() {
        colorNode = intent.getSerializableExtra(Constants.COLOR_NODE_KEY) as? DbColorNode
    }

    @SuppressLint("ClickableViewAccessibility", "StringFormatMatches", "SetTextI18n")
    private fun initView() {
        toolbarTv.text = getString(R.string.color_checked_set)
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }

        color_picker.setInitialColor(Color.WHITE)
        color_picker.subscribe(colorObserver)
        color_picker!!.setOnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }

        sb_w_bright_add!!.setOnTouchListener { v, event ->
            addWhiteBright(event)
            true
        }
        sb_w_bright_less!!.setOnTouchListener { v, event ->
            lessWhiteBright(event)
            true
        }
        sbBrightness_add.setOnTouchListener { v, event ->
            addBrightness(event)
            true
        }
        sbBrightness_less.setOnTouchListener { v, event ->
            lessBrightness(event)
            true
        }

        rgb_sbBrightness.setOnSeekBarChangeListener(barChangeListener)
        rgb_white_seekbar.setOnSeekBarChangeListener(barChangeListener)

        if (Constants.IS_OPEN_AUXFUN) {
            ll_r_r.visibility = View.VISIBLE
            ll_g_g.visibility = View.VISIBLE
            ll_b_b.visibility = View.VISIBLE
        } else {
            ll_r_r.visibility = View.GONE
            ll_g_g.visibility = View.GONE
            ll_b_b.visibility = View.GONE
        }
        ll_r_r.setOnClickListener(this)
        ll_g_g.setOnClickListener(this)
        ll_b_b.setOnClickListener(this)
        if (colorNode!!.rgbw == -1) {
            rgb_sbBrightness.progress = 100
            colorNode!!.brightness = 100
            rgb_white_seekbar.progress = 0
            sbBrightness_num.text = 100.toString() + "%"
            sb_w_bright_num.text = 0.toString() + "%"

            when {
                rgb_sbBrightness!!.progress >= 100 -> {
                    sbBrightness_add.isEnabled = false
                    sbBrightness_less.isEnabled = true
                }
                rgb_sbBrightness!!.progress <= 0 -> {
                    sbBrightness_less.isEnabled = false
                    sbBrightness_add.isEnabled = true
                }
                else -> {
                    sbBrightness_less.isEnabled = true
                    sbBrightness_add.isEnabled = true
                }
            }


            when {
                rgb_white_seekbar.progress >= 100 -> {
                    sb_w_bright_add.isEnabled = false
                    sb_w_bright_less.isEnabled = true
                }
                rgb_white_seekbar.progress <= 0 -> {
                    sb_w_bright_less.isEnabled = false
                    sb_w_bright_add.isEnabled = true
                }
                else -> {
                    sb_w_bright_less.isEnabled = true
                    sb_w_bright_add.isEnabled = true
                }
            }
        } else {

            var w = ((colorNode?.rgbw ?: 0) and 0xff000000.toInt()) shr 24
            var r = Color.red(colorNode?.rgbw!!)
            var g = Color.green(colorNode?.rgbw!!)
            var b = Color.blue(colorNode?.rgbw!!)
            color_picker.setInitialColor((colorNode?.rgbw ?: 0 and 0xffffff) or 0xff000000.toInt())

            rgb_sbBrightness.progress = colorNode!!.brightness
            rgb_white_seekbar.progress = w
            sbBrightness_num.text = colorNode!!.brightness.toString() + "%"
            sb_w_bright_num.text = "$w%"

            when {
                rgb_sbBrightness!!.progress >= 100 -> {
                    sbBrightness_add.isEnabled = false
                    sbBrightness_less.isEnabled = true
                }
                rgb_sbBrightness!!.progress <= 0 -> {
                    sbBrightness_less.isEnabled = false
                    sbBrightness_add.isEnabled = true
                }
                else -> {
                    sbBrightness_less.isEnabled = true
                    sbBrightness_add.isEnabled = true
                }
            }


            when {
                rgb_white_seekbar.progress >= 100 -> {
                    sb_w_bright_add.isEnabled = false
                    sb_w_bright_less.isEnabled = true
                }
                rgb_white_seekbar.progress <= 0 -> {
                    sb_w_bright_less.isEnabled = false
                    sb_w_bright_add.isEnabled = true
                }
                else -> {
                    sb_w_bright_less.isEnabled = true
                    sb_w_bright_add.isEnabled = true
                }
            }

            color_r.text = r.toString()
            color_g.text = g.toString()
            color_b.text = b.toString()
        }

        btn_save.setOnClickListener(this)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_save -> {
                doFinish()
            }

            R.id.ll_r_r -> {
                var dialog = InputRGBColorDialog(this, R.style.Dialog, color_r.text.toString(), color_g.text.toString(), color_b.text.toString(), InputRGBColorDialog.RGBColorListener { red, green, blue ->
                    redColor = red.toInt()
                    greenColor = green.toInt()
                    blueColor = blue.toInt()
                    setColorPicker()
                    isColor = true
                })
                dialog.show()
                dialog.window!!.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            }

            R.id.ll_g_g -> {
                var dialog = InputRGBColorDialog(this, R.style.Dialog, color_r.text.toString(), color_g.text.toString(), color_b.text.toString(), InputRGBColorDialog.RGBColorListener { red, green, blue ->
                    redColor = red.toInt()
                    greenColor = green.toInt()
                    blueColor = blue.toInt()

                    setColorPicker()
                    isColor = true
                })
                dialog.show()
                dialog.window!!.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            }

            R.id.ll_b_b -> {

                var dialog = InputRGBColorDialog(this, R.style.Dialog, color_r.text.toString(), color_g.text.toString(), color_b.text.toString(), InputRGBColorDialog.RGBColorListener { red, green, blue ->
                    redColor = red.toInt()
                    greenColor = green.toInt()
                    blueColor = blue.toInt()

                    setColorPicker()
                    isColor = true
                })
                dialog.show()
                dialog.window!!.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)

            }
        }
    }

    private fun setColorPicker() {
        val r = redColor!!
        val g = greenColor!!
        val b = blueColor!!
        color_r?.text = r.toString()
        color_g?.text = g.toString()
        color_b?.text = b.toString()
        var w = rgb_white_seekbar.progress
        if (w <= 0) w = 1
        val color: Int = (w shl 24) or (r shl 16) or (g shl 8) or b

        var ws = (color!! and 0xff000000.toInt()) shr 24
        val red = (color!! and 0xff0000) shr 16
        val green = (color and 0x00ff00) shr 8
        val blue = color and 0x0000ff

        color_picker.setInitialColor((color and 0xffffff) or 0xff000000.toInt())
        GlobalScope.launch {

            try {

                if (w!! > Constants.MAX_VALUE) {
                    w = Constants.MAX_VALUE
                }
                if (ws > Constants.MAX_VALUE) {
                    ws = Constants.MAX_VALUE
                }
                if (ws == -1) {
                    ws = 0
                }

                delay(80)
                changeColor(red.toByte(), green.toByte(), blue.toByte(), color, true)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        colorNode!!.rgbw = color

        color_r?.text = red.toString()
        color_g?.text = green.toString()
        color_b?.text = blue.toString()
    }

    private fun doFinish() {
        var intent = Intent()
        intent.putExtra("color", colorNode)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private val colorObserver = ColorObserver { color, fromUser ->
        isColor = true
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val w = rgb_white_seekbar.progress

        colorNow = (w shl 24) or (r shl 16) or (g shl 8) or b
        Log.d("", "onColorSelected: " + Integer.toHexString(colorNow))
        if (fromUser) {
            when {
                r == 0 && g == 0 && b == 0 -> {
                }
                else -> changeColor(r.toByte(), g.toByte(), b.toByte(), colorNow, false)
            }
        }

        color_r?.text = r.toString()
        color_g?.text = g.toString()
        color_b?.text = b.toString()

        colorNode!!.rgbw = colorNow
    }

    private fun changeColor(R: Byte, G: Byte, B: Byte, color: Int, isOnceSet: Boolean) {
        if (Constants.IS_ROUTE_MODE)//路由发送色盘之不用发送白光 亮度 色温等 白光在color内已经存在
            routerConfigRGBNum(colorNode!!.dstAddress, 6, color)
        else
            GlobalScope.launch {
                var red = R
                var green = G
                var blue = B

                val opcode = Opcode.SET_TEMPERATURE

                val params = byteArrayOf(0x04, red, green, blue)

                val logStr = String.format("R = %x, G = %x, B = %x", red, green, blue)
                Log.d("RGBCOLOR", logStr)

                if (isOnceSet) {
                    for (i in 0..3) {
                        delay(50)
                        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, colorNode!!.dstAddress, params)
                    }
                } else {
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, colorNode!!.dstAddress, params)
                }
            }
    }

    @SuppressLint("CheckResult")
    private fun routerConfigRGBNum(meshAddr: Int, deviceType: Int, color: Int) {
        RouterModel.routeConfigRGBNum(meshAddr, deviceType, color, "setDiyRGB")?.subscribe({
            LogUtils.v("zcl-----------收到路由调节色盘成功-------")
            when (it.errorCode) {
                0 -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    disposableRouteTimer?.dispose()
                    disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                            .subscribe {
                                hideLoadingDialog()
                                ToastUtils.showShort(getString(R.string.congfig_rgb_fail))
                            }
                }
                90020 -> ToastUtils.showShort(getString(R.string.gradient_not_exit))
                90018 -> ToastUtils.showShort(getString(R.string.device_not_exit))
                90008 -> ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                90007 -> ToastUtils.showShort(getString(R.string.gp_not_exit))
                90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                90004 -> ToastUtils.showShort(getString(R.string.region_not_router))
                else-> ToastUtils.showShort(it.message)
            }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    private val barChangeListener = object : SeekBar.OnSeekBarChangeListener {

        private var preTime: Long = 0
        private val delayTime = Constants.MAX_SCROLL_DELAY_VALUE

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            stopTracking = true
            this.onValueChange(seekBar, seekBar.progress, true)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            stopTracking = false
            this.preTime = System.currentTimeMillis()
            this.onValueChange(seekBar, seekBar.progress, true)
        }

        override fun onProgressChanged(seekBar: SeekBar, progress: Int,
                                       fromUser: Boolean) {
            onValueChangeView(seekBar, progress)
            val currentTime = System.currentTimeMillis()

            if (currentTime - this.preTime > this.delayTime) {
                this.onValueChange(seekBar, progress, true)
                this.preTime = currentTime
            }
        }

        @SuppressLint("SetTextI18n")
        private fun onValueChangeView(view: View, progress: Int) {
            if (view === rgb_sbBrightness) {
                sbBrightness_num.text = "$progress%"
            } else if (view === rgb_white_seekbar) {
                sb_w_bright_num.text = "$progress%"
            }
        }

        private fun onValueChange(view: View, progress: Int, immediate: Boolean) {
            var addr = colorNode!!.dstAddress
            sendProgress = progress
            when (view) {
                rgb_sbBrightness -> {
                    when {
                        progress >= 100 -> {
                            sbBrightness_add.isEnabled = false
                            sbBrightness_less.isEnabled = true
                        }
                        progress <= 0 -> {
                            sbBrightness_less.isEnabled = false
                            sbBrightness_add.isEnabled = true
                        }
                        else -> {
                            sbBrightness_less.isEnabled = true
                            sbBrightness_add.isEnabled = true
                        }
                    }
                    if (!Constants.IS_ROUTE_MODE)
                        sendBri(progress, addr, immediate)
                    else
                        routerConfigBriOrWhite(true)

                }
                rgb_white_seekbar -> {

                    when {
                        progress >= 100 -> {
                            sb_w_bright_add.isEnabled = false
                            sb_w_bright_less.isEnabled = true
                        }
                        progress <= 0 -> {
                            sb_w_bright_less.isEnabled = false
                            sb_w_bright_add.isEnabled = true
                        }
                        else -> {
                            sb_w_bright_add.isEnabled = true
                            sb_w_bright_less.isEnabled = true
                        }
                    }
                    if (!Constants.IS_ROUTE_MODE)
                        sendWhiteCommend(progress, addr, immediate)
                    else
                        routerConfigBriOrWhite(false)
                }
            }
        }
    }

    private fun sendWhiteCommend(progress: Int, addr: Int, immediate: Boolean) {
        var opcode = Opcode.SET_W_LUM
        var params = byteArrayOf(progress.toByte())
        afterSendWhite()
        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params, immediate)
    }

    private fun afterSendWhite() {
        var color = colorNode!!.rgbw

        val red = (color!! and 0xff0000) shr 16
        val green = (color and 0x00ff00) shr 8
        val blue = color and 0x0000ff
        val w = sendProgress

        colorNode?.rgbw = (w shl 24) or (red shl 16) or (green shl 8) or blue
    }

    private fun sendBri(progress: Int, addr: Int, immediate: Boolean) {
        val opcode: Byte = Opcode.SET_LUM
        val params: ByteArray = byteArrayOf(progress.toByte())
        colorNode!!.brightness = progress
        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params, immediate)
    }

    private fun routerConfigBriOrWhite(isBrightness: Boolean) = when {
        isBrightness -> {//亮度
            routeConfigBriGpOrLight(colorNode!!.dstAddress, 6, rgb_sbBrightness.progress, "diyBri")
        }
        else -> {
            routeConfigWhiteGpOrLight(colorNode!!.dstAddress, 6, rgb_white_seekbar.progress, "diywhite")
        }
    }

    @SuppressLint("CheckResult")
    open fun routeConfigWhiteGpOrLight(meshAddr: Int, deviceType: Int, white: Int, serId: String) {
        LogUtils.v("zcl----------- zcl-----------发送路由调白色参数-------$white-------")
        var gpColor = colorNode!!.rgbw

        val red = (gpColor and 0xff0000) shr 16
        val green = (gpColor and 0x00ff00) shr 8
        val blue = gpColor and 0x0000ff
        var color = (white shl 24) or (red shl 16) or (green shl 8) or blue
        //var ws = (color and 0xff000000.toInt()) shr 24//白色
        RouterModel.routeConfigWhiteNum(meshAddr, deviceType, color, serId)?.subscribe({
            //    "errorCode": 90018"该设备不存在，请重新刷新数据"    "errorCode": 90008,"该设备没有绑定路由，无法操作"
            //    "errorCode": 90007,"该组不存在，请重新刷新数据    "errorCode": 90005"message": "该设备绑定的路由没在线"
            when (it.errorCode) {
                0 -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    disposableRouteTimer?.dispose()
                    disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                            .subscribe {
                                hideLoadingDialog()
                                ToastUtils.showShort(getString(R.string.config_white_fail))
                            }
                }
                90018 -> {
                    ToastUtils.showShort(getString(R.string.device_not_exit))
                    SyncDataPutOrGetUtils.syncGetDataStart(DBUtils.lastUser!!, syncCallbackGet)
                }
                90008 -> ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                90007 -> ToastUtils.showShort(getString(R.string.gp_not_exit))
                90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                else-> ToastUtils.showShort(it.message)
            }
        }) {
            ToastUtils.showShort(it.message)
        }
    }

    override fun tzRouterConfigRGB(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl------收到路由调节色盘通知------------$cmdBean")
        hideLoadingDialog()
        if (cmdBean.ser_id == "setDiyRGB") {
            disposableRouteTimer?.dispose()
            if (cmdBean.status == 0) {
                color_picker.setInitialColor((colorNow and 0xffffff) or 0xff000000.toInt())
                colorNode?.rgbw = colorNow
            } else {
                color_picker.setInitialColor((colorNode!!.rgbw and 0xffffff) or 0xff000000.toInt())
            }
        }
    }

    override fun tzRouterConfigWhite(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl------收到路由配置白光灯通知------------$cmdBean----$sendProgress")
        disposableRouteTimer?.dispose()
        when (cmdBean.status) {
            0 -> {
                afterSendWhite()
                hideLoadingDialog()
            }
            else -> {
                hideLoadingDialog()
                var ws = (colorNode!!.rgbw and 0xff000000.toInt()) shr 24
                rgb_white_seekbar.progress = ws
                ToastUtils.showShort(getString(R.string.config_white_fail))
            }
        }
    }

    override fun tzRouterConfigBriOrTemp(cmdBean: CmdBodyBean, isBri: Boolean) {
        LogUtils.v("zcl------收到路由配置白光灯通知------------$cmdBean------$sendProgress")
        disposableRouteTimer?.dispose()
        when (cmdBean.status) {
            0 -> {
                colorNode!!.brightness = sendProgress
                hideLoadingDialog()
            }
            else -> {
                hideLoadingDialog()
                rgb_sbBrightness.progress = colorNode!!.brightness
                //sb_w_bright_num.text="$ws%"
                ToastUtils.showShort(getString(R.string.config_white_fail))
            }
        }
    }


    private fun lessBrightness(event: MotionEvent?) {
        when {
            event!!.action == MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                onBtnTouch = true
                GlobalScope.launch {
                    while (onBtnTouch) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            val msg = handlerBrightnessLess.obtainMessage()
                            msg.arg1 = tvValue
                            handlerBrightnessLess.sendMessage(msg)
                            Log.e("TAG_TOUCH", tvValue++.toString())
                            try {
                                delay(100)
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }

                        }
                    }
                }

            }
            event.action == MotionEvent.ACTION_UP -> {
                onBtnTouch = false
                if (thisTime - downTime < 500) {
                    tvValue++
                    val msg = handlerBrightnessLess.obtainMessage()
                    msg.arg1 = tvValue
                    handlerBrightnessLess.sendMessage(msg)
                }
            }
            event.action == MotionEvent.ACTION_CANCEL -> {
                onBtnTouch = false
            }
        }
    }

    private fun addBrightness(event: MotionEvent?) {
        when {
            event!!.action == MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                onBtnTouch = true
                GlobalScope.launch {
                    while (onBtnTouch) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            val msg = handlerBrightnessAdd.obtainMessage()
                            msg.arg1 = tvValue
                            handlerBrightnessAdd.sendMessage(msg)
                            Log.e("TAG_TOUCH", tvValue++.toString())
                            try {
                                delay(100)
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }

                        }
                    }
                }

            }
            event.action == MotionEvent.ACTION_UP -> {
                onBtnTouch = false
                if (thisTime - downTime < 500) {
                    tvValue++
                    val msg = handlerBrightnessAdd.obtainMessage()
                    msg.arg1 = tvValue
                    handlerBrightnessAdd.sendMessage(msg)
                }
            }
            event.action == MotionEvent.ACTION_CANCEL -> {
                onBtnTouch = false
            }
        }
    }

    private fun lessWhiteBright(event: MotionEvent?) {
        when {
            event!!.action == MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                onBtnTouch = true
                GlobalScope.launch {
                    while (onBtnTouch) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            val msg = handlerLess.obtainMessage()
                            msg.arg1 = tvValue
                            handlerLess.sendMessage(msg)
                            Log.e("TAG_TOUCH", tvValue++.toString())
                            try {
                                delay(100)
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
            event.action == MotionEvent.ACTION_UP -> {
                onBtnTouch = false
                if (thisTime - downTime < 500) {
                    tvValue++
                    val msg = handlerLess.obtainMessage()
                    msg.arg1 = tvValue
                    handlerLess.sendMessage(msg)
                }
            }
            event.action == MotionEvent.ACTION_CANCEL -> {
                onBtnTouch = false
            }
        }
    }

    private fun addWhiteBright(event: MotionEvent?) {
        when {
            event!!.action == MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                onBtnTouch = true
                GlobalScope.launch {
                    while (onBtnTouch) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            val msg = handler.obtainMessage()
                            msg.arg1 = tvValue
                            handler.sendMessage(msg)
                            Log.e("TAG_TOUCH", tvValue++.toString())
                            try {
                                delay(100)
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
            event.action == MotionEvent.ACTION_UP -> {
                onBtnTouch = false
                if (thisTime - downTime < 500) {
                    tvValue++
                    val msg = handler.obtainMessage()
                    msg.arg1 = tvValue
                    handler.sendMessage(msg)
                }
            }
            event.action == MotionEvent.ACTION_CANCEL -> {
                onBtnTouch = false
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            rgb_white_seekbar.progress++
            when {
                rgb_white_seekbar.progress > 100 -> {
                    sb_w_bright_add.isEnabled = false
                    stopTracking = false
                    onBtnTouch = false
                }
                rgb_white_seekbar.progress == 100 -> {
                    sb_w_bright_add.isEnabled = false
                    sb_w_bright_num.text = rgb_white_seekbar.progress.toString() + "%"
                    stopTracking = false
                    onBtnTouch = false
                }
                else -> {
                    sb_w_bright_add.isEnabled = true
                    stopTracking = true
                }
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val handlerBrightnessAdd = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            rgb_sbBrightness.progress++
            when {
                rgb_sbBrightness.progress > 100 -> {
                    sbBrightness_add.isEnabled = false
                    stopTracking = false
                    onBtnTouch = false
                }
                rgb_sbBrightness.progress == 100 -> {
                    sbBrightness_add.isEnabled = false
                    sbBrightness_num.text = rgb_sbBrightness.progress.toString() + "%"
                    stopTracking = false
                    onBtnTouch = false
                }
                else -> {
                    sbBrightness_add.isEnabled = true
                    stopTracking = true
                }
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val handlerBrightnessLess = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            rgb_sbBrightness.progress--
            when {
                rgb_sbBrightness.progress < 0 -> {
                    sbBrightness_less.isEnabled = false
                    stopTracking = false
                    onBtnTouch = false
                }
                rgb_sbBrightness.progress == 0 -> {
                    sbBrightness_less.isEnabled = false
                    sbBrightness_num.text = rgb_sbBrightness.progress.toString() + "%"
                    stopTracking = false
                    onBtnTouch = false
                }
                else -> {
                    sbBrightness_less.isEnabled = true
                    stopTracking = true
                }
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val handlerLess = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            rgb_white_seekbar.progress--
            when {
                rgb_white_seekbar.progress < 0 -> {
                    sb_w_bright_less.isEnabled = false
                    stopTracking = false
                    onBtnTouch = false
                }
                rgb_white_seekbar.progress == 0 -> {
                    sb_w_bright_less.isEnabled = false
                    sb_w_bright_num.text = rgb_white_seekbar.progress.toString() + "%"
                    stopTracking = false
                    onBtnTouch = false
                }
                else -> {
                    sb_w_bright_less.isEnabled = true
                    stopTracking = true
                }
            }
        }
    }
}
package com.dadoutek.uled.scene

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Message
import android.support.annotation.RequiresApi
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.*
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.blankj.utilcode.util.Utils.runOnUiThread
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.network.RouterTimeoutBean
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.Dot
import com.dadoutek.uled.util.OtherUtils
import com.google.gson.Gson
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONException
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by hejiajun on 2018/5/5.
 */
class SceneGroupAdapter(layoutResId: Int, data: List<ItemGroup>) : BaseQuickAdapter<ItemGroup, BaseViewHolder>(layoutResId, data), SeekBar.OnSeekBarChangeListener {

    private var seekBarprogress: Int = 0
    private  var currentSeekBar: SeekBar?=null
    private var sendNum: Int = 0
    open  var stompRecevice: StompReceiver = StompReceiver()
    private var loadDialog: Dialog? = null
    private var disposableRouteTimer: Disposable? = null
    private var clickType: Int = 0
    private var currentPostion: Int = 0
    private var isBrightness: Boolean = false
    private var algText: TextView? = null
    private var addAlgSpeed: ImageView? = null
    private var lessAlgSpeed: ImageView? = null
    private var speedSeekbar: IndicatorSeekBar? = null
    private var algLy: LinearLayout? = null
    private var topRgLy: RadioGroup? = null
    private var preTime: Long = 0
    private val delayTime = Constants.MAX_SCROLL_DELAY_VALUE
    private var addBrightnessCW: ImageView? = null
    private var lessBrightnessCW: ImageView? = null
    private var lessTemperatureCW: ImageView? = null
    private var addTemperatureCW: ImageView? = null
    private var sbBrightnessCW: SeekBar? = null
    private var sbtemperature: SeekBar? = null

    private lateinit var sbBrightnessRGB: SeekBar
    private lateinit var sbWhiteLightRGB: SeekBar
    private lateinit var addBrightnessRGB: ImageView
    private lateinit var lessBrightnessRGB: ImageView
    private lateinit var addWhiteLightRGB: ImageView
    private lateinit var lessWhiteLightRGB: ImageView
    private var isColorMode: Boolean = true

    internal var downTime: Long = 0//Button被按下时的时间
    internal var thisTime: Long = 0//while每次循环时的时间
    internal var onBtnTouch = false//Button是否被按下
    internal var tvValue = 0//TextView中的值
    @SuppressLint("ClickableViewAccessibility")
    override fun convert(helper: BaseViewHolder, item: ItemGroup) {
        val position = helper.adapterPosition
        sbBrightnessCW = helper.getView(R.id.normal_sbBrightness)
        sbtemperature = helper.getView(R.id.normal_temperature)
        addBrightnessCW = helper.getView(R.id.cw_brightness_add)
        lessBrightnessCW = helper.getView(R.id.cw_brightness_less)
        lessTemperatureCW = helper.getView(R.id.temperature_less)
        addTemperatureCW = helper.getView(R.id.temperature_add)
        speedSeekbar = helper.getView(R.id.speed_seekbar)
        algText = helper.getView(R.id.alg_text)

        var docOne = helper.getView<Dot>(R.id.dot_one)
        var dotRgb = helper.getView<ImageView>(R.id.dot_rgb)

        sbBrightnessRGB = helper.getView(R.id.rgb_sbBrightness)
        addBrightnessRGB = helper.getView(R.id.sbBrightness_add)
        lessBrightnessRGB = helper.getView(R.id.sbBrightness_less)
        sbWhiteLightRGB = helper.getView(R.id.rgb_white_seekbar)
        addWhiteLightRGB = helper.getView(R.id.sb_w_bright_add)
        lessWhiteLightRGB = helper.getView(R.id.sb_w_bright_less)

        addAlgSpeed = helper.getView(R.id.speed_seekbar_alg_add)
        lessAlgSpeed = helper.getView(R.id.speed_seekbar_alg_less)

        topRgLy = helper.getView(R.id.top_rg_ly)
        algLy = helper.getView(R.id.alg_ly)

        val cbTotal = helper.getView<CheckBox>(R.id.cb_total)
        val cbBright = helper.getView<CheckBox>(R.id.cb_bright)
        val cbWhiteLight = helper.getView<CheckBox>(R.id.cb_white_light)

        cbTotal.isChecked = item.isOn
        cbBright.isChecked = item.isEnableBright
        cbWhiteLight.isChecked = item.isEnableWhiteLight

        //0x4FFFE0  不在使用  使用0来判断
        docOne.setChecked(true, if (item.color == 0) {
            docOne.visibility = View.GONE
            dotRgb.visibility = View.VISIBLE
            TelinkLightApplication.getApp().resources.getColor(R.color.primary)
        } else {
            //0xff0000 shl 8 就是左移八位 转换为0xff000000 左移n位，指 按2进制的位 左移n位， （等于 乘 2的n次方），超出最高位的数则丢掉。 比如0xff000000
            //右移n位，指 按2进制的 位 右移n位， （等于 除以 2的n次方），低于最低位的数则丢掉 。
            //位与是指两个二进制数按对应的位上的两个二进制数相乘，口诀是有0出0，11出1，如10 & 01=00。
            docOne.visibility = View.VISIBLE
            dotRgb.visibility = View.GONE
            (0x00ff0000 shl 8) or (item.color and 0xffffff)
        })
        helper.setText(R.id.name_gp, item.gpName)

        when {
            OtherUtils.isRGBGroup(DBUtils.getGroupByMeshAddr(item.groupAddress)) -> {
                val progress = if (sbBrightnessRGB.progress != 0) sbBrightnessRGB.progress else 1
                helper.setProgress(R.id.rgb_sbBrightness, item.brightness)
                        .setProgress(R.id.rgb_white_seekbar, item.temperature)
                        //.setProgress(R.id.speed_seekbar, item.gradientSpeed)
                        .setText(R.id.sbBrightness_num, "$progress%")
                        .setText(R.id.sb_w_bright_num, sbWhiteLightRGB.progress.toString() + "%")
                        .setText(R.id.speed_seekbar_alg_tv, (speedSeekbar!!.progress + 1).toString() + "%")
                speedSeekbar?.setProgress(item.gradientSpeed.toFloat())
                when (item.rgbType) {
                    0 -> {
                        visiableMode(helper, true)
                        setAlgClickAble(item, addBrightnessRGB!!, lessBrightnessRGB!!)

                        when {
                            item.isEnableBright -> {
                                sbBrightnessRGB.isEnabled = true
                                addBrightnessRGB.isEnabled = true
                                lessBrightnessRGB.isEnabled = true
                            }
                            else -> {
                                sbBrightnessRGB.isEnabled = false
                                addBrightnessRGB.isEnabled = false
                                lessBrightnessRGB.isEnabled = false
                            }
                        }
                        when {
                            item.isEnableWhiteLight -> {
                                sbWhiteLightRGB.isEnabled = true
                                addWhiteLightRGB.isEnabled = true
                                lessWhiteLightRGB.isEnabled = true
                            }
                            else -> {
                                sbWhiteLightRGB.isEnabled = false
                                addWhiteLightRGB.isEnabled = false
                                lessWhiteLightRGB.isEnabled = false
                            }
                        }
                    }
                    else -> {//渐变模式
                        if (!TextUtils.isEmpty(item.gradientName))
                            algText?.text = item.gradientName
                        visiableMode(helper, false)
                        setAlgClickAble(item, addAlgSpeed!!, lessAlgSpeed!!, true)
                    }
                }
            }
            else -> {
                helper.setProgress(R.id.normal_sbBrightness, item.brightness).setProgress(R.id.normal_temperature, item.temperature)
                        .setText(R.id.cw_brightness_num, sbBrightnessCW!!.progress.toString() + "%")
                        .setText(R.id.temperature_num, sbtemperature!!.progress.toString() + "%")
                setAlgClickAble(item, addBrightnessCW!!, lessBrightnessCW!!)
                setAlgClickAble(item, addTemperatureCW!!, lessTemperatureCW!!)
            }
        }

        when {
            OtherUtils.isRGBGroup(DBUtils.getGroupByMeshAddr(item.groupAddress)) -> {
                helper.setGone(R.id.cw_scene, false)
                        .setGone(R.id.top_rg_ly, true)
                        .setGone(R.id.switch_scene, false)
                        .setGone(R.id.scene_curtain, false)
                        .setGone(R.id.scene_relay, false)
                when (item.rgbType) {//rgb 类型 0:颜色模式 1：渐变模式
                    0 -> visiableMode(helper, true)
                    1 -> visiableMode(helper, false)
                }
            }
            OtherUtils.isNormalGroup(DBUtils.getGroupByMeshAddr(item.groupAddress)) -> {
                helper.setGone(R.id.oval, true)
                        .setGone(R.id.tv_select_color, false)
                        .setGone(R.id.dot_rgb, false)
                        .setGone(R.id.dot_one, false)
                        .setGone(R.id.rgb_scene, false)
                        .setGone(R.id.top_rg_ly, false)
                        .setGone(R.id.alg_ly, false)
                        .setGone(R.id.cw_scene, false)
                        .setGone(R.id.cw_scene, true)
                        .setGone(R.id.switch_scene, false)
                        .setGone(R.id.scene_curtain, false)
                        .setGone(R.id.scene_relay, false)
                topRgLy?.visibility = View.GONE
            }
            OtherUtils.isConnector(DBUtils.getGroupByMeshAddr(item.groupAddress)) -> {
                helper.setGone(R.id.oval, true)
                        .setGone(R.id.tv_select_color, false)
                        .setGone(R.id.dot_rgb, false)
                        .setGone(R.id.dot_one, false)
                        .setGone(R.id.rgb_scene, false)
                        .setGone(R.id.top_rg_ly, false)
                        .setGone(R.id.alg_ly, false)
                        .setGone(R.id.cw_scene, false)
                        .setGone(R.id.switch_scene, true)
                        .setGone(R.id.scene_curtain, false)
                        .setGone(R.id.scene_relay, true)
                topRgLy?.visibility = View.GONE
                when {
                    item.isOn -> {
                        helper.setChecked(R.id.rg_xx, true)
                        helper.setImageResource(R.id.scene_relay, R.drawable.scene_acceptor_yes)
                    }
                    else -> {
                        helper.setChecked(R.id.rg_yy, true)
                        helper.setImageResource(R.id.scene_relay, R.drawable.scene_acceptor_no)
                    }
                }
            }
            OtherUtils.isCurtain(DBUtils.getGroupByMeshAddr(item.groupAddress)) -> {
                helper.setGone(R.id.oval, true)
                        .setGone(R.id.tv_select_color, false)
                        .setGone(R.id.dot_rgb, false)
                        .setGone(R.id.dot_one, false)
                        .setGone(R.id.rgb_scene, false)
                        .setGone(R.id.top_rg_ly, false)
                        .setGone(R.id.alg_ly, false)
                        .setGone(R.id.cw_scene, false)
                        .setGone(R.id.switch_scene, true)
                        .setGone(R.id.scene_relay, false)
                        .setGone(R.id.scene_curtain, true)
                topRgLy?.visibility = View.GONE
                if (item.isOn) {
                    helper.setChecked(R.id.rg_xx, true)
                    helper.setImageResource(R.id.scene_curtain, R.drawable.scene_curtain_yes)
                } else {
                    helper.setChecked(R.id.rg_yy, true)
                    helper.setImageResource(R.id.scene_curtain, R.drawable.scene_curtain_no)
                }
            }
            else -> {
                helper.setGone(R.id.oval, true)
                        .setGone(R.id.tv_select_color, false)
                        .setGone(R.id.dot_rgb, false)
                        .setGone(R.id.dot_one, false)
                        .setGone(R.id.rgb_scene, false)
                        .setGone(R.id.top_rg_ly, false)
                        .setGone(R.id.alg_ly, false)
                        .setGone(R.id.cw_scene, true)
                        .setGone(R.id.switch_scene, false)
                topRgLy?.visibility = View.GONE
            }
        }

        sbBrightnessCW!!.tag = position
        sbtemperature!!.tag = position
        sbBrightnessRGB!!.tag = position
        sbWhiteLightRGB!!.tag = position
        speedSeekbar!!.tag = position

        sbBrightnessCW!!.setOnSeekBarChangeListener(this)
        sbtemperature!!.setOnSeekBarChangeListener(this)
        sbBrightnessRGB!!.setOnSeekBarChangeListener(this)
        sbWhiteLightRGB!!.setOnSeekBarChangeListener(this)
        speedSeekbar!!.onSeekChangeListener = object : OnSeekChangeListener {
            override fun onSeeking(seekParams: SeekParams?) {
                //解决seekbar 设置min 为1时,滑动不到100%
                var value = seekParams?.progress ?: 0
                helper.setText(R.id.speed_seekbar_alg_tv, "$value%")
                data[position].gradientSpeed = value
                when {
                    value >= 100 -> {
                        lessAlgSpeed?.isEnabled = true
                        addAlgSpeed?.isEnabled = false
                    }
                    value <= 1 -> {
                        lessAlgSpeed?.isEnabled = false
                        addAlgSpeed?.isEnabled = true
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: IndicatorSeekBar?) {}
            override fun onStopTrackingTouch(seekBar: IndicatorSeekBar?) {

            }
        }
        topRgLy!!.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.color_mode_rb -> {
                    isColorMode = true
                    helper.setGone(R.id.alg_ly, false)
                    helper.setGone(R.id.oval, true)
                    helper.setGone(R.id.rgb_scene, true)
                    algLy?.visibility = View.GONE
                    item.rgbType = 0
                    helper.setTextColor(R.id.color_mode_rb, mContext.getColor(R.color.blue_text))
                            .setTextColor(R.id.gradient_mode_rb, mContext.getColor(R.color.gray9))
                }
                R.id.gradient_mode_rb -> {
                    isColorMode = false
                    algLy?.visibility = View.VISIBLE
                    helper.setGone(R.id.alg_ly, true)
                    helper.setGone(R.id.oval, false)
                    helper.setGone(R.id.rgb_scene, false)
                    item.rgbType = 1
                    helper.setTextColor(R.id.color_mode_rb, mContext.getColor(R.color.gray9))
                            .setTextColor(R.id.gradient_mode_rb, mContext.getColor(R.color.blue_text))
                }
            }
            notifyDataSetChanged()
        }
        helper.addOnClickListener(R.id.btn_delete)
                .addOnClickListener(R.id.dot_one)
                .addOnClickListener(R.id.dot_rgb)
                .addOnClickListener(R.id.sbBrightness_add)
                .addOnClickListener(R.id.rg_xx)
                .addOnClickListener(R.id.rg_yy)
                .addOnClickListener(R.id.alg_text)
                .addOnClickListener(R.id.cb_bright)
                .addOnClickListener(R.id.cb_total)
                .addOnClickListener(R.id.cb_white_light)

        addTemperatureCW!!.setOnTouchListener { v, event ->
            currentPostion = position
            clickType = 1//普灯色温
            addTemperature(event, position)
            true
        }
        lessTemperatureCW!!.setOnTouchListener { v, event ->
            currentPostion = position
            clickType = 2//普灯色温
            lessTemperature(event, position)
            true
        }
        addBrightnessCW!!.setOnTouchListener { v, event ->
            currentPostion = position
            clickType = 3//普通亮度
            addBrightness(event, position)//亮度
            true
        }
        lessBrightnessCW!!.setOnTouchListener { v, event ->
            currentPostion = position
            clickType = 4//普通亮度
            lessBrightness(event, position)//亮度
            true
        }
        addBrightnessRGB!!.setOnTouchListener { v, event ->
            currentPostion = position
            clickType = 5//彩灯亮度
            addRGBBrightness(event, position)//亮度
            true
        }
        lessBrightnessRGB!!.setOnTouchListener { v, event ->
            currentPostion = position
            clickType = 6//彩灯亮度
            lessRGBBrightness(event, position)//亮度
            true
        }
        addWhiteLightRGB!!.setOnTouchListener { _, event ->
            currentPostion = position
            clickType = 7//彩灯白光
            addRGBWhiteLight(event, position)
            true
        }
        lessWhiteLightRGB!!.setOnTouchListener { v, event ->
            currentPostion = position
            clickType = 8//普通色温亮度 彩灯亮度白光
            lessRGBWhiteLight(event, position)
            true
        }
        addAlgSpeed?.setOnTouchListener { _, event ->
            currentPostion = position
            addAlgSpeedNum(event, position)
            true
        }
        lessAlgSpeed?.setOnTouchListener { _, event ->
            currentPostion = position
            lessAlgSpeedNum(event, position)
            true
        }
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        val currentTime = System.currentTimeMillis()
        val position = seekBar.tag as Int

         currentSeekBar = seekBar
         seekBarprogress = progress
        currentPostion = position
        if (fromUser&&!Constants.IS_ROUTE_MODE)
            changeTextView(seekBar, progress, position)

        if (currentTime - this.preTime > this.delayTime) {
            if (fromUser) {
                val address = data[position].groupAddress
                val opcode: Byte
                when (seekBar.id) {
                    R.id.normal_sbBrightness -> {
                        clickType = 3
                        opcode = Opcode.SET_LUM
                        if (Constants.IS_ROUTE_MODE) {
                            routerConfigBrightnesssOrColorTemp()
                        } else GlobalScope.launch { sendCmd(opcode, address, progress) }
                    }
                    R.id.normal_temperature -> {
                        clickType = 1
                        if (Constants.IS_ROUTE_MODE) {
                            routerConfigBrightnesssOrColorTemp()
                        } else {
                            opcode = Opcode.SET_TEMPERATURE
                            GlobalScope.launch { sendCmd(opcode, address, progress) }
                        }
                    }
                    R.id.rgb_sbBrightness -> {
                        clickType = 5
                        opcode = Opcode.SET_LUM
                        if (Constants.IS_ROUTE_MODE) {
                            routerConfigBrightnesssOrColorTemp()
                        } else
                            GlobalScope.launch { sendCmd(opcode, address, progress) }
                    }
                    R.id.rgb_white_seekbar -> {
                        clickType = 7
                        opcode = Opcode.SET_W_LUM
                        if (Constants.IS_ROUTE_MODE) {
                            routerConfigBrightnesssOrColorTemp()
                        } else
                            GlobalScope.launch { sendCmd(opcode, address, progress) }
                    }
                }
            }
            this.preTime = currentTime
        }
    }

    private fun visiableMode(helper: BaseViewHolder, isVisiable: Boolean) {
        helper.setGone(R.id.oval, isVisiable)
                .setGone(R.id.rgb_scene, isVisiable)
                .setGone(R.id.alg_ly, !isVisiable)

        when {
            isVisiable -> {
                algLy?.visibility = View.GONE
                helper.setTextColor(R.id.color_mode_rb, mContext.getColor(R.color.blue_text))
                        .setTextColor(R.id.gradient_mode_rb, mContext.getColor(R.color.gray9))
            }
            else -> {
                algLy?.visibility = View.VISIBLE
                helper.setTextColor(R.id.color_mode_rb, mContext.getColor(R.color.gray9))
                        .setTextColor(R.id.gradient_mode_rb, mContext.getColor(R.color.blue_text))
            }
        }
    }

    private fun setAlgClickAble(item: ItemGroup, addBtn: ImageView, lessBtn: ImageView, isGradient: Boolean = false) {
        var zero = if (isGradient) 1 else 0
        when {
            item.brightness <= zero -> {
                addBtn.isEnabled = true
                lessBtn.isEnabled = false
            }
            item.brightness >= 100 -> {
                addBtn.isEnabled = false
                lessBtn.isEnabled = true
            }
            else -> {
                addBtn.isEnabled = true
                lessBtn.isEnabled = true
            }
        }
    }

    private fun lessAlgSpeedNum(event: MotionEvent?, position: Int) {
        when {
            event!!.action == MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                onBtnTouch = true
                GlobalScope.launch {
                    while (onBtnTouch) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            val msg = algHandlerLess.obtainMessage()
                            msg.arg1 = tvValue
                            msg.arg2 = position
                            algHandlerLess.sendMessage(msg)
                            delay(100)
                        }
                    }
                }
            }
            event.action == MotionEvent.ACTION_UP -> {
                onBtnTouch = false
                if (thisTime - downTime < 500) {
                    tvValue++
                    val msg = algHandlerLess.obtainMessage()
                    msg.arg1 = tvValue
                    msg.arg2 = position
                    algHandlerLess.sendMessage(msg)
                }
            }
            event.action == MotionEvent.ACTION_CANCEL -> onBtnTouch = false
        }
    }

    private fun addAlgSpeedNum(event: MotionEvent?, position: Int) {
        when {
            event!!.action == MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                onBtnTouch = true
                GlobalScope.launch {
                    while (onBtnTouch) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            val msg = algHandlerAdd.obtainMessage()
                            msg.arg1 = tvValue
                            msg.arg2 = position
                            algHandlerAdd.sendMessage(msg)
                            delay(100)
                        }
                    }
                }
            }
            event.action == MotionEvent.ACTION_UP -> {
                onBtnTouch = false
                if (thisTime - downTime < 500) {
                    tvValue++
                    val msg = algHandlerAdd.obtainMessage()
                    msg.arg1 = tvValue
                    msg.arg2 = position
                    algHandlerAdd.sendMessage(msg)
                }
            }
            event.action == MotionEvent.ACTION_CANCEL -> onBtnTouch = false
        }
    }

    private fun lessRGBWhiteLight(event: MotionEvent?, position: Int) {
        isBrightness = false
        when {
            event!!.action == MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                onBtnTouch = true
                GlobalScope.launch {
                    while (onBtnTouch&&!Constants.IS_ROUTE_MODE) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            val msg = whitelightLess.obtainMessage()
                            msg.arg1 = tvValue
                            msg.arg2 = position
                            whitelightLess.sendMessage(msg)
                            delay(100)
                        }
                    }
                }
            }
            event.action == MotionEvent.ACTION_UP -> {
                if (Constants.IS_ROUTE_MODE) {
                    routerConfigBrightnesssOrColorTemp()
                } else {
                    onBtnTouch = false
                    if (thisTime - downTime < 500) {
                        tvValue++
                        val msg = whitelightLess.obtainMessage()
                        msg.arg1 = tvValue
                        msg.arg2 = position
                        whitelightLess.sendMessage(msg)
                    }
                }
            }
            event.action == MotionEvent.ACTION_CANCEL -> onBtnTouch = false
        }
    }

    private fun addRGBWhiteLight(event: MotionEvent?, position: Int) {
        isBrightness = false
        when {
            event!!.action == MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                onBtnTouch = true
                GlobalScope.launch {
                    while (onBtnTouch&&!Constants.IS_ROUTE_MODE) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            val msg = whitelightAdds.obtainMessage()
                            msg.arg1 = tvValue
                            msg.arg2 = position
                            whitelightAdds.sendMessage(msg)
                            delay(100)
                        }
                    }
                }
            }
            event.action == MotionEvent.ACTION_UP -> {
                onBtnTouch = false
                if (Constants.IS_ROUTE_MODE) {
                    routerConfigBrightnesssOrColorTemp()
                } else {
                    if (thisTime - downTime < 500) {
                        tvValue++
                        val msg = whitelightAdds.obtainMessage()
                        msg.arg1 = tvValue
                        msg.arg2 = position
                        whitelightAdds.sendMessage(msg)
                    }
                }
            }
            event.action == MotionEvent.ACTION_CANCEL -> onBtnTouch = false
        }
    }

    private fun routerConfigBrightnesssOrColorTemp()/*= when*/ {
      /*  isBrightness -> {//亮度
            val group = data[currentPostion]
            //clickType 普通色温亮度 彩灯亮度白光
            var seekBar = when (clickType) {
                1, 2 -> getViewByPosition(currentPostion, R.id.normal_temperature) as SeekBar
                3, 4 -> getViewByPosition(currentPostion, R.id.normal_sbBrightness) as SeekBar
                6, 5 -> getViewByPosition(currentPostion, R.id.rgb_sbBrightness) as SeekBar
                7, 8 -> getViewByPosition(currentPostion, R.id.rgb_white_seekbar) as SeekBar
                else -> getViewByPosition(currentPostion, R.id.rgb_white_seekbar) as SeekBar
            }
        }
        else -> {*/
            val group = data[currentPostion]
            var seekBar: SeekBar
            when (group) {
                null -> {
                }
                else -> {
                    when (clickType) {//普通色温亮度 彩灯亮度白光
                        1, 2 -> {
                            seekBar = getViewByPosition(currentPostion, R.id.normal_temperature) as SeekBar
                            configTempOrBri(seekBar, group, "gpTem")
                        }
                        3, 4 -> {
                            seekBar = getViewByPosition(currentPostion, R.id.normal_sbBrightness) as SeekBar
                            configTempOrBri(seekBar, group, "gpBri")
                        }
                        5, 6 -> {
                            seekBar = getViewByPosition(currentPostion, R.id.rgb_sbBrightness) as SeekBar
                            configTempOrBri(seekBar, group, "gpRgbBri")
                        }
                        7, 8 -> {
                            seekBar = getViewByPosition(currentPostion, R.id.rgb_white_seekbar) as SeekBar
                            sendNum = seekBar.progress
                            showLoadingDialog(mContext.getString(R.string.please_wait))
                            routeConfigWhiteGpOrLight(group.groupAddress, 97, sendNum, "gpRgbWhite")
                        }
                        else -> {
                        }
                    }
                }
        }
    }

    private fun configTempOrBri(seekBar: SeekBar, group: ItemGroup, serId: String) {
        sendNum = seekBar.progress
        showLoadingDialog(mContext.getString(R.string.please_wait))
        routeConfigTempGpOrLight(group.groupAddress, 97, sendNum, "gpTem")
    }


    @SuppressLint("CheckResult")
    open fun routeConfigTempGpOrLight(meshAddr: Int, deviceType: Int, brightness: Int, serId: String) {
        LogUtils.v("zcl----------- zcl-----------发送路由调色参数-------$brightness-------")
        RouterModel.routeConfigColorTemp(meshAddr, deviceType, brightness, serId)?.subscribe({
            //    "errorCode": 90018"该设备不存在，请重新刷新数据"    "errorCode": 90008,"该设备没有绑定路由，无法操作"
            //    "errorCode": 90007,"该组不存在，请重新刷新数据   "errorCode": 90005"message": "该设备绑定的路由没在线"
            configBriOrColorTempResult(it)
        }) {
            ToastUtils.showShort(it.message)
        }
    }

    @SuppressLint("CheckResult")
    open fun routeConfigBriGpOrLight(meshAddr: Int, deviceType: Int, brightness: Int, serId: String) {
        LogUtils.v("zcl-----------发送路由调光参数-------$brightness")
        RouterModel.routeConfigBrightness(meshAddr, deviceType, brightness, serId)?.subscribe({
            //    "errorCode": 90018"该设备不存在，请重新刷新数据"    "errorCode": 90008,"该设备没有绑定路由，无法操作"
            //    "errorCode": 90007,"该组不存在，请重新刷新数据    "errorCode": 90005"message": "该设备绑定的路由没在线"
            configBriOrColorTempResult(it)
        }) {
            ToastUtils.showShort(it.message)
        }
    }

    @SuppressLint("CheckResult")
    open fun routeConfigWhiteGpOrLight(meshAddr: Int, deviceType: Int, white: Int, serId: String) {
        LogUtils.v("zcl----------- zcl-----------发送路由调白色参数-------$white-------")
        val group = DBUtils.getGroupByID(meshAddr.toLong())
        var gpColor = group?.color ?: 0
        val red = (gpColor and 0xff0000) shr 16
        val green = (gpColor and 0x00ff00) shr 8
        val blue = gpColor and 0x0000ff
        var color = (white shl 24) or (red shl 16) or (green shl 8) or blue
        RouterModel.routeConfigWhiteNum(meshAddr, deviceType, color, serId)?.subscribe({
            //    "errorCode": 90018"该设备不存在，请重新刷新数据"    "errorCode": 90008,"该设备没有绑定路由，无法操作"
            //    "errorCode": 90007,"该组不存在，请重新刷新数据    "errorCode": 90005"message": "该设备绑定的路由没在线"
            configBriOrColorTempResult(it)
        }) {
            ToastUtils.showShort(it.message)
        }
    }

    private fun configBriOrColorTempResult(it: Response<RouterTimeoutBean>) {
        when (it.errorCode) {
            0 -> {
                showLoadingDialog(mContext.getString(R.string.please_wait))
                disposableRouteTimer?.dispose()
                disposableRouteTimer = io.reactivex.Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                        .subscribe {
                            hideLoadingDialog()
                            when (clickType) {//普通色温亮度 彩灯亮度白光
                                1, 2 -> ToastUtils.showShort(mContext.getString(R.string.config_color_temp_fail))
                                3, 4 -> ToastUtils.showShort(mContext.getString(R.string.config_bri_fail))
                                5, 6 -> ToastUtils.showShort(mContext.getString(R.string.config_bri_fail))
                                7, 8 -> ToastUtils.showShort(mContext.getString(R.string.config_white_fail))
                            }
                        }
            }
            90018 -> ToastUtils.showShort(mContext.getString(R.string.device_not_exit))
            90008 -> ToastUtils.showShort(mContext.getString(R.string.no_bind_router_cant_perform))
            90007 -> ToastUtils.showShort(mContext.getString(R.string.gp_not_exit))
            90005 -> ToastUtils.showShort(mContext.getString(R.string.router_offline))
            else-> ToastUtils.showShort(it.message)
        }
    }

    fun tzRouterConfigBriOrTemp(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl------收到路由配置亮度灯通知------------$cmdBean---$clickType")
        val group = data[currentPostion]
        disposableRouteTimer?.dispose()
        when (cmdBean.status) {
            0 -> {
                when (clickType) {//普通色温亮度 彩灯亮度白光
                    1 -> hAddCwTem(currentPostion, data)
                    2 -> hLessCwTem(currentPostion, data)
                    3 -> hAddCwBri(currentPostion, data)
                    4 -> hCwLessBri(currentPostion, data)
                    5 -> hAddRgbBri(currentPostion, data)
                    6 -> hLessRgbBri(currentPostion, data)
                    7 -> hAddWhite(currentPostion, data)
                    8 -> hLesswihte(currentPostion, data)
                }
                if (currentSeekBar!=null)
                changeTextView(currentSeekBar!!, seekBarprogress, currentPostion)
                hideLoadingDialog()
            }
            else -> {
                hideLoadingDialog()
                var seekBar: SeekBar
                when (clickType) {//普通色温亮度 彩灯亮度白光
                    1, 2 -> {
                        seekBar = getViewByPosition(currentPostion, R.id.normal_temperature) as SeekBar
                        seekBar.progress = group.temperature
                        ToastUtils.showShort(mContext.getString(R.string.config_color_temp_fail))
                    }
                    3, 4 -> {
                        seekBar = getViewByPosition(currentPostion, R.id.normal_sbBrightness) as SeekBar
                        seekBar.progress = group.brightness
                        ToastUtils.showShort(mContext.getString(R.string.config_bri_fail))
                    }
                    5, 6 -> {
                        seekBar = getViewByPosition(currentPostion, R.id.rgb_sbBrightness) as SeekBar
                        seekBar.progress = group.brightness
                        ToastUtils.showShort(mContext.getString(R.string.config_bri_fail))
                    }
                    7, 8 -> {
                        seekBar = getViewByPosition(currentPostion, R.id.rgb_white_seekbar) as SeekBar
                        var ws = (group.color and 0xff000000.toInt()) shr 24
                        seekBar.progress = ws
                        ToastUtils.showShort(mContext.getString(R.string.config_white_fail))
                    }
                    else -> {
                    }
                }
            }
        }
    }

    private fun lessRGBBrightness(event: MotionEvent?, position: Int) {
        isBrightness = false
        when {
            event!!.action == MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                onBtnTouch = true
                GlobalScope.launch {
                    while (onBtnTouch&&!Constants.IS_ROUTE_MODE) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            val msg = brightnessLess.obtainMessage()
                            msg.arg1 = tvValue
                            msg.arg2 = position
                            brightnessLess.sendMessage(msg)
                            delay(100)
                        }
                    }
                }
            }
            event.action == MotionEvent.ACTION_UP -> {
                onBtnTouch = false
                if (Constants.IS_ROUTE_MODE) {
                    routerConfigBrightnesssOrColorTemp()
                } else {
                    if (thisTime - downTime < 500) {
                        tvValue++
                        val msg = brightnessLess.obtainMessage()
                        msg.arg1 = tvValue
                        msg.arg2 = position
                        brightnessLess.sendMessage(msg)
                    }
                }
            }
            event.action == MotionEvent.ACTION_CANCEL -> {
                onBtnTouch = false
            }
        }
    }

    private fun addRGBBrightness(event: MotionEvent?, position: Int) {
        currentPostion = position
        isBrightness = false
        when {
            event!!.action == MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                onBtnTouch = true
                GlobalScope.launch {
                    while (onBtnTouch&&!Constants.IS_ROUTE_MODE) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            val msg = brightnessAdd.obtainMessage()
                            msg.arg1 = tvValue
                            msg.arg2 = position
                            brightnessAdd.sendMessage(msg)
                            Log.e("TAG_TOUCH", tvValue++.toString())
                            delay(100)
                        }
                    }
                }
            }
            event.action == MotionEvent.ACTION_UP -> {
                if (Constants.IS_ROUTE_MODE) {
                    routerConfigBrightnesssOrColorTemp()
                } else {
                    onBtnTouch = false
                    if (thisTime - downTime < 500) {
                        tvValue++
                        val msg = brightnessAdd.obtainMessage()
                        msg.arg1 = tvValue
                        msg.arg2 = position
                        brightnessAdd.sendMessage(msg)
                    }
                }
            }
            event.action == MotionEvent.ACTION_CANCEL -> {
                onBtnTouch = false
            }
        }
    }

    private fun lessTemperature(event: MotionEvent?, position: Int) {
        currentPostion = position
        isBrightness = true
        when {
            event!!.action == MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                onBtnTouch = true
                Thread {
                    while (onBtnTouch&&!Constants.IS_ROUTE_MODE) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            val msg = handlerTemperatureLess.obtainMessage()
                            msg.arg1 = tvValue
                            msg.arg2 = position
                            handlerTemperatureLess.sendMessage(msg)
                            Log.e("TAG_TOUCH", tvValue++.toString())
                            try {
                                sleep(100)
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }.start()
            }
            event.action == MotionEvent.ACTION_UP -> {
                onBtnTouch = false
                if (Constants.IS_ROUTE_MODE) {
                    routerConfigBrightnesssOrColorTemp()
                } else {
                    if (thisTime - downTime < 500) {
                        tvValue++
                        val msg = handlerTemperatureLess.obtainMessage()
                        msg.arg1 = tvValue
                        msg.arg2 = position
                        handlerTemperatureLess.sendMessage(msg)
                    }
                }
            }
            event.action == MotionEvent.ACTION_CANCEL -> {
                onBtnTouch = false
            }
        }
    }

    private fun addTemperature(event: MotionEvent?, position: Int) {
        currentPostion = position
        isBrightness = true
        when {
            event!!.action == MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                onBtnTouch = true
                Thread {
                    while (onBtnTouch&&!Constants.IS_ROUTE_MODE) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            val msg = handlerTemperatureAdd.obtainMessage()
                            msg.arg1 = tvValue
                            msg.arg2 = position
                            handlerTemperatureAdd.sendMessage(msg)
                            Log.e("TAG_TOUCH", tvValue++.toString())
                            try {
                                sleep(100)
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }.start()
            }
            event.action == MotionEvent.ACTION_UP -> {
                onBtnTouch = false
                if (Constants.IS_ROUTE_MODE) {
                    routerConfigBrightnesssOrColorTemp()
                } else {
                    if (thisTime - downTime < 500) {
                        tvValue++
                        val msg = handlerTemperatureLess.obtainMessage()
                        msg.arg1 = tvValue
                        msg.arg2 = position
                        handlerTemperatureLess.sendMessage(msg)
                    } else {
                        if (thisTime - downTime < 500) {
                            tvValue++
                            val msg = handlerTemperatureAdd.obtainMessage()
                            msg.arg1 = tvValue
                            msg.arg2 = position
                            handlerTemperatureAdd.sendMessage(msg)
                        }
                    }
                }
            }
            event.action == MotionEvent.ACTION_CANCEL -> {
                onBtnTouch = false
            }
        }
    }

    private fun lessBrightness(event: MotionEvent?, position: Int) {
        currentPostion = position
        isBrightness = false
        when {
            event!!.action == MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                onBtnTouch = true
                Thread {
                    while (onBtnTouch&&!Constants.IS_ROUTE_MODE) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            val msg = handlerBrightnessLess.obtainMessage()
                            msg.arg1 = tvValue
                            msg.arg2 = position
                            handlerBrightnessLess.sendMessage(msg)
                            Log.e("TAG_TOUCH", tvValue++.toString())
                            try {
                                sleep(100)
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }.start()
            }
            event.action == MotionEvent.ACTION_UP -> {
                if (Constants.IS_ROUTE_MODE) {
                    routerConfigBrightnesssOrColorTemp()
                } else {
                    onBtnTouch = false
                    when {
                        thisTime - downTime < 500 -> {
                            tvValue++
                            val msg = handlerBrightnessLess.obtainMessage()
                            msg.arg1 = tvValue
                            msg.arg2 = position
                            handlerBrightnessLess.sendMessage(msg)
                        }
                        else -> {
                            if (thisTime - downTime < 500) {
                                tvValue++
                                val msg = handlerTemperatureAdd.obtainMessage()
                                msg.arg1 = tvValue
                                msg.arg2 = position
                                handlerTemperatureAdd.sendMessage(msg)
                            } else {
                                if (thisTime - downTime < 500) {
                                    tvValue++
                                    val msg = handlerBrightnessLess.obtainMessage()
                                    msg.arg1 = tvValue
                                    msg.arg2 = position
                                    handlerBrightnessLess.sendMessage(msg)
                                }

                            }
                        }
                    }
                }

            }
            event.action == MotionEvent.ACTION_CANCEL -> {
                onBtnTouch = false
            }
        }
    }

    private fun addBrightness(event: MotionEvent?, position: Int) {
        currentPostion = position
        isBrightness = false
        when {
            event!!.action == MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                onBtnTouch = true
                Thread {
                    while (onBtnTouch&&!Constants.IS_ROUTE_MODE) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            val msg = handlerBrightnessAdd.obtainMessage()
                            msg.arg1 = tvValue
                            msg.arg2 = position
                            handlerBrightnessAdd.sendMessage(msg)
                            Log.e("TAG_TOUCH", tvValue++.toString())
                            try {
                                sleep(100)
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }.start()
            }
            event.action == MotionEvent.ACTION_UP -> {
                if (Constants.IS_ROUTE_MODE) {
                    routerConfigBrightnesssOrColorTemp()
                } else {
                    onBtnTouch = false
                    if (thisTime - downTime < 500) {
                        tvValue++
                        val msg = handlerBrightnessAdd.obtainMessage()
                        msg.arg1 = tvValue
                        msg.arg2 = position
                        handlerBrightnessAdd.sendMessage(msg)
                    }
                }
            }
            event.action == MotionEvent.ACTION_CANCEL -> {
                onBtnTouch = false
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val handlerBrightnessAdd = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            hAddCwBri(msg.arg2, data)
        }
    }

    private fun hAddCwBri(msg: Int, data: List<ItemGroup>) {
        var pos = msg
        var seekBar = getViewByPosition(pos, R.id.normal_sbBrightness) as SeekBar?
        var brightnText = getViewByPosition(pos, R.id.cw_brightness_num) as TextView?
        var addImage = getViewByPosition(pos, R.id.cw_brightness_add) as ImageView?
        var lessImage = getViewByPosition(pos, R.id.cw_brightness_less) as ImageView?
        val address = data[pos].groupAddress
        val opcode: Byte
        val itemGroup = data[pos]
        var params: ByteArray
            seekBar!!.progress++

        when {
            seekBar!!.progress > 100 -> {
                addImage!!.isEnabled = false
                onBtnTouch = false
            }
            seekBar!!.progress == 100 -> {
                addImage!!.isEnabled = false
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                onBtnTouch = false
                itemGroup.brightness = seekBar!!.progress
                if (!Constants.IS_ROUTE_MODE) {
                    opcode = Opcode.SET_LUM
                    params = byteArrayOf(seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
            else -> {
                addImage!!.isEnabled = true
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                itemGroup.brightness = seekBar!!.progress
                if (!Constants.IS_ROUTE_MODE) {
                    opcode = Opcode.SET_LUM
                    params = byteArrayOf(seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
        }

        if (seekBar!!.progress > 0) {
            lessImage!!.isEnabled = true
        }
    }

    @SuppressLint("HandlerLeak")
    private val handlerBrightnessLess = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            hCwLessBri(msg.arg2, data)
        }
    }

    private fun hCwLessBri(msg: Int, data: List<ItemGroup>) {
        var pos = msg
        var seekBar = getViewByPosition(pos, R.id.normal_sbBrightness) as SeekBar?
        var brightnText = getViewByPosition(pos, R.id.cw_brightness_num) as TextView?
        var lessImage = getViewByPosition(pos, R.id.cw_brightness_less) as ImageView?
        var addImage = getViewByPosition(pos, R.id.cw_brightness_add) as ImageView?
            seekBar!!.progress--

        val address = data[pos].groupAddress
        val opcode: Byte
        val itemGroup = data[pos]
        var params: ByteArray

        when {
            seekBar!!.progress < 1 -> {
                lessImage!!.isEnabled = false
                onBtnTouch = false
            }
            seekBar!!.progress == 1 -> {
                lessImage!!.isEnabled = false
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                onBtnTouch = false
                itemGroup.brightness = seekBar!!.progress
                if (!Constants.IS_ROUTE_MODE) {
                    opcode = Opcode.SET_LUM
                    params = byteArrayOf(seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
            else -> {
                lessImage!!.isEnabled = true
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                itemGroup.brightness = seekBar!!.progress
                if (!Constants.IS_ROUTE_MODE) {
                    opcode = Opcode.SET_LUM
                    params = byteArrayOf(seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
        }

        if (seekBar!!.progress < 100) {
            addImage!!.isEnabled = true
        }
    }

    @SuppressLint("HandlerLeak")
    private val handlerTemperatureAdd = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            hAddCwTem(msg.arg2, data)
        }
    }

    private fun hAddCwTem(msg: Int, data: List<ItemGroup>) {
        var pos = msg
        var seekBar = getViewByPosition(pos, R.id.normal_temperature) as SeekBar?
        var brightnText = getViewByPosition(pos, R.id.temperature_num) as TextView?
        var addImage = getViewByPosition(pos, R.id.temperature_add) as ImageView?
        var lessImage = getViewByPosition(pos, R.id.temperature_less) as ImageView?

        val address = data[pos].groupAddress
        val opcode: Byte
        val itemGroup = data[pos]
        var params: ByteArray
            seekBar!!.progress++

        when {
            seekBar!!.progress > 100 -> {
                addImage!!.isEnabled = false
                onBtnTouch = false
            }
            seekBar!!.progress == 100 -> {
                addImage!!.isEnabled = false
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                onBtnTouch = false
                itemGroup.temperature = seekBar!!.progress
                if (!Constants.IS_ROUTE_MODE) {
                    opcode = Opcode.SET_TEMPERATURE
                    params = byteArrayOf(0x05, seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
            else -> {
                addImage!!.isEnabled = true
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                itemGroup.temperature = seekBar!!.progress
                if (!Constants.IS_ROUTE_MODE) {
                    opcode = Opcode.SET_TEMPERATURE
                    params = byteArrayOf(0x05, seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
        }

        if (seekBar!!.progress > 0) {
            lessImage!!.isEnabled = true
        }
    }

    @SuppressLint("HandlerLeak")
    private val handlerTemperatureLess = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            hLessCwTem(msg.arg2, data)
        }
    }

    private fun hLessCwTem(msg: Int, data: List<ItemGroup>) {
        var pos = msg

        var seekBar = getViewByPosition(pos, R.id.normal_temperature) as SeekBar?
        var brightnText = getViewByPosition(pos, R.id.temperature_num) as TextView?
        var addImage = getViewByPosition(pos, R.id.temperature_add) as ImageView?
        var lessImage = getViewByPosition(pos, R.id.temperature_less) as ImageView?

        val address = data[pos].groupAddress
        val opcode: Byte
        val itemGroup = data[pos]
        var params: ByteArray
            seekBar!!.progress--

        when {
            seekBar!!.progress < 1 -> {
                lessImage!!.isEnabled = false
                onBtnTouch = false
            }
            seekBar!!.progress == 1 -> {
                lessImage!!.isEnabled = false
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                onBtnTouch = false
                itemGroup.temperature = seekBar!!.progress
                if (!Constants.IS_ROUTE_MODE) {
                    opcode = Opcode.SET_TEMPERATURE
                    params = byteArrayOf(0x05, seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
            else -> {
                lessImage!!.isEnabled = true
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                itemGroup.temperature = seekBar!!.progress
                if (!Constants.IS_ROUTE_MODE) {
                    opcode = Opcode.SET_TEMPERATURE
                    params = byteArrayOf(0x05, seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
        }

        if (seekBar!!.progress > 0) {
            addImage!!.isEnabled = true
        }
    }

    @SuppressLint("HandlerLeak")
    private val brightnessAdd = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            hAddRgbBri(msg.arg2, data)
        }
    }

    private fun hAddRgbBri(msg: Int, data: List<ItemGroup>) {
        var pos = msg

        var seekBar = getViewByPosition(pos, R.id.rgb_sbBrightness) as SeekBar?
        var brightnText = getViewByPosition(pos, R.id.sbBrightness_num) as TextView?
        var addImage = getViewByPosition(pos, R.id.sbBrightness_add) as ImageView?
        var lessImage = getViewByPosition(pos, R.id.sbBrightness_less) as ImageView?

        val address = data[pos].groupAddress
        val opcode: Byte
        val itemGroup = data[pos]
        var params: ByteArray
            seekBar!!.progress++

        when {
            seekBar!!.progress > 100 -> {
                addImage!!.isEnabled = false
                onBtnTouch = false
            }
            seekBar!!.progress == 100 -> {
                addImage!!.isEnabled = false
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                onBtnTouch = false
                itemGroup.brightness = seekBar!!.progress
                if (!Constants.IS_ROUTE_MODE) {
                    opcode = Opcode.SET_LUM
                    params = byteArrayOf(0x05, seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
            else -> {
                addImage!!.isEnabled = true
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                itemGroup.brightness = seekBar!!.progress
                if (!Constants.IS_ROUTE_MODE) {
                    opcode = Opcode.SET_LUM
                    params = byteArrayOf(0x05, seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
        }

        if (seekBar!!.progress > 1) {
            lessImage!!.isEnabled = true
        }
    }

    @SuppressLint("HandlerLeak")
    private val brightnessLess = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            hLessRgbBri(msg.arg2, data)
        }
    }

    private fun hLessRgbBri(msg: Int, data: List<ItemGroup>) {
        var pos = msg
        var seekBar = getViewByPosition(pos, R.id.rgb_sbBrightness) as SeekBar?
        var brightnText = getViewByPosition(pos, R.id.sbBrightness_num) as TextView?
        var lessImage = getViewByPosition(pos, R.id.sbBrightness_less) as ImageView?
        var addImage = getViewByPosition(pos, R.id.sbBrightness_add) as ImageView?
            seekBar!!.progress--

        val address = data[pos].groupAddress
        val opcode: Byte
        val itemGroup = data[pos]
        var params: ByteArray

        when {
            seekBar!!.progress < 1 -> {
                seekBar!!.progress = 1
                lessImage!!.isEnabled = false
                onBtnTouch = false
            }
            seekBar!!.progress == 1 -> {
                lessImage!!.isEnabled = false
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                onBtnTouch = false
                itemGroup.brightness = seekBar!!.progress
                if (!Constants.IS_ROUTE_MODE) {
                    opcode = Opcode.SET_LUM
                    params = byteArrayOf(seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
            else -> {
                lessImage!!.isEnabled = true
                var progress = seekBar!!.progress
                if (progress == 0)
                    progress = 1
                brightnText!!.text = "$progress%"
                itemGroup.brightness = seekBar!!.progress
                if (!Constants.IS_ROUTE_MODE) {
                    opcode = Opcode.SET_LUM
                    params = byteArrayOf(seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
        }

        if (seekBar!!.progress < 100) {
            addImage!!.isEnabled = true
        }
    }

    @SuppressLint("HandlerLeak")
    private val algHandlerAdd = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            hAddSpeed(msg.arg2, data)
        }
    }

    private fun hAddSpeed(msg: Int, data: List<ItemGroup>) {
        var pos = msg
        var seekBar = getViewByPosition(pos, R.id.speed_seekbar) as IndicatorSeekBar?
        var speedTv = getViewByPosition(pos, R.id.speed_seekbar_alg_tv) as TextView?
        var lessImage = getViewByPosition(pos, R.id.speed_seekbar_alg_less) as ImageView?
        var addImage = getViewByPosition(pos, R.id.speed_seekbar_alg_add) as ImageView?
        //seekBar!!.progress++
            seekBar?.progress?.toFloat()?.plus(1f)?.let { speedSeekbar?.setProgress(it) }

        val itemGroup = data[pos]

        when {
            seekBar!!.progress > 5 -> {
                addImage!!.isEnabled = false
                onBtnTouch = false
            }
            seekBar!!.progress == 5 -> {
                addImage!!.isEnabled = false
                speedTv!!.text = seekBar!!.progress.toString() + "%"
                onBtnTouch = false
                itemGroup.gradientSpeed = seekBar!!.progress
            }
            else -> {
                addImage!!.isEnabled = true
                speedTv!!.text = seekBar!!.progress.toString() + "%"
                itemGroup.gradientSpeed = seekBar!!.progress
            }
        }

        if (seekBar!!.progress < 5)
            lessImage!!.isEnabled = true
    }

    @SuppressLint("HandlerLeak")
    private val algHandlerLess = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            var pos = msg.arg2
            var seekBar = getViewByPosition(pos, R.id.speed_seekbar) as IndicatorSeekBar?
            var brightnText = getViewByPosition(pos, R.id.speed_seekbar_alg_tv) as TextView?
            var lessImage = getViewByPosition(pos, R.id.speed_seekbar_alg_less) as ImageView?
            var addImage = getViewByPosition(pos, R.id.speed_seekbar_alg_add) as ImageView?
            //seekBar!!.progress--
            seekBar?.progress?.toFloat()?.minus(1f)?.let { speedSeekbar?.setProgress(it) }

            val address = data[pos].groupAddress
            val opcode: Byte
            val itemGroup = data[pos]
            var params: ByteArray

            when {
                seekBar?.progress!! < 1f -> {
                    lessImage!!.isEnabled = false
                    onBtnTouch = false
                }
                seekBar?.progress == 1 -> {
                    lessImage!!.isEnabled = false
                    brightnText!!.text = seekBar!!.progress.toString() + "%"
                    onBtnTouch = false
                    itemGroup.brightness = seekBar!!.progress
                    opcode = Opcode.SET_LUM
                    params = byteArrayOf(seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
                else -> {
                    lessImage!!.isEnabled = true
                    brightnText!!.text = seekBar!!.progress.toString() + "%"
                    itemGroup.brightness = seekBar!!.progress
                    opcode = Opcode.SET_LUM
                    params = byteArrayOf(seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }

            if (seekBar!!.progress < 100)
                addImage!!.isEnabled = true
        }
    }

    @SuppressLint("HandlerLeak")
    private val whitelightLess = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            hLesswihte(msg.arg2, data)
        }
    }

    private fun hLesswihte(msg: Int, data: List<ItemGroup>) {
        var pos = msg
        var seekBar = getViewByPosition(pos, R.id.rgb_white_seekbar) as SeekBar?
        var brightnText = getViewByPosition(pos, R.id.sb_w_bright_num) as TextView?
        var lessImage = getViewByPosition(pos, R.id.sb_w_bright_less) as ImageView?
        var addImage = getViewByPosition(pos, R.id.sb_w_bright_add) as ImageView?
            seekBar!!.progress--

        val address = data[pos].groupAddress
        val opcode: Byte
        val itemGroup = data[pos]
        var params: ByteArray

        when {
            seekBar!!.progress < 1 -> {
                lessImage!!.isEnabled = false
                onBtnTouch = false
            }
            seekBar!!.progress == 1 -> {
                lessImage!!.isEnabled = false
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                onBtnTouch = false
                itemGroup.temperature = seekBar!!.progress
                if (!Constants.IS_ROUTE_MODE) {
                    opcode = Opcode.SET_W_LUM
                    params = byteArrayOf(seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
            else -> {
                lessImage!!.isEnabled = true
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                itemGroup.temperature = seekBar!!.progress
                if (!Constants.IS_ROUTE_MODE) {
                    opcode = Opcode.SET_W_LUM
                    params = byteArrayOf(seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
        }

        if (seekBar!!.progress < 100) {
            addImage!!.isEnabled = true
        }
    }

    @SuppressLint("HandlerLeak")
    private val whitelightAdds = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            hAddWhite(msg.arg2, data)
        }
    }

    private fun hAddWhite(msg: Int, data: List<ItemGroup>) {
        var pos = msg
        var seekBar = getViewByPosition(pos, R.id.rgb_white_seekbar) as SeekBar?
        var brightnText = getViewByPosition(pos, R.id.sb_w_bright_num) as TextView?
        var lessImage = getViewByPosition(pos, R.id.sb_w_bright_less) as ImageView?
        var addImage = getViewByPosition(pos, R.id.sb_w_bright_add) as ImageView?
            seekBar!!.progress++

        val address = data[pos].groupAddress
        val opcode: Byte
        val itemGroup = data[pos]
        var params: ByteArray

        when {
            seekBar!!.progress > 100 -> {
                addImage!!.isEnabled = false
                onBtnTouch = false
            }
            seekBar!!.progress == 100 -> {
                addImage!!.isEnabled = false
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                onBtnTouch = false
                itemGroup.temperature = seekBar!!.progress
                if (!Constants.IS_ROUTE_MODE) {
                    opcode = Opcode.SET_W_LUM
                    params = byteArrayOf(seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
            else -> {
                addImage!!.isEnabled = true
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                itemGroup.temperature = seekBar!!.progress
                if (!Constants.IS_ROUTE_MODE) {
                    opcode = Opcode.SET_W_LUM
                    params = byteArrayOf(seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
        }

        if (seekBar!!.progress < 100)
            lessImage!!.isEnabled = true
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        this.preTime = System.currentTimeMillis()
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        val pos = seekBar.tag as Int

        val address = if (pos < data.size) data[pos].groupAddress else 0
        val opcode: Byte
        val itemGroup = data[pos]
        when (seekBar.id) {
            R.id.normal_sbBrightness -> {
                (Objects.requireNonNull<View>(getViewByPosition(pos, R.id.cw_brightness_num)) as TextView).text = seekBar.progress.toString() + "%"
                itemGroup.brightness = seekBar.progress
                opcode = Opcode.SET_LUM
                Thread { sendCmd(opcode, address, seekBar.progress) }.start()

                when {
                    seekBar.progress <= 0 -> {
                        addBrightnessCW!!.isEnabled = true
                        lessBrightnessCW!!.isEnabled = false
                    }
                    seekBar.progress >= 100 -> {
                        addBrightnessCW!!.isEnabled = false
                        lessBrightnessCW!!.isEnabled = true
                    }
                    else -> {
                        addBrightnessCW!!.isEnabled = true
                        lessBrightnessCW!!.isEnabled = true
                    }
                }
            }
            R.id.normal_temperature -> {
                (Objects.requireNonNull<View>(getViewByPosition(pos, R.id.temperature_num)) as TextView).text = seekBar.progress.toString() + "%"
                itemGroup.temperature = seekBar.progress
                opcode = Opcode.SET_TEMPERATURE
                Thread { sendCmd(opcode, address, seekBar.progress) }.start()

                when {
                    seekBar.progress <= 0 -> {
                        addTemperatureCW!!.isEnabled = true
                        lessTemperatureCW!!.isEnabled = false
                    }
                    seekBar.progress >= 100 -> {
                        addTemperatureCW!!.isEnabled = false
                        lessTemperatureCW!!.isEnabled = true
                    }
                    else -> {
                        addTemperatureCW!!.isEnabled = true
                        lessTemperatureCW!!.isEnabled = true
                    }
                }

            }
            R.id.rgb_sbBrightness -> {
                var progress = seekBar.progress
                if (progress == 0) progress = 1
                (Objects.requireNonNull<View>(getViewByPosition(pos, R.id.sbBrightness_num)) as TextView).text = "$progress%"
                itemGroup.brightness = seekBar.progress
                opcode = Opcode.SET_LUM
                Thread { sendCmd(opcode, address, seekBar.progress) }.start()

                when {
                    seekBar.progress <= 0 -> {
                        addBrightnessRGB!!.isEnabled = true
                        lessBrightnessRGB!!.isEnabled = false
                    }
                    seekBar.progress >= 100 -> {
                        addBrightnessRGB!!.isEnabled = false
                        lessBrightnessRGB!!.isEnabled = true
                    }
                    else -> {
                        addBrightnessRGB!!.isEnabled = true
                        lessBrightnessRGB!!.isEnabled = true
                    }
                }

            }
            R.id.rgb_white_seekbar -> {
                (Objects.requireNonNull<View>(getViewByPosition(pos, R.id.sb_w_bright_num)) as TextView).text = seekBar.progress.toString() + "%"
                itemGroup.temperature = seekBar.progress
                opcode = Opcode.SET_W_LUM
                Thread { sendCmd(opcode, address, seekBar.progress) }.start()

                when {
                    seekBar.progress <= 0 -> {
                        addWhiteLightRGB!!.isEnabled = true
                        lessWhiteLightRGB!!.isEnabled = false
                    }
                    seekBar.progress >= 100 -> {
                        addWhiteLightRGB!!.isEnabled = false
                        lessWhiteLightRGB!!.isEnabled = true
                    }
                    else -> {
                        addWhiteLightRGB!!.isEnabled = true
                        lessWhiteLightRGB!!.isEnabled = true
                    }
                }
            }
        }
        notifyItemChanged(seekBar.tag as Int)
    }

    @SuppressLint("SetTextI18n")
    fun changeTextView(seekBar: SeekBar, progress: Int, position: Int) {
        when (seekBar.id) {
            R.id.normal_sbBrightness -> {
                val tvBrightness = getViewByPosition(position, R.id.cw_brightness_num) as TextView?
                if (tvBrightness != null) {
                    tvBrightness?.text = "$progress%"
                }
            }
            R.id.normal_temperature -> {
                val tvTemperature = getViewByPosition(position, R.id.temperature_num) as TextView?
                if (tvTemperature != null) {
                    tvTemperature?.text = "$progress%"
                }
            }
            R.id.rgb_sbBrightness -> {
                val tvBrightnessRGB = getViewByPosition(position, R.id.sbBrightness_num) as TextView?
                if (tvBrightnessRGB != null) {
                    if (progress == 0)
                        tvBrightnessRGB.text = "1%"
                    else
                        tvBrightnessRGB.text = "$progress%"

                }
            }
            R.id.rgb_white_seekbar -> {
                val tvSbWhiteLight = getViewByPosition(position, R.id.sb_w_bright_num) as TextView?
                if (tvSbWhiteLight != null) {
                    tvSbWhiteLight.text = "$progress%"
                }
            }
        }
    }

    fun sendCmd(opcode: Byte, address: Int, progress: Int) {
        var progressCmd = progress
        if (progress > Constants.MAX_VALUE)
            progressCmd = Constants.MAX_VALUE

        var params: ByteArray
        params = when (opcode) {
            Opcode.SET_TEMPERATURE -> byteArrayOf(0x05, progressCmd.toByte())
            Opcode.SET_LUM -> byteArrayOf(progressCmd.toByte())
            else -> byteArrayOf(progressCmd.toByte())
        }
        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
    }


    fun showLoadingDialog(content: String) {
        val inflater = LayoutInflater.from(mContext)
        val v = inflater.inflate(R.layout.dialogview, null)

        val layout = v.findViewById<View>(R.id.dialog_view) as LinearLayout
        val tvContent = v.findViewById<View>(R.id.tvContent) as TextView
        tvContent.text = content

        if (loadDialog == null)
            loadDialog = Dialog(mContext, R.style.FullHeightDialog)

        if (loadDialog!!.isShowing)
            return

        //loadDialog没显示才把它显示出来
        if (!mContext.isRestricted && !loadDialog!!.isShowing) {
            loadDialog!!.setCancelable(false)
            loadDialog!!.setCanceledOnTouchOutside(false)
            loadDialog!!.setContentView(layout)
            if (!mContext.isRestricted) {
                loadDialog!!.show()
            }
        }
    }

    fun hideLoadingDialog() {
        if (!mContext.isRestricted && loadDialog != null && loadDialog!!.isShowing) {
            runOnUiThread { loadDialog?.dismiss() }
        }
    }

    inner class StompReceiver : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onReceive(context: Context?, intent: Intent?) {
            val msg = intent?.getStringExtra(Constants.LOGIN_OUT) ?: ""
            val cmdBean: CmdBodyBean = Gson().fromJson(msg, CmdBodyBean::class.java)
            try {
                when (cmdBean.cmd) {
                    Cmd.routeUpdateScenes -> {
                    }
                    Cmd.tzRouteConfigBri -> tzRouterConfigBriOrTemp(cmdBean)
                    Cmd.tzRouteConfigTem -> tzRouterConfigBriOrTemp(cmdBean)
                    Cmd.tzRouteConfigWhite -> tzRouterConfigBriOrTemp(cmdBean)
                }

            } catch (js: JSONException) {
                js.message
            }
        }
    }
}

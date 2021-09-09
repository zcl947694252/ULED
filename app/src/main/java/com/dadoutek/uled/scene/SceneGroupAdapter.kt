package com.dadoutek.uled.scene

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Message
import androidx.annotation.RequiresApi
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.*
import com.app.hubert.guide.util.LogUtil
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.blankj.utilcode.util.Utils.runOnUiThread
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.network.RouterTimeoutBean
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.Dot
import com.dadoutek.uled.util.OtherUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import com.google.gson.Gson
import com.warkiz.widget.IndicatorSeekBar
//import com.warkiz.widget.OnSeekChangeListener
//import com.warkiz.widget.SeekParams
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
//import org.jetbrains.anko.sdk27.coroutines.onSeekBarChangeListener
import org.json.JSONException
import java.lang.NumberFormatException
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by hejiajun on 2018/5/5.
 */
class SceneGroupAdapter(layoutResId: Int, data: List<ItemGroup>) : BaseQuickAdapter<ItemGroup, BaseViewHolder>(layoutResId, data), SeekBar.OnSeekBarChangeListener {

    open var stompRecevice: StompReceiver = StompReceiver()
    private var loadDialog: Dialog? = null
    private var disposableRouteTimer: Disposable? = null
    private var clickType: Int = 0
    private var currentPostion: Int = 0
    private var isBrightness: Boolean = false
    private var algText: TextView? = null
    private var addAlgSpeed: ImageView? = null
    private var lessAlgSpeed: ImageView? = null
    private var speedSeekbar: SeekBar? = null
    private var algLy: LinearLayout? = null
    private var topRgLy: RadioGroup? = null
    private var preTime: Long = 0
    private val delayTime = Constant.MAX_SCROLL_DELAY_VALUE
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

//    窗帘相关tv，seekbar，more，less
    private lateinit var tvCurRange: TextView
    private lateinit var curtainSeekbar : SeekBar
    private lateinit var curtainRangeLess : ImageView
    private lateinit var curtainRangeAdd : ImageView

    internal var downTime: Long = 0//Button被按下时的时间
    private var thisTime: Long = 0//while每次循环时的时间
    internal var onBtnTouch = false//Button是否被按下

    private lateinit var cbTotal: CheckBox
    private lateinit var cbBright: CheckBox
    private lateinit var cbWhiteLight:CheckBox

    private lateinit var cwBrightnessNum: TextView
    private lateinit var temperatureNum: TextView
    private lateinit var sbBrightnessNum: TextView
    private lateinit var sbWBrightNum: TextView

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

        val docOneLy = helper.getView<RelativeLayout>(R.id.dot_one_ly)
        val docOne = helper.getView<Dot>(R.id.dot_one)
        val dotRgb = helper.getView<ImageView>(R.id.dot_rgb)

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

        //窗帘
        tvCurRange = helper.getView(R.id.tv_cur_range)
        curtainSeekbar = helper.getView(R.id.curtain_seekbar)
        curtainRangeLess = helper.getView(R.id.curtain_range_less)
        curtainRangeAdd = helper.getView(R.id.curtain_range_add)

        cwBrightnessNum = helper.getView(R.id.cw_brightness_num)
        temperatureNum = helper.getView(R.id.temperature_num)
        sbBrightnessNum = helper.getView(R.id.sbBrightness_num)
        sbWBrightNum = helper.getView(R.id.sb_w_bright_num)

        cbTotal = helper.getView(R.id.cb_total)
        cbBright = helper.getView(R.id.cb_bright)
        cbWhiteLight = helper.getView(R.id.cb_white_light)

        cbTotal.isChecked = item.isOn

        cbBright.isChecked = item.isEnableBright
        cbWhiteLight.isChecked = item.isEnableWhiteBright

        // 窗帘的初始化
        curtainSeekbar.progress = item.curtainOnOffRange
        tvCurRange.text = "幅度：${curtainSeekbar.progress}%"


        val w = (item.color and 0xff000000.toInt()) shr 24
//        LogUtils.v("chown --=-=-=-=-=--=- item.color $w")
        val r = Color.red(item.color)
        val g = Color.green(item.color)
        val b = Color.blue(item.color)
        //0x4FFFE0  不在使用  使用0来判断
        docOne.setChecked(true, if (r == 0 && g == 0 && b == 0) {
            docOneLy.visibility = View.GONE
            dotRgb.visibility = View.VISIBLE
            TelinkLightApplication.getApp().resources.getColor(R.color.primary)
        } else {
            //0xff0000 shl 8 就是左移八位 转换为0xff000000 左移n位，指 按2进制的位 左移n位 （等于 乘 2的n次方），超出最高位的数则丢掉。 比如0xff000000
            //右移n位，指 按2进制的 位 右移n位， （等于 除以 2的n次方），低于最低位的数则丢掉 。
            //位与是指两个二进制数按对应的位上的两个二进制数相乘，口诀是有0出0，11出1，如10 & 01=00。
            docOneLy.visibility = View.VISIBLE
            dotRgb.visibility = View.GONE
            (0x00ff0000 shl 8) or (item.color and 0xffffff)
        })
        helper.setText(R.id.name_gp, item.gpName)

        when {
            OtherUtils.isRGBGroup(DBUtils.getGroupByMeshAddr(item.groupAddress)) -> {
                val progress = if (item.brightness != 0) item.brightness else 50
                val white = if (w != 0) w else 50

                helper.setProgress(R.id.rgb_sbBrightness, progress) // rgb 亮度
                        .setProgress(R.id.rgb_white_seekbar, white) // 白光
                        .setGone(R.id.speed_seekbar_alg_tv,true) // 速度显示
                        .setProgress(R.id.speed_seekbar, item.gradientSpeed) // 速度条
                        .setChecked(R.id.color_mode_rb, item.rgbType == 0) //颜色模式
                        .setChecked(R.id.gradient_mode_rb, item.rgbType == 1) // 渐变模式
                        .setText(R.id.sbBrightness_num, "$progress%") // 亮度百分值
                        .setText(R.id.sb_w_bright_num, sbWhiteLightRGB.progress.toString() + "%") // 白光百分值
                        .setText(R.id.speed_seekbar_alg_tv, (progress).toString() + "%") // 速度百分值
                speedSeekbar?.progress = item.gradientSpeed.toFloat().toInt() //chown
                helper.setText(R.id.speed_seekbar_alg_tv, (speedSeekbar?.progress).toString() + "%")

                when (item.rgbType) {
                    0 -> {
                        visiableMode(helper, true)
//                        setAlgClickAble(item, addBrightnessRGB, lessBrightnessRGB)
                        if (item.isOn) {
                            cbWhiteLight.isEnabled = true
                            cbBright.isEnabled = true
                            if (item.isEnableBright) { //chown 亮度
                                sbBrightnessRGB.isEnabled = true
                                when {
                                    progress <= 1 -> {
                                        addBrightnessRGB.isEnabled = true
                                        lessBrightnessRGB.isEnabled = false
                                    }
                                    progress >=100 -> {
                                        addBrightnessRGB.isEnabled = false
                                        lessBrightnessRGB.isEnabled = true
                                    }
                                    else -> {
                                        addBrightnessRGB.isEnabled = true
                                        lessBrightnessRGB.isEnabled = true
                                    }
                                }
                            } else {
                                addBrightnessRGB.isEnabled = false
                                lessBrightnessRGB.isEnabled = false
                                sbBrightnessRGB.isEnabled = false
                            }
                            if (item.isEnableWhiteBright) {//chown 白光
                                sbWhiteLightRGB.isEnabled = true
                                when {
                                    white <= 1 -> {
                                        addWhiteLightRGB.isEnabled = true
                                        lessWhiteLightRGB.isEnabled = false
                                    }
                                    white >= 100 -> {
                                        addWhiteLightRGB.isEnabled = false
                                        lessWhiteLightRGB.isEnabled = true
                                    }
                                    else -> {
                                        addWhiteLightRGB.isEnabled = true
                                        lessWhiteLightRGB.isEnabled = true
                                    }
                                }
                            } else {
                                sbWhiteLightRGB.isEnabled = false
                                addWhiteLightRGB.isEnabled = false
                                lessWhiteLightRGB.isEnabled = false
                            }

                        } else {
                            cbWhiteLight.isEnabled = false
                            cbBright.isEnabled = false
                            addBrightnessRGB.isEnabled = false
                            lessBrightnessRGB.isEnabled = false
                            addWhiteLightRGB.isEnabled = false
                            lessWhiteLightRGB.isEnabled = false
                            sbWhiteLightRGB.isEnabled = false
                            sbBrightnessRGB.isEnabled = false
                        }
                    }
                    else -> {//渐变模式
                        if (!TextUtils.isEmpty(item.gradientName))
                            algText?.text = item.gradientName
                        visiableMode(helper, false)
//                        setAlgClickAble(item, addAlgSpeed!!, lessAlgSpeed!!, true)
                    }
                }
            }
            else -> {// 冷暖灯的场景设置就在这里了
//                normalVisiableMode(helper, true)
                val brightness = if(item.brightness!=50) item.brightness else 50
                val temperature = if(item.temperature !=50) item.temperature else 50
                helper.setProgress(R.id.normal_sbBrightness, brightness)
                        .setProgress(R.id.normal_temperature, temperature)
                        .setText(R.id.cw_brightness_num, sbBrightnessCW!!.progress.toString() + "%")
                        .setText(R.id.temperature_num, sbtemperature!!.progress.toString() + "%")
                // chown changed it
            }
        }

        isJBVisable(item, helper, position)

        sbBrightnessCW!!.tag = position
        sbtemperature!!.tag = position
        sbBrightnessRGB.tag = position
        sbWhiteLightRGB.tag = position
        speedSeekbar!!.tag = position
        curtainSeekbar.tag = position

        sbBrightnessCW!!.setOnSeekBarChangeListener(this)
        sbtemperature!!.setOnSeekBarChangeListener(this)
        sbBrightnessRGB.setOnSeekBarChangeListener(this)
        sbWhiteLightRGB.setOnSeekBarChangeListener(this)
        curtainSeekbar.setOnSeekBarChangeListener(this)
//        curtainSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
//            @SuppressLint("SetTextI18n")
//            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
//                LogUtils.v("chown -- onprogress ${curtainSeekbar.progress} %")
//                tvCurRange.text = "幅度：${curtainSeekbar.progress}%"
//                data[position].curtainOnOffRange = progress
//                item.curtainOnOffRange = progress
//                when {
//                    progress >= 100 -> {
//                        curtainSeekbar.progress = 100
//                        curtainRangeLess.isEnabled = true
//                        curtainRangeAdd.isEnabled = false
//                    }
//                    progress <= 1 -> {
//                        curtainSeekbar.progress = 1
//                        curtainRangeLess.isEnabled = false
//                        curtainRangeAdd.isEnabled = true
//                    }
//                    else -> {
//                        curtainRangeLess.isEnabled = true
//                        curtainRangeAdd.isEnabled = true
//                    }
//                }
//            }
//
//            override fun onStartTrackingTouch(seekBar: SeekBar?) {
//
//            }
//
//            override fun onStopTrackingTouch(seekBar: SeekBar?) {
//
//            }
//
//        })
        speedSeekbar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = seekBar?.progress ?: 0
                helper.setGone(R.id.speed_seekbar_alg_tv,true)
                helper.setText(R.id.speed_seekbar_alg_tv, "$value%")
                data[position].gradientSpeed = value
                when {
                    value >= 100 -> {
                        seekBar?.progress = 100
                        lessAlgSpeed?.isEnabled = true
                        addAlgSpeed?.isEnabled = false
                    }
                    value <= 1 -> {
                        seekBar?.progress = 1
                        lessAlgSpeed?.isEnabled = false
                        addAlgSpeed?.isEnabled = true
                    }
                    else -> {
                        lessAlgSpeed?.isEnabled = true
                        addAlgSpeed?.isEnabled = true
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }


        })

        helper.addOnClickListener(R.id.btn_delete)
                .addOnClickListener(R.id.dot_one_ly)
                .addOnClickListener(R.id.dot_rgb)
                .addOnClickListener(R.id.sbBrightness_add)
                .addOnClickListener(R.id.rg_xx)
                .addOnClickListener(R.id.rg_yy)
                .addOnClickListener(R.id.alg_text)
                .addOnClickListener(R.id.cb_bright)
                .addOnClickListener(R.id.cb_total)
                .addOnClickListener(R.id.cb_white_light)
                .addOnClickListener(R.id.color_mode_rb)
                .addOnClickListener(R.id.gradient_mode_rb)

        addTemperatureCW!!.setOnTouchListener { _, event ->
            currentPostion = position
            clickType = 1//普灯色温
            addTemperature(event, position)
            true
        }
        lessTemperatureCW!!.setOnTouchListener { _, event ->
            currentPostion = position
            clickType = 2//普灯色温
            lessTemperature(event, position)
            true
        }
        addBrightnessCW!!.setOnTouchListener { _, event ->
            currentPostion = position
            clickType = 3//普通亮度
            addBrightness(event, position)//亮度
            true
        }
        lessBrightnessCW!!.setOnTouchListener { _, event ->
            currentPostion = position
            clickType = 4//普通亮度
            lessBrightness(event, position)//亮度
            true
        }
        addBrightnessRGB.setOnTouchListener { _, event ->
            currentPostion = position
            clickType = 5//彩灯亮度
            addRGBBrightness(event, position)//亮度
            true
        }
        lessBrightnessRGB.setOnTouchListener { _, event ->
            currentPostion = position
            clickType = 6//彩灯亮度
            lessRGBBrightness(event, position)//亮度
            true
        }
        addWhiteLightRGB.setOnTouchListener { _, event ->
            currentPostion = position
            clickType = 7//彩灯白光
            addRGBWhiteLight(event, position)
            true
        }
        lessWhiteLightRGB.setOnTouchListener { _, event ->
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
        curtainRangeAdd.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val value = ++curtainSeekbar.progress
                data[position].curtainOnOffRange = curtainSeekbar.progress
                item.curtainOnOffRange = curtainSeekbar.progress
                curtainRangeAdd.isEnabled = value < 100
            }
            if(event.action== MotionEvent.ACTION_MOVE) {
                val value = ++curtainSeekbar.progress
                data[position].curtainOnOffRange = curtainSeekbar.progress
                item.curtainOnOffRange = curtainSeekbar.progress
                curtainRangeAdd.isEnabled = value < 100
                GlobalScope.launch {
                    delay(200)
                }
            }
            true
        }
        curtainRangeLess.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP){
                val value = --curtainSeekbar.progress
                data[position].curtainOnOffRange = curtainSeekbar.progress
                item.curtainOnOffRange = curtainSeekbar.progress
                curtainRangeLess.isEnabled = value != 1
            }
            true
        }
    }

    private fun isJBVisable(item: ItemGroup, helper: BaseViewHolder, position: Int) {
        when {
            OtherUtils.isRGBGroup(DBUtils.getGroupByMeshAddr(item.groupAddress)) -> {
//                getViewByPosition(recyclerView,position,R.id.cw_scene)?.visibility = View.GONE
                helper.setGone(R.id.tv_select_color, true)
                        .setGone(R.id.top_rg_ly, true)
                        .setGone(R.id.switch_scene, false)
                        .setGone(R.id.cw_scene, false) //chown
                when (item.rgbType) {//rgb 类型 0:颜色模式 1：渐变模式
                    0 -> visiableMode(helper, true)
                    1 -> visiableMode(helper, false)
                }
            }
            OtherUtils.isNormalGroup(DBUtils.getGroupByMeshAddr(item.groupAddress)) || item.groupAddress == 0xffff -> {
                getViewByPosition(position,R.id.cw_scene)?.visibility = View.VISIBLE
                helper.setGone(R.id.oval, true)
                        .setGone(R.id.tv_select_color, false)
                        .setGone(R.id.dot_rgb, false)
                        .setGone(R.id.dot_one_ly, false)
                        .setGone(R.id.rgb_scene, false)
                        .setGone(R.id.top_rg_ly, false)
                        .setGone(R.id.alg_ly, false)
//                        .setGone(R.id.cw_scene, true)
                        .setGone(R.id.switch_scene, false)
                topRgLy?.visibility = View.GONE
                if (item.isOn) {
                    sbBrightnessCW!!.isEnabled = true
                    sbtemperature!!.isEnabled = true
                    addBrightnessCW!!.isEnabled = sbBrightnessCW!!.progress < 100
                    lessBrightnessCW!!.isEnabled = sbBrightnessCW!!.progress > 1
                    lessTemperatureCW!!.isEnabled = sbtemperature!!.progress > 1
                    addTemperatureCW!!.isEnabled = sbtemperature!!.progress < 100
                } else {
                    sbBrightnessCW!!.isEnabled = false
                    sbtemperature!!.isEnabled = false
                    addBrightnessCW!!.isEnabled = false
                    lessBrightnessCW!!.isEnabled = false
                    lessTemperatureCW!!.isEnabled = false
                    addTemperatureCW!!.isEnabled = false
                }
            }
            OtherUtils.isConnector(DBUtils.getGroupByMeshAddr(item.groupAddress)) -> {
                helper.setGone(R.id.oval, true)
                        .setGone(R.id.tv_select_color, false)
                        .setGone(R.id.dot_rgb, false)
                        .setGone(R.id.dot_one_ly, false)
                        .setGone(R.id.rgb_scene, false)
                        .setGone(R.id.top_rg_ly, false)
                        .setGone(R.id.alg_ly, false)
                        .setGone(R.id.cw_scene, false)
                        .setGone(R.id.switch_scene, true)
                        .setGone(R.id.scene_curtain, false)
                        .setGone(R.id.scene_relay, true)
                        .setGone(R.id.curtain_add_less_layout,false)
                        .setGone(R.id.tv_cur_range,false)
                        .setGone(R.id.curtain_seekbar,false)
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
                        .setGone(R.id.dot_one_ly, false)
                        .setGone(R.id.rgb_scene, false)
                        .setGone(R.id.top_rg_ly, false)
                        .setGone(R.id.alg_ly, false)
                        .setGone(R.id.cw_scene, false)
                        .setGone(R.id.switch_scene, true)
                        .setGone(R.id.scene_relay, false)
                        .setGone(R.id.scene_curtain, true)
                        .setGone(R.id.curtain_add_less_layout,true)
                        .setGone(R.id.tv_cur_range,true)
                        .setGone(R.id.curtain_seekbar,true)
                topRgLy?.visibility = View.GONE
                if (item.isOn) {
                    helper.setChecked(R.id.rg_xx, true)
                    helper.setImageResource(R.id.scene_curtain, R.drawable.scene_curtain_yes)
                    curtainRangeLess.isEnabled = curtainSeekbar.progress > 1
                    curtainRangeAdd.isEnabled = curtainSeekbar.progress < 100
                } else {
                    helper.setChecked(R.id.rg_yy, true)
                    helper.setImageResource(R.id.scene_curtain, R.drawable.scene_curtain_no)
                    curtainRangeLess.isEnabled = curtainSeekbar.progress > 1
                    curtainRangeAdd.isEnabled = curtainSeekbar.progress < 100
                }
            }
            else -> {
                helper.setGone(R.id.oval, true)
                        .setGone(R.id.tv_select_color, false)
                        .setGone(R.id.dot_rgb, false)
                        .setGone(R.id.dot_one_ly, false)
                        .setGone(R.id.rgb_scene, false)
                        .setGone(R.id.top_rg_ly, false)
                        .setGone(R.id.alg_ly, false)
                        .setGone(R.id.cw_scene, true)
                        .setGone(R.id.switch_scene, false)
                topRgLy?.visibility = View.GONE
            }
        }
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) { // chown seekBar
        val currentTime = System.currentTimeMillis()
        val position = seekBar.tag as Int
        LogUtils.v("zcl-------进度条pos-----------$position")
        currentPostion = position
        if (fromUser /*&& !Constant.IS_ROUTE_MODE*/)
            changeTextView(seekBar, progress, position)
        if (currentTime - this.preTime > this.delayTime) {
            if (fromUser) {
                val address = data[position].groupAddress
                val opcode: Byte
                when (seekBar.id) {
                    R.id.normal_sbBrightness -> {
                        clickType = 3
                        opcode = Opcode.SET_LUM
                        if (Constant.IS_ROUTE_MODE) {
                            routerConfigBrightnesssOrColorTemp()
                        } else GlobalScope.launch { sendCmd(opcode, address, progress) }
                    }
                    R.id.normal_temperature -> {
                        clickType = 1
                        if (Constant.IS_ROUTE_MODE) {
                            routerConfigBrightnesssOrColorTemp()
                        } else {
                            opcode = Opcode.SET_TEMPERATURE
                            GlobalScope.launch { sendCmd(opcode, address, progress) }
                        }
                    }
                    R.id.rgb_sbBrightness -> {
                        clickType = 5
                        opcode = Opcode.SET_LUM
                        if (Constant.IS_ROUTE_MODE) {
                            routerConfigBrightnesssOrColorTemp()
                        } else
                            GlobalScope.launch { sendCmd(opcode, address, progress) }
                    }
                    R.id.rgb_white_seekbar -> {
                        clickType = 7
                        opcode = Opcode.SET_W_LUM
                        if (Constant.IS_ROUTE_MODE) {
                            routerConfigBrightnesssOrColorTemp()
                        } else
                            GlobalScope.launch { sendCmd(opcode, address, progress) }
                    }

                }
            }
        }
        this.preTime = currentTime
    }

    private fun visiableMode(helper: BaseViewHolder, isVisible: Boolean) {
        helper.setGone(R.id.oval, isVisible)
                .setGone(R.id.rgb_scene, isVisible)
                .setGone(R.id.alg_ly, !isVisible)
        when {
            isVisible -> {
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
//    private fun normalVisiableMode(helper: BaseViewHolder, isVisible: Boolean) {
//        helper.setGone(R.id.cw_scene,isVisible)
//    }
//
//    private fun setAlgClickAble(item: ItemGroup, addBtn: ImageView, lessBtn: ImageView, isGradient: Boolean = false) {
//        when {
//            item.brightness <= 1 -> {
//                addBtn.isEnabled = true
//                lessBtn.isEnabled = false
//            }
//            item.brightness >= 100 -> {
//                addBtn.isEnabled = false
//                lessBtn.isEnabled = true
//            }
//            else -> {
//                addBtn.isEnabled = true
//                lessBtn.isEnabled = true
//            }
//        }
//    }

    private fun lessAlgSpeedNum(event: MotionEvent?, position: Int) {
        when {
            event!!.action == MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                onBtnTouch = true
                GlobalScope.launch {
                    while (onBtnTouch) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {

                            val msg = algHandlerLess.obtainMessage()

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

                    val msg = algHandlerLess.obtainMessage()

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

                            val msg = algHandlerAdd.obtainMessage()

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

                    val msg = algHandlerAdd.obtainMessage()

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
                    while (onBtnTouch && !Constant.IS_ROUTE_MODE) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            val msg = whitelightLess.obtainMessage()
                            msg.arg2 = position
                            whitelightLess.sendMessage(msg)
                            delay(100)
                        }
                    }
                }
            }
            event.action == MotionEvent.ACTION_UP -> {
                onBtnTouch = false
                if (thisTime - downTime < 500) {
                    val msg = whitelightLess.obtainMessage()
                    msg.arg2 = position
                    whitelightLess.sendMessage(msg)
                }
                if (Constant.IS_ROUTE_MODE)
                    routerConfigBrightnesssOrColorTemp()
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
                    while (onBtnTouch && !Constant.IS_ROUTE_MODE) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            val msg = whitelightAdds.obtainMessage()
                            msg.arg2 = position
                            whitelightAdds.sendMessage(msg)
                            delay(100)
                        }
                    }
                }
            }
            event.action == MotionEvent.ACTION_UP -> {
                onBtnTouch = false
                if (thisTime - downTime < 500) {
                    val msg = whitelightAdds.obtainMessage()
                    msg.arg2 = position
                    whitelightAdds.sendMessage(msg)
                }
                if (Constant.IS_ROUTE_MODE)
                    routerConfigBrightnesssOrColorTemp()
            }
            event.action == MotionEvent.ACTION_CANCEL -> onBtnTouch = false
        }
    }

    private fun routerConfigBrightnesssOrColorTemp() {
        val group = data[currentPostion]
        val seekBar: SeekBar
        when (group) {
            null -> {
            }
            else -> {
                when (clickType) { //普通色温亮度 彩灯亮度白光
                    1, 2 -> {
                        seekBar = getViewByPosition(currentPostion, R.id.normal_temperature) as SeekBar
                        routeConfigTempGpOrLight(group.groupAddress, 97, seekBar.progress, "gpTem")
                    }
                    3, 4 -> {
                        seekBar = getViewByPosition(currentPostion, R.id.normal_sbBrightness) as SeekBar
                        routeConfigBriGpOrLight(group.groupAddress, 97, seekBar.progress, "gpBri")
                    }
                    5, 6 -> {
                        seekBar = getViewByPosition(currentPostion, R.id.rgb_sbBrightness) as SeekBar
                        routeConfigBriGpOrLight(group.groupAddress, 97, seekBar.progress, "gpBri")
                    }
                    7, 8 -> {
                        seekBar = getViewByPosition(currentPostion, R.id.rgb_white_seekbar) as SeekBar
                        routeConfigWhiteGpOrLight(group.groupAddress, 97, seekBar.progress, "gpRgbWhite")
                    }
                    else -> {
                    }
                }
            }
        }
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
        var isEnableBright = if (brightness == 0) 0 else 1
        RouterModel.routeConfigBrightness(meshAddr, deviceType, brightness, isEnableBright, serId)?.subscribe({
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
        val gpColor = group?.color ?: 0
//        val gpColor = data[currentPostion].color //chown
        LogUtils.v("chown ---=-=-==-=-= ${gpColor.toInt()}")
        val red = (gpColor and 0xff0000) shr 16
        val green = (gpColor and 0x00ff00) shr 8
        val blue = gpColor and 0x0000ff
        val color = (white shl 24) or (red shl 16) or (green shl 8) or blue
//        val color = (white shl 24) or (gpColor and 0xffffff)
        LogUtils.v("chown ---=-=-==-=-= $color")

        val isEnableWhiteBright = if (white == 0) 0 else 1
        RouterModel.routeConfigWhiteNum(meshAddr, deviceType, color, isEnableWhiteBright, serId)?.subscribe({
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
                //showLoadingDialog(mContext.getString(R.string.please_wait))
                disposableRouteTimer?.dispose()
                disposableRouteTimer = io.reactivex.Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            tzConfigBriTemFail(data[currentPostion])
                            when (clickType) {//普通色温亮度 彩灯亮度白光
                                1, 2 -> ToastUtils.showShort(mContext.getString(R.string.config_color_temp_fail))
                                3, 4 -> ToastUtils.showShort(mContext.getString(R.string.config_bri_fail))
                                5, 6 -> ToastUtils.showShort(mContext.getString(R.string.config_bri_fail))
                                7, 8 -> ToastUtils.showShort(mContext.getString(R.string.config_white_fail))
                            }
                        }
            }
            90018 -> {
                DBUtils.deleteLocalData()
                //ToastUtils.showShort(mContext.getString(R.string.device_not_exit))
                SyncDataPutOrGetUtils.syncGetDataStart(DBUtils.lastUser!!, object : SyncCallback {
                    override fun start() {}
                    override fun complete() {}
                    override fun error(msg: String?) {}
                })
            }
            90008 -> {
                hideLoadingDialog()
                ToastUtils.showShort(mContext.getString(R.string.no_bind_router_cant_perform))
            }
            90007 -> ToastUtils.showShort(mContext.getString(R.string.gp_not_exit))
            90005 -> ToastUtils.showShort(mContext.getString(R.string.router_offline))
            else -> ToastUtils.showShort(it.message)
        }
    }

    fun tzRouterConfigBriOrTemp(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl------收到路由配置亮度色温白光灯通知------------$cmdBean---$clickType")
        val group = data[currentPostion]
        disposableRouteTimer?.dispose()
        when (cmdBean.status) {
            0 -> {
                /*   when (clickType) {//普通色温亮度 彩灯亮度白光
                       1 -> hAddCwTem(currentPostion, data)
                       2 -> hLessCwTem(currentPostion, data)
                       3 -> hAddCwBri(currentPostion, data)
                       4 -> hCwLessBri(currentPostion, data)
                       5 -> hAddRgbBri(currentPostion, data)
                       6 -> hLessRgbBri(currentPostion, data)
                       7 -> hAddWhite(currentPostion, data)
                       8 -> hLesswihte(currentPostion, data)
                   }*/
                //if (currentSeekBar != null)
                //   changeTextView(currentSeekBar!!, seekBarprogress, currentPostion)
                hideLoadingDialog()
            }
            else -> {
                tzConfigBriTemFail(group)
            }
        }
    }

    private fun tzConfigBriTemFail(group: ItemGroup) {
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
                val ws = (group.color and 0xff000000.toInt()) shr 24
                seekBar.progress = ws
                ToastUtils.showShort(mContext.getString(R.string.config_white_fail))
            }
            else -> {
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
                    while (onBtnTouch && !Constant.IS_ROUTE_MODE) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            val msg = brightnessLess.obtainMessage()
                            msg.arg2 = position
                            brightnessLess.sendMessage(msg)
                            delay(100)
                        }
                    }
                }
            }
            event.action == MotionEvent.ACTION_UP -> {
                onBtnTouch = false
                if (thisTime - downTime < 500) {
                    val msg = brightnessLess.obtainMessage()
                    msg.arg2 = position
                    brightnessLess.sendMessage(msg)
                }
                if (Constant.IS_ROUTE_MODE)
                    routerConfigBrightnesssOrColorTemp()
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
                    while (onBtnTouch && !Constant.IS_ROUTE_MODE) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            val msg = brightnessAdd.obtainMessage()
                            msg.arg2 = position
                            brightnessAdd.sendMessage(msg)
                            delay(100)
                        }
                    }
                }
            }
            event.action == MotionEvent.ACTION_UP -> {
                onBtnTouch = false
                if (thisTime - downTime < 500) {
                    val msg = brightnessAdd.obtainMessage()
                    msg.arg2 = position
                    brightnessAdd.sendMessage(msg)
                }
                if (Constant.IS_ROUTE_MODE)
                    routerConfigBrightnesssOrColorTemp()
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
                    while (onBtnTouch && !Constant.IS_ROUTE_MODE) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            val msg = handlerTemperatureLess.obtainMessage()
                            msg.arg2 = position
                            handlerTemperatureLess.sendMessage(msg)
                            sleep(100)
                        }
                    }
                }.start()
            }
            event.action == MotionEvent.ACTION_UP -> {
                onBtnTouch = false
                if (thisTime - downTime < 500) {
                    val msg = handlerTemperatureLess.obtainMessage()
                    msg.arg2 = position
                    handlerTemperatureLess.sendMessage(msg)
                }
                if (Constant.IS_ROUTE_MODE)
                    routerConfigBrightnesssOrColorTemp()
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
                    while (onBtnTouch && !Constant.IS_ROUTE_MODE) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            val msg = handlerTemperatureAdd.obtainMessage()
                            msg.arg2 = position
                            handlerTemperatureAdd.sendMessage(msg)
                            sleep(100)
                        }
                    }
                }.start()
            }
            event.action == MotionEvent.ACTION_UP -> {
                onBtnTouch = false
                if (thisTime - downTime < 500) {
                    val msg = handlerTemperatureAdd.obtainMessage()
                    msg.arg2 = position
                    handlerTemperatureAdd.sendMessage(msg)
                }
                if (Constant.IS_ROUTE_MODE)
                    routerConfigBrightnesssOrColorTemp()
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
                    while (onBtnTouch && !Constant.IS_ROUTE_MODE) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            val msg = handlerBrightnessLess.obtainMessage()
                            msg.arg2 = position
                            handlerBrightnessLess.sendMessage(msg)
                            sleep(100)
                        }
                    }
                }.start()
            }
            event.action == MotionEvent.ACTION_UP -> {
                onBtnTouch = false
                if (thisTime - downTime < 500) {
                    val msg = handlerBrightnessLess.obtainMessage()
                    msg.arg2 = position
                    handlerBrightnessLess.sendMessage(msg)
                }
                if (Constant.IS_ROUTE_MODE)
                    routerConfigBrightnesssOrColorTemp()

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
                    while (onBtnTouch && !Constant.IS_ROUTE_MODE) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            val msg = handlerBrightnessAdd.obtainMessage()
                            msg.arg2 = position
                            handlerBrightnessAdd.sendMessage(msg)
                            sleep(100)
                        }
                    }
                }.start()
            }
            event.action == MotionEvent.ACTION_UP -> {
                onBtnTouch = false
                if (thisTime - downTime < 500) {
                    val msg = handlerBrightnessAdd.obtainMessage()
                    msg.arg2 = position
                    handlerBrightnessAdd.sendMessage(msg)
                }
                if (Constant.IS_ROUTE_MODE)
                    routerConfigBrightnesssOrColorTemp()
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

    @SuppressLint("SetTextI18n")
    private fun hAddCwBri(msg: Int, data: List<ItemGroup>) {
        val pos = msg
        val seekBar = getViewByPosition(pos, R.id.normal_sbBrightness) as SeekBar?
        val brightnText = getViewByPosition(pos, R.id.cw_brightness_num) as TextView?
        val addImage = getViewByPosition(pos, R.id.cw_brightness_add) as ImageView?
        val lessImage = getViewByPosition(pos, R.id.cw_brightness_less) as ImageView?
        val address = data[pos].groupAddress
        val opcode: Byte
        val itemGroup = data[pos]
        val params: ByteArray
        seekBar!!.progress++

        when {
            seekBar.progress > 100 -> {
                addImage!!.isEnabled = false
                onBtnTouch = false
            }
            seekBar.progress == 100 -> {
                addImage!!.isEnabled = false
                brightnText!!.text = seekBar.progress.toString() + "%"
                onBtnTouch = false
                if (!Constant.IS_ROUTE_MODE) {
                    itemGroup.brightness = seekBar.progress
                    opcode = Opcode.SET_LUM
                    params = byteArrayOf(seekBar.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
            else -> {
                addImage!!.isEnabled = true
                brightnText!!.text = seekBar.progress.toString() + "%"
                if (!Constant.IS_ROUTE_MODE) {
                    itemGroup.brightness = seekBar.progress
                    opcode = Opcode.SET_LUM
                    params = byteArrayOf(seekBar.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
        }

        if (seekBar.progress > 1) {
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

    @SuppressLint("SetTextI18n")
    private fun hCwLessBri(msg: Int, data: List<ItemGroup>) {
        val pos = msg
        val seekBar = getViewByPosition(pos, R.id.normal_sbBrightness) as SeekBar?
        val brightnText = getViewByPosition(pos, R.id.cw_brightness_num) as TextView?
        val lessImage = getViewByPosition(pos, R.id.cw_brightness_less) as ImageView?
        val addImage = getViewByPosition(pos, R.id.cw_brightness_add) as ImageView?
        seekBar!!.progress--

        val address = data[pos].groupAddress
        val opcode: Byte
        val itemGroup = data[pos]
        val params: ByteArray

        when {
            seekBar.progress < 1 -> {
                lessImage!!.isEnabled = false
                onBtnTouch = false
            }
            seekBar.progress == 1 -> {
                lessImage!!.isEnabled = false
                brightnText!!.text = seekBar.progress.toString() + "%"
                onBtnTouch = false
                if (!Constant.IS_ROUTE_MODE) {
                    itemGroup.brightness = seekBar.progress
                    opcode = Opcode.SET_LUM
                    params = byteArrayOf(seekBar.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
            else -> {
                lessImage!!.isEnabled = true
                brightnText!!.text = seekBar.progress.toString() + "%"
                if (!Constant.IS_ROUTE_MODE) {
                    itemGroup.brightness = seekBar.progress
                    opcode = Opcode.SET_LUM
                    params = byteArrayOf(seekBar.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
        }

        if (seekBar.progress < 100) {
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

    @SuppressLint("SetTextI18n")
    private fun hAddCwTem(msg: Int, data: List<ItemGroup>) {
        val pos = msg
        val seekBar = getViewByPosition(pos, R.id.normal_temperature) as SeekBar?
        val brightnText = getViewByPosition(pos, R.id.temperature_num) as TextView?
        val addImage = getViewByPosition(pos, R.id.temperature_add) as ImageView?
        val lessImage = getViewByPosition(pos, R.id.temperature_less) as ImageView?

        val address = data[pos].groupAddress
        val opcode: Byte
        val itemGroup = data[pos]
        val params: ByteArray
        seekBar!!.progress++

        when {
            seekBar.progress > 100 -> {
                addImage!!.isEnabled = false
                onBtnTouch = false
            }
            seekBar.progress == 100 -> {
                addImage!!.isEnabled = false
                brightnText!!.text = seekBar.progress.toString() + "%"
                onBtnTouch = false
                if (!Constant.IS_ROUTE_MODE) {
                    itemGroup.temperature = seekBar.progress
                    opcode = Opcode.SET_TEMPERATURE
                    params = byteArrayOf(0x05, seekBar.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
            else -> {
                addImage!!.isEnabled = true
                brightnText!!.text = seekBar.progress.toString() + "%"
                if (!Constant.IS_ROUTE_MODE) {
                    itemGroup.temperature = seekBar.progress
                    opcode = Opcode.SET_TEMPERATURE
                    params = byteArrayOf(0x05, seekBar.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
        }

        if (seekBar.progress > 1) {
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
        val pos = msg
        val seekBar = getViewByPosition(pos, R.id.normal_temperature) as SeekBar?
        val brightnText = getViewByPosition(pos, R.id.temperature_num) as TextView?
        val addImage = getViewByPosition(pos, R.id.temperature_add) as ImageView?
        val lessImage = getViewByPosition(pos, R.id.temperature_less) as ImageView?

        val address = data[pos].groupAddress
        val opcode: Byte
        val itemGroup = data[pos]
        val params: ByteArray
        seekBar!!.progress--

        when {
            seekBar.progress < 1 -> {
                lessImage!!.isEnabled = false
                onBtnTouch = false
            }
            seekBar.progress == 1 -> {
                lessImage!!.isEnabled = false
                brightnText!!.text = seekBar.progress.toString() + "%"
                onBtnTouch = false
                if (!Constant.IS_ROUTE_MODE) {
                    itemGroup.temperature = seekBar.progress
                    opcode = Opcode.SET_TEMPERATURE
                    params = byteArrayOf(0x05, seekBar.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
            else -> {
                lessImage!!.isEnabled = true
                brightnText!!.text = seekBar.progress.toString() + "%"
                if (!Constant.IS_ROUTE_MODE) {
                    itemGroup.temperature = seekBar.progress
                    opcode = Opcode.SET_TEMPERATURE
                    params = byteArrayOf(0x05, seekBar.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
        }

        if (seekBar.progress > 1) {
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

    @SuppressLint("SetTextI18n")
    private fun hAddRgbBri(msg: Int, data: List<ItemGroup>) {
        val pos = msg
        val seekBar = getViewByPosition(pos, R.id.rgb_sbBrightness) as SeekBar?
        val brightnText = getViewByPosition(pos, R.id.sbBrightness_num) as TextView?
        val addImage = getViewByPosition(pos, R.id.sbBrightness_add) as ImageView?
        val lessImage = getViewByPosition(pos, R.id.sbBrightness_less) as ImageView?

        val address = data[pos].groupAddress
        val opcode: Byte
        val itemGroup = data[pos]
        val params: ByteArray
        seekBar!!.progress++

        when {
            seekBar.progress > 100 -> {
                addImage!!.isEnabled = false
                onBtnTouch = false
            }
            seekBar.progress == 100 -> {
                addImage!!.isEnabled = false
                brightnText!!.text = seekBar.progress.toString() + "%"
                onBtnTouch = false
                if (!Constant.IS_ROUTE_MODE) {
                    itemGroup.brightness = seekBar.progress
                    opcode = Opcode.SET_LUM
                    params = byteArrayOf(0x05, seekBar.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
            else -> {
                addImage!!.isEnabled = true
                brightnText!!.text = seekBar.progress.toString() + "%"
                if (!Constant.IS_ROUTE_MODE) {
                    itemGroup.brightness = seekBar.progress
                    opcode = Opcode.SET_LUM
                    params = byteArrayOf(0x05, seekBar.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
        }

        if (seekBar.progress > 1) {
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

    @SuppressLint("SetTextI18n")
    private fun hLessRgbBri(msg: Int, data: List<ItemGroup>) {
        val pos = msg
        val seekBar = getViewByPosition(pos, R.id.rgb_sbBrightness) as SeekBar?
        val brightnText = getViewByPosition(pos, R.id.sbBrightness_num) as TextView?
        val lessImage = getViewByPosition(pos, R.id.sbBrightness_less) as ImageView?
        val addImage = getViewByPosition(pos, R.id.sbBrightness_add) as ImageView?
        --seekBar!!.progress

        val address = data[pos].groupAddress
        val opcode: Byte
        val itemGroup = data[pos]
        val params: ByteArray

        when {
            seekBar.progress < 1 -> {
                seekBar.progress = 1
                lessImage!!.isEnabled = false
                onBtnTouch = false
            }
            seekBar.progress == 1 -> {
                lessImage!!.isEnabled = false
                brightnText!!.text = seekBar.progress.toString() + "%"
                onBtnTouch = false
                if (!Constant.IS_ROUTE_MODE) {
                    itemGroup.brightness = seekBar.progress
                    opcode = Opcode.SET_LUM
                    params = byteArrayOf(seekBar.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
            else -> {
                lessImage!!.isEnabled = true
                var progress = seekBar.progress
                if (progress == 0)
                    progress = 1
                brightnText!!.text = "$progress%"
                if (!Constant.IS_ROUTE_MODE) {
                    itemGroup.brightness = seekBar.progress
                    opcode = Opcode.SET_LUM
                    params = byteArrayOf(seekBar.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
        }

        if (seekBar.progress < 100) {
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

    @SuppressLint("SetTextI18n")
    private fun hAddSpeed(msg: Int, data: List<ItemGroup>) {
        val pos = msg
        val seekBar = getViewByPosition(pos, R.id.speed_seekbar) as IndicatorSeekBar?
        val speedTv = getViewByPosition(pos, R.id.speed_seekbar_alg_tv) as TextView?
        val lessImage = getViewByPosition(pos, R.id.speed_seekbar_alg_less) as ImageView?
        val addImage = getViewByPosition(pos, R.id.speed_seekbar_alg_add) as ImageView?
        //seekBar!!.progress++
//        seekBar?.progress?.toFloat()?.plus(1f)?.let { speedSeekbar?.setProgress(it) }

        val itemGroup = data[pos]

        when {
            seekBar!!.progress > 5 -> {
                addImage!!.isEnabled = false
                onBtnTouch = false
            }
            seekBar.progress == 5 -> {
                addImage!!.isEnabled = false
                speedTv!!.text = seekBar.progress.toString() + "%"
                onBtnTouch = false
                itemGroup.gradientSpeed = seekBar.progress
            }
            else -> {
                addImage!!.isEnabled = true
                speedTv!!.text = seekBar.progress.toString() + "%"
                itemGroup.gradientSpeed = seekBar.progress
            }
        }

        if (seekBar.progress < 5)
            lessImage!!.isEnabled = true
    }

    @SuppressLint("HandlerLeak")
    private val algHandlerLess = object : Handler() {
        @SuppressLint("SetTextI18n")
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val pos = msg.arg2
            val seekBar = getViewByPosition(pos, R.id.speed_seekbar) as IndicatorSeekBar?
            val brightnText = getViewByPosition(pos, R.id.speed_seekbar_alg_tv) as TextView?
            val lessImage = getViewByPosition(pos, R.id.speed_seekbar_alg_less) as ImageView?
            val addImage = getViewByPosition(pos, R.id.speed_seekbar_alg_add) as ImageView?
            //seekBar!!.progress--
//            seekBar?.progress?.toFloat()?.minus(1f)?.let { speedSeekbar?.setProgress(it) }

            val address = data[pos].groupAddress
            val opcode: Byte
            val itemGroup = data[pos]
            val params: ByteArray

            when {
                seekBar?.progress!! < 1f -> {
                    lessImage!!.isEnabled = false
                    onBtnTouch = false
                }
                seekBar.progress == 1 -> {
                    lessImage!!.isEnabled = false
                    brightnText!!.text = seekBar.progress.toString() + "%"
                    onBtnTouch = false
                    itemGroup.brightness = seekBar.progress
                    opcode = Opcode.SET_LUM
                    params = byteArrayOf(seekBar.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
                else -> {
                    lessImage!!.isEnabled = true
                    brightnText!!.text = seekBar.progress.toString() + "%"
                    itemGroup.brightness = seekBar.progress
                    opcode = Opcode.SET_LUM
                    params = byteArrayOf(seekBar.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }

            if (seekBar.progress < 100)
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
        val pos = msg
        val seekBar = getViewByPosition(pos, R.id.rgb_white_seekbar) as SeekBar?
        val brightnText = getViewByPosition(pos, R.id.sb_w_bright_num) as TextView?
        val lessImage = getViewByPosition(pos, R.id.sb_w_bright_less) as ImageView?
        val addImage = getViewByPosition(pos, R.id.sb_w_bright_add) as ImageView?
        seekBar!!.progress--

        val address = data[pos].groupAddress
        val opcode: Byte
        val itemGroup = data[pos]
        val params: ByteArray

        val red = Color.red(itemGroup.color) //
        val green = Color.green(itemGroup.color) //
        val blue = Color.red(itemGroup.color) //

        when {
            seekBar.progress < 1 -> {
                lessImage!!.isEnabled = false
                onBtnTouch = false
            }
            seekBar.progress == 1 -> {
                lessImage!!.isEnabled = false
                brightnText!!.text = seekBar.progress.toString() + "%"
                onBtnTouch = false
                if (!Constant.IS_ROUTE_MODE) {
//                    itemGroup.temperature = seekBar.progress //chown
                    itemGroup.color = (seekBar.progress shl 24) or (red shl 16) or (green shl 8) or blue
                    opcode = Opcode.SET_W_LUM
                    params = byteArrayOf(seekBar.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
            else -> {
                lessImage!!.isEnabled = true
                brightnText!!.text = seekBar.progress.toString() + "%"
                if (!Constant.IS_ROUTE_MODE) {
//                    itemGroup.temperature = seekBar.progress
                    itemGroup.color = (seekBar.progress shl 24) or (red shl 16) or (green shl 8) or blue
                    opcode = Opcode.SET_W_LUM
                    params = byteArrayOf(seekBar.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
        }

        if (seekBar.progress < 100) {
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

    @SuppressLint("SetTextI18n")
    private fun hAddWhite(msg: Int, data: List<ItemGroup>) {
        val pos = msg
        val seekBar = getViewByPosition(pos, R.id.rgb_white_seekbar) as SeekBar?
        val brightnText = getViewByPosition(pos, R.id.sb_w_bright_num) as TextView?
        val lessImage = getViewByPosition(pos, R.id.sb_w_bright_less) as ImageView?
        val addImage = getViewByPosition(pos, R.id.sb_w_bright_add) as ImageView?
        seekBar!!.progress++

        val address = data[pos].groupAddress
        val opcode: Byte
        val itemGroup = data[pos]
        val params: ByteArray

        val red = Color.red(itemGroup.color)
        val green = Color.green(itemGroup.color)
        val blue = Color.red(itemGroup.color)
        when {
            seekBar.progress > 100 -> {
                addImage!!.isEnabled = false
                onBtnTouch = false
            }
            seekBar.progress == 100 -> {
                addImage!!.isEnabled = false
                brightnText!!.text = seekBar.progress.toString() + "%"
                onBtnTouch = false
                if (!Constant.IS_ROUTE_MODE) {
                    itemGroup.color = (seekBar.progress shl 24) or (red shl 16) or (green shl 8) or blue
                    opcode = Opcode.SET_W_LUM
                    params = byteArrayOf(seekBar.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
            else -> {
                addImage!!.isEnabled = true
                brightnText!!.text = seekBar.progress.toString() + "%"
                if (!Constant.IS_ROUTE_MODE) {
                    itemGroup.color = (seekBar.progress shl 24) or (red shl 16) or (green shl 8) or blue
                    opcode = Opcode.SET_W_LUM
                    params = byteArrayOf(seekBar.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }
        }

        if (seekBar.progress < 100)
            lessImage!!.isEnabled = true
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) { // chwon seekBar
        this.preTime = System.currentTimeMillis()
    }

    @SuppressLint("SetTextI18n")
    override fun onStopTrackingTouch(seekBar: SeekBar) { // chown seekBar
        val pos = seekBar.tag as Int
        val address = if (pos < data.size) data[pos].groupAddress else 0
        var opcode: Byte = Opcode.SET_LUM
        val itemGroup = data[pos]
//        LogUtils.v("chown -- onStop ${address.toString()}")
        when (seekBar.id) {
            R.id.normal_sbBrightness -> {
                (Objects.requireNonNull<View>(getViewByPosition(pos, R.id.cw_brightness_num)) as TextView).text = seekBar.progress.toString() + "%"
                val seek = getViewByPosition(pos, R.id.normal_sbBrightness) as SeekBar?
                val lessImage = getViewByPosition(pos, R.id.cw_brightness_less) as ImageView?
                val addImage = getViewByPosition(pos, R.id.cw_brightness_add) as ImageView?

                itemGroup.brightness = sbBrightnessCW!!.progress
                opcode = Opcode.SET_LUM
                clickType = 3
                when {
                    seek!!.progress <= 1 -> {
                        sbBrightnessCW!!.progress = 1
                        lessImage!!.isEnabled = false
                        addImage!!.isEnabled = true
                    }
                    seek.progress >= 100 -> {
                        sbBrightnessCW!!.progress = 100
                        lessBrightnessCW!!.isEnabled = true
                        addImage!!.isEnabled = false
                    }
                    else -> {
                        lessImage!!.isEnabled = true
                        addImage!!.isEnabled = true
                    }
                }
            }
            R.id.normal_temperature -> {
                (Objects.requireNonNull<View>(getViewByPosition(pos, R.id.temperature_num)) as TextView).text = seekBar.progress.toString() + "%"
                val seek = getViewByPosition(pos, R.id.normal_temperature) as SeekBar?
                val lessImage = getViewByPosition(pos, R.id.temperature_less) as ImageView?
                val addImage = getViewByPosition(pos, R.id.temperature_add) as ImageView?

//                sbtemperature  //替换seek
//                lessTemperatureCW // 替换lessImage
//                addTemperatureCW //替换addImage
                itemGroup.temperature = sbtemperature!!.progress
                opcode = Opcode.SET_TEMPERATURE
                clickType = 1
                //Thread { sendCmd(opcode, address, seekBar.progress) }.start()

                when {
                    seek!!.progress <= 1 -> {
                        sbtemperature!!.progress = 1
                        addImage!!.isEnabled = true
                        lessImage!!.isEnabled = false
                    }
                    seek.progress >= 100 -> {
                        sbtemperature!!.progress = 100
                        addImage!!.isEnabled = false
                        lessImage!!.isEnabled = true
                    }
                    else -> {
                        addImage!!.isEnabled = true
                        lessImage!!.isEnabled = true
                    }
                }
            }
            R.id.rgb_sbBrightness -> {
                val progress = seekBar.progress
                (Objects.requireNonNull<View>(getViewByPosition(pos, R.id.sbBrightness_num)) as TextView).text = "$progress%"
                val seek = getViewByPosition(pos, R.id.rgb_sbBrightness) as SeekBar?
                val lessImage = getViewByPosition(pos, R.id.sbBrightness_less) as ImageView?
                val addImage = getViewByPosition(pos, R.id.sbBrightness_add) as ImageView?
                itemGroup.brightness = seek!!.progress
                opcode = Opcode.SET_LUM
                clickType = 5
                when {
                    seek.progress <= 1 -> {
                        seek.progress = 1
                        addImage?.isEnabled = true
                        lessImage?.isEnabled = false
                    }
                    seek.progress >= 100 -> {
                        seek.progress = 100
                        addImage?.isEnabled = false
                        lessImage?.isEnabled = true
                    }
                    else -> {
                        addImage?.isEnabled = true
                        lessImage?.isEnabled = true
                    }
                }
            }
            R.id.rgb_white_seekbar -> {
                (Objects.requireNonNull<View>(getViewByPosition(pos, R.id.sb_w_bright_num)) as TextView).text = seekBar.progress.toString() + "%"
                val seek = getViewByPosition(pos, R.id.rgb_white_seekbar) as SeekBar?
                val lessImage = getViewByPosition(pos, R.id.sb_w_bright_less) as ImageView?
                val addImage = getViewByPosition(pos, R.id.sb_w_bright_add) as ImageView?
                val red = Color.red(itemGroup.color)
                val green = Color.green(itemGroup.color)
                val blue = Color.red(itemGroup.color)
                itemGroup.color = (seek!!.progress shl 24) or (red shl 16) or (green shl 8) or blue
                opcode = Opcode.SET_W_LUM
                clickType = 7
                when {
                    seek.progress <= 1 -> {
                        seek.progress = 1
                        addImage?.isEnabled = true
                        lessImage?.isEnabled = false
                    }
                    seek.progress >= 100 -> {
                        seek.progress = 100
                        addImage?.isEnabled = false
                        lessImage?.isEnabled = true
                    }
                    else -> {
                        addImage?.isEnabled = true
                        lessImage?.isEnabled = true
                    }
                }
            }
            R.id.curtain_seekbar -> {
                LogUtils.v("chown -- onprogress ${curtainSeekbar.progress} %")
                (Objects.requireNonNull<View>(getViewByPosition(pos, R.id.tv_cur_range)) as TextView).text = "幅度：" + seekBar.progress.toString() + "%"
                val seek = getViewByPosition(pos, R.id.curtain_seekbar) as SeekBar?
                val lessImage = getViewByPosition(pos, R.id.curtain_range_less) as ImageView?
                val addImage = getViewByPosition(pos, R.id.curtain_range_add) as ImageView?
                opcode = Opcode.SET_W_LUM
                data[pos].curtainOnOffRange = seek!!.progress
//                tvCurRange.text = "幅度：${curtainSeekbar.progress}%"

                when {
                    seek.progress >= 100 -> {
                        seek.progress = 100
                        lessImage?.isEnabled = true
                        addImage?.isEnabled = false
                    }
                    seek.progress <= 1 -> {
                        seek.progress = 1
                        lessImage?.isEnabled = false
                        addImage?.isEnabled = true
                    }
                    else -> {
                        lessImage?.isEnabled = true
                        addImage?.isEnabled = true
                    }
                }
            }
        }
        when {
            Constant.IS_ROUTE_MODE -> routerConfigBrightnesssOrColorTemp()
            else -> GlobalScope.launch { sendCmd(opcode, address, seekBar.progress) }
        }
//        notifyItemRangeChanged(seekBar.tag as Int, data.size) //chown
    }

    @SuppressLint("SetTextI18n")
    fun changeTextView(seekBar: SeekBar, progress: Int, position: Int) { //chown
        when (seekBar.id) {
            R.id.normal_sbBrightness -> {
                LogUtils.v("zcl--进度条-----position$position----------")
                val tvBrightness = getViewByPosition(position, R.id.cw_brightness_num) as TextView?
                if (tvBrightness != null) {
                    //  data[position].brightness = progress
                    tvBrightness.text = "$progress%"
                }
            }
            R.id.normal_temperature -> {
                val tvTemperature = getViewByPosition(position, R.id.temperature_num) as TextView?
                if (tvTemperature != null) {
                    tvTemperature.text = "$progress%"
                    // data[position].temperature = progress
                }
            }
            R.id.rgb_sbBrightness -> {
                val tvBrightnessRGB = getViewByPosition(position, R.id.sbBrightness_num) as TextView?
                if (tvBrightnessRGB != null) {
                    if (progress == 0)
                        tvBrightnessRGB.text = "1%"
                    else
                        tvBrightnessRGB.text = "$progress%"
                    data[position].brightness = progress
                }
            }
            R.id.rgb_white_seekbar -> {
                val tvSbWhiteLight = getViewByPosition(position, R.id.sb_w_bright_num) as TextView?
                if (tvSbWhiteLight != null) {
                    tvSbWhiteLight.text = "$progress%"
                    val itemGroup = data[position]
                    val red = Color.red(itemGroup.color)
                    val green = Color.green(itemGroup.color)
                    val blue = Color.red(itemGroup.color)
                    itemGroup.color = (progress shl 24) or (red shl 16) or (green shl 8) or blue
                }
            }
            R.id.curtain_seekbar -> {
                val tvCurtain = getViewByPosition(position, R.id.tv_cur_range) as TextView?
                if (tvCurtain!=null) {
                    tvCurtain.text = "幅度：$progress%"
                }
            }
        }
    }

    fun sendCmd(opcode: Byte, address: Int, progress: Int) {
        var progressCmd = progress
        if (progress > Constant.MAX_VALUE)
            progressCmd = Constant.MAX_VALUE

        val params: ByteArray = when (opcode) {
            Opcode.SET_TEMPERATURE -> {
                data[currentPostion].temperature = progressCmd
                byteArrayOf(0x05, progressCmd.toByte())
            }
            Opcode.SET_LUM -> {
                data[currentPostion].brightness = progressCmd
                byteArrayOf(progressCmd.toByte())
            }
            Opcode.SET_W_LUM -> {
                data[currentPostion].color = (progressCmd shl 24) or (Color.red(data[currentPostion].color) shl 16) or
                        (Color.green(data[currentPostion].color) shl 8) or Color.blue(data[currentPostion].color)
                byteArrayOf(progressCmd.toByte())
            }
            else -> {
                data[currentPostion].brightness = progressCmd
                byteArrayOf(progressCmd.toByte())
            }
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
            val msg = intent?.getStringExtra(Constant.LOGIN_OUT) ?: ""
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

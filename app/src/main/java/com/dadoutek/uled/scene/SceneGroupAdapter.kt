package com.dadoutek.uled.scene

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.*
import com.blankj.utilcode.util.LogUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.ItemGroup
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.Dot
import com.dadoutek.uled.util.OtherUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

/**
 * Created by hejiajun on 2018/5/5.
 */
class SceneGroupAdapter(layoutResId: Int, data: List<ItemGroup>) : BaseQuickAdapter<ItemGroup, BaseViewHolder>(layoutResId, data), SeekBar.OnSeekBarChangeListener {

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
    private var isColorMode: Boolean = true

    internal var downTime: Long = 0//Button被按下时的时间
    internal var thisTime: Long = 0//while每次循环时的时间
    internal var onBtnTouch = false//Button是否被按下
    internal var tvValue = 0//TextView中的值

    @SuppressLint("ClickableViewAccessibility")
    override fun convert(helper: BaseViewHolder, item: ItemGroup) {
        val position = helper.layoutPosition
        sbBrightnessCW = helper.getView(R.id.cw_sbBrightness)
        sbtemperature = helper.getView(R.id.temperature)
        addBrightnessCW = helper.getView(R.id.cw_brightness_add)
        lessBrightnessCW = helper.getView(R.id.cw_brightness_less)
        lessTemperatureCW = helper.getView(R.id.temperature_less)
        addTemperatureCW = helper.getView(R.id.temperature_add)
        speedSeekbar = helper.getView(R.id.speed_seekbar)
        algText = helper.getView(R.id.alg_text)

        var docOne = helper.getView<Dot>(R.id.dot_one)
        var dotRgb = helper.getView<ImageView>(R.id.dot_rgb)

        sbBrightnessRGB = helper.getView(R.id.sbBrightness)
        addBrightnessRGB = helper.getView(R.id.sbBrightness_add)
        lessBrightnessRGB = helper.getView(R.id.sbBrightness_less)
        sbWhiteLightRGB = helper.getView(R.id.sb_w_bright)
        addWhiteLightRGB = helper.getView(R.id.sb_w_bright_add)
        lessWhiteLightRGB = helper.getView(R.id.sb_w_bright_less)

        addAlgSpeed = helper.getView(R.id.speed_seekbar_alg_add)
        lessAlgSpeed = helper.getView(R.id.speed_seekbar_alg_less)

        topRgLy = helper.getView(R.id.top_rg_ly)
        algLy = helper.getView(R.id.alg_ly)

        val cb_total = helper.getView<CheckBox>(R.id.cb_total)
        val cb_bright = helper.getView<CheckBox>(R.id.cb_bright)
        val cb_white_light = helper.getView<CheckBox>(R.id.cb_white_light)

        cb_total.isChecked = item.isOn
        cb_bright.isChecked = item.isEnableBright
        cb_white_light.isChecked = item.isEnableWhiteLight


        //0x4FFFE0
        docOne.setChecked(true, if (item.color == 16777215) {
            docOne.visibility = View.GONE
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
                helper.setProgress(R.id.sbBrightness, item.brightness)
                        .setProgress(R.id.sb_w_bright, item.temperature)
                        .setProgress(R.id.speed_seekbar, item.gradientSpeed)
                        .setText(R.id.sbBrightness_num, sbBrightnessRGB!!.progress.toString() + "%")
                        .setText(R.id.sb_w_bright_num, sbWhiteLightRGB!!.progress.toString() + "%")
                        .setText(R.id.speed_seekbar_alg_tv, speedSeekbar!!.progress.toString() + "%")
                when (item.rgbType) {
                    0 -> {
                        visiableMode(helper, true)
                        setAlgClickAble(item, addBrightnessRGB!!, lessBrightnessRGB!!)

                        //LogUtils.e(item.toString())

                        if (item.isEnableBright) {
                            sbBrightnessRGB.isEnabled = true
                            addBrightnessRGB.isEnabled = true
                            lessBrightnessRGB.isEnabled = true
                        } else {
                            sbBrightnessRGB.isEnabled = false
                            addBrightnessRGB.isEnabled = false
                            lessBrightnessRGB.isEnabled = false
                        }

                        if (item.isEnableWhiteLight) {
                            sbWhiteLightRGB.isEnabled = true
                            addWhiteLightRGB.isEnabled = true
                            lessWhiteLightRGB.isEnabled = true
                        } else {
                            sbWhiteLightRGB.isEnabled = false
                            addWhiteLightRGB.isEnabled = false
                            lessWhiteLightRGB.isEnabled = false
                        }

                        /* when {
                             item.brightness <= 0 -> {
                                 addBrightnessRGB!!.isEnabled = true
                                 lessBrightnessRGB!!.isEnabled = false
                             }
                             item.brightness >= 100 -> {
                                 addBrightnessRGB!!.isEnabled = false
                                 lessBrightnessRGB!!.isEnabled = true
                             }
                             else -> {
                                 addBrightnessRGB!!.isEnabled = true
                                 lessBrightnessRGB!!.isEnabled = true
                             }
                         }
                         when {
                             item.temperature <= 0 -> {
                                 addWhiteLightRGB!!.isEnabled = true
                                 lessWhiteLightRGB!!.isEnabled = false
                             }
                             item.temperature >= 100 -> {
                                 addWhiteLightRGB!!.isEnabled = false
                                 lessWhiteLightRGB!!.isEnabled = true
                             }
                             else -> {
                                 addWhiteLightRGB!!.isEnabled = true
                                 lessWhiteLightRGB!!.isEnabled = true
                             }
                         }*/
                    }
                    else -> {//渐变模式
                        if (!TextUtils.isEmpty(item.gradientName))
                            algText?.text = item.gradientName
                        visiableMode(helper, false)
                        setAlgClickAble(item, addAlgSpeed!!, lessAlgSpeed!!, true)
                        /*   when {
                               item.gradientSpeed <= 0 -> {
                                   addAlgSpeed!!.isEnabled = true
                                   lessAlgSpeed!!.isEnabled = false
                               }
                               item.gradientSpeed >= 100 -> {
                                   addAlgSpeed!!.isEnabled = false
                                   lessAlgSpeed!!.isEnabled = true
                               }
                               else -> {
                                   addAlgSpeed!!.isEnabled = true
                                   lessAlgSpeed!!.isEnabled = true
                               }
                           }*/
                    }
                }
            }
            else -> {
                helper.setProgress(R.id.cw_sbBrightness, item.brightness)
                        .setProgress(R.id.temperature, item.temperature)
                        .setText(R.id.cw_brightness_num, sbBrightnessCW!!.progress.toString() + "%")
                        .setText(R.id.temperature_num, sbtemperature!!.progress.toString() + "%")
                setAlgClickAble(item, addBrightnessCW!!, lessBrightnessCW!!)
                setAlgClickAble(item, addTemperatureCW!!, lessTemperatureCW!!)
                /*  when {
                      item.brightness <= 0 -> {
                          addBrightnessCW!!.isEnabled = true
                          lessBrightnessCW!!.isEnabled = false
                      }
                      item.brightness >= 100 -> {
                          addBrightnessCW!!.isEnabled = false
                          lessBrightnessCW!!.isEnabled = true
                      }
                      else -> {
                          addBrightnessCW!!.isEnabled = true
                          lessBrightnessCW!!.isEnabled = true
                      }
                  }
                  when {
                      item.temperature <= 0 -> {
                          addTemperatureCW!!.isEnabled = true
                          lessTemperatureCW!!.isEnabled = false
                      }
                      item.temperature >= 100 -> {
                          addTemperatureCW!!.isEnabled = false
                          lessTemperatureCW!!.isEnabled = true
                      }
                      else -> {
                          addTemperatureCW!!.isEnabled = true
                          lessTemperatureCW!!.isEnabled = true
                      }
                  }*/
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
//                        .setGone(R.id.tv_select_color, false)
//                        .setGone(R.id.dot_rgb, false)
                        .setGone(R.id.rgb_scene, false)
                        .setGone(R.id.top_rg_ly, false)
                        .setGone(R.id.alg_ly, false)
                        .setGone(R.id.cw_scene, true)
                        .setGone(R.id.switch_scene, false)
                        .setGone(R.id.scene_curtain, false)
                        .setGone(R.id.scene_relay, false)
            }
            OtherUtils.isConnector(DBUtils.getGroupByMeshAddr(item.groupAddress)) -> {
                helper.setGone(R.id.oval, true)
                        .setGone(R.id.tv_select_color, false)
                        .setGone(R.id.dot_rgb, false)
                        .setGone(R.id.rgb_scene, false)
                        .setGone(R.id.top_rg_ly, false)
                        .setGone(R.id.alg_ly, false)
                        .setGone(R.id.cw_scene, false)
                        .setGone(R.id.switch_scene, true)
                        .setGone(R.id.scene_curtain, false)
                        .setGone(R.id.scene_relay, true)
                if (item.isOn) {
                    helper.setChecked(R.id.rg_xx, true)
                    helper.setImageResource(R.id.scene_relay, R.drawable.scene_acceptor_yes)
                } else {
                    helper.setChecked(R.id.rg_yy, true)
                    helper.setImageResource(R.id.scene_relay, R.drawable.scene_acceptor_no)
                }
            }
            OtherUtils.isCurtain(DBUtils.getGroupByMeshAddr(item.groupAddress)) -> {
                helper.setGone(R.id.oval, true)
                        .setGone(R.id.tv_select_color, false)
                        .setGone(R.id.dot_rgb, false)
                        .setGone(R.id.rgb_scene, false)
                        .setGone(R.id.top_rg_ly, false)
                        .setGone(R.id.alg_ly, false)
                        .setGone(R.id.cw_scene, false)
                        .setGone(R.id.switch_scene, true)
                        .setGone(R.id.scene_relay, false)
                        .setGone(R.id.scene_curtain, true)
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
                        .setGone(R.id.rgb_scene, false)
                        .setGone(R.id.top_rg_ly, false)
                        .setGone(R.id.alg_ly, false)
                        .setGone(R.id.cw_scene, true)
                        .setGone(R.id.switch_scene, false)
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
        speedSeekbar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                helper.setText(R.id.speed_seekbar_alg_tv, "$progress%")
                data[position].gradientSpeed = progress
                if (progress >= 100) {
                    lessAlgSpeed?.isEnabled = true
                    addAlgSpeed?.isEnabled = false
                } else if (progress <= 1) {
                    lessAlgSpeed?.isEnabled = false
                    addAlgSpeed?.isEnabled = true
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

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
            addTemperature(event, position)
            true
        }
        lessTemperatureCW!!.setOnTouchListener { v, event ->
            lessTemperature(event, position)
            true
        }
        addBrightnessCW!!.setOnTouchListener { v, event ->
            addBrightness(event, position)
            true
        }
        lessBrightnessCW!!.setOnTouchListener { v, event ->
            lessBrightness(event, position)
            true
        }
        addBrightnessRGB!!.setOnTouchListener { v, event ->
            addRGBBrightness(event, position)
            true
        }
        lessBrightnessRGB!!.setOnTouchListener { v, event ->
            lessRGBBrightness(event, position)
            true
        }
        addWhiteLightRGB!!.setOnTouchListener { _, event ->
            addRGBWhiteLight(event, position)
            true
        }
        lessWhiteLightRGB!!.setOnTouchListener { v, event ->
            lessRGBWhiteLight(event, position)
            true
        }
        addAlgSpeed?.setOnTouchListener { _, event ->
            addAlgSpeedNum(event, position)
            true
        }
        lessAlgSpeed?.setOnTouchListener { _, event ->
            lessAlgSpeedNum(event, position)
            true
        }
    }

    private fun visiableMode(helper: BaseViewHolder, isVisiable: Boolean) {
        helper.setGone(R.id.oval, isVisiable)
                .setGone(R.id.rgb_scene, isVisiable)
                .setGone(R.id.alg_ly, !isVisiable)

        if (isVisiable) {
            algLy?.visibility = View.GONE
            helper.setTextColor(R.id.color_mode_rb, mContext.getColor(R.color.blue_text))
                    .setTextColor(R.id.gradient_mode_rb, mContext.getColor(R.color.gray9))
        } else {
            algLy?.visibility = View.VISIBLE
            helper.setTextColor(R.id.color_mode_rb, mContext.getColor(R.color.gray9))
                    .setTextColor(R.id.gradient_mode_rb, mContext.getColor(R.color.blue_text))
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

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        val currentTime = System.currentTimeMillis()
        val position = seekBar.tag as Int
        if (fromUser)
            changeTextView(seekBar, progress, position)

        if (currentTime - this.preTime > this.delayTime) {
            if (fromUser) {
                val address = data[position].groupAddress
                val opcode: Byte
                when (seekBar.id) {
                    R.id.cw_sbBrightness -> {
                        opcode = Opcode.SET_LUM
                        GlobalScope.launch { sendCmd(opcode, address, progress) }
                    }
                    R.id.temperature -> {
                        opcode = Opcode.SET_TEMPERATURE
                        GlobalScope.launch { sendCmd(opcode, address, progress) }
                    }
                    R.id.sbBrightness -> {
                        opcode = Opcode.SET_LUM
                        GlobalScope.launch { sendCmd(opcode, address, progress) }
                    }
                    R.id.sb_w_bright -> {
                        opcode = Opcode.SET_W_LUM
                        GlobalScope.launch { sendCmd(opcode, address, progress) }
                    }
                }
            }
            this.preTime = currentTime
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
        when {
            event!!.action == MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                onBtnTouch = true
                GlobalScope.launch {
                    while (onBtnTouch) {
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
                onBtnTouch = false
                if (thisTime - downTime < 500) {
                    tvValue++
                    val msg = whitelightLess.obtainMessage()
                    msg.arg1 = tvValue
                    msg.arg2 = position
                    whitelightLess.sendMessage(msg)
                }
            }
            event.action == MotionEvent.ACTION_CANCEL -> onBtnTouch = false
        }
    }

    private fun addRGBWhiteLight(event: MotionEvent?, position: Int) {
        when {
            event!!.action == MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                onBtnTouch = true
                GlobalScope.launch {
                    while (onBtnTouch) {
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
                if (thisTime - downTime < 500) {
                    tvValue++
                    val msg = whitelightAdds.obtainMessage()
                    msg.arg1 = tvValue
                    msg.arg2 = position
                    whitelightAdds.sendMessage(msg)
                }
            }
            event.action == MotionEvent.ACTION_CANCEL -> onBtnTouch = false
        }
    }

    private fun lessRGBBrightness(event: MotionEvent?, position: Int) {
        if (event!!.action == MotionEvent.ACTION_DOWN) {
            downTime = System.currentTimeMillis()
            onBtnTouch = true
            GlobalScope.launch {
                while (onBtnTouch) {
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
        } else if (event.action == MotionEvent.ACTION_UP) {
            onBtnTouch = false
            if (thisTime - downTime < 500) {
                tvValue++
                val msg = brightnessLess.obtainMessage()
                msg.arg1 = tvValue
                msg.arg2 = position
                brightnessLess.sendMessage(msg)
            }
        } else if (event.action == MotionEvent.ACTION_CANCEL) {
            onBtnTouch = false
        }
    }

    private fun addRGBBrightness(event: MotionEvent?, position: Int) {
        if (event!!.action == MotionEvent.ACTION_DOWN) {
            downTime = System.currentTimeMillis()
            onBtnTouch = true
            GlobalScope.launch {
                while (onBtnTouch) {
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
        } else if (event.action == MotionEvent.ACTION_UP) {
            onBtnTouch = false
            if (thisTime - downTime < 500) {
                tvValue++
                val msg = brightnessAdd.obtainMessage()
                msg.arg1 = tvValue
                msg.arg2 = position
                brightnessAdd.sendMessage(msg)
            }
        } else if (event.action == MotionEvent.ACTION_CANCEL) {
            onBtnTouch = false
        }
    }

    private fun lessTemperature(event: MotionEvent?, position: Int) {
        if (event!!.action == MotionEvent.ACTION_DOWN) {
            downTime = System.currentTimeMillis()
            onBtnTouch = true
            val t = object : Thread() {
                override fun run() {
                    while (onBtnTouch) {
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
                }
            }
            t.start()
        } else if (event.action == MotionEvent.ACTION_UP) {
            onBtnTouch = false
            if (thisTime - downTime < 500) {
                tvValue++
                val msg = handlerTemperatureLess.obtainMessage()
                msg.arg1 = tvValue
                msg.arg2 = position
                handlerTemperatureLess.sendMessage(msg)
            }
        } else if (event.action == MotionEvent.ACTION_CANCEL) {
            onBtnTouch = false
        }
    }

    private fun addTemperature(event: MotionEvent?, position: Int) {
        if (event!!.action == MotionEvent.ACTION_DOWN) {
            downTime = System.currentTimeMillis()
            onBtnTouch = true
            val t = object : Thread() {
                override fun run() {
                    while (onBtnTouch) {
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
                }
            }
            t.start()
        } else if (event.action == MotionEvent.ACTION_UP) {
            onBtnTouch = false
            if (thisTime - downTime < 500) {
                tvValue++
                val msg = handlerTemperatureAdd.obtainMessage()
                msg.arg1 = tvValue
                msg.arg2 = position
                handlerTemperatureAdd.sendMessage(msg)
            }
        } else if (event.action == MotionEvent.ACTION_CANCEL) {
            onBtnTouch = false
        }
    }

    private fun lessBrightness(event: MotionEvent?, position: Int) {
        if (event!!.action == MotionEvent.ACTION_DOWN) {
            downTime = System.currentTimeMillis()
            onBtnTouch = true
            val t = object : Thread() {
                override fun run() {
                    while (onBtnTouch) {
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
                }
            }
            t.start()
        } else if (event.action == MotionEvent.ACTION_UP) {
            onBtnTouch = false
            if (thisTime - downTime < 500) {
                tvValue++
                val msg = handlerBrightnessLess.obtainMessage()
                msg.arg1 = tvValue
                msg.arg2 = position
                handlerBrightnessLess.sendMessage(msg)
            }
        } else if (event.action == MotionEvent.ACTION_CANCEL) {
            onBtnTouch = false
        }
    }

    private fun addBrightness(event: MotionEvent?, position: Int) {
        if (event!!.action == MotionEvent.ACTION_DOWN) {
            downTime = System.currentTimeMillis()
            onBtnTouch = true
            val t = object : Thread() {
                override fun run() {
                    while (onBtnTouch) {
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
                }
            }
            t.start()
        } else if (event.action == MotionEvent.ACTION_UP) {
            onBtnTouch = false
            if (thisTime - downTime < 500) {
                tvValue++
                val msg = handlerBrightnessAdd.obtainMessage()
                msg.arg1 = tvValue
                msg.arg2 = position
                handlerBrightnessAdd.sendMessage(msg)
            }
        } else if (event.action == MotionEvent.ACTION_CANCEL) {
            onBtnTouch = false
        }
    }

    @SuppressLint("HandlerLeak")
    private val handlerBrightnessAdd = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            var pos = msg.arg2
            var seekBar = getViewByPosition(pos, R.id.cw_sbBrightness) as SeekBar?
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
                    opcode = Opcode.SET_LUM
                    params = byteArrayOf(seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
                else -> {
                    addImage!!.isEnabled = true
                    brightnText!!.text = seekBar!!.progress.toString() + "%"
                    itemGroup.brightness = seekBar!!.progress
                    opcode = Opcode.SET_LUM
                    params = byteArrayOf(seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }

            if (seekBar!!.progress > 0) {
                lessImage!!.isEnabled = true
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val handlerBrightnessLess = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            var pos = msg.arg2
            var seekBar = getViewByPosition(pos, R.id.cw_sbBrightness) as SeekBar?
            var brightnText = getViewByPosition(pos, R.id.cw_brightness_num) as TextView?
            var lessImage = getViewByPosition(pos, R.id.cw_brightness_less) as ImageView?
            var addImage = getViewByPosition(pos, R.id.cw_brightness_add) as ImageView?

            seekBar!!.progress--

            val address = data[pos].groupAddress
            val opcode: Byte
            val itemGroup = data[pos]
            var params: ByteArray

            when {
                seekBar!!.progress < 0 -> {
                    lessImage!!.isEnabled = false
                    onBtnTouch = false
                }
                seekBar!!.progress == 0 -> {
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

            if (seekBar!!.progress < 100) {
                addImage!!.isEnabled = true
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val handlerTemperatureAdd = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            var pos = msg.arg2
            var seekBar = getViewByPosition(pos, R.id.temperature) as SeekBar?
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
                    opcode = Opcode.SET_TEMPERATURE
                    params = byteArrayOf(0x05, seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
                else -> {
                    addImage!!.isEnabled = true
                    brightnText!!.text = seekBar!!.progress.toString() + "%"
                    itemGroup.temperature = seekBar!!.progress
                    opcode = Opcode.SET_TEMPERATURE
                    params = byteArrayOf(0x05, seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }

            if (seekBar!!.progress > 0) {
                lessImage!!.isEnabled = true
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val handlerTemperatureLess = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            var pos = msg.arg2

            var seekBar = getViewByPosition(pos, R.id.temperature) as SeekBar?
            var brightnText = getViewByPosition(pos, R.id.temperature_num) as TextView?
            var addImage = getViewByPosition(pos, R.id.temperature_add) as ImageView?
            var lessImage = getViewByPosition(pos, R.id.temperature_less) as ImageView?

            val address = data[pos].groupAddress
            val opcode: Byte
            val itemGroup = data[pos]
            var params: ByteArray

            seekBar!!.progress--

            when {
                seekBar!!.progress < 0 -> {
                    lessImage!!.isEnabled = false
                    onBtnTouch = false
                }
                seekBar!!.progress == 0 -> {
                    lessImage!!.isEnabled = false
                    brightnText!!.text = seekBar!!.progress.toString() + "%"
                    onBtnTouch = false
                    itemGroup.temperature = seekBar!!.progress
                    opcode = Opcode.SET_TEMPERATURE
                    params = byteArrayOf(0x05, seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
                else -> {
                    lessImage!!.isEnabled = true
                    brightnText!!.text = seekBar!!.progress.toString() + "%"
                    itemGroup.temperature = seekBar!!.progress
                    opcode = Opcode.SET_TEMPERATURE
                    params = byteArrayOf(0x05, seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }

            if (seekBar!!.progress > 0) {
                addImage!!.isEnabled = true
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val brightnessAdd = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            var pos = msg.arg2

            var seekBar = getViewByPosition(pos, R.id.sbBrightness) as SeekBar?
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
                    opcode = Opcode.SET_LUM
                    params = byteArrayOf(0x05, seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
                else -> {
                    addImage!!.isEnabled = true
                    brightnText!!.text = seekBar!!.progress.toString() + "%"
                    itemGroup.brightness = seekBar!!.progress
                    opcode = Opcode.SET_LUM
                    params = byteArrayOf(0x05, seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }

            if (seekBar!!.progress > 0) {
                lessImage!!.isEnabled = true
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val brightnessLess = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            var pos = msg.arg2
            var seekBar = getViewByPosition(pos, R.id.sbBrightness) as SeekBar?
            var brightnText = getViewByPosition(pos, R.id.sbBrightness_num) as TextView?
            var lessImage = getViewByPosition(pos, R.id.sbBrightness_less) as ImageView?
            var addImage = getViewByPosition(pos, R.id.sbBrightness_add) as ImageView?

            seekBar!!.progress--

            val address = data[pos].groupAddress
            val opcode: Byte
            val itemGroup = data[pos]
            var params: ByteArray

            when {
                seekBar!!.progress < 0 -> {
                    lessImage!!.isEnabled = false
                    onBtnTouch = false
                }
                seekBar!!.progress == 0 -> {
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

            if (seekBar!!.progress < 100) {
                addImage!!.isEnabled = true
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val algHandlerAdd = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            var pos = msg.arg2
            var seekBar = getViewByPosition(pos, R.id.speed_seekbar) as SeekBar?
            var speedTv = getViewByPosition(pos, R.id.speed_seekbar_alg_tv) as TextView?
            var lessImage = getViewByPosition(pos, R.id.speed_seekbar_alg_less) as ImageView?
            var addImage = getViewByPosition(pos, R.id.speed_seekbar_alg_add) as ImageView?
            seekBar!!.progress++

            val itemGroup = data[pos]

            when {
                seekBar!!.progress > 100 -> {
                    addImage!!.isEnabled = false
                    onBtnTouch = false
                }
                seekBar!!.progress == 100 -> {
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

            if (seekBar!!.progress < 100)
                lessImage!!.isEnabled = true
        }
    }

    @SuppressLint("HandlerLeak")
    private val algHandlerLess = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            var pos = msg.arg2
            var seekBar = getViewByPosition(pos, R.id.speed_seekbar) as SeekBar?
            var brightnText = getViewByPosition(pos, R.id.speed_seekbar_alg_tv) as TextView?
            var lessImage = getViewByPosition(pos, R.id.speed_seekbar_alg_less) as ImageView?
            var addImage = getViewByPosition(pos, R.id.speed_seekbar_alg_add) as ImageView?

            seekBar!!.progress--

            val address = data[pos].groupAddress
            val opcode: Byte
            val itemGroup = data[pos]
            var params: ByteArray

            when {
                seekBar.progress < 0 -> {
                    lessImage!!.isEnabled = false
                    onBtnTouch = false
                }
                seekBar.progress == 0 -> {
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

            var pos = msg.arg2
            var seekBar = getViewByPosition(pos, R.id.sb_w_bright) as SeekBar?
            var brightnText = getViewByPosition(pos, R.id.sb_w_bright_num) as TextView?
            var lessImage = getViewByPosition(pos, R.id.sb_w_bright_less) as ImageView?
            var addImage = getViewByPosition(pos, R.id.sb_w_bright_add) as ImageView?

            seekBar!!.progress--

            val address = data[pos].groupAddress
            val opcode: Byte
            val itemGroup = data[pos]
            var params: ByteArray

            when {
                seekBar!!.progress < 0 -> {
                    lessImage!!.isEnabled = false
                    onBtnTouch = false
                }
                seekBar!!.progress == 0 -> {
                    lessImage!!.isEnabled = false
                    brightnText!!.text = seekBar!!.progress.toString() + "%"
                    onBtnTouch = false
                    itemGroup.temperature = seekBar!!.progress
                    opcode = Opcode.SET_W_LUM
                    params = byteArrayOf(seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
                else -> {
                    lessImage!!.isEnabled = true
                    brightnText!!.text = seekBar!!.progress.toString() + "%"
                    itemGroup.temperature = seekBar!!.progress
                    opcode = Opcode.SET_W_LUM
                    params = byteArrayOf(seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }

            if (seekBar!!.progress < 100) {
                addImage!!.isEnabled = true
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val whitelightAdds = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            var pos = msg.arg2
            var seekBar = getViewByPosition(pos, R.id.sb_w_bright) as SeekBar?
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
                    opcode = Opcode.SET_W_LUM
                    params = byteArrayOf(seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
                else -> {
                    addImage!!.isEnabled = true
                    brightnText!!.text = seekBar!!.progress.toString() + "%"
                    itemGroup.temperature = seekBar!!.progress
                    opcode = Opcode.SET_W_LUM
                    params = byteArrayOf(seekBar!!.progress.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
                }
            }

            if (seekBar!!.progress < 100)
                lessImage!!.isEnabled = true
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        this.preTime = System.currentTimeMillis()
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        val pos = seekBar.tag as Int

        val address = if (pos < data.size)
            data[pos].groupAddress
        else
            0

        val opcode: Byte
        val itemGroup = data[pos]
        when (seekBar.id) {
            R.id.cw_sbBrightness -> {
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
            R.id.temperature -> {
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
            R.id.sbBrightness -> {
                (Objects.requireNonNull<View>(getViewByPosition(pos, R.id.sbBrightness_num)) as TextView).text = seekBar.progress.toString() + "%"
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
            R.id.sb_w_bright -> {
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
            R.id.cw_sbBrightness -> {
                val tvBrightness = getViewByPosition(position, R.id.cw_brightness_num) as TextView?
                if (tvBrightness != null) {
                    tvBrightness?.text = "$progress%"
                }
            }
            R.id.temperature -> {
                val tvTemperature = getViewByPosition(position, R.id.temperature_num) as TextView?
                if (tvTemperature != null) {
                    tvTemperature?.text = "$progress%"
                }
            }
            R.id.sbBrightness -> {
                val tvBrightnessRGB = getViewByPosition(position, R.id.sbBrightness_num) as TextView?
                if (tvBrightnessRGB != null) {
                    tvBrightnessRGB.text = "$progress%"
                }
            }
            R.id.sb_w_bright -> {
                val tvSbWhiteLight = getViewByPosition(position, R.id.sb_w_bright_num) as TextView?
                if (tvSbWhiteLight != null) {
                    tvSbWhiteLight.text = "$progress%"
                }
            }
        }
    }

    fun sendCmd(opcode: Byte, address: Int, progress: Int) {
        var progressCmd = progress
        if (progress > Constant.MAX_VALUE)
            progressCmd = Constant.MAX_VALUE

        var params: ByteArray
        params = when (opcode) {
            Opcode.SET_TEMPERATURE -> byteArrayOf(0x05, progressCmd.toByte())
            Opcode.SET_LUM -> byteArrayOf(progressCmd.toByte())
            else -> byteArrayOf(progressCmd.toByte())
        }
        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
    }

    open fun getIsColorMode(): Boolean {
        return isColorMode
    }
}

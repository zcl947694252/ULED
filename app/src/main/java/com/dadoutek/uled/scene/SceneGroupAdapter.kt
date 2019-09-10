package com.dadoutek.uled.scene

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
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
import java.util.*

/**
 * Created by hejiajun on 2018/5/5.
 */
class SceneGroupAdapter(layoutResId: Int, data: List<ItemGroup>) : BaseQuickAdapter<ItemGroup, BaseViewHolder>(layoutResId, data), SeekBar.OnSeekBarChangeListener {

    private var preTime: Long = 0
    private val delayTime = Constant.MAX_SCROLL_DELAY_VALUE
    private var addBrightnessCW: ImageView? = null
    private var lessBrightnessCW: ImageView? = null
    private var lessTemperatureCW: ImageView? = null
    private var addTemperatureCW: ImageView? = null
    private var sbBrightnessCW: SeekBar? = null
    private var sbtemperature: SeekBar? = null

    private var sbBrightnessRGB: SeekBar? = null
    private var sbWhiteLightRGB: SeekBar? = null
    private var addBrightnessRGB: ImageView? = null
    private var lessBrightnessRGB: ImageView? = null
    private var addWhiteLightRGB: ImageView? = null
    private var lessWhiteLightRGB: ImageView? = null

    internal var downTime: Long = 0//Button被按下时的时间
    internal var thisTime: Long = 0//while每次循环时的时间
    internal var onBtnTouch = false//Button是否被按下
    internal var tvValue = 0//TextView中的值
    @SuppressLint("ClickableViewAccessibility")
    override fun convert(helper: BaseViewHolder, item: ItemGroup) {

        val position = helper.layoutPosition
        sbBrightnessCW = helper.getView<SeekBar>(R.id.cw_sbBrightness)
        sbtemperature = helper.getView<SeekBar>(R.id.temperature)
        addBrightnessCW = helper.getView(R.id.cw_brightness_add)
        lessBrightnessCW = helper.getView(R.id.cw_brightness_less)
        lessTemperatureCW = helper.getView(R.id.temperature_less)
        addTemperatureCW = helper.getView(R.id.temperature_add)
        var docOne = helper.getView<Dot>(R.id.dot_one)

        var dotRgb = helper.getView<ImageView>(R.id.dot_rgb)


        sbBrightnessRGB = helper.getView(R.id.sbBrightness)
        sbWhiteLightRGB = helper.getView(R.id.sb_w_bright)
        addBrightnessRGB = helper.getView(R.id.sbBrightness_add)
        lessBrightnessRGB = helper.getView(R.id.sbBrightness_less)
        addWhiteLightRGB = helper.getView(R.id.sb_w_bright_add)
        lessWhiteLightRGB = helper.getView(R.id.sb_w_bright_less)

        //0x4FFFE0
        docOne.setChecked(true, if (item.color == 16777215) {
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
        Log.e("zcl", "zclSceneGroupAdapter*****************${0xff0000 shl 8}*****************" + { item.color and 0xffffff })


        helper.setText(R.id.name_gp, item.gpName)


        if (OtherUtils.isRGBGroup(DBUtils.getGroupByMesh(item.groupAress))) {
            helper.setProgress(R.id.sbBrightness, item.brightness)
            helper.setProgress(R.id.sb_w_bright, item.temperature)
            helper.setText(R.id.sbBrightness_num, sbBrightnessRGB!!.progress.toString() + "%")
            helper.setText(R.id.sb_w_bright_num, sbWhiteLightRGB!!.progress.toString() + "%")

            when {
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
            }

        } else {
            helper.setProgress(R.id.cw_sbBrightness, item.brightness)
            helper.setProgress(R.id.temperature, item.temperature)
            helper.setText(R.id.cw_brightness_num, sbBrightnessCW!!.progress.toString() + "%")
            helper.setText(R.id.temperature_num, sbtemperature!!.progress.toString() + "%")

            if (item.brightness <= 0) {
                addBrightnessCW!!.isEnabled = true
                lessBrightnessCW!!.isEnabled = false
            } else if (item.brightness >= 100) {
                addBrightnessCW!!.isEnabled = false
                lessBrightnessCW!!.isEnabled = true
            } else {
                addBrightnessCW!!.isEnabled = true
                lessBrightnessCW!!.isEnabled = true
            }

            if (item.temperature <= 0) {
                addTemperatureCW!!.isEnabled = true
                lessTemperatureCW!!.isEnabled = false
            } else if (item.temperature >= 100) {
                addTemperatureCW!!.isEnabled = false
                lessTemperatureCW!!.isEnabled = true
            } else {
                addTemperatureCW!!.isEnabled = true
                lessTemperatureCW!!.isEnabled = true
            }
        }

        if (OtherUtils.isRGBGroup(DBUtils.getGroupByMesh(item.groupAress))) {
            helper.setGone(R.id.textView7, true)
            helper.setGone(R.id.oval, true)
            helper.setGone(R.id.rgb_scene, true)
            helper.setGone(R.id.cw_scene, false)
            helper.setGone(R.id.switch_scene, false)
            helper.setGone(R.id.scene_curtain, false)
            helper.setGone(R.id.scene_relay, false)
        } else if (OtherUtils.isNormalGroup(DBUtils.getGroupByMesh(item.groupAress))) {
            helper.setGone(R.id.textView7, false)
            helper.setGone(R.id.oval, false)
            helper.setGone(R.id.rgb_scene, false)
            helper.setGone(R.id.cw_scene, true)
            helper.setGone(R.id.switch_scene, false)
            helper.setGone(R.id.scene_curtain, false)
            helper.setGone(R.id.scene_relay, false)
        } else if (OtherUtils.isConnector(DBUtils.getGroupByMesh(item.groupAress))) {
            helper.setGone(R.id.textView7, false)
            helper.setGone(R.id.oval, false)
            helper.setGone(R.id.rgb_scene, false)
            helper.setGone(R.id.cw_scene, false)
            helper.setGone(R.id.switch_scene, true)
            helper.setGone(R.id.scene_curtain, false)
            helper.setGone(R.id.scene_relay, true)
            if (item.isNo) {
                helper.setChecked(R.id.rg_xx, true)
                helper.setImageResource(R.id.scene_relay, R.drawable.scene_acceptor_yes)
            } else {
                helper.setChecked(R.id.rg_yy, true)
                helper.setImageResource(R.id.scene_relay, R.drawable.scene_acceptor_no)
            }
        } else if (OtherUtils.isCurtain(DBUtils.getGroupByMesh(item.groupAress))) {
            helper.setGone(R.id.textView7, false)
            helper.setGone(R.id.oval, false)
            helper.setGone(R.id.rgb_scene, false)
            helper.setGone(R.id.cw_scene, false)
            helper.setGone(R.id.switch_scene, true)
            helper.setGone(R.id.scene_relay, false)
            helper.setGone(R.id.scene_curtain, true)
            if (item.isNo) {
                helper.setChecked(R.id.rg_xx, true)
                helper.setImageResource(R.id.scene_curtain, R.drawable.scene_curtain_yes)
            } else {
                helper.setChecked(R.id.rg_yy, true)
                helper.setImageResource(R.id.scene_curtain, R.drawable.scene_curtain_no)
            }
        } else {
            helper.setGone(R.id.textView7, false)
            helper.setGone(R.id.oval, false)
            helper.setGone(R.id.rgb_scene, false)
            helper.setGone(R.id.cw_scene, true)
            helper.setGone(R.id.switch_scene, false)
        }

        sbBrightnessCW!!.tag = position
        sbtemperature!!.tag = position
        sbBrightnessRGB!!.tag = position
        sbWhiteLightRGB!!.tag = position
        sbBrightnessCW!!.setOnSeekBarChangeListener(this)
        sbtemperature!!.setOnSeekBarChangeListener(this)
        sbBrightnessRGB!!.setOnSeekBarChangeListener(this)
        sbWhiteLightRGB!!.setOnSeekBarChangeListener(this)
        helper.addOnClickListener(R.id.btn_delete)
        helper.addOnClickListener(R.id.dot_one)
        helper.addOnClickListener(R.id.dot_rgb)
        helper.addOnClickListener(R.id.sbBrightness_add)
        helper.addOnClickListener(R.id.rg_xx)
        helper.addOnClickListener(R.id.rg_yy)

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

        addWhiteLightRGB!!.setOnTouchListener { v, event ->
            addRGBWhiteLight(event, position)
            true
        }

        lessWhiteLightRGB!!.setOnTouchListener { v, event ->
            lessRGBWhiteLight(event, position)
            true
        }

    }

    private fun lessRGBWhiteLight(event: MotionEvent?, position: Int) {
        if (event!!.action == MotionEvent.ACTION_DOWN) {
            //                    tvValue = Integer.parseInt(textView.getText().toString());
            downTime = System.currentTimeMillis()
            onBtnTouch = true
            val t = object : Thread() {
                override fun run() {
                    while (onBtnTouch) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            val msg = whiteLight_less.obtainMessage()
                            msg.arg1 = tvValue
                            msg.arg2 = position
                            whiteLight_less.sendMessage(msg)
                            Log.e("TAG_TOUCH", tvValue++.toString())
                            try {
                                Thread.sleep(100)
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
                val msg = whiteLight_less.obtainMessage()
                msg.arg1 = tvValue
                msg.arg2 = position
                whiteLight_less.sendMessage(msg)
            }
        } else if (event.action == MotionEvent.ACTION_CANCEL) {
            onBtnTouch = false
        }
    }

    private fun addRGBWhiteLight(event: MotionEvent?, position: Int) {
        if (event!!.action == MotionEvent.ACTION_DOWN) {
            //                    tvValue = Integer.parseInt(textView.getText().toString());
            downTime = System.currentTimeMillis()
            onBtnTouch = true
            val t = object : Thread() {
                override fun run() {
                    while (onBtnTouch) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            val msg = whiteLight_adds.obtainMessage()
                            msg.arg1 = tvValue
                            msg.arg2 = position
                            whiteLight_adds.sendMessage(msg)
                            Log.e("TAG_TOUCH", tvValue++.toString())
                            try {
                                Thread.sleep(100)
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
                val msg = whiteLight_adds.obtainMessage()
                msg.arg1 = tvValue
                msg.arg2 = position
                whiteLight_adds.sendMessage(msg)
            }
        } else if (event.action == MotionEvent.ACTION_CANCEL) {
            onBtnTouch = false
        }
    }

    private fun lessRGBBrightness(event: MotionEvent?, position: Int) {
        if (event!!.action == MotionEvent.ACTION_DOWN) {
            //                    tvValue = Integer.parseInt(textView.getText().toString());
            downTime = System.currentTimeMillis()
            onBtnTouch = true
            val t = object : Thread() {
                override fun run() {
                    while (onBtnTouch) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            val msg = brightness_less.obtainMessage()
                            msg.arg1 = tvValue
                            msg.arg2 = position
                            brightness_less.sendMessage(msg)
                            Log.e("TAG_TOUCH", tvValue++.toString())
                            try {
                                Thread.sleep(100)
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
                val msg = brightness_less.obtainMessage()
                msg.arg1 = tvValue
                msg.arg2 = position
                brightness_less.sendMessage(msg)
            }
        } else if (event.action == MotionEvent.ACTION_CANCEL) {
            onBtnTouch = false
        }
    }

    private fun addRGBBrightness(event: MotionEvent?, position: Int) {
        if (event!!.action == MotionEvent.ACTION_DOWN) {
            //                    tvValue = Integer.parseInt(textView.getText().toString());
            downTime = System.currentTimeMillis()
            onBtnTouch = true
            val t = object : Thread() {
                override fun run() {
                    while (onBtnTouch) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            val msg = brightness_add.obtainMessage()
                            msg.arg1 = tvValue
                            msg.arg2 = position
                            brightness_add.sendMessage(msg)
                            Log.e("TAG_TOUCH", tvValue++.toString())
                            try {
                                Thread.sleep(100)
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
                val msg = brightness_add.obtainMessage()
                msg.arg1 = tvValue
                msg.arg2 = position
                brightness_add.sendMessage(msg)
            }
        } else if (event.action == MotionEvent.ACTION_CANCEL) {
            onBtnTouch = false
        }
    }

    private fun lessTemperature(event: MotionEvent?, position: Int) {
        if (event!!.action == MotionEvent.ACTION_DOWN) {
            //                    tvValue = Integer.parseInt(textView.getText().toString());
            downTime = System.currentTimeMillis()
            onBtnTouch = true
            val t = object : Thread() {
                override fun run() {
                    while (onBtnTouch) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            val msg = handler_temperature_less.obtainMessage()
                            msg.arg1 = tvValue
                            msg.arg2 = position
                            handler_temperature_less.sendMessage(msg)
                            Log.e("TAG_TOUCH", tvValue++.toString())
                            try {
                                Thread.sleep(100)
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
                val msg = handler_temperature_less.obtainMessage()
                msg.arg1 = tvValue
                msg.arg2 = position
                handler_temperature_less.sendMessage(msg)
            }
        } else if (event.action == MotionEvent.ACTION_CANCEL) {
            onBtnTouch = false
        }
    }

    private fun addTemperature(event: MotionEvent?, position: Int) {
        if (event!!.action == MotionEvent.ACTION_DOWN) {
            //                    tvValue = Integer.parseInt(textView.getText().toString());
            downTime = System.currentTimeMillis()
            onBtnTouch = true
            val t = object : Thread() {
                override fun run() {
                    while (onBtnTouch) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            val msg = handler_temperature_add.obtainMessage()
                            msg.arg1 = tvValue
                            msg.arg2 = position
                            handler_temperature_add.sendMessage(msg)
                            Log.e("TAG_TOUCH", tvValue++.toString())
                            try {
                                Thread.sleep(100)
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
                val msg = handler_temperature_add.obtainMessage()
                msg.arg1 = tvValue
                msg.arg2 = position
                handler_temperature_add.sendMessage(msg)
            }
        } else if (event.action == MotionEvent.ACTION_CANCEL) {
            onBtnTouch = false
        }
    }

    private fun lessBrightness(event: MotionEvent?, position: Int) {
        if (event!!.action == MotionEvent.ACTION_DOWN) {
            //                    tvValue = Integer.parseInt(textView.getText().toString());
            downTime = System.currentTimeMillis()
            onBtnTouch = true
            val t = object : Thread() {
                override fun run() {
                    while (onBtnTouch) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            val msg = handler_brightness_less.obtainMessage()
                            msg.arg1 = tvValue
                            msg.arg2 = position
                            handler_brightness_less.sendMessage(msg)
                            Log.e("TAG_TOUCH", tvValue++.toString())
                            try {
                                Thread.sleep(100)
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
                val msg = handler_brightness_less.obtainMessage()
                msg.arg1 = tvValue
                msg.arg2 = position
                handler_brightness_less.sendMessage(msg)
            }
        } else if (event.action == MotionEvent.ACTION_CANCEL) {
            onBtnTouch = false
        }
    }

    private fun addBrightness(event: MotionEvent?, position: Int) {
        if (event!!.action == MotionEvent.ACTION_DOWN) {
            //                    tvValue = Integer.parseInt(textView.getText().toString());
            downTime = System.currentTimeMillis()
            onBtnTouch = true
            val t = object : Thread() {
                override fun run() {
                    while (onBtnTouch) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            val msg = handler_brightness_add.obtainMessage()
                            msg.arg1 = tvValue
                            msg.arg2 = position
                            handler_brightness_add.sendMessage(msg)
                            Log.e("TAG_TOUCH", tvValue++.toString())
                            try {
                                Thread.sleep(100)
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
                val msg = handler_brightness_add.obtainMessage()
                msg.arg1 = tvValue
                msg.arg2 = position
                handler_brightness_add.sendMessage(msg)
            }
        } else if (event.action == MotionEvent.ACTION_CANCEL) {
            onBtnTouch = false
        }
    }

    @SuppressLint("HandlerLeak")
    private val handler_brightness_add = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            var pos = msg.arg2

            var seekBar = getViewByPosition(pos, R.id.cw_sbBrightness) as SeekBar?

            var brightnText = getViewByPosition(pos, R.id.cw_brightness_num) as TextView?

            var addImage = getViewByPosition(pos, R.id.cw_brightness_add) as ImageView?

            var lessImage = getViewByPosition(pos, R.id.cw_brightness_less) as ImageView?

            val address = data[pos].groupAress
            val opcode: Byte
            val itemGroup = data[pos]
            var params: ByteArray

            seekBar!!.progress++

            if (seekBar!!.progress > 100) {
                addImage!!.isEnabled = false
                onBtnTouch = false
            } else if (seekBar!!.progress == 100) {
                addImage!!.isEnabled = false
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                onBtnTouch = false
                itemGroup.brightness = seekBar!!.progress
                opcode = Opcode.SET_LUM
                params = byteArrayOf(seekBar!!.progress.toByte())
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
            } else {
                addImage!!.isEnabled = true
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                itemGroup.brightness = seekBar!!.progress
                opcode = Opcode.SET_LUM
                params = byteArrayOf(seekBar!!.progress.toByte())
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
            }

            if (seekBar!!.progress > 0) {
                lessImage!!.isEnabled = true
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val handler_brightness_less = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            var pos = msg.arg2

            var seekBar = getViewByPosition(pos, R.id.cw_sbBrightness) as SeekBar?

            var brightnText = getViewByPosition(pos, R.id.cw_brightness_num) as TextView?

            var lessImage = getViewByPosition(pos, R.id.cw_brightness_less) as ImageView?

            var addImage = getViewByPosition(pos, R.id.cw_brightness_add) as ImageView?

            seekBar!!.progress--

            val address = data[pos].groupAress
            val opcode: Byte
            val itemGroup = data[pos]
            var params: ByteArray

            if (seekBar!!.progress < 0) {
                lessImage!!.isEnabled = false
                onBtnTouch = false
            } else if (seekBar!!.progress == 0) {
                lessImage!!.isEnabled = false
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                onBtnTouch = false
                itemGroup.brightness = seekBar!!.progress
                opcode = Opcode.SET_LUM
                params = byteArrayOf(seekBar!!.progress.toByte())
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
            } else {
                lessImage!!.isEnabled = true
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                itemGroup.brightness = seekBar!!.progress
                opcode = Opcode.SET_LUM
                params = byteArrayOf(seekBar!!.progress.toByte())
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
            }

            if (seekBar!!.progress < 100) {
                addImage!!.isEnabled = true
            }
        }
    }


    @SuppressLint("HandlerLeak")
    private val handler_temperature_add = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            var pos = msg.arg2

            var seekBar = getViewByPosition(pos, R.id.temperature) as SeekBar?

            var brightnText = getViewByPosition(pos, R.id.temperature_num) as TextView?

            var addImage = getViewByPosition(pos, R.id.temperature_add) as ImageView?

            var lessImage = getViewByPosition(pos, R.id.temperature_less) as ImageView?

            val address = data[pos].groupAress
            val opcode: Byte
            val itemGroup = data[pos]
            var params: ByteArray

            seekBar!!.progress++

            if (seekBar!!.progress > 100) {
                addImage!!.isEnabled = false
                onBtnTouch = false
            } else if (seekBar!!.progress == 100) {
                addImage!!.isEnabled = false
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                onBtnTouch = false
                itemGroup.temperature = seekBar!!.progress
                opcode = Opcode.SET_TEMPERATURE
                params = byteArrayOf(0x05, seekBar!!.progress.toByte())
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
            } else {
                addImage!!.isEnabled = true
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                itemGroup.temperature = seekBar!!.progress
                opcode = Opcode.SET_TEMPERATURE
                params = byteArrayOf(0x05, seekBar!!.progress.toByte())
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
            }

            if (seekBar!!.progress > 0) {
                lessImage!!.isEnabled = true
            }
        }
    }


    @SuppressLint("HandlerLeak")
    private val handler_temperature_less = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            var pos = msg.arg2

            var seekBar = getViewByPosition(pos, R.id.temperature) as SeekBar?

            var brightnText = getViewByPosition(pos, R.id.temperature_num) as TextView?

            var addImage = getViewByPosition(pos, R.id.temperature_add) as ImageView?

            var lessImage = getViewByPosition(pos, R.id.temperature_less) as ImageView?

            val address = data[pos].groupAress
            val opcode: Byte
            val itemGroup = data[pos]
            var params: ByteArray

            seekBar!!.progress--

            if (seekBar!!.progress < 0) {
                lessImage!!.isEnabled = false
                onBtnTouch = false
            } else if (seekBar!!.progress == 0) {
                lessImage!!.isEnabled = false
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                onBtnTouch = false
                itemGroup.temperature = seekBar!!.progress
                opcode = Opcode.SET_TEMPERATURE
                params = byteArrayOf(0x05, seekBar!!.progress.toByte())
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
            } else {
                lessImage!!.isEnabled = true
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                itemGroup.temperature = seekBar!!.progress
                opcode = Opcode.SET_TEMPERATURE
                params = byteArrayOf(0x05, seekBar!!.progress.toByte())
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
            }

            if (seekBar!!.progress > 0) {
                addImage!!.isEnabled = true
            }
        }
    }


    @SuppressLint("HandlerLeak")
    private val brightness_add = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            var pos = msg.arg2

            var seekBar = getViewByPosition(pos, R.id.sbBrightness) as SeekBar?

            var brightnText = getViewByPosition(pos, R.id.sbBrightness_num) as TextView?

            var addImage = getViewByPosition(pos, R.id.sbBrightness_add) as ImageView?

            var lessImage = getViewByPosition(pos, R.id.sbBrightness_less) as ImageView?

            val address = data[pos].groupAress
            val opcode: Byte
            val itemGroup = data[pos]
            var params: ByteArray

            seekBar!!.progress++

            if (seekBar!!.progress > 100) {
                addImage!!.isEnabled = false
                onBtnTouch = false
            } else if (seekBar!!.progress == 100) {
                addImage!!.isEnabled = false
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                onBtnTouch = false
                itemGroup.brightness = seekBar!!.progress
                opcode = Opcode.SET_LUM
                params = byteArrayOf(0x05, seekBar!!.progress.toByte())
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
            } else {
                addImage!!.isEnabled = true
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                itemGroup.brightness = seekBar!!.progress
                opcode = Opcode.SET_LUM
                params = byteArrayOf(0x05, seekBar!!.progress.toByte())
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
            }

            if (seekBar!!.progress > 0) {
                lessImage!!.isEnabled = true
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val brightness_less = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            var pos = msg.arg2

            var seekBar = getViewByPosition(pos, R.id.sbBrightness) as SeekBar?

            var brightnText = getViewByPosition(pos, R.id.sbBrightness_num) as TextView?

            var lessImage = getViewByPosition(pos, R.id.sbBrightness_less) as ImageView?

            var addImage = getViewByPosition(pos, R.id.sbBrightness_add) as ImageView?

            seekBar!!.progress--

            val address = data[pos].groupAress
            val opcode: Byte
            val itemGroup = data[pos]
            var params: ByteArray

            if (seekBar!!.progress < 0) {
                lessImage!!.isEnabled = false
                onBtnTouch = false
            } else if (seekBar!!.progress == 0) {
                lessImage!!.isEnabled = false
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                onBtnTouch = false
                itemGroup.brightness = seekBar!!.progress
                opcode = Opcode.SET_LUM
                params = byteArrayOf(seekBar!!.progress.toByte())
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
            } else {
                lessImage!!.isEnabled = true
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                itemGroup.brightness = seekBar!!.progress
                opcode = Opcode.SET_LUM
                params = byteArrayOf(seekBar!!.progress.toByte())
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
            }

            if (seekBar!!.progress < 100) {
                addImage!!.isEnabled = true
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val whiteLight_less = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            var pos = msg.arg2

            var seekBar = getViewByPosition(pos, R.id.sb_w_bright) as SeekBar?

            var brightnText = getViewByPosition(pos, R.id.sb_w_bright_num) as TextView?

            var lessImage = getViewByPosition(pos, R.id.sb_w_bright_less) as ImageView?

            var addImage = getViewByPosition(pos, R.id.sb_w_bright_add) as ImageView?

            seekBar!!.progress--

            val address = data[pos].groupAress
            val opcode: Byte
            val itemGroup = data[pos]
            var params: ByteArray

            if (seekBar!!.progress < 0) {
                lessImage!!.isEnabled = false
                onBtnTouch = false
            } else if (seekBar!!.progress == 0) {
                lessImage!!.isEnabled = false
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                onBtnTouch = false
                itemGroup.temperature = seekBar!!.progress
                opcode = Opcode.SET_W_LUM
                params = byteArrayOf(seekBar!!.progress.toByte())
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
            } else {
                lessImage!!.isEnabled = true
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                itemGroup.temperature = seekBar!!.progress
                opcode = Opcode.SET_W_LUM
                params = byteArrayOf(seekBar!!.progress.toByte())
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
            }

            if (seekBar!!.progress < 100) {
                addImage!!.isEnabled = true
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val whiteLight_adds = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            var pos = msg.arg2

            var seekBar = getViewByPosition(pos, R.id.sb_w_bright) as SeekBar?

            var brightnText = getViewByPosition(pos, R.id.sb_w_bright_num) as TextView?

            var lessImage = getViewByPosition(pos, R.id.sb_w_bright_less) as ImageView?

            var addImage = getViewByPosition(pos, R.id.sb_w_bright_add) as ImageView?

            seekBar!!.progress++

            val address = data[pos].groupAress
            val opcode: Byte
            val itemGroup = data[pos]
            var params: ByteArray

            if (seekBar!!.progress > 100) {
                addImage!!.isEnabled = false
                onBtnTouch = false
            } else if (seekBar!!.progress == 100) {
                addImage!!.isEnabled = false
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                onBtnTouch = false
                itemGroup.temperature = seekBar!!.progress
                opcode = Opcode.SET_W_LUM
                params = byteArrayOf(seekBar!!.progress.toByte())
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
            } else {
                addImage!!.isEnabled = true
                brightnText!!.text = seekBar!!.progress.toString() + "%"
                itemGroup.temperature = seekBar!!.progress
                opcode = Opcode.SET_W_LUM
                params = byteArrayOf(seekBar!!.progress.toByte())
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
            }

            if (seekBar!!.progress < 100) {
                lessImage!!.isEnabled = true
            }
        }
    }


    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        val currentTime = System.currentTimeMillis()
        val position = seekBar.tag as Int

        if (fromUser) {
            changeTextView(seekBar, progress, position)
        }

        if (currentTime - this.preTime > this.delayTime) {
            if (fromUser) {
                val address = data[position].groupAress
                val opcode: Byte
                if (seekBar.id == R.id.cw_sbBrightness) {
                    opcode = Opcode.SET_LUM
                    Thread { sendCmd(opcode, address, progress) }.start()
                } else if (seekBar.id == R.id.temperature) {
                    opcode = Opcode.SET_TEMPERATURE
                    Thread { sendCmd(opcode, address, progress) }.start()
                } else if (seekBar.id == R.id.sbBrightness) {
                    opcode = Opcode.SET_LUM
                    Thread { sendCmd(opcode, address, progress) }.start()
                } else if (seekBar.id == R.id.sb_w_bright) {
                    opcode = Opcode.SET_W_LUM
                    Thread { sendCmd(opcode, address, progress) }.start()
                }
            }
            this.preTime = currentTime
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        this.preTime = System.currentTimeMillis()
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        val pos = seekBar.tag as Int
        val address = data[pos].groupAress
        val opcode: Byte
        val itemGroup = data[pos]
        if (seekBar.id == R.id.cw_sbBrightness) {
            (Objects.requireNonNull<View>(getViewByPosition(pos, R.id.cw_brightness_num)) as TextView).text = seekBar.progress.toString() + "%"
            itemGroup.brightness = seekBar.progress
            opcode = Opcode.SET_LUM
            Thread { sendCmd(opcode, address, seekBar.progress) }.start()

            if (seekBar.progress <= 0) {
                addBrightnessCW!!.isEnabled = true
                lessBrightnessCW!!.isEnabled = false
            } else if (seekBar.progress >= 100) {
                addBrightnessCW!!.isEnabled = false
                lessBrightnessCW!!.isEnabled = true
            } else {
                addBrightnessCW!!.isEnabled = true
                lessBrightnessCW!!.isEnabled = true
            }

        } else if (seekBar.id == R.id.temperature) {
            (Objects.requireNonNull<View>(getViewByPosition(pos, R.id.temperature_num)) as TextView).text = seekBar.progress.toString() + "%"
            itemGroup.temperature = seekBar.progress
            opcode = Opcode.SET_TEMPERATURE
            Thread { sendCmd(opcode, address, seekBar.progress) }.start()

            if (seekBar.progress <= 0) {
                addTemperatureCW!!.isEnabled = true
                lessTemperatureCW!!.isEnabled = false
            } else if (seekBar.progress >= 100) {
                addTemperatureCW!!.isEnabled = false
                lessTemperatureCW!!.isEnabled = true
            } else {
                addTemperatureCW!!.isEnabled = true
                lessTemperatureCW!!.isEnabled = true
            }

        } else if (seekBar.id == R.id.sbBrightness) {
            (Objects.requireNonNull<View>(getViewByPosition(pos, R.id.sbBrightness_num)) as TextView).text = seekBar.progress.toString() + "%"
            itemGroup.brightness = seekBar.progress
            opcode = Opcode.SET_LUM
            Thread { sendCmd(opcode, address, seekBar.progress) }.start()

            if (seekBar.progress <= 0) {
                addBrightnessRGB!!.isEnabled = true
                lessBrightnessRGB!!.isEnabled = false
            } else if (seekBar.progress >= 100) {
                addBrightnessRGB!!.isEnabled = false
                lessBrightnessRGB!!.isEnabled = true
            } else {
                addBrightnessRGB!!.isEnabled = true
                lessBrightnessRGB!!.isEnabled = true
            }

        } else if (seekBar.id == R.id.sb_w_bright) {
            (Objects.requireNonNull<View>(getViewByPosition(pos, R.id.sb_w_bright_num)) as TextView).text = seekBar.progress.toString() + "%"
            itemGroup.temperature = seekBar.progress
            opcode = Opcode.SET_W_LUM
            Thread { sendCmd(opcode, address, seekBar.progress) }.start()

            if (seekBar.progress <= 0) {
                addWhiteLightRGB!!.isEnabled = true
                lessWhiteLightRGB!!.isEnabled = false
            } else if (seekBar.progress >= 100) {
                addWhiteLightRGB!!.isEnabled = false
                lessWhiteLightRGB!!.isEnabled = true
            } else {
                addWhiteLightRGB!!.isEnabled = true
                lessWhiteLightRGB!!.isEnabled = true
            }
        }
        notifyItemChanged(seekBar.tag as Int)
    }

    fun changeTextView(seekBar: SeekBar, progress: Int, position: Int) {
        if (seekBar.id == R.id.cw_sbBrightness) {
            val tvBrightness = getViewByPosition(position, R.id.cw_brightness_num) as TextView?
            if (tvBrightness != null) {
                tvBrightness?.text = progress.toString() + "%"
            }
        } else if (seekBar.id == R.id.temperature) {
            val tvTemperature = getViewByPosition(position, R.id.temperature_num) as TextView?
            if (tvTemperature != null) {
                tvTemperature?.text = progress.toString() + "%"
            }
        } else if (seekBar.id == R.id.sbBrightness) {
            val tvBrightnessRGB = getViewByPosition(position, R.id.sbBrightness_num) as TextView?
            if (tvBrightnessRGB != null) {
                tvBrightnessRGB.text = progress.toString() + "%"
            }
        } else if (seekBar.id == R.id.sb_w_bright) {
            val tvSbWhiteLight = getViewByPosition(position, R.id.sb_w_bright_num) as TextView?
            if (tvSbWhiteLight != null) {
                tvSbWhiteLight.text = progress.toString() + "%"
            }
        }
    }

    fun sendCmd(opcode: Byte, address: Int, progress: Int) {
        var progressCmd = progress
        if (progress > Constant.MAX_VALUE) {
            progressCmd = Constant.MAX_VALUE
        }
        var params: ByteArray
        if (opcode == Opcode.SET_TEMPERATURE) {
            params = byteArrayOf(0x05, progressCmd.toByte())
        } else if (opcode == Opcode.SET_LUM) {
            params = byteArrayOf(progressCmd.toByte())
        } else {
            params = byteArrayOf(progressCmd.toByte())
        }
        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, address, params)
    }
}

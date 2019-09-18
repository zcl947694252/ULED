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
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DbColorNode
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.InputRGBColorDialog
import kotlinx.android.synthetic.main.activity_select_color_gradient.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.defaults.colorpicker.ColorObserver

class SelectColorGradientAct : TelinkBaseActivity(), View.OnClickListener {
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
        colorNode = intent.getSerializableExtra(Constant.COLOR_NODE_KEY) as? DbColorNode
    }

    @SuppressLint("ClickableViewAccessibility", "StringFormatMatches")
    private fun initView() {
        toolbar.title = getString(R.string.color_check)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

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

        sbBrightness.setOnSeekBarChangeListener(barChangeListener)
        sb_w_bright.setOnSeekBarChangeListener(barChangeListener)
        ll_r_r.setOnClickListener(this)
        ll_g_g.setOnClickListener(this)
        ll_b_b.setOnClickListener(this)
        if (colorNode!!.rgbw == -1) {
            sbBrightness.progress = 100
            colorNode!!.brightness = 100
            sb_w_bright.progress = 0
            sbBrightness_num.text = 100.toString() + "%"
            sb_w_bright_num.text = 0.toString() + "%"

            if (sbBrightness!!.progress >= 100) {
                sbBrightness_add.isEnabled = false
                sbBrightness_less.isEnabled = true
            } else if (sbBrightness!!.progress <= 0) {
                sbBrightness_less.isEnabled = false
                sbBrightness_add.isEnabled = true
            } else {
                sbBrightness_less.isEnabled = true
                sbBrightness_add.isEnabled = true
            }


            if (sb_w_bright.progress >= 100) {
                sb_w_bright_add.isEnabled = false
                sb_w_bright_less.isEnabled = true
            } else if (sb_w_bright.progress <= 0) {
                sb_w_bright_less.isEnabled = false
                sb_w_bright_add.isEnabled = true
            } else {
                sb_w_bright_less.isEnabled = true
                sb_w_bright_add.isEnabled = true
            }
        } else {

            var w = ((colorNode?.rgbw ?: 0) and 0xff000000.toInt()) shr 24
            var r = Color.red(colorNode?.rgbw!!)
            var g = Color.green(colorNode?.rgbw!!)
            var b = Color.blue(colorNode?.rgbw!!)
            color_picker.setInitialColor((colorNode?.rgbw ?: 0 and 0xffffff) or 0xff000000.toInt())

            sbBrightness.progress = colorNode!!.brightness
            sb_w_bright.progress = w
            sbBrightness_num.text = colorNode!!.brightness.toString() + "%"
            sb_w_bright_num.text = w.toString() + "%"

            if (sbBrightness!!.progress >= 100) {
                sbBrightness_add.isEnabled = false
                sbBrightness_less.isEnabled = true
            } else if (sbBrightness!!.progress <= 0) {
                sbBrightness_less.isEnabled = false
                sbBrightness_add.isEnabled = true
            } else {
                sbBrightness_less.isEnabled = true
                sbBrightness_add.isEnabled = true
            }


            if (sb_w_bright.progress >= 100) {
                sb_w_bright_add.isEnabled = false
                sb_w_bright_less.isEnabled = true
            } else if (sb_w_bright.progress <= 0) {
                sb_w_bright_less.isEnabled = false
                sb_w_bright_add.isEnabled = true
            } else {
                sb_w_bright_less.isEnabled = true
                sb_w_bright_add.isEnabled = true
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
//                redColor = color_r.text.toString().toInt()
//                greenColor = color_g.text.toString().toInt()
//                blueColor = color_b.text.toString().toInt()
//                setColorPicker()
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
        var w = sb_w_bright.progress
        if (w <= 0) w = 1
        val color: Int = (w shl 24) or (r shl 16) or (g shl 8) or b

        var ws = (color!! and 0xff000000.toInt()) shr 24
        val red = (color!! and 0xff0000) shr 16
        val green = (color and 0x00ff00) shr 8
        val blue = color and 0x0000ff

        color_picker.setInitialColor((color and 0xffffff) or 0xff000000.toInt())
        var showW = ws
        Thread {

            try {

                if (w!! > Constant.MAX_VALUE) {
                    w = Constant.MAX_VALUE
                }
                if (ws > Constant.MAX_VALUE) {
                    ws = Constant.MAX_VALUE
                }
                if (ws == -1) {
                    ws = 0
                    showW = 0
                }

                Thread.sleep(80)
                changeColor(red.toByte(), green.toByte(), blue.toByte(), true)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }.start()

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
        val w = sb_w_bright.progress

        val color: Int = (w shl 24) or (r shl 16) or (g shl 8) or b
        Log.d("", "onColorSelected: " + Integer.toHexString(color))
        if (fromUser) {
            if (r == 0 && g == 0 && b == 0) {
            } else {
                changeColor(r.toByte(), g.toByte(), b.toByte(), false)
            }
        }

        color_r?.text = r.toString()
        color_g?.text = g.toString()
        color_b?.text = b.toString()

        colorNode!!.rgbw = color
    }

    private fun changeColor(R: Byte, G: Byte, B: Byte, isOnceSet: Boolean) {
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

    private val barChangeListener = object : SeekBar.OnSeekBarChangeListener {

        private var preTime: Long = 0
        private val delayTime = Constant.MAX_SCROLL_DELAY_VALUE

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
            onValueChangeView(seekBar, progress, true)
            val currentTime = System.currentTimeMillis()

            if (currentTime - this.preTime > this.delayTime) {
                this.onValueChange(seekBar, progress, true)
                this.preTime = currentTime
            }
        }

        private fun onValueChangeView(view: View, progress: Int, immediate: Boolean) {
            if (view === sbBrightness) {
                sbBrightness_num.text = progress.toString() + "%"
            } else if (view === sb_w_bright) {
                sb_w_bright_num.text = progress.toString() + "%"
            }
        }

        private fun onValueChange(view: View, progress: Int, immediate: Boolean) {

            var addr = colorNode!!.dstAddress

            val opcode: Byte
            val params: ByteArray

            if (view == sbBrightness) {
                opcode = Opcode.SET_LUM
                params = byteArrayOf(progress.toByte())

                if (progress >= 100) {
                    sbBrightness_add.isEnabled = false
                    sbBrightness_less.isEnabled = true
                } else if (progress <= 0) {
                    sbBrightness_less.isEnabled = false
                    sbBrightness_add.isEnabled = true
                } else {
                    sbBrightness_less.isEnabled = true
                    sbBrightness_add.isEnabled = true
                }

                colorNode!!.brightness = progress
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params, immediate)
            } else if (view == sb_w_bright) {
                opcode = Opcode.SET_W_LUM
                params = byteArrayOf(progress.toByte())
                var color = colorNode!!.rgbw

                if (progress >= 100) {
                    sb_w_bright_add.isEnabled = false
                    sb_w_bright_less.isEnabled = true
                } else if (progress <= 0) {
                    sb_w_bright_less.isEnabled = false
                    sb_w_bright_add.isEnabled = true
                } else {
                    sb_w_bright_add.isEnabled = true
                    sb_w_bright_less.isEnabled = true
                }

                val red = (color!! and 0xff0000) shr 16
                val green = (color and 0x00ff00) shr 8
                val blue = color and 0x0000ff
                val w = progress

                colorNode?.rgbw = (w shl 24) or (red shl 16) or (green shl 8) or blue
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params, immediate)
            }
        }
    }

    private fun lessBrightness(event: MotionEvent?) {
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
                handler_brightness_less.sendMessage(msg)
            }
        } else if (event.action == MotionEvent.ACTION_CANCEL) {
            onBtnTouch = false
        }
    }

    private fun addBrightness(event: MotionEvent?) {
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
                handler_brightness_add.sendMessage(msg)
            }
        } else if (event.action == MotionEvent.ACTION_CANCEL) {
            onBtnTouch = false
        }
    }

    private fun lessWhiteBright(event: MotionEvent?) {
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
                            val msg = handler_less.obtainMessage()
                            msg.arg1 = tvValue
                            handler_less.sendMessage(msg)
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
                val msg = handler_less.obtainMessage()
                msg.arg1 = tvValue
                handler_less.sendMessage(msg)
            }
        } else if (event.action == MotionEvent.ACTION_CANCEL) {
            onBtnTouch = false
        }
    }

    private fun addWhiteBright(event: MotionEvent?) {
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
                            val msg = handler.obtainMessage()
                            msg.arg1 = tvValue
                            handler.sendMessage(msg)
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
                val msg = handler.obtainMessage()
                msg.arg1 = tvValue
                handler.sendMessage(msg)
            }
        } else if (event.action == MotionEvent.ACTION_CANCEL) {
            onBtnTouch = false
        }
    }

    @SuppressLint("HandlerLeak")
    private val handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            sb_w_bright.progress++
            if (sb_w_bright.progress > 100) {
                sb_w_bright_add.isEnabled = false
                stopTracking = false
                onBtnTouch = false
            } else if (sb_w_bright.progress == 100) {
                sb_w_bright_add.isEnabled = false
                sb_w_bright_num.text = sb_w_bright.progress.toString() + "%"
                stopTracking = false
                onBtnTouch = false
            } else {
                sb_w_bright_add.isEnabled = true
                stopTracking = true
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val handler_brightness_add = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            sbBrightness.progress++
            if (sbBrightness.progress > 100) {
                sbBrightness_add.isEnabled = false
                stopTracking = false
                onBtnTouch = false
            } else if (sbBrightness.progress == 100) {
                sbBrightness_add.isEnabled = false
                sbBrightness_num.text = sbBrightness.progress.toString() + "%"
                stopTracking = false
                onBtnTouch = false
            } else {
                sbBrightness_add.isEnabled = true
                stopTracking = true
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val handler_brightness_less = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            sbBrightness.progress--
            if (sbBrightness.progress < 0) {
                sbBrightness_less.isEnabled = false
                stopTracking = false
                onBtnTouch = false
            } else if (sbBrightness.progress == 0) {
                sbBrightness_less.isEnabled = false
                sbBrightness_num.text = sbBrightness.progress.toString() + "%"
                stopTracking = false
                onBtnTouch = false
            } else {
                sbBrightness_less.isEnabled = true
                stopTracking = true
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val handler_less = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            sb_w_bright.progress--
            if (sb_w_bright.progress < 0) {
                sb_w_bright_less.isEnabled = false
                stopTracking = false
                onBtnTouch = false
            } else if (sb_w_bright.progress == 0) {
                sb_w_bright_less.isEnabled = false
                sb_w_bright_num.text = sb_w_bright.progress.toString() + "%"
                stopTracking = false
                onBtnTouch = false
            } else {
                sb_w_bright_less.isEnabled = true
                stopTracking = true
            }
        }
    }
}
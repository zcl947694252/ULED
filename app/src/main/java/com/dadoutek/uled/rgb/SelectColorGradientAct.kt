package com.dadoutek.uled.rgb

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import com.blankj.utilcode.util.LogUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbColorNode
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.rgb.ColorSceneSelectDiyRecyclerViewAdapter
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.OtherUtils
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.android.synthetic.main.activity_select_color_gradient.*
import kotlinx.android.synthetic.main.toolbar.*
import top.defaults.colorpicker.ColorObserver
import java.util.ArrayList
import javax.xml.transform.Result

class SelectColorGradientAct:TelinkBaseActivity(),View.OnClickListener {
    private var colorNode:DbColorNode?=null
    private var stopTracking = false
    
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

        color_picker.reset()
        color_picker.subscribe(colorObserver)
        color_picker!!.setOnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }

        sbBrightness.setOnSeekBarChangeListener(barChangeListener)
        sb_w_bright.setOnSeekBarChangeListener(barChangeListener)
        if(colorNode!!.rgbw==-1){
            sbBrightness.progress=50
            sb_w_bright.progress=50
            tv_brightness_rgb.text = getString(R.string.device_setting_brightness,50)
            tv_brightness_w.text = getString(R.string.w_bright,50)
        }else{

            var w = ((colorNode?.rgbw ?: 0) and 0xff000000.toInt()) shr 24
            var r=Color.red(colorNode?.rgbw!!)
            var g=Color.green(colorNode?.rgbw!!)
            var b=Color.blue(colorNode?.rgbw!!)
            color_picker.setInitialColor((colorNode?.rgbw?:0 and 0xffffff) or 0xff000000.toInt())

            sbBrightness.progress=colorNode!!.brightness
            sb_w_bright.progress=w
            tv_brightness_rgb.text = getString(R.string.device_setting_brightness,colorNode!!.brightness)
            tv_brightness_w.text = getString(R.string.w_bright,w)

            color_r.text = r.toString()
            color_g.text = g.toString()
            color_b.text = b.toString()
        }

        btn_save.setOnClickListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> doFinish()
        }
        return super.onOptionsItemSelected(item)
    }
    
    override fun onClick(v: View?) {
       when(v?.id){
           R.id.btn_save->{
               doFinish()
           }
       }
    }
    
    private fun doFinish(){
        var intent= Intent()
        intent.putExtra("color",colorNode)
        setResult(Activity.RESULT_OK,intent)
        finish()
    }

    private val colorObserver = ColorObserver { color, fromUser ->
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)

        val w = sb_w_bright.progress

        val color: Int = (w shl 24) or (r shl 16) or (g shl 8) or b
//        val color =
        Log.d("", "onColorSelected: " + Integer.toHexString(color))
        if (fromUser) {

            color_r?.text = r.toString()
            color_g?.text = g.toString()
            color_b?.text = b.toString()

//            scrollView?.setBackgroundColor(0xff000000.toInt() or color)
            if (r == 0 && g == 0 && b == 0) {
            } else {
                Thread {
                    colorNode!!.rgbw=color

                    changeColor(r.toByte(), g.toByte(), b.toByte(), false)

                }.start()
            }
        }
    }

    private fun changeColor(R: Byte, G: Byte, B: Byte, isOnceSet: Boolean) {

        var red = R
        var green = G
        var blue = B

        val opcode = Opcode.SET_TEMPERATURE


        val params = byteArrayOf(0x04, red, green, blue)

        val logStr = String.format("R = %x, G = %x, B = %x", red, green, blue)
        Log.d("RGBCOLOR", logStr)

        if (isOnceSet) {
            for(i in 0..3){
            Thread.sleep(50)
            TelinkLightService.Instance().sendCommandNoResponse(opcode, colorNode!!.dstAddress, params)
            }
        } else {
            TelinkLightService.Instance().sendCommandNoResponse(opcode, colorNode!!.dstAddress, params)
        }
    }

    private val barChangeListener = object : SeekBar.OnSeekBarChangeListener {

        private var preTime: Long = 0
        private val delayTime = 100

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            stopTracking = true
            this.onValueChange(seekBar, seekBar.progress, true)
            LogUtils.d("seekBarstop" + seekBar.progress)
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

        private fun onValueChangeView(view: View, progress: Int, immediate: Boolean){
            if (view === sbBrightness) {
                tv_brightness_rgb.text = getString(R.string.device_setting_brightness, progress.toString() + "")
            } else if (view === sb_w_bright) {
                tv_brightness_w.text = getString(R.string.w_bright, progress.toString() + "")
            }
        }

        private fun onValueChange(view: View, progress: Int, immediate: Boolean) {

            var addr = colorNode!!.dstAddress

            val opcode: Byte
            val params: ByteArray

            if (view == sbBrightness) {
                opcode = Opcode.SET_LUM
                params = byteArrayOf(progress.toByte())

                colorNode!!.brightness = progress
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params, immediate)
            } else if (view == sb_w_bright) {
                opcode = Opcode.SET_W_LUM
                params = byteArrayOf(progress.toByte())
                var color = colorNode!!.rgbw

                val red = (color!! and 0xff0000) shr 16
                val green = (color and 0x00ff00) shr 8
                val blue = color and 0x0000ff
                val w = progress
                
                colorNode?.rgbw = (w shl 24) or (red shl 16) or (green shl 8) or blue
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params, immediate)
            }
        }
    }
}
package com.dadoutek.uled.othersview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
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
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.rgb.ColorSceneSelectDiyRecyclerViewAdapter
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.OtherUtils
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.android.synthetic.main.activity_select_color.*
import kotlinx.android.synthetic.main.toolbar.*
import java.util.ArrayList
import javax.xml.transform.Result

class SelectColorAct:TelinkBaseActivity(),View.OnClickListener {
    private var itemGroup:ItemGroup?=null
    private var presetColors: MutableList<ItemColorPreset>? = null
    private var colorSelectDiyRecyclerViewAdapter: ColorSceneSelectDiyRecyclerViewAdapter? = null
    private var stopTracking = false
    private var wValue=0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_color)
        initData()
        initView()
    }

    private fun initData() {
        itemGroup = intent.getSerializableExtra(Constant.GROUPS_KEY) as? ItemGroup
    }
    
    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        toolbar.title = getString(R.string.color_checked_set)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        
        color_picker!!.setColorListener(colorEnvelopeListener)
        color_picker!!.setOnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }

        presetColors = SharedPreferencesHelper.getObject(this, Constant.PRESET_COLOR) as? MutableList<ItemColorPreset>
        if (presetColors == null) {
            presetColors = ArrayList()
            for (i in 0..4) {
                val itemColorPreset = ItemColorPreset()
                itemColorPreset.color = OtherUtils.getCreateInitColor(i)
                presetColors!!.add(itemColorPreset)
            }
        }

        diy_color_recycler_list_view!!.layoutManager = GridLayoutManager(this, 5)
        colorSelectDiyRecyclerViewAdapter = ColorSceneSelectDiyRecyclerViewAdapter(R.layout.color_select_diy_item, presetColors)
        colorSelectDiyRecyclerViewAdapter!!.onItemChildClickListener = diyOnItemChildClickListener
        colorSelectDiyRecyclerViewAdapter!!.onItemChildLongClickListener = diyOnItemChildLongClickListener
        colorSelectDiyRecyclerViewAdapter!!.bindToRecyclerView(diy_color_recycler_list_view)

        var w = ((itemGroup?.color ?: 0) and 0xff000000.toInt()) shr 24
        if(w==-1){
            w=0
        }

        wValue=w
        tv_brightness_w.text = getString(R.string.w_bright, w.toString() + "")
        sb_w_bright.progress = w

        sb_w_bright.setOnSeekBarChangeListener(this.barChangeListener)
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
        intent.putExtra("color",itemGroup!!.color)
        setResult(Activity.RESULT_OK,intent)
        finish()
    }

    private val colorEnvelopeListener = ColorEnvelopeListener { envelope, fromUser ->
        val argb = envelope.argb


        color_r?.text = argb[1].toString()
        color_g?.text = argb[2].toString()
        color_b?.text = argb[3].toString()
        val w = sb_w_bright.progress

        val color: Int = (w shl 24) or (argb[1] shl 16) or (argb[2] shl 8) or argb[3]
//        val color =
        Log.d("", "onColorSelected: " + Integer.toHexString(color))
        if (fromUser) {
//            scrollView?.setBackgroundColor(0xff000000.toInt() or color)
            if (argb[1] == 0 && argb[2] == 0 && argb[3] == 0) {
            } else {
                Thread {
                    itemGroup!!.color=color

                    changeColor(argb[1].toByte(), argb[2].toByte(), argb[3].toByte(), false)

                }.start()
            }
        }
    }

    private fun changeColor(R: Byte, G: Byte, B: Byte, isOnceSet: Boolean) {

        var red = R
        var green = G
        var blue = B

        val opcode = Opcode.SET_TEMPERATURE

        val minVal = 0x50

        if (green.toInt() and 0xff <= minVal)
            green = 0
        if (red.toInt() and 0xff <= minVal)
            red = 0
        if (blue.toInt() and 0xff <= minVal)
            blue = 0

        val params = byteArrayOf(0x04, red, green, blue)

        val logStr = String.format("R = %x, G = %x, B = %x", red, green, blue)
        Log.d("RGBCOLOR", logStr)

        if (isOnceSet) {
//            for(i in 0..3){
            Thread.sleep(50)
            TelinkLightService.Instance().sendCommandNoResponse(opcode, itemGroup!!.groupAress, params)
//            }
        } else {
            TelinkLightService.Instance().sendCommandNoResponse(opcode, itemGroup!!.groupAress, params)
        }
    }

    internal var diyOnItemChildClickListener: BaseQuickAdapter.OnItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        val color = presetColors?.get(position)?.color
        val brightness = presetColors?.get(position)?.brightness
        val red = (color!! and 0xff0000) shr 16
        val green = (color and 0x00ff00) shr 8
        val blue = color and 0x0000ff
//        Thread {
        changeColor(red.toByte(), green.toByte(), blue.toByte(), true)

        try {
            Thread.sleep(100)

            val opcode: Byte = Opcode.SET_LUM
            val params: ByteArray = byteArrayOf(brightness!!.toByte())
            itemGroup!!.color=(wValue shl 24) or (red shl 16) or (green shl 8) or blue
            
            Thread.sleep(50)
//            TelinkLightService.Instance().sendCommandNoResponse(opcode, itemGroup!!.groupAress, params)

        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        
//        tv_brightness_w.text = getString(R.string.w_bright, w.toString() + "")
//        scrollView?.setBackgroundColor(color)
        color_r?.text = red.toString()
        color_g?.text = green.toString()
        color_b?.text = blue.toString()
    }

    @SuppressLint("SetTextI18n")
    internal var diyOnItemChildLongClickListener: BaseQuickAdapter.OnItemChildLongClickListener = BaseQuickAdapter.OnItemChildLongClickListener { adapter, view, position ->
            presetColors?.get(position)!!.color = itemGroup!!.color
        val textView = adapter.getViewByPosition(position, R.id.btn_diy_preset) as TextView?
        textView?.setBackgroundColor(0xff000000.toInt() or itemGroup!!.color)
        textView?.text = ""
        SharedPreferencesHelper.putObject(this, Constant.PRESET_COLOR, presetColors)
        false
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
            val currentTime = System.currentTimeMillis()

            if (currentTime - this.preTime > this.delayTime) {
                this.onValueChange(seekBar, progress, true)
                this.preTime = currentTime
            }
        }

        private fun onValueChange(view: View, progress: Int, immediate: Boolean) {

            var addr = itemGroup!!.groupAress

            val opcode: Byte
            val params: ByteArray

            if (view == sb_w_bright) {
                opcode = Opcode.SET_W_LUM
                params = byteArrayOf(progress.toByte())
                var color = itemGroup!!.color

                val red = (color!! and 0xff0000) shr 16
                val green = (color and 0x00ff00) shr 8
                val blue = color and 0x0000ff
                val w = progress
                wValue=w

                itemGroup?.color = (w shl 24) or (red shl 16) or (green shl 8) or blue

                tv_brightness_w.text = getString(R.string.w_bright, progress.toString() + "")
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params, immediate)
            }
        }
    }
}
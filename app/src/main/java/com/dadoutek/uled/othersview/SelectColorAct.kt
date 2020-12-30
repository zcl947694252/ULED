package com.dadoutek.uled.othersview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.util.Log
import android.view.*
import android.widget.SeekBar
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.rgb.ColorSceneSelectDiyRecyclerViewAdapter
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.Dot
import com.dadoutek.uled.util.InputRGBColorDialog
import com.dadoutek.uled.util.OtherUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import io.reactivex.Observable
import kotlinx.android.synthetic.main.activity_select_color.*
import kotlinx.android.synthetic.main.activity_select_color.color_b
import kotlinx.android.synthetic.main.activity_select_color.color_g
import kotlinx.android.synthetic.main.activity_select_color.color_picker
import kotlinx.android.synthetic.main.activity_select_color.color_r
import kotlinx.android.synthetic.main.activity_select_color.diy_color_recycler_list_view
import kotlinx.android.synthetic.main.activity_select_color.ll_b
import kotlinx.android.synthetic.main.activity_select_color.ll_g
import kotlinx.android.synthetic.main.activity_select_color.ll_r
import kotlinx.android.synthetic.main.activity_select_color.rgb_white_seekbar
import kotlinx.android.synthetic.main.activity_select_color.tv_brightness_w
import kotlinx.coroutines.*
import top.defaults.colorpicker.ColorObserver
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 描述	    场景选择颜色弹框
 *
 * 更新者     zcl
 * 更新时间
 * 更新描述   ${为类添加标识}$
 */

class SelectColorAct : TelinkBaseActivity(), View.OnClickListener {
    private var thisTime: Long = 0
    private var color: Int = 0
    private var itemGroup: ItemGroup? = null
    private var presetColors: MutableList<ItemColorPreset>? = null
    private var colorSelectDiyRecyclerViewAdapter: ColorSceneSelectDiyRecyclerViewAdapter? = null
    private var stopTracking = false
    private var wValue = 0
    private var num = 0

    private var redColor: Int? = null
    private var greenColor: Int? = null
    private var blueColor: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_color)
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.BOTTOM)
        initData()
        initView()
    }

    private fun initData() {
        itemGroup = intent.getSerializableExtra(Constant.GROUPS_KEY) as? ItemGroup
        num = intent.getIntExtra("circle", 0)
    }

    @SuppressLint("ClickableViewAccessibility", "StringFormatInvalid")
    private fun initView() {
        color_picker.reset()
        color_picker.subscribe(colorObserver)
        color_picker!!.setOnTouchListener { v, _ ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }

        when {
            Constant.IS_OPEN_AUXFUN -> {
                ll_r.visibility = View.VISIBLE
                ll_b.visibility = View.VISIBLE
                ll_g.visibility = View.VISIBLE
            }
            else -> {
                ll_r.visibility = View.GONE
                ll_b.visibility = View.GONE
                ll_g.visibility = View.GONE
            }
        }

        ll_r.setOnClickListener(this)
        ll_b.setOnClickListener(this)
        ll_g.setOnClickListener(this)

        presetColors = SharedPreferencesHelper.getObject(this, Constant.PRESET_COLOR) as? MutableList<ItemColorPreset>
        if (presetColors == null) {
            presetColors = ArrayList()
            for (i in 0..4) {
                val itemColorPreset = ItemColorPreset()
                itemColorPreset.color = OtherUtils.getCreateInitColor(i)
                presetColors!!.add(itemColorPreset)
            }
        }

        diy_color_recycler_list_view!!.layoutManager = GridLayoutManager(this, 4)
        colorSelectDiyRecyclerViewAdapter = ColorSceneSelectDiyRecyclerViewAdapter(R.layout.color_select_diy_item, presetColors)
        colorSelectDiyRecyclerViewAdapter!!.onItemChildClickListener = diyOnItemChildClickListener
        colorSelectDiyRecyclerViewAdapter!!.onItemChildLongClickListener = diyOnItemChildLongClickListener
        colorSelectDiyRecyclerViewAdapter!!.bindToRecyclerView(diy_color_recycler_list_view)

        var w = ((itemGroup?.color ?: 0) and 0xff000000.toInt()) shr 24
        var r = Color.red(itemGroup?.color!!)
        var g = Color.green(itemGroup?.color!!)
        var b = Color.blue(itemGroup?.color!!)
        color_picker.setInitialColor((itemGroup?.color ?: 0 and 0xffffff) or 0xff000000.toInt())
        if (w == -1) {
            w = 0
        }

        color_r.text = r.toString()
        color_g.text = g.toString()
        color_b.text = b.toString()

        wValue = w
        tv_brightness_w.text = getString(R.string.w_bright, w.toString() + "")
        rgb_white_seekbar.progress = w


        rgb_white_seekbar.setOnSeekBarChangeListener(this.barChangeListener)
        btn_save.setOnClickListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> doFinish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_save -> {
                doFinish()
            }

            R.id.ll_r -> {
                var dialog = InputRGBColorDialog(this, R.style.Dialog, color_r.text.toString(), color_g.text.toString(), color_b.text.toString(), InputRGBColorDialog.RGBColorListener { red, green, blue ->
                    redColor = red.toInt()
                    greenColor = green.toInt()
                    blueColor = blue.toInt()

                    setColorPicker()

                })
                dialog.show()
                dialog.window!!.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            }

            R.id.ll_g -> {
                var dialog = InputRGBColorDialog(this, R.style.Dialog, color_r.text.toString(), color_g.text.toString(), color_b.text.toString(), InputRGBColorDialog.RGBColorListener { red, green, blue ->
                    redColor = red.toInt()
                    greenColor = green.toInt()
                    blueColor = blue.toInt()

                    setColorPicker()

                })
                dialog.show()
                dialog.window!!.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            }

            R.id.ll_b -> {
                var dialog = InputRGBColorDialog(this, R.style.Dialog, color_r.text.toString(), color_g.text.toString(), color_b.text.toString(), InputRGBColorDialog.RGBColorListener { red, green, blue ->
                    redColor = red.toInt()
                    greenColor = green.toInt()
                    blueColor = blue.toInt()

                    setColorPicker()

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
//
        val color: Int = (w shl 24) or (r shl 16) or (g shl 8) or b
        var ws = (color!! and 0xff000000.toInt()) shr 24
        val red = (color!! and 0xff0000) shr 16
        val green = (color and 0x00ff00) shr 8
        val blue = color and 0x0000ff

        color_picker.setInitialColor((color and 0xffffff) or 0xff000000.toInt())

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (w!! > Constant.MAX_VALUE)
                    w = Constant.MAX_VALUE
                if (ws > Constant.MAX_VALUE)
                    ws = Constant.MAX_VALUE
                if (ws == -1)
                    ws = 0
                delay(80)
                changeColor(red, green, blue, true)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        itemGroup!!.color = color

        color_r?.text = red.toString()
        color_g?.text = green.toString()
        color_b?.text = blue.toString()
    }

    private fun doFinish() {
        var intent = Intent()
        intent.putExtra("color", itemGroup!!.color)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private val colorObserver = ColorObserver { color, fromUser ->
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)

        color_r?.text = r.toString()
        color_g?.text = g.toString()
        color_b?.text = b.toString()
        val w = rgb_white_seekbar.progress

        val color: Int = (r shl 16) or (g shl 8) or b
        Log.d("", "onColorSelected: " + Integer.toHexString(color))
        if (fromUser) {
            if (r != 0 || g != 0 || b != 0) {
                Thread {
                    itemGroup!!.color = color
                    changeColor(r, g, b, false)
                }.start()
            }
        }
    }

    private fun changeColor(r: Int, G: Int, B: Int, isOnceSet: Boolean) {
        thisTime = System.currentTimeMillis()

        var red = r.toByte()
        var green = G.toByte()
        var blue = B.toByte()
        val opcode = Opcode.SET_TEMPERATURE  //0x04 代表rgb
        val params = byteArrayOf(0x04, red, green, blue)
        Log.d("RGBCOLOR", String.format("R = %x, G = %x, B = %x", red, green, blue))

        var white =( itemGroup!!.color and 0xff000000.toInt()) shr 24
        color = (white shl 24) or (r shl 16) or (G shl 8) or B

        if (thisTime - lastTime >= 200) {
            when {
                Constant.IS_ROUTE_MODE//路由发送色盘之不用发送白光 亮度 色温等 白光在color内已经存在
                -> routerConfigRGBNum(itemGroup!!.groupAddress, 97, color)
                else -> {
                    if (isOnceSet) {
                        Thread.sleep(50)
                        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, itemGroup!!.groupAddress, params)
                    } else {
                        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, itemGroup!!.groupAddress, params)
                    }
                }
            }
            lastTime = thisTime
        }
    }


    @SuppressLint("CheckResult")
    private fun routerConfigRGBNum(meshAddr: Int, deviceType: Int, color: Int) {
        RouterModel.routeConfigRGBNum(meshAddr, deviceType, color, "setRGB")?.subscribe({
            LogUtils.v("zcl-----------收到路由调节色盘成功-------")
            when (it.errorCode) {
                0 -> {
                    // showLoadingDialog(getString(R.string.please_wait))
                    disposableRouteTimer?.dispose()
                    disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                            .subscribe {
                                hideLoadingDialog()
                                ToastUtils.showShort(getString(R.string.congfig_rgb_fail))
                            }
                }
                90020 -> ToastUtils.showShort(getString(R.string.gradient_not_exit))
                90018 -> {
                    DBUtils.deleteLocalData()
                   // ToastUtils.showShort(getString(R.string.device_not_exit))
                    SyncDataPutOrGetUtils.syncGetDataStart(DBUtils.lastUser!!, syncCallbackGet)
                    finish()
                }
                  90008 -> {hideLoadingDialog()
                ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))}
                90007 -> ToastUtils.showShort(getString(R.string.gp_not_exit))
                90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                90004 -> ToastUtils.showShort(getString(R.string.region_no_router))
                else -> ToastUtils.showShort(it.message)
            }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    override fun tzRouterConfigRGB(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl------收到路由调节色盘通知------------$cmdBean")

        hideLoadingDialog()
        if (cmdBean.ser_id == "setRGB") {
            disposableRouteTimer?.dispose()
            if (cmdBean.status == 0) {
                color_picker.setInitialColor((color and 0xffffff) or 0xff000000.toInt())
                itemGroup?.color = color
            } else {
                color_picker.setInitialColor((itemGroup!!.color and 0xffffff) or 0xff000000.toInt())
                ToastUtils.showShort(getString(R.string.congfig_rgb_fail))
            }
        }
    }

    private var diyOnItemChildClickListener: BaseQuickAdapter.OnItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        val color = presetColors?.get(position)?.color
        val brightness = presetColors?.get(position)?.brightness
        val red = (color!! and 0xff0000) shr 16
        val green = (color and 0x00ff00) shr 8
        val blue = color and 0x0000ff

        color_picker.setInitialColor((color and 0xffffff) or 0xff000000.toInt())
        changeColor(red, green, blue, true)
        try {
            Thread.sleep(100)
            itemGroup!!.color = (wValue shl 24) or (red shl 16) or (green shl 8) or blue
            Thread.sleep(50)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        color_r?.text = red.toString()
        color_g?.text = green.toString()
        color_b?.text = blue.toString()
    }

    @SuppressLint("SetTextI18n")
    internal var diyOnItemChildLongClickListener: BaseQuickAdapter.OnItemChildLongClickListener = BaseQuickAdapter.OnItemChildLongClickListener { adapter, view, position ->
        presetColors?.get(position)!!.color = itemGroup!!.color
        val textView = adapter.getViewByPosition(position, R.id.btn_diy_preset) as Dot?
        textView?.setChecked(true, 0xff000000.toInt() or itemGroup!!.color)
        Log.e("TAG_COLOR", itemGroup!!.color.toString())
        SharedPreferencesHelper.putObject(this, Constant.PRESET_COLOR, presetColors)
        false
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

        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - this.preTime > this.delayTime) {
                this.onValueChange(seekBar, progress, true)
                this.preTime = currentTime
            }
        }

        @SuppressLint("StringFormatInvalid")
        private fun onValueChange(view: View, progress: Int, immediate: Boolean) {
            var addr = itemGroup!!.groupAddress
            val opcode: Byte
            val params: ByteArray

            if (view == rgb_white_seekbar) {
                opcode = Opcode.SET_W_LUM
                params = byteArrayOf(progress.toByte())
                var color = itemGroup!!.color

                val red = (color!! and 0xff0000) shr 16
                val green = (color and 0x00ff00) shr 8
                val blue = color and 0x0000ff
                val w = progress

                wValue = w
                itemGroup?.color = (w shl 24) or (red shl 16) or (green shl 8) or blue

                tv_brightness_w.text = getString(R.string.w_bright, progress.toString() + "")
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params, immediate)
            }
        }
    }
}
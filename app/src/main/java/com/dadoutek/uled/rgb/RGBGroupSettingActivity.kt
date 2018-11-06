package com.dadoutek.uled.rgb

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter

import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.group.NormalGroupSettingActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.ItemColorPreset
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.OtherUtils
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.android.synthetic.main.activity_group_setting.*
import kotlinx.android.synthetic.main.fragment_rgb_group_setting.*
import java.util.*
import kotlin.experimental.and

class RGBGroupSettingActivity : TelinkBaseActivity(), OnClickListener, TextView.OnEditorActionListener {
    private var backView: ImageView? = null
    private var mApplication: TelinkLightApplication? = null
    private var brightnessBar: SeekBar? = null
    private var temperatureBar: SeekBar? = null
    private var colorR: TextView? = null
    private var colorG: TextView? = null
    private var colorB: TextView? = null
    private var scrollView: ScrollView? = null
    private var diyColorRecyclerListView: RecyclerView? = null
    private var colorPicker: ColorPickerView? = null
    private var btn_remove_group: Button? = null
    private var btn_rename: Button? = null
    private var stopTracking = false
    private var presetColors: MutableList<ItemColorPreset>? = null
    private var colorSelectDiyRecyclerViewAdapter: ColorSelectDiyRecyclerViewAdapter? = null
    private var group: DbGroup? = null
    private var editTitle: EditText? =null

    private val clickListener = OnClickListener { v ->
        if (v === backView) {
            setResult(Constant.RESULT_OK)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rgb_group_setting)

        initData()
        initView()
    }

    private fun initData() {
        this.mApplication = this.application as TelinkLightApplication
        this.group = this.intent.extras!!.get("group") as DbGroup
    }

    private fun initView() {
        if (group != null) {
            editTitle = this.findViewById<View>(R.id.edit_title) as EditText
            if (group!!.meshAddr == 0xffff) {
                editTitle!!.setText(getString(R.string.allLight))
            } else {
                editTitle!!.setText(group!!.name)
            }
        }

        this.backView = this
                .findViewById<View>(R.id.img_header_menu_left) as ImageView
        this.backView!!.setOnClickListener(this.clickListener)

        this.brightnessBar = findViewById<View>(R.id.sb_brightness) as SeekBar
        this.temperatureBar = findViewById<View>(R.id.sb_temperature) as SeekBar
        this.colorR = findViewById<View>(R.id.color_r) as TextView
        this.colorG = findViewById<View>(R.id.color_g) as TextView
        this.colorB = findViewById<View>(R.id.color_b) as TextView
        this.scrollView = findViewById<View>(R.id.scroll_view) as ScrollView
        this.diyColorRecyclerListView = findViewById<View>(R.id.diy_color_recycler_list_view) as RecyclerView

        this.colorPicker = findViewById<View>(R.id.color_picker) as ColorPickerView
        this.colorPicker!!.setOnTouchListener { v, _ ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }
        btn_rename = findViewById<Button>(R.id.btn_rename)
        btn_remove_group = findViewById<Button>(R.id.btn_remove_group)

        btn_remove_group?.setOnClickListener(this)
        btn_rename?.setOnClickListener(this)

        presetColors = SharedPreferencesHelper.getObject(this, Constant.PRESET_COLOR) as? MutableList<ItemColorPreset>
        if (presetColors == null) {
            presetColors = java.util.ArrayList<ItemColorPreset>()
            for (i in 0..4) {
                val itemColorPreset = ItemColorPreset()
                itemColorPreset.color=OtherUtils.getCreateInitColor(i)
                presetColors?.add(itemColorPreset)
            }
        }

        diyColorRecyclerListView?.layoutManager = GridLayoutManager(this,5)
        colorSelectDiyRecyclerViewAdapter = ColorSelectDiyRecyclerViewAdapter(R.layout.color_select_diy_item, presetColors)
        colorSelectDiyRecyclerViewAdapter?.onItemChildClickListener = diyOnItemChildClickListener
        colorSelectDiyRecyclerViewAdapter?.onItemChildLongClickListener = diyOnItemChildLongClickListener
        colorSelectDiyRecyclerViewAdapter?.bindToRecyclerView(diyColorRecyclerListView)

        brightnessBar!!.progress = group!!.brightness
        tv_brightness_rgb.text = getString(R.string.device_setting_brightness, group!!.brightness.toString() + "")
        temperatureBar!!.progress = group!!.colorTemperature
        tv_temperature_rgb!!.text = getString(R.string.device_setting_temperature, group!!.colorTemperature.toString() + "")


        this.brightnessBar!!.setOnSeekBarChangeListener(this.barChangeListener)
        this.temperatureBar!!.setOnSeekBarChangeListener(this.barChangeListener)
        this.colorPicker?.setColorListener(colorEnvelopeListener)
        checkGroupIsSystemGroup()
    }

    internal var diyOnItemChildClickListener: BaseQuickAdapter.OnItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        val color = presetColors?.get(position)?.color
        val brightness = presetColors?.get(position)?.brightness
        val red = (color!! and 0xff0000) shr 16
        val green = (color and 0x00ff00) shr 8
        val blue = color and 0x0000ff
        Thread {
            changeColor(red.toByte(), green.toByte(), blue.toByte())

            try {
                Thread.sleep(200)
                val addr = group?.meshAddr
                val opcode: Byte = Opcode.SET_LUM
                val params: ByteArray = byteArrayOf(brightness!!.toByte())
                group?.brightness = brightness
                group?.color = color
                TelinkLightService.Instance().sendCommandNoResponse(opcode, addr!!, params)
                DBUtils.updateGroup(group!!)
                updateLights(color, "rgb_color", group!!)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }.start()

        brightnessBar?.progress = brightness!!
//        scrollView?.setBackgroundColor(color)
        colorR?.text = red.toString()
        colorG?.text = green.toString()
        colorB?.text = blue.toString()
    }

    @SuppressLint("SetTextI18n")
    internal var diyOnItemChildLongClickListener: BaseQuickAdapter.OnItemChildLongClickListener = BaseQuickAdapter.OnItemChildLongClickListener { adapter, view, position ->
        presetColors?.get(position)!!.color = group!!.color
        presetColors?.get(position)!!.brightness = group!!.brightness
        val textView = adapter.getViewByPosition(position, R.id.btn_diy_preset) as TextView?
        textView!!.text = group!!.brightness.toString() + "%"
        textView.setBackgroundColor(0xff000000.toInt() or group!!.color)
        SharedPreferencesHelper.putObject(this, Constant.PRESET_COLOR, presetColors)
        false
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_remove_group -> AlertDialog.Builder(Objects.requireNonNull<FragmentActivity>(this)).setMessage(R.string.delete_group_confirm)
                    .setPositiveButton(R.string.btn_ok) { _, _ ->
                        this.showLoadingDialog(getString(R.string.deleting))

                        deleteGroup(DBUtils.getLightByGroupID(group!!.id), group!!,
                                successCallback = {
                                    this.hideLoadingDialog()
                                    this.setResult(Constant.RESULT_OK)
                                    this.finish()
                                },
                                failedCallback = {
                                    this.hideLoadingDialog()
                                    ToastUtils.showShort(R.string.move_out_some_lights_in_group_failed)
                                })
                    }
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show()
            R.id.btn_rename -> renameGp()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            setResult(Constant.RESULT_OK)
            finish()
            return false
        } else {
            return super.onKeyDown(keyCode, event)
        }

    }

    private val barChangeListener = object : SeekBar.OnSeekBarChangeListener {

        private var preTime: Long = 0
        private val delayTime = 20

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

            if (currentTime - this.preTime < this.delayTime) {
                this.preTime = currentTime
                return
            }

            LogUtils.d("seekBarChange" + seekBar.progress)
            this.onValueChange(seekBar, progress, true)
        }

        private fun onValueChange(view: View, progress: Int, immediate: Boolean) {

            val addr = group!!.meshAddr
            val opcode: Byte
            val params: ByteArray

            if (view === brightnessBar) {
                opcode = Opcode.SET_LUM
                params = byteArrayOf(progress.toByte())
                group!!.brightness = progress
                DBUtils.updateGroup(group!!)
                tv_brightness_rgb.text = getString(R.string.device_setting_brightness, progress.toString() + "")
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params, immediate)

                if (stopTracking) {
                    updateLights(progress, "brightness", group!!)
                }
            } else if (view === temperatureBar) {

                opcode = Opcode.SET_TEMPERATURE
                params = byteArrayOf(0x05, progress.toByte())
                group!!.colorTemperature = progress
                DBUtils.updateGroup(group!!)
                tv_temperature_rgb!!.text = getString(R.string.device_setting_temperature, progress.toString() + "")
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params, immediate)

                if (stopTracking) {
                    updateLights(progress, "colorTemperature", group!!)
                }
            }
        }
    }

    private fun updateLights(progress: Int, type: String, group: DbGroup) {
        Thread {
            var lightList: MutableList<DbLight> = ArrayList()

            if (group.meshAddr == 0xffff) {
                //            lightList = DBUtils.getAllLight();
                val list = DBUtils.groupList
                for (j in list.indices) {
                    lightList.addAll(DBUtils.getLightByGroupID(list[j].id))
                }
            } else {
                lightList = DBUtils.getLightByGroupID(group.id)
            }

            for (dbLight: DbLight in lightList) {
                if (type == "brightness") {
                    dbLight.brightness = progress
                } else if (type == "colorTemperature") {
                    dbLight.colorTemperature = progress
                } else if(type == "rgb_color"){
                    dbLight.color = progress
                }
                DBUtils.updateLight(dbLight)
            }
        }.start()
    }

    private val colorEnvelopeListener = ColorEnvelopeListener { envelope, fromUser ->
        val argb = envelope.argb

        colorR?.text = argb[1].toString()
        colorG?.text = argb[2].toString()
        colorB?.text = argb[3].toString()

        val color:Int = argb[1] shl 16 or (argb[2] shl 8) or argb[3]
//        val color =
        Log.d("", "onColorSelected: " + Integer.toHexString(color))
        if (fromUser) {
//            scrollView?.setBackgroundColor(0xff000000.toInt() or color)
            if(argb[1]==0 && argb[2]==0 && argb[3]==0){
            }else{
                Thread{
                    group?.color = color
                    changeColor(argb[1].toByte(), argb[2].toByte(), argb[3].toByte())
                    DBUtils.updateGroup(group!!)
                    updateLights(color, "rgb_color", group!!)
                }.start()
            }
        }
    }

    private fun changeColor(R: Byte, G: Byte, B: Byte) {

        var red  = R
        var green = G
        var blue = B

        val addr = group?.meshAddr
        val opcode = 0xE2.toByte()

        val minVal = 0x50

        if ((green.toInt() and 0xff) <= minVal)
            green = 0
        if ((red.toInt() and 0xff) <= minVal)
            red = 0

        if ((blue.toInt() and 0xff) <= minVal)
            blue = 0


        val params = byteArrayOf(0x04, red, green, blue)

        val logStr = String.format("R = %x, G = %x, B = %x", red, green, blue)
        Log.d("RGBCOLOR", logStr)

        TelinkLightService.Instance().sendCommandNoResponse(opcode, addr!!, params)
    }

    //所有灯控分组暂标为系统默认分组不做修改处理
    private fun checkGroupIsSystemGroup() {
        if (group!!.meshAddr == 0xFFFF) {
            btn_remove_group!!.visibility = View.GONE
            btn_rename!!.visibility = View.GONE
        }
    }

    /**
     * 删除组，并且把组里的灯的组也都删除。
     */
    private fun deleteGroup(lights: MutableList<DbLight>, group: DbGroup, retryCount: Int = 0,
                            successCallback: () -> Unit, failedCallback: () -> Unit) {
        Thread {
            if (lights.count() != 0) {
                val maxRetryCount = 3
                if (retryCount <= maxRetryCount) {
                    val light = lights[0]
                    val lightMeshAddr = light.meshAddr
                    Commander.deleteGroup(lightMeshAddr,
                            successCallback = {
                                light.belongGroupId = DBUtils.groupNull!!.id
                                DBUtils.updateLight(light)
                                lights.remove(light)
                                //修改分组成功后删除场景信息。
                                deleteAllSceneByLightAddr(light.meshAddr)
                                Thread.sleep(100)
                                if (lights.count() == 0) {
                                    //所有灯都删除了分组
                                    DBUtils.deleteGroupOnly(group)
                                    this?.runOnUiThread {
                                        successCallback.invoke()
                                    }
                                } else {
                                    //还有灯要删除分组
                                    deleteGroup(lights, group,
                                            successCallback = successCallback,
                                            failedCallback = failedCallback)
                                }
                            },
                            failedCallback = {
                                deleteGroup(lights, group, retryCount = retryCount + 1,
                                        successCallback = successCallback,
                                        failedCallback = failedCallback)
                            })
                } else {    //超过了重试次数
                    this?.runOnUiThread {
                        failedCallback.invoke()
                    }
                    LogUtils.d("retry delete group timeout")
                }
            } else {
                DBUtils.deleteGroupOnly(group)
                this?.runOnUiThread {
                    successCallback.invoke()
                }
            }
        }.start()

    }

    private fun getRelatedSceneIds(groupAddress: Int): List<Long> {
        val sceneIds = java.util.ArrayList<Long>()
        val dbSceneList = DBUtils.sceneList
        sceneLoop@ for (dbScene in dbSceneList) {
            val dbActions = DBUtils.getActionsBySceneId(dbScene.id)
            for (action in dbActions) {
                if (groupAddress == action.groupAddr || 0xffff == action.groupAddr) {
                    sceneIds.add(dbScene.id)
                    continue@sceneLoop
                }
            }
        }
        return sceneIds
    }

    /**
     * 删除指定灯里的所有场景
     *
     * @param lightMeshAddr 灯的mesh地址
     */
    private fun deleteAllSceneByLightAddr(lightMeshAddr: Int) {
        val opcode = Opcode.SCENE_ADD_OR_DEL
        val params: ByteArray
        params = byteArrayOf(0x00, 0xff.toByte())
        TelinkLightService.Instance().sendCommandNoResponse(opcode, lightMeshAddr, params)
    }

    private fun renameGp() {
//        val intent = Intent(this, RenameActivity::class.java)
//        intent.putExtra("group", group)
//        startActivity(intent)
//        this?.finish()
        editTitle?.isFocusableInTouchMode = true
        editTitle?.isFocusable = true
        editTitle?.requestFocus()
        btn_sure_edit_rename.visibility=View.VISIBLE
        btn_sure_edit_rename.setOnClickListener {
            saveName()
            btn_sure_edit_rename.visibility=View.GONE
        }
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editTitle, InputMethodManager.SHOW_FORCED)
        editTitle?.setOnEditorActionListener(this)
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        doWhichOperation(actionId)
        return true
    }

    private fun doWhichOperation(actionId: Int) {
        when (actionId) {
            EditorInfo.IME_ACTION_DONE,
            EditorInfo.IME_ACTION_NONE -> {
                saveName()
                btn_sure_edit_rename.visibility=View.GONE
            }
        }
    }

    private fun saveName(){
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editTitle?.getWindowToken(), 0)
        editTitle?.setFocusableInTouchMode(false)
        editTitle?.setFocusable(false)
        checkAndSaveName()
    }

    private fun checkAndSaveName(){
        val name = editTitle?.text.toString().trim()
        if (compileExChar(name)) {
            Toast.makeText(this, R.string.rename_tip_check, Toast.LENGTH_SHORT).show()
        }else{
            group?.name=name
            DBUtils.updateGroup(group!!)
        }
    }
}

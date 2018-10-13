package com.dadoutek.uled.group

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.ItemColorPreset
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.othersview.BaseFragment
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.android.synthetic.main.fragment_group_setting.*
import kotlinx.android.synthetic.main.fragment_rgb_group_setting.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.experimental.and

class RGBGroupSettingFragment : BaseFragment(), View.OnClickListener {

    private var brightnessBar: SeekBar? = null
    private var temperatureBar: SeekBar? = null
    private var colorR: TextView? = null
    private var colorG: TextView? = null
    private var colorB: TextView? = null
    private var scrollView: ScrollView? = null
    private var diyColorRecyclerListView: RecyclerView? =null
    private var colorPicker: ColorPickerView? = null
    private var mApplication: TelinkLightApplication? = null
    private var btn_remove_group: Button? = null
    private var btn_rename: Button? = null
    var group: DbGroup? = null
    private var stopTracking = false
    private var presetColors: MutableList<ItemColorPreset>? = null
    private var colorSelectDiyRecyclerViewAdapter: ColorSelectDiyRecyclerViewAdapter? = null

    private val barChangeListener = object : OnSeekBarChangeListener {

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

            //            if (progress % 5 != 0)
            //                return;
            //
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
        Thread{
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

        val color = Color.argb(255, argb[1], argb[2], argb[3])
        if (fromUser) {
            scrollView?.setBackgroundColor(color)
            group?.setColor(color)
            changeColor(argb[1].toByte(), argb[2].toByte(), argb[3].toByte())
        }
    }

    private fun changeColor(R: Byte, G: Byte, B: Byte) {

        var red = R
        var green = G
        var blue = B

        val addr = group?.meshAddr
        val opcode = 0xE2.toByte()

        val minVal = 0x50.toByte()

        if (green and 0xff.toByte() <= minVal)
            green = 0
        if (red and 0xff.toByte() <= minVal)
            red = 0
        if (blue and 0xff.toByte() <= minVal)
            blue = 0


        val params = byteArrayOf(0x04, red, green, blue)

        val logStr = String.format("R = %x, G = %x, B = %x", red, green, blue)
        Log.d("Saw", logStr)

        TelinkLightService.Instance().sendCommandNoResponse(opcode, addr!!, params)
    }

//    private val colorChangedListener = object : ColorPicker.OnColorChangeListener {
//
//        private var preTime: Long = 0
//        private val delayTime = 20
//
//        override fun onStartTrackingTouch(view: ColorPicker) {
//            this.preTime = System.currentTimeMillis()
//            this.changeColor(view.color)
//        }
//
//        override fun onStopTrackingTouch(view: ColorPicker) {
//            this.changeColor(view.color)
//        }
//
//        override fun onColorChanged(view: ColorPicker, color: Int) {
//
//            val currentTime = System.currentTimeMillis()
//
//            if (currentTime - this.preTime >= this.delayTime) {
//                this.preTime = currentTime
//                this.changeColor(color)
//            }
//        }
//
//        private fun changeColor(color: Int) {
//
//            val red = (color shr 16 and 0xFF).toByte()
//            val green = (color shr 8 and 0xFF).toByte()
//            val blue = (color and 0xFF).toByte()
//
//            val addr = group!!.meshAddr
//            val opcode = 0xE2.toByte()
//            val params = byteArrayOf(0x04, red, green, blue)
//
//            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params)
//        }
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mApplication = activity!!.application as TelinkLightApplication


    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_rgb_group_setting, null)

        this.brightnessBar = view.findViewById<View>(R.id.sb_brightness) as SeekBar
        this.temperatureBar = view.findViewById<View>(R.id.sb_temperature) as SeekBar
        this.colorR = view.findViewById<View>(R.id.color_r) as TextView
        this.colorG = view.findViewById<View>(R.id.color_g) as TextView
        this.colorB = view.findViewById<View>(R.id.color_b) as TextView
        this.scrollView = view.findViewById<View>(R.id.scroll_view) as ScrollView
        this.diyColorRecyclerListView = view.findViewById<View>(R.id.diy_color_recycler_list_view) as RecyclerView

        this.colorPicker = view.findViewById<View>(R.id.color_picker) as ColorPickerView
        btn_rename = view.findViewById<Button>(R.id.btn_rename)
        btn_remove_group = view.findViewById<Button>(R.id.btn_remove_group)

        btn_remove_group?.setOnClickListener(this)
        btn_rename?.setOnClickListener(this)

        presetColors = SharedPreferencesHelper.getObject(activity, Constant.GROUP_PRESET_COLOR) as MutableList<ItemColorPreset>
        if (presetColors == null) {
            presetColors = java.util.ArrayList<ItemColorPreset>()
            for (i in 0..4) {
                val itemColorPreset = ItemColorPreset()
                presetColors?.add(itemColorPreset)
            }
        }

        val layoutmanager = LinearLayoutManager(activity)
        layoutmanager.orientation = LinearLayoutManager.HORIZONTAL
        diyColorRecyclerListView?.layoutManager = layoutmanager
        colorSelectDiyRecyclerViewAdapter = ColorSelectDiyRecyclerViewAdapter(R.layout.color_select_diy_item, presetColors)
        colorSelectDiyRecyclerViewAdapter?.onItemChildClickListener = diyOnItemChildClickListener
        colorSelectDiyRecyclerViewAdapter?.onItemChildLongClickListener = diyOnItemChildLongClickListener
        colorSelectDiyRecyclerViewAdapter?.bindToRecyclerView(diyColorRecyclerListView)

        return view
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
                group?.brightness = brightness!!
                TelinkLightService.Instance().sendCommandNoResponse(opcode, addr!!, params)
                DBUtils.updateGroup(group!!)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }.start()

        brightnessBar?.progress = brightness!!
        scrollView?.setBackgroundColor(color)
        colorR?.text = red.toString()
        colorG?.text = green.toString()
        colorB?.text = blue.toString()
    }

    @SuppressLint("SetTextI18n")
    internal var diyOnItemChildLongClickListener: BaseQuickAdapter.OnItemChildLongClickListener = BaseQuickAdapter.OnItemChildLongClickListener { adapter, view, position ->
        presetColors?.get(position)!!.color = group!!.getColor()
        presetColors?.get(position)!!.brightness = group!!.getBrightness()
        val textView = adapter.getViewByPosition(position, R.id.btn_diy_preset) as TextView?
        textView!!.text = group!!.brightness.toString() + "%"
        textView.setBackgroundColor(group!!.getColor())
            SharedPreferencesHelper.putObject(activity, Constant.GROUP_PRESET_COLOR, presetColors)
        false
    }

    //所有灯控分组暂标为系统默认分组不做修改处理
    private fun checkGroupIsSystemGroup() {
        if (group!!.meshAddr == 0xFFFF) {
            btn_remove_group!!.visibility = View.GONE
            btn_rename!!.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        checkGroupIsSystemGroup()
        brightnessBar!!.progress = group!!.brightness
        tv_brightness_rgb.text = getString(R.string.device_setting_brightness, group!!.brightness.toString() + "")
        temperatureBar!!.progress = group!!.colorTemperature
        tv_temperature_rgb!!.text = getString(R.string.device_setting_temperature, group!!.colorTemperature.toString() + "")


        this.brightnessBar!!.setOnSeekBarChangeListener(this.barChangeListener)
        this.temperatureBar!!.setOnSeekBarChangeListener(this.barChangeListener)
        this.colorPicker?.setColorListener(colorEnvelopeListener)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_remove_group -> AlertDialog.Builder(Objects.requireNonNull<FragmentActivity>(activity)).setMessage(R.string.delete_group_confirm)
                    .setPositiveButton(R.string.btn_ok) { _, _ ->
                        (activity as NormalGroupSettingActivity).showLoadingDialog(getString(R.string.deleting))

                        deleteGroup(DBUtils.getLightByGroupID(group!!.id), group!!,
                                successCallback = {
                                    (activity as NormalGroupSettingActivity).hideLoadingDialog()
                                    activity?.setResult(Constant.RESULT_OK)
                                    activity?.finish()
                                },
                                failedCallback = {
                                    (activity as NormalGroupSettingActivity).hideLoadingDialog()
                                    ToastUtils.showShort(R.string.move_out_some_lights_in_group_failed)
                                })
                    }
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show()
            R.id.btn_rename -> renameGp()
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
                                    activity?.runOnUiThread {
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
                    activity?.runOnUiThread {
                        failedCallback.invoke()
                    }
                    LogUtils.d("retry delete group timeout")
                }
            } else {
                DBUtils.deleteGroupOnly(group)
                activity?.runOnUiThread {
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
        val intent = Intent(activity, RenameActivity::class.java)
        intent.putExtra("group", group)
        startActivity(intent)
        activity?.finish()
    }


}

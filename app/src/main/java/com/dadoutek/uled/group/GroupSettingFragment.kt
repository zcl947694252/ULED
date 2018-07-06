package com.dadoutek.uled.group

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.widget.ColorPicker
import kotlinx.android.synthetic.main.fragment_group_setting.*
import java.util.*

class GroupSettingFragment : Fragment(), View.OnClickListener {

    private var brightnessBar: SeekBar? = null
    private var temperatureBar: SeekBar? = null
    private var colorPicker: ColorPicker? = null
    private var mApplication: TelinkLightApplication? = null
    private var btn_remove_group: Button? = null
    private var btn_rename: Button? = null
    var group: DbGroup? = null

    private val barChangeListener = object : OnSeekBarChangeListener {

        private var preTime: Long = 0
        private val delayTime = 20

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            this.onValueChange(seekBar, seekBar.progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            this.preTime = System.currentTimeMillis()
            this.onValueChange(seekBar, seekBar.progress)
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

            this.onValueChange(seekBar, progress)
        }

        private fun onValueChange(view: View, progress: Int) {

            val addr = group!!.meshAddr
            val opcode: Byte
            val params: ByteArray

            if (view === brightnessBar) {
                opcode = Opcode.SET_LUM
                params = byteArrayOf(progress.toByte())
                group!!.brightness = progress
                DBUtils.updateGroup(group)
                tv_brightness!!.text = getString(R.string.device_setting_brightness, progress.toString() + "")
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params)

            } else if (view === temperatureBar) {

                opcode = Opcode.SET_TEMPERATURE
                params = byteArrayOf(0x05, progress.toByte())
                group!!.colorTemperature = progress
                DBUtils.updateGroup(group)
                tv_temperature!!.text = getString(R.string.device_setting_temperature, progress.toString() + "")
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params)
            }
        }
    }
    private val colorChangedListener = object : ColorPicker.OnColorChangeListener {

        private var preTime: Long = 0
        private val delayTime = 20

        override fun onStartTrackingTouch(view: ColorPicker) {
            this.preTime = System.currentTimeMillis()
            this.changeColor(view.color)
        }

        override fun onStopTrackingTouch(view: ColorPicker) {
            this.changeColor(view.color)
        }

        override fun onColorChanged(view: ColorPicker, color: Int) {

            val currentTime = System.currentTimeMillis()

            if (currentTime - this.preTime >= this.delayTime) {
                this.preTime = currentTime
                this.changeColor(color)
            }
        }

        private fun changeColor(color: Int) {

            val red = (color shr 16 and 0xFF).toByte()
            val green = (color shr 8 and 0xFF).toByte()
            val blue = (color and 0xFF).toByte()

            val addr = group!!.meshAddr
            val opcode = 0xE2.toByte()
            val params = byteArrayOf(0x04, red, green, blue)

            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mApplication = activity!!.application as TelinkLightApplication


    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_group_setting, null)

        this.brightnessBar = view.findViewById<View>(R.id.sb_brightness) as SeekBar
        this.temperatureBar = view.findViewById<View>(R.id.sb_temperature) as SeekBar


        this.colorPicker = view.findViewById<View>(R.id.color_picker) as ColorPicker
        btn_rename = view.findViewById<Button>(R.id.btn_rename)
        btn_remove_group = view.findViewById<Button>(R.id.btn_remove_group)

        btn_remove_group?.setOnClickListener(this)
        btn_rename?.setOnClickListener(this)

        return view
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
        tv_brightness!!.text = getString(R.string.device_setting_brightness, group!!.brightness.toString() + "")
        temperatureBar!!.progress = group!!.colorTemperature
        tv_temperature!!.text = getString(R.string.device_setting_temperature, group!!.colorTemperature.toString() + "")


        this.brightnessBar!!.setOnSeekBarChangeListener(this.barChangeListener)
        this.temperatureBar!!.setOnSeekBarChangeListener(this.barChangeListener)
        this.colorPicker!!.setOnColorChangeListener(this.colorChangedListener)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_remove_group -> AlertDialog.Builder(Objects.requireNonNull<FragmentActivity>(activity)).setMessage(R.string.delete_group_confirm)
                    .setPositiveButton(R.string.btn_ok) { _, _ ->
                        (activity as GroupSettingActivity).showLoadingDialog(getString(R.string.deleting))

                        deleteGroup(DBUtils.getLightByGroupID(group!!.id), group!!,
                                successCallback = {
                                    (activity as GroupSettingActivity).hideLoadingDialog()
                                    activity?.setResult(Constant.RESULT_OK)
                                    activity?.finish()
                                },
                                failedCallback = {
                                    (activity as GroupSettingActivity).hideLoadingDialog()
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
                                light.belongGroupId = DBUtils.getGroupNull().id
                                DBUtils.updateLight(light)
                                lights.remove(light)
                                if (lights.count() == 0) {
                                    DBUtils.deleteGroupOnly(group)
                                    activity?.runOnUiThread {
                                        successCallback.invoke()
                                    }
                                } else
                                    deleteGroup(lights, group,
                                            successCallback = successCallback,
                                            failedCallback = failedCallback)

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

    private fun renameGp() {
        val intent = Intent(activity, RenameActivity::class.java)
        intent.putExtra("group", group)
        startActivity(intent)
        activity?.finish()
    }


}
package com.dadoutek.uled.scene

import android.view.View
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
import com.dadoutek.uled.util.OtherUtils
import java.util.*

/**
 * Created by hejiajun on 2018/5/5.
 */

class SceneGroupAdapter
(layoutResId: Int, data: List<ItemGroup>) : BaseQuickAdapter<ItemGroup, BaseViewHolder>(layoutResId, data), SeekBar.OnSeekBarChangeListener {
    private var preTime: Long = 0
    private val delayTime = Constant.MAX_SCROLL_DELAY_VALUE
    override fun convert(helper: BaseViewHolder, item: ItemGroup) {
        val position = helper.layoutPosition
        val sbBrightness = helper.getView<SeekBar>(R.id.sbBrightness)
        val sBtemperature = helper.getView<SeekBar>(R.id.sbTemperature)


//        if(DBUtils.getCurtainName(item.gpName).size==0){
            helper.setText(R.id.name_gp, item.gpName)
//        }
        helper.setBackgroundColor(R.id.rgb_view, if (item.color == 0)
            TelinkLightApplication.getInstance().resources.getColor(R.color.primary)
        else
            (0xff0000 shl 8) or (item.color and 0xffffff))
        helper.setProgress(R.id.sbBrightness, item.brightness)
        helper.setProgress(R.id.sbTemperature, item.temperature)
//        if(sbBrightness.progress.toString()!="-1" || sbBrightness.progress.toString()!="-2"){
            helper.setText(R.id.tvBrightness, sbBrightness.progress.toString() + "%")
            helper.setText(R.id.tvTemperature, sBtemperature.progress.toString() + "%")
//        }else{
//            if(sbBrightness.progress.toString()=="-1"){
//                helper.setChecked(R.id.rg_xx,true)
//            }else{
//                helper.setChecked(R.id.rg_yy,true)
//            }
//        }

        if (OtherUtils.isRGBGroup(DBUtils.getGroupByMesh(item.groupAress))) {
            helper.setGone(R.id.scene_rgb_layout, true)
            helper.setGone(R.id.sb_temperature_layout, false)
        } else  if(OtherUtils.isNormalGroup(DBUtils.getGroupByMesh(item.groupAress))){
            helper.setGone(R.id.scene_rgb_layout, false)
            helper.setGone(R.id.sb_temperature_layout, true)
        }else if(OtherUtils.isConnector(DBUtils.getGroupByMesh(item.groupAress))){
            helper.setGone(R.id.scene_rgb_layout,false)
            helper.setGone(R.id.sb_temperature_layout,false)
            helper.setGone(R.id.linearLayout2,false)
            helper.setGone(R.id.scene_switch_layout,true)
            if(item.isNo){
                helper.setChecked(R.id.rg_xx,true)
            }else{
                helper.setChecked(R.id.rg_yy,true)
            }
        }else if(OtherUtils.isCurtain(DBUtils.getGroupByMesh(item.groupAress))){
            helper.setGone(R.id.scene_rgb_layout,false)
            helper.setGone(R.id.sb_temperature_layout,false)
            helper.setGone(R.id.linearLayout2,false)
            helper.setGone(R.id.scene_switch_layout,true)
            if(item.isNo){
                helper.setChecked(R.id.rg_xx,true)
            }else{
                helper.setChecked(R.id.rg_yy,true)
            }
        }else{
            helper.setGone(R.id.scene_rgb_layout, false)
            helper.setGone(R.id.sb_temperature_layout, true)
        }

        sbBrightness.tag = position
        sBtemperature.tag = position
        sbBrightness.setOnSeekBarChangeListener(this)
        sBtemperature.setOnSeekBarChangeListener(this)
        helper.addOnClickListener(R.id.btn_delete)
        helper.addOnClickListener(R.id.rgb_view)
        helper.addOnClickListener(R.id.rg_xx)
        helper.addOnClickListener(R.id.rg_yy)
    }


    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        val currentTime = System.currentTimeMillis()
        val position = seekBar.tag as Int

        if(fromUser){
            changeTextView(seekBar, progress, position)
        }

        if (currentTime - this.preTime > this.delayTime) {
            if (fromUser) {
                val address = data[position].groupAress
                val opcode: Byte
                if (seekBar.id == R.id.sbBrightness) {
                    opcode = Opcode.SET_LUM
                    Thread { sendCmd(opcode, address, progress) }.start()
                } else if (seekBar.id == R.id.sbTemperature) {
                    opcode = Opcode.SET_TEMPERATURE
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
        if (seekBar.id == R.id.sbBrightness) {
            (Objects.requireNonNull<View>(getViewByPosition(pos, R.id.tvBrightness)) as TextView).text = seekBar.progress.toString() + "%"
            itemGroup.brightness = seekBar.progress
            opcode = Opcode.SET_LUM
            Thread { sendCmd(opcode, address, seekBar.progress) }.start()
        } else if (seekBar.id == R.id.sbTemperature) {
            (Objects.requireNonNull<View>(getViewByPosition(pos, R.id.tvTemperature)) as TextView).text = seekBar.progress.toString() + "%"
            itemGroup.temperature = seekBar.progress
            opcode = Opcode.SET_TEMPERATURE
            Thread { sendCmd(opcode, address, seekBar.progress) }.start()
        }
        notifyItemChanged(seekBar.tag as Int)
    }

    fun changeTextView(seekBar: SeekBar, progress: Int, position: Int) {
        if (seekBar.id == R.id.sbBrightness) {
            val tvBrightness = getViewByPosition(position, R.id.tvBrightness) as TextView?
            if (tvBrightness != null) {
                tvBrightness?.text = progress.toString() + "%"
            }
        } else if (seekBar.id == R.id.sbTemperature) {
            val tvTemperature = getViewByPosition(position, R.id.tvTemperature) as TextView?
            if (tvTemperature != null) {
                tvTemperature?.text = progress.toString() + "%"
            }
        }
    }

    fun sendCmd(opcode: Byte, address: Int, progress: Int) {
        var progressCmd=progress
        if(progress > Constant.MAX_VALUE){
            progressCmd=Constant.MAX_VALUE
        }
        var params: ByteArray
        if(opcode==Opcode.SET_TEMPERATURE){
            params = byteArrayOf(0x05, progressCmd.toByte())
        }else{
            params = byteArrayOf(progressCmd.toByte())
        }
        TelinkLightService.Instance().sendCommandNoResponse(opcode, address, params)
    }
}

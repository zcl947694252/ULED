package com.dadoutek.uled.scene

import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.ItemGroup
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
    private val delayTime = 100
    override fun convert(helper: BaseViewHolder, item: ItemGroup) {
        val position = helper.layoutPosition
        val sbBrightness = helper.getView<SeekBar>(R.id.sbBrightness)
        val sBtemperature = helper.getView<SeekBar>(R.id.sbTemperature)

        helper.setText(R.id.name_gp, item.gpName)
        helper.setBackgroundColor(R.id.rgb_view, if (item.color == 0)
            TelinkLightApplication.getInstance().resources.getColor(R.color.primary)
        else
            (0xff0000 shl 8) or (item.color and 0xffffff))
        helper.setProgress(R.id.sbBrightness, item.brightness)
        helper.setProgress(R.id.sbTemperature, item.temperature)
        helper.setText(R.id.tvBrightness, sbBrightness.progress.toString() + "%")
        helper.setText(R.id.tvTemperature, sBtemperature.progress.toString() + "%")
        if (OtherUtils.isRGBGroup(DBUtils.getGroupByMesh(item.groupAress))) {
            helper.setGone(R.id.scene_rgb_layout, true)
            helper.setGone(R.id.sb_temperature_layout, false)
        } else {
            helper.setGone(R.id.scene_rgb_layout, false)
            helper.setGone(R.id.sb_temperature_layout, true)
        }

        sbBrightness.tag = position
        sBtemperature.tag = position
        sbBrightness.setOnSeekBarChangeListener(this)
        sBtemperature.setOnSeekBarChangeListener(this)
        helper.addOnClickListener(R.id.btn_delete)
        helper.addOnClickListener(R.id.rgb_view)
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
                val params: ByteArray
                if (seekBar.id == R.id.sbBrightness) {
                    opcode = 0xD2.toByte()
                    params = byteArrayOf(progress.toByte())
                    Thread { TelinkLightService.Instance().sendCommandNoResponse(opcode, address, params) }.start()
                } else if (seekBar.id == R.id.sbTemperature) {
                    opcode = 0xE2.toByte()
                    params = byteArrayOf(0x05, progress.toByte())
                    Thread { TelinkLightService.Instance().sendCommandNoResponse(opcode, address, params) }.start()
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
        val params: ByteArray
        val itemGroup = data[pos]
        if (seekBar.id == R.id.sbBrightness) {
            (Objects.requireNonNull<View>(getViewByPosition(pos, R.id.tvBrightness)) as TextView).text = seekBar.progress.toString() + "%"
            itemGroup.brightness = seekBar.progress
            opcode = 0xD2.toByte()
            params = byteArrayOf(seekBar.progress.toByte())
            Thread { TelinkLightService.Instance().sendCommandNoResponse(opcode, address, params) }.start()
        } else if (seekBar.id == R.id.sbTemperature) {
            (Objects.requireNonNull<View>(getViewByPosition(pos, R.id.tvTemperature)) as TextView).text = seekBar.progress.toString() + "%"
            itemGroup.temperature = seekBar.progress
            opcode = 0xE2.toByte()
            params = byteArrayOf(0x05, seekBar.progress.toByte())
            Thread { TelinkLightService.Instance().sendCommandNoResponse(opcode, address, params) }.start()
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
}

package com.dadoutek.uled.group

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.DeviceType



/**
 * 创建者     ZCL
 * 创建时间   2019/11/11 9:21
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class BatchFourLightAdapter2(item: Int, deviceData: MutableList<DbLight>, context: Context) : RecyclerView.Adapter<BatchFourLightAdapter2.ViewHolder>(), View.OnClickListener, View.OnLongClickListener {
    var itemResId = item
    var data = deviceData
    var mContext = context
    var mPosition =1000

    override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
        val item = data[p1]
        mPosition = p1
        p0.deviceName.text = item.name
        p0.batchSelected.isChecked = item.selected

        if (item?.groupName != "") {
            p0.deviceName.setTextColor(mContext.getColor(R.color.blue_text))
            p0.groupName.setTextColor(mContext.getColor(R.color.blue_text))
            p0.groupName.visibility = View.VISIBLE
            p0.groupName.text = item?.groupName

            if (item?.productUUID == DeviceType.LIGHT_RGB) {
                p0.icon.setImageResource(R.drawable.icon_rgblight)
            } else {
                p0.icon.setImageResource(R.drawable.icon_device_open)
            }
        } else {
            p0.deviceName.setTextColor(mContext.getColor(R.color.gray_3))
            p0.groupName.visibility = View.GONE
            if (item.productUUID == DeviceType.LIGHT_RGB) {
                p0.icon.setImageResource(R.drawable.icon_device_down)
            } else {
                p0.icon.setImageResource(R.drawable.icon_rgblight_down)
            }
        }

        p0.deviceItem.setOnClickListener(this)
        p0.deviceItem.setOnLongClickListener(this)
    }

    override fun onLongClick(v: View?): Boolean {
        //zclClickListeners.zclLongClik(v,mPosition)
        return true
    }

    override fun onClick(v: View?) {
        //zclClickListeners.
    }



    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        val view = View.inflate(mContext, itemResId, null)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return if (data == null) {
            0
        } else {
            data.size
        }
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var icon = itemView.findViewById<ImageView>(R.id.batch_img_icon)
        var deviceName = itemView.findViewById<TextView>(R.id.batch_tv_device_name)
        var groupName = itemView.findViewById<TextView>(R.id.batch_tv_group_name)
        var batchSelected = itemView.findViewById<CheckBox>(R.id.batch_selected)
        var deviceItem = itemView.findViewById<LinearLayout>(R.id.batch_device_item)
    }

    /**
     * 第一步定义回调接口
     */

    interface  zclClickListeners{
        fun zclLongClik(postion:Int)
        fun zclClik(postion:Int)
    }

    // 接口回调第二步: 初始化接口的引用
    fun setOnRecyclerViewListener(l: zclClickListeners) {
        //this.mClikLisenters = l
    }

}
package com.dadoutek.uled.scene

import android.annotation.SuppressLint
import android.text.TextUtils
import android.view.View
import android.widget.TextView

import com.chad.library.adapter.base.BaseItemDraggableAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbScene
import com.dadoutek.uled.model.ItemGroup
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.OtherUtils

class SceneRecycleListAdapter(layoutResId: Int, data: List<DbScene>?, internal var isDelete: Boolean) : BaseItemDraggableAdapter<DbScene, BaseViewHolder>(layoutResId, data) {

    private var showGroupList: ArrayList<ItemGroup>? = null

    @SuppressLint("SetTextI18n")
    override fun convert(helper: BaseViewHolder, scene: DbScene?) {
        if (scene != null) {
            val groupNum = helper.getView<TextView>(R.id.template_device_group_name)

            var resId = if (TextUtils.isEmpty(scene.imgName)) R.drawable.icon_1 else OtherUtils.getResourceId(scene.imgName, mContext)
            helper.setImageResource(R.id.template_device_icon, resId)

            val actions = DBUtils.getActionsBySceneId(scene.id)
            showGroupList = ArrayList()
            for (i in actions.indices) {
                val item = DBUtils.getGroupByMeshAddr(actions[i].groupAddr)
                val itemGroup = ItemGroup()
                if (item != null) {
                    itemGroup.gpName = item.name
                    itemGroup.enableCheck = false
                    itemGroup.groupAddress = actions[i].groupAddr
                    itemGroup.brightness = actions[i].brightness
                    itemGroup.temperature = actions[i].colorTemperature
                    itemGroup.color = actions[i].color
                    itemGroup.isOn = actions[i].isOn
                    item.checked = true
                    showGroupList!!.add(itemGroup)
                }
            }


            groupNum.text =  if (showGroupList!!.size > 0)
                 TelinkLightApplication.getApp().getString(R.string.total) + showGroupList!!.size + TelinkLightApplication.getApp().getString(R.string.piece) +
                         TelinkLightApplication.getApp().getString(R.string.group)
             else
                TelinkLightApplication.getApp().getString(R.string.total) + 0 + TelinkLightApplication.getApp().getString(R.string.piece) + TelinkLightApplication.getApp().getString(R.string.group)

            helper.setVisible(R.id.template_device_card_delete,isDelete)
            helper.setChecked(R.id.scene_delete, scene.isSelected)

            helper.setText(R.id.template_device_group_name, scene.name)
                    .setVisible(R.id.template_device_more,false)
                    .addOnClickListener(R.id.template_device_card_delete)
                    .addOnClickListener(R.id.template_device_setting)
                    .addOnClickListener(R.id.template_device_icon)
        }
    }

    fun changeState(isDelete: Boolean) {
        this.isDelete = isDelete
    }
}

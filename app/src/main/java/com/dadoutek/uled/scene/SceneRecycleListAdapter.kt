package com.dadoutek.uled.scene

import android.annotation.SuppressLint
import android.view.View
import android.widget.TextView

import com.chad.library.adapter.base.BaseItemDraggableAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbScene
import com.dadoutek.uled.model.ItemGroup
import com.dadoutek.uled.tellink.TelinkLightApplication

class SceneRecycleListAdapter(layoutResId: Int, data: List<DbScene>?, internal var isDelete: Boolean) : BaseItemDraggableAdapter<DbScene, BaseViewHolder>(layoutResId, data) {

    private var showGroupList: ArrayList<ItemGroup>? = null

    @SuppressLint("SetTextI18n")
    override fun convert(helper: BaseViewHolder, scene: DbScene?) {
        if (scene != null) {
            val deleteIcon = helper.getView<TextView>(R.id.scene_delete)
            val groupNum = helper.getView<TextView>(R.id.scene_group)

            val actions = DBUtils.getActionsBySceneId(scene!!.id)
            showGroupList = ArrayList()
            for (i in actions.indices) {
                val item = DBUtils.getGroupByMesh(actions[i].groupAddr)
                val itemGroup = ItemGroup()
                if (item != null) {
                    itemGroup.gpName = item.name
                    itemGroup.enableCheck = false
                    itemGroup.groupAress = actions[i].groupAddr
                    itemGroup.brightness = actions[i].brightness
                    itemGroup.temperature = actions[i].colorTemperature
                    itemGroup.color = actions[i].color
                    itemGroup.isNo = actions[i].isOn
                    item.checked = true
                    showGroupList!!.add(itemGroup)
                }
            }


            if (showGroupList!!.size > 0) {
                groupNum.text = TelinkLightApplication.getInstance().getString(R.string.total) + showGroupList!!.size + TelinkLightApplication.getInstance().getString(R.string.piece) + TelinkLightApplication.getInstance().getString(R.string.group)
            } else {
                groupNum.text = TelinkLightApplication.getInstance().getString(R.string.total) + 0 + TelinkLightApplication.getInstance().getString(R.string.piece) + TelinkLightApplication.getInstance().getString(R.string.group)
            }


            if (isDelete) {
                deleteIcon.visibility = View.VISIBLE
            } else {
                deleteIcon.visibility = View.GONE
            }

            if (scene.isSelected) {
                helper.setChecked(R.id.scene_delete, true)
            } else {
                helper.setChecked(R.id.scene_delete, false)
            }

            helper.setText(R.id.scene_name, scene.name)
                    .addOnClickListener(R.id.scene_delete)
                    .addOnClickListener(R.id.scene_edit)
        }
    }

    fun changeState(isDelete: Boolean) {
        this.isDelete = isDelete
    }
}

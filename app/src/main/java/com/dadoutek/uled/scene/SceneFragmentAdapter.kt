package com.dadoutek.uled.scene


/**
 * 创建者     ZCL
 * 创建时间   2020/6/18 11:13
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
import android.view.View
import android.widget.TextView
import com.chad.library.adapter.base.BaseItemDraggableAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.dbModel.DbScene
import com.dadoutek.uled.util.OtherUtils



class SceneFragmentAdapter(layoutResId: Int, data: MutableList<DbScene>) : BaseItemDraggableAdapter<DbScene, BaseViewHolder>(layoutResId, data) {
    private var deleteB: Boolean = false

    override fun convert(helper: BaseViewHolder, item: DbScene?) {
        val deviceName = helper.getView<TextView>(R.id.template_device_group_name)

        if (item?.name==null||item?.name=="")
            deviceName.visibility = View.GONE
        else
            deviceName.visibility = View.VISIBLE
        var resourceId = OtherUtils.getResourceId(item?.imgName, mContext)
        if (resourceId==0)
            resourceId = R.drawable.icon_1

        helper.setText(R.id.template_device_group_name,item?.name)
                .setText(R.id.template_gp_name, item?.name)
                .setVisible(R.id.template_gp_name,true)
                .setImageResource(R.id.template_device_icon,resourceId)
                .addOnClickListener(R.id.template_device_setting)
                .addOnClickListener(R.id.template_device_icon)
                .addOnClickListener(R.id.template_device_more)
    }

    fun changeState(delete: Boolean) {
        deleteB =delete
    }
}

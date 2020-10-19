package com.dadoutek.uled.region.adapter

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Constants
import com.dadoutek.uled.region.bean.SettingItemBean

class SettingAdapter(layoutResId: Int, data: MutableList<SettingItemBean>, var isSetting: Boolean = false) : BaseQuickAdapter<SettingItemBean, BaseViewHolder>(layoutResId, data) {
    override fun convert(helper: BaseViewHolder?, item: SettingItemBean?) {
        helper?.setImageResource(R.id.item_setting_icon, item!!.icon)
                ?.setText(R.id.item_setting_text, item.title)

        if (!isSetting) {
            if (Constants.IS_ROUTE_MODE) {
                if (helper?.adapterPosition == 0 || helper?.adapterPosition == 2)
                    helper?.setTextColor(R.id.item_setting_text, mContext.getColor(R.color.gray))
            }
        } else {//如果是settingActivity
            helper?.setVisible(R.id.item_setting_back, helper?.adapterPosition != 0)//用户复位无箭头
                    ?.setVisible(R.id.item_setting_second_tv, helper?.adapterPosition == 3)//选择模式有后面text      if (IS_ROUTE_MODE)
            /*  if (helper?.adapterPosition == 3) {
                  var ly = helper?.getView<RelativeLayout>(R.id.item_setting_ly)
                  ly.visibility = if (DBUtils.getAllRouter().size > 1) View.VISIBLE else View.GONE
              }*/


            if (Constants.IS_ROUTE_MODE)
                helper?.setText(R.id.item_setting_second_tv, mContext.getString(R.string.route_mode))
            else
                helper?.setText(R.id.item_setting_second_tv, mContext.getString(R.string.bluetooth_mode))

            if (helper?.adapterPosition == 2) {//第四位是辅助功能变checkbox
                if (Constants.IS_OPEN_AUXFUN)
                    helper?.setImageResource(R.id.item_setting_back, R.drawable.choice_on)
                else
                    helper?.setImageResource(R.id.item_setting_back, R.drawable.choice_off)
            }
        }
    }
}

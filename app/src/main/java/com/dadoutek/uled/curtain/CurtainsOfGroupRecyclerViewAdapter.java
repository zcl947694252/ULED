package com.dadoutek.uled.curtain;

import android.widget.TextView;

import com.chad.library.adapter.base.BaseItemDraggableAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.dadoutek.uled.R;
import com.dadoutek.uled.model.dbModel.DbCurtain;
import com.dadoutek.uled.tellink.TelinkLightApplication;

import java.util.List;

/**
 * Created by hejiajun on 2018/4/25.
 */

public class CurtainsOfGroupRecyclerViewAdapter extends BaseItemDraggableAdapter<DbCurtain, BaseViewHolder> {

    public boolean delete =false;

    public CurtainsOfGroupRecyclerViewAdapter(int layoutResId, List<DbCurtain> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, DbCurtain item) {
        TextView tvName=helper.getView(R.id.template_device_group_name);
//        TextView tvRgbColor=helper.getView(R.id.tv_rgb_color);

        if(TelinkLightApplication.Companion.getApp().getConnectDevice() == null){
           tvName.setTextColor(mContext.getResources().getColor(R.color.black));
        }else{
            if(TelinkLightApplication.Companion.getApp().getConnectDevice().meshAddress==item.getMeshAddr()){
                tvName.setTextColor(mContext.getResources().getColor(R.color.primary));
            }else{
                tvName.setTextColor(mContext.getResources().getColor(R.color.gray));
            }
        }

        tvName.setText(item.getName());


        helper.addOnClickListener(R.id.template_device_setting)
                .setTag(R.id.template_device_setting, helper.getAdapterPosition())
                .setVisible(R.id.template_device_more, false)
                .setVisible(R.id.template_device_card_delete, delete)
                .setVisible(R.id.template_gp_name, false)
                .setTag(R.id.template_device_icon, helper.getAdapterPosition())
                .setImageResource(R.id.template_device_icon,item.icon)
                .addOnClickListener(R.id.template_device_icon)
                .addOnClickListener(R.id.template_device_card_delete);
    }

    public void changeState(boolean delete) {
        this.delete = delete;
    }
}
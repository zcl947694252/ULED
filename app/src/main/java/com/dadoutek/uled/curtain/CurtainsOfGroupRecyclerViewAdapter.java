package com.dadoutek.uled.curtain;

import android.widget.TextView;

import com.chad.library.adapter.base.BaseItemDraggableAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.dadoutek.uled.R;
import com.dadoutek.uled.model.DbModel.DbCurtain;
import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.dadoutek.uled.util.StringUtils;

import java.util.List;

/**
 * Created by hejiajun on 2018/4/25.
 */

public class CurtainsOfGroupRecyclerViewAdapter extends BaseItemDraggableAdapter<
        DbCurtain, BaseViewHolder> {

    public CurtainsOfGroupRecyclerViewAdapter(List<DbCurtain> data) {
        super(data);
    }

    public CurtainsOfGroupRecyclerViewAdapter(int layoutResId, List<DbCurtain> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, DbCurtain item) {
        TextView tvName=helper.getView(R.id.name);
        TextView tvLightName=helper.getView(R.id.template_device_name_n);
//        TextView tvRgbColor=helper.getView(R.id.tv_rgb_color);
        tvName.setText(StringUtils.getCurtainName(item));

        if(TelinkLightApplication.Companion.getApp().getConnectDevice() == null){
           tvName.setTextColor(mContext.getResources().getColor(R.color.black));
        }else{
            if(TelinkLightApplication.Companion.getApp().getConnectDevice().meshAddress==item.getMeshAddr()){
                tvName.setTextColor(mContext.getResources().getColor(R.color.primary));
                tvLightName.setTextColor(mContext.getResources().getColor(R.color.primary));
            }else{
                tvName.setTextColor(mContext.getResources().getColor(R.color.gray));
                tvLightName.setTextColor(mContext.getResources().getColor(R.color.black));
            }
        }

        tvLightName.setText(item.getName());

        helper.addOnClickListener(R.id.tv_setting)
                .setTag(R.id.tv_setting,helper.getAdapterPosition())
                .setTag(R.id.img_light,helper.getAdapterPosition())
                .setBackgroundRes(R.id.img_light,item.icon)
                .addOnClickListener(R.id.img_light);
    }
}
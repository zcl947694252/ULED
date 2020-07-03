package com.dadoutek.uled.light;

import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.TextView;

import com.blankj.utilcode.util.LogUtils;
import com.chad.library.adapter.base.BaseItemDraggableAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.dadoutek.uled.model.DbModel.DbLight;
import com.dadoutek.uled.R;
import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.dadoutek.uled.util.StringUtils;

import java.util.List;

/**
 * Created by hejiajun on 2018/4/25.
 */

public class LightsOfGroupRecyclerViewAdapter extends BaseItemDraggableAdapter<DbLight, BaseViewHolder> {

    private Boolean isDelete = false;

    public LightsOfGroupRecyclerViewAdapter(int layoutResId, List<DbLight> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, DbLight item) {
        TextView tvName=helper.getView(R.id.name);
        TextView tvLightName=helper.getView(R.id.template_device_name_n);
        TextView tvRgbColor=helper.getView(R.id.tv_rgb_color);
        tvName.setText(StringUtils.getLightGroupName(item));

        if(TelinkLightApplication.Companion.getApp().getConnectDevice() == null){
           tvName.setTextColor(mContext.getResources().getColor(R.color.black));
        }else{
            if(TelinkLightApplication.Companion.getApp().getConnectDevice().meshAddress == item.getMeshAddr()){
                LogUtils.d("pos = " + helper.getAdapterPosition() + " meshAddr = " + item.getMeshAddr());
                tvName.setTextColor(mContext.getResources().getColor(R.color.primary));
                tvLightName.setTextColor(mContext.getResources().getColor(R.color.primary));
            }else{
                tvName.setTextColor(mContext.getResources().getColor(R.color.gray));
                tvLightName.setTextColor(mContext.getResources().getColor(R.color.black));
            }
        }

        tvLightName.setText(item.getName());

        GradientDrawable myGrad = (GradientDrawable)tvRgbColor.getBackground();
        if(item.getColor()==0||item.getColor()==0xffffff){
            tvRgbColor.setVisibility(View.GONE);
        }else{
            tvRgbColor.setVisibility(View.GONE);
            myGrad.setColor(0Xff000000|item.getColor());
        }

        helper.addOnClickListener(R.id.tv_setting)
                .setTag(R.id.tv_setting,helper.getAdapterPosition())
                .setTag(R.id.img_light,helper.getAdapterPosition())
                .setVisible(R.id.iv_delete,isDelete)
                .setBackgroundRes(R.id.img_light,item.icon)
                .addOnClickListener(R.id.img_light);
    }

    /**
     * 改变状态
     * @param isDelete  是否处于删除状态
     */
    public void  changeState( Boolean  isDelete) {
        this.isDelete = isDelete;
    }
}
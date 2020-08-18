package com.dadoutek.uled.light;

import android.widget.TextView;

import com.blankj.utilcode.util.LogUtils;
import com.chad.library.adapter.base.BaseItemDraggableAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.dadoutek.uled.R;
import com.dadoutek.uled.model.dbModel.DbLight;
import com.dadoutek.uled.tellink.TelinkLightApplication;

import java.util.List;

/**
 * Created by hejiajun on 2018/4/25.
 */

public class LightsOfGroupRecyclerViewAdapter extends BaseItemDraggableAdapter<DbLight, BaseViewHolder> {

    private Boolean isDelete = false;
    private Integer type =0;

    public LightsOfGroupRecyclerViewAdapter(int layoutResId, List<DbLight> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, DbLight item) {
        TextView tvLightName=helper.getView(R.id.template_device_group_name);

        if(TelinkLightApplication.Companion.getApp().getConnectDevice() == null){
            tvLightName.setTextColor(mContext.getResources().getColor(R.color.black));
        }else{
            if(TelinkLightApplication.Companion.getApp().getConnectDevice().meshAddress == item.getMeshAddr()){
                LogUtils.d("pos = " + helper.getAdapterPosition() + " meshAddr = " + item.getMeshAddr());
                tvLightName.setTextColor(mContext.getResources().getColor(R.color.primary));
            }else{
                tvLightName.setTextColor(mContext.getResources().getColor(R.color.gray));
            }
        }

        tvLightName.setText(item.getName());

/*        GradientDrawable myGrad = (GradientDrawable)tvRgbColor.getBackground();
        if(item.getColor()==0||item.getColor()==0xffffff){
            tvRgbColor.setVisibility(View.GONE);
        }else{
            tvRgbColor.setVisibility(View.GONE);
            myGrad.setColor(0Xff000000|item.getColor());
        }*/

        helper.addOnClickListener(R.id.template_device_setting)
                .setTag(R.id.template_device_setting,helper.getAdapterPosition())
                .setTag(R.id.template_device_icon,helper.getAdapterPosition())
                .setVisible(R.id.template_device_card_delete,isDelete)
                .setImageResource(R.id.template_device_icon,item.icon)
                .setVisible(R.id.template_device_more,false)
                .addOnClickListener(R.id.template_device_icon)
                .addOnClickListener(R.id.template_device_card_delete);
    }

    /**
     * 改变状态
     * @param isDelete  是否处于删除状态
     */
    public void  changeState( Boolean  isDelete) {
        this.isDelete = isDelete;
    }

    public void setType(Integer deviceType) {
        type = deviceType;
    }
}
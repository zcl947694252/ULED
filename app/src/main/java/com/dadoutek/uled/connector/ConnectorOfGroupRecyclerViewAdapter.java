package com.dadoutek.uled.connector;

import android.widget.TextView;

import com.chad.library.adapter.base.BaseItemDraggableAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.dadoutek.uled.R;
import com.dadoutek.uled.model.DbModel.DbConnector;
import com.dadoutek.uled.tellink.TelinkLightApplication;

import java.util.List;

public class ConnectorOfGroupRecyclerViewAdapter extends BaseItemDraggableAdapter<
        DbConnector, BaseViewHolder> {
    public ConnectorOfGroupRecyclerViewAdapter(List<DbConnector> data) {
        super(data);
    }

    public ConnectorOfGroupRecyclerViewAdapter(int layoutResId, List<DbConnector> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, DbConnector item) {
        TextView tvName=helper.getView(R.id.template_device_group_name);

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

   /*     GradientDrawable myGrad = (GradientDrawable)tvRgbColor.getBackground();
        if(item.getColor()==0||item.getColor()==0xffffff){
            tvRgbColor.setVisibility(View.GONE);
        }else{
            tvRgbColor.setVisibility(View.VISIBLE);
            myGrad.setColor(0Xff000000|item.getColor());
        }*/

        helper.addOnClickListener(R.id.template_device_setting)
                .setTag(R.id.template_device_setting,helper.getAdapterPosition())
                .setTag(R.id.template_device_icon,helper.getAdapterPosition())
                .setVisible(R.id.template_device_more,false)
                .setBackgroundRes(R.id.template_device_icon,item.icon)
                .addOnClickListener(R.id.template_device_icon);
    }
}

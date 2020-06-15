package com.dadoutek.uled.connector;

import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseItemDraggableAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.dadoutek.uled.R;
import com.dadoutek.uled.model.DbModel.DbConnector;
import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.dadoutek.uled.util.StringUtils;

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
        TextView tvName=helper.getView(R.id.name);
        TextView tvLightName=helper.getView(R.id.template_device_name_n);
        TextView tvRgbColor=helper.getView(R.id.tv_rgb_color);
        tvName.setText(StringUtils.getConnectorName(item));

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

        GradientDrawable myGrad = (GradientDrawable)tvRgbColor.getBackground();
        if(item.getColor()==0||item.getColor()==0xffffff){
            tvRgbColor.setVisibility(View.GONE);
        }else{
            tvRgbColor.setVisibility(View.VISIBLE);
            myGrad.setColor(0Xff000000|item.getColor());
        }

        helper.addOnClickListener(R.id.tv_setting)
                .setTag(R.id.tv_setting,helper.getAdapterPosition())
                .setTag(R.id.img_light,helper.getAdapterPosition())
                .setBackgroundRes(R.id.img_light,item.icon)
                .addOnClickListener(R.id.img_light);
    }
}

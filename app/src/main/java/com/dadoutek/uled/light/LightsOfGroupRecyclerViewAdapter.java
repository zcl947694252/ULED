package com.dadoutek.uled.light;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseItemDraggableAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.dadoutek.uled.model.DbModel.DbLight;
import com.dadoutek.uled.R;
import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.dadoutek.uled.intf.SwitchButtonOnCheckedChangeListener;
import com.dadoutek.uled.util.DataManager;
import com.dadoutek.uled.util.StringUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by hejiajun on 2018/4/25.
 */

public class LightsOfGroupRecyclerViewAdapter extends BaseItemDraggableAdapter<
        DbLight, BaseViewHolder> {

    public LightsOfGroupRecyclerViewAdapter(List<DbLight> data) {
        super(data);
    }

    public LightsOfGroupRecyclerViewAdapter(int layoutResId, List<DbLight> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, DbLight item) {
        TextView tvName=helper.getView(R.id.name);
        TextView tvLightName=helper.getView(R.id.light_name);
        TextView tvRgbColor=helper.getView(R.id.tv_rgb_color);
        tvName.setText(StringUtils.getLightName(item));

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
            tvRgbColor.setVisibility(View.GONE);
            myGrad.setColor(0Xff000000|item.getColor());
        }

        helper.addOnClickListener(R.id.tv_setting)
                .setTag(R.id.tv_setting,helper.getAdapterPosition())
                .setTag(R.id.img_light,helper.getAdapterPosition())
                .setBackgroundRes(R.id.img_light,item.icon)
                .addOnClickListener(R.id.img_light);
    }
}
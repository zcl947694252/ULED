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
//    {
//    private final LayoutInflater mLayoutInflater;
//    private final Context mContext;
//    private List<DbLight> lightList;
//    private DataManager dataManager;
//    private TelinkLightApplication mApplication;
//    public SwitchButtonOnCheckedChangeListener onCheckedChangeListener;
//
//    public LightsOfGroupRecyclerViewAdapter(Context mContext, List<DbLight> lightList,
//                                            SwitchButtonOnCheckedChangeListener onCheckedChangeListener) {
//        this.mContext = mContext;
//        this.lightList = lightList;
//        this.onCheckedChangeListener = onCheckedChangeListener;
//        mLayoutInflater = LayoutInflater.from(mContext);
//        mApplication = (TelinkLightApplication) TelinkLightApplication.getInstance();
//        dataManager = new DataManager(mContext, mApplication.getMesh().getName(), mApplication.getMesh().getPassword());
//    }
//
//    @NonNull
//    @Override
//    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = mLayoutInflater.inflate(R.layout.item_lights_of_group, parent, false);
//        ViewHolder holder = new ViewHolder(view);
//        return holder;
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
//        DbLight light = lightList.get(position);
//        holder.name.setText(dataManager.getLightName(light));
//
//        if(TelinkLightApplication.getInstance().getConnectDevice() == null){
//            holder.name.setTextColor(mContext.getResources().getColor(R.color.black));
//        }else{
//            if(TelinkLightApplication.getInstance().getConnectDevice().meshAddress==light.getMeshAddr()){
//                holder.name.setTextColor(mContext.getResources().getColor(R.color.primary));
//            }else{
//                holder.name.setTextColor(mContext.getResources().getColor(R.color.black));
//            }
//        }
//
//        GradientDrawable myGrad = (GradientDrawable)holder.tvRgbColor.getBackground();
//        if(light.getColor()==0||light.getColor()==0xffffff){
//            holder.tvRgbColor.setVisibility(View.GONE);
//        }else{
//            holder.tvRgbColor.setVisibility(View.VISIBLE);
//            myGrad.setColor(0Xff000000|light.getColor());
//        }
////        holder.name.setTextColor(light.textColor);
//        holder.ivSetting.setOnClickListener(this);
//        holder.ivSetting.setTag(position);
//        holder.imgLight.setBackgroundResource(light.icon);
//        holder.imgLight.setOnClickListener(this);
//        holder.imgLight.setTag(position);
//    }
//
//    @Override
//    public int getItemCount() {
//        return lightList == null ? 0 : lightList.size();
//    }
//
//    @Override
//    public long getItemId(int position) {
//        return super.getItemId(position);
//    }
//
//    @Override
//    public void onClick(View v) {
//        onCheckedChangeListener.OnCheckedChangeListener(v, (int) v.getTag());
//    }
//
//    static class ViewHolder extends RecyclerView.ViewHolder {
//        @BindView(R.id.img_light)
//        ImageView imgLight;
//        @BindView(R.id.name)
//        TextView name;
//        @BindView(R.id.tv_setting)
//        ImageView ivSetting;
//        @BindView(R.id.tv_rgb_color)
//        TextView tvRgbColor;
//
//        ViewHolder(View view) {
//            super(view);
//            ButterKnife.bind(this, view);
//        }
//    }
//    }

    public LightsOfGroupRecyclerViewAdapter(List<DbLight> data) {
        super(data);
    }

    public LightsOfGroupRecyclerViewAdapter(int layoutResId, List<DbLight> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, DbLight item) {
        TextView tvName=helper.getView(R.id.name);
        TextView tvRgbColor=helper.getView(R.id.tv_rgb_color);
        tvName.setText(StringUtils.getLightName(item));

        if(TelinkLightApplication.getInstance().getConnectDevice() == null){
           tvName.setTextColor(mContext.getResources().getColor(R.color.black));
        }else{
            if(TelinkLightApplication.getInstance().getConnectDevice().meshAddress==item.getMeshAddr()){
                tvName.setTextColor(mContext.getResources().getColor(R.color.primary));
            }else{
                tvName.setTextColor(mContext.getResources().getColor(R.color.black));
            }
        }

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
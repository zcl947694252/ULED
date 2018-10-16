package com.dadoutek.uled.light;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dadoutek.uled.model.DbModel.DbLight;
import com.dadoutek.uled.R;
import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.dadoutek.uled.intf.SwitchButtonOnCheckedChangeListener;
import com.dadoutek.uled.util.DataManager;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by hejiajun on 2018/4/25.
 */

public class LightsOfGroupRecyclerViewAdapter extends
        RecyclerView.Adapter<LightsOfGroupRecyclerViewAdapter.ViewHolder> implements View.OnClickListener {
    private final LayoutInflater mLayoutInflater;
    private final Context mContext;
    private List<DbLight> lightList;
    private DataManager dataManager;
    private TelinkLightApplication mApplication;
    public SwitchButtonOnCheckedChangeListener onCheckedChangeListener;

    public LightsOfGroupRecyclerViewAdapter(Context mContext, List<DbLight> lightList,
                                            SwitchButtonOnCheckedChangeListener onCheckedChangeListener) {
        this.mContext = mContext;
        this.lightList = lightList;
        this.onCheckedChangeListener = onCheckedChangeListener;
        mLayoutInflater = LayoutInflater.from(mContext);
        mApplication = (TelinkLightApplication) TelinkLightApplication.getInstance();
        dataManager = new DataManager(mContext, mApplication.getMesh().getName(), mApplication.getMesh().getPassword());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mLayoutInflater.inflate(R.layout.item_lights_of_group, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DbLight light = lightList.get(position);
        holder.name.setText(dataManager.getLightName(light));

        if(TelinkLightApplication.getInstance().getConnectDevice() == null){
            holder.name.setTextColor(mContext.getResources().getColor(R.color.black));
        }else{
            if(TelinkLightApplication.getInstance().getConnectDevice().meshAddress==light.getMeshAddr()){
                holder.name.setTextColor(mContext.getResources().getColor(R.color.primary));
            }else{
                holder.name.setTextColor(mContext.getResources().getColor(R.color.black));
            }
        }
//        holder.name.setTextColor(light.textColor);
        holder.ivSetting.setOnClickListener(this);
        holder.ivSetting.setTag(position);
        holder.imgLight.setBackgroundResource(light.icon);
        holder.imgLight.setOnClickListener(this);
        holder.imgLight.setTag(position);
    }

    @Override
    public int getItemCount() {
        return lightList == null ? 0 : lightList.size();
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @Override
    public void onClick(View v) {
        onCheckedChangeListener.OnCheckedChangeListener(v, (int) v.getTag());
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.img_light)
        ImageView imgLight;
        @BindView(R.id.name)
        TextView name;
        @BindView(R.id.tv_setting)
        ImageView ivSetting;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}

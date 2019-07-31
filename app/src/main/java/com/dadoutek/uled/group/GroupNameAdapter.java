package com.dadoutek.uled.group;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dadoutek.uled.R;
import com.dadoutek.uled.intf.OnRecyclerviewItemClickListener;
import com.dadoutek.uled.intf.OnRecyclerviewItemLongClickListener;
import com.dadoutek.uled.model.DbModel.DbDeviceName;
import com.dadoutek.uled.model.DbModel.DbGroup;
import com.dadoutek.uled.model.Group;
import com.dadoutek.uled.model.Groups;

import java.util.List;

public class GroupNameAdapter  extends RecyclerView.Adapter<GroupNameAdapter.ViewHolder> implements View.OnClickListener, View.OnLongClickListener {

    private List<DbDeviceName> mGroupList;
    //声明自定义的监听接口
    private OnRecyclerviewItemClickListener mOnRecyclerviewItemClickListener = null;
    private OnRecyclerviewItemLongClickListener mOnRecyclerviewItemLongClickListener = null;

    @Override
    public void onClick(View v) {
        //将监听传递给自定义接口
        mOnRecyclerviewItemClickListener.onItemClickListener(v, ((int) v.getTag()));
    }

    public Group get(int addr) {
        return Groups.getInstance().getByMeshAddress(addr);
    }

    @Override
    public boolean onLongClick(View v) {
        mOnRecyclerviewItemLongClickListener.onItemLongClickListener(v, ((int) v.getTag()));
        return false;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView groupImage;
        TextView groupName;

        public ViewHolder(View view) {
            super(view);
            groupImage = (ImageView) view.findViewById(R.id.cw_light_image);
            groupName = (TextView) view.findViewById(R.id.cw_light_text);
        }
    }

    public GroupNameAdapter(List<DbDeviceName> mGroupList, OnRecyclerviewItemClickListener mOnRecyclerviewItemClickListener
    ) {
        this.mGroupList = mGroupList;
        this.mOnRecyclerviewItemClickListener = mOnRecyclerviewItemClickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adpter_device_name, parent, false);
        ViewHolder holder = new ViewHolder(view);
        view.setOnClickListener(this);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        DbDeviceName group = mGroupList.get(position);
        holder.groupName.setText(group.getName());
        holder.itemView.setTag(position);//给view设置tag以作为参数传递到监听回调方法中
        if (mGroupList.get(position).checked) {
//            holder.groupImage.setBackgroundResource(R.drawable.btn_rec_blue_bt);
            holder.groupName.setTextColor(Color.parseColor("#18B4ED"));
            holder.groupImage.setVisibility(View.VISIBLE);
        } else {
            holder.groupImage.setVisibility(View.GONE);
            holder.groupName.setTextColor(Color.parseColor("#999999"));
        }

        RecyclerView.LayoutParams param = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
            holder.itemView.setVisibility(View.VISIBLE);
//            param.width = RecyclerView.LayoutParams.WRAP_CONTENT;
//            param.height = RecyclerView.LayoutParams.WRAP_CONTENT;
            holder.itemView.setLayoutParams(param);

//        if (position == mGroupList.size() - 1) {
//            holder.itemView.setVisibility(View.GONE);
//        }
    }


    @Override
    public int getItemCount() {
        return mGroupList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

}

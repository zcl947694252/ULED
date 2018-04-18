package com.dadoutek.uled.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkLightApplication;
import com.dadoutek.uled.intf.OnRecyclerviewItemClickListener;
import com.dadoutek.uled.model.Constant;
import com.dadoutek.uled.model.Group;
import com.dadoutek.uled.model.Groups;
import com.dadoutek.uled.model.SharedPreferencesHelper;

import java.util.List;

/**
 * Created by hejiajun on 2018/3/28.
 */

public class GroupsRecyclerViewAdapter extends RecyclerView.Adapter<GroupsRecyclerViewAdapter.ViewHolder> implements View.OnClickListener {

    private Groups mGroupList;
    //声明自定义的监听接口
    private OnRecyclerviewItemClickListener mOnRecyclerviewItemClickListener = null;

    @Override
    public void onClick(View v) {
        //将监听传递给自定义接口
        mOnRecyclerviewItemClickListener.onItemClickListener(v, ((int) v.getTag()));
    }

    public Group get(int addr) {
        return Groups.getInstance().getByMeshAddress(addr);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView groupImage;
        TextView groupName;

        public ViewHolder(View view) {
            super(view);
            groupImage = (ImageView) view.findViewById(R.id.group_img);
            groupName = (TextView) view.findViewById(R.id.group_name);
        }
    }

    public GroupsRecyclerViewAdapter(Groups mGroupList, OnRecyclerviewItemClickListener mOnRecyclerviewItemClickListener) {
        this.mGroupList = mGroupList;
        this.mOnRecyclerviewItemClickListener = mOnRecyclerviewItemClickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adpter_item_recycler_groups, parent, false);
        ViewHolder holder = new ViewHolder(view);
        view.setOnClickListener(this);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Group group = mGroupList.get(position);
        holder.groupName.setText(group.name);
        holder.itemView.setTag(position);//给view设置tag以作为参数传递到监听回调方法中
        if (mGroupList.get(position).checked) {
            holder.groupImage.setImageResource(R.drawable.ic_group_black_48dp);
        } else {
            holder.groupImage.setImageResource(R.drawable.ic_group_white_48dp);
        }

//        if (position == mGroupList.size() - 1) {
//            holder.itemView.setVisibility(View.GONE);
//        }
    }


    @Override
    public int getItemCount() {
        return mGroupList.size() - 1;
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

}

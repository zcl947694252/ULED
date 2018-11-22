package com.dadoutek.uled.group;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.dadoutek.uled.R;
import com.dadoutek.uled.intf.OnRecyclerviewItemClickListener;
import com.dadoutek.uled.model.DbModel.DbGroup;
import com.dadoutek.uled.model.Group;
import com.dadoutek.uled.model.Groups;

import java.util.List;

/**
 * Created by hejiajun on 2018/3/28.
 */

public class GroupsRecyclerViewAdapter extends BaseQuickAdapter<Group, BaseViewHolder> {

    public GroupsRecyclerViewAdapter(int layoutResId, @Nullable List<Group> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, Group item) {
        helper.setText()
    }

//    static class ViewHolder extends RecyclerView.ViewHolder {
//        ImageView groupImage;
//        TextView groupName;
//
//        public ViewHolder(View view) {
//            super(view);
//            groupImage = (ImageView) view.findViewById(R.id.group_img);
//            groupName = (TextView) view.findViewById(R.id.group_name);
//        }
//    }
//
//    public GroupsRecyclerViewAdapter(List<DbGroup> mGroupList, OnRecyclerviewItemClickListener mOnRecyclerviewItemClickListener) {
//        this.mGroupList = mGroupList;
//        this.mOnRecyclerviewItemClickListener = mOnRecyclerviewItemClickListener;
//    }
//
//    @Override
//    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adpter_item_recycler_groups, parent, false);
//        ViewHolder holder = new ViewHolder(view);
//        view.setOnClickListener(this);
//        return holder;
//    }
//
//    @Override
//    public void onBindViewHolder(ViewHolder holder, int position) {
//        DbGroup group = mGroupList.get(position);
//        holder.groupName.setText(group.getName());
//        holder.itemView.setTag(position);//给view设置tag以作为参数传递到监听回调方法中
//        if (mGroupList.get(position).checked) {
//            holder.groupImage.setImageResource(R.drawable.ic_group_black_48dp);
//        } else {
//            holder.groupImage.setImageResource(R.drawable.ic_group_white_48dp);
//        }
//
//        RecyclerView.LayoutParams param = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
//        if (position == 0 && mGroupList.get(position).getMeshAddr() == 0xffff) {
//            holder.itemView.setVisibility(View.GONE);
//            param.height = 0;
//            param.width = 0;
//            holder.itemView.setLayoutParams(param);
//        } else {
//            holder.itemView.setVisibility(View.VISIBLE);
//            param.width = RecyclerView.LayoutParams.WRAP_CONTENT;
//            param.height = RecyclerView.LayoutParams.WRAP_CONTENT;
//            holder.itemView.setLayoutParams(param);
//        }
//
////        if (position == mGroupList.size() - 1) {
////            holder.itemView.setVisibility(View.GONE);
////        }
//    }
//
//
//    @Override
//    public int getItemCount() {
//        return mGroupList.size();
//    }
//
//    @Override
//    public int getItemViewType(int position) {
//        return super.getItemViewType(position);
//    }
//
//    @Override
//    public long getItemId(int position) {
//        return super.getItemId(position);
//    }

}

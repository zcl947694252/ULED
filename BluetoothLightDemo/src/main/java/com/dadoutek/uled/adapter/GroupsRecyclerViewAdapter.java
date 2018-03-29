package com.dadoutek.uled.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dadoutek.uled.R;
import com.dadoutek.uled.model.Group;
import com.dadoutek.uled.model.Groups;

import java.util.List;

/**
 * Created by hejiajun on 2018/3/28.
 */

public class GroupsRecyclerViewAdapter extends RecyclerView.Adapter<GroupsRecyclerViewAdapter.ViewHolder>{

    private Groups mGroupList;

    static class ViewHolder extends RecyclerView.ViewHolder{
        ImageView groupImage;
        TextView groupName;

        public ViewHolder(View view) {
            super(view);
            groupImage = (ImageView) view.findViewById(R.id.group_img);
            groupName = (TextView) view.findViewById(R.id.group_name);
        }
    }

    public GroupsRecyclerViewAdapter(Groups mGroupList) {
        this.mGroupList = mGroupList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adpter_item_recycler_groups,parent,false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Group group = mGroupList.get(position);
        holder.groupName.setText(group.name);
        holder.groupImage.setImageResource(group.icon);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for(int i=0;i<mGroupList.size();i++){
                    if(i!=holder.getItemId()){
                        mGroupList.get(i).checked=false;
                    }else{
                        mGroupList.get(i).checked=true;
                        holder.groupImage.setImageResource(R.drawable.ic_group_white_48dp);
                    }
                }
            }
        });
    }


    @Override
    public int getItemCount() {
        return mGroupList.size();
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }
}

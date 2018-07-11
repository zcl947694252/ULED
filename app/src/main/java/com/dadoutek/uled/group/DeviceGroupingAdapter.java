package com.dadoutek.uled.group;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.dadoutek.uled.R;
import com.dadoutek.uled.model.DbModel.DbGroup;

import java.util.List;

public class DeviceGroupingAdapter extends BaseAdapter {

    private List<DbGroup> groupsInit;
    private LayoutInflater inflater;
    Context mContext;

    public DeviceGroupingAdapter(List<DbGroup> groupsInit, Context context) {
        this.groupsInit=groupsInit;
        mContext=context;
        inflater=LayoutInflater.from(mContext);
    }

    @Override
    public int getCount() {
        return groupsInit.size();
    }

    @Override
    public DbGroup getItem(int position) {
        return groupsInit.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public DbGroup get(int addr) {
        for (int j = 0; j < groupsInit.size(); j++) {
            if (addr == groupsInit.get(j).getMeshAddr()) {
                return groupsInit.get(j);
            }
        }
        return null;
    }

    @Override
    @Deprecated
    public View getView(int position, View convertView, ViewGroup parent) {

        GroupItemHolder holder;

//            if (convertView == null) {

        convertView = inflater.inflate(R.layout.grouping_item, null);

        TextView txtName = (TextView) convertView
                .findViewById(R.id.txt_name);

        holder = new GroupItemHolder();
        holder.name = txtName;

        convertView.setTag(holder);
//
//            } else {
//                holder = (GroupItemHolder) convertView.getTag();
//            }

        DbGroup group = this.getItem(position);

        if (group != null) {
            holder.name.setText(group.getName());

            if (group.checked) {
                ColorStateList color = mContext.getResources()
                        .getColorStateList(R.color.primary);
                holder.name.setTextColor(color);
            } else {
                ColorStateList color = mContext.getResources()
                        .getColorStateList(R.color.black);
                holder.name.setTextColor(color);
            }

        }

        if (position == 0) {
            AbsListView.LayoutParams layoutParams = new AbsListView.LayoutParams(1, 1);
            convertView.setLayoutParams(layoutParams);
        }

        return convertView;
    }

    private static class GroupItemHolder {
        public TextView name;
    }
}

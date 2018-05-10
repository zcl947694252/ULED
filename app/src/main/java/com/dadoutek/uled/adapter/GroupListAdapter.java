package com.dadoutek.uled.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.dadoutek.uled.R;
import com.dadoutek.uled.model.Group;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by hejiajun on 2018/5/10.
 */

public class GroupListAdapter extends BaseAdapter{

    Context context;
    List<Group>list;

    public GroupListAdapter(Context context, List<Group>list) {
        this.context=context;
        this.list=list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = View.inflate(context, R.layout.item_group,null);
        TextView tvName = (TextView) convertView.findViewById(R.id.tv_group_name);
        if(list.get(position).selected){
            convertView.setVisibility(View.GONE);
        }else{
            convertView.setVisibility(View.VISIBLE);
            tvName.setText(list.get(position).name);
        }
        return convertView;
    }
}

package com.dadoutek.uled.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.dadoutek.uled.DbModel.DbScene;
import com.dadoutek.uled.R;
import com.dadoutek.uled.intf.AdapterOnClickListner;
import com.dadoutek.uled.model.Scenes;

import java.util.List;

/**
 * Created by hejiajun on 2018/5/2.
 */

public class SceneAdaper extends BaseAdapter implements View.OnClickListener{

    private List<DbScene>list;
    private Context context;
    private boolean isDelete;
    private LayoutInflater mLayoutInflater;
    private AdapterOnClickListner onClickListner1;

    public SceneAdaper(List<DbScene>list, Context context, boolean isDelete, AdapterOnClickListner onClickListner1) {
        this.list=list;
        this.context=context;
        this.isDelete=isDelete;
        mLayoutInflater=LayoutInflater.from(context);
        this.onClickListner1=onClickListner1;
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
        ViewHolder holder;
        if(convertView==null){
            convertView=mLayoutInflater.inflate(R.layout.item_scene,null);
            holder=new ViewHolder();

            holder.apply=convertView.findViewById(R.id.scene_apply);
            holder.delete=convertView.findViewById(R.id.scene_delete);
            holder.sceneName=convertView.findViewById(R.id.scene_name);

            convertView.setTag(holder);
        }else{
            holder= (ViewHolder) convertView.getTag();
        }

        if(isDelete){
            holder.delete.setVisibility(View.VISIBLE);
        }else{
            holder.delete.setVisibility(View.GONE);
        }

        holder.sceneName.setText(list.get(position).getName());
        holder.delete.setOnClickListener(this);
        holder.delete.setTag(position);
        holder.apply.setOnClickListener(this);
        holder.apply.setTag(position);
        return convertView;
    }

    @Override
    public void onClick(View v) {
        onClickListner1.adapterOnClick(v, (int) v.getTag());
    }

    public void changeState(boolean isDelete){
        this.isDelete=isDelete;
    }

    public class ViewHolder{
        public TextView delete;
        public TextView sceneName;
        public TextView apply;
    }
}

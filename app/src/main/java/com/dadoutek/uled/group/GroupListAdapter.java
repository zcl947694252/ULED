package com.dadoutek.uled.group;

import android.support.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.dadoutek.uled.R;
import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.dadoutek.uled.model.DbModel.DbGroup;

import java.util.List;

/**
 * Created by hejiajun on 2018/5/10.
 */

public class GroupListAdapter extends BaseQuickAdapter<DbGroup, BaseViewHolder> {

    public GroupListAdapter(int layoutResId, @Nullable List<DbGroup> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, DbGroup item) {

        helper.setText(R.id.tv_group_name,item.getName());

        if(item.checked){
            helper.itemView.setBackgroundColor(TelinkLightApplication.getInstance().getResources().getColor(R.color.primary));
        }else{
            helper.itemView.setBackgroundColor(TelinkLightApplication.getInstance().getResources().getColor(R.color.white));
        }
    }


//    Context context;
//    List<DbGroup> list;
//
//    public GroupListAdapter(Context context, List<DbGroup> list) {
//        this.context = context;
//        this.list = list;
//    }
//
//    @Override
//    public int getCount() {
//        return list.size();
//    }
//
//    @Override
//    public Object getItem(int position) {
//        return list.get(position);
//    }
//
//    @Override
//    public long getItemId(int position) {
//        return position;
//    }
//
//    @Override
//    public View getView(int position, View convertView, ViewGroup parent) {
//        convertView = View.inflate(context, R.layout.item_group, null);
//        TextView tvName = (TextView) convertView.findViewById(R.id.tv_group_name);
//        if (list.get(position).selected) {
//            convertView.setVisibility(View.GONE);
//        } else {
//            convertView.setVisibility(View.VISIBLE);
//            tvName.setText(list.get(position).getName());
//        }
//        return convertView;
//    }
}

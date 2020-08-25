package com.dadoutek.uled.group

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.model.dbModel.DbGroup

/**
 * Created by hejiajun on 2018/5/10.
 */

class GroupListAdapter(layoutResId: Int, data: List<DbGroup>?) : BaseQuickAdapter<DbGroup, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, item: DbGroup) {

        helper.setText(R.id.template_group_name_s, item.name)

        if(item.meshAddr==0xffff){
            helper.setText(R.id.template_group_name_s, R.string.allLight)
        }

        if (item.checked) {
            helper.itemView.setBackgroundColor(TelinkLightApplication.getApp().resources.getColor(R.color.primary))
        } else {
            helper.itemView.setBackgroundColor(TelinkLightApplication.getApp().resources.getColor(R.color.white))
        }
    }


    //    Context context;
    //    List<DbGroup> listTask;
    //
    //    public GroupListAdapter(Context context, List<DbGroup> listTask) {
    //        this.context = context;
    //        this.listTask = listTask;
    //    }
    //
    //    @Override
    //    public int getCount() {
    //        return listTask.size();
    //    }
    //
    //    @Override
    //    public Object getItem(int position) {
    //        return listTask.get(position);
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
    //        if (listTask.get(position).selected) {
    //            convertView.setVisibility(View.GONE);
    //        } else {
    //            convertView.setVisibility(View.VISIBLE);
    //            tvName.setText(listTask.get(position).getName());
    //        }
    //        return convertView;
    //    }
}

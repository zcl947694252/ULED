package com.dadoutek.uled.group

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.model.DbModel.DbGroup

/**
 * Created by hejiajun on 2018/5/10.
 */

class GroupListAdapter(layoutResId: Int, data: List<DbGroup>?) : BaseQuickAdapter<DbGroup, BaseViewHolder>(layoutResId, data) {

    override fun convert(helper: BaseViewHolder, item: DbGroup) {

        helper.setText(R.id.tv_group_name, item.name)

        if(item.meshAddr==0xffff){
            helper.setText(R.id.tv_group_name, R.string.allLight)
        }

        if (item.checked) {
            helper.itemView.setBackgroundColor(TelinkLightApplication.getInstance().resources.getColor(R.color.primary))
        } else {
            helper.itemView.setBackgroundColor(TelinkLightApplication.getInstance().resources.getColor(R.color.white))
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

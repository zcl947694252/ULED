package com.dadoutek.uled.group

import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder

import com.dadoutek.uled.R
import com.dadoutek.uled.intf.OnRecyclerviewItemClickListener
import com.dadoutek.uled.intf.OnRecyclerviewItemLongClickListener
import com.dadoutek.uled.model.DbModel.DbDeviceName
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.Group
import com.dadoutek.uled.model.Groups

class GroupNameAdapter(layoutResId: Int, data: MutableList<DbDeviceName>): BaseQuickAdapter<DbDeviceName, BaseViewHolder>(layoutResId, data){
    override fun convert(helper: BaseViewHolder?, item: DbDeviceName?) {

        helper?.setText(R.id.cw_light_text, item?.name)
//        holder.groupName.text = group.name
//        helper?.setTag(R.id.cw_light_text, )
//        holder.itemView.tag = position//给view设置tag以作为参数传递到监听回调方法中

//        if (item?.isChecked == true) {
//            //            holder.groupImage.setBackgroundResource(R.drawable.btn_rec_blue_bt);
//            helper?.setTextColor(R.id.cw_light_text, Color.parseColor("#18B4ED"))
////            item?.name.setTextColor(Color.parseColor("#18B4ED"))
//            helper?.setVisible(R.id.cw_light_image, true)
//            holder.groupImage.visibility = View.VISIBLE
//        } else {
//            holder.groupImage.visibility = View.GONE
//            holder.groupName.setTextColor(Color.parseColor("#999999"))
//        }
//
//        val param = holder.itemView.layoutParams as RecyclerView.LayoutParams
//        holder.itemView.visibility = View.VISIBLE
//        //            param.width = RecyclerView.LayoutParams.WRAP_CONTENT;
//        //            param.height = RecyclerView.LayoutParams.WRAP_CONTENT;
//        holder.itemView.layoutParams = param

        //        if (position == mGroupList.size() - 1) {
        //            holder.itemView.setVisibility(View.GONE);
        //        }
    }
//
//
//
//    operator fun get(addr: Int): Group {
//        return Groups.getInstance().getByMeshAddress(addr)
//    }
//
//
////    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
////        val view = LayoutInflater.from(parent.context).inflate(R.layout.adpter_device_name, parent, false)
////        val holder = ViewHolder(view)
////        view.setOnClickListener(this)
////        return holder
////    }
//
//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        val group = mGroupList[position]
//        holder.groupName.text = group.name
//        holder.itemView.tag = position//给view设置tag以作为参数传递到监听回调方法中
//        if (mGroupList[position].checked) {
//            //            holder.groupImage.setBackgroundResource(R.drawable.btn_rec_blue_bt);
//            holder.groupName.setTextColor(Color.parseColor("#18B4ED"))
//            holder.groupImage.visibility = View.VISIBLE
//        } else {
//            holder.groupImage.visibility = View.GONE
//            holder.groupName.setTextColor(Color.parseColor("#999999"))
//        }
//
//        val param = holder.itemView.layoutParams as RecyclerView.LayoutParams
//        holder.itemView.visibility = View.VISIBLE
//        //            param.width = RecyclerView.LayoutParams.WRAP_CONTENT;
//        //            param.height = RecyclerView.LayoutParams.WRAP_CONTENT;
//        holder.itemView.layoutParams = param
//
//        //        if (position == mGroupList.size() - 1) {
//        //            holder.itemView.setVisibility(View.GONE);
//        //        }
//    }
//
//
//    override fun getItemCount(): Int {
//        return mGroupList.size
//    }
//
//    override fun getItemViewType(position: Int): Int {
//        return super.getItemViewType(position)
//    }
//
//    override fun getItemId(position: Int): Long {
//        return super.getItemId(position)
//    }

}

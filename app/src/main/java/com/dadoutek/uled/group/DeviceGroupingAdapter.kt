package com.dadoutek.uled.group

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.TextView
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DbGroup

class DeviceGroupingAdapter(private val groupsInit: List<DbGroup>, internal var mContext: Context) : BaseAdapter() {
    private val inflater: LayoutInflater = LayoutInflater.from(mContext)

    override fun getCount(): Int {
        return groupsInit.size
    }

    override fun getItem(position: Int): DbGroup? {
        return groupsInit[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    operator fun get(addr: Int): DbGroup? {
        for (j in groupsInit.indices) {
            if (addr == groupsInit[j].meshAddr) {
                return groupsInit[j]
            }
        }
        return null
    }

    @SuppressLint("ViewHolder")
    @Deprecated("")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView: View? = inflater.inflate(R.layout.template_batch_device_item, null)

        val holder: GroupItemHolder = GroupItemHolder()

        val txtName = convertView?.findViewById<View>(R.id.template_device_batch_title) as TextView

        holder.name = txtName

        convertView.tag = holder

        val group = this.getItem(position)

        if (group != null) {
            holder.name!!.text = group.name

            if (group.checked) {
                val color = mContext.resources.getColorStateList(R.color.primary)
                holder.name!!.setTextColor(color)
            } else {
                val color = mContext.resources.getColorStateList(R.color.black)
                holder.name!!.setTextColor(color)
            }

        }

        if (position == 0 && groupsInit[position].meshAddr==0xffff) {
            val layoutParams = AbsListView.LayoutParams(1, 1)
            convertView.layoutParams = layoutParams
        }
        return convertView
    }

    private class GroupItemHolder {
        var name: TextView? = null
    }
}

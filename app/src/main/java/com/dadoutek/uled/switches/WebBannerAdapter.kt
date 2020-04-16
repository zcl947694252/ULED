package com.dadoutek.uled.switches

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.dadoutek.uled.R
import com.example.library.banner.BannerLayout


/**
 * 创建者     ZCL
 * 创建时间   2020/1/10 16:12
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */

class WebBannerAdapter( private val urlList: List<Int>) : RecyclerView.Adapter<WebBannerAdapter.MzViewHolder>() {
    private var onBannerItemClickListener: BannerLayout.OnBannerItemClickListener? = null

    fun setOnBannerItemClickListener(onBannerItemClickListener: BannerLayout.OnBannerItemClickListener) {
        this.onBannerItemClickListener = onBannerItemClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MzViewHolder {
        return MzViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_banner, parent, false))
    }

    override fun onBindViewHolder(holder: MzViewHolder, position: Int) {

        val url = urlList[position]
         holder.imageView.setImageResource(url)

        holder.imageView.setOnClickListener {
            if (onBannerItemClickListener != null) {
                onBannerItemClickListener!!.onItemClick(position)
            }
        }

    }

    override fun getItemCount(): Int {
        return urlList.size
    }


    inner class MzViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageView: ImageView = itemView.findViewById<View>(R.id.eight_switch_item_image) as ImageView
    }

}


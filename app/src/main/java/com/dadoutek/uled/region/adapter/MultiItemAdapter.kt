package com.dadoutek.uled.region.adapter
import android.annotation.SuppressLint
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.TextView
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.network.bean.RegionAuthorizeBean
import com.dadoutek.uled.region.bean.MultiRegionBean
import com.dadoutek.uled.region.bean.RegionBean


class MultiItemAdapter(data: ArrayList<MultiRegionBean>) : BaseMultiItemQuickAdapter<MultiRegionBean, BaseViewHolder>(data) {

    init {
        //就相当于再构造里面 此处没有其他意义 使用单个也可以
        addItemType(Constant.REGION_TYPE, R.layout.template_recycleview)
        addItemType(Constant.REGION_AUTHORIZE_TYPE,R.layout.template_recycleview)
    }
    @SuppressLint("StringFormatMatches", "StringFormatInvalid")
    override fun convert(helper: BaseViewHolder?, item: MultiRegionBean?) {
        var recyclerViewTitle = helper?.getView<TextView>(R.id.recycleview_title_title)
        var recyclerView = helper?.getView<RecyclerView>(R.id.recycleview_title_recycle)

        recyclerView?.layoutManager =LinearLayoutManager(mContext,LinearLayoutManager.VERTICAL,false)

        when(item!!.itemType){
            Constant.REGION_TYPE->{
                recyclerViewTitle?.text = mContext.getString(R.string.me_net_num, item.list?.size)

                val adapter = RegionDialogAdapter(R.layout.region_dialog_item, item.list as MutableList<RegionBean>)
                recyclerView?.adapter = adapter

            }
            Constant.REGION_AUTHORIZE_TYPE->{
                recyclerViewTitle?.text = mContext.getString(R.string.received_net_num, item.list?.size)

                val adapter = RegionAuthorizeDialogAdapter(R.layout.region_dialog_item, item.list as MutableList<RegionAuthorizeBean>)
                recyclerView?.adapter = adapter
            }
        }
    }


}



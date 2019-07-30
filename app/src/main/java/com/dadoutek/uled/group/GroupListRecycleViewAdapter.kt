package com.dadoutek.uled.group

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import com.chad.library.adapter.base.BaseItemDraggableAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.callback.ItemDragAndSwipeCallback
import com.chad.library.adapter.base.listener.OnItemDragListener
import com.dadoutek.uled.R
import com.dadoutek.uled.intf.MyBaseQuickAdapterOnClickListner
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.ItemTypeGroup
import com.dadoutek.uled.util.OtherUtils
import com.telink.TelinkApplication

class GroupListRecycleViewAdapter(layoutResId: Int,internal var onItemChildClickListener1 : MyBaseQuickAdapterOnClickListner,data: List<ItemTypeGroup>) :
        BaseItemDraggableAdapter<ItemTypeGroup, BaseViewHolder>(layoutResId, data){

    var recyclerViewChild : RecyclerView ?=null

    private var adapter: GroupListRecycleViewChildAdapter? = null

    override fun convert(helper: BaseViewHolder, itemTypeGroup : ItemTypeGroup?) {
        helper.setText(R.id.device_type_name,itemTypeGroup!!.name)
        if(itemTypeGroup!!.name==TelinkApplication.getInstance().getString(R.string.normal_light)){
            var num:Int?=null
            var lightNum:Int=0
            for(i in itemTypeGroup.list!!.indices){
                if(OtherUtils.isNormalGroup(itemTypeGroup.list[i])){
                    num=DBUtils.getLightByGroupID(itemTypeGroup.list[i].id).size
                    lightNum += num
                }
            }
            helper.setText(R.id.device_type_name,itemTypeGroup!!.name+"("+lightNum+")")
        }else if(itemTypeGroup!!.name==TelinkApplication.getInstance().getString(R.string.rgb_light)){
            var num:Int?=null
            var RGBNum:Int=0
            for(i in itemTypeGroup.list!!.indices){
                if(OtherUtils.isRGBGroup(itemTypeGroup.list[i])){
                    num=DBUtils.getLightByGroupID(itemTypeGroup.list[i].id).size
                    RGBNum += num
                }
            }
            helper.setText(R.id.device_type_name,itemTypeGroup!!.name+"("+RGBNum+")")
        }else if(itemTypeGroup!!.name==TelinkApplication.getInstance().getString(R.string.curtain)){
            var num:Int?=null
            var curtainNum:Int=0
            for(i in itemTypeGroup.list!!.indices){
                 if(OtherUtils.isCurtain(itemTypeGroup.list[i])){
                     num=DBUtils.getCurtainByGroupID(itemTypeGroup.list[i].id).size
                     curtainNum+=num
                 }
            }
            helper.setText(R.id.device_type_name,itemTypeGroup!!.name+"("+curtainNum+")")
        }else if(itemTypeGroup!!.name==TelinkApplication.getInstance().getString(R.string.connector)){
            var num:Int?=null
            var curtainNum:Int=0
            for(i in itemTypeGroup.list!!.indices){
                if(OtherUtils.isConnector(itemTypeGroup.list[i])){
                    num=DBUtils.getConnectorByGroupID(itemTypeGroup.list[i].id).size
                    curtainNum+=num
                }
            }
            helper.setText(R.id.device_type_name,itemTypeGroup!!.name+"("+curtainNum+")")
        }
        if(itemTypeGroup.icon!=0){
            helper.setBackgroundRes(R.id.device_img, itemTypeGroup.icon)
        }
        recyclerViewChild=helper.getView<RecyclerView>(R.id.device_type_child_group)
        val layoutmanager = LinearLayoutManager(mContext)
        layoutmanager.orientation = LinearLayoutManager.VERTICAL
        recyclerViewChild!!.layoutManager = layoutmanager
        this.adapter = GroupListRecycleViewChildAdapter(R.layout.group_item_child, itemTypeGroup!!.list)

//        val decoration = DividerItemDecoration(mContext,
//                DividerItemDecoration
//                        .VERTICAL)
//        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(mContext, R.color
//                .divider)))
        //添加分割线
////        recyclerView?.addItemDecoration(decoration)
//        recyclerViewChild?.itemAnimator = DefaultItemAnimator() as RecyclerView.ItemAnimator?

//        adapter!!.setOnItemChildClickListener { adapter, view, position ->
//            onItemChildClickListener1.onItemChildClick(adapter,view,position,helper.adapterPosition)
//        }
//        adapter!!.addFooterView(getFooterView())
        adapter!!.bindToRecyclerView(recyclerViewChild)
        setMove(recyclerViewChild!!)
    }


    private fun setMove(recyclerViewChild: RecyclerView) {
        var startPos=0
        var endPos=0
        val list = adapter!!.data
        val onItemDragListener = object : OnItemDragListener {
            override fun onItemDragStart(viewHolder: RecyclerView.ViewHolder, pos: Int) {

                startPos=pos
                endPos=0

              //"indexchange--"+"--start:"+pos)
            }

            override fun onItemDragMoving(source: RecyclerView.ViewHolder, from: Int,
                                          target: RecyclerView.ViewHolder, to: Int) {
            }

            override fun onItemDragEnd(viewHolder: RecyclerView.ViewHolder, pos: Int) {
                //                viewHolder.getItemId();
                endPos=pos
              //"indexchange--"+"--end:"+pos)

                updateGroupList(list,startPos,endPos)
              //"indexchange--"+"--start:"+startPos+"--end:"+endPos)
            }
        }

        val itemDragAndSwipeCallback = ItemDragAndSwipeCallback(adapter)
        val itemTouchHelper = ItemTouchHelper(itemDragAndSwipeCallback)
        itemTouchHelper.attachToRecyclerView(recyclerViewChild)

        adapter!!.enableDragItem(itemTouchHelper, R.id.txt_name, true)
        adapter!!.setOnItemDragListener(onItemDragListener)
    }

    private fun updateGroupList(list: MutableList<DbGroup>, startPos: Int, endPos: Int) {

        var tempIndex = list[endPos].index

        if(endPos<startPos){
            for(i in endPos..startPos){
                if(i==startPos){
                    list[i].index=tempIndex
                }else{
                    list[i].index=list[i+1].index
                }
            }
        }else if(endPos>startPos){
            for(i in endPos downTo startPos){
                if(i==startPos){
                    list[i].index=tempIndex
                }else{
                    list[i].index=list[i-1].index
                }
            }
        }

        DBUtils.updateGroupList(list)
    }
    
}

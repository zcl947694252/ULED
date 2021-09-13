package com.dadoutek.uled.group

import android.graphics.Color
import android.view.View
import android.widget.GridLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.dadoutek.uled.R
import com.dadoutek.uled.base.BaseActivity
import com.dadoutek.uled.fragment.GroupOrderAdapter
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbGroup
import kotlinx.android.synthetic.main.toolbar.*
import org.greenrobot.greendao.DbUtils
import org.jetbrains.anko.textColor
import java.util.*
import kotlin.collections.ArrayList


/**
 * 创建者     Chown
 * 创建时间   2021/9/9 19:19
 * 描述
 */
class GroupListOrderActivity: BaseActivity(), View.OnClickListener {

    private lateinit var normalTv: TextView
    private lateinit var rgbTv: TextView
    private lateinit var curtainTv: TextView
    private lateinit var relayTv: TextView
    private lateinit var orderRecycler: RecyclerView

    private var groupList: ArrayList<DbGroup> = ArrayList()
    private var currentPosition: Int = 0
    private var adapter: GroupOrderAdapter? = null

    override fun initListener() {
        normalTv.setOnClickListener(this)
        rgbTv.setOnClickListener(this)
        curtainTv.setOnClickListener(this)
        relayTv.setOnClickListener(this)
    }

    override fun initData() {
        groupList.clear()
        when(currentPosition) {
            0 -> {
                setTextColor(0)
                val allGroups = DBUtils.allGroups
                if (allGroups.size > 0)
                    groupList.add(0, allGroups[0])
                groupList.addAll(DBUtils.getGroupsByDeviceType(DeviceType.LIGHT_NORMAL))
                groupList.addAll(DBUtils.getGroupsByDeviceType(DeviceType.LIGHT_NORMAL_OLD))
            }
            1 -> {
                setTextColor(1)
                val allGroups = DBUtils.allGroups
                if (allGroups.size > 0)
                    groupList.add(0, allGroups[0])
                groupList.addAll(DBUtils.getGroupsByDeviceType(DeviceType.LIGHT_RGB))
            }
            2 -> {
                setTextColor(2)
                groupList.addAll( DBUtils.getGroupsByDeviceType(DeviceType.SMART_CURTAIN))
            }
            3 -> {
                setTextColor(3)
                groupList.addAll( DBUtils.getGroupsByDeviceType(DeviceType.SMART_RELAY))
            }
        }
        groupList.sortBy { it.index }
        adapter?.notifyDataSetChanged()
    }

    fun setTextColor(index: Int) {
        when(index) {
            0 -> {
                normalTv.textColor = getColor(R.color.blue_background)
                rgbTv.textColor = Color.GRAY
                curtainTv.textColor = Color.GRAY
                relayTv.textColor = Color.GRAY
            }
            1-> {
                normalTv.textColor = Color.GRAY
                rgbTv.textColor = getColor(R.color.blue_background)
                curtainTv.textColor = Color.GRAY
                relayTv.textColor = Color.GRAY
            }
            2 -> {
                normalTv.textColor = Color.GRAY
                rgbTv.textColor = Color.GRAY
                curtainTv.textColor = getColor(R.color.blue_background)
                relayTv.textColor = Color.GRAY
            }
            3 -> {
                normalTv.textColor = Color.GRAY
                rgbTv.textColor = Color.GRAY
                curtainTv.textColor = Color.GRAY
                relayTv.textColor = getColor(R.color.blue_background)
            }
        }
    }

    override fun initView() {
        toolbarTv.text = getString(R.string.group_title)
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener {
            var i = 0
            groupList.forEach{
                it.index = i++
                DBUtils.updateGroup(it)
            }
            finish()
        }
        initDeviceTypeNavigation()
    }

    override fun setLayoutID(): Int {
        return R.layout.layout_order_group_list
    }

    private fun initDeviceTypeNavigation() {
        normalTv = findViewById(R.id.page1)
        rgbTv = findViewById(R.id.page2)
        curtainTv = findViewById(R.id.page3)
        relayTv = findViewById(R.id.page4)
        orderRecycler = findViewById(R.id.order_group_recycler)
        orderRecycler.layoutManager = GridLayoutManager(this,2)

        adapter = GroupOrderAdapter(R.layout.template_device_type_item,groupList,false)

        adapter?.bindToRecyclerView(orderRecycler)
        helper.attachToRecyclerView(orderRecycler)
    }

    var helper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            var dragFrlg = 0
            dragFrlg = ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            return makeMovementFlags(dragFrlg, 0)
        }

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            val fromPosition = viewHolder.adapterPosition
            val toPosition = target.adapterPosition
            if (currentPosition == 0 ||currentPosition ==1) {
                if (fromPosition == 0 || toPosition == 0)
                    return false
            }
            if (fromPosition < toPosition) {
                for (i in fromPosition until toPosition) {
                    Collections.swap(groupList,i,i+1)
                }
            } else {
                for (i in fromPosition downTo toPosition + 1) {
                    Collections.swap(groupList,i,i-1)
                }
            }
            adapter?.notifyItemMoved(fromPosition,toPosition)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

        }

    })

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.page1 -> {
                currentPosition = 0
                initData()
            }
            R.id.page2 -> {
                currentPosition = 1
                initData()
            }
            R.id.page3 -> {
                currentPosition = 2
                initData()
            }

            R.id.page4 -> {
                currentPosition = 3
                initData()
            }
        }
    }
}
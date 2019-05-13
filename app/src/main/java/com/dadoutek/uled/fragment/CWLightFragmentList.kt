package com.dadoutek.uled.fragment

import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dadoutek.uled.R
import com.dadoutek.uled.group.GroupListRecycleViewChildAdapter
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGradientBody
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.othersview.BaseFragment

class CWLightFragmentList : BaseFragment() {

    private var inflater: LayoutInflater? = null

    private var recyclerView: RecyclerView? = null

    private var add_group: ConstraintLayout? = null

    private var groupAdapter: GroupListAdapter? = null

    private lateinit var groupList: ArrayList<DbGroup>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = getView(inflater)
        this.initData()
        return view
    }

    private fun getView(inflater: LayoutInflater): View {
        this.inflater = inflater
        val view = inflater.inflate(R.layout.group_list_fragment, null)
        add_group = view.findViewById(R.id.no_group)
        recyclerView = view.findViewById(R.id.group_recyclerView)

        return view
    }

    private fun initData() {
        groupList = ArrayList()

        val listAll = DBUtils.getAllGroupsOrderByIndex()
        for (group in listAll) {
            when (group.deviceType) {
                Constant.DEVICE_TYPE_LIGHT_NORMAL->{
                    groupList.add(group)
                }
                Constant.DEVICE_TYPE_DEFAULT_ALL->{
                    groupList.add(group)
                }
            }
        }

        val layoutmanager = LinearLayoutManager(activity)
        layoutmanager.orientation = LinearLayoutManager.VERTICAL
        recyclerView!!.layoutManager = layoutmanager

        this.groupAdapter = GroupListAdapter(R.layout.group_item_child, groupList)
        groupAdapter!!.bindToRecyclerView(recyclerView)
    }
}

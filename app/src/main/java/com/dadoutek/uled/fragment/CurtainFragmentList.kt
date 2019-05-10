package com.dadoutek.uled.fragment

import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.dadoutek.uled.R
import com.dadoutek.uled.group.GroupListRecycleViewAdapter
import com.dadoutek.uled.group.GroupNameAdapter
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbDeviceName
import com.dadoutek.uled.othersview.BaseFragment
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.DataManager
import kotlinx.android.synthetic.main.fragment_group_list.*

class CurtainFragmentList : BaseFragment() {

    private var inflater: LayoutInflater? = null

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


        return view
    }

    private fun initData() {

    }
}

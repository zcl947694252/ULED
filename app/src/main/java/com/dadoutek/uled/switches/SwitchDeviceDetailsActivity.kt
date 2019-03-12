package com.dadoutek.uled.switches

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbSwitch
import com.dadoutek.uled.tellink.TelinkBaseActivity
import kotlinx.android.synthetic.main.toolbar.*

class SwitchDeviceDetailsActivity : TelinkBaseActivity() {

    private var switchData:ArrayList<DbSwitch>?=null

    private var recyclerView:RecyclerView?=null

    private var adapter:SwitchDeviceDetailsAdapter?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_switch_device_details)
        initView()
        initData()
    }

    private fun initData() {
        switchData=DBUtils.getAllSwitch()
    }

    private fun initView() {
        val layoutmanager = LinearLayoutManager(this)
        recyclerView = findViewById<RecyclerView>(R.id.recycleView)
        recyclerView!!.layoutManager = layoutmanager
        val decoration = DividerItemDecoration(this!!,
                DividerItemDecoration
                        .VERTICAL)
        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this!!, R.color
                .divider)))
        recyclerView!!.addItemDecoration(decoration)
        //添加Item变化动画
        recyclerView!!.itemAnimator = DefaultItemAnimator()
        adapter = SwitchDeviceDetailsAdapter(R.layout.device_detail_adapter,switchData)
        adapter!!.bindToRecyclerView(recyclerView)

        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbar.setTitle(R.string.details)
    }
}

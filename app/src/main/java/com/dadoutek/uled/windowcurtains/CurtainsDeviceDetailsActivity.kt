package com.dadoutek.uled.windowcurtains

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.*
import com.dadoutek.uled.R
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbCurtain
import com.dadoutek.uled.tellink.TelinkBaseActivity
import kotlinx.android.synthetic.main.toolbar.*

class CurtainsDeviceDetailsActivity : TelinkBaseActivity() {

    private var curtain:ArrayList<DbCurtain>?=null

    private var recyclerView:RecyclerView?=null

    private var adapter:CurtainDeviceDetailsAdapter?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_curtains_device_details)
        initView()
        initData()
    }

    private fun initData() {
        curtain=DBUtils.getAllCurtain()
    }

    private fun initView() {
        val layoutmanager = LinearLayoutManager(this)
        recyclerView = findViewById<RecyclerView>(R.id.recycleView)
        recyclerView!!.layoutManager = GridLayoutManager(this,3)
        val decoration = DividerItemDecoration(this!!,
                DividerItemDecoration
                        .VERTICAL)
        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this!!, R.color
                .divider)))
        recyclerView!!.addItemDecoration(decoration)
        //添加Item变化动画
        recyclerView!!.itemAnimator = DefaultItemAnimator()
        adapter = CurtainDeviceDetailsAdapter(R.layout.device_detail_adapter, curtain)
        adapter!!.bindToRecyclerView(recyclerView)

        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbar.setTitle(R.string.details)
    }
}

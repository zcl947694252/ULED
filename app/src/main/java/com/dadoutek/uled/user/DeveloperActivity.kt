package com.dadoutek.uled.user

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter.OnItemChildClickListener
import com.dadoutek.uled.R
import com.dadoutek.uled.adapter.AreaItemAdapter
import com.dadoutek.uled.tellink.TelinkBaseActivity
import kotlinx.android.synthetic.main.activtiy_devloper.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.toast

class DeveloperActivity : TelinkBaseActivity() {
    var array: ArrayList<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activtiy_devloper)
        initView()
        initData()
        initListener()
    }

    private fun initListener() {

    }

    private fun initData() {
        array = ArrayList()
        array?.let {
            it.add(getString(R.string.open_blutooth))
        }

        recycleView_developer.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        val itemAdapter = AreaItemAdapter(R.layout.item_area_net, array!!)
        itemAdapter.onItemChildClickListener = OnItemChildClickListener { adapter, view, position ->
            run { toast("hhah") }
        }
        recycleView_developer.adapter = itemAdapter

    }

    private fun initView() {
        toolbar.title = getString(R.string.developer)
        toolbar.setNavigationIcon(R.drawable.icon_top_tab_back)
        toolbar.setNavigationOnClickListener {finish()}
    }

}

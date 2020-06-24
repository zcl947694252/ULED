package com.dadoutek.uled.user

import android.os.Bundle
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.util.SharedPreferencesUtils
import kotlinx.android.synthetic.main.activtiy_devloper.*
import kotlinx.android.synthetic.main.toolbar.*

class DeveloperActivity : TelinkBaseActivity() {
    var array: ArrayList<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activtiy_devloper)
        initView()
        //initData()
        initListener()
    }

    private fun initListener() {
        developer_switch_cb.setOnCheckedChangeListener { _, isChecked ->
            SharedPreferencesUtils.setDeveloperModel(isChecked)
        }
    }

    private fun initData() {
        array = ArrayList()
        array?.let {
            it.add(getString(R.string.open_blutooth))
        }

//        recycleView_developer.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
//        val itemAdapter = AreaItemAdapter(R.layout.item_area_net, array!!, dbUser.last_region_id)
//        itemAdapter.onItemChildClickListener = OnItemChildClickListener { adapter, view, position ->
//            run { toast("hhah") }
//        }
//        recycleView_developer.adapter = itemAdapter

    }

    private fun initView() {
        toolbarTv.text = getString(R.string.developer)
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener { finish() }
            developer_switch_cb.isChecked =SharedPreferencesUtils.isDeveloperModel()
    }


}

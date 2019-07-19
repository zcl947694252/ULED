package com.dadoutek.uled.othersview

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.Gravity
import android.view.View
import android.widget.PopupWindow
import com.dadoutek.uled.adapter.AreaItemAdapter
import com.dadoutek.uled.model.DbModel.DbUser
import com.dadoutek.uled.util.PopUtil
import kotlinx.android.synthetic.main.activity_network.*
import kotlinx.android.synthetic.main.popwindown_region.*
import kotlinx.android.synthetic.main.toolbar.*


class NetworkActivity : AppCompatActivity(), View.OnClickListener {
    var dbUser: DbUser? = null
    var adapter: AreaItemAdapter? = null
    var list: List<String>? = null
    var pop: PopupWindow? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.dadoutek.uled.R.layout.activity_network)
        initView()
        initData()
        initListener()
    }

    private fun initListener() {
        adapter!!.setOnItemClickListener { _, _, position -> setPop(position) }
    }

    private fun setPop(position: Int) {
        var get = list?.get(position)
        pop = PopUtil.makeMWf(this, com.dadoutek.uled.R.layout.popwindown_region)
        PopUtil.show(pop, window.decorView, Gravity.BOTTOM)
        pop_net_top.text=getString(com.dadoutek.uled.R.string.network_tip_title)
        pop_quipment_num.text = getString(com.dadoutek.uled.R.string.number)

        pop_user_net.setOnClickListener(this)
        pop_del_net.setOnClickListener(this)
        pop_share_net.setOnClickListener(this)
        pop_update_net.setOnClickListener(this)
    }

    private fun initView() {
        toolbarTv.visibility = View.VISIBLE
        toolbarTv.text = getString(com.dadoutek.uled.R.string.area)
        img_function1.visibility = View.VISIBLE
        image_bluetooth.visibility = View.VISIBLE
    }

    private fun initData() {
        list = listOf("1", "2", "3", "4")
        area_recycleview.layoutManager = LinearLayoutManager(this@NetworkActivity, LinearLayoutManager.VERTICAL, false)
        adapter = AreaItemAdapter(com.dadoutek.uled.R.layout.item_area_net, list!!)
        area_recycleview.adapter = adapter
    }


    override fun onClick(v: View?) {
        when(v!!.id){
            com.dadoutek.uled.R.id.pop_user_net->{

            }
            com.dadoutek.uled.R.id.pop_del_net->{

            }
            com.dadoutek.uled.R.id.pop_share_net->{
                getQr()
            }
            com.dadoutek.uled.R.id.pop_update_net->{

            }
        }
    }
    private fun getQr() {
    }

    override fun onDestroy() {
        super.onDestroy()
        PopUtil.dismiss(pop)
    }
}



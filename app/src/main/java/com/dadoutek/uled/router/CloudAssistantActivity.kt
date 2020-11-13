package com.dadoutek.uled.router

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.Button
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.network.ThirdPartyBean
import com.dadoutek.uled.router.CloudAssistantItemAdapter
import kotlinx.android.synthetic.main.template_recycleview.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.textColor


/**
 * 创建者     ZCL
 * 创建时间   2020/11/9 17:50
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class CloudAssistantActivity : TelinkBaseActivity() {
    var data   = mutableListOf<ThirdPartyBean>()
    val adapter = CloudAssistantItemAdapter(R.layout.group_item, data)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloud_assistant)
        initView()
        initData()
        initListener()
    }

    private fun initListener() {
        adapter.setOnItemClickListener { _, _, position ->
            updateDataToThird(position)
        }
    }

    @SuppressLint("CheckResult")
    private fun updateDataToThird(position: Int) {
        RouterModel.updateToThirdParty(data[position].id)?.subscribe({
            ToastUtils.showShort(getString(R.string.upload_data_success))
        },{
            ToastUtils.showShort(it.message)
        })
    }

    @SuppressLint("CheckResult")
    private fun initData() {
        RouterModel.appThirdParty()?.subscribe({
            if (it.isNotEmpty()){
                data=it
                adapter.notifyDataSetChanged()
            }
        },{
            ToastUtils.showShort(it.message)
        })
    }

    private fun initView() {
        toolbarTv.text = getString(R.string.cloud_assistant)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbar.setNavigationIcon(R.drawable.icon_return)
        template_recycleView.layoutManager = LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false)
        template_recycleView.adapter = adapter
        adapter.bindToRecyclerView(template_recycleView)
        var emptyView = View.inflate(this, R.layout.empty_view, null)
        val addBtn = emptyView.findViewById<Button>(R.id.add_device_btn)
        addBtn.background =null
        addBtn.text = getString(R.string.no_third_party)
        addBtn.textColor = getColor(R.color.gray)
        adapter.emptyView = emptyView
    }
}
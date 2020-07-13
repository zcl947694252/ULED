package com.dadoutek.uled.switches

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils.allGroups
import com.dadoutek.uled.model.DbModel.DBUtils.sceneList
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbScene
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.widget.RecyclerGridDecoration
import kotlinx.android.synthetic.main.template_recycleview.*
import kotlinx.android.synthetic.main.toolbar.*


/**
 * 创建者     ZCL
 * 创建时间   2020/1/13 11:53
 * 描述  单组列表选择
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class ChooseGroupOrSceneActivity : TelinkBaseActivity(), BaseQuickAdapter.OnItemClickListener {
    private var deviceType: Int = 0
    var groupList = mutableListOf<DbGroup>()
    private var sceneAdapter = SceneItemAdapter(R.layout.template_batch_device_item, sceneList)
    private var groupAdapter = GroupItemAdapter(R.layout.template_batch_device_item, groupList)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.choose_group_scene)
        initView()
        initData()
        initListener()
    }

    private fun initData() {
        //template_recycleView?.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        template_recycleView?.layoutManager = GridLayoutManager(this,5)

        type = intent.getIntExtra(Constant.EIGHT_SWITCH_TYPE, 0)
        deviceType = intent.getIntExtra(Constant.DEVICE_TYPE, 0)

        when (type) {
            0, 2 -> {
                template_recycleView?.adapter = groupAdapter
                groupAdapter.bindToRecyclerView(template_recycleView)
                toolbarTv.text = getString(R.string.select_group)
                groupList.filter { it.deviceType == deviceType.toLong() }
            }
            else -> {
                template_recycleView?.adapter = sceneAdapter
                sceneAdapter.bindToRecyclerView(template_recycleView)
                toolbarTv.text = getString(R.string.choose_scene)
            }
        }
    }

    private fun initView() {
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener { finish() }
        val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        template_recycleView?.layoutManager = linearLayoutManager
    }

    private fun initListener() {
        sceneAdapter.onItemClickListener = this
        groupAdapter.onItemClickListener = this
    }

    override fun onItemClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int) {
        if (type == 0 || type == 2)
            setResult(Activity.RESULT_OK, Intent().putExtra(Constant.EIGHT_SWITCH_TYPE, allGroups[position]))
        else
            setResult(Activity.RESULT_OK, Intent().putExtra(Constant.EIGHT_SWITCH_TYPE, sceneList[position]))
        finish()
    }
}
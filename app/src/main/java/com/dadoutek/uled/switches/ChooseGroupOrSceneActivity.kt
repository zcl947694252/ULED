package com.dadoutek.uled.switches

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils.allGroups
import com.dadoutek.uled.model.DbModel.DBUtils.sceneList
import kotlinx.android.synthetic.main.template_recycleview.*
import kotlinx.android.synthetic.main.toolbar.*


/**
 * 创建者     ZCL
 * 创建时间   2020/1/13 11:53
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class ChooseGroupOrSceneActivity : TelinkBaseActivity(), BaseQuickAdapter.OnItemClickListener {
    private var type: Int = 0
    private var sceneAdapter = SensorAdapter(R.layout.item_group, sceneList)
    private var groupAdapter = GroupItemAdapter(R.layout.item_group, allGroups)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.choose_group_scene)
        initView()
        initData()
        initListener()
    }

    private fun initData() {
        template_recycleView?.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        type = intent.getIntExtra(Constant.EIGHT_SWITCH_TYPE, 0)
        if (type == 0) {
            template_recycleView?.adapter = groupAdapter
            groupAdapter.bindToRecyclerView(template_recycleView)
            toolbarTv.text = getString(R.string.select_group)
        } else {
            template_recycleView?.adapter = sceneAdapter
            sceneAdapter.bindToRecyclerView(template_recycleView)
            toolbarTv.text = getString(R.string.select_scene)
        }
    }

    private fun initView() {
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener { finish() }
        template_recycleView?.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    }

    private fun initListener() {
        sceneAdapter.onItemClickListener = this
        groupAdapter.onItemClickListener = this
    }

    override fun onItemClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int) {
        if (type == 0)
            setResult(Activity.RESULT_OK, Intent().putExtra(Constant.EIGHT_SWITCH_TYPE, allGroups[position]))
        else
            setResult(Activity.RESULT_OK, Intent().putExtra(Constant.EIGHT_SWITCH_TYPE, sceneList[position]))
        finish()
    }
}
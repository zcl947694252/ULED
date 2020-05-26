package com.dadoutek.uled.switches

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.model.DbModel.DBUtils
import kotlinx.android.synthetic.main.template_recycleview.*
import kotlinx.android.synthetic.main.toolbar.*


/**
 * 创建者     ZCL
 * 创建时间   2020/1/2 16:58
 * 描述 选择场景
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class SelectSceneListActivity : TelinkBaseActivity() {
    private val mSceneList = DBUtils.sceneAll
    private val adpter = SceneListAdapter(R.layout.item_group, mSceneList)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.template_activity_list)
        initView()
        initData()
        initListener()
    }

    private fun initListener() {
    }

    private fun initData() {

    }

    private fun initView() {
        toolbarTv.text = getString(R.string.scene_list)
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        template_recycleView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        template_recycleView.adapter = adpter
        adpter.bindToRecyclerView(template_recycleView)
        adpter.setOnItemClickListener { _, _, position ->
            val dbScene = mSceneList[position]
            val intent = Intent()
            intent.putExtra("data", dbScene)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }
}


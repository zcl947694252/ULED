package com.dadoutek.uled.switches

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import kotlinx.android.synthetic.main.choose_group_scene.*
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
    private val noPo = 100000
    private var currentPosition: Int = noPo
    private val mSceneList = DBUtils.sceneAll
    private val adpterDevice = SceneListAdapter(R.layout.template_batch_small_item2, mSceneList)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.choose_group_scene)
        initView()
        initListener()
    }


    private fun initView() {
        toolbarTv.text = getString(R.string.scene_list)
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        template_recycleView.layoutManager = GridLayoutManager(this, 5)
        template_recycleView.adapter = adpterDevice
        adpterDevice.bindToRecyclerView(template_recycleView)
        adpterDevice.setOnItemClickListener { _, _, position ->
            currentPosition= position
            for (i in mSceneList.indices)
                mSceneList[i].isChecked = i == currentPosition
                    adpterDevice.notifyDataSetChanged()
        }
    }

    private fun initListener() {
        choose_scene_confim.setOnClickListener {
            if (currentPosition == noPo) {
                ToastUtils.showShort(getString(R.string.please_setting_least_one_scene))
                return@setOnClickListener
            }
            val dbScene = mSceneList[currentPosition]
            val intent = Intent()
            intent.putExtra("data", dbScene)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }


}


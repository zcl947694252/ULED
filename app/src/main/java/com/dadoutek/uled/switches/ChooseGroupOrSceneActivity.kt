package com.dadoutek.uled.switches

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import kotlinx.android.synthetic.main.choose_group_scene.*
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
    private var isGroup: Boolean = true
    private val noPo = 100000
    private var currentPosition: Int = noPo
    private var deviceType: Int = 0
    var groupList = mutableListOf<DbGroup>()
    var sceneList = DBUtils.sceneList
    private var sceneAdapter = SceneItemAdapter(R.layout.template_batch_small_item, sceneList)
    private var groupAdapter = GroupItemAdapter(R.layout.template_batch_small_item, groupList)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.choose_group_scene)
        initView()
        initData()
        initListener()
    }

    private fun initData() {
        val elements = DBUtils.groupList
        elements[0].deviceType = Constant.DEVICE_TYPE_NO
        DBUtils.saveGroup(elements[0], false)
        elements.removeAt(0)
        groupList.addAll(elements)


        template_recycleView?.layoutManager = GridLayoutManager(this, 5)
        val get = intent.extras.get(Constant.EIGHT_SWITCH_TYPE)
        if (get != null) type = get as Int

        val get1 = intent.extras.get(Constant.DEVICE_TYPE)
        if (get1 != null) deviceType = get1 as Int

        isGroup = type == 0 || type == 2
        when (type) {
            0, 2 -> {
                template_recycleView?.adapter = groupAdapter
                groupAdapter.bindToRecyclerView(template_recycleView)
                toolbarTv.text = getString(R.string.group_list)

                var filter: List<DbGroup> = mutableListOf()
                when (deviceType) {
                    Constant.DEVICE_TYPE_LIGHT.toInt() -> {
                        filter = groupList.filter { it.deviceType == Constant.DEVICE_TYPE_LIGHT_RGB || it.deviceType == Constant.DEVICE_TYPE_LIGHT_NORMAL||
                                it.deviceType == Constant.DEVICE_TYPE_CONNECTOR }
                    }
                    Constant.DEVICE_TYPE_LIGHT_NORMAL.toInt(), Constant.DEVICE_TYPE_LIGHT_RGB.toInt(), Constant.DEVICE_TYPE_CURTAIN.toInt(),
                    Constant.DEVICE_TYPE_CONNECTOR.toInt() -> {
                        filter = groupList.filter { it.deviceType == deviceType.toLong() }
                    }
                }
                filter.forEach {
                    it.isChecked = false
                }
                groupList.clear()
                groupList.addAll(filter)
                groupList.add(0,DBUtils.allGroups[0])
                groupAdapter?.notifyDataSetChanged()
            }
            else -> {
                template_recycleView?.adapter = sceneAdapter
                sceneAdapter.bindToRecyclerView(template_recycleView)
                toolbarTv.text = getString(R.string.scene_list)
                sceneList.forEach {
                    it.isChecked = false
                }
                sceneAdapter?.notifyDataSetChanged()
            }
        }
    }

    private fun initView() {
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener { finish() }
        // val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        //template_recycleView?.layoutManager = linearLayoutManager
    }

    private fun initListener() {
        sceneAdapter.onItemClickListener = this
        groupAdapter.onItemClickListener = this
        choose_scene_confim.setOnClickListener {
            if (currentPosition == noPo) {
                if (isGroup)
                    ToastUtils.showShort(getString(R.string.please_setting_least_one_gp))
                else
                    ToastUtils.showShort(getString(R.string.please_setting_least_one_scene))
                return@setOnClickListener
            }
            when {
                isGroup -> setResult(Activity.RESULT_OK, Intent().putExtra(Constant.EIGHT_SWITCH_TYPE, groupList[currentPosition]))
                else -> setResult(Activity.RESULT_OK, Intent().putExtra(Constant.EIGHT_SWITCH_TYPE, sceneList[currentPosition]))
            }
            groupList.forEach { it.isChecked =false }
            sceneList.forEach { it.isChecked =false }
            finish()
        }
    }

    override fun onItemClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int) {
        currentPosition = position
        if (isGroup) {
            for (i in groupList.indices)
                groupList[i].checked = i == currentPosition
            groupAdapter.notifyDataSetChanged()
        } else {
            for (i in sceneList.indices)
                sceneList[i].isChecked = i == currentPosition
            sceneAdapter.notifyDataSetChanged()
        }
    }
}
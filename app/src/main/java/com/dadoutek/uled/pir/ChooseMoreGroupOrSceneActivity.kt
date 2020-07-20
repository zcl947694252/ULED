package com.dadoutek.uled.pir

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
import com.dadoutek.uled.model.DbModel.DBUtils.groupList
import com.dadoutek.uled.model.DbModel.DBUtils.sceneList
import com.dadoutek.uled.switches.SceneMoreItemAdapter
import kotlinx.android.synthetic.main.template_recycleview.*
import kotlinx.android.synthetic.main.toolbar.*
import java.util.ArrayList


/**
 * 创建者     ZCL
 * 创建时间   2020/1/13 11:53
 * 描述 多组列表选择
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class ChooseMoreGroupOrSceneActivity : TelinkBaseActivity(), BaseQuickAdapter.OnItemClickListener {
    private var groupDatumms: kotlin.collections.ArrayList<CheckItemBean> = arrayListOf()
    private var sceneDatumms: kotlin.collections.ArrayList<CheckItemBean> = arrayListOf()
    private var sceneAdapter = SceneMoreItemAdapter(R.layout.template_batch_small_item, sceneDatumms)
    private var groupAdapter = GroupMoreItemAdapter(R.layout.template_batch_small_item, groupDatumms)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.choose_group_scene)
        initView()
        initData()
        initListener()
    }

    private fun initData() {
        sceneList.forEach {
            sceneDatumms.add(CheckItemBean(it.id, it.name, false,it.imgName))
        }
        groupList.forEach {
            groupDatumms.add(CheckItemBean(it.id, it.name, false,it.name))
        }

        //template_recycleView?.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        template_recycleView?.layoutManager = GridLayoutManager(this, 5)
        type = intent.getIntExtra(Constant.EIGHT_SWITCH_TYPE, 0)
        when (type) {
            0, 2 -> {//选群组
                template_recycleView?.adapter = groupAdapter
                groupAdapter.bindToRecyclerView(template_recycleView)
                toolbarTv.text = getString(R.string.select_group)
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
        tv_function1.text = getString(R.string.confirm)
        tv_function1.visibility =View.VISIBLE
        template_recycleView?.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    }

    private fun initListener() {
        sceneAdapter.onItemClickListener = this
        groupAdapter.onItemClickListener = this
        tv_function1.setOnClickListener {
            val intent = Intent()
            if (type == 0 || type == 2)
                intent.putParcelableArrayListExtra("data", groupDatumms.filter { it.checked } as ArrayList)
            else
                intent.putParcelableArrayListExtra("data", sceneDatumms.filter { it.checked } as ArrayList)

            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    override fun onItemClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int) {
        if (type == 0 || type == 2) {
            if (groupDatumms.filter { it.checked }.size==7 && !groupDatumms[position].checked){
                ToastUtils.showShort(getString(R.string.group_max))
            }else{
                groupDatumms[position].checked = !groupDatumms[position].checked
                groupAdapter.notifyDataSetChanged()
            }
        } else {
            if (sceneDatumms.filter { it.checked }.size==7 && !sceneDatumms[position].checked){
                ToastUtils.showShort(getString(R.string.group_max))
            }else{
                sceneDatumms[position].checked = !sceneDatumms[position].checked
                sceneAdapter.notifyDataSetChanged()
            }

        }
    }
}
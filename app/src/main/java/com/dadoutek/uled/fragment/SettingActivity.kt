package com.dadoutek.uled.fragment

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.region.adapter.SettingAdapter
import com.dadoutek.uled.region.bean.SettingItemBean
import kotlinx.android.synthetic.main.activity_mainsss.view.*
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.android.synthetic.main.toolbar.*


/**
 * 创建者     ZCL
 * 创建时间   2020/8/17 19:46
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class SettingActivity : TelinkBaseActivity() {
    val list = arrayListOf<SettingItemBean>()
    val adapter =  SettingAdapter(R.layout.item_setting, list,true)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        initView()
        initData()
        initListener()
    }

    private fun initListener() {

    }


     fun initData() {
         list.clear()
        list.add(SettingItemBean(R.drawable.icon_reset, getString(R.string.user_reset)))
        list.add(SettingItemBean(R.drawable.icon_lock, getString(R.string.safe_lock)))
        list.add(SettingItemBean(R.drawable.icon_restore_factory, getString(R.string.work_mode)))
        list.add(SettingItemBean(R.drawable.icon_restore, getString(R.string.bind_reouter)))
        list.add(SettingItemBean(R.drawable.icon_restore, getString(R.string.auxfun)))

        recycleView_setting.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        val settingAdapter = SettingAdapter(R.layout.item_setting, list)
        recycleView_setting.adapter = settingAdapter
        settingAdapter.bindToRecyclerView(recycleView_setting)

        settingAdapter.setOnItemClickListener { _, _, position ->
            val lastUser = DBUtils.lastUser
            lastUser?.let {
                if (it.id.toString() != it.last_authorizer_user_id)
                    ToastUtils.showLong(getString(R.string.author_region_warm))
                else
                    when (position) {
                      /*  0 -> startToRecoverDevices()
                        1 -> emptyTheCache()            //清除数据
                        2 -> showSureResetDialogByApp()      //qingzhi物理恢复
                        3 -> physicalRecovery()   */ //物理恢复
                        //3 -> userReset()
                        //1 -> checkNetworkAndSyncs(this)
                    }
            }
        }
        setting_masking.setOnClickListener {}
    }

    private fun initView() {
        image_bluetooth.visibility = View.VISIBLE
        toolbarTv.text = getString(R.string.setting)
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener { finish() }
        recycleView_setting.layoutManager = LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false)

    }
}
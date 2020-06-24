package com.dadoutek.uled.scene

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.util.OtherUtils
import kotlinx.android.synthetic.main.template_recycleview.*
import kotlinx.android.synthetic.main.toolbar.*


/**
 * 创建者     ZCL
 * 创建时间   2020/6/17 11:15
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class SelectSceneIconActivity : TelinkBaseActivity() {
    private  val iconList = arrayListOf(R.drawable.icon_1,R.drawable.icon_2,R.drawable.icon_3,R.drawable.icon_4,
            R.drawable.icon_5,R.drawable.icon_6,R.drawable.icon_home,R.drawable.icon_film,R.drawable.icon_from,R.drawable.icon_guest ,
            R.drawable.icon_jobs,R.drawable.icon_out ,R.drawable.icon_party ,R.drawable.icon_sleep ,R.drawable.icon_warm)

    private val iconAdapter: IconAdapter = IconAdapter(R.layout.item_icon,iconList)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_scene_icon)
        initView()
        initData()
        initListener()
    }

    fun initListener() {
        iconAdapter.setOnItemClickListener { _, _, position ->
            val resID = iconList[position]
            var intent = Intent()
            intent.putExtra("ID",resID)
            setResult(Activity.RESULT_OK,intent)
            finish()
        }
    }

    fun initData() {
        LogUtils.v("zcl-------${OtherUtils.getResourceId("icon_1",this)}----${OtherUtils.getResourceId("com.dadoutek.uled:drawable/icon_1",this)}----${R.drawable.icon_1}")
        LogUtils.v("zcl------------------${OtherUtils.getResourceName(R.drawable.icon_1,this)}----icon_1")
        template_recycleView.layoutManager = GridLayoutManager(this,3)
        template_recycleView.adapter = iconAdapter
    }

    fun initView() {
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbarTv.text = getString(R.string.select_icon)
    }

}
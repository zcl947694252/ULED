package com.dadoutek.uled.router

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.Toolbar
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.base.TelinkBaseToolbarActivity
import com.dadoutek.uled.gateway.GwLoginActivity
import com.dadoutek.uled.gateway.bean.DbRouter
import com.dadoutek.uled.model.dbModel.DBUtils
import kotlinx.android.synthetic.main.toolbar.*
import org.greenrobot.greendao.DbUtils


/**
 * 创建者     ZCL
 * 创建时间   2020/10/10 10:21
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class RouterDetailActivity : TelinkBaseToolbarActivity() {
    private var router: DbRouter? = null

    override fun batchGpVisible(): Boolean {
        batchGpAll?.title = getString(R.string.config_WIFI)
        return true
    }

    override fun skipBatch() {
        val intent = Intent(this, GwLoginActivity::class.java)
        intent.putExtra("is_router", true)
        intent.putExtra("mac", router?.macAddr?.toLowerCase())
        startActivity(intent)
        finish()
    }

    override fun setDeletePositiveBtn() {
    }

    override fun editeDeviceAdapter() {
    }

    override fun setToolbar(): Toolbar {
        return toolbar
    }

    override fun setDeviceDataSize(num: Int): Int {
        return 1
    }

    override fun setLayoutId(): Int {
        return R.layout.activity_router_detail
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initData()
        initListener()
    }

    private fun initListener() {

    }

    private fun initData() {
        val routerId = intent.getLongExtra("routerId", 1000000)
        router = DBUtils.getRouterByID(routerId)
        deleteDeviceAll?.title = router?.version
    }

    private fun initView() {
        toolbarTv.text = getString(R.string.router)
    }

    override fun editeDevice() {
        deleteDeviceAll?.title = router?.version
    }
}
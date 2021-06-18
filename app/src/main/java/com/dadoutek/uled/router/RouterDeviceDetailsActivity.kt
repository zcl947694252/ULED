package com.dadoutek.uled.router

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.appcompat.widget.*
import androidx.appcompat.widget.Toolbar
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseToolbarActivity
import com.dadoutek.uled.gateway.bean.DbRouter
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.Constant.*
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.router.adapter.RouterDeviceDetailsAdapter
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.StringUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import com.uuzuche.lib_zxing.activity.CaptureActivity
import com.uuzuche.lib_zxing.activity.CodeUtils
import kotlinx.android.synthetic.main.activity_switch_device_details.add_device_btn
import kotlinx.android.synthetic.main.activity_switch_device_details.no_device_relativeLayout
import kotlinx.android.synthetic.main.activity_switch_device_details.recycleView
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.startActivity

/**
 * 开关列表
 */
class RouterDeviceDetailsActivity : TelinkBaseToolbarActivity() {
    private val REQUEST_CODE: Int = 1000
    private var popupWindow: PopupWindow? = null
    private var popVersion: TextView? = null
    private var views: View? = null
    private var routerData: MutableList<DbRouter> = mutableListOf()
    private var adapter: RouterDeviceDetailsAdapter = RouterDeviceDetailsAdapter(R.layout.template_device_type_item, routerData, this)
    private var currentDevice: DbRouter? = null
    private var positionCurrent: Int = 10000
    private var installDevice: TextView? = null
    private var createGroup: TextView? = null
    private var createScene: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = this.intent.getIntExtra(DEVICE_TYPE, 0)
        views = LayoutInflater.from(this).inflate(R.layout.popwindown_switch, null)
        initView()
    }

    override fun onResume() {
        super.onResume()
        initData()
        makePop()
    }

    override fun skipBatch() {
        when {
            IS_ROUTE_MODE -> startActivity(Intent(this@RouterDeviceDetailsActivity, RouterTimerSceneListActivity::class.java))
            else -> ToastUtils.showShort(getString(R.string.route_cont_support_ble))
        }
    }

    override fun editeDeviceAdapter() {
        adapter!!.changeState(isEdite)
        adapter!!.notifyDataSetChanged()
    }

    override fun deleteDeviceVisible(): Boolean {
        return true
    }

    override fun setToolbar(): Toolbar {
        return toolbar
    }

    override fun onlineUpdateAllVisible(): Boolean {
        return false
    }

    override fun batchGpVisible(): Boolean {
        batchGpAll?.title = getString(R.string.timer_scene)
        return true
    }

    override fun setDeletePositiveBtn() {
        currentDevice?.let {
            RouterModel.routeDelSelf(it.macAddr)
                    ?.subscribe({ ita ->
                        LogUtils.v("zcl-----------删除路由-------$ita")
                        if (ita.errorCode == 0) {
                            ToastUtils.showShort(getString(R.string.delete_success))
                            DBUtils.deleteRouter(it)
                            //添加删除服务器接口
                            routerData.remove(it)
                            adapter.notifyDataSetChanged()
                        } else {
                            ToastUtils.showShort(getString(R.string.delete_device_fail))
                        }
                    }, {itt->
                        LogUtils.v("zcl-----------删除路由失败-------$itt")
                        ToastUtils.showShort(getString(R.string.delete_device_fail))
                    })
        }
        adapter.notifyDataSetChanged()
        isEmptyDevice()
    }

    override fun setDeviceDataSize(num: Int): Int {
        return routerData.size
    }

    override fun setLayoutId(): Int {
        return R.layout.activity_switch_device_details
    }

    private fun makePop() {
        popupWindow = PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        views?.let { itv ->
            popupWindow?.contentView = itv
            popupWindow?.isFocusable = true

            val reConfig = itv.findViewById<TextView>(R.id.switch_group)
            val ota = itv.findViewById<TextView>(R.id.ota)
            val delete = itv.findViewById<TextView>(R.id.deleteBtn)
            val rename = itv.findViewById<TextView>(R.id.rename)
            popVersion = itv.findViewById<TextView>(R.id.pop_version)
            popVersion?.text = getString(R.string.firmware_version) + currentDevice?.ble_version
            popVersion?.visibility = View.VISIBLE
            delete.text = getString(R.string.delete)
            rename.setOnClickListener {
                if (isRightPos()) return@setOnClickListener
                if (!TextUtils.isEmpty(currentDevice?.name))
                    renameEt?.setText(currentDevice?.name)
                renameEt?.setSelection(renameEt?.text.toString().length)

                if (this != null && !this.isFinishing) {
                    renameDialog?.dismiss()
                    renameDialog?.show()
                }

                renameConfirm?.setOnClickListener {    // 获取输入框的内容
                    if (StringUtils.compileExChar(renameEt?.text.toString().trim { it <= ' ' })) {
                        ToastUtils.showLong(getString(R.string.rename_tip_check))
                    } else {
                        currentDevice?.name = renameEt?.text.toString().trim { it <= ' ' }
                        DBUtils.updateRouter(currentDevice!!)
                        toolbarTv.text = currentDevice?.name
                        adapter!!.notifyDataSetChanged()
                        renameDialog.dismiss()
                    }
                }
            }
            reConfig.setOnClickListener {
                goConfig()

            }
            ota.setOnClickListener {
                if (isRightPos()) return@setOnClickListener
                if (currentDevice != null) {
                    TelinkLightService.Instance()?.idleMode(true)
                    transformView()
                } else {
                    LogUtils.d("currentDevice = $currentDevice")
                }
            }
            delete.setOnClickListener {     //恢复出厂设置
                if (isRightPos()) return@setOnClickListener
                var deleteSwitch = routerData[positionCurrent]
                AlertDialog.Builder(this).setMessage(R.string.delete_switch_confirm)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            DBUtils.deleteRouter(deleteSwitch)
                            //notifyData()
                            initData()
                            Toast.makeText(this@RouterDeviceDetailsActivity, R.string.delete_switch_success, Toast.LENGTH_LONG).show()
                        }
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show()
            }
        }
    }

    private fun goConfig() {
        if (isRightPos()) return
        if (!IS_ROUTE_MODE) {
            ToastUtils.showShort(getString(R.string.route_cont_support_ble))
            return
        }
        val intent = Intent(this@RouterDeviceDetailsActivity, RouterDetailActivity::class.java)
        intent.putExtra("routerId", currentDevice?.id)
        startActivity(intent)
        finish()
    }

    private fun isRightPos(): Boolean {
        popupWindow?.dismiss()
        if (positionCurrent == 10000) {
            ToastUtils.showShort(getString(R.string.invalid_data))
            return true
        }
        currentDevice = routerData[positionCurrent]
        return false
    }

    private fun initData() {
        routerData.clear()
        routerData.addAll(DBUtils.getAllRouter())
        setScanningMode(true)
        isEmptyDevice()
    }

    private fun addDevice() {
        val lastUser = DBUtils.lastUser
        lastUser?.let {
            if (it.id.toString() != it.last_authorizer_user_id)
                ToastUtils.showLong(getString(R.string.author_region_warm))
            else {
                var intent = Intent(this@RouterDeviceDetailsActivity, CaptureActivity::class.java)
                startActivityForResult(intent, REQUEST_CODE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE -> {  //处理扫描结果
                if (null != data) {
                    var bundle: Bundle? = data.extras ?: return
                    when {
                        bundle!!.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS -> {
                            var result = bundle.getString(CodeUtils.RESULT_STRING)
                            LogUtils.v("zcl-----------------解析路由器扫描的一维码-$result")
                            if (result != null) {
                                val intent = Intent(this@RouterDeviceDetailsActivity, RoutingNetworkActivity::class.java)
                                intent.putExtra(Constant.ONE_QR, result)
                                startActivity(intent)
                                finish()
                            } else {
                                ToastUtils.showShort(getString(R.string.qr_not_null))
                            }
                        }
                        bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_FAILED -> {
                            Toast.makeText(this, getString(R.string.fail_parse_qr), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun initView() {
        recycleView!!.layoutManager = GridLayoutManager(this, 2)
        recycleView!!.itemAnimator = DefaultItemAnimator()
        adapter.bindToRecyclerView(recycleView)
        adapter.onItemChildClickListener = onItemChildClickListener

        installDevice = findViewById(R.id.install_device)
        createGroup = findViewById(R.id.create_group)
        createScene = findViewById(R.id.create_scene)

        add_device_btn.setOnClickListener { addDevice() }
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbarTv.text = getString(R.string.router)
    }

    var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { _, view, position ->
        currentDevice = routerData?.get(position)
        positionCurrent = position

        val lastUser = DBUtils.lastUser
        lastUser?.let { it ->
            if (it.id.toString() != it.last_authorizer_user_id)
                ToastUtils.showLong(getString(R.string.author_region_warm))
            else {
                when (view.id) {
                    R.id.template_device_card_delete -> {
                        val string = getString(R.string.sure_delete_device, currentDevice?.name)
                        builder?.setMessage(string)
                        builder?.create()?.show()
                    }
                    R.id.template_device_setting -> goConfig()
                    else -> {
                    }
                }
            }
        }
    }


    private fun isEmptyDevice() {
        if (routerData.size > 0) {
            no_device_relativeLayout.visibility = View.GONE
            recycleView.visibility = View.VISIBLE
        } else {
            no_device_relativeLayout.visibility = View.VISIBLE
            recycleView.visibility = View.GONE
        }
    }


    private fun transformView() {
        startActivity<RouterOtaActivity>("deviceMeshAddress" to 100000, "deviceType" to currentDevice!!.productUUID,
                "deviceMac" to currentDevice!!.macAddr, "version" to currentDevice!!.version)
    }


    fun notifyData() {
        val mOldDatas: MutableList<DbRouter>? = routerData
        val mNewDatas: MutableList<DbRouter>? = getNewData()
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return mOldDatas?.get(oldItemPosition)?.id?.equals(mNewDatas?.get(newItemPosition)?.id) ?: false
            }

            override fun getOldListSize(): Int {
                return mOldDatas?.size ?: 0
            }

            override fun getNewListSize(): Int {
                return mNewDatas?.size ?: 0
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val beanOld = mOldDatas?.get(oldItemPosition)
                val beanNew = mNewDatas?.get(newItemPosition)
                return if (!beanOld?.name.equals(beanNew?.name)) {
                    return false//如果有内容不同，就返回false
                } else true
            }
        }, true)
        adapter?.let { diffResult.dispatchUpdatesTo(it) }
        routerData = mNewDatas!!
        adapter!!.setNewData(routerData)

        initView()
    }

    private fun getNewData(): MutableList<DbRouter> {
        routerData.clear()
        routerData.addAll(DBUtils.getAllRouter())
        toolbarTv.text = (currentDevice!!.name ?: "")
        return routerData
    }

}

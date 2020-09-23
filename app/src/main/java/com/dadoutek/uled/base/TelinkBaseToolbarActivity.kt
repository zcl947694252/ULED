package com.dadoutek.uled.base

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.widget.TextView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.group.BatchGroupFourDeviceActivity
import com.dadoutek.uled.group.GroupOTAListActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.tellink.TelinkLightApplication
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import org.jetbrains.anko.startActivity
import java.util.concurrent.TimeUnit

abstract class TelinkBaseToolbarActivity : TelinkBaseActivity() {
    private var bindRouter: MenuItem? = null
    lateinit var dialogTv: TextView
    var builder: AlertDialog.Builder? = null
    var dialogDelete: AlertDialog? = null
    var deleteDeviceAll: MenuItem? = null
    private var onlineUpdateAll: MenuItem? = null
    private var batchGpAll: MenuItem? = null
    public var disposableRouteTimer: Disposable?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(setLayoutId())
        initTool(setToolbar())
        makeDeleteDialog()
    }

    private fun initTool(toolbar: Toolbar) {
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener { finish() }
        val moreIcon = ContextCompat.getDrawable(toolbar.context, R.drawable.abc_ic_menu_overflow_material)
        if (moreIcon != null) {
            moreIcon.setColorFilter(ContextCompat.getColor(toolbar.context, R.color.black), PorterDuff.Mode.SRC_ATOP)
            toolbar.overflowIcon = moreIcon
        }

        toolbar.inflateMenu(R.menu.menu_rgb_group_setting)
        batchGpAll = toolbar.menu?.findItem(R.id.toolbar_batch_gp)
        onlineUpdateAll = toolbar.menu?.findItem(R.id.toolbar_on_line)
        deleteDeviceAll = toolbar.menu?.findItem(R.id.toolbar_delete_device)
        bindRouter = toolbar.menu?.findItem(R.id.toolbar_bind_router)

        batchGpAll?.title = getString(R.string.batch_group)
        onlineUpdateAll?.title = getString(R.string.online_upgrade)
        deleteDeviceAll?.title = getString(R.string.edite_device)

        batchGpAll?.isVisible = gpAllVisible()
        bindRouter?.isVisible = bindRouterVisible()

        toolbar.setOnMenuItemClickListener { itm ->
            DBUtils.lastUser?.let {
                when {
                    it.id.toString() != it.last_authorizer_user_id -> ToastUtils.showLong(getString(R.string.author_region_warm))
                    else ->
                        if (setDeviceDataSize(0) > 0)
                            when (itm.itemId) {
                                R.id.toolbar_batch_gp -> skipBatch()
                                R.id.toolbar_on_line -> goOta()
                                R.id.toolbar_delete_device -> editeDevice()
                                R.id.toolbar_bind_router -> bindDeviceRouter()
                            }
                        else
                            ToastUtils.showShort(getString(R.string.no_device))
                }
            }
            true
        }
    }

    open fun bindDeviceRouter() {

    }

    open fun bindRouterVisible(): Boolean {
        return false
    }

    abstract fun gpAllVisible(): Boolean

    private fun skipBatch() {
        val lastUser = DBUtils.lastUser
        lastUser?.let {
            if (it.id.toString() != it.last_authorizer_user_id)
                ToastUtils.showLong(getString(R.string.author_region_warm))
            else {
                if (TelinkLightApplication.getApp().connectDevice != null ||Constant.IS_ROUTE_MODE) {
                    val intent = Intent(this, BatchGroupFourDeviceActivity::class.java)
                    when (type) {
                        Constant.INSTALL_NORMAL_LIGHT -> intent.putExtra(Constant.DEVICE_TYPE, DeviceType.LIGHT_NORMAL)
                        Constant.INSTALL_RGB_LIGHT -> intent.putExtra(Constant.DEVICE_TYPE, DeviceType.LIGHT_RGB)
                        Constant.INSTALL_CURTAIN -> intent.putExtra(Constant.DEVICE_TYPE, DeviceType.SMART_CURTAIN)
                        Constant.INSTALL_CONNECTOR -> intent.putExtra(Constant.DEVICE_TYPE, DeviceType.SMART_RELAY)
                    }
                    startActivity(intent)
                } else autoConnectAll()
            }
        }
    }


    private fun goOta() {
            when (type) {
                Constant.INSTALL_NORMAL_LIGHT -> {
                    if (DBUtils.getAllNormalLight().size == 0)
                        ToastUtils.showShort(getString(R.string.no_device))
                    else
                        startActivity<GroupOTAListActivity>("DeviceType" to DeviceType.LIGHT_NORMAL)
                }

                Constant.INSTALL_RGB_LIGHT -> {
                    if (DBUtils.getAllRGBLight().size == 0)
                        ToastUtils.showShort(getString(R.string.no_device))
                    else
                        startActivity<GroupOTAListActivity>("DeviceType" to DeviceType.LIGHT_RGB)
                }
                Constant.INSTALL_CURTAIN -> {
                    if (DBUtils.getAllCurtains().size == 0)
                        ToastUtils.showShort(getString(R.string.no_device))
                    else
                        startActivity<GroupOTAListActivity>("DeviceType" to DeviceType.SMART_CURTAIN)
                }
                Constant.INSTALL_CONNECTOR -> {
                    if (DBUtils.getAllRelay().size == 0)
                        ToastUtils.showShort(getString(R.string.no_device))
                    else
                        startActivity<GroupOTAListActivity>("DeviceType" to DeviceType.SMART_RELAY)
                }
                Constant.INSTALL_SWITCH -> {
                    if (DBUtils.getAllSwitch().size == 0)
                        ToastUtils.showShort(getString(R.string.no_device))
                    else
                        startActivity<GroupOTAListActivity>("DeviceType" to DeviceType.NORMAL_SWITCH)
                }
                Constant.INSTALL_SENSOR -> {
                    if (DBUtils.getAllSensor().size == 0)
                        ToastUtils.showShort(getString(R.string.no_device))
                    else
                        startActivity<GroupOTAListActivity>("DeviceType" to DeviceType.SENSOR)
                }
                Constant.INSTALL_GATEWAY -> {
                    if (DBUtils.getAllGateWay().size == 0)
                        ToastUtils.showShort(getString(R.string.no_device))
                    else
                        startActivity<GroupOTAListActivity>("DeviceType" to DeviceType.GATE_WAY)
                }
            }
     }

    private fun editeDevice() {
        isEdite = !isEdite
        if (isEdite)
            deleteDeviceAll?.title = getString(R.string.cancel_edite)
        else
            deleteDeviceAll?.title = getString(R.string.edite_device)

        DBUtils.lastUser?.let {
            if (it.id.toString() != it.last_authorizer_user_id)
                ToastUtils.showLong(getString(R.string.author_region_warm))
            else {
                editeDeviceAdapter()
            }
        }
    }

    private fun makeDeleteDialog() {
        builder = AlertDialog.Builder(this)
        //dialogTv.setText(getString(R.string.sure_delete_device))
        builder?.setPositiveButton(getString(android.R.string.ok)) { _, _ ->
            setPositiveBtn()
        }
        builder?.setNegativeButton(getString(R.string.cancel)) { _, _ -> }
    }

    @SuppressLint("CheckResult")
    open fun routeOpenOrClose(meshAddr: Int,productUUID: Int, status: Int,serId:String) {//如果发送后失败则还原
        RouterModel.routeOpenOrClose(meshAddr, productUUID, status,serId)?.subscribe({
            LogUtils.v("zcl-----------收到路由成功-------$it")
            //    "errorCode": 90018,该设备不存在，请重新刷新数据"
            //    "errorCode": 90008,该设备没有绑定路由，无法操作"
            //   "errorCode": 90007该组不存在，请重新刷新数据"
            //    errorCode": 90005,"message": "该设备绑定的路由没在线"
            when (it.errorCode) {
                0 -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    disposableRouteTimer?.dispose()
                    disposableRouteTimer = Observable.timer(1500, TimeUnit.MILLISECONDS)
                            .subscribe {
                                hideLoadingDialog()
                                ToastUtils.showShort(getString(R.string.open_faile))
                            }
                }
                90018 -> ToastUtils.showShort(getString(R.string.device_not_exit))
                90008 -> ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                90007 -> ToastUtils.showShort(getString(R.string.gp_not_exit))
                90005 -> ToastUtils.showShort(getString(R.string.router_offline))
            }
        }, {
            LogUtils.v("zcl-----------收到路由失败-------$it")
            ToastUtils.showShort(it.message)
        })
    }

    abstract fun setPositiveBtn()
    abstract fun editeDeviceAdapter()
    abstract fun setToolbar(): Toolbar
    abstract fun setDeviceDataSize(num: Int): Int
    abstract fun setLayoutId(): Int

    override fun onDestroy() {
        super.onDestroy()
        disposableRouteTimer?.dispose()
    }
}
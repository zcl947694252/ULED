package com.dadoutek.uled.base

import android.app.AlertDialog
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.widget.TextView
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.group.BatchGroupFourDeviceActivity
import com.dadoutek.uled.group.GroupOTAListActivity
import com.dadoutek.uled.model.Constants
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.tellink.TelinkLightApplication
import org.jetbrains.anko.startActivity

abstract class TelinkBaseToolbarActivity : TelinkBaseActivity() {
    var bindRouter: MenuItem? = null
    lateinit var dialogTv: TextView
    var builder: AlertDialog.Builder? = null
    var dialogDelete: AlertDialog? = null
    open var deleteDeviceAll: MenuItem? = null
    private var onlineUpdateAll: MenuItem? = null
    open var batchGpAll: MenuItem? = null
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

        batchGpAll?.isVisible = batchGpVisible()
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

    abstract fun batchGpVisible(): Boolean

    open fun skipBatch() {
        val lastUser = DBUtils.lastUser
        lastUser?.let {
            if (it.id.toString() != it.last_authorizer_user_id)
                ToastUtils.showLong(getString(R.string.author_region_warm))
            else {
                if (TelinkLightApplication.getApp().connectDevice != null || Constants.IS_ROUTE_MODE) {
                    val intent = Intent(this, BatchGroupFourDeviceActivity::class.java)
                    when (type) {
                        Constants.INSTALL_NORMAL_LIGHT -> intent.putExtra(Constants.DEVICE_TYPE, DeviceType.LIGHT_NORMAL)
                        Constants.INSTALL_RGB_LIGHT -> intent.putExtra(Constants.DEVICE_TYPE, DeviceType.LIGHT_RGB)
                        Constants.INSTALL_CURTAIN -> intent.putExtra(Constants.DEVICE_TYPE, DeviceType.SMART_CURTAIN)
                        Constants.INSTALL_CONNECTOR -> intent.putExtra(Constants.DEVICE_TYPE, DeviceType.SMART_RELAY)
                    }
                    startActivity(intent)
                } else autoConnectAll()
            }
        }
    }


    open fun goOta() {
            when (type) {
            Constants.INSTALL_NORMAL_LIGHT -> {
                when (DBUtils.getAllNormalLight().size) {
                    0 -> ToastUtils.showShort(getString(R.string.no_device))
                    else -> startActivity<GroupOTAListActivity>("DeviceType" to DeviceType.LIGHT_NORMAL)
                }
            }

            Constants.INSTALL_RGB_LIGHT -> {
                when (DBUtils.getAllRGBLight().size) {
                    0 -> ToastUtils.showShort(getString(R.string.no_device))
                    else -> startActivity<GroupOTAListActivity>("DeviceType" to DeviceType.LIGHT_RGB)
                }
            }
            Constants.INSTALL_CURTAIN -> {
                when (DBUtils.getAllCurtains().size) {
                    0 -> ToastUtils.showShort(getString(R.string.no_device))
                    else -> startActivity<GroupOTAListActivity>("DeviceType" to DeviceType.SMART_CURTAIN)
                }
            }
            Constants.INSTALL_CONNECTOR -> {
                when (DBUtils.getAllRelay().size) {
                    0 -> ToastUtils.showShort(getString(R.string.no_device))
                    else -> startActivity<GroupOTAListActivity>("DeviceType" to DeviceType.SMART_RELAY)
                }
            }
            Constants.INSTALL_SWITCH -> {
                when (DBUtils.getAllSwitch().size) {
                    0 -> ToastUtils.showShort(getString(R.string.no_device))
                    else -> startActivity<GroupOTAListActivity>("DeviceType" to DeviceType.NORMAL_SWITCH)
                }
            }
            Constants.INSTALL_SENSOR -> {
                when (DBUtils.getAllSensor().size) {
                    0 -> ToastUtils.showShort(getString(R.string.no_device))
                    else -> startActivity<GroupOTAListActivity>("DeviceType" to DeviceType.SENSOR)
                }
            }
            Constants.INSTALL_GATEWAY -> {//路由不支持网关
                when {
                    DBUtils.getAllGateWay().size == 0 -> ToastUtils.showShort(getString(R.string.no_device))
                    Constants.IS_ROUTE_MODE -> ToastUtils.showShort(getString(R.string.dissupport))
                    else -> startActivity<GroupOTAListActivity>("DeviceType" to DeviceType.GATE_WAY)
                }
            }
            Constants.INSTALL_ROUTER -> {
                when {
                    DBUtils.getAllRouter().size == 0 -> ToastUtils.showShort(getString(R.string.no_device))
                   Constants.IS_ROUTE_MODE -> ToastUtils.showShort(getString(R.string.dissupport_gp_ota))
                    else -> startActivity<GroupOTAListActivity>("DeviceType" to DeviceType.ROUTER)
                }
            }
        }
    }

    open fun editeDevice() {
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
            setDeletePositiveBtn()
        }
        builder?.setNegativeButton(getString(R.string.cancel)) { _, _ -> }
    }

    abstract fun setDeletePositiveBtn()
    abstract fun editeDeviceAdapter()
    abstract fun setToolbar(): Toolbar
    abstract fun setDeviceDataSize(num: Int): Int
    abstract fun setLayoutId(): Int

    override fun onDestroy() {
        super.onDestroy()
        disposableRouteTimer?.dispose()
    }
}
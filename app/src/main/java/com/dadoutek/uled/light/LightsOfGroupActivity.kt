package com.dadoutek.uled.light

import android.app.AlertDialog
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.util.DiffUtil
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.view.MenuItem
import android.view.View
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.group.BatchGroupFourDeviceActivity
import com.dadoutek.uled.group.GroupOTAListActivity

import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.model.dbModel.DbLight
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.rgb.RGBSettingActivity
import com.dadoutek.uled.router.BindRouterActivity
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.light.ConnectionStatus
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_lights_of_group.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.startActivity
import java.util.*

/**
 * 创建者     zcl
 * 创建时间   2019/8/30 18:46
 * 描述	      ${点击组名跳转搜索设备}$
 *
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   显示X组里所有灯的页面
 */

class LightsOfGroupActivity : TelinkBaseActivity(), SearchView.OnQueryTextListener {
    private var isOpen: Int = 0
    private var bindRouter: MenuItem? = null
    private var deviceType: Int? = 0
    private var deleteDevice: MenuItem? = null
    private var onlineUpdate: MenuItem? = null
    private var batchGp: MenuItem? = null
    private val REQ_LIGHT_SETTING: Int = 0x01
    private var group: DbGroup? = null
    private var lightList: MutableList<DbLight> = mutableListOf()
    private var deviceAdapter: LightsOfGroupRecyclerViewAdapter? = LightsOfGroupRecyclerViewAdapter(R.layout.template_device_type_item, lightList)
    private var positionCurrent: Int = 0
    private var currentLight: DbLight? = null
    private var searchView: SearchView? = null
    private var canBeRefresh = true
    private var mConnectDisposal: Disposable? = null
    private var mScanDisposal: Disposable? = null
    private var mScanTimeoutDisposal: Disposable? = null
    private var mCheckRssiDisposal: Disposable? = null
    private var mNotFoundSnackBar: Snackbar? = null
    private var acitivityIsAlive = true
    private var recyclerView: RecyclerView? = null
    private var isDelete: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lights_of_group)
        initToolbar()
        initData()
        initView()
        initOnLayoutListener()
    }

    private fun addDevice() {
        val intent = Intent(this, BatchGroupFourDeviceActivity::class.java)
        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD -> intent.putExtra(Constant.DEVICE_TYPE, DeviceType.LIGHT_NORMAL)
            DeviceType.LIGHT_RGB -> intent.putExtra(Constant.DEVICE_TYPE, DeviceType.LIGHT_RGB)
        }
        startActivity(intent)
    }

    private fun initToolbar() {
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setNavigationIcon(R.drawable.icon_return)
        val moreIcon = ContextCompat.getDrawable(toolbar.context, R.drawable.abc_ic_menu_overflow_material)
        if (moreIcon != null) {
            moreIcon.setColorFilter(ContextCompat.getColor(toolbar.context, R.color.black), PorterDuff.Mode.SRC_ATOP)
            toolbar.overflowIcon = moreIcon
        }
        setMenu()
    }

    private fun skipeBatch() {
        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD -> {
                when {
                    DBUtils.getAllNormalLight().size == 0 -> ToastUtils.showShort(getString(R.string.no_device))
                    TelinkLightApplication.getApp().connectDevice != null || Constant.IS_ROUTE_MODE ->
                        startActivity<BatchGroupFourDeviceActivity>(Constant.DEVICE_TYPE to DeviceType.LIGHT_NORMAL, "gp" to group?.meshAddr)
                    else -> ToastUtils.showShort(getString(R.string.connect_fail))
                }
            }

            DeviceType.LIGHT_RGB -> {
                when {
                    DBUtils.getAllRGBLight().size == 0 -> ToastUtils.showShort(getString(R.string.no_device))
                    TelinkLightApplication.getApp().connectDevice != null || Constant.IS_ROUTE_MODE ->
                        startActivity<BatchGroupFourDeviceActivity>(Constant.DEVICE_TYPE to DeviceType.LIGHT_RGB, "gp" to group?.meshAddr)
                    else -> ToastUtils.showShort(getString(R.string.connect_fail))
                }
            }
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        searchView!!.clearFocus()
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        if (newText != null && newText.isNotEmpty()) {
            filter(newText, true)
            deviceAdapter!!.notifyDataSetChanged()
        } else {
            filter(newText, false)
            deviceAdapter!!.notifyDataSetChanged()
        }

        return false
    }

    private fun filter(groupName: String?, isSearch: Boolean) {
        val list = DBUtils.groupList
        if (lightList != null && lightList.size > 0)
            lightList.clear()

        when {
            isSearch -> for (i in list.indices)
                if (groupName == list[i].name || (list[i].name).startsWith(groupName!!))
                    lightList.addAll(DBUtils.getLightByGroupID(list[i].id))
            else -> {
                for (i in list.indices)
                    if (list[i].meshAddr == 0xffff)
                        Collections.swap(list, 0, i)
                for (j in list.indices)
                    lightList.addAll(DBUtils.getLightByGroupID(list[j].id))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        initData()
    }

    override fun onStop() {
        super.onStop()
        TelinkLightService.Instance()?.disableAutoRefreshNotify()
    }

    override fun onDestroy() {
        super.onDestroy()
        canBeRefresh = false
        acitivityIsAlive = false
        mScanDisposal?.dispose()
        if (TelinkLightApplication.getApp().connectDevice == null) {
            TelinkLightService.Instance()?.idleMode(true)
            LeBluetooth.getInstance().stopScan()
        }
    }

    private fun initData() {
        val gp = this.intent.extras!!.get("group")
        if (gp != null) group = gp as DbGroup
        deviceType = group?.deviceType?.toInt()
        lightList.clear()
        if (group?.meshAddr == 0xffff)
            filter("", false)
        else
            lightList.addAll(DBUtils.getLightByGroupID(group!!.id))
        lightList.forEach {
            when (it.productUUID) {
                DeviceType.LIGHT_NORMAL -> it.updateIcon()
                else -> it.updateRgbIcon()
            }
        }
        deviceAdapter?.notifyDataSetChanged()
        setEmptyAndToolbarTV()
    }

    private fun setEmptyAndToolbarTV() {
        toolbarTv.text = group?.name + "(${lightList?.size})"
        if (lightList.size > 0) {
            recycler_view_lights.visibility = View.VISIBLE
            no_light_ly.visibility = View.GONE
        } else {
            recycler_view_lights.visibility = View.GONE
            no_light_ly.visibility = View.VISIBLE
        }
    }

    private fun setMenu() {
        toolbar.inflateMenu(R.menu.menu_rgb_group_setting)
        batchGp = toolbar.menu?.findItem(R.id.toolbar_batch_gp)
        onlineUpdate = toolbar.menu?.findItem(R.id.toolbar_on_line)
        deleteDevice = toolbar.menu?.findItem(R.id.toolbar_delete_device)
        bindRouter = toolbar.menu?.findItem(R.id.toolbar_bind_router)

        batchGp?.title = getString(R.string.batch_group)
        onlineUpdate?.title = getString(R.string.online_upgrade)
        deleteDevice?.title = getString(R.string.edite_device)
        bindRouter?.isVisible = true

        deleteDevice?.isVisible = true
        batchGp?.isVisible = true
        toolbar.setOnMenuItemClickListener { itm ->
            DBUtils.lastUser?.let {
                if (it.id.toString() != it.last_authorizer_user_id)
                    ToastUtils.showLong(getString(R.string.author_region_warm))
                else
                    when (itm.itemId) {
                        R.id.toolbar_batch_gp -> skipeBatch()
                        R.id.toolbar_on_line -> goOta()
                        R.id.toolbar_delete_device -> editeDevice()
                        R.id.toolbar_bind_router -> bindRouterDevice()
                    }
            }
            true
        }
    }

    private fun bindRouterDevice() {
        var intent = Intent(this, BindRouterActivity::class.java)
        intent.putExtra("group", group)
        startActivity(intent)
    }

    private fun editeDevice() {
        isDelete = !isDelete
        if (isDelete)
            deleteDevice?.title = getString(R.string.cancel_edite)
        else
            deleteDevice?.title = getString(R.string.edite_device)

        deviceAdapter?.changeState(isDelete)
        deviceAdapter?.notifyDataSetChanged()
    }

    private fun goOta() {
        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD -> if (DBUtils.getAllNormalLight().size == 0)
                ToastUtils.showShort(getString(R.string.no_device))
            else
                startActivity<GroupOTAListActivity>("group" to group!!, "DeviceType" to DeviceType.LIGHT_NORMAL)

            DeviceType.LIGHT_RGB -> if (DBUtils.getAllRGBLight().size == 0)
                ToastUtils.showShort(getString(R.string.no_device))
            else
                startActivity<GroupOTAListActivity>("group" to group!!, "DeviceType" to DeviceType.LIGHT_RGB)
        }
    }

    private fun getNewData(): MutableList<DbLight> {
        if (group?.meshAddr == 0xffff) {
            filter("", false)
        } else {
            lightList = DBUtils.getLightByGroupID(group?.id ?: 100000000000)
        }

        if (group?.meshAddr == 0xffff)
            toolbarTv.text = getString(R.string.allLight) + " (" + lightList.size + ")"
        else
            toolbarTv.text = (group?.name ?: "") + " (" + lightList.size + ")"

        return lightList
    }


    fun notifyData() {
        val mOldDatas: MutableList<DbLight>? = lightList
        val mNewDatas: MutableList<DbLight>? = getNewData()
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return mOldDatas?.get(oldItemPosition)?.id?.equals(mNewDatas?.get
                (newItemPosition)?.id) ?: false;
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
        deviceAdapter?.let { diffResult.dispatchUpdatesTo(it) }
        lightList = mNewDatas!!
        deviceAdapter?.setNewData(lightList)
    }


    private fun initView() {
        when (group?.meshAddr) {
            0xffff -> toolbarTv.text = getString(R.string.allLight) + " (" + lightList.size + ")"
            else -> toolbarTv.text = (group?.name ?: "") + " (" + lightList.size + ")"
        }
        light_add_device_btn.setOnClickListener {
            when (deviceType) {
                DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD -> if (DBUtils.getAllNormalLight().size == 0) {
                    skipeScanning(DeviceType.LIGHT_NORMAL)
                } else addDevice()

                DeviceType.LIGHT_RGB -> if (DBUtils.getAllRGBLight().size == 0) {
                    skipeScanning(DeviceType.LIGHT_RGB)
                } else addDevice()
            }
        }

        recyclerView = findViewById(R.id.recycler_view_lights)
        recyclerView!!.layoutManager = GridLayoutManager(this, 2)
        recyclerView!!.itemAnimator = DefaultItemAnimator()
        deviceAdapter!!.onItemChildClickListener = onItemChildClickListener
        deviceAdapter!!.bindToRecyclerView(recyclerView)
        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD -> {
                for (i in lightList.indices)
                    lightList[i].updateIcon()

                if (DBUtils.getAllNormalLight().size == 0) light_add_device_btn.text = getString(R.string.device_scan_scan)
            }

            DeviceType.LIGHT_RGB -> {
                for (i in lightList.indices)
                    lightList[i].updateRgbIcon()
                if (DBUtils.getAllRGBLight().size == 0) light_add_device_btn.text = getString(R.string.device_scan_scan)
            }
        }

    }

    private fun skipeScanning(type: Int) {
        intent = Intent(this, DeviceScanningNewActivity::class.java)
        intent.putExtra(Constant.DEVICE_TYPE, type)
        startActivityForResult(intent, 0)
    }


    var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        currentLight = lightList[position]
        positionCurrent = position
        when (view.id) {
            R.id.template_device_icon -> {
                canBeRefresh = true

                when (currentLight!!.connectionStatus) {
                    ConnectionStatus.OFF.value -> {
                        isOpen = 1
                        if (Constant.IS_ROUTE_MODE)
                            routeOpenOrCloseBase(currentLight!!.meshAddr, currentLight!!.productUUID, 1, "lightSw")
                        else {
                            Commander.openOrCloseLights(lightList[position]!!.meshAddr, true)
                            lightList[position]!!.connectionStatus = ConnectionStatus.ON.value
                            afterLightIcon(position)
                        }
                    }
                    else -> {
                        isOpen = 0
                        if (Constant.IS_ROUTE_MODE)
                            routeOpenOrCloseBase(currentLight!!.meshAddr, currentLight!!.productUUID, 0, "lightSw")
                        else {
                            Commander.openOrCloseLights(lightList[position]!!.meshAddr, false)
                            lightList[position]!!.connectionStatus = ConnectionStatus.OFF.value
                            afterLightIcon(position)
                        }
                    }
                }
            }
            R.id.template_device_setting -> {
                if (scanPb.visibility != View.VISIBLE) {
                    //判断是否为rgb灯
                    var intent = Intent(this@LightsOfGroupActivity, NormalSettingActivity::class.java)

                    if (currentLight?.productUUID == DeviceType.LIGHT_RGB) {
                        intent = Intent(this@LightsOfGroupActivity, RGBSettingActivity::class.java)
                        intent.putExtra(Constant.TYPE_VIEW, Constant.TYPE_LIGHT)
                    }
                    intent.putExtra(Constant.LIGHT_ARESS_KEY, currentLight)
                    intent.putExtra(Constant.GROUP_ARESS_KEY, group?.meshAddr)
                    intent.putExtra(Constant.LIGHT_REFRESH_KEY, Constant.LIGHT_REFRESH_KEY_OK)
                    startActivityForResult(intent, REQ_LIGHT_SETTING)
                } else {
                    ToastUtils.showLong(R.string.connecting_tip)
                }
            }
            R.id.template_device_card_delete -> showDeleteSingleDialog(currentLight!!)
        }
    }

    override fun tzRouterOpenOrClose(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl-----------收到路由lightSw通知-------$cmdBean")
        if (cmdBean.ser_id == "lightSw") {
            disposableRouteTimer?.dispose()
            hideLoadingDialog()
            if (cmdBean.status == 0) {
                currentLight!!.connectionStatus = isOpen
                afterLightIcon(positionCurrent)
            } else {
                if (isOpen==1)
                    ToastUtils.showShort(getString(R.string.open_light_faile))
                else
                    ToastUtils.showShort(getString(R.string.close_faile))
            }
        }
    }

    private fun afterLightIcon(position: Int) {
        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD -> lightList[position]!!.updateIcon()
            DeviceType.LIGHT_RGB -> lightList[position]!!.updateRgbIcon()
        }

        DBUtils.updateLight(lightList[position]!!)
        deviceAdapter?.notifyDataSetChanged()
    }

    private fun showDeleteSingleDialog(dbLight: DbLight) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.sure_delete_device, dbLight.name))
        TelinkLightService.Instance()?.idleMode(true)
        builder.setPositiveButton(getString(android.R.string.ok)) { _, _ ->
            /*   deletePreGroup(dbLight)
               DBUtils.updateGroup(group!!)
               dbLight.hasGroup = false
               dbLight.belongGroupId = 1
               DBUtils.updateLight(dbLight)*/
            DBUtils.deleteLight(dbLight)
            lightList.remove(dbLight)
            deviceAdapter?.notifyDataSetChanged()
            setEmptyAndToolbarTV()
        }
        builder.setNegativeButton(getString(R.string.cancel)) { _, _ -> }
        val dialog = builder.show()
        dialog.show()
    }

    private fun deletePreGroup(dbLight: DbLight) {
        if (DBUtils.getGroupByID(dbLight!!.belongGroupId!!) != null) {
            val groupAddress = DBUtils.getGroupByID(dbLight!!.belongGroupId!!)?.meshAddr
            val opcode = Opcode.SET_GROUP
            val params = byteArrayOf(0x00, (groupAddress!! and 0xFF).toByte(), (groupAddress shr 8 and 0xFF).toByte())//0x00表示删除组
            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, dbLight.meshAddr, params)
        }
    }

    private fun stopConnectTimer() {
        mConnectDisposal?.dispose()
    }

    override fun onPause() {
        super.onPause()
        mScanTimeoutDisposal?.dispose()
        mConnectDisposal?.dispose()
        mNotFoundSnackBar?.dismiss()
        //移除事件
        stopConnectTimer()
        mCheckRssiDisposal?.dispose()
    }
}


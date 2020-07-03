package com.dadoutek.uled.light

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
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.group.BatchGroupActivity
import com.dadoutek.uled.group.BatchGroupFourDeviceActivity
import com.dadoutek.uled.group.GroupOTAListActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.rgb.RGBSettingActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.DataManager
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.light.ConnectionStatus
import com.telink.bluetooth.light.DeviceInfo
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_lights_of_group.*
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.startActivity
import java.util.*
import kotlin.collections.ArrayList

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
    private var deviceType: Int? = 0
    private var deleteDevice: MenuItem? = null
    private var onlineUpdate: MenuItem? = null
    private var batchGp: MenuItem? = null
    private val REQ_LIGHT_SETTING: Int = 0x01
    private var group: DbGroup? = null
    private lateinit var lightList: MutableList<DbLight>
    private var adapter: LightsOfGroupRecyclerViewAdapter? = null
    private var positionCurrent: Int = 0
    private var currentLight: DbLight? = null
    private var searchView: SearchView? = null
    private var canBeRefresh = true
    private var connectMeshAddress: Int = 0
    private var mConnectDisposal: Disposable? = null
    private var mScanDisposal: Disposable? = null
    private var mScanTimeoutDisposal: Disposable? = null
    private var mCheckRssiDisposal: Disposable? = null
    private var mNotFoundSnackBar: Snackbar? = null
    private var acitivityIsAlive = true
    private var recyclerView: RecyclerView? = null

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
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD ->intent.putExtra(Constant.DEVICE_TYPE, DeviceType.LIGHT_NORMAL)

            DeviceType.LIGHT_RGB ->  intent.putExtra(Constant.DEVICE_TYPE, DeviceType.LIGHT_RGB)
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
    }

    private fun skipeBatch() {
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

    override fun onQueryTextSubmit(query: String?): Boolean {
        searchView!!.clearFocus()
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        if (newText != null && newText.isNotEmpty()) {
            filter(newText, true)
            adapter!!.notifyDataSetChanged()
        } else {
            filter(newText, false)
            adapter!!.notifyDataSetChanged()
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
        initToolbar()
        initData()
        initView()
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
        lightList = ArrayList()
        if (group?.meshAddr == 0xffff)
            filter("", false)
        else
            lightList = DBUtils.getLightByGroupID(group!!.id)

        setMenu()

        toolbarTv.text = group?.name + "(${group?.deviceCount})"

        if (lightList.size > 0) {
            recycler_view_lights.visibility = View.VISIBLE
            no_light.visibility = View.GONE
        } else {
            recycler_view_lights.visibility = View.GONE
            no_light.visibility = View.VISIBLE
        }
    }

    private fun setMenu() {
        if (lightList.size > 0) {
            toolbar.inflateMenu(R.menu.menu_rgb_group_setting)
            batchGp = toolbar.menu?.findItem(R.id.toolbar_delete_group)
            onlineUpdate = toolbar.menu?.findItem(R.id.toolbar_rename_group)
            deleteDevice = toolbar.menu?.findItem(R.id.toolbar_delete_device)
            batchGp?.title = getString(R.string.batch_group)
            onlineUpdate?.title = getString(R.string.online_upgrade)
            deleteDevice?.title = getString(R.string.edite_device)
            deleteDevice?.isVisible = true
            toolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.toolbar_delete_group -> skipeBatch()
                    R.id.toolbar_rename_group -> goOta()
                    R.id.toolbar_delete_device -> editeDevice()
                }
                true
            }
        }
    }

    private fun editeDevice() {

    }

    private fun goOta() {
        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD -> {
                if (DBUtils.getAllNormalLight().size == 0)
                    ToastUtils.showShort(getString(R.string.no_device))
                else
                    startActivity<GroupOTAListActivity>("DeviceType" to DeviceType.LIGHT_NORMAL)
            }

            DeviceType.LIGHT_RGB -> {
                if (DBUtils.getAllRGBLight().size == 0)
                    ToastUtils.showShort(getString(R.string.no_device))
                else
                    startActivity<GroupOTAListActivity>("DeviceType" to DeviceType.LIGHT_RGB)
            }
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
        adapter?.let { diffResult.dispatchUpdatesTo(it) }
        lightList = mNewDatas!!
        adapter?.setNewData(lightList)
    }


    private fun initView() {
        when (group?.meshAddr) {
            0xffff ->
                toolbarTv.text = getString(R.string.allLight) + " (" + lightList.size + ")"
            else ->
                toolbarTv.text = (group?.name ?: "") + " (" + lightList.size + ")"
        }
        light_add_device_btn.setOnClickListener {
            when (deviceType) {
                DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD -> if (DBUtils.getAllNormalLight().size == 0) {
                    skipeScanning(DeviceType.LIGHT_NORMAL)
                } else addDevice()

                DeviceType.LIGHT_RGB ->   if (DBUtils.getAllRGBLight().size == 0) {
                    skipeScanning(DeviceType.LIGHT_RGB)
                } else addDevice()
            }
        }


        recyclerView = findViewById(R.id.recycler_view_lights)
        recyclerView!!.layoutManager = GridLayoutManager(this, 3)
        recyclerView!!.itemAnimator = DefaultItemAnimator()
        adapter = LightsOfGroupRecyclerViewAdapter(R.layout.item_lights_of_group, lightList)
        adapter!!.onItemChildClickListener = onItemChildClickListener
        adapter!!.bindToRecyclerView(recyclerView)
        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD -> {
                for (i in lightList.indices)
                    lightList[i].updateIcon()

                when (DBUtils.getAllNormalLight().size) {
                    0 -> light_add_device_btn.text = getString(R.string.device_scan_scan)
                    else -> light_add_device_btn.text = getString(R.string.add_device)
                }
            }

            DeviceType.LIGHT_RGB -> {
                for (i in lightList.indices)
                    lightList[i].updateRgbIcon()
                when (DBUtils.getAllRGBLight().size) {
                    0 -> light_add_device_btn.text = getString(R.string.device_scan_scan)
                    else -> light_add_device_btn.text = getString(R.string.add_device)
                }
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
        val opcode = Opcode.LIGHT_ON_OFF
        when (view.id) {
            R.id.img_light -> {
                canBeRefresh = true
                if (currentLight!!.connectionStatus == ConnectionStatus.OFF.value) {
//                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, currentLight!!.meshAddr,byteArrayOf(0x01, 0x00, 0x00))
                    when (currentLight!!.productUUID) {
                        DeviceType.SMART_CURTAIN -> Commander.openOrCloseCurtain(currentLight!!.meshAddr, true, false)
                        else -> Commander.openOrCloseLights(currentLight!!.meshAddr, true)
                    }
                    currentLight!!.connectionStatus = ConnectionStatus.ON.value
                } else {
                    when (currentLight!!.productUUID) {
                        DeviceType.SMART_CURTAIN -> Commander.openOrCloseCurtain(currentLight!!.meshAddr, false, false)
                        else -> Commander.openOrCloseLights(currentLight!!.meshAddr, false)
                    }
                    currentLight!!.connectionStatus = ConnectionStatus.OFF.value
                }

                when (deviceType) {
                    DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD -> currentLight!!.updateIcon()
                    DeviceType.LIGHT_RGB -> currentLight!!.updateRgbIcon()
                }

                DBUtils.updateLight(currentLight!!)
                runOnUiThread {
                    adapter?.notifyDataSetChanged()
                }
            }
            R.id.tv_setting -> {
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
                    ToastUtils.showLong(R.string.reconnecting)
                }
            }
            R.id.iv_delete->{

            }
        }
    }

    /*   override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
           super.onActivityResult(requestCode, resultCode, data)
           notifyData()
           val isConnect = data?.getBooleanExtra("data", false) ?: false
           if (isConnect) {
               scanPb.visibility = View.VISIBLE
           }

           GlobalScope.launch {
               //踢灯后没有回调 状态刷新不及时 延时2秒获取最新连接状态
               delay(2000)
               if (this@LightsOfGroupActivity == null ||
                       this@LightsOfGroupActivity.isDestroyed ||
                       this@LightsOfGroupActivity.isFinishing || !acitivityIsAlive) {
               } else {
                   connect()?.subscribe(
                           {
                               onConnected(it)
                           },
                           {
                               LogUtils.d(it)
                           }
                   )
               }
           }
       }*/

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

    private fun onConnected(deviceInfo: DeviceInfo) {
        GlobalScope.launch(Dispatchers.Main) {
            stopConnectTimer()
            if (progressBar?.visibility != View.GONE)
                progressBar?.visibility = View.GONE
            delay(300)
        }
        this.connectMeshAddress = deviceInfo.meshAddress
        scanPb.visibility = View.GONE
        adapter?.notifyDataSetChanged()
    }


}


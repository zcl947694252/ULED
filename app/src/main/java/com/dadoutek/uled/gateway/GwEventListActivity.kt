package com.dadoutek.uled.gateway

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.widget.Toolbar
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.gateway.adapter.GwEventItemAdapter
import com.dadoutek.uled.gateway.bean.*
import com.dadoutek.uled.gateway.util.Base64Utils
import com.dadoutek.uled.gateway.util.GsonUtil
import com.dadoutek.uled.intf.OtaPrepareListner
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.Constant.*
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.httpModel.GwModel
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.GwGattBody
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.receiver.GwBrocasetReceiver
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.*
import com.telink.TelinkApplication
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.LightAdapter
import com.telink.bluetooth.light.LightService
import com.telink.util.Event
import com.telink.util.EventListener
import com.yanzhenjie.recyclerview.SwipeMenu
import com.yanzhenjie.recyclerview.SwipeMenuItem
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_event_list.*
import kotlinx.android.synthetic.main.bottom_version_ly.*
import kotlinx.android.synthetic.main.template_bottom_add_no_line.*
import kotlinx.android.synthetic.main.template_swipe_recycleview.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.*
import org.jetbrains.anko.toast
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * 创建者     ZCL
 * 创建时间   2020/3/3 14:46
 * 描述
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GwEventListActivity : TelinkBaseActivity(), BaseQuickAdapter.OnItemChildClickListener, EventListener<String> {
    private var isDelete: Boolean = false
    private var fiOta: MenuItem? = null
    private var downloadDispoable: Disposable? = null
    private var fiVersion: MenuItem? = null
    private var fiDelete: MenuItem? = null
    private var isRestSuccess: Boolean = false
    private var showDialogHardDeleteGw: androidx.appcompat.app.AlertDialog? = null
    private var fiFactoryReset: MenuItem? = null
    private var disposableFactoryTimer: Disposable? = null
    private var showDialogDelete: androidx.appcompat.app.AlertDialog? = null
    private var fiChangeGp: MenuItem? = null
    private var receiver: GwBrocasetReceiver? = null
    private lateinit var currentGwTag: GwTagBean
    private var deleteBean: GwTagBean? = null
    private var connectCount: Int = 1
    private lateinit var mApp: TelinkLightApplication
    private var listOne = mutableListOf<GwTagBean>()
    private var listTwo = mutableListOf<GwTagBean>()
    private var checkedIdType: Int = 0
    private var dbGw: DbGateway? = null
    var renameEditText: EditText? = null
    val adapter = GwEventItemAdapter(R.layout.event_item, listOne)
    private val adapter2 = GwEventItemAdapter(R.layout.event_item, listTwo)
    private val function: (leftMenu: SwipeMenu, rightMenu: SwipeMenu, position: Int) -> Unit = { _, rightMenu, _ ->
        val menuItem = SwipeMenuItem(this@GwEventListActivity)// 创建菜单
        menuItem.height = ViewGroup.LayoutParams.MATCH_PARENT
        menuItem.weight = DensityUtil.dip2px(this, 500f)
        menuItem.textSize = 20
        menuItem.setBackgroundColor(getColor(R.color.red))
        menuItem.setText(R.string.delete)
        rightMenu.addMenuItem(menuItem)//添加进右侧菜单
    }

    fun initView() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setOnMenuItemClickListener(menuItemClickListener)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setNavigationIcon(R.drawable.icon_return)
        if (TelinkLightApplication.getApp().isConnectGwBle)
            image_bluetooth.setImageResource(R.drawable.cloud)
        var moreIcon = ContextCompat.getDrawable(toolbar.context, R.drawable.abc_ic_menu_overflow_material);
        if (moreIcon != null) {
            moreIcon.setColorFilter(ContextCompat.getColor(toolbar.context, R.color.black), PorterDuff.Mode.SRC_ATOP);
            toolbar.overflowIcon = moreIcon;
        }
        toolbar.setOnClickListener { renameGw() }

        img_function1.visibility = View.GONE
        image_bluetooth.setImageResource(R.drawable.icon_bluetooth)
        image_bluetooth.visibility = View.VISIBLE
        add_group_btn_tv.text = getString(R.string.add_timing_label)
    }

    private fun renameGw() {
        renameEditText?.setText(dbGw?.name)
        renameEditText?.setSelection(renameEditText?.text.toString().length)

        if (this != null && !this.isFinishing) {
            renameDialog?.dismiss()
            renameDialog?.show()
        }

        renameConfirm?.setOnClickListener {    // 获取输入框的内容
            val trim = renameEditText?.text.toString().trim { it <= ' ' }
            if (StringUtils.compileExChar(trim)) {
                ToastUtils.showLong(getString(R.string.rename_tip_check))
            } else {
                dbGw?.name = trim
                toolbarTv.text = trim
                DBUtils.saveGateWay(dbGw!!, false)
                renameDialog?.dismiss()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        DBUtils.lastUser?.let {
            if (it.id.toString() == it.last_authorizer_user_id) {
                menuInflater.inflate(R.menu.menu_rgb_light_setting, menu)
                fiChangeGp = menu?.findItem(R.id.toolbar_fv_change_group)
                fiChangeGp?.title = getString(R.string.config_net)

                fiFactoryReset = menu?.findItem(R.id.toolbar_fv_rest)
                fiDelete = menu?.findItem(R.id.toolbar_f_delete)
                fiOta = menu?.findItem(R.id.toolbar_f_ota)

                fiOta?.isVisible = TelinkLightApplication.getApp().isConnectGwBle
                fiDelete?.isVisible = TelinkLightApplication.getApp().isConnectGwBle
                fiChangeGp?.isVisible = TelinkLightApplication.getApp().isConnectGwBle
                fiFactoryReset?.isVisible = TelinkLightApplication.getApp().isConnectGwBle

                fiVersion = menu?.findItem(R.id.toolbar_f_version)
                if (TextUtils.isEmpty(dbGw?.version))
                    dbGw?.version = getString(R.string.number_no)
                fiVersion?.title = dbGw?.version
            }
        }
        LogUtils.v("zcl------onCreateOptionsMenu------------${dbGw?.version}")
        return super.onCreateOptionsMenu(menu)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu?): Boolean {
        fiVersion?.title = dbGw?.version
        return super.onMenuOpened(featureId, menu)
    }

    val menuItemClickListener = Toolbar.OnMenuItemClickListener { item ->
        if (TelinkLightApplication.getApp().connectDevice != null) {
            when (item?.itemId) {
                R.id.toolbar_f_rename -> renameGw()
                R.id.toolbar_fv_change_group -> configNet()
                R.id.toolbar_fv_rest -> userReset()
                R.id.toolbar_f_ota -> goOta()
                R.id.toolbar_f_delete -> deleteDevice()
            }
        } else {
            if (Constant.IS_ROUTE_MODE) return@OnMenuItemClickListener true
            showLoadingDialog(getString(R.string.connecting_tip))
            connect(dbGw!!.meshAddr, true)?.subscribeOn(Schedulers.io())
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.subscribe({
                        hideLoadingDialog()
                        ToastUtils.showShort(getString(R.string.connect_success))
                    }, {
                        hideLoadingDialog()
                        ToastUtils.showShort(getString(R.string.connect_fail))
                    })
        }
        true
    }

    @SuppressLint("CheckResult")
    private fun goOta() {
        showLoadingDialog(getString(R.string.please_wait))
        if (TelinkApplication.getInstance().connectDevice != null) {
            downloadDispoable = Commander.getDeviceVersion(dbGw!!.meshAddr)
                    .subscribe(
                            { s: String ->
                                dbGw!!.version = s
                                DBUtils.saveGateWay(dbGw!!, false)
                                var isBoolean: Boolean = SharedPreferencesHelper.getBoolean(TelinkLightApplication.getApp(), IS_DEVELOPER_MODE, false)
                                if (isBoolean) {
                                    transformView()
                                } else {
                                    if (OtaPrepareUtils.instance().checkSupportOta(s)!!)
                                        OtaPrepareUtils.instance().gotoUpdateView(this@GwEventListActivity, s, otaPrepareListner)
                                    else
                                        ToastUtils.showShort(getString(R.string.version_disabled))
                                }
                                hideLoadingDialog()
                            }, {
                        hideLoadingDialog()
                        ToastUtils.showLong(getString(R.string.get_version_fail))
                    }
                    )
        } else {
            ToastUtils.showShort(getString(R.string.connect_fail))
            finish()
        }
    }

    private var otaPrepareListner: OtaPrepareListner = object : OtaPrepareListner {

        override fun downLoadFileStart() {
            showLoadingDialog(getString(R.string.get_update_file))
        }

        override fun startGetVersion() {
            showLoadingDialog(getString(R.string.verification_version))
        }

        override fun getVersionSuccess(s: String) {
            hideLoadingDialog()
        }

        override fun getVersionFail() {
            ToastUtils.showLong(R.string.verification_version_fail)
            hideLoadingDialog()
        }


        override fun downLoadFileSuccess() {
            hideLoadingDialog()
            transformView()
        }

        override fun downLoadFileFail(message: String) {
            hideLoadingDialog()
            ToastUtils.showLong(R.string.download_pack_fail)
        }
    }

    private fun transformView() {
        disableConnectionStatusListener()
        if (TelinkLightApplication.getApp().connectDevice != null && TelinkLightApplication.getApp().connectDevice.meshAddress == dbGw?.meshAddr) {
            val intent = Intent(this@GwEventListActivity, OTAUpdateActivity::class.java)
            intent.putExtra(UPDATE_LIGHT, dbGw)
            intent.putExtra(OTA_MES_Add, dbGw?.meshAddr ?: 0)
            intent.putExtra(OTA_MAC, dbGw?.macAddr)
            intent.putExtra(OTA_VERSION, dbGw?.version)
            intent.putExtra(OTA_TYPE, DeviceType.GATE_WAY)

            startActivity(intent)
        }
    }

    private fun deleteDevice() {
        //恢复出厂设置
        showDialogDelete = androidx.appcompat.app.AlertDialog.Builder(this).setMessage(R.string.sure_delete_device2)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    hardTimer()
                    showLoadingDialog(getString(R.string.please_wait))
                    sendGwResetFactory(0)////恢复出厂设置
                    SyncDataPutOrGetUtils.syncGetDataStart(DBUtils.lastUser!!, syncCallbackGet)
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
    }

    private fun hardTimer() {
        disposableFactoryTimer?.dispose()
        disposableFactoryTimer = Observable.timer(15000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    hideLoadingDialog()
                    CoroutineScope(Dispatchers.Main).launch {
                        showDialogDelete?.dismiss()
                        showDialogHardDeleteGw = androidx.appcompat.app.AlertDialog.Builder(this@GwEventListActivity).setMessage(R.string.delete_device_hard_tip)
                                .setPositiveButton(android.R.string.ok) { _, _ ->
                                    GlobalScope.launch(Dispatchers.Main) {
                                        showLoadingDialog(getString(R.string.please_wait))
                                    }
                                    showDialogHardDelete?.dismiss()
                                    isRestSuccess = true
                                    deleteGwData(isRestSuccess)
                                }
                                .setNegativeButton(R.string.btn_cancel, null)
                                .show()
                    }
                }
    }

    private fun deleteGwData(restSuccess: Boolean = false) {
        if (TelinkLightApplication.getApp().mesh.removeDeviceByMeshAddress(dbGw!!.meshAddr))
            TelinkLightApplication.getApp().mesh.saveOrUpdate(this)

        DBUtils.deleteGateway(dbGw!!)
        val gattBody = GwGattBody()
        gattBody.idList = mutableListOf(dbGw!!.id.toInt())

        GwModel.deleteGwList(gattBody)?.subscribe(object : NetworkObserver<String?>() {
            override fun onNext(t: String) {
                LogUtils.v("zcl-----网关删除成功返回-------------$t")
                GlobalScope.launch(Dispatchers.Main) {
                    if (restSuccess)
                        delay(10000)
                    Toast.makeText(this@GwEventListActivity, R.string.delete_switch_success, Toast.LENGTH_LONG).show()
                    hideLoadingDialog()
                    finish()
                }
            }

            override fun onError(e: Throwable) {
                super.onError(e)
                ToastUtils.showShort(e.message)
                hideLoadingDialog()
                //清除网关的时候。已经从数据库删除了数据，此处也需要从数据库重新拿数据，更新到UI
                LogUtils.v("zcl-----网关删除成功返回-------------${e.message}")
            }
        })
    }

    private fun userReset() {
        showDialogDelete = androidx.appcompat.app.AlertDialog.Builder(this).setMessage(R.string.user_reset_tip)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    disposableFactoryTimer?.dispose()
                    disposableFactoryTimer = Observable.timer(15000, TimeUnit.MILLISECONDS)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                hideLoadingDialog()
                                ToastUtils.showShort(getString(R.string.user_reset_faile))
                            }
                    GlobalScope.launch(Dispatchers.Main) {
                        showLoadingDialog(getString(R.string.please_wait))
                    }
                    if (TelinkLightApplication.getApp().connectDevice != null) {
                        sendGwResetFactory(1)//用户恢复   sendGwResetFactory(0)//恢复出厂设置
                    } else {
                        ToastUtils.showShort(getString(R.string.connect_fail))
                        finish()
                    }
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
    }

    private fun sendGwResetFactory(frist: Int) {
        isDelete = true
        var labHeadPar = byteArrayOf(frist.toByte(), 0, 0, 0, 0, 0, 0, 0)
        TelinkLightService.Instance()?.sendCommandResponse(Opcode.CONFIG_GW_REST_FACTORY, dbGw?.meshAddr ?: 0, labHeadPar, "1")
    }

    private fun configNet() {
        //重新配置  intent = Intent(this@GwDeviceDetailActivity, GwEventListActivity::class.java)
        var intent = Intent(this@GwEventListActivity, GwLoginActivity::class.java)
        SharedPreferencesHelper.putBoolean(this, IS_GW_CONFIG_WIFI, true)
        intent.putExtra("data", dbGw)
        startActivity(intent)
    }

    private fun makePopuwindow() {
        popReNameView = View.inflate(this, R.layout.pop_rename, null)
        renameEditText = popReNameView?.findViewById(R.id.pop_rename_edt)
        renameCancel = popReNameView?.findViewById(R.id.pop_rename_cancel)
        renameConfirm = popReNameView?.findViewById(R.id.pop_rename_confirm)

        renameDialog = Dialog(this)
        renameDialog!!.setContentView(popReNameView!!)
        renameDialog!!.setCanceledOnTouchOutside(false)

        renameCancel?.setOnClickListener {
            renameDialog?.dismiss()
        }
        renameConfirm?.setOnClickListener {
            renameDialog?.dismiss()
            dbGw?.name = renameEditText?.text.toString()
            DBUtils.saveGateWay(dbGw!!, false)
        }
    }


    override fun onResume() {
        super.onResume()
        this.mApp.removeEventListeners()
        this.mApp.addEventListener(DeviceEvent.STATUS_CHANGED, this)
    }

    override fun onPause() {
        super.onPause()
        this.mApp.removeEventListener(DeviceEvent.STATUS_CHANGED, this)
    }

    private fun retryConnect() {
        connect(macAddress = dbGw!!.macAddr, fastestMode = true)?.subscribe(
                object : NetworkObserver<DeviceInfo?>() {
                    override fun onNext(t: DeviceInfo) {
                        TmtUtils.midToast(this@GwEventListActivity, getString(R.string.config_success))
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.icon_bluetooth)
                    }

                    override fun onError(e: Throwable) {
                        super.onError(e)
                        CoroutineScope(Dispatchers.Main).launch {
                            TmtUtils.midToast(this@GwEventListActivity, getString(R.string.connect_fail))
                        }
                        //DBUtils.saveGateWay(dbGw!!, true)
                        finish()
                    }
                }
        )
    }

    private fun deleteSuceess() {
        runOnUiThread {
            hideLoadingDialog()
            disposableTimer?.dispose()

            if (dbGw?.type == 0) {//定时
                listOne.remove(deleteBean)
                dbGw?.tags = GsonUtils.toJson(listOne)//赋值时一定要转换为gson字符串
                isShowAdd(listOne)
            } else {
                listTwo.remove(deleteBean)
                dbGw?.timePeriodTags = GsonUtils.toJson(listTwo)//赋值时一定要转换为gson字符串
                isShowAdd(listTwo)
            }

            DBUtils.saveGateWay(dbGw!!, true)
            addGw(dbGw!!)//添加网关就是更新网关

            adapter.notifyDataSetChanged()
            adapter2.notifyDataSetChanged()
        }
    }

    /**
     * 蓝牙直连时发送时区信息
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendTimeZoneParmars() {
        showLoadingDialog(getString(R.string.please_wait))
        disposableTimer?.dispose()
        disposableTimer = Observable.timer(2000, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    hideLoadingDialog()
                    runOnUiThread { ToastUtils.showLong(getString(R.string.get_time_zone_fail)) }
                    DBUtils.saveGateWay(dbGw!!, true)
                    finish()
                }
        val default = TimeZone.getDefault()
        val name = default.getDisplayName(true, TimeZone.SHORT)
        val split = if (name.contains("+")) //0正时区 1负时区
            name.split("+")
        else
            name.split("-")

        val time = split[1].split(":")// +/- 08:46
        val tzHour = if (name.contains("+"))
            time[0].toInt() or (0b00000000)
        else
            time[0].toInt() or (0b10000000)

        val tzMinutes = time[1].toInt()

        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour: Int = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        val week = calendar.get(Calendar.DAY_OF_WEEK) - 1
        val yearH = (year shr 8) and (0xff)
        val yearL = year and (0xff)

        var params = byteArrayOf(tzHour.toByte(), tzMinutes.toByte(), yearH.toByte(),
                yearL.toByte(), month.toByte(), day.toByte(), hour.toByte(), minute.toByte(), second.toByte(), week.toByte())
        LogUtils.v("zcl-----------发送时区-------地址${dbGw?.meshAddr}")
        TelinkLightService.Instance()?.sendCommandResponse(Opcode.CONFIG_GW_SET_TIME_ZONE, dbGw?.meshAddr ?: 0, params, "1")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    fun initData() {
        checkedIdType = event_timer_mode.id
        dbGw = intent.getParcelableExtra("data")
        if (dbGw == null) {
            TmtUtils.midToast(this@GwEventListActivity, getString(R.string.no_get_device_info))
            finish()
        }

        toolbarTv.text = dbGw?.name

        if (TelinkLightApplication.getApp().isConnectGwBle) {//直连时候获取版本号
            val disposable = Commander.getDeviceVersion(dbGw!!.meshAddr).subscribe({ s: String ->
                bottom_version_number.text = s
                if (TextUtils.isEmpty(dbGw?.version))
                    dbGw?.version = getString(R.string.number_no)
                dbGw!!.version = s
                fiVersion?.title = dbGw?.version
                DBUtils.saveGateWay(dbGw!!, false)
            }, {})
        }
        bottom_version_number.text = dbGw?.version

        swipe_recycleView.layoutManager = LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.VERTICAL, false)
        swipe_recycleView.isItemViewSwipeEnabled = false //侧滑删除，默认关闭。
        // 设置监听器。
        swipe_recycleView.setSwipeMenuCreator(function)
        swipe_recycleView.setOnItemMenuClickListener { menuBridge, adapterPosition ->
            menuBridge.closeMenu()
            deleteBean = listOne[adapterPosition]
            deleteTimerLable(deleteBean!!, Date().time)
        }
        swipe_recycleView.adapter = adapter


        swipe_recycleView2.layoutManager = LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.VERTICAL, false)
        swipe_recycleView2.isItemViewSwipeEnabled = false //侧滑删除，默认关闭。
        // 设置监听器。
        swipe_recycleView2.setSwipeMenuCreator(function)
        swipe_recycleView2.setOnItemMenuClickListener { menuBridge, adapterPosition ->
            menuBridge.closeMenu()
            deleteBean = listTwo[adapterPosition]
            deleteTimerLable(deleteBean!!, Date().time)
        }
        swipe_recycleView2.adapter = adapter2


        getNewData()
        dbGw?.type = 0
        if (TelinkLightApplication.getApp().isConnectGwBle)//直连发送时区 不是直连不发
            sendTimeZoneParmars()
    }

    private fun getNewData() {
        listOne.clear()
        listTwo.clear()
        if (!TextUtils.isEmpty(dbGw?.tags)) {
            val elements = GsonUtil.stringToList(dbGw?.tags, GwTagBean::class.java)
            listOne.addAll(elements)
            listOne.sortBy { gwTagBean -> gwTagBean.tagId }
            listOne.forEach {
                it.weekStr = getWeekStr(it.week)
            }
            val list = listOne.filter { it.status == 1 }
            if (list.isEmpty()) {
                add_group_btn.visibility = View.VISIBLE
                changeRecycleView()
            }

            if (dbGw?.type == 0) {
                changeRecycleView()
                isShowAdd(listOne)
            }
        }
        if (!TextUtils.isEmpty(dbGw?.timePeriodTags)) {
            val elements = GsonUtil.stringToList(dbGw?.timePeriodTags, GwTagBean::class.java)
            listTwo.addAll(elements)
            listTwo.sortBy { gwTagBean -> gwTagBean.tagId }
            listTwo.forEach {
                it.weekStr = getWeekStr(it.week)
            }

            val list = listTwo.filter { it.status == 1 }

            if (list.isEmpty()) {
                add_group_btn.visibility = View.VISIBLE
                changeRecycleView()
            }
            if (dbGw?.type == 1) {
                changeRecycleView2()
                isShowAdd(listTwo)
            }
        }
    }

    private fun isShowAdd(list: MutableList<GwTagBean>) {
        if (list.size >= 0)
            add_group_btn?.visibility = View.VISIBLE
        else
            add_group_btn?.visibility = View.GONE
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    fun initListener() {
        add_group_btn?.setOnClickListener { addNewTag() }

        event_timer_mode.setOnClickListener {
            changeRecycleView()
            dbGw?.type = 0///网关模式 0定时 1循环
            add_group_btn_tv.text = getString(R.string.add_timing_label)
            isShowAdd(listOne)
        }

        event_time_pattern_mode.setOnClickListener {
            changeRecycleView2()
            dbGw?.type = 1///网关模式 0定时 1循环
            add_group_btn_tv.text = getString(R.string.add_cycle_label)
            isShowAdd(listTwo)
        }

        adapter.onItemChildClickListener = this
        adapter2.onItemChildClickListener = this
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onItemChildClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int) {
        dbGw?.pos = position
        when (view?.id) {
            R.id.item_event_ly -> {
                val intent = Intent(this, GwConfigTagActivity::class.java)
                dbGw?.addTag = 1//不是新的
                if (dbGw?.type == 0) //定时
                    dbGw?.tags = GsonUtils.toJson(listOne)//赋值时一定要转换为gson字符串
                else
                    dbGw?.timePeriodTags = GsonUtils.toJson(listTwo)//赋值时一定要转换为gson字符串

                intent.putExtra("data", dbGw)
                startActivityForResult(intent, 1000)
            }
            R.id.item_event_switch -> {
                currentGwTag = if (adapter == adapter2)
                    listTwo[position]
                else
                    listOne[position]

                if ((view as CheckBox).isChecked)
                    currentGwTag.status = 1
                else
                    currentGwTag.status = 0
                showLoadingDialog(getString(R.string.please_wait))
                if (currentGwTag.status == 1) {//执行开的操作 status; //开1 关0
                    isMutexType(currentGwTag, adapter)
                } else {
                    sendOpenOrCloseGw(currentGwTag, false)
                }
            }
        }
    }

    private fun changeRecycleView2() {
        swipe_recycleView.visibility = View.GONE
        swipe_recycleView2.visibility = View.VISIBLE
        event_timer_mode.setTextColor(getColor(R.color.gray9))
        event_time_pattern_mode.setTextColor(getColor(R.color.blue_text))
        add_group_btn_tv.text = getString(R.string.add_cycle_label)
        adapter2.notifyDataSetChanged()
    }

    private fun changeRecycleView() {
        swipe_recycleView.visibility = View.VISIBLE
        swipe_recycleView2.visibility = View.GONE
        event_timer_mode.setTextColor(getColor(R.color.blue_text))
        event_time_pattern_mode.setTextColor(getColor(R.color.gray9))
        add_group_btn_tv.text = getString(R.string.add_timing_label)
        adapter.notifyDataSetChanged()
    }

    /**
     * 是否是互斥的类型
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun isMutexType(dbGwTag: GwTagBean, adapterNow: BaseQuickAdapter<*, *>?) {
        val taskList = GsonUtil.stringToList(dbGwTag.tasks, GwTagBean::class.java)
        if (dbGwTag.status == 1) {//开启任务
            if (taskList.size > 0) {//如果有标签时间需要进行对比自己里面有没有互斥
                if (adapterNow == adapter) {//定时模式 开启任务判断
                    //获取要开启的标签的有效时间
                    val currentAllTime = getGwTimerAllTime(dbGwTag)

                    //1.获取定时已开启的tag
                    val oldList = mutableListOf<GwTimeAndDataBean>()
                    val listOneOpen = listOne.filter { it.status == 1 && taskList.size > 0 }//时间任务大于0个
                    listOneOpen.forEach {
                        oldList.addAll(getGwTimerAllTime(it))//获取所有日期的时间
                    }

                    val canOpen = canOpenTG(currentAllTime, oldList, true)
                    if (!canOpen) {
                        listOne[dbGw?.pos ?: 0].status = 0
                        hideLoadingDialog()
                        dbGw?.tags = GsonUtils.toJson(listOne)
                        DBUtils.saveGateWay(dbGw!!, true)
                        addGw(dbGw!!)//添加网关就是更新网关
                        return
                    }
                } else {//时间段开启任务判断
                    //获取要开启的标签的有效时间
                    val currentAllTime = getGwTimerAllTime(dbGwTag)

                    //1.获取定时已开启的tag
                    val oldList = mutableListOf<GwTimeAndDataBean>()
                    //it.tasks.size代表该标签有时间或者时间段
                    val listTwoOpen = listTwo.filter {
                        it.status == 1 && GsonUtil.stringToList(it.tasks, GwTagBean::class.java).size > 0
                    }

                    listTwoOpen.forEach {
                        oldList.addAll(getGwTimerAllTime(it))
                    }

                    val canOpen = canOpenTG(currentAllTime, oldList, false)
                    if (!canOpen) {
                        listTwo[dbGw?.pos ?: 0].status = 0
                        dbGw?.timePeriodTags = GsonUtils.toJson(listTwo)
                        DBUtils.saveGateWay(dbGw!!, true)
                        addGw(dbGw!!)//添加网关就是更新网关
                        hideLoadingDialog()
                        return
                    }
                }
            }
        }
        if (adapterNow == adapter2) {//执行点击操作状态下并且没有冲突走到这里 当前点击的adapter是循环的就重置定时 反之一样
            listOne.forEach {
                it.status = 0
            }
        } else {
            listTwo.forEach {
                it.status = 0
            }
        }
        adapter.notifyDataSetChanged()
        adapter2.notifyDataSetChanged()

        dbGw?.tags = GsonUtils.toJson(listOne)//赋值时一定要转换为gson字符串
        dbGw?.timePeriodTags = GsonUtils.toJson(listTwo)//赋值时一定要转换为gson字符串
        DBUtils.saveGateWay(dbGw!!, true)
        sendOpenOrCloseGw(dbGwTag, true)
    }


    private fun canOpenTG(currentAllTime: MutableList<GwTimeAndDataBean>, oldList: MutableList<GwTimeAndDataBean>, isTimer: Boolean): Boolean {
        var canOpenTag = true
        currentAllTime.forEach {
            if (canOpenTag)
                oldList.forEach { itOld ->
                    if (isTimer) {
                        if (it.id != itOld.id && it.week == itOld.week && it.startTime == itOld.startTime) {
                            canOpenTag = false
                            TmtUtils.midToast(this@GwEventListActivity, getString(R.string.tag_mutex, itOld.name))
                            updateBackStatus(true)
                            return@forEach
                        }
                    } else {
                        if (it.id != itOld.id && it.week == itOld.week) {
                            if ((it.startTime < itOld.endTime && it.startTime >= itOld.startTime) || (it.endTime >= itOld.endTime && it.endTime <= itOld.startTime)) {
                                canOpenTag = false
                                TmtUtils.midToast(this@GwEventListActivity, getString(R.string.tag_mutex, itOld.name))
                                updateBackStatus(true)
                                return@forEach
                            }
                        }
                    }
                }
        }
        return canOpenTag
    }

    private fun updateBackStatus(isMutex: Boolean) {
        GlobalScope.launch(Dispatchers.Main) {
            if (isMutex) {
                if (dbGw?.type == 0)
                    listOne[dbGw?.pos ?: 0].status = 0
                else
                    listTwo[dbGw?.pos ?: 0].status = 0
            } else {
                if (currentGwTag.status == 0)
                    currentGwTag.status = 1
                else
                    currentGwTag.status = 0
            }

            adapter.notifyDataSetChanged()
            adapter2.notifyDataSetChanged()
        }
    }

    private fun getGwTimerAllTime(dbGwTag: GwTagBean): MutableList<GwTimeAndDataBean> {
        val tasks = GsonUtil.stringToList(dbGwTag.tasks, GwTagBean::class.java)
        var splitWeek: List<String> = dbGwTag.weekStr.split(",")
        if (splitWeek.size == 1) {
            if (splitWeek[0] == getString(R.string.every_day)) {
                splitWeek = mutableListOf(getString(R.string.monday), getString(R.string.tuesday), getString(R.string.wednesday),
                        getString(R.string.thursday), getString(R.string.friday), getString(R.string.saturday), getString(R.string.sunday))
            } else if (splitWeek[0] == getString(R.string.only_one)) {
                splitWeek = getOnlyOne()
            }
        }
        val listCurrent = mutableListOf<GwTimeAndDataBean>()
        splitWeek.forEach {
            //获取需要判断的有效时间
            tasks.forEach { itTask ->
                val startTime = itTask.startHour * 60 + itTask.startMins
                val endTime = itTask.endHour * 60 + itTask.endMins
                listCurrent.add(GwTimeAndDataBean(dbGwTag.tagId, dbGwTag.tagName, it, startTime, endTime))
            }
        }
        return listCurrent
    }

    private fun getWeekStr(week: Int): String {
        var tmpWeek = week
        val sb = StringBuilder()
        when (tmpWeek) {
            0b10000000 -> sb.append(getString(R.string.every_day))
            0b00000000 -> sb.append(getString(R.string.only_one))
            else -> {
                var list = mutableListOf(
                        WeekBean(getString(R.string.monday), 1, (tmpWeek and MONDAY) != 0),
                        WeekBean(getString(R.string.tuesday), 2, (tmpWeek and TUESDAY) != 0),
                        WeekBean(getString(R.string.wednesday), 3, (tmpWeek and WEDNESDAY) != 0),
                        WeekBean(getString(R.string.thursday), 4, (tmpWeek and THURSDAY) != 0),
                        WeekBean(getString(R.string.friday), 5, (tmpWeek and FRIDAY) != 0),
                        WeekBean(getString(R.string.saturday), 6, (tmpWeek and SATURDAY) != 0),
                        WeekBean(getString(R.string.sunday), 7, (tmpWeek and SUNDAY) != 0))
                for (i in 0 until list!!.size) {
                    var weekBean = list!![i]
                    if (weekBean.selected) {
                        if (i == list!!.size - 1)
                            sb.append(weekBean.week)
                        else
                            sb.append(weekBean.week).append(",")
                    }
                }
            }
        }
        return sb.toString()
    }

    private fun getOnlyOne(): MutableList<String> {
        var weekDay = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1) {
            0 -> getString(R.string.sunday)
            1 -> getString(R.string.monday)
            2 -> getString(R.string.tuesday)
            3 -> getString(R.string.wednesday)
            4 -> getString(R.string.thursday)
            5 -> getString(R.string.friday)
            6 -> getString(R.string.saturday)
            else -> getString(R.string.sunday)
        }
        return mutableListOf(weekDay)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendOpenOrCloseGw(dbGwTag: GwTagBean, isMutex: Boolean) {
        disposableTimer?.dispose()
        connectCount++
        disposableTimer = Observable.timer(20000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (connectCount < 3)
                        sendOpenOrCloseGw(dbGwTag, isMutex)
                    else {
                        updateBackStatus(isMutex)
                        hideLoadingDialog()
                        runOnUiThread { TmtUtils.midToast(this@GwEventListActivity, getString(R.string.gate_way_label_switch_fail)) }
                    }
                }

        var meshAddress = dbGw?.meshAddr ?: 0
        //p = byteArrayOf(0x02, Opcode.GROUP_BRIGHTNESS_MINUS, 0x00, 0x00, 0x03, Opcode.GROUP_CCT_MINUS, 0x00, 0x00)
        //从第八位开始opcode, 设备meshAddr  参数11-12-13-14 15-16-17-18
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        LogUtils.v("zcl-----------当前日期:--------$month-$day")

        val opcodeHead = if (dbGwTag.isTimer())
            Opcode.CONFIG_GW_TIMER_LABLE_HEAD
        else
            Opcode.CONFIG_GW_TIMER_PERIOD_LABLE_HEAD

        if (!TelinkLightApplication.getApp().isConnectGwBle) {
            var gattPar = byteArrayOf(0x11, 0x11, 0x11, 0, 0, 0, 0,
                    opcodeHead, 0x11, 0x02,
                    dbGwTag.tagId.toByte(), dbGwTag.status.toByte(),
                    dbGwTag.week.toByte(), 0, month.toByte(), day.toByte(), 0, 0, 0, 0)
            LogUtils.v("zcl-----------发送到服务器标签列表开关-------$gattPar")


            val s = Base64Utils.encodeToStrings(gattPar)
            val gattBody = GwGattBody()
            gattBody.data = s
            gattBody.ser_id = GW_GATT_LABEL_SWITCH
            gattBody.macAddr = dbGw?.macAddr
            gattBody.tagName = dbGwTag.tagName
            sendToServer(gattBody)
        } else {
            var labHeadPar = byteArrayOf(dbGwTag.tagId.toByte(), dbGwTag.status.toByte(),//标签开与关
                    dbGwTag.week.toByte(), 0, month.toByte(), day.toByte(), 0, 0)
            TelinkLightService.Instance()?.sendCommandResponse(opcodeHead, meshAddress, labHeadPar, "1")
            LogUtils.v("zcl-----------发送命令0xf6-------")
        }
    }

    private fun sendToServer(gattBody: GwGattBody) {
        GwModel.sendToGatt(gattBody)?.subscribe(object : NetworkObserver<String?>() {
            override fun onNext(t: String) {
                LogUtils.v("zcl------------------$t")
            }

            override fun onError(e: Throwable) {
                super.onError(e)
                LogUtils.e("zcl------------------${e.message}")
            }
        })
    }

    /**
     * 添加网关
     */
    private fun addGw(dbGw: DbGateway) {
        GwModel.add(dbGw)?.subscribe(object : NetworkObserver<DbGateway?>() {
            override fun onNext(t: DbGateway) {
                LogUtils.v("zcl-----网关失添成功返回-------------$t")
            }

            override fun onError(e: Throwable) {
                super.onError(e)
                LogUtils.v("zcl-------网关失添加败-----------" + e.message)
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun deleteTimerLable(gwTagBean: GwTagBean, currentTime: Long) {
        showLoadingDialog(getString(R.string.please_wait))
        disposableTimer?.dispose()
        connectCount++
        val opcodedelete = if (gwTagBean.isTimer())
            Opcode.CONFIG_GW_TIMER_DELETE_LABLE
        else
            Opcode.CONFIG_GW_TIMER_PERIOD_DELETE_LABLE

        if (TelinkLightApplication.getApp().isConnectGwBle) {
            setTimerDelay(gwTagBean, currentTime, 2000)
            //11-18 11位labelId
            GlobalScope.launch(Dispatchers.Main) {
                if (currentTime - lastTime < 400)
                    delay(400)
                var paramer = byteArrayOf(gwTagBean.tagId.toByte(), 0, 0, 0, 0, 0, 0, 0)
                TelinkLightService.Instance()?.sendCommandResponse(opcodedelete, dbGw?.meshAddr ?: 0, paramer, "1")
            }
        } else {
            setTimerDelay(gwTagBean, currentTime, 6500)
            var labHeadPar = byteArrayOf(0x11, 0x11, 0x11, 0, 0, 0, 0, opcodedelete, 0x11, 0x02,
                    gwTagBean.tagId.toByte(), 0, 0, 0, 0, 0, 0, 0, 0, 0)
            LogUtils.v("zcl-----------发送到服务器标签列表删除标签-------$labHeadPar")
            val s = Base64Utils.encodeToStrings(labHeadPar)
            val gattBody = GwGattBody()
            gattBody.data = s
            gattBody.ser_id = GW_GATT_DELETE_LABEL
            gattBody.macAddr = dbGw?.macAddr
            gattBody.tagName = gwTagBean.tagName
            sendToServer(gattBody)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setTimerDelay(gwTagBean: GwTagBean, currentTime: Long, delay: Long) {
        disposableTimer = Observable.timer(delay, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (connectCount < 3)
                        deleteTimerLable(gwTagBean, currentTime)
                    else
                        GlobalScope.launch(Dispatchers.Main) {
                            hideLoadingDialog()
                            TmtUtils.midToast(this@GwEventListActivity, getString(R.string.delete_gate_way_label_fail))
                        }
                }
    }

    private fun addNewTag() {
        var list = if (dbGw?.type == 0)
            listOne
        else
            listTwo
        if (list.size >= 20)
            toast(getString(R.string.gate_way_time_max))
        else {
            val intent = Intent(this@GwEventListActivity, GwConfigTagActivity::class.java)
            dbGw?.addTag = 0//创建新的
            intent.putExtra("data", dbGw)
            startActivityForResult(intent, 1000)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_list)
        receiver = GwBrocasetReceiver()
        val filter = IntentFilter()
        filter.addAction(LightService.ACTION_STATUS_CHANGED)
        registerReceiver(receiver, filter)
        receiver?.setOnGwStateChangeListerner(object : GwBrocasetReceiver.GwStateChangeListerner {
            override fun loginSuccess() {
                if (!Constant.IS_ROUTE_MODE)
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.icon_bluetooth)
            }

            override fun loginFail() {
                if (!Constant.IS_ROUTE_MODE)
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                // retryConnect()
            }

            override fun setGwComplete(deviceInfo: DeviceInfo) {
                LogUtils.v("zcl-----------发送时区回调-------$deviceInfo")
                when (deviceInfo.gwVoipState) {
                    GW_TIME_ZONE_VOIP, GW_CONFIG_TIMER_LABEL_VOIP, GW_CONFIG_TIME_PERIVODE_LABEL_VOIP -> {
                        disposableTimer?.dispose()
                        hideLoadingDialog()
                    }
                    GW_DELETE_TIMER_LABEL_VOIP, GW_DELETE_TIME_PERIVODE_LABEL_VOIP -> {
                        hideLoadingDialog()
                        runOnUiThread { deleteSuceess() }
                    }
                    GW_RESET_VOIP -> {
                        showDialogHardDelete?.dismiss()
                        disposableFactoryTimer?.dispose()
                        LogUtils.v("zcl-----------获取网关相关返回信息-------$deviceInfo")
                        isRestSuccess = true
                        deleteGwData(isRestSuccess)
                    }
                    GW_RESET_USER_VOIP -> {
                        disposableFactoryTimer?.dispose()
                        resetUserGwData()
                    }
                }
            }

            override fun getMacComplete(deviceInfo: DeviceInfo) {
                //mac信息获取成功
                dbGw?.sixByteMacAddr = deviceInfo.sixByteMacAddress
            }
        })
        mApp = (this.application as TelinkLightApplication)
        initView()
        makePopuwindow()
        initData()
        initListener()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == 1000) {
            dbGw = data?.getParcelableExtra("data")
            getNewData()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        disposableTimer?.dispose()
        //定时
        if (checkedIdType == R.id.event_timer_mode) {//定時模式  0定时 1循环
            dbGw?.tags = GsonUtils.toJson(listOne)//赋值时一定要转换为gson字符串
            dbGw?.timePeriodTags = GsonUtils.toJson(listTwo)
        } else {//時間段模式
            dbGw?.tags = GsonUtils.toJson(listOne)
            dbGw?.timePeriodTags = GsonUtils.toJson(listTwo)//赋值时一定要转换为gson字符串
        }
        if (!isDelete)
            DBUtils.saveGateWay(dbGw!!, true)
        TelinkLightService.Instance()?.idleMode(true)
    }

    override fun receviedGwCmd2000(serId: String) {
        if (GW_GATT_LABEL_SWITCH == serId.toInt()) {
            runOnUiThread { hideLoadingDialog() }
            disposableTimer?.dispose()
        } else if (GW_GATT_DELETE_LABEL == serId?.toInt()) {
            runOnUiThread { deleteSuceess() }
        }
    }

    override fun performed(event: Event<String>?) {
        if (event is DeviceEvent)
            this.onDeviceEvent(event)
    }

    private fun onDeviceEvent(event: DeviceEvent) {
        when (event.type) {
            DeviceEvent.STATUS_CHANGED -> when (event.args.status) {
                LightAdapter.STATUS_SET_GW_COMPLETED -> {//Dadou   Dadoutek2018
                    val deviceInfo = event.args
                    when (deviceInfo.gwVoipState) {
                        GW_RESET_VOIP -> {
                            showDialogHardDelete?.dismiss()
                            disposableFactoryTimer?.dispose()
                            LogUtils.v("zcl-----------获取网关相关返回信息-------$deviceInfo")
                            isRestSuccess = true
                            deleteGwData(isRestSuccess)
                        }
                        GW_RESET_USER_VOIP -> {
                            disposableFactoryTimer?.dispose()
                            resetUserGwData()
                        }
                    }
                }
                LightAdapter.STATUS_LOGIN -> {
                    if (!Constant.IS_ROUTE_MODE)
                    toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.icon_bluetooth)
                }
                LightAdapter.STATUS_LOGOUT -> {
                    if (!Constant.IS_ROUTE_MODE)
                    toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                    if (!isRestSuccess)
                        retryConnect()
                }
                LightAdapter.STATUS_SET_GW_COMPLETED -> {//Dadou   Dadoutek2018
                    val deviceInfo = event.args
                    when (deviceInfo.gwVoipState) {
                        GW_TIME_ZONE_VOIP, GW_CONFIG_TIMER_LABEL_VOIP, GW_CONFIG_TIME_PERIVODE_LABEL_VOIP -> {
                            LogUtils.v("zcl-----------获取网关相关返回信息-------$deviceInfo")
                            disposableTimer?.dispose()
                            hideLoadingDialog()
                        }
                        GW_DELETE_TIMER_LABEL_VOIP, GW_DELETE_TIME_PERIVODE_LABEL_VOIP -> {
                            LogUtils.v("zcl-----------获取网关相关返回信息-------$deviceInfo")
                            hideLoadingDialog()
                            runOnUiThread { deleteSuceess() }
                        }
                    }
                }

                //获取设备mac
                LightAdapter.STATUS_GET_DEVICE_MAC_COMPLETED -> {
                    //mac信息获取成功
                    val deviceInfo = event.args
                    LogUtils.v("zcl-----------蓝牙数据获取设备的macaddress-------$deviceInfo--------------${deviceInfo.sixByteMacAddress}")
                    dbGw?.sixByteMacAddr = deviceInfo.sixByteMacAddress
                }
                LightAdapter.STATUS_GET_DEVICE_MAC_FAILURE -> {
                }
            }
        }
    }

    private fun resetUserGwData() {
        GwModel.clearGwData(dbGw!!.id)?.subscribe(object : NetworkObserver<ClearGwBean?>() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onNext(t: ClearGwBean) {
                saveResetGwData(t)
                hideLoadingDialog()
                disposableFactoryTimer?.dispose()
                ToastUtils.showShort(getString(R.string.gw_user_reset_switch_success))
                getNewData()
            }

            override fun onError(e: Throwable) {
                super.onError(e)
                disposableFactoryTimer?.dispose()
                ToastUtils.showShort(e.message)
            }
        })
    }

    private fun saveResetGwData(t: ClearGwBean) {
        dbGw?.belongRegionId = t.belongRegionId
        dbGw?.id = t.id.toLong()
        dbGw?.macAddr = t.macAddr
        dbGw?.meshAddr = t.meshAddr
        dbGw?.name = t.name
        dbGw?.openTag = t.openTag
        dbGw?.productUUID = t.productUUID
        dbGw?.state = t.state
        dbGw?.tags = t.tags.toString()
        dbGw?.timePeriodTags = t.timePeriodTags
        dbGw?.type = t.type
        dbGw?.uid = t.uid
        dbGw?.version = t.version
        dbGw?.tags = t.tags
        DBUtils.saveGateWay(dbGw!!, false)
    }
}
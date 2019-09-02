package com.dadoutek.uled.light

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.bluetooth.le.ScanFilter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.util.Log
import android.util.SparseArray
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import butterknife.ButterKnife
import butterknife.OnClick
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.group.GroupsRecyclerViewAdapter
import com.dadoutek.uled.intf.OnRecyclerviewItemClickListener
import com.dadoutek.uled.intf.OnRecyclerviewItemLongClickListener
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.Constant.VENDOR_ID
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.othersview.LogInfoActivity
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.othersview.SplashActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.tellink.TelinkMeshErrorDealActivity
import com.dadoutek.uled.util.*
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.TelinkLog
import com.telink.bluetooth.event.*
import com.telink.bluetooth.light.*
import com.telink.bluetooth.light.DeviceInfo
import com.telink.util.Event
import com.telink.util.EventListener
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_device_scanning.*
import kotlinx.android.synthetic.main.toolbar.*
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 创建者     zcl
 * 创建时间   2019/8/28 18:37
 * 描述	      ${搜索全彩灯设备}$
 *
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${TODO}$
 */
class DeviceScanningNewActivity : TelinkMeshErrorDealActivity(), AdapterView.OnItemClickListener, EventListener<String>, Toolbar.OnMenuItemClickListener {
    private var mApplication: TelinkLightApplication? = null
    private var mRxPermission: RxPermissions? = null
    //防止内存泄漏
    internal var mDisposable = CompositeDisposable()
    private val loadDialog: Dialog? = null
    //分组所含灯的缓存
    private var nowLightList: MutableList<DbLight>? = null
    private var inflater: LayoutInflater? = null
    private var grouping: Boolean = false
    private var adapter: DeviceListAdapter? = null
    internal var isFirtst = true
    //标记登录状态
    private var isLoginSuccess = false
    private var deviceListView: GridView? = null
    private var groupsRecyclerViewAdapter: GroupsRecyclerViewAdapter? = null
    private var groups: MutableList<DbGroup>? = null
    private var mTimer: Disposable? = null
    private var mRetryCount = 0
    private var type: String? = null
    //当前所选组index
    private var currentGroupIndex = -1
    private var updateList: MutableList<DbLight>? = null
    private val indexList = ArrayList<Int>()
    //对一个灯重复分组时记录上一次分组
    private var originalGroupID = -1
    private val mGroupingDisposable: Disposable? = null
    private var tvStopScan: TextView? = null
    //灯的mesh地址
    private val dstAddress: Int = 0
    private var mConnectTimer: Disposable? = null
    private val mBlinkDisposables = SparseArray<Disposable>()
    private var isSelectAll = false
    private var scanRGBLight = false
    private var initHasGroup = false
    private var guideShowCurrentPage = false
    private var isGuide = false
    private var layoutmanager: LinearLayoutManager? = null
    private var allLightId: Long = 0
    private var updateMeshStatus: UPDATE_MESH_STATUS? = null
    /**
     * 有无被选中的用来分组的灯
     * @return true: 选中了       false:没选中
     */
    private val isSelectLight: Boolean
        get() = currentSelectLights.size > 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRxPermission = RxPermissions(this)
        //设置屏幕常亮
        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_device_scanning)
        ButterKnife.bind(this)
        initData()
        initView()
        initClick()
        startScan(0)
    }

    //如果所选灯已有分组，清空后再继续添加到新的分组          nowLightList.get(i).belongGroups.clear();
    private val currentSelectLights: List<DbLight>
        get() {
            val arrayList = ArrayList<DbLight>()
            indexList.clear()
            for (i in nowLightList!!.indices) {
                if (nowLightList!![i].selected && !nowLightList!![i].hasGroup) {
                    arrayList.add(nowLightList!![i])
                    indexList.add(i)
                } else if (nowLightList!![i].selected && nowLightList!![i].hasGroup) {
                    originalGroupID = Integer.parseInt(nowLightList!![i].belongGroupId.toString())
                    arrayList.add(nowLightList!![i])
                    indexList.add(i)
                }
            }
            return arrayList
        }

    private val currentGroup: DbGroup?
        get() {
            if (currentGroupIndex == -1) {
                if (groups!!.size > 1) {
                    Toast.makeText(this, R.string.please_select_group, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, R.string.tip_add_gp, Toast.LENGTH_SHORT).show()
                }
                return null
            }
            return groups!![currentGroupIndex]
        }

    /**
     * 是否所有灯都分了组
     *
     * @return false还有没有分组的灯 true所有灯都已经分组
     */
    private val isAllLightsGrouped: Boolean
        get() {
            if (nowLightList != null)
                for (j in nowLightList!!.indices) {
                    if (nowLightList!![j] == null)
                        return true
                    if (nowLightList!![j].belongGroupId == allLightId) {
                        return false
                    }
                }
            return true
        }


    private val onRecyclerviewItemClickListener = OnRecyclerviewItemClickListener { v, position ->
        currentGroupIndex = position
        for (i in groups!!.indices.reversed()) {
            if (i != position && groups!![i].checked) {
                updateData(i, false)
            } else if (i == position && !groups!![i].checked) {
                updateData(i, true)
            } else if (i == position && groups!![i].checked) {
                updateData(i, true)
            }
        }

        groupsRecyclerViewAdapter!!.notifyDataSetChanged()
        SharedPreferencesHelper.putInt(TelinkLightApplication.getInstance(),
                Constant.DEFAULT_GROUP_ID, currentGroupIndex)
    }

    private var startConnect = false

    private val clickListener = View.OnClickListener { v ->
        if (v === btn_scan) {
            doFinish()
        } else if (v.id == R.id.btn_log) {
            startActivity(Intent(this@DeviceScanningNewActivity, LogInfoActivity::class.java))
        }
    }

    private val onClick = View.OnClickListener {
        stopTimer()
        closeAnimation()
        if (TelinkLightService.Instance().isLogin) {

            Observable.interval(0, 200, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                    .subscribe(object : Observer<Long> {
                        var disposable: Disposable? = null
                        override fun onSubscribe(d: Disposable) {
                            disposable = d
                        }

                        //updateMesh超时时间 50*200=10S
                        override fun onNext(t: Long) {
                            if (t >= 50) {
                                disposable?.dispose()
                                scanSuccess()
                            } else {
                                if (updateMeshStatus == UPDATE_MESH_STATUS.SUCCESS) {
                                    tvStopScan!!.visibility = View.GONE
                                    if (nowLightList != null && nowLightList!!.size > 0) {
                                        nowLightList!!.clear()
                                    }
                                    if (nowLightList != null)
                                        nowLightList!!.addAll(adapter!!.getLights()!!)

                                    scanPb!!.visibility = View.GONE
                                    startGrouping()
                                    disposable?.dispose()
                                } else if (updateMeshStatus == UPDATE_MESH_STATUS.FAILED) {
                                    tvStopScan!!.visibility = View.GONE
                                    if (nowLightList != null && nowLightList!!.size > 0) {
                                        nowLightList!!.clear()
                                    }
                                    if (nowLightList != null)
                                        nowLightList!!.addAll(adapter!!.getLights()!!)

                                    scanPb!!.visibility = View.GONE
                                    startGrouping()
                                    disposable?.dispose()
                                }
                            }
                        }

                        override fun onError(e: Throwable) {}

                        override fun onComplete() {}
                    })
        } else scanSuccess()

    }

    internal var syncCallback: SyncCallback = object : SyncCallback {

        override fun start() {}

        override fun complete() {}

        override fun error(msg: String) {
            ToastUtils.showLong(R.string.upload_data_failed)
            Log.d("Error", msg)
        }
    }


    private var groupingSuccess = false
    private val groupingLight: DbLight? = null
    private val groupingGroup: DbGroup? = null
    private var bestRssiDevice: DeviceInfo? = null

    enum class UPDATE_MESH_STATUS {
        SUCCESS,
        FAILED,
        UPDATING_MESH
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val light = this.adapter!!.getItem(position)
        light!!.selected = !light.selected
        val holder = view.tag as DeviceItemHolder
        holder.selected!!.isChecked = light.selected

        if (light.selected) {
            this.updateList!!.add(light)
            nowLightList!![position].selected = true

            btn_add_groups?.setText(R.string.set_group)

            if (hasGroup()) {
                if (light != null) {
                    startBlink(light)
                }
            } else {
                ToastUtils.showLong(R.string.tip_add_group)
            }
        } else {
            nowLightList!![position].selected = false
            if (light != null) {
                stopBlink(light)
                this.updateList!!.remove(light)
            }
            if (!isSelectLight && isAllLightsGrouped) {
                btn_add_groups?.setText(R.string.complete)
            }
        }
    }

    private fun isSelectAll() {
        if (isSelectAll) {
            for (j in nowLightList!!.indices) {
                this.updateList!!.add(nowLightList!![j])
                nowLightList!![j].selected = true

                btn_add_groups?.setText(R.string.set_group)

                if (hasGroup()) {
                    startBlink(nowLightList!![j])
                } else {
                    ToastUtils.showLong(R.string.tip_add_group)
                }
            }

            this.adapter!!.notifyDataSetChanged()
        } else {
            for (j in nowLightList!!.indices) {
                this.updateList!!.remove(nowLightList!![j])
                nowLightList!![j].selected = false
                stopBlink(nowLightList!![j])
                if (!isSelectLight && isAllLightsGrouped) {
                    btn_add_groups?.setText(R.string.complete)
                }
            }

            this.adapter!!.notifyDataSetChanged()
        }
    }

    private fun hasGroup(): Boolean {
        if (groups!!.size == -1) {
            groups = ArrayList()
            return false
        } else {
            return true
        }

    }

    /**
     * 让灯开始闪烁
     */
    private fun startBlink(light: DbLight?) {
        val group: DbGroup
        if (light != null) {
            val groupId = light.belongGroupId ?: return
            val groupOfTheLight = DBUtils.getGroupByID(groupId)
            if (groupOfTheLight == null)
                group = groups!![0]
            else
                group = groupOfTheLight
            val groupAddress = group.meshAddr
            Log.d("Saw", "startBlink groupAddresss = $groupAddress")
            val dstAddress = light.meshAddr
            val opcode = Opcode.SET_GROUP
            val params = byteArrayOf(0x01, (groupAddress and 0xFF).toByte(), (groupAddress shr 8 and 0xFF).toByte())
            params[0] = 0x01

            if (mBlinkDisposables.get(dstAddress) != null) {
                mBlinkDisposables.get(dstAddress).dispose()
            }

            //每隔1s发一次，就是为了让灯一直闪.
            mBlinkDisposables.put(dstAddress, Observable.interval(0, 1000, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { aLong -> TelinkLightService.Instance().sendCommandNoResponse(opcode, dstAddress, params) })
        }
    }

    private fun stopBlink(light: DbLight) {
        val disposable = mBlinkDisposables.get(light.meshAddr)
        disposable?.dispose()
    }

    //扫描失败处理方法
    private fun scanFail() {
        showToast(getString(R.string.scan_end))
        closeAnimation()
        doFinish()
    }

    private fun startTimer() {
        stopTimer()
        // 防止onLescanTimeout不调用，导致UI卡住的问题。设为正常超时时间的2倍
        if (mTimer != null && !mTimer!!.isDisposed)
            mTimer!!.dispose()
        mTimer = Observable.timer((SCAN_TIMEOUT_SECOND * 2).toLong(), TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe { aLong ->
                    if (mRetryCount < MAX_RETRY_COUNT) {
                        mRetryCount++
                        Log.d("ScanningTest", "rxjava timer timeout , retry count = $mRetryCount")
                        startScan(0)
                    } else {
                        Log.d("ScanningTest", "rxjava timer timeout , do not retry")
                        onLeScanTimeout()
                    }
                }

    }


    private fun stopTimer() {
        if (mTimer != null && !mTimer!!.isDisposed) {
            mTimer!!.dispose()
        }
    }


    private fun createConnectTimeout(): Disposable {
        return Observable.timer(15, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe { aLong ->
                    Toast.makeText(mApplication, getString(R.string.connect_fail), Toast.LENGTH_SHORT).show()
                    hideLoadingDialog()
                    TelinkLightService.Instance().idleMode(true)
                    mConnectTimer = null
                }
    }

    //处理扫描成功后
    private fun scanSuccess() {
        //更新Title
        tvStopScan?.visibility = View.GONE
        toolbar!!.title = getString(R.string.title_scanned_lights_num, adapter!!.count)
        //存储当前添加的灯。
        //2018-4-19-hejiajun 添加灯调整位置，防止此时点击灯造成下标越界
        if (nowLightList != null && nowLightList!!.size > 0) {
            nowLightList!!.clear()
        }
        if (nowLightList != null)
            nowLightList!!.addAll(adapter!!.getLights()!!)

        scanPb!!.visibility = View.GONE

        //先连接灯。
        autoConnect(true)
        //倒计时，出问题了就超时。
        mConnectTimer = createConnectTimeout()

        btn_add_groups?.visibility = View.VISIBLE
        btn_add_groups?.setText(R.string.start_group_bt)

        btn_add_groups?.setOnClickListener { v ->
            if (isLoginSuccess) {
                //进入分组
                startGrouping()
            } else if (mConnectTimer == null) {
                autoConnect(true)
                mConnectTimer = createConnectTimeout()
            } else {    //正在连接中
                showLoadingDialog(resources.getString(R.string.connecting_tip))
                closeAnimation()

            }
        }

    }

    private fun doFinish() {
        if (updateList != null && updateList!!.size > 0) {
            checkNetworkAndSync()
        }
        //        TelinkLightService.Instance().idleMode(true);
        this.mApplication!!.removeEventListener(this)
        this.updateList = null
        mDisposable.dispose()  //销毁时取消订阅.
        if (mTimer != null)
            mTimer!!.dispose()
        mGroupingDisposable?.dispose()
        if (mConnectTimer != null)
            mConnectTimer!!.dispose()

        for (i in 0 until mBlinkDisposables.size()) {
            val disposable = mBlinkDisposables.get(i)
            disposable?.dispose()
        }

        if (ActivityUtils.isActivityExistsInStack(MainActivity::class.java))
            ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
        else {
            ActivityUtils.startActivity(MainActivity::class.java)
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        if (mConnectTimer != null)
            mConnectTimer!!.dispose()
    }

    /**
     * 开始分组
     */
    private fun startGrouping() {
        closeAnimation()

        LeBluetooth.getInstance().stopScan()

        //初始化分组页面
        changeGroupView()

        //完成分组跳转
        changOtherView()

        //确定当前分组
        sureGroupingEvent()

        toolbar!!.setNavigationOnClickListener { _ ->
            AlertDialog.Builder(this@DeviceScanningNewActivity)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        if (currentSelectLights.size > 0) {
                            for (i in 0 until currentSelectLights.size) {
                                //让选中的灯停下来别再发闪的命令了。
                                stopBlink(currentSelectLights[i])
                            }
                        }
                        doFinish()
                    }
                    .setNegativeButton(R.string.btn_cancel) { dialog, which -> }
                    .setMessage(R.string.exit_tips_in_group)
                    .show()
        }
    }

    private fun sureGroupingEvent() {
        btn_add_groups?.setText(R.string.sure_group)
        btn_add_groups?.setOnClickListener { v ->
            if (isAllLightsGrouped && !isSelectLight) {
                doFinish()
            } else {
                sureGroups()
            }
        }
    }

    private fun changOtherView() {
        grouping_completed?.setOnClickListener { v ->
            //判定是否还有灯没有分组，如果没有允许跳转到下一个页面
            if (isAllLightsGrouped) {//所有灯都有分组可以跳转
                showToast(getString(R.string.group_completed))
                //页面跳转前进行分组数据保存
                //                TelinkLightService.Instance().idleMode(true);
                //目前测试调到主页
                doFinish()
            } else {
                showToast(getString(R.string.have_lamp_no_group_tip))
            }
        }
    }

    private fun sureGroups() {
        if (isSelectLight) {
            //进行分组操作
            //获取当前选择的分组
            val group = currentGroup
            if (group != null) {
                if (group.meshAddr == 0xffff) {
                    ToastUtils.showLong(R.string.tip_add_gp)
                    return
                }
                //获取当前勾选灯的列表
                val selectLights = currentSelectLights

                showLoadingDialog(resources.getString(R.string.grouping_wait_tip,
                        selectLights.size.toString() + ""))
                //将灯列表的灯循环设置分组
                setGroups(group, selectLights)
            }

        } else {
            showToast(getString(R.string.selected_lamp_tip))
        }
    }


    private fun setGroupOneByOne(dbGroup: DbGroup, selectLights: List<DbLight>, index: Int) {
        val dbLight = selectLights[index]
        val lightMeshAddr = dbLight.meshAddr
        Commander.addGroup(lightMeshAddr, dbGroup.meshAddr, {
            dbLight.belongGroupId = dbGroup.id
            updateGroupResult(dbLight, dbGroup)
            if (index + 1 > selectLights.size - 1)
                completeGroup(selectLights)
            else
                setGroupOneByOne(dbGroup, selectLights, index + 1)
            null
        }) {
            dbLight.belongGroupId = allLightId
            ToastUtils.showLong(R.string.group_fail_tip)
            updateGroupResult(dbLight, dbGroup)
            if (TelinkLightApplication.getInstance().connectDevice == null) {
                ToastUtils.showLong("断开连接")
                stopTimer()
                onLeScanTimeout()
            } else {
                if (index + 1 > selectLights.size - 1)
                    completeGroup(selectLights)
                else
                    setGroupOneByOne(dbGroup, selectLights, index + 1)
            }
            null
        }

    }

    private fun completeGroup(selectLights: List<DbLight>) {
        //取消分组成功的勾选的灯
        for (i in selectLights.indices) {
            val light = selectLights[i]
            light.selected = false
        }
        adapter!!.notifyDataSetChanged()
        hideLoadingDialog()
        if (isAllLightsGrouped) {
            btn_add_groups?.setText(R.string.complete)
        }
    }

    private fun setGroups(group: DbGroup?, selectLights: List<DbLight>) {
        if (group == null) {
            Toast.makeText(mApplication, R.string.select_group_tip, Toast.LENGTH_SHORT).show()
            return
        }

        if (isSelectAll) {
            toolbar!!.menu.findItem(R.id.menu_select_all).title = getString(R.string.select_all)
            isSelectAll = false
        }

        for (i in selectLights.indices) {
            //让选中的灯停下来别再发闪的命令了。
            stopBlink(selectLights[i])
        }

        setGroupOneByOne(group, selectLights, 0)
    }

    private fun updateGroupResult(light: DbLight, group: DbGroup) {
        for (i in nowLightList!!.indices) {
            if (light.meshAddr == nowLightList!![i].meshAddr) {
                if (light.belongGroupId != allLightId) {
                    nowLightList!![i].hasGroup = true
                    nowLightList!![i].belongGroupId = group.id
                    nowLightList!![i].name = getString(R.string.unnamed)
                    DBUtils.updateLight(light)
                } else {
                    nowLightList!![i].hasGroup = false
                }
            }
        }
    }

    override fun onBackPressed() {
        //        super.onBackPressed();
        if (grouping) {
            if (currentSelectLights.size > 0) {
                for (i in 0 until currentSelectLights.size) {
                    //让选中的灯停下来别再发闪的命令了。
                    stopBlink(currentSelectLights[i])
                }
                doFinish()
            }
        } else {
            AlertDialog.Builder(this)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        doFinish()
                    }
                    .setNegativeButton(R.string.btn_cancel) { _, _ -> }
                    .setMessage(R.string.exit_tips_in_scanning)
                    .show()
        }
    }

    //分组页面调整
    private fun changeGroupView() {
        grouping = true
        toolbar!!.inflateMenu(R.menu.menu_grouping_select_all)
        toolbar!!.setOnMenuItemClickListener(this)
        deviceListView!!.onItemClickListener = this
        deviceListView!!.adapter = adapter
        adapter!!.notifyDataSetChanged()
        btn_add_groups?.visibility = View.VISIBLE
        groups_bottom?.visibility = View.VISIBLE

        layoutmanager = LinearLayoutManager(this)
        layoutmanager!!.orientation = LinearLayoutManager.HORIZONTAL
        recycler_view_groups.layoutManager = layoutmanager

        if (groups!!.size > 0) {
            groupsRecyclerViewAdapter = GroupsRecyclerViewAdapter(groups, onRecyclerviewItemClickListener,
                    OnRecyclerviewItemLongClickListener { _, position ->
                        showGroupForUpdateNameDialog(position)
                    })
            recycler_view_groups?.adapter = groupsRecyclerViewAdapter
            add_group_relativeLayout?.visibility = View.GONE
            add_group?.visibility = View.VISIBLE
        } else {
            add_group_relativeLayout?.visibility = View.VISIBLE
            add_group?.visibility = View.GONE
        }

        disableEventListenerInGrouping()
        initOnLayoutListener()
    }

    private fun showGroupForUpdateNameDialog(position: Int) {
        val textGp = EditText(this)
        StringUtils.initEditTextFilter(textGp)
        textGp.setText(groups!![position].name)
        //        //设置光标默认在最后
        textGp.setSelection(textGp.text.toString().length)
        AlertDialog.Builder(this)
                .setTitle(getString(R.string.update_name_gp))
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(textGp)
                .setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
                    if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                        ToastUtils.showShort(getString(R.string.rename_tip_check))
                    } else {
                        groups!![position].name = textGp.text.toString().trim { it <= ' ' }
                        DBUtils.updateGroup(groups!![position])
                        groupsRecyclerViewAdapter!!.notifyItemChanged(position)
                        adapter!!.notifyDataSetChanged()
                        //                                DBUtils.INSTANCE.getLightByGroupMesh(groups.get(position).getMeshAddr());
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_select_all -> {
                if (isSelectAll) {
                    isSelectAll = false
                    item.setTitle(R.string.select_all)
                } else {
                    isSelectAll = true
                    item.setTitle(R.string.cancel)
                }
                isSelectAll()
            }
        }
        return false
    }

    private fun disableEventListenerInGrouping() {
        this.mApplication!!.removeEventListener(LeScanEvent.LE_SCAN, this)
        this.mApplication!!.removeEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)
    }

    @OnClick(R.id.add_group_layout)
    fun onViewClicked() {
        isGuide = false
        addNewGroup()
    }

    private fun addNewGroup() {
        val textGp = EditText(this)
        textGp.setText(DBUtils.getDefaultNewGroupName())
        StringUtils.initEditTextFilter(textGp)
        val builder = AlertDialog.Builder(this@DeviceScanningNewActivity)
        builder.setTitle(R.string.create_new_group)
        builder.setIcon(android.R.drawable.ic_dialog_info)
        builder.setView(textGp)
        builder.setCancelable(false)
        builder.setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
            // 获取输入框的内容
            if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                ToastUtils.showShort(getString(R.string.rename_tip_check))
            } else {
                //往DB里添加组数据
                if (scanRGBLight) {
                    DBUtils.addNewGroupWithType(textGp.text.toString().trim { it <= ' ' }, groups!!, Constant.DEVICE_TYPE_LIGHT_RGB, this)
                } else {
                    DBUtils.addNewGroupWithType(textGp.text.toString().trim { it <= ' ' }, groups!!, Constant.DEVICE_TYPE_LIGHT_NORMAL, this)
                }

                refreshView()
                dialog.dismiss()
                val imm = this@DeviceScanningNewActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS)
                guideStep2()
            }
        }
        if (!isGuide) {
            builder.setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }
        }
        textGp.isFocusable = true
        textGp.isFocusableInTouchMode = true
        textGp.requestFocus()
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                val inputManager = textGp.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputManager.showSoftInput(textGp, 0)
            }
        }, 200)
        builder.show()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun refreshView() {
        currentGroupIndex = groups!!.size - 1
        for (i in groups!!.indices.reversed()) {
            groups!![i].checked = i == groups!!.size - 1
        }
        groupsRecyclerViewAdapter = GroupsRecyclerViewAdapter(groups, onRecyclerviewItemClickListener,
                OnRecyclerviewItemLongClickListener { _, position ->
                    showGroupForUpdateNameDialog(position)
                })
        recycler_view_groups?.adapter = groupsRecyclerViewAdapter
        add_group_relativeLayout?.visibility = View.GONE
        add_group?.visibility = View.VISIBLE
        recycler_view_groups?.smoothScrollToPosition(groups!!.size - 1)
        groupsRecyclerViewAdapter!!.notifyDataSetChanged()
        SharedPreferencesHelper.putInt(TelinkLightApplication.getInstance(),
                Constant.DEFAULT_GROUP_ID, currentGroupIndex)
    }

    private fun updateData(position: Int, checkStateChange: Boolean) {
        groups!![position].checked = checkStateChange
    }

    /**
     * 自动重连
     * 此处用作设备登录
     */
    private fun autoConnect(b: Boolean) {
        if (TelinkLightService.Instance() != null) {
            if (TelinkLightService.Instance().mode != LightAdapter.MODE_AUTO_CONNECT_MESH) {
                if (b)
                showLoadingDialog(resources.getString(R.string.connecting_tip))
                closeAnimation()
                startConnect = true

                val meshName = DBUtils.lastUser!!.controlMeshName

                //自动重连参数
                val connectParams = Parameters.createAutoConnectParameters()
                connectParams.setMeshName(meshName)
                connectParams.setPassword(NetworkFactory.md5(NetworkFactory.md5(meshName) + meshName).substring(0, 16))
                connectParams.autoEnableNotification(true)
                //连接，如断开会自动重连
                Thread {
                    TelinkLightService.Instance().autoConnect(connectParams)
                }.start()
            }

            //刷新Notify参数
            val refreshNotifyParams = Parameters.createRefreshNotifyParameters()
            refreshNotifyParams.setRefreshRepeatCount(2)
            refreshNotifyParams.setRefreshInterval(1000)
            //开启自动刷新Notify
            TelinkLightService.Instance().autoRefreshNotify(refreshNotifyParams)
        }
    }

    private fun closeAnimation() {
        lottieAnimationView?.cancelAnimation()
        lottieAnimationView?.visibility = View.GONE
    }

    fun connectDevice(mac: String) {
        TelinkLightService.Instance().connect(mac, TIME_OUT_CONNECT)
    }


    override fun initOnLayoutListener() {
        val view = window.decorView
        val viewTreeObserver = view.viewTreeObserver
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                //                lazyLoad();
            }
        })
    }

    fun lazyLoad() {
        guideStep1()
    }

    //第一步添加组
    private fun guideStep1() {
        guideShowCurrentPage = !GuideUtils.getCurrentViewIsEnd(this, GuideUtils.END_INSTALL_LIGHT_KEY, false)
        if (guideShowCurrentPage) {
            GuideUtils.resetDeviceScanningGuide(this)
            val guide1 = add_group_relativeLayout
            GuideUtils.guideBuilder(this, GuideUtils.STEP3_GUIDE_CREATE_GROUP)
                    .addGuidePage(guide1?.let {
                        GuideUtils.addGuidePage(it, R.layout.view_guide_scan1, getString(R.string.scan_light_guide_1), View.OnClickListener { v ->
                            isGuide = true
                            addNewGroup()
                        }, GuideUtils.END_INSTALL_LIGHT_KEY, this)
                    })
                    .show()
        }
    }

    //第二部选择组
    private fun guideStep2() {
        guideShowCurrentPage = !GuideUtils.getCurrentViewIsEnd(this, GuideUtils.END_INSTALL_LIGHT_KEY, false)
        if (guideShowCurrentPage) {
            val guide2 = recycler_view_groups
            GuideUtils.guideBuilder(this, GuideUtils.STEP4_GUIDE_SELECT_GROUP)
                    .addGuidePage(guide2?.let {
                        GuideUtils.addGuidePage(it, R.layout.view_guide_scan1, getString(R.string.scan_light_guide_2),
                                View.OnClickListener { v -> guideStep3() }, GuideUtils.END_INSTALL_LIGHT_KEY, this)
                    })
                    .show()
        }
    }

    //第三部选择灯
    private fun guideStep3() {
        guideShowCurrentPage = !GuideUtils.getCurrentViewIsEnd(this, GuideUtils.END_INSTALL_LIGHT_KEY, false)
        if (guideShowCurrentPage) {
            val guide3 = list_devices!!.getChildAt(0)
            GuideUtils.guideBuilder(this, GuideUtils.STEP5_GUIDE_SELECT_SOME_LIGHT)
                    .addGuidePage(GuideUtils.addGuidePage(guide3, R.layout.view_guide_scan2, getString(R.string.scan_light_guide_3), View.OnClickListener { v ->
                        list_devices!!.performItemClick(guide3, 0, list_devices!!.getItemIdAtPosition(0))
                        guideStep4()
                    }, GuideUtils.END_INSTALL_LIGHT_KEY, this))
                    .show()
        }
    }

    //第四部确定分组
    private fun guideStep4() {
        guideShowCurrentPage = !GuideUtils.getCurrentViewIsEnd(this, GuideUtils.END_INSTALL_LIGHT_KEY, false)
        if (guideShowCurrentPage) {
            val guide4 = btn_add_groups
            GuideUtils.guideBuilder(this, GuideUtils.STEP6_GUIDE_SURE_GROUP)
                    .addGuidePage(guide4?.let {
                        GuideUtils.addGuidePage(it, R.layout.view_guide_scan3, getString(R.string.scan_light_guide_4), View.OnClickListener { v ->
                            guide4.performClick()
                            GuideUtils.changeCurrentViewIsEnd(this, GuideUtils.END_INSTALL_LIGHT_KEY, true)
                        }, GuideUtils.END_INSTALL_LIGHT_KEY, this)
                    })
                    .show()
        }
    }

    private fun initClick() {
        this.btn_scan?.setOnClickListener(this.clickListener)
        this.btn_log?.setOnClickListener(this.clickListener)
    }

    private fun initView() {
        initToolbar()
        //监听事件
        this.mApplication!!.addEventListener(LeScanEvent.LE_SCAN, this)
        this.mApplication!!.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)
        this.mApplication!!.addEventListener(DeviceEvent.STATUS_CHANGED, this)
        this.mApplication!!.addEventListener(MeshEvent.UPDATE_COMPLETED, this)
        this.mApplication!!.addEventListener(ErrorReportEvent.ERROR_REPORT, this)
        this.mApplication!!.addEventListener(NotificationEvent.GET_GROUP, this)
        this.inflater = this.layoutInflater
        this.adapter = DeviceListAdapter()

        this.grouping_completed?.setBackgroundColor(resources.getColor(R.color.gray))
        this.btn_scan?.isEnabled = false
        this.btn_scan?.setBackgroundResource(R.color.gray)
        deviceListView = this.findViewById(R.id.list_devices)
        //修改
        deviceListView!!.adapter = adapter

        this.updateList = ArrayList()

        btn_scan?.visibility = View.GONE
        btn_log?.visibility = View.GONE
        btn_add_groups?.visibility = View.GONE
        grouping_completed?.visibility = View.GONE

        light_num_layout?.visibility = View.GONE
        tvStopScan = toolbar?.findViewById(R.id.tv_function1)
        tvStopScan?.setText(R.string.stop_scan)
        tvStopScan?.setOnClickListener(onClick)
        tvStopScan?.visibility = View.GONE

        add_group_relativeLayout?.setOnClickListener { v -> addNewGroup() }
    }

    @SuppressLint("ResourceType")
    private fun initToolbar() {
        toolbar?.setTitle(R.string.scanning)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar?.setNavigationContentDescription(R.drawable.navigation_back_white)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initData() {
        val intent = intent
        scanRGBLight = intent.getBooleanExtra(Constant.IS_SCAN_RGB_LIGHT, false)

        if (DBUtils.getGroupByMesh(0xffff) != null) {
            allLightId = DBUtils.getGroupByMesh(0xffff)!!.id!!
        }
        type = intent.getStringExtra(Constant.TYPE_VIEW)

        this.mApplication = this.application as TelinkLightApplication
        nowLightList = ArrayList()
        if (groups == null) {
            groups = ArrayList()
            val list = DBUtils.groupList


            if (scanRGBLight) {
                for (i in list.indices) {
                    if (OtherUtils.isRGBGroup(list[i])) {
                        groups!!.add(list[i])
                    }
                }
            } else {
                for (i in list.indices) {
                    LogUtils.e("zcl----isNormalGroup----"+list[i])
                    if (OtherUtils.isNormalGroup(list[i])) {
                        groups!!.add(list[i])
                    }
                }
                for (i in list.indices) {
                    LogUtils.e("zcl----isAllRightGroup----"+list[i])
                    if (OtherUtils.isAllRightGroup(list[i])) {
                        groups!!.add(list[i])
                    }
                }
                for (i in list.indices) {
                    LogUtils.e("zcl----isDefaultGroup----"+list[i])
                    if (OtherUtils.isDefaultGroup(list[i])) {
                        groups!!.add(list[i])
                    }
                }
            }
        }

        if (groups!!.size > 0) {
            for (i in groups!!.indices) {
                if (i == groups!!.size - 1) {
                    groups!![i].checked = true
                    currentGroupIndex = i
                    SharedPreferencesHelper.putInt(TelinkLightApplication.getInstance(),
                            Constant.DEFAULT_GROUP_ID, currentGroupIndex)
                } else {
                    groups!![i].checked = false
                }
            }
            initHasGroup = true
        } else {
            initHasGroup = false
            currentGroupIndex = -1
        }
    }

    override fun onResume() {
        super.onResume()
        //检测service是否为空，为空则重启
        if (TelinkLightService.Instance() == null)
            mApplication!!.startLightService(TelinkLightService::class.java)
    }


    // 如果没有网络，则弹出网络设置对话框
    fun checkNetworkAndSync() {
        if (NetWorkUtils.isNetworkAvalible(this))
            SyncDataPutOrGetUtils.syncPutDataStart(this, syncCallback)
    }


    override fun onLocationEnable() {}

    private class DeviceItemHolder {
        var icon: ImageView? = null
        var txtName: TextView? = null
        var selected: CheckBox? = null
    }


    internal inner class DeviceListAdapter : BaseAdapter() {

        private var lights: MutableList<DbLight>? = null

        override fun getCount(): Int {
            return if (this.lights == null) 0 else this.lights!!.size
        }

        override fun getItem(position: Int): DbLight? {
            return this.lights!![position]
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

            var convertView = inflater?.inflate(R.layout.device_item, null)!!
            val icon = convertView.findViewById<ImageView>(R.id.img_icon)
            val txtName = convertView.findViewById<View>(R.id.txt_name) as TextView
            val selected = convertView.findViewById<View>(R.id.selected) as CheckBox

            var holder = DeviceItemHolder()

            holder.icon = icon
            holder.txtName = txtName
            holder.selected = selected

            convertView.tag = holder


            val light = this.getItem(position)

            holder.txtName!!.setText(R.string.not_grouped)
            if (scanRGBLight) {
                holder.icon!!.setImageResource(R.drawable.icon_rgblight)
            } else {
                holder.icon!!.setImageResource(R.drawable.icon_device_open)
            }


            holder.selected!!.isChecked = light!!.selected

            if (light.hasGroup) {
                holder.txtName!!.text = getDeviceName(light)
                holder.icon!!.visibility = View.VISIBLE
                holder.selected!!.visibility = View.VISIBLE
            } else {
                holder.txtName!!.visibility = View.VISIBLE
                holder.icon!!.visibility = View.VISIBLE
                if (grouping) {
                    holder.selected!!.visibility = View.VISIBLE
                } else {
                    holder.selected!!.visibility = View.GONE
                }
            }

            return convertView
        }

        fun add(light: DbLight) {
            if (this.lights == null)
                this.lights = ArrayList()
            DBUtils.saveLight(light, false)
            this.lights!!.add(light)
        }

        operator fun get(meshAddress: Int): DbLight? {

            if (this.lights == null)
                return null

            for (light in this.lights!!) {
                if (light.meshAddr == meshAddress) {
                    return light
                }
            }

            return null
        }

        fun getLights(): List<DbLight>? {
            return lights
        }
    }

    private fun getDeviceName(light: DbLight): String {
        return if (light.belongGroupId != allLightId) {
            DBUtils.getGroupNameByID(light.belongGroupId)
        } else {
            getString(R.string.not_grouped)
        }
    }

    /*********************************泰凌微后台数据部分 */

    /**
     * 事件处理方法
     *
     * @param event
     */
    override fun performed(event: Event<String>) {

        when (event.type) {
            LeScanEvent.LE_SCAN -> this.onLeScan(event as LeScanEvent)
            LeScanEvent.LE_SCAN_TIMEOUT -> {
                stopTimer()
                this.onLeScanTimeout()
            }
            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)
            NotificationEvent.GET_GROUP -> this.onGetGroupEvent(event as NotificationEvent)
            MeshEvent.ERROR -> this.onMeshEvent(event as MeshEvent)
            ErrorReportEvent.ERROR_REPORT -> {
                val info = (event as ErrorReportEvent).args
                onErrorReport(info)
            }
        }
    }

    private fun onErrorReport(info: ErrorReportInfo) {
        when (info.stateCode) {
            ErrorReportEvent.STATE_SCAN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_SCAN_BLE_DISABLE -> {
                        LogUtils.e("蓝牙未开启")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_ADV -> {
                        LogUtils.e("无法收到广播包以及响应包")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_TARGET -> {
                        LogUtils.e("未扫到目标设备")
                    }
                }

            }
            ErrorReportEvent.STATE_CONNECT -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_CONNECT_ATT -> {
                        LogUtils.e("未读到att表")
                    }
                    ErrorReportEvent.ERROR_CONNECT_COMMON -> {
                        LogUtils.e("未建立物理连接")
                    }
                }
                autoConnect(false)

            }
            ErrorReportEvent.STATE_LOGIN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_LOGIN_VALUE_CHECK -> {
                        LogUtils.e("value check失败： 密码错误")
                    }
                    ErrorReportEvent.ERROR_LOGIN_READ_DATA -> {
                        LogUtils.e("read login data 没有收到response")
                    }
                    ErrorReportEvent.ERROR_LOGIN_WRITE_DATA -> {
                        LogUtils.e("write login data 没有收到response")
                    }
                }
                autoConnect(false)

            }
        }
    }


    private fun onGetGroupEvent(event: NotificationEvent) {
        val info = event.args

        val srcAddress = info.src and 0xFF
        val params = info.params

        if (groupingLight == null || groupingGroup == null) {
            return
        }

        if (srcAddress != groupingLight.meshAddr) {
            return
        }

        var groupAddress: Int
        val len = params.size

        for (j in 0 until len) {

            groupAddress = params[j].toInt()

            if (groupAddress == 0x00 || groupAddress == 0xFF)
                break

            groupAddress = groupAddress or 0x8000

            if (groupingGroup.meshAddr == groupAddress) {
                groupingSuccess = true
            }
        }
    }

    private fun onMeshEvent(event: MeshEvent) {
        ToastUtils.showShort(R.string.restart_bluetooth)
    }

    private fun onNError(event: DeviceEvent) {

        TelinkLightService.Instance().idleMode(true)
        TelinkLog.d("DeviceScanningActivity#onNError")
        onLeScanTimeout()
    }

    /**
     * 扫描不到任何设备了
     * （扫描结束）
     */
    private fun onLeScanTimeout() {
        LeBluetooth.getInstance().stopScan()
        TelinkLightService.Instance().idleMode(true)
        this.btn_scan?.setBackgroundResource(R.color.primary)
        if (adapter!!.count > 0) {//表示目前已经搜到了至少有一个设备
            scanSuccess()
        } else {
            scanFail()
        }
    }

    /**
     * 开始扫描
     */
    private fun startScan(delay: Int) {
        //添加进disposable，防止内存溢出.
        mRxPermission?.request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN)?.subscribe { granted ->
            if (granted!!) {
                startTimer()
                startAnimation()
                handleIfSupportBle()
                TelinkLightService.Instance().idleMode(true)

                val scanFilters = ArrayList<ScanFilter>()
                var manuData: ByteArray?

                if (scanRGBLight) {
                    manuData = byteArrayOf(0, 0, 0, 0, 0, 0, DeviceType.LIGHT_RGB.toByte())
                } else {
                    manuData = byteArrayOf(0, 0, 0, 0, 0, 0, DeviceType.LIGHT_NORMAL.toByte())
                }

                val manuDataMask = byteArrayOf(0, 0, 0, 0, 0, 0, 0xFF.toByte())

                val scanFilter = ScanFilter.Builder()
                        .setManufacturerData(VENDOR_ID, manuData, manuDataMask)
                        .build()
                scanFilters.add(scanFilter)

                val mesh = mApplication!!.mesh
                //扫描参数
                val params = LeScanParameters.create()
                if (!AppUtils.isExynosSoc()) {
                    params.setScanFilters(scanFilters)
                }
                params.setMeshName(mesh.factoryName)
                params.setOutOfMeshName(Constant.OUT_OF_MESH_NAME)
                params.setTimeoutSeconds(SCAN_TIMEOUT_SECOND)
                params.setScanMode(true)
                scanPb?.visibility = View.GONE
                mDisposable.add(Observable.timer(delay.toLong(), TimeUnit.MILLISECONDS, Schedulers.io())
                        .subscribe { TelinkLightService.Instance().startScan(params) })

            } else {
                DialogUtils.showNoBlePermissionDialog(this, {
                    startScan(0)
                    null
                }, {
                    doFinish()
                    null
                })
            }
        }?.let {
            mDisposable.add(it)
        }

    }

    private fun startAnimation() {
        lottieAnimationView?.playAnimation()
        lottieAnimationView?.visibility = View.VISIBLE
    }

    fun handleIfSupportBle() {
        //检查是否支持蓝牙设备
        if (!LeBluetooth.getInstance().isSupport(applicationContext)) {
            Toast.makeText(this, "ble not support", Toast.LENGTH_SHORT).show()
            doFinish()
            return
        }

        if (!LeBluetooth.getInstance().isEnabled) {
            LeBluetooth.getInstance().enable(applicationContext)
        }
    }

    /**
     * 处理扫描事件
     *
     * @param event
     */
    private fun onLeScan(event: LeScanEvent) {
        val mesh = this.mApplication!!.mesh
        val meshAddress = mesh.generateMeshAddr()

        if (meshAddress == -1) {
            ToastUtils.showLong(getString(R.string.much_lamp_tip))
            if (adapter?.getLights() != null && adapter?.getLights()?.isNotEmpty()!!) {
                stopTimer()
                onLeScanTimeout()
                return
            } else {
                doFinish()
            }
            return
        }
        val deviceInfo = event.args
        if (scanRGBLight) {
            if (checkIsLight(deviceInfo.productUUID) && deviceInfo.productUUID == DeviceType.LIGHT_RGB && deviceInfo.rssi < MAX_RSSI)
                updateMesh(deviceInfo, meshAddress, mesh)
        } else {
            if (checkIsLight(deviceInfo.productUUID) && deviceInfo.productUUID == DeviceType.LIGHT_NORMAL && deviceInfo.rssi < MAX_RSSI)
                updateMesh(deviceInfo, meshAddress, mesh)
        }
    }

    private fun updateMesh(deviceInfo: DeviceInfo, meshAddress: Int, mesh: Mesh) {
        //更新参数
        updateMeshStatus = UPDATE_MESH_STATUS.UPDATING_MESH
        deviceInfo.meshAddress = meshAddress
        val params = Parameters.createUpdateParameters()
        params.setOldMeshName(mesh.factoryName)
        params.setOldPassword(mesh.factoryPassword)
        params.setNewMeshName(mesh.name)
        params.setNewPassword(NetworkFactory.md5(NetworkFactory.md5(mesh.name) + mesh.name!!).substring(0, 16))
        params.setUpdateDeviceList(deviceInfo)
        TelinkLightService.Instance().updateMesh(params)

        Log.d(TAG, "onDeviceStatusChanged_onLeScan: " + deviceInfo.meshAddress + "" +
                "--" + deviceInfo.macAddress + "--productUUID:" + deviceInfo.productUUID)
    }

    private fun checkIsLight(productUUID: Int): Boolean {
        return productUUID == DeviceType.LIGHT_RGB || productUUID == DeviceType.LIGHT_NORMAL || productUUID == DeviceType.LIGHT_NORMAL_OLD
    }

    private fun addDevice(deviceInfo: DeviceInfo) {
        val meshAddress = deviceInfo.meshAddress and 0xFF
        var light = this.adapter!![meshAddress]

        if (light == null) {
            light = DbLight()
            light.name = getString(R.string.unnamed)
            light.meshAddr = meshAddress
            light.textColor = this.resources.getColor(
                    R.color.black)
            light.belongGroupId = allLightId
            light.macAddr = deviceInfo.macAddress
            light.meshUUID = deviceInfo.meshUUID
            light.productUUID = deviceInfo.productUUID
            light.isSelected = false
            adapter?.let {
                it.add(light)
                it.notifyDataSetChanged()
                this.list_devices?.smoothScrollToPosition(it.count - 1)
            }
        }
    }

    private fun onDeviceStatusChanged(event: DeviceEvent) {

        val deviceInfo = event.args

        when (deviceInfo.status) {
            LightAdapter.STATUS_UPDATE_MESH_COMPLETED -> {
                //加灯完成继续扫描,直到扫不到设备
                val deviceInfo1 = com.dadoutek.uled.model.DeviceInfo()
                deviceInfo1.deviceName = deviceInfo.deviceName
                deviceInfo1.firmwareRevision = deviceInfo.firmwareRevision
                deviceInfo1.longTermKey = deviceInfo.longTermKey
                deviceInfo1.macAddress = deviceInfo.macAddress
                TelinkLog.d("deviceInfo-Mac:" + deviceInfo.productUUID)
                deviceInfo1.meshAddress = deviceInfo.meshAddress
                deviceInfo1.meshUUID = deviceInfo.meshUUID
                deviceInfo1.productUUID = deviceInfo.productUUID
                deviceInfo1.status = deviceInfo.status
                deviceInfo1.meshName = deviceInfo.meshName
                if (bestRssiDevice == null) {
                    bestRssiDevice = deviceInfo
                } else {
                    if (bestRssiDevice!!.rssi < deviceInfo.rssi)
                        bestRssiDevice = deviceInfo
                }

                Thread { this@DeviceScanningNewActivity.mApplication!!.mesh.saveOrUpdate(this@DeviceScanningNewActivity) }.start()

                if (scanRGBLight) {
                    if (checkIsLight(deviceInfo1.productUUID) && deviceInfo1.productUUID == DeviceType.LIGHT_RGB)
                        addDevice(deviceInfo)
                } else {
                    if (checkIsLight(deviceInfo1.productUUID) && deviceInfo1.productUUID == DeviceType.LIGHT_NORMAL)
                        addDevice(deviceInfo)
                }

                //扫描出灯就设置为非首次进入
                if (isFirtst) {
                    isFirtst = false
                    SharedPreferencesHelper.putBoolean(this@DeviceScanningNewActivity, SplashActivity.IS_FIRST_LAUNCH, false)
                }
                toolbar?.title = getString(R.string.title_scanning_lights_num, adapter?.count)
                tvStopScan?.visibility = View.VISIBLE

                Log.d("ScanningTest", "update mesh success")
                updateMeshStatus = UPDATE_MESH_STATUS.SUCCESS
                mRetryCount = 0
                this.startScan(0)
            }
            LightAdapter.STATUS_UPDATE_MESH_FAILURE -> {
                //加灯失败继续扫描
                if (mRetryCount < MAX_RETRY_COUNT) {
                    mRetryCount++
                    Log.d("ScanningTest", "update mesh failed , retry count = $mRetryCount")
                    stopTimer()
                    this.startScan(0)
                } else {
                    Log.d("ScanningTest", "update mesh failed , do not retry")
                }
                updateMeshStatus = UPDATE_MESH_STATUS.FAILED
            }

            LightAdapter.STATUS_ERROR_N -> this.onNError(event)
            LightAdapter.STATUS_LOGIN -> {
                Log.d("ScanningTest", "mConnectTimer = $mConnectTimer")
                if (mConnectTimer != null && !mConnectTimer!!.isDisposed) {
                    Log.d("ScanningTest", " !mConnectTimer.isDisposed() = " + !mConnectTimer!!.isDisposed)
                    mConnectTimer!!.dispose()
                    isLoginSuccess = true
                    //进入分组
                    hideLoadingDialog()
                    startGrouping()
                }
            }
            LightAdapter.STATUS_LOGOUT -> isLoginSuccess = false
        }
    }

    companion object {
        private val MAX_RETRY_COUNT = 4   //update mesh failed的重试次数设置为4次
        private val MAX_RSSI = 90
        private val TAG = DeviceScanningNewActivity::class.java.simpleName
        private val SCAN_TIMEOUT_SECOND = 10
        private val TIME_OUT_CONNECT = 15
    }

}

package com.dadoutek.uled.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.communicate.Commander.connect
import com.dadoutek.uled.connector.ConnectorOfGroupActivity
import com.dadoutek.uled.connector.ConnectorSettingActivity
import com.dadoutek.uled.curtain.CurtainOfGroupActivity
import com.dadoutek.uled.curtains.WindowCurtainsActivity
import com.dadoutek.uled.gateway.bean.GwStompBean
import com.dadoutek.uled.gateway.util.Base64Utils
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.light.LightsOfGroupActivity
import com.dadoutek.uled.light.NormalSettingActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.dbModel.*
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.httpModel.GwModel
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.model.httpModel.GroupMdodel
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.network.GwGattBody
import com.dadoutek.uled.network.RouterDelGpBody
import com.dadoutek.uled.othersview.BaseFragment
import com.dadoutek.uled.othersview.InstructionsForUsActivity
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.rgb.RGBSettingActivity
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.router.bean.RouteGroupingOrDelBean
import com.dadoutek.uled.stomp.MqttBodyBean
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.SharedPreferencesUtils
import com.dadoutek.uled.util.StringUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import com.telink.bluetooth.light.ConnectionStatus
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.support.v4.runOnUiThread
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

abstract class BaseGroupFragment : BaseFragment() {
    var disposableRouteTimer: Disposable? = null
    private var seehelp: TextView? = null
    private var seeHelp2: TextView? = null
    private var addGroupTv: TextView? = null
    private var mConnectDisposable: Disposable? = null
    var currentPosition: Int = 0
    private var disposableTimer: Disposable? = null
    var currentGroup: DbGroup? = null
    private var lin: View? = null
    private var inflater: LayoutInflater? = null
    private var recyclerView: RecyclerView? = null
    private var noGroup: LinearLayout? = null
    private var groupAdapter: GroupListAdapter? = null
    open var groupList: ArrayList<DbGroup> = ArrayList()
    private var isFristUserClickCheckConnect = true
    private var updateLightDisposal: Disposable? = null
    private var mContext: Activity? = null
    private var addGroupBtn: ConstraintLayout? = null
    private var viewLine: View? = null
    private var viewLineRecycler: View? = null
    private var isDelete = false
    private var groupMesher: ArrayList<String>? = null
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private lateinit var br: BroadcastReceiver
    private lateinit var deleteList: ArrayList<DbGroup>
    private var addNewGroup: Button? = null
    private var compositeDisposable = CompositeDisposable()
    private var isDeleteSucess = false

    var renameCancel: TextView? = null
    var renameConfirm: TextView? = null
    var textGp: EditText? = null
    var popReNameView: View? = null
    lateinit var renameDialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.mContext = this.activity
        setHasOptionsMenu(true)
        makeRenamePopuwindow()
        localBroadcastManager = LocalBroadcastManager.getInstance(this.mContext!!)
        val intentFilter = IntentFilter()
        intentFilter.addAction("back")
        intentFilter.addAction("delete")
        intentFilter.addAction("switch")
        intentFilter.addAction("switch_here")
        intentFilter.addAction("delete_true")
        intentFilter.addAction("isDelete")
        intentFilter.addAction("is")
        br = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val key = intent.getStringExtra("back")
                val sonDeleteGroup = intent.getStringExtra("delete")
                val lightStatus = intent.getStringExtra("switch_here")
                val delete = intent.getStringExtra("delete_true")
                val isDeleteReceive = intent.getStringExtra("isDelete")//必定将删除模式恢复掉
                if (key == "true" || delete == "true" || isDeleteReceive == "true") {
                    isDelete = false
                    groupAdapter!!.changeState(isDelete)
                    groupList.let {
                        for (i in it.indices)
                            if (it[i].isSelected)
                                it[i].isSelected = false
                    }
                    refreshData()
                }
                if (sonDeleteGroup == "true") {
                    deleteList = ArrayList()
                    for (i in groupList.indices)
                        if (groupList[i].isSelected)
                            deleteList.add(groupList[i])

                    isDeleteSucess = false
                    for (j in deleteList.indices) {
                        showLoadingDialog(getString(R.string.deleting))
                        Thread.sleep(300)
                        val dbGroup = deleteList[j]
                        val lights = DBUtils.getLightByGroupID(dbGroup.id)
                        deleteGroup(lights, dbGroup,
                                successCallback = {
                                    isDeleteSucess = true
                                    if (j == deleteList.size - 1 && isDeleteSucess) {
                                        hideLoadingDialog()
                                        isDelete = false
                                        sendDeleteBrocastRecevicer(300)
                                        refreshData()
                                    }
                                },
                                failedCallback = {
                                    if (j == deleteList.size - 1)
                                        hideLoadingDialog()
                                    ToastUtils.showLong(R.string.move_out_some_lights_in_group_failed)
                                })
                    }
                }

                when (lightStatus) {
                    "on" -> {
                        for (i in groupList.indices) {
                            groupList[i].connectionStatus = ConnectionStatus.ON.value
                            DBUtils.updateGroup(groupList[i])
                            groupAdapter!!.notifyDataSetChanged()
                        }
                    }
                    "false" -> {
                        for (i in groupList.indices) {
                            groupList[i].connectionStatus = ConnectionStatus.OFF.value
                            DBUtils.updateGroup(groupList[i])
                            groupAdapter!!.notifyDataSetChanged()
                        }
                    }
                }
            }
        }
        localBroadcastManager.registerReceiver(br, intentFilter)
    }


    private fun sendDeleteBrocastRecevicer(delayTime: Long) {
        Thread.sleep(delayTime)
        val intent = Intent("delete_true")
        intent.putExtra("delete_true", "true")
        LocalBroadcastManager.getInstance(this.mContext!!).sendBroadcast(intent)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = getView(inflater)
        this.initData()
        return view
    }

    private fun makeRenamePopuwindow() {
        popReNameView = View.inflate(mContext, R.layout.pop_rename, null)
        textGp = popReNameView?.findViewById(R.id.pop_rename_edt)
        textGp?.hint = getString(R.string.please_rename)
        renameCancel = popReNameView?.findViewById(R.id.pop_rename_cancel)
        renameConfirm = popReNameView?.findViewById(R.id.pop_rename_confirm)
        StringUtils.initEditTextFilter(textGp)

        renameDialog = Dialog(mContext)
        renameDialog?.setContentView(popReNameView)
        renameDialog?.setCanceledOnTouchOutside(false)
        renameCancel?.setOnClickListener { renameDialog?.dismiss() }
        //确定回调 单独写
    }

    private fun getView(inflater: LayoutInflater): View {
        this.inflater = inflater
        val view = inflater.inflate(R.layout.group_list_fragment, null)
        groupMesher = ArrayList()
        noGroup = view.findViewById(R.id.no_group)
        recyclerView = view.findViewById(R.id.group_recyclerView)
        addNewGroup = view.findViewById(R.id.add_device_btn)
        viewLine = view.findViewById(R.id.view)
        seehelp = view.findViewById(R.id.group_see_helpe)
        viewLineRecycler = view.findViewById(R.id.viewLine)

        lin = LayoutInflater.from(activity).inflate(R.layout.template_add_help, null)
        addGroupTv = lin?.findViewById(R.id.main_add_device)
        addGroupTv?.text = getString(R.string.add_groups)
        seeHelp2 = lin?.findViewById(R.id.main_go_help)

        return view
    }

    override fun onResume() {
        super.onResume()
        isFristUserClickCheckConnect = true
        currentGroup?.id?.let {
            currentGroup = DBUtils.getGroupByID(it)
        }
        refreshData()
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        if (isVisibleToUser)
            refreshData()
    }

    private fun initData() {
        groupList = ArrayList()
        groupList.addAll(getGroupData())

        if (groupList.size > 0) {
            noGroup?.visibility = View.GONE
            recyclerView?.visibility = View.VISIBLE
            addGroupBtn?.visibility = View.VISIBLE
            viewLine?.visibility = View.VISIBLE
            viewLineRecycler?.visibility = View.VISIBLE
        } else {
            noGroup?.visibility = View.VISIBLE
            recyclerView?.visibility = View.GONE
            addGroupBtn?.visibility = View.GONE
            viewLine?.visibility = View.GONE
            viewLineRecycler?.visibility = View.GONE
        }

        //发送切换fragment广播
        val intent = Intent("switch_fragment")
        intent.putExtra("switch_fragment", "true")
        LocalBroadcastManager.getInstance(this.mContext!!).sendBroadcast(intent)

        addGroupBtn?.setOnClickListener(onClickAddGroup)
        addNewGroup?.setOnClickListener(onClickAddGroup)
        addGroupTv?.setOnClickListener(onClickAddGroup)
        seeHelp2?.setOnClickListener { seeHelpe() }
        seehelp?.setOnClickListener { seeHelpe() }

        val layoutmanager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        // recyclerView!!.layoutManager = layoutmanager
        recyclerView!!.layoutManager = GridLayoutManager(context, 2)

        Collections.sort(groupList, kotlin.Comparator { o1, o2 ->
            return@Comparator o1.name.compareTo(o2.name)
        })

        //this.groupAdapter = GroupListAdapter(R.layout.group_item_child, groupList, isDelete)
        this.groupAdapter = GroupListAdapter(R.layout.template_device_type_item, groupList, isDelete)
        val decoration = DividerItemDecoration(activity, DividerItemDecoration.VERTICAL)
        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(activity!!, R.color.divider)))
        //添加分割线
        groupAdapter!!.addFooterView(lin)
        groupAdapter!!.onItemChildClickListener = onItemChildClickListener
        //如果是自己的区域才允许长按删除
        groupAdapter!!.onItemLongClickListener = onItemChildLongClickListener
        groupAdapter!!.bindToRecyclerView(recyclerView)
    }

    open fun seeHelpe() {
        var wbType = when (setGroupType()) {
            Constant.DEVICE_TYPE_LIGHT_NORMAL -> "#control-normal-group"
            Constant.DEVICE_TYPE_LIGHT_RGB -> "#control-color-light-group"
            Constant.DEVICE_TYPE_CURTAIN -> "#control-curtain-group"
            Constant.DEVICE_TYPE_CONNECTOR -> "#control-relay-group"
            else -> "#control-normal-group"
        }
        var intent = Intent(mContext, InstructionsForUsActivity::class.java)
        intent.putExtra(Constant.WB_TYPE, wbType)
        startActivity(intent)
    }

    private var onItemChildLongClickListener = BaseQuickAdapter.OnItemLongClickListener { _, _, postion ->
        val lastUser = DBUtils.lastUser
        lastUser?.let {
            if (it.id.toString() != it.last_authorizer_user_id) {
                ToastUtils.showLong(getString(R.string.author_region_warm))
            } else {
                if (groupList[postion].deviceType == Constant.DEVICE_TYPE_CURTAIN || groupList[postion].deviceType == Constant.DEVICE_TYPE_CONNECTOR || postion != 0) {
                    when {
                        !isDelete -> {
                            isDelete = true
                            SharedPreferencesUtils.setDelete(true)
                            val intent = Intent("showPro")
                            intent.putExtra("is_delete", "true")
                            this.activity?.let { it1 ->
                                LocalBroadcastManager.getInstance(it1).sendBroadcast(intent)
                            }
                        }
                        else -> {//先长按  选中 在长按 就会通知外面关闭了
                            isDelete = false
                            val intent = Intent("showPro")
                            intent.putExtra("is_delete", "false")
                            this.activity?.let { it1 ->
                                LocalBroadcastManager.getInstance(it1).sendBroadcast(intent)
                            }
                        }
                    }
                    SharedPreferencesHelper.putBoolean(TelinkLightApplication.getApp(), Constant.IS_DELETE, isDelete)
                    groupAdapter?.changeState(isDelete)
                    groupList[postion].isSelected = isDelete
                    refreshData()
                }
            }
        }
        return@OnItemLongClickListener true
    }

    fun refreshData() {
        groupList.clear()
        groupList.addAll(getGroupData())//根据设备类型获取设备组数
        //以下是检索组里有多少设备的代码
        for (group in groupList) {
            when (group.deviceType) {
                //查询改组内设备数量  普通灯和冷暖灯是一个方法  查询什么设备类型有grouplist内容决定
                Constant.DEVICE_TYPE_LIGHT_NORMAL -> group.deviceCount = DBUtils.getLightByGroupID(group.id).size
                Constant.DEVICE_TYPE_LIGHT_RGB -> group.deviceCount = DBUtils.getLightByGroupID(group.id).size  //查询改组内设备数量
                Constant.DEVICE_TYPE_CONNECTOR -> group.deviceCount = DBUtils.getRelayByGroupID(group.id).size  //查询改组内设备数量
                Constant.DEVICE_TYPE_CURTAIN -> group.deviceCount = DBUtils.getCurtainByGroupID(group.id).size  //查询改组内设备数量//窗帘和传感器是一个方法
            }
        }

        if (groupList.size > 0) {
            noGroup?.visibility = View.GONE
            recyclerView?.visibility = View.VISIBLE
        } else {
            noGroup?.visibility = View.VISIBLE
            recyclerView?.visibility = View.GONE
        }
        groupAdapter?.notifyDataSetChanged()
    }

    abstract fun getGroupData(): Collection<DbGroup>


    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendToGw(isOpen: Boolean) {
        val gateWay = DBUtils.getAllGateWay()
        if (gateWay.size > 0)
            GwModel.getGwList()?.subscribe({
                TelinkLightApplication.getApp().offLine = true
                hideLoadingDialog()
                it.forEach { db ->
                    //网关在线状态，1表示在线，0表示离线
                    if (db.state == 1)
                        TelinkLightApplication.getApp().offLine = false
                }
                if (!TelinkLightApplication.getApp().offLine) {
                    disposableTimer?.dispose()
                    disposableTimer = Observable.timer(7000, TimeUnit.MILLISECONDS).subscribe {
                        hideLoadingDialog()
                        runOnUiThread { ToastUtils.showShort(getString(R.string.gate_way_offline)) }
                    }
                    val low = currentGroup!!.meshAddr and 0xff
                    val hight = (currentGroup!!.meshAddr shr 8) and 0xff
                    val gattBody = GwGattBody()
                    var gattPar: ByteArray
                    if (isOpen) {
                        gattPar = byteArrayOf(0x11, 0x11, 0x11, 0, 0, low.toByte(), hight.toByte(), Opcode.LIGHT_ON_OFF,
                                0x11, 0x02, 0x01, 0x64, 0, 0, 0, 0, 0, 0, 0, 0)
                        gattBody.ser_id = Constant.SER_ID_GROUP_ON
                    } else {
                        gattPar = byteArrayOf(0x11, 0x11, 0x11, 0, 0, low.toByte(), hight.toByte(), Opcode.LIGHT_ON_OFF,
                                0x11, 0x02, 0x00, 0x64, 0, 0, 0, 0, 0, 0, 0, 0)
                        gattBody.ser_id = Constant.SER_ID_GROUP_OFF
                    }

                    //val encoder = Base64.getEncoder()
                    // val s = encoder.encodeToString(gattPar)
                    gattBody.data = Base64Utils.encodeToStrings(gattPar)
                    gattBody.cmd = Constant.CMD_MQTT_CONTROL
                    gattBody.meshAddr = currentGroup!!.meshAddr
                    sendToServer(gattBody, isOpen)
                } else {
                    ToastUtils.showShort(getString(R.string.gw_not_online))
                }
            }, {
                hideLoadingDialog()
                ToastUtils.showShort(getString(R.string.gw_not_online))
            })
    }

    @SuppressLint("CheckResult")
    private fun sendToServer(gattBody: GwGattBody, open: Boolean) {
        GwModel.sendDeviceToGatt(gattBody)?.subscribe({
            disposableTimer?.dispose()
            updateLights(open, currentGroup!!)
            LogUtils.v("zcl-----------远程控制-------$it")
        }, {
            disposableTimer?.dispose()
            ToastUtils.showShort(it.message)
            LogUtils.v("zcl-----------远程控制-------${it.message}")
        })
    }

    @SuppressLint("CheckResult")
    open fun routeOpenOrClose(meshAddr: Int, productUUID: Int, status: Int, serId: String) {//如果发送后失败则还原
        RouterModel.routeOpenOrClose(meshAddr, productUUID, status, serId)?.subscribe({
            LogUtils.v("zcl----meshAddr-$meshAddr----组${DBUtils.getGroupByMeshAddr(meshAddr)}--收到路由组成功-------$it")
            //    "errorCode": 90018,该设备不存在，请重新刷新数据"
            //    "errorCode": 90008,该设备没有绑定路由，无法操作"
            //   "errorCode": 90007该组不存在，请重新刷新数据"
            //    errorCode": 90005,"message": "该设备绑定的路由没在线"
            when (it.errorCode) {
                0 -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    disposableRouteTimer?.dispose()
                    disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                hideLoadingDialog()
                                ToastUtils.showShort(getString(R.string.open_light_faile))
                            }
                }
                90018 -> {
                    DBUtils.deleteLocalData()
                    //ToastUtils.showShort(mContext?.getString(R.string.device_not_exit))
                    SyncDataPutOrGetUtils.syncGetDataStart(DBUtils.lastUser!!, object : SyncCallback {
                        override fun start() {}
                        override fun complete() {}
                        override fun error(msg: String?) {}
                    })
                }
                90008 -> {
                    hideLoadingDialog()
                    ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                }
                90007 -> ToastUtils.showShort(getString(R.string.gp_not_exit))
                90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                else -> ToastUtils.showShort(it.message)
            }
        }, {
            LogUtils.v("zcl-----------收到路由开关组失败-------$it")
            ToastUtils.showShort(it.message)
        })
    }

    override fun tzRouterOpenOrCloseFragment(cmdBean: CmdBodyBean) {
        disposableRouteTimer?.dispose()
        hideLoadingDialog()
        if (cmdBean.ser_id == "zu" && currentGroup != null) {
            LogUtils.v("zcl------收到路由开关组通知------------$cmdBean")
            when (cmdBean.status) {
                0 -> {
                    groupSwSuccess(currentPosition, false)
                    DBUtils.saveGroup(currentGroup!!, true)
                }
                else -> {
                    if (currentGroup?.connectionStatus == 0) 1 else 0
                    ToastUtils.showShort(getString(R.string.open_light_faile))
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { _, view, position ->
        currentGroup = groupList[position]
        currentPosition = position
        val dstAddr = currentGroup!!.meshAddr
        val groupType = setGroupType()

        when (view!!.id) {
            R.id.template_device_icon -> {
                when {
                    TelinkLightApplication.getApp().connectDevice == null && !Constant.IS_ROUTE_MODE && !TelinkLightApplication.getApp().isConnect -> {
                        goConnect()
                        sendToGw(currentGroup?.connectionStatus == ConnectionStatus.OFF.value)
                    }
                    currentGroup!!.deviceType == Constant.DEVICE_TYPE_LIGHT_RGB || currentGroup!!.deviceType == Constant.DEVICE_TYPE_LIGHT_NORMAL
                            || currentGroup!!.deviceType == Constant.DEVICE_TYPE_CONNECTOR || currentGroup!!.deviceType == Constant.DEVICE_TYPE_NO
                            ||currentGroup!!.deviceType == Constant.DEVICE_TYPE_CURTAIN-> {
                        when {
                            Constant.IS_ROUTE_MODE -> {// status 是	int	0关1开   meshType普通灯 = 4 彩灯 = 6 连接器 = 5 组 = 97
                                var status = if (currentGroup!!.connectionStatus == 0) 1 else 0
                                LogUtils.v("zcl---请求路由开关组之前--connectionStatus-----${currentGroup!!.connectionStatus}--status--$status")
                                routeOpenOrClose(currentGroup!!.meshAddr, 97, status, "zu")
                                currentGroup?.connectionStatus = status
                            }
                            else -> {
                                var isopen = currentGroup!!.connectionStatus == 0 //0位关闭 则去打开
                                when (currentGroup!!.deviceType) {
                                    Constant.DEVICE_TYPE_CURTAIN -> {
                                        val elements: Byte = if (isopen) 0x0A else 0x0C
                                        val opcode = Opcode.CURTAIN_ON_OFF
                                        val params = byteArrayOf(Opcode.CURTAIN_PACK_START, elements, 0x00, Opcode.CURTAIN_PACK_END)
                                        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, currentGroup!!.meshAddr, params)
                                    }
                                    else -> Commander.openOrCloseLights(dstAddr, isopen)
                                }
                                LogUtils.v("zcl-----------本地发送开关命令------isopen$isopen-$dstAddr")
                                groupSwSuccess(position, true)
                            }
                        }
                        if (Constant.IS_ROUTE_MODE)
                            DBUtils.saveGroup(currentGroup!!, true)
                    }
                    currentGroup!!.deviceType == Constant.DEVICE_TYPE_CURTAIN -> {
                        if (Constant.IS_ROUTE_MODE)
                            routeSwitchCurtain()
                    }
                }
            }

            R.id.template_device_setting -> {
                val lastUser = DBUtils.lastUser
                lastUser?.let {
                    val isLight = groupType == Constant.DEVICE_TYPE_LIGHT_NORMAL || groupType == Constant.DEVICE_TYPE_LIGHT_RGB
                    when {
                        isLight && position == 0 -> {//进入所有灯    meshType普通灯 = 4 彩灯 = 6 连接器 = 5 组 = 97
                            if (TelinkLightApplication.getApp().connectDevice != null || Constant.IS_ROUTE_MODE) {
                                val intentSetting = Intent(context, NormalSettingActivity::class.java)
                                intentSetting.putExtra(Constant.TYPE_VIEW, Constant.TYPE_GROUP)
                                intentSetting.putExtra("group", DBUtils.allGroups[0])
                                startActivityForResult(intentSetting, 1)
                            } else {
                                ToastUtils.showShort(getString(R.string.device_not_connected))
                                val activity = activity as MainActivity
                                activity.autoConnect()
                            }
                        }
                        currentGroup!!.deviceType != Constant.DEVICE_TYPE_DEFAULT_ALL && (currentGroup!!.deviceType == groupType) -> {
                            var num = 0
                            when (groupType) {
                                Constant.DEVICE_TYPE_LIGHT_NORMAL -> num = DBUtils.getLightByGroupID(currentGroup!!.id).size
                                Constant.DEVICE_TYPE_LIGHT_RGB -> num = DBUtils.getLightByGroupID(currentGroup!!.id).size
                                //蓝牙接收器
                                Constant.DEVICE_TYPE_CONNECTOR -> num = DBUtils.getRelayByGroupID(currentGroup!!.id).size
                                Constant.DEVICE_TYPE_CURTAIN -> num = DBUtils.getCurtainByGroupID(currentGroup!!.id).size
                            }

                            if (num != 0) {
                                var intent: Intent? = null
                                when (groupType) {
                                    Constant.DEVICE_TYPE_LIGHT_NORMAL -> intent = Intent(mContext, NormalSettingActivity::class.java)
                                    Constant.DEVICE_TYPE_LIGHT_RGB -> intent = Intent(mContext, RGBSettingActivity::class.java)
                                    //蓝牙接收器
                                    Constant.DEVICE_TYPE_CONNECTOR -> intent = Intent(mContext, ConnectorSettingActivity::class.java)
                                    Constant.DEVICE_TYPE_CURTAIN -> intent = Intent(mContext, WindowCurtainsActivity::class.java)
                                }
                                intent?.putExtra(Constant.TYPE_VIEW, Constant.TYPE_GROUP)
                                intent?.putExtra("group", currentGroup)
                                if (!TelinkLightService.Instance().isLogin && !Constant.IS_ROUTE_MODE) {
                                    goConnect()
                                } else {
                                    startActivityForResult(intent, 2)
                                }
                            }
                        }
                    }
                }
            }

            R.id.template_device_card_delete -> deleteSingleGroup(currentGroup!!)

            //不能使用group_name否则会造成长按监听无效 跳转组详情
            //  R.id.item_layout -> {
            R.id.template_device_more -> {
                if (TelinkLightService.Instance()?.isLogin == true || Constant.IS_ROUTE_MODE) {
                    var intent = Intent()
                    when (groupType) {
                        Constant.DEVICE_TYPE_LIGHT_NORMAL -> {
                            intent = Intent(mContext, LightsOfGroupActivity::class.java)
                            intent.putExtra("light", "cw_light")
                        }
                        Constant.DEVICE_TYPE_LIGHT_RGB -> {
                            intent = Intent(mContext, LightsOfGroupActivity::class.java)
                            intent.putExtra("light", "rgb_light")
                        }//蓝牙接收器
                        Constant.DEVICE_TYPE_CONNECTOR -> {
                            intent = Intent(mContext, ConnectorOfGroupActivity::class.java)
                        }
                        Constant.DEVICE_TYPE_CURTAIN -> {
                            intent = Intent(mContext, CurtainOfGroupActivity::class.java)
                        }
                    }
                    intent.putExtra("group", currentGroup)
                    startActivityForResult(intent, 2)
                } else {
                    goConnect()
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun routeSwitchCurtain() {//controlCmd 开 = 0x0a 暂停 = 0x0b 关 = 0x0c调节速度 = 0x15 恢复出厂 = 0xec 重启 = 0xea 换向 = 0x11
        currentGroup?.let {
            var opcode = if (it.status == 1) 0x0c else 0x0a

            RouterModel.routeControlCurtain(it.meshAddr, 97, opcode, 1, "groupSwCurtain")//换向 = 0x11
                    ?.subscribe({ itr ->
                        LogUtils.v("zcl-----------收到路由控制-开0x0a 暂停0x0b 关0x0c调节速度 0x15 恢复出厂 0xec 重启 0xea 0x11--$opcode----$it")
                        when (itr.errorCode) {
                            0 -> {
                                showLoadingDialog(getString(R.string.please_wait))
                                disposableRouteTimer?.dispose()
                                disposableRouteTimer = Observable.timer(itr.t.timeout.toLong(), TimeUnit.SECONDS)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe {
                                            hideLoadingDialog()
                                            when (opcode) {
                                                0x0a -> ToastUtils.showShort(getString(R.string.open_faile))
                                                0x0c -> ToastUtils.showShort(getString(R.string.close_faile))
                                            }
                                        }
                            }
                            90018 -> {
                                DBUtils.deleteLocalData()
                                //ToastUtils.showShort(getString(R.string.device_not_exit))
                                SyncDataPutOrGetUtils.syncGetDataStart(DBUtils.lastUser!!, object : SyncCallback {
                                    override fun start() {}
                                    override fun complete() {}
                                    override fun error(msg: String?) {}
                                })
                            }
                            90008 -> {
                                hideLoadingDialog()
                                ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                            }
                            90007 -> ToastUtils.showShort(getString(R.string.gp_not_exit))
                            90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                            else -> ToastUtils.showShort(itr.message)
                        }
                    }, { itt ->
                        ToastUtils.showShort(itt.message)
                    })
        }
    }

    @SuppressLint("StringFormatInvalid")
    private fun deleteSingleGroup(dbGroup: DbGroup) {
        AlertDialog.Builder(mContext)
                .setMessage(getString(R.string.delete_group_confirm, dbGroup?.name))
                .setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
                    if (Constant.IS_ROUTE_MODE) {
                        routeDeleteGroup(dbGroup)
                    } else {
                        when (dbGroup.deviceType) {
                            Constant.DEVICE_TYPE_LIGHT_RGB, Constant.DEVICE_TYPE_LIGHT_NORMAL -> {
                                val lights = DBUtils.getLightByGroupID(dbGroup.id)
                                showLoadingDialog(getString(R.string.please_wait))
                                deleteGroup(lights, dbGroup,
                                        successCallback = {
                                            deleteComplete()
                                        },
                                        failedCallback = {
                                            deleteFailToast()
                                        })
                            }
                            Constant.DEVICE_TYPE_CURTAIN -> {
                                val lights = DBUtils.getCurtainByGroupID(dbGroup.id)
                                showLoadingDialog(getString(R.string.please_wait))
                                deleteGroupCurtain(lights, dbGroup,
                                        successCallback = {
                                            deleteComplete()
                                        },
                                        failedCallback = {
                                            deleteFailToast()
                                        })
                            }
                            Constant.DEVICE_TYPE_CONNECTOR -> {
                                val lights = DBUtils.getRelayByGroupID(dbGroup.id)
                                showLoadingDialog(getString(R.string.please_wait))
                                deleteGroupRelay(lights, dbGroup,
                                        successCallback = {
                                            deleteComplete()
                                        },
                                        failedCallback = {
                                            deleteFailToast()
                                        })
                            }
                        }
                    }

                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()
    }

    @SuppressLint("CheckResult")
    private fun routeDeleteGroup(dbGroup: DbGroup) {
        RouterModel.routerDelGp(RouterDelGpBody("delGp", dbGroup.meshAddr))?.subscribe({
            /**
            90007,"该组不存在，本地删除即可 "  90015,"空组直接本地删除，后台数据库也会同步删除(无需app调用删除接口)"
            90008,该组里的全部设备都未绑定路由，无法删除" 90005,"以下路由全部没有上线，无法开始分组" 90009,"默认组无法删除"
            }
             */
            when (it.errorCode) {
                0, 90015, 90007 -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    if (it.errorCode == 0) {
                        disposableRouteTimer?.dispose()
                        disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe {
                                    hideLoadingDialog()
                                    ToastUtils.showShort(getString(R.string.delete_gp_fail))
                                }
                    } else {
                        DBUtils.deleteGroupOnly(dbGroup)
                        deleteComplete()
                    }
                }
                90008 -> {
                    hideLoadingDialog()
                    ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                }
                90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                90009 -> ToastUtils.showShort(getString(R.string.all_gp_cont_del))
                else -> ToastUtils.showShort(it.message)
            }
            LogUtils.v("zcl-----------收到路由删组-------$it")
        }, {
            ToastUtils.showShort(it.message)

        })
    }

    @SuppressLint("StringFormatInvalid", "StringFormatMatches")
    override fun tzRouterDelGroupResult(routerGroup: RouteGroupingOrDelBean) {
        LogUtils.v("zcl-----------收到路由删组通知-------${routerGroup}")
        disposableTimer?.dispose()
        disposableRouteTimer?.dispose()
        if (routerGroup.ser_id == "delGp") {
            hideLoadingDialog()
            if (routerGroup?.finish) {
                val gp = DBUtils.getGroupByID(routerGroup.targetGroupId.toLong())
                when (routerGroup?.status) {
                    0 -> {
                        if (gp != null)
                            DBUtils.deleteGroupOnly(gp!!)
                        deleteComplete()
                        SyncDataPutOrGetUtils.syncGetDataStart(DBUtils.lastUser!!, object : SyncCallback {
                            override fun start() {}
                            override fun complete() {}
                            override fun error(msg: String?) {}
                        })
                        ToastUtils.showShort(getString(R.string.delete_group_success, routerGroup.succeedNow.size))
                    }
                    1 -> ToastUtils.showShort(getString(R.string.delete_group_some_fail))
                    -1 -> ToastUtils.showShort(getString(R.string.delete_gp_fail))
                }
            } else {
                ToastUtils.showShort(getString(R.string.router_del_gp, routerGroup?.succeedNow?.size))
            }
        }
    }

    private fun deleteFailToast() {
        hideLoadingDialog()
        ToastUtils.showLong(R.string.move_out_some_lights_in_group_failed)
    }

    private fun deleteComplete() {
        isDeleteSucess = true
        hideLoadingDialog()
        sendDeleteBrocastRecevicer(300)
        refreshData()
    }

    private fun goConnect(ishow: Boolean = true) {
        if (Constant.IS_ROUTE_MODE)
            return
        val deviceTypes = mutableListOf(DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD, DeviceType.LIGHT_RGB)
        if (ishow)
            ToastUtils.showShort(getString(R.string.connecting_tip))
        mConnectDisposable?.dispose()
        mConnectDisposable = connect(deviceTypes = deviceTypes, fastestMode = true, retryTimes = 10)
                ?.subscribe({}, { LogUtils.d("connect failed") })
    }

    fun groupSwSuccess(position: Int, isChnage: Boolean) {
        if (isChnage)
            currentGroup?.connectionStatus = if (currentGroup!!.connectionStatus == 0) ConnectionStatus.ON.value else ConnectionStatus.OFF.value
        groupAdapter?.notifyItemChanged(position)
        GlobalScope.launch {
            currentGroup?.let {
                DBUtils.updateGroup(it)
                LogUtils.v("zcl--------收到路由通知后更新群组------${it.status}-currentGroup${currentGroup?.status}")
                updateLights(true, it)
            }
        }
    }

    abstract fun setIntentDeviceType(): String?

    override fun onStop() {
        super.onStop()
        isFristUserClickCheckConnect = false
    }

    private fun updateLights(isOpen: Boolean, group: DbGroup) {
        updateLightDisposal?.dispose()
        updateLightDisposal = Observable.timer(300, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    var lightList: MutableList<DbLight> = ArrayList()
                    when (group.meshAddr) {
                        0xffff -> {//如果是所有组 那么所有灯就都更新
                            val list = DBUtils.groupList
                            for (j in list.indices)
                                lightList.addAll(DBUtils.getLightByGroupID(list[j].id))
                        }
                        else -> lightList = DBUtils.getLightByGroupID(group.id)
                    }

                    for (dbLight: DbLight in lightList) {
                        when {
                            isOpen -> dbLight.connectionStatus = ConnectionStatus.ON.value
                            else -> dbLight.connectionStatus = ConnectionStatus.OFF.value
                        }
                        DBUtils.updateLightLocal(dbLight)//更新灯的状态
                    }
                }
    }

    private val onClickAddGroup = View.OnClickListener {
        addNewGroupMode()
    }

    private fun addNewGroupMode() {
        val lastUser = DBUtils.lastUser
        lastUser?.let {
            if (it.id.toString() != it.last_authorizer_user_id)
                ToastUtils.showLong(getString(R.string.author_region_warm))
            else
                addNewGroup()
        }
    }


    @SuppressLint("CheckResult")
    private fun addNewGroup() {
        StringUtils.initEditTextFilter(textGp)
        if (this != null && mContext?.isFinishing == false) {
            renameDialog?.dismiss()
            renameDialog?.show()
        }

        renameConfirm?.setOnClickListener {    // 获取输入框的内容
            if (StringUtils.compileExChar(textGp?.text.toString().trim { it <= ' ' })) {
                ToastUtils.showLong(getString(R.string.rename_tip_check))
            } else {//往DB里添加组数据
                val dbGroup = DBUtils.addNewGroupWithType(textGp?.text.toString().trim { it <= ' ' }, setGroupType())
                dbGroup?.let {
                    if (Constant.IS_ROUTE_MODE) {
                        GroupMdodel.batchAddOrUpdateGp(mutableListOf(dbGroup!!))?.subscribe({
                            if (it.errorCode == 0)
                                addGroupSuccess(dbGroup)
                            else
                                ToastUtils.showShort(getString(R.string.add_group_fail))
                        }, {
                            ToastUtils.showShort(it.message)
                        })
                    } else {
                        addGroupSuccess(dbGroup)
                    }
                }
                renameDialog.dismiss()
            }
        }
    }

    private fun addGroupSuccess(dbGroup: DbGroup?) {
        groupList?.add(dbGroup!!)
        refreshData()
    }

    abstract fun setGroupType(): Long


    /**
     * 删除组，并且把组里的灯的组也都删除。
     */
    private fun deleteGroup(lights: MutableList<DbLight>, group: DbGroup, retryCount: Int = 0,
                            successCallback: () -> Unit, failedCallback: () -> Unit) {
        Thread {
            if (lights.count() != 0) {
                val maxRetryCount = 3
                if (retryCount <= maxRetryCount) {
                    val light = lights[0]
                    val lightMeshAddr = light.meshAddr
                    Commander.deleteGroup(lightMeshAddr, successCallback = {
                        light.belongGroupId = DBUtils.groupNull!!.id//该等所在组
                        DBUtils.updateLight(light)
                        lights.remove(light)
                        //修改分组成功后删除场景信息。
                        deleteAllSceneByLightAddr(light.meshAddr)
                        Thread.sleep(100)
                        if (lights.count() == 0) {
                            //所有灯都删除了分组
                            DBUtils.deleteGroupOnly(group)//亲删除改组
                            runOnUiThread {
                                successCallback.invoke()
                            }
                        } else //还有灯要删除分组
                            deleteGroup(lights, group, successCallback = successCallback, failedCallback = failedCallback)

                        LogUtils.e("zcl删除组后" + DBUtils.getGroupsByDeviceType(DeviceType.LIGHT_RGB))
                    }, failedCallback = {
                        deleteGroup(lights, group, retryCount = retryCount + 1, successCallback = successCallback, failedCallback = failedCallback)
                    })
                } else {//超过了重试次数
                    runOnUiThread {
                        failedCallback.invoke()
                    }
                }
            } else {
                DBUtils.deleteGroupOnly(group)
                runOnUiThread {
                    successCallback.invoke()
                }
            }
        }.start()

    }

    /**
     * 删除组，并且把组里的灯的组也都删除。
     */
    private fun deleteGroupRelay(lights: MutableList<DbConnector>, group: DbGroup, retryCount: Int = 0,
                                 successCallback: () -> Unit, failedCallback: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            if (lights.count() != 0) {
                val maxRetryCount = 3
                if (retryCount <= maxRetryCount) {
                    val light = lights[0]
                    val lightMeshAddr = light.meshAddr
                    Commander.deleteGroup(lightMeshAddr, successCallback = {
                        light.belongGroupId = DBUtils.groupNull!!.id//该等所在组
                        DBUtils.updateConnector(light)
                        lights.remove(light)
                        //修改分组成功后删除场景信息。
                        // deleteAllSceneByLightAddr(light.meshAddr)
                        Thread.sleep(100)
                        if (lights.count() == 0) {
                            //所有灯都删除了分组
                            DBUtils.deleteGroupOnly(group)//亲删除改组
                            runOnUiThread {
                                successCallback.invoke()
                            }
                        } else //还有灯要删除分组
                            deleteGroupRelay(lights, group, successCallback = successCallback, failedCallback = failedCallback)

                        LogUtils.e("zcl删除组后" + DBUtils.getGroupsByDeviceType(DeviceType.LIGHT_RGB))
                    }, failedCallback = {
                        deleteGroupRelay(lights, group, retryCount = retryCount + 1, successCallback = successCallback, failedCallback = failedCallback)
                    })
                } else //超过了重试次数
                    runOnUiThread {
                        failedCallback.invoke()
                    }
            } else {
                DBUtils.deleteGroupOnly(group)
                runOnUiThread {
                    successCallback.invoke()
                }
            }
        }
    }

    /**
     * 删除组，并且把组里的灯的组也都删除。
     */
    private fun deleteGroupCurtain(lights: MutableList<DbCurtain>, group: DbGroup, retryCount: Int = 0,
                                   successCallback: () -> Unit, failedCallback: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            if (lights.count() != 0) {
                val maxRetryCount = 3
                if (retryCount <= maxRetryCount) {
                    val light = lights[0]
                    val lightMeshAddr = light.meshAddr
                    Commander.deleteGroup(lightMeshAddr, successCallback = {
                        light.belongGroupId = DBUtils.groupNull!!.id//该等所在组
                        DBUtils.updateCurtain(light)
                        lights.remove(light)
                        //修改分组成功后删除场景信息。
                        // deleteAllSceneByLightAddr(light.meshAddr)
                        Thread.sleep(100)
                        if (lights.count() == 0) {
                            //所有灯都删除了分组
                            DBUtils.deleteGroupOnly(group)//亲删除改组
                            runOnUiThread {
                                successCallback.invoke()
                            }
                        } else //还有灯要删除分组
                            deleteGroupCurtain(lights, group, successCallback = successCallback, failedCallback = failedCallback)

                        LogUtils.e("zcl删除组后" + DBUtils.getGroupsByDeviceType(DeviceType.LIGHT_RGB))
                    }, failedCallback = {
                        deleteGroupCurtain(lights, group, retryCount = retryCount + 1, successCallback = successCallback, failedCallback = failedCallback)
                    })
                } else //超过了重试次数
                    runOnUiThread {
                        failedCallback.invoke()
                    }
            } else {
                DBUtils.deleteGroupOnly(group)
                runOnUiThread {
                    successCallback.invoke()
                }
            }
        }
    }

    private fun deleteAllSceneByLightAddr(lightMeshAddr: Int) {
        val opcode = Opcode.SCENE_ADD_OR_DEL
        val params: ByteArray = byteArrayOf(0x00, 0xff.toByte())
        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, lightMeshAddr, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
        mConnectDisposable?.dispose()
        localBroadcastManager.unregisterReceiver(br)
    }

    fun getGroupDeleteList(): MutableList<DbGroup> {
        val list = mutableListOf<DbGroup>()
        for (group in groupList) {
            LogUtils.e("zcl---单个-------------$group")
            if (group.isSelected)
                list.add(group)
            LogUtils.e("zcl要删除的组-----$list")
        }
        return list
    }

    override fun receviedGwCmd2500(gwStompBean: GwStompBean) {
        disposableTimer?.dispose()
        hideLoadingDialog()
        groupSwSuccess(currentPosition, true)

        when (gwStompBean.ser_id.toInt()) {
            Constant.SER_ID_GROUP_ON -> {
                LogUtils.v("zcl-----------远程控制群组开启成功-------")
            }
            Constant.SER_ID_GROUP_OFF -> {
                LogUtils.v("zcl-----------远程控制群组关闭成功-------")
            }
        }
    }

    override fun receviedGwCmd2500M(gwStompBean: MqttBodyBean) {
        groupSwSuccess(currentPosition, true)
        disposableTimer?.dispose()
        hideLoadingDialog()
        /*   when (gwStompBean.ser_id.toInt()) {
               Constant.SER_ID_GROUP_ON -> {
                   LogUtils.v("zcl-----------远程控制群组开启成功-------")

               }
               Constant.SER_ID_GROUP_OFF -> {
                   LogUtils.v("zcl-----------远程控制群组关闭成功-------")
                   disposableTimer?.dispose()
                   hideLoadingDialog()
                   groupCloseSuccess(currentPosition, true)
               }
           }*/
    }
}
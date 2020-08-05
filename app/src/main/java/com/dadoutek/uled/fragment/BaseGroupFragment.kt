package com.dadoutek.uled.fragment

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
import com.dadoutek.uled.gateway.bean.DbGateway
import com.dadoutek.uled.gateway.bean.GwStompBean
import com.dadoutek.uled.gateway.util.Base64Utils
import com.dadoutek.uled.light.LightsOfGroupActivity
import com.dadoutek.uled.light.NormalSettingActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.*
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.HttpModel.GwModel
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.GwGattBody
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.othersview.BaseFragment
import com.dadoutek.uled.othersview.InstructionsForUsActivity
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.rgb.RGBSettingActivity
import com.dadoutek.uled.stomp.MqttBodyBean
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.SharedPreferencesUtils
import com.dadoutek.uled.util.StringUtils
import com.telink.bluetooth.light.ConnectionStatus
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.group_list_fragment.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.support.v4.runOnUiThread
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

abstract class BaseGroupFragment : BaseFragment() {
    private var seehelp: TextView? = null
    private var seeHelp2: TextView? = null
    private var addGroupTv: TextView? = null
    private var mConnectDisposable: Disposable? = null
    private var currentPosition: Int = 0
    private var disposableTimer: Disposable? = null
    private var currentGroup: DbGroup? = null
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
       var wbType =  when(setGroupType()){
           Constant.DEVICE_TYPE_LIGHT_NORMAL->"#control-normal-group"
           Constant.DEVICE_TYPE_LIGHT_RGB->"#control-color-light-group"
           Constant.DEVICE_TYPE_CURTAIN->"#control-curtain-group"
           Constant.DEVICE_TYPE_CONNECTOR->"#control-relay-group"
           else -> "#control-normal-group"
       }
        var intent = Intent(mContext, InstructionsForUsActivity::class.java)
        intent.putExtra(Constant.WB_TYPE,wbType)
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
                Constant.DEVICE_TYPE_CONNECTOR -> group.deviceCount = DBUtils.getConnectorByGroupID(group.id).size  //查询改组内设备数量
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


    private fun sendToGw(isOpen: Boolean) {
        val gateWay = DBUtils.getAllGateWay()
        if (gateWay.size > 0)
            GwModel.getGwList()?.subscribe(object : NetworkObserver<List<DbGateway>?>() {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onNext(t: List<DbGateway>) {
                    TelinkLightApplication.getApp().offLine = true
                    hideLoadingDialog()
                    t.forEach { db ->
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
                        sendToServer(gattBody)
                    } else {
                        ToastUtils.showShort(getString(R.string.gw_not_online))
                    }
                }

                override fun onError(e: Throwable) {
                    super.onError(e)
                    hideLoadingDialog()
                    ToastUtils.showShort(getString(R.string.gw_not_online))
                }
            })
    }

    private fun sendToServer(gattBody: GwGattBody) {
        GwModel.sendDeviceToGatt(gattBody)?.subscribe(object : NetworkObserver<String?>() {
            override fun onNext(t: String) {
                disposableTimer?.dispose()
                LogUtils.v("zcl-----------远程控制-------$t")
            }

            override fun onError(e: Throwable) {
                super.onError(e)
                disposableTimer?.dispose()
                ToastUtils.showShort(e.message)
                LogUtils.v("zcl-----------远程控制-------${e.message}")
            }
        })
    }

    var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { _, view, position ->
        currentGroup = groupList[position]
        currentPosition = position
        val dstAddr = currentGroup!!.meshAddr
        val groupType = setGroupType()

        when (view!!.id) {
            R.id.template_device_icon -> {
                if (TelinkLightApplication.getApp().connectDevice == null) {
                    goConnect(false)
                    sendToGw(true)
                } else
                    if (currentGroup!!.deviceType == Constant.DEVICE_TYPE_LIGHT_RGB || currentGroup!!.deviceType == Constant.DEVICE_TYPE_LIGHT_NORMAL
                            || currentGroup!!.deviceType == Constant.DEVICE_TYPE_CONNECTOR || currentGroup!!.deviceType == Constant.DEVICE_TYPE_NO) {
                        if (currentGroup!!.status == 0) {
                            Commander.openOrCloseLights(dstAddr, true)
                            currentGroup?.connectionStatus = 1
                            groupOpenSuccess(position)
                        } else {
                            Commander.openOrCloseLights(dstAddr, false)
                            groupCloseSuccess(position)
                            currentGroup?.connectionStatus = 0
                        }
                        DBUtils.saveGroup(currentGroup!!, true)
                        LogUtils.v("zcl所有组------------------${DBUtils.allGroups[0].connectionStatus}")
                    } else {
                        if (currentGroup!!.status == 0) {
                            Commander.openOrCloseCurtain(dstAddr, true, false)
                            groupOpenSuccess(position)
                        } else {
                            Commander.openOrCloseCurtain(dstAddr, false, false)
                            groupCloseSuccess(position)
                        }
                    }
            }

            R.id.template_device_setting -> {
                val lastUser = DBUtils.lastUser
                lastUser?.let {
                    val isLight = groupType == Constant.DEVICE_TYPE_LIGHT_NORMAL || groupType == Constant.DEVICE_TYPE_LIGHT_RGB
                    when {
                        isLight && position == 0 -> {
                            if (TelinkLightApplication.getApp().connectDevice != null) {
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
                                Constant.DEVICE_TYPE_CONNECTOR -> num = DBUtils.getConnectorByGroupID(currentGroup!!.id).size
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
                                when (TelinkLightApplication.getApp().connectDevice) {
                                    null -> goConnect()
                                    else -> startActivityForResult(intent, 2)
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
            }
        }
    }

    private fun deleteSingleGroup(dbGroup: DbGroup) {
        AlertDialog.Builder(mContext)
                .setMessage(getString(R.string.delete_group_confirm, dbGroup?.name))
                .setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
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
                        } Constant.DEVICE_TYPE_CONNECTOR -> {
                            val lights = DBUtils.getConnectorByGroupID(dbGroup.id)
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

                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()
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
        val deviceTypes = mutableListOf(DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD, DeviceType.LIGHT_RGB)
        if (ishow)
            ToastUtils.showShort(getString(R.string.connecting))
        mConnectDisposable?.dispose()
        mConnectDisposable = connect(deviceTypes = deviceTypes, fastestMode = true, retryTimes = 10)
                ?.subscribe({}, { LogUtils.d("connect failed") })
    }

    private fun groupCloseSuccess(position: Int) {
        currentGroup?.connectionStatus = ConnectionStatus.OFF.value
        groupAdapter?.notifyItemChanged(position)
        GlobalScope.launch {
            currentGroup?.let {
                DBUtils.updateGroup(currentGroup!!)
                updateLights(true, currentGroup!!)
            }
        }
    }

    private fun groupOpenSuccess(position: Int) {
        currentGroup?.connectionStatus = ConnectionStatus.ON.value
        groupAdapter?.notifyItemChanged(position)
        GlobalScope.launch {
            currentGroup?.let {
                DBUtils.updateGroup(currentGroup!!)
                updateLights(true, currentGroup!!)
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
        updateLightDisposal = Observable.timer(300, TimeUnit.MILLISECONDS, Schedulers.io())
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
                    groupList?.add(it)
                }
                refreshData()
                renameDialog.dismiss()
            }
        }
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
                        // deleteAllSceneByLightAddr(light.meshAddr)
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
        when (gwStompBean.ser_id.toInt()) {
            Constant.SER_ID_GROUP_ON -> {
                LogUtils.v("zcl-----------远程控制群组开启成功-------")
                disposableTimer?.dispose()
                hideLoadingDialog()
                groupOpenSuccess(currentPosition)
            }
            Constant.SER_ID_GROUP_OFF -> {
                LogUtils.v("zcl-----------远程控制群组关闭成功-------")
                disposableTimer?.dispose()
                hideLoadingDialog()
                groupCloseSuccess(currentPosition)
            }
        }
    }

    override fun receviedGwCmd2500M(gwStompBean: MqttBodyBean) {
        when (gwStompBean.ser_id.toInt()) {
            Constant.SER_ID_GROUP_ON -> {
                LogUtils.v("zcl-----------远程控制群组开启成功-------")
                disposableTimer?.dispose()
                hideLoadingDialog()
                groupOpenSuccess(currentPosition)
            }
            Constant.SER_ID_GROUP_OFF -> {
                LogUtils.v("zcl-----------远程控制群组关闭成功-------")
                disposableTimer?.dispose()
                hideLoadingDialog()
                groupCloseSuccess(currentPosition)
            }
        }
    }
}
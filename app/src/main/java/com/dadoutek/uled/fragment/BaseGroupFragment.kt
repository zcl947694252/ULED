package com.dadoutek.uled.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.connector.ConnectorOfGroupActivity
import com.dadoutek.uled.connector.ConnectorSettingActivity
import com.dadoutek.uled.curtain.CurtainOfGroupActivity
import com.dadoutek.uled.curtains.WindowCurtainsActivity
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.light.LightsOfGroupActivity
import com.dadoutek.uled.light.NormalSettingActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.othersview.BaseFragment
import com.dadoutek.uled.rgb.RGBSettingActivity
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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.support.v4.runOnUiThread
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

abstract class BaseGroupFragment : BaseFragment() {
    private var lin: View? = null
    private var inflater: LayoutInflater? = null
    private var recyclerView: RecyclerView? = null
    private var no_group: ConstraintLayout? = null
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.mContext = this.activity
        setHasOptionsMenu(true)
        localBroadcastManager = LocalBroadcastManager
                .getInstance(this!!.mContext!!)
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
                val isDeleteRecevice = intent.getStringExtra("isDelete")//必定将删除模式恢复掉
                if (key == "true" || delete == "true" || isDeleteRecevice == "true") {
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
                    for (i in groupList.indices) {
                        if (groupList[i].isSelected)
                            deleteList.add(groupList[i])
                    }
                    isDeleteSucess = false
                    for (j in deleteList.indices) {
                        showLoadingDialog(getString(R.string.deleting))
                        Thread.sleep(300)
                        deleteGroup(DBUtils.getLightByGroupID(deleteList[j].id), deleteList[j],
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
                                    ToastUtils.showShort(R.string.move_out_some_lights_in_group_failed)
                                })
                    }
                    Log.e("TAG_DELETE", deleteList.size.toString())
                }

                if (lightStatus == "on") {
                    for (i in groupList.indices) {
                        groupList[i].connectionStatus = ConnectionStatus.ON.value
                        DBUtils.updateGroup(groupList[i])
                        groupAdapter!!.notifyDataSetChanged()
                    }
                } else if (lightStatus == "false") {
                    for (i in groupList.indices) {
                        groupList[i].connectionStatus = ConnectionStatus.OFF.value
                        DBUtils.updateGroup(groupList[i])
                        groupAdapter!!.notifyDataSetChanged()
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

    private fun getView(inflater: LayoutInflater): View {
        this.inflater = inflater
        val view = inflater.inflate(R.layout.group_list_fragment, null)
        groupMesher = ArrayList()
        no_group = view.findViewById(R.id.no_group)
        recyclerView = view.findViewById(R.id.group_recyclerView)
        addNewGroup = view.findViewById(R.id.add_device_btn)
        viewLine = view.findViewById(R.id.view)
        viewLineRecycler = view.findViewById(R.id.viewLine)
        lin = LayoutInflater.from(activity).inflate(R.layout.add_group, null)
        return view
    }

    override fun onResume() {
        super.onResume()
        isFristUserClickCheckConnect = true
        refreshData()
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        if (isVisibleToUser) {
            refreshData()
        }
    }

    private fun initData() {
        groupList = ArrayList()
        groupList.addAll(getGroupData())

        if (groupList.size > 0) {
            no_group?.visibility = View.GONE
            recyclerView?.visibility = View.VISIBLE
            addGroupBtn?.visibility = View.VISIBLE
            viewLine?.visibility = View.VISIBLE
            viewLineRecycler?.visibility = View.VISIBLE
        } else {
            no_group?.visibility = View.VISIBLE
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
        lin?.setOnClickListener(onClickAddGroup)

        val layoutmanager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        recyclerView!!.layoutManager = layoutmanager

        Collections.sort(groupList, kotlin.Comparator { o1, o2 ->
            return@Comparator o1.name.compareTo(o2.name)
        })

        //LogUtils.e("zcl删除组前$groupList")
        this.groupAdapter = GroupListAdapter(R.layout.group_item_child, groupList, isDelete)
        val decoration = DividerItemDecoration(activity, DividerItemDecoration.VERTICAL)
        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(activity!!, R.color.divider)))
        //添加分割线
        recyclerView?.addItemDecoration(decoration)

        groupAdapter!!.addFooterView(lin)

        groupAdapter!!.onItemChildClickListener = onItemChildClickListener
        val lastUser = DBUtils.lastUser
        //如果是自己的区域才允许长按删除
        groupAdapter!!.onItemLongClickListener = onItemChildLongClickListener

        groupAdapter!!.bindToRecyclerView(recyclerView)
    }

    private var onItemChildLongClickListener = BaseQuickAdapter.OnItemLongClickListener { _, _, postion ->
        val lastUser = DBUtils.lastUser
        lastUser?.let {
            if (it.id.toString() != it.last_authorizer_user_id) {
                ToastUtils.showShort(getString(R.string.author_region_warm))
            } else {
                if (!isDelete) {
                    isDelete = true
                    SharedPreferencesUtils.setDelete(true)
                    val intent = Intent("showPro")
                    intent.putExtra("is_delete", "true")
                    this.activity?.let {
                        LocalBroadcastManager.getInstance(it).sendBroadcast(intent)
                    }
                } else {//先长按  选中 在长按 就会通知外面关闭了
                    isDelete = false
                    val intent = Intent("showPro")
                    intent.putExtra("is_delete", "false")
                    this.activity?.let {
                        LocalBroadcastManager.getInstance(it).sendBroadcast(intent)
                    }
                }
                SharedPreferencesHelper.putBoolean(TelinkLightApplication.getApp(), Constant.IS_DELETE, isDelete)
                groupAdapter?.changeState(isDelete)
                groupList[postion].isSelected = isDelete
                refreshData()
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
                Constant.DEVICE_TYPE_LIGHT_NORMAL -> {
                    group.deviceCount = DBUtils.getLightByGroupID(group.id).size  //查询改组内设备数量  普通灯和冷暖灯是一个方法  查询什么设备类型有grouplist内容决定
                }
                Constant.DEVICE_TYPE_LIGHT_RGB -> {
                    group.deviceCount = DBUtils.getLightByGroupID(group.id).size  //查询改组内设备数量
                }
                Constant.DEVICE_TYPE_CONNECTOR -> {
                    group.deviceCount = DBUtils.getConnectorByGroupID(group.id).size  //查询改组内设备数量
                }
                Constant.DEVICE_TYPE_CURTAIN -> {
                    group.deviceCount = DBUtils.getCurtainByGroupID(group.id).size  //查询改组内设备数量//窗帘和传感器是一个方法
                }
            }
        }

        if (groupList.size > 0) {
            no_group?.visibility = View.GONE
            recyclerView?.visibility = View.VISIBLE
        } else {
            no_group?.visibility = View.VISIBLE
            recyclerView?.visibility = View.GONE
        }

        groupAdapter?.notifyDataSetChanged()
    }

    abstract fun getGroupData(): Collection<DbGroup>

    var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { _, view, position ->
        var currentLight = groupList[position]
        val dstAddr = currentLight.meshAddr
        val groupType = setGroupType()
        when (view!!.id) {
            R.id.btn_on, R.id.tv_on -> {
                if (currentLight.deviceType != Constant.DEVICE_TYPE_DEFAULT_ALL) {
                    Commander.openOrCloseLights(dstAddr, true)
                    currentLight.connectionStatus = ConnectionStatus.ON.value
                    groupAdapter!!.notifyItemChanged(position)
                    GlobalScope.launch {
                        DBUtils.updateGroup(currentLight)
                        updateLights(true, currentLight)
                    }
                }
            }
            R.id.btn_off, R.id.tv_off -> {
                if (currentLight.deviceType != Constant.DEVICE_TYPE_DEFAULT_ALL) {
                    Commander.openOrCloseLights(dstAddr, false)
                    currentLight.connectionStatus = ConnectionStatus.OFF.value
                    groupAdapter!!.notifyItemChanged(position)
                    GlobalScope.launch {
                        DBUtils.updateGroup(currentLight)
                        updateLights(true, currentLight)
                    }
                }
            }

            R.id.btn_set, R.id.curtain_setting -> {
                val lastUser = DBUtils.lastUser
                lastUser?.let {
                    if (it.id.toString() != it.last_authorizer_user_id)
                        ToastUtils.showShort(getString(R.string.author_region_warm))
                    else {
                        if (currentLight.deviceType != Constant.DEVICE_TYPE_DEFAULT_ALL && (currentLight.deviceType == groupType)) {
                            var num = 0
                            when (groupType) {
                                Constant.DEVICE_TYPE_LIGHT_NORMAL -> {
                                    num = DBUtils.getLightByGroupID(currentLight.id).size
                                }
                                Constant.DEVICE_TYPE_LIGHT_RGB -> {
                                    num = DBUtils.getLightByGroupID(currentLight.id).size
                                }
                                Constant.DEVICE_TYPE_CONNECTOR -> {
                                    num = DBUtils.getConnectorByGroupID(currentLight.id).size
                                }//蓝牙接收器
                                Constant.DEVICE_TYPE_CURTAIN -> {
                                    num = DBUtils.getCurtainByGroupID(currentLight.id).size
                                }
                            }

                            if (num != 0) {
                                var intent: Intent? = null

                                when (groupType) {
                                    Constant.DEVICE_TYPE_LIGHT_NORMAL -> {
                                        intent = Intent(mContext, NormalSettingActivity::class.java)
                                    }
                                    Constant.DEVICE_TYPE_LIGHT_RGB -> {
                                        intent = Intent(mContext, RGBSettingActivity::class.java)
                                    }
                                    Constant.DEVICE_TYPE_CONNECTOR -> {
                                        intent = Intent(mContext, ConnectorSettingActivity::class.java)
                                    }//蓝牙接收器
                                    Constant.DEVICE_TYPE_CURTAIN -> {
                                        intent = Intent(mContext, WindowCurtainsActivity::class.java)
                                    }
                                }
                                intent?.putExtra(Constant.TYPE_VIEW, Constant.TYPE_GROUP)
                                intent?.putExtra("group", currentLight)
                                startActivityForResult(intent, 2)
                            }
                        }
                    }
                }
            }

            R.id.selected_group -> {
                groupList[position].isSelected = !groupList[position].isSelected
            }

            //不能使用group_name否则会造成长按监听无效
            R.id.item_layout -> {
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
                intent.putExtra("group", currentLight)
                startActivityForResult(intent, 2)
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

                    if (group.meshAddr == 0xffff) {//如果是所有组 那么所有灯就都更新
                        val list = DBUtils.groupList
                        for (j in list.indices) {
                            lightList.addAll(DBUtils.getLightByGroupID(list[j].id))
                        }
                    } else {
                        lightList = DBUtils.getLightByGroupID(group.id)
                    }

                    for (dbLight: DbLight in lightList) {
                        if (isOpen) {
                            dbLight.connectionStatus = ConnectionStatus.ON.value
                        } else {
                            dbLight.connectionStatus = ConnectionStatus.OFF.value
                        }
                        DBUtils.updateLightLocal(dbLight)//更新灯的状态
                    }
                }
    }

    private val onClickAddGroup = View.OnClickListener {
        val lastUser = DBUtils.lastUser
        lastUser?.let {
            if (it.id.toString() != it.last_authorizer_user_id)
                ToastUtils.showShort(getString(R.string.author_region_warm))
            else {
                if (TelinkLightApplication.getApp().connectDevice == null) {
                    ToastUtils.showLong(activity!!.getString(R.string.device_not_connected))
                } else {
                    addNewGroup()
                }
            }
        }
    }

    private fun addNewGroup() {
        val textGp = EditText(activity)
        StringUtils.initEditTextFilter(textGp)
        textGp.setText(DBUtils.getDefaultNewGroupName())
        //设置光标默认在最后
        textGp.setSelection(textGp.text.toString().length)
        AlertDialog.Builder(activity)
                .setTitle(R.string.create_new_group)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(textGp)

                .setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
                    // 获取输入框的内容
                    if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                        ToastUtils.showShort(getString(R.string.rename_tip_check))
                    } else {
                        //往DB里添加组数据
                        val dbGroup = DBUtils.addNewGroupWithType(textGp.text.toString().trim { it <= ' ' }, setGroupType())

                        dbGroup?.let {
                            groupList?.add(it)
                        }
                        refreshData()
                        dialog.dismiss()
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()
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
                    Commander.deleteGroup(lightMeshAddr,
                            successCallback = {
                                light.belongGroupId = DBUtils.groupNull!!.id//该等所在组
                                DBUtils.updateLight(light)
                                lights.remove(light)

                                //修改分组成功后删除场景信息。
                                // deleteAllSceneByLightAddr(light.meshAddr)
                                Thread.sleep(100)
                                if (lights.count() == 0) {
                                    //所有灯都删除了分组
                                    DBUtils.deleteGroupOnly(group)//亲删除改组
                                    this.runOnUiThread {
                                        successCallback.invoke()
                                    }
                                } else {
                                    //还有灯要删除分组
                                    deleteGroup(lights, group,
                                            successCallback = successCallback,
                                            failedCallback = failedCallback)
                                }

                                LogUtils.e("zcl删除组后" + DBUtils.getGroupsByDeviceType(DeviceType.LIGHT_RGB))
                            },
                            failedCallback = {
                                deleteGroup(lights, group, retryCount = retryCount + 1,
                                        successCallback = successCallback,
                                        failedCallback = failedCallback)
                            })
                } else {    //超过了重试次数
                    this.runOnUiThread {
                        failedCallback.invoke()
                    }
                }
            } else {
                DBUtils.deleteGroupOnly(group)
                this.runOnUiThread {
                    successCallback.invoke()
                }
            }
        }.start()

    }

    private fun deleteAllSceneByLightAddr(lightMeshAddr: Int) {
        val opcode = Opcode.SCENE_ADD_OR_DEL
        val params: ByteArray = byteArrayOf(0x00, 0xff.toByte())
        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, lightMeshAddr, params)
    }

    override fun onDestroy() {
        super.onDestroy()
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


    private fun syncData() {
        mContext?.let {
            SyncDataPutOrGetUtils.syncPutDataStart(it, object : SyncCallback {
                override fun complete() {
                    hideLoadingDialog()
                    val disposable = Observable.timer(500, TimeUnit.MILLISECONDS)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe { }
                    if (compositeDisposable.isDisposed) {
                        compositeDisposable = CompositeDisposable()
                    }
                    compositeDisposable.add(disposable)
                }

                override fun error(msg: String) {
                    hideLoadingDialog()
                    ToastUtils.showShort(R.string.backup_failed)
                }

                override fun start() {}
            })
        }
    }
}
package com.dadoutek.uled.fragment

import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.router.bean.CmdBodyBean

class CurtainFragmentList : BaseGroupFragment() {

    override fun setIntentDeviceType(): String? {
        return "curtain_light"
    }
    override fun setGroupType(): Long {
        return Constant.DEVICE_TYPE_CURTAIN
    }

    override fun getGroupData(): Collection<DbGroup> {
        val list = mutableListOf<DbGroup>()
        list.addAll( DBUtils.getGroupsByDeviceType(DeviceType.SMART_CURTAIN))
        return list
    }

    override fun tzRouteContorlCurtaine(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl-----------收到路由控制通知-------$cmdBean")
        if (cmdBean.ser_id == "groupSwCurtain") {
            disposableRouteTimer?.dispose()
            hideLoadingDialog()
            currentGroup?.status =    if (currentGroup?.status == 1) 2 else 1
            if (cmdBean.status == 0)
                groupSwSuccess(currentPosition, false)//因为窗帘不是connectstatus 使用外面处理
            currentGroup?.let {
                DBUtils.saveGroup(it, true)
            }
        }
    }
 /*   private var inflater: LayoutInflater? = null



    private var recyclerView: RecyclerView? = null

    private var no_group: ConstraintLayout? = null

    private var groupAdapter: CurtainGroupListAdapter? = null

    private lateinit var groupList: ArrayList<DbGroup>

    private var isFristUserClickCheckConnect = true

    private var updateLightDisposal: Disposable? = null

    private var mContext: Activity? = null

    private var addGroupBtn: ConstraintLayout? = null

    private var viewLine: View? = null

    private var viewLineRecycler: View? = null

    private var isDelete = false

    private var groupListGroup: GroupListFragment? = null

    private var groupMesher: ArrayList<String>? = null

    private lateinit var deleteList: ArrayList<DbGroup>

    private lateinit var localBroadcastManager: LocalBroadcastManager

    private lateinit var br: BroadcastReceiver

    private var addNewGroup: Button? = null

    private var isDeleteTrue: Boolean = true

    private var isLong: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.mContext = this.activity
        setHasOptionsMenu(true)
        localBroadcastManager = LocalBroadcastManager.getInstance(this.mContext!!)
        val intentFilter = IntentFilter()
        intentFilter.addAction("back")
        intentFilter.addAction("delete")
        intentFilter.addAction("switch")
        br = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val key = intent.getStringExtra("back")
                val str = intent.getStringExtra("delete")
                val switch = intent.getStringExtra("switch")
                if (key == "true") {
                    isDelete = false
                    isLong = true
                    groupAdapter!!.changeState(isDelete)
                    for (i in groupList.indices) {
                        if (groupList[i].isSelected) {
                            groupList[i].isSelected = false
                        }
                    }
                    refreshData()
                }
                if (str == "true") {
                    deleteList = ArrayList()
                    for (i in groupList.indices) {
                        if (groupList[i].isSelected) {
                            deleteList.add(groupList[i])
                        }
                    }

                    for (j in deleteList.indices) {
                        showLoadingDialog(getString(R.string.deleting))
                        Thread.sleep(300)
                        deleteGroup(DBUtils.getCurtainByGroupID(deleteList[j].id), deleteList[j]!!,
                                successCallback = {
                                    hideLoadingDialog()
                                    setResult(Constant.RESULT_OK)
                                },
                                failedCallback = {
                                    hideLoadingDialog()
                                    ToastUtils.showLong(R.string.move_out_some_lights_in_group_failed)
                                    val intent = Intent("delete_true")
                                    intent.putExtra("delete_true", "true")
                                    LocalBroadcastManager.getInstance(context)
                                            .sendBroadcast(intent)
                                })
                    }
                    Log.e("TAG_DELETE", deleteList.size.toString())
                }

                if (switch == "true") {
                    for (i in groupList.indices) {
                        if (groupList[i].isSelected) {
                            groupList[i].isSelected = false
                        }
                    }
                }
            }
        }
        localBroadcastManager.registerReceiver(br, intentFilter)

    }

    private fun setResult(resulT_OK: Int) {
        Thread.sleep(300)
        val intent = Intent("delete_true")
        intent.putExtra("delete_true", "true")
        LocalBroadcastManager.getInstance(this!!.mContext!!)
                .sendBroadcast(intent)
        isDeleteTrue = false
        refreshView()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        isDeleteTrue = true
        isLong = true
        val view = getView(inflater)
        this.initData()
        return view
    }

    private fun getView(inflater: LayoutInflater): View {
        this.inflater = inflater
        groupMesher = ArrayList()
        val view = inflater.inflate(R.layout.group_list_fragment, null)
        no_group = view.findViewById(R.id.no_group)
        recyclerView = view.findViewById(R.id.group_recyclerView)
//        addGroupBtn = view.findViewById(R.id.add_group_btn)
        addNewGroup = view.findViewById(R.id.add_device_btn)
        viewLine = view.findViewById(R.id.view)
        viewLineRecycler = view.findViewById(R.id.viewLine)
        return view
    }

    override fun onResume() {
        super.onResume()
        isFristUserClickCheckConnect = true
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        if (isVisibleToUser) {
            isDeleteTrue = true
            isLong = true
            refreshView()
        }
    }

    private fun initData() {
        groupList = ArrayList()

        val listAll = DBUtils.getAllGroupsOrderByIndex()
        for (group in listAll) {
            when (group.deviceType) {
                Constant.DEVICE_TYPE_CURTAIN -> {
                    groupList.add(group)
                }
                Constant.DEVICE_TYPE_DEFAULT_ALL -> {
                    groupList.add(group)
                }
            }
        }

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

        val intent = Intent("switch_fragment")
        intent.putExtra("switch_fragment", "true")
        LocalBroadcastManager.getInstance(this.mContext!!).sendBroadcast(intent)

        addGroupBtn?.setOnClickListener(onClick)
        addNewGroup?.setOnClickListener(onClick)

        val layoutmanager = LinearLayoutManager(activity)
        layoutmanager.orientation = LinearLayoutManager.VERTICAL
        recyclerView!!.layoutManager = layoutmanager

        Collections.sort(groupList, kotlin.Comparator { o1, o2 ->
            return@Comparator o1.name.compareTo(o2.name)
        })

        this.groupAdapter = CurtainGroupListAdapter(R.layout.curtain_list_adapter, groupList, isDelete)
        val decoration = DividerItemDecoration(activity,
                DividerItemDecoration
                        .VERTICAL)
        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(activity!!, R.color
                .divider)))
        //添加分割线
        recyclerView?.addItemDecoration(decoration)
        var lin = LayoutInflater.from(activity).inflate(R.layout.add_group, null)
        lin.setOnClickListener {
            if (TelinkLightApplication.getApp().connectDevice == null) {
                ToastUtils.showLong(activity!!.getString(R.string.device_not_connected))
            } else {
                addNewGroup()
            }
        }
        groupAdapter!!.addFooterView(lin)
        groupAdapter!!.onItemChildClickListener = onItemChildClickListener
        groupAdapter!!.onItemLongClickListener = onItemChildLongClickListener
        groupAdapter!!.bindToRecyclerView(recyclerView)


    }

    var onItemChildLongClickListener = BaseQuickAdapter.OnItemLongClickListener { adapter, view, position ->
        if (!isDelete) {
            isDelete = true
            isLong = false
            SharedPreferencesUtils.setDelete(true)
            val intent = Intent("showPro")
            intent.putExtra("is_delete", "true")
            this!!.activity?.let {
                LocalBroadcastManager.getInstance(it)
                        .sendBroadcast(intent)
            }
        }
        groupAdapter!!.changeState(isDelete)
        refreshData()
        return@OnItemLongClickListener true
    }

    fun refreshData() {
        groupAdapter?.notifyDataSetChanged()
    }

    var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        var currentLight = groupList[position]
        var intent: Intent
        when (view!!.id) {

            R.id.btn_set -> {
                if(isLong) {
                    if (currentLight.deviceType != Constant.DEVICE_TYPE_DEFAULT_ALL && (currentLight.deviceType == Constant.DEVICE_TYPE_CURTAIN && DBUtils.getCurtainByGroupID(currentLight.id).size != 0)) {
                        intent = Intent(mContext, WindowCurtainsActivity::class.java)
                        intent.putExtra(Constant.TYPE_VIEW, Constant.TYPE_GROUP)
                        intent.putExtra("group", currentLight)
                        startActivityForResult(intent, 2)
                    }
                }
            }


            R.id.selected_group_curtain -> {
                currentLight.isSelected = !currentLight.isSelected
            }

            R.id.item_layout -> {
                if (isLong) {
                    intent = Intent(mContext, CurtainOfGroupActivity::class.java)
                    intent.putExtra("group", currentLight)
                    startActivityForResult(intent, 2)
                }
            }
        }
    }


    override fun onStop() {
        super.onStop()
        isFristUserClickCheckConnect = false
    }


    private val onClick = View.OnClickListener {
        //点击任何一个选项跳转页面都隐藏引导
//        val controller=guide2()
//            controller?.remove()
        when (it.id) {
//            R.id.add_group_btn -> {
//                if (TelinkLightApplication.getApp().connectDevice == null) {
//                    ToastUtils.showLong(activity!!.getString(R.string.device_not_connected))
//                } else {
//                    addNewGroup()
//                }
//            }
            R.id.add_device_btn -> {
                addNewGroup()
            }
        }
    }

    private fun addNewGroup() {
        val textGp = EditText(activity)
        StringUtils.initEditTextFilter(textGp)
        textGp.setText(DBUtils.getDefaultNewGroupName())
        //设置光标默认在最后
        textGp.setSelection(textGp.getText().toString().length)
        AlertDialog.Builder(activity)
                .setTitle(R.string.create_new_group)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(textGp)

                .setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
                    // 获取输入框的内容
                    if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                        ToastUtils.showLong(getString(R.string.rename_tip_check))
                    } else {
                        //往DB里添加组数据
                        val dbGroup = DBUtils.addNewGroupWithType(textGp.text.toString().trim { it <= ' ' }, Constant.DEVICE_TYPE_CURTAIN)
                        dbGroup?.let {
                            groupList.add(it)
                        }
                        isLong = true
                        dialog.dismiss()

                        groupAdapter?.notifyDataSetChanged()
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()
    }


    private fun refreshView() {
        if (activity != null) {
            groupList = ArrayList()

            val listAll = DBUtils.getAllGroupsOrderByIndex()
            for (group in listAll) {
                when (group.deviceType) {
                    Constant.DEVICE_TYPE_CURTAIN -> {
                        groupList.add(group)
                    }
                    Constant.DEVICE_TYPE_DEFAULT_ALL -> {
                        groupList.add(group)
                    }
                }
            }

            if (groupList.size > 0) {
                no_group?.visibility = View.GONE
                recyclerView?.visibility = View.VISIBLE
                addGroupBtn?.visibility = View.VISIBLE
                viewLine?.visibility = View.VISIBLE
            } else {
                no_group?.visibility = View.VISIBLE
                recyclerView?.visibility = View.GONE
                addGroupBtn?.visibility = View.GONE
                viewLine?.visibility = View.GONE
            }


            if(isDeleteTrue){
                isDelete = false
                SharedPreferencesUtils.setDelete(false)
                val intent = Intent("switch_fragment")
                intent.putExtra("switch_fragment", "true")
                LocalBroadcastManager.getInstance(this!!.mContext!!)
                        .sendBroadcast(intent)
            }

            for (i in groupList.indices) {
                if (groupList[i].isSelected) {
                    groupList[i].isSelected = false
                }
            }
            refreshData()

            addGroupBtn?.setOnClickListener(onClick)
            addNewGroup?.setOnClickListener(onClick)

            val layoutmanager = LinearLayoutManager(activity)
            layoutmanager.orientation = LinearLayoutManager.VERTICAL
            recyclerView!!.layoutManager = layoutmanager

            Collections.sort(groupList, kotlin.Comparator { o1, o2 ->
                return@Comparator o1.name.compareTo(o2.name)
            })
            this.groupAdapter = CurtainGroupListAdapter(R.layout.curtain_list_adapter, groupList, isDelete)
            val decoration = DividerItemDecoration(activity,
                    DividerItemDecoration
                            .VERTICAL)
            decoration.setDrawable(ColorDrawable(ContextCompat.getColor(activity!!, R.color
                    .divider)))
            //添加分割线
            recyclerView?.addItemDecoration(decoration)
            var lin = LayoutInflater.from(activity).inflate(R.layout.add_group, null)
            lin.setOnClickListener(View.OnClickListener {
                if (TelinkLightApplication.getApp().connectDevice == null) {
                    ToastUtils.showLong(activity!!.getString(R.string.device_not_connected))
                } else {
                    addNewGroup()
                }
            })
            groupAdapter!!.addFooterView(lin)
            groupAdapter!!.onItemChildClickListener = onItemChildClickListener
            groupAdapter!!.onItemLongClickListener = onItemChildLongClickListener
            groupAdapter!!.bindToRecyclerView(recyclerView)
        }
    }

    *//**
     * 删除组，并且把组里的灯的组也都删除。
     *//*
    private fun deleteGroup(lights: MutableList<DbCurtain>, group: DbGroup, retryCount: Int = 0,
                            successCallback: () -> Unit, failedCallback: () -> Unit) {
        Thread {
            if (lights.count() != 0) {
                val maxRetryCount = 3
                if (retryCount <= maxRetryCount) {
                    val light = lights[0]
                    val lightMeshAddr = light.meshAddr
                    Commander.deleteGroup(lightMeshAddr,
                            successCallback = {
                                light.belongGroupId = DBUtils.groupNull!!.id
                                DBUtils.updateCurtain(light)
                                lights.remove(light)
                                //修改分组成功后删除场景信息。
                                deleteAllSceneByLightAddr(light.meshAddr)
                                Thread.sleep(100)
                                if (lights.count() == 0) {
                                    //所有灯都删除了分组
                                    DBUtils.deleteGroupOnly(group)
                                    this.runOnUiThread {
                                        successCallback.invoke()
                                    }
                                } else {
                                    //还有灯要删除分组
                                    deleteGroup(lights, group,
                                            successCallback = successCallback,
                                            failedCallback = failedCallback)
                                }
                            },
                            failedCallback = {
                                deleteGroup(lights, group, retryCount = retryCount + 1,
                                        successCallback = successCallback,
                                        failedCallback = failedCallback)
                            })
                } else {    //超过了重试次数
                    this?.runOnUiThread {
                        failedCallback.invoke()
                    }
                   //("retry delete group timeout")
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
    }*/
}
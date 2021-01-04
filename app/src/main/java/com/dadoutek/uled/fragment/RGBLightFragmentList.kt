package com.dadoutek.uled.fragment

import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.model.DeviceType

class RGBLightFragmentList : BaseGroupFragment() {
    override fun setIntentDeviceType(): String? {
        return "rgb_light"
    }

    override fun setGroupType(): Long {
        return Constant.DEVICE_TYPE_LIGHT_RGB
    }

    override fun getGroupData(): Collection<DbGroup> {
        val list = mutableListOf<DbGroup>()
        val allGroups = DBUtils.allGroups
        if (allGroups.size > 0) {
            list.add(0, allGroups[0])
        }
        list.addAll(DBUtils.getGroupsByDeviceType(DeviceType.LIGHT_RGB))
        return list
    }

    /* private var lin: View? = null
     private var inflater: LayoutInflater? = null

     private var recyclerView: RecyclerView? = null

     private var no_group: ConstraintLayout? = null

     private var groupAdapter: GroupListAdapter? = null

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

     private lateinit var localBroadcastManager: LocalBroadcastManager

     private lateinit var br: BroadcastReceiver

     private lateinit var deleteList: ArrayList<DbGroup>

     private var addNewGroup: Button? = null

     private var isDeleteTrue: Boolean = true


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
         br = object : BroadcastReceiver() {

             override fun onReceive(context: Context, intent: Intent) {
                 val key = intent.getStringExtra("back")
                 val str = intent.getStringExtra("delete")
                 val switch = intent.getStringExtra("switch")
                 val lightStatus = intent.getStringExtra("switch_here")
                 if (key == "true") {
                     isDelete = false
                     groupAdapter!!.changeState(isDelete)
                     groupList.let {
                         for (i in it.indices)
                             if (it[i].isSelected)
                                 it[i].isSelected = false
                     }
                     refreshData()
                 }
                 if (str == "true") {
                     deleteList = ArrayList()
                     for (i in groupList.indices) {
                         if (groupList[i].isSelected)
                             deleteList.add(groupList[i])
                     }

                     for (j in deleteList.indices) {
                         showLoadingDialog(getString(R.string.deleting))
                         Thread.sleep(300)
                         deleteGroup(DBUtils.getLightByGroupID(deleteList[j].id), deleteList[j]!!,
                                 successCallback = {
                                     hideLoadingDialog()
                                     isDelete = false
                                     sendDeleteBrocastRecevicer(300)
                                     refreshData()
                                 },
                                 failedCallback = {
                                     hideLoadingDialog()
                                     ToastUtils.showLong(R.string.move_out_some_lights_in_group_failed)
                                     sendDeleteBrocastRecevicer(0)
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
         LocalBroadcastManager.getInstance(this!!.mContext!!).sendBroadcast(intent)
         *//* isDeleteTrue = false
         refreshView()*//*
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // isDeleteTrue = true
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
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        if (isVisibleToUser) {
//            isDeleteTrue = true
            refreshData()
        }
    }

    private fun initData() {
        groupList = ArrayList()
        groupList.addAll(DBUtils.getGroupsByDeviceType(DeviceType.LIGHT_RGB))

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
        LocalBroadcastManager.getInstance(this!!.mContext!!).sendBroadcast(intent)

        addGroupBtn?.setOnClickListener(onClickAddGroup)
        addNewGroup?.setOnClickListener(onClickAddGroup)
        lin?.setOnClickListener(onClickAddGroup)

        val layoutmanager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        recyclerView!!.layoutManager = layoutmanager

        Collections.sort(groupList, kotlin.Comparator { o1, o2 ->
            return@Comparator o1.name.compareTo(o2.name)
        })

        this.groupAdapter = GroupListAdapter(R.layout.group_item_child, groupList, isDelete)
        val decoration = DividerItemDecoration(activity, DividerItemDecoration.VERTICAL)
        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(activity!!, R.color.divider)))
        //添加分割线
        recyclerView?.addItemDecoration(decoration)

        groupAdapter!!.addFooterView(lin)

        groupAdapter!!.onItemChildClickListener = onItemChildClickListener
        groupAdapter!!.onItemLongClickListener = onItemChildLongClickListener
        groupAdapter!!.bindToRecyclerView(recyclerView)
    }

    var onItemChildLongClickListener = BaseQuickAdapter.OnItemLongClickListener { adapter, view, position ->
        if (!isDelete) {
            isDelete = true
            SharedPreferencesUtils.setDelete(true)
            val intent = Intent("showPro")
            intent.putExtra("is_delete", "true")
            this!!.activity?.let {
                LocalBroadcastManager.getInstance(it)
                        .sendBroadcast(intent)
            }
        } else {//先长按  选中 在长按 就会通知外面关闭了
            isDelete = false
            val intent = Intent("showPro")
            intent.putExtra("is_delete", "false")
            this!!.activity?.let {
                LocalBroadcastManager.getInstance(it).sendBroadcast(intent)
            }
        }
        groupAdapter?.changeState(isDelete)
        refreshData()
        return@OnItemLongClickListener true
    }

    fun refreshData() {
        groupList.clear()
        groupList.addAll(DBUtils.getGroupsByDeviceType(DeviceType.LIGHT_RGB))//根据设备类型获取设备组数

        //以下是检索组里有多少设备的代码
        for (group in groupList) {
            when (group.deviceType) {
                Constant.DEVICE_TYPE_LIGHT_NORMAL -> group.deviceCount = DBUtils.getLightByGroupID(group.id).size  //查询改组内设备数量  普通灯和冷暖灯是一个方法  查询什么设备类型有grouplist内容决定
                Constant.DEVICE_TYPE_LIGHT_RGB -> group.deviceCount = DBUtils.getLightByGroupID(group.id).size  //查询改组内设备数量
                Constant.DEVICE_TYPE_CONNECTOR -> group.deviceCount = DBUtils.getConnectorByGroupID(group.id).size  //查询改组内设备数量//窗帘和传感器是一个方法
                Constant.DEVICE_TYPE_CURTAIN -> group.deviceCount = DBUtils.getConnectorByGroupID(group.id).size  //查询改组内设备数量
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

    var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        var currentLight = groupList[position]
        val dstAddr = currentLight.meshAddr
        var intent: Intent

        when (view!!.id) {
            R.id.btn_on -> {
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
            R.id.btn_off -> {
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

            R.id.btn_set -> {
                if (currentLight.deviceType != Constant.DEVICE_TYPE_DEFAULT_ALL) {
                    if (currentLight.deviceType != Constant.DEVICE_TYPE_DEFAULT_ALL && (currentLight.deviceType == Constant.DEVICE_TYPE_LIGHT_RGB && DBUtils.getLightByGroupID(currentLight.id).size != 0)) {
                        intent = Intent(mContext, RGBSettingActivity::class.java)
                        intent.putExtra(Constant.TYPE_VIEW, Constant.TYPE_GROUP)
                        intent.putExtra("group", currentLight)
                        startActivityForResult(intent, 2)
                    }
                }
            }

            R.id.selected_group -> {
                currentLight.isChecked = !currentLight.isChecked
            }

            R.id.item_layout -> {
                intent = Intent(mContext, LightsOfGroupActivity::class.java)
                intent.putExtra("group", currentLight)
                intent.putExtra("light", "rgb_light")
                startActivityForResult(intent, 2)
            }
        }

    }


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
                        val listTask = DBUtils.groupList
                        for (j in listTask.indices) {
                            lightList.addAll(DBUtils.getLightByGroupID(listTask[j].id))
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
        if (TelinkLightApplication.getApp().connectDevice == null) {
            ToastUtils.showLong(activity!!.getString(R.string.device_not_connected))
        } else {
            addNewGroup()
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
                        ToastUtils.showLong(getString(R.string.rename_tip_check))
                    } else {
                        //往DB里添加组数据
                        val dbGroup = DBUtils.addNewGroupWithType(textGp.text.toString().trim { it <= ' ' }, Constant.DEVICE_TYPE_LIGHT_RGB)
                        dbGroup?.let {
                            groupList?.add(it)
                        }
                        refreshData()
                        dialog.dismiss()
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()
    }


    */
    /**
     * 删除组，并且把组里的灯的组也都删除。
     *//*
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
                                deleteAllSceneByLightAddr(light.meshAddr)
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
    }*/
}
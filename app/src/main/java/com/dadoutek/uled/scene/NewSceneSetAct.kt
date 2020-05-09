package com.dadoutek.uled.scene

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.support.v4.view.ViewPager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.PopupWindow
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.*
import com.dadoutek.uled.model.DeviceType.LIGHT_RGB
import com.dadoutek.uled.model.DeviceType.SMART_CURTAIN
import com.dadoutek.uled.model.DeviceType.SMART_RELAY
import com.dadoutek.uled.model.ItemGroup
import com.dadoutek.uled.model.ItemRgbGradient
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.othersview.SelectColorAct
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.*
import kotlinx.android.synthetic.main.activity_new_scene_set.*
import kotlinx.android.synthetic.main.toolbar.*

/**
 * 描述	      ${设置场景颜色盘}$
 *
 * 更新者     zcl
 * 更新时间
 * 更新描述   ${设置场景颜色盘}$
 */
class NewSceneSetAct : TelinkBaseActivity(), View.OnClickListener {
    private var currentPosition: Int = 1000000
    private lateinit var currentRgbGradient: ItemRgbGradient
    private var rgbRecyclerView: RecyclerView? = null
    private lateinit var diyGradientList: MutableList<DbDiyGradient>
    private var buildInModeList: ArrayList<ItemRgbGradient> = ArrayList()
    private var isFirst: Boolean = true
    private var currentPageIsEdit = false
    private var scene: DbScene? = null
    private var isChangeScene = false
    private var isChange = true
    private var isResult = false
    private var isToolbar = false
    private var notCheckedGroupList: ArrayList<ItemGroup>? = null
    private val showGroupList: ArrayList<ItemGroup> = arrayListOf()
    private var showCheckListData: MutableList<DbGroup>? = null
    private val sceneGroupAdapter: SceneGroupAdapter = SceneGroupAdapter(R.layout.scene_adapter_layout, showGroupList)
    private val rgbSceneModeAdapter: RgbSceneModeAdapter = RgbSceneModeAdapter(R.layout.scene_mode, buildInModeList)
    private var sceneEditListAdapter: SceneEditListAdapter? = null
    private var editSceneName: String? = null
    private val groupMeshAddrArrayList = java.util.ArrayList<Int>()
    private var guideShowCurrentPage = false
    private val DATA_LIST_KEY = "DATA_LIST_KEY"
    private val SCENE_KEY = "SCENE_KEY"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_scene_set)
        makePop()
        if (savedInstanceState != null && savedInstanceState.containsKey(DATA_LIST_KEY)) {
            isResult = true
            val list = savedInstanceState.getSerializable(DATA_LIST_KEY) as ArrayList<ItemGroup>
            showGroupList.addAll(list)
            scene = savedInstanceState.getParcelable(SCENE_KEY) as? DbScene
        } else {
            isResult = false
        }
        initChangeState()
        initScene()
        if (!isChangeScene) {
            edit_name.setText(DBUtils.getDefaultNewSceneName())
            edit_name.setSelection(DBUtils.getDefaultNewSceneName().length)
            initOnLayoutListener()
        }
    }

    private fun makePop() {
        getModeData()

        var popView = LayoutInflater.from(this).inflate(R.layout.pop_rgb_mode_list, null)
        rgbRecyclerView = popView.findViewById(R.id.pop_scene_mode_recycle)
        rgbRecyclerView?.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        rgbRecyclerView?.adapter = this.rgbSceneModeAdapter
        rgbSceneModeAdapter.notifyDataSetChanged()
        rgbSceneModeAdapter.setOnItemClickListener { _, _, position ->
            currentRgbGradient = buildInModeList[position]
            if (currentPosition != 1000000){
                sceneGroupAdapter.data[currentPosition].gradientName = currentRgbGradient.name
                sceneGroupAdapter.data[currentPosition].gradientId = currentRgbGradient.id
                sceneGroupAdapter.data[currentPosition].gradientType = currentRgbGradient.gradientType //渐变类型 1：自定义渐变  2：内置渐变
            }
            sceneGroupAdapter.notifyDataSetChanged()
            pop?.dismiss()
        }

        pop = PopupWindow(popView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        pop?.let {
            it.isFocusable = true // 设置PopupWindow可获得焦点
            it.isTouchable = true // 设置PopupWindow可触摸补充：
            it.isOutsideTouchable = false
        }
    }

    private fun getModeData() {
        buildInModeList.clear()
        val presetGradientList = resources.getStringArray(R.array.preset_gradient)
        for (i in 1..11) {
            var item = ItemRgbGradient()
            item.id = i
            item.gradientType = 2//渐变类型 1：自定义渐变  2：内置渐变
            item.name = presetGradientList[i - 1]
            buildInModeList?.add(item)
        }

        diyGradientList = DBUtils.diyGradientList
        diyGradientList.forEach {
            var item = ItemRgbGradient()
            item.id = it.id.toInt()
            item.name = it.name
            item.isDiy = true
            item.speed = it.speed
            item.gradientType = 1
            item.colorNodes = it.colorNodes

            buildInModeList?.add(item)
        }

    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (isShouldHideKeyboard(v, ev)) {
                val res = hideKeyboard(v.windowToken)
                if (res) {
                    //隐藏了输入法，则不再分发事件
                    return super.dispatchTouchEvent(ev)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    /**
     * 根据EditText所在坐标和用户点击的坐标相对比，来判断是否隐藏键盘，因为当用户点击EditText时则不能隐藏
     */
    private fun isShouldHideKeyboard(v: View?, event: MotionEvent): Boolean {
        if (v != null && v is EditText) {
            val l = intArrayOf(0, 0)
            v.getLocationInWindow(l)
            val left = l[0]
            val top = l[1]
            val bottom = top + v.height
            val right = left + v.width
            return !(event.x > left && event.x < right && event.y > top && event.y < bottom)
        }
        //如果焦点不是EditText则忽略，这个发生在视图刚绘制完，第一个焦点不在EditText上，和用户用轨迹球选择其他的焦点
        return false
    }

    /**
     * 获取InputMethodManager，隐藏软键盘
     * @param token
     */
    private fun hideKeyboard(token: IBinder?): Boolean {
        if (token != null) {
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            return im.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS)
        }
        return false
    }


    private fun step2Guide() {
        guideShowCurrentPage = !GuideUtils.getCurrentViewIsEnd(this, GuideUtils.END_ADD_SCENE_KEY, false)
        if (guideShowCurrentPage) {
            val guide1 = guide_location
            GuideUtils.guideBuilder(this, GuideUtils.STEP9_GUIDE_ADD_SCENE_SURE)
                    .addGuidePage(GuideUtils.addGuidePage(guide1!!, R.layout.view_guide_simple, getString(R.string.add_scene_guide_2),
                            View.OnClickListener { guide1.performClick() }, GuideUtils.END_ADD_SCENE_KEY, this)).show()
        }
    }

    private fun stepEndGuide() {
        guideShowCurrentPage = !GuideUtils.getCurrentViewIsEnd(this, GuideUtils.END_ADD_SCENE_KEY, false)
        if (guideShowCurrentPage) {
            val guide6 = confirm
            GuideUtils.guideBuilder(this, GuideUtils.STEP13_GUIDE_ADD_SCENE_SAVE)
                    .addGuidePage(GuideUtils.addGuidePage(guide6, R.layout.view_guide_simple_scene_set1, getString(R.string.add_scene_guide_6),
                            View.OnClickListener {
                                guide6.performClick()
                                GuideUtils.changeCurrentViewIsEnd(this, GuideUtils.END_ADD_SCENE_KEY, true)
                            }, GuideUtils.END_ADD_SCENE_KEY, this)).show()
        }
    }


    private fun initChangeState() {
        if (showCheckListData == null)
            showCheckListData = mutableListOf()
        showCheckListData?.clear()
        val allGroups = DBUtils.allGroups
        LogUtils.e("zcl---------所有灯组${DBUtils.allGroups}")
        for (index in allGroups.indices) {
            val gp = allGroups[index]
            //if (gp.deviceCount > 0 || index == 0) {//不含有设备的组也要显示不然加入组1有一个等分到场景
            //再次移除以后重新配置场景则场景改组不在了没法配置
            if (index == 0)
                gp.name = getString(R.string.allLight)
            showCheckListData?.add(gp)
            //}
        }

        LogUtils.e("zcl----------$showCheckListData")
        if (showGroupList!!.size != 0) {

            for (i in showCheckListData!!.indices) {
                for (j in showGroupList!!.indices) {
                    if (showCheckListData!![i].meshAddr == showGroupList!![j].groupAddress) {
                        showCheckListData!![i].checked = true
                        break
                    } else if (j == showGroupList!!.size - 1 && showCheckListData!![i].meshAddr != showGroupList!![j].groupAddress) {
                        showCheckListData!![i].checked = false
                    }
                }
            }
            changeCheckedViewData()
        } else {
            for (i in showCheckListData!!.indices) {
                showCheckListData!![i].isCheckedInGroup = true
                showCheckListData!![i].checked = false
            }
        }
    }

    private fun initScene() {
        val intent = intent
        isChangeScene = intent.extras!!.get(Constant.IS_CHANGE_SCENE) as Boolean
        if (isChangeScene && !isResult) {
            scene = intent.extras!!.get(Constant.CURRENT_SELECT_SCENE) as DbScene
            edit_name.setText(scene?.name)
            //获取场景具体信息
            val actions = DBUtils.getActionsBySceneId(scene!!.id)
            for (i in actions.indices) {
                val item = DBUtils.getGroupByMeshAddr(actions[i].groupAddr)
                val itemGroup = ItemGroup()
                if (item != null) {
                    itemGroup.gpName = item.name
                    itemGroup.enableCheck = false
                    itemGroup.groupAddress = actions[i].groupAddr
                    itemGroup.brightness = actions[i].brightness
                    itemGroup.temperature = actions[i].colorTemperature
                    itemGroup.color = actions[i].color
                    itemGroup.isNo = actions[i].isOn
                    item.checked = true
                    showGroupList!!.add(itemGroup)
                    groupMeshAddrArrayList.add(item.meshAddr)
                }
            }
        }

        if (isResult) {
            if (isChangeScene)
                initChangeView()
            else
                initCreateView()
            showDataListView()
        } else {
            if (isChangeScene) {
                initChangeView()
                showDataListView()
            } else {
                initCreateView()
                showEditListVew()
            }
        }
        edit_data_view_layout.setOnClickListener { }
        confirm.setOnClickListener(this)
        StringUtils.initEditTextFilter(edit_name)
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener {
            if (currentPageIsEdit) {
                if (currentPageIsEdit && !isToolbar)
                    showExitSaveDialog()
                else
                    finish()
            } else showExitSaveDialog()
        }
        step2Guide()
    }

    private fun showExitSaveDialog() {
        val exitDialogBuilder = AlertDialog.Builder(this)
        exitDialogBuilder.setMessage(getString(R.string.save_scene_tip))
        exitDialogBuilder.setPositiveButton(getString(android.R.string.ok)) { _, _ ->
            save()
        }
        exitDialogBuilder.setNegativeButton(getString(R.string.cancel)) { _, _ ->
            finish()
        }

        exitDialogBuilder.create().show()
    }

    private fun initCreateView() {
        toolbar.setTitle(R.string.create_scene)
    }

    private fun initChangeView() {
        toolbar.setTitle(R.string.edit_scene)
        editSceneName = scene!!.name
    }

    internal var onItemChildClickListener: BaseQuickAdapter.OnItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        currentPosition = position
        when (view.id) {
            R.id.btn_delete -> delete(adapter, position)
            R.id.dot_rgb -> changeToColorSelect(position)
            R.id.dot_one -> changeToColorSelect(position)
            R.id.rg_xx -> open(position)
            R.id.rg_yy -> close(position)
            R.id.alg_text -> showPopMode(position)
        }
    }

    private fun showPopMode(position: Int) {
        val itemGroup = showGroupList[position]
        if (itemGroup.rgbType == 1)
            buildInModeList.forEach {
                it.isSceneModeSelect = it.id == itemGroup.gradientId
            }
        rgbSceneModeAdapter.notifyDataSetChanged()
        pop?.showAtLocation(window.decorView, Gravity.BOTTOM, 0, 0)
    }


    private fun close(position: Int) {
        this.showGroupList[position].isNo = false
        sceneGroupAdapter.notifyItemChanged(position)
        Commander.openOrCloseLights(showGroupList[position].groupAddress, false)
    }

    private fun open(position: Int) {
        this.showGroupList[position].isNo = true
        sceneGroupAdapter.notifyItemChanged(position)
        Commander.openOrCloseLights(showGroupList[position].groupAddress, true)

    }

    private fun changeToColorSelect(position: Int) {
        val intent = Intent(this, SelectColorAct::class.java)
        intent.putExtra(Constant.GROUPS_KEY, showGroupList[position])
        startActivityForResult(intent, position)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            try {
                val color = data?.getIntExtra("color", 0)
                showGroupList!![requestCode].color = color!!
                sceneGroupAdapter?.notifyItemChanged(requestCode)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private var onItemClickListenerCheck: BaseQuickAdapter.OnItemClickListener = BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
        changeCheck(position)
    }

    private fun changeCheck(position: Int) {
        val item = showCheckListData!![position]
        if (item.isCheckedInGroup) {
            showCheckListData!![position].checked = !item.checked
            changeCheckedViewData()
            sceneEditListAdapter?.notifyDataSetChanged()
        }
    }

    private fun delete(adapter: BaseQuickAdapter<*, *>, position: Int) {
        for (index in showCheckListData!!.indices) {
            if (showCheckListData!![index].meshAddr == showGroupList!![position].groupAddress)
                showCheckListData!![index].isChecked = false
        }
        adapter.remove(position)
    }

    //显示数据列表页面
    private fun showDataListView() {
        currentPageIsEdit = false
        data_view_layout.visibility = View.VISIBLE
        edit_data_view_layout.visibility = View.GONE
        tv_function1.visibility = View.VISIBLE
        tv_function1.text = getString(R.string.edit)
        tv_function1.setOnClickListener(this)

        val layoutmanager = LinearLayoutManager(this)
        layoutmanager.orientation = LinearLayoutManager.VERTICAL
        recyclerView_group_list_view.layoutManager = layoutmanager
        recyclerView_group_list_view?.addItemDecoration(SpacesItemDecorationScene(40))
        recyclerView_group_list_view.itemAnimator!!.changeDuration = 0
        if (isFirst) {
            isFirst = false
            sceneGroupAdapter.bindToRecyclerView(recyclerView_group_list_view)
        } else
            sceneGroupAdapter.notifyDataSetChanged()

        sceneGroupAdapter.onItemChildClickListener = onItemChildClickListener
    }

    //显示配置数据页面
    private fun showEditListVew() {
        if (editSceneName != null) {
            edit_name.setText(editSceneName)
        } else {
            if (scene != null) {
                edit_name.setText(scene!!.name)
            }
        }
        isToolbar = false
        currentPageIsEdit = true
        data_view_layout.visibility = View.GONE
        edit_data_view_layout.visibility = View.VISIBLE
        tv_function1.visibility = View.GONE
        edit_name.setSelection(edit_name.text.toString().length)

        edit_name!!.isFocusable = true
        edit_name.isFocusableInTouchMode = true
        edit_name.requestFocus()
        edit_name.setOnClickListener(this)

        val inputManager = edit_name.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.showSoftInput(edit_name, 0)

        initChangeState()

        val layoutmanager = LinearLayoutManager(this)
        layoutmanager.orientation = LinearLayoutManager.VERTICAL
        recyclerView_select_group_list_view.layoutManager = layoutmanager

        var list = ArrayList<DbGroup>()
        for (h in showCheckListData!!.indices) {
            if (!OtherUtils.isCurtain(showCheckListData!![h]) || !OtherUtils.isConnector(showCheckListData!![h])) {
                list!!.add(showCheckListData!![h])
            }
        }
        this.sceneEditListAdapter = SceneEditListAdapter(R.layout.scene_group_edit_item, list)


        sceneEditListAdapter?.bindToRecyclerView(recyclerView_select_group_list_view)
        sceneEditListAdapter?.onItemClickListener = onItemClickListenerCheck
    }

    private fun changeCheckedViewData() {
        var isAllCanCheck = true
        for (i in showCheckListData!!.indices) {
            if (showCheckListData!![0].meshAddr == 0xffff && showCheckListData!![0].checked) {
                showCheckListData!![0].isCheckedInGroup = true
                if (showCheckListData!!.size > 1 && i > 0) {
                    showCheckListData!![i].isCheckedInGroup = false
                }
            } else {
                showCheckListData!![0].isCheckedInGroup = false
                if (showCheckListData!!.size > 1 && i > 0) {
                    showCheckListData!![i].isCheckedInGroup = true
                }

                if (i > 0 && showCheckListData!![i].checked) {
                    isAllCanCheck = false//有其他组选中的情况下不允许再次选中所有组
                }
            }
        }
        if (isAllCanCheck) {
            showCheckListData!![0].isCheckedInGroup = true
        }
    }

    private fun changeEditView() {
        showEditListVew()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tv_function1 -> changeEditView()
            R.id.confirm -> save()
        }
    }

    private fun save() {
        saveCurrenEditResult()

        if (edit_name.text.toString().isEmpty()) {
            ToastUtils.showLong(getString(R.string.name_can_not_null))
            return
        }

        if (showGroupList!!.size <= 0) {
            ToastUtils.showLong(R.string.add_scene_gp_tip)
            return
        }
        isToolbar = true
        if (!currentPageIsEdit) {
            saveScene()
        } else {
            showDataListView()
            stepEndGuide()
        }
    }

    private fun saveCurrenEditResult() {
        editSceneName = edit_name.text.toString()
        val oldResultItemList = ArrayList<ItemGroup>()
        val newResultItemList = ArrayList<ItemGroup>()

        for (i in showCheckListData!!.indices) {
            if (showCheckListData!![i].checked) {
                if (showGroupList!!.size == 0) {
                    val newItemGroup = ItemGroup()
                    newItemGroup.brightness = 50
                    newItemGroup.temperature = 50
                    newItemGroup.color = 0xffffff
                    newItemGroup.checked = true
                    newItemGroup.enableCheck = true
                    newItemGroup.gpName = showCheckListData!![i].name
                    newItemGroup.groupAddress = showCheckListData!![i].meshAddr
                    newResultItemList.add(newItemGroup)
                } else {
                    for (j in showGroupList!!.indices) {
                        if (showCheckListData!![i].meshAddr == showGroupList!![j].groupAddress) {
                            oldResultItemList.add(showGroupList!![j])
                            break
                        } else if (j == showGroupList!!.size - 1) {
                            val newItemGroup = ItemGroup()
                            newItemGroup.brightness = 50
                            newItemGroup.temperature = 50
                            newItemGroup.color = 0xffffff
                            newItemGroup.checked = true
                            newItemGroup.enableCheck = true
                            newItemGroup.gpName = showCheckListData!![i].name
                            newItemGroup.groupAddress = showCheckListData!![i].meshAddr
                            newResultItemList.add(newItemGroup)
                        }
                    }
                }
            }
        }
        showGroupList?.clear()
        showGroupList?.addAll(oldResultItemList)
        showGroupList?.addAll(newResultItemList)
    }

    private fun saveScene() {
        if (checked()) {
            saveAndFinish()
        }
    }

    private fun saveAndFinish() {
        if (isChangeScene) {
            updateOldScene()
        } else {
            saveNewScene()
        }
        setResult(Constant.RESULT_OK)
    }


    private fun saveNewScene() {
        showLoadingDialog(getString(R.string.saving))
        Thread {
            val name = editSceneName
            val itemGroups = showGroupList

            val dbScene = DbScene()
            dbScene.id = getSceneId()
            dbScene.name = name
            dbScene.belongRegionId = SharedPreferencesUtils.getCurrentUseRegionId()
            DBUtils.saveScene(dbScene, false)

            val idAction = dbScene.id!!

            for (i in itemGroups!!.indices) {
                val sceneActions = DbSceneActions()

                val item = itemGroups[i]
                LogUtils.e("zcl**********************$item")


                when {
                    OtherUtils.isCurtain(DBUtils.getGroupByMeshAddr(item.groupAddress)) -> {
                        sceneActions.belongSceneId = idAction

                        sceneActions.brightness = item.brightness
                        sceneActions.colorTemperature = item.temperature
                        sceneActions.groupAddr = item.groupAddress
                        sceneActions.setColor(item.color)
                        sceneActions.deviceType = 0x10
                        sceneActions.isOn = item.isNo

                        DBUtils.saveSceneActions(sceneActions)
                    }
                    OtherUtils.isConnector(DBUtils.getGroupByMeshAddr(item.groupAddress)) -> {
                        sceneActions.belongSceneId = idAction
                        sceneActions.brightness = item.brightness
                        sceneActions.colorTemperature = item.temperature
                        sceneActions.groupAddr = item.groupAddress
                        sceneActions.setColor(item.color)
                        sceneActions.deviceType = 0x05
                        sceneActions.isOn = item.isNo
                        DBUtils.saveSceneActions(sceneActions)
                    }
                    OtherUtils.isRGBGroup(DBUtils.getGroupByMeshAddr(item.groupAddress)) -> {
                        sceneActions.belongSceneId = idAction
                        sceneActions.brightness = item.brightness
                        sceneActions.colorTemperature = item.temperature
                        sceneActions.groupAddr = item.groupAddress
                        sceneActions.setColor(item.color)
                        sceneActions.deviceType = 0x06

                        DBUtils.saveSceneActions(sceneActions)
                    }
                    else -> {
                        sceneActions.belongSceneId = idAction
                        sceneActions.brightness = item.brightness
                        sceneActions.colorTemperature = item.temperature
                        sceneActions.groupAddr = item.groupAddress
                        sceneActions.setColor(item.color)

                        sceneActions.deviceType = 0x04
                        DBUtils.saveSceneActions(sceneActions)
                    }
                }
            }

            try {
                Thread.sleep(100)
                addScene(idAction)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } finally {
                hideLoadingDialog()
                finish()
            }
        }.start()
    }

    @Throws(InterruptedException::class)
    private fun addScene(id: Long) {
        val opcode = Opcode.SCENE_ADD_OR_DEL
        val list = DBUtils.getActionsBySceneId(id)
        var params: ByteArray

        for (i in list.indices) {
            var count = 0
            do {
                count++
                Thread.sleep(300)
                var temperature = list[i].colorTemperature.toByte()
                if (temperature > 99)
                    temperature = 99
                var light = list[i].brightness.toByte()
                if (light > 99)
                    light = 99
                val color = list[i].getColor()
                var red = color and 0xff0000 shr 16
                var green = color and 0x00ff00 shr 8
                var blue = color and 0x0000ff
                var w = color shr 24
                var type = list[i].deviceType
                if (type == SMART_CURTAIN) {
                    if (list[i].isOn) {
                        params = byteArrayOf(0x01, id.toByte(), light, red.toByte(), green.toByte(), blue.toByte(), temperature, w.toByte(), 0x01) //窗帘开是1
                        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, list[i].groupAddr, params)
                    } else {
                        params = byteArrayOf(0x01, id.toByte(), light, red.toByte(), green.toByte(), blue.toByte(), temperature, w.toByte(), 0x02)  //窗帘关是2
                        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, list[i].groupAddr, params)
                    }
                } else if (type == SMART_RELAY) {
                    if (list[i].isOn) {
                        params = byteArrayOf(0x01, id.toByte(), light, red.toByte(), green.toByte(), blue.toByte(), temperature, w.toByte(), 0x01) //接收器开是1
                        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, list[i].groupAddr, params)
                    } else {
                        params = byteArrayOf(0x01, id.toByte(), light, red.toByte(), green.toByte(), blue.toByte(), temperature, w.toByte(), 0x02) //接收器关是2
                        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, list[i].groupAddr, params)
                    }
                } else if (type == LIGHT_RGB) {
                    params = byteArrayOf(0x01, id.toByte(), light, red.toByte(), green.toByte(), blue.toByte(), list[i].brightness.toByte(), temperature)
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, list[i].groupAddr, params)
                } else {
                    params = byteArrayOf(0x01, id.toByte(), light, red.toByte(), green.toByte(), blue.toByte(), temperature, w.toByte())
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, list[i].groupAddr, params)
                }

            } while (count < 3)
        }
    }

    private fun getSceneId(): Long {
        val list = DBUtils.sceneList
        val idList = java.util.ArrayList<Int>()
        for (i in list.indices) {
            idList.add(list[i].id!!.toInt())
        }

        var id = 0
        for (i in 1..100) {
            if (idList.contains(i)) {
                Log.d("sceneID", "getSceneId: " + "aaaaa")
                continue
            } else {
                id = i
                Log.d("sceneID", "getSceneId: bbbbb$id")
                break
            }
        }

        if (list.size == 0) {
            id = 1
        }

        return java.lang.Long.valueOf(id.toLong())
    }

    private fun updateOldScene() {
        showLoadingDialog(getString(R.string.saving))
        Thread {
            val name = editSceneName
            val itemGroups = showGroupList
            val nameList = java.util.ArrayList<Int>()

            scene?.name = name
            DBUtils.updateScene(scene!!)
            val idAction = scene?.id!!

            DBUtils.deleteSceneActionsList(DBUtils.getActionsBySceneId(scene?.getId()!!))

            for (i in itemGroups!!.indices) {
                val sceneActions = DbSceneActions()

                when {
                    OtherUtils.isCurtain(DBUtils.getGroupByMeshAddr(itemGroups[i].groupAddress)) -> {
                        sceneActions.belongSceneId = idAction
                        sceneActions.brightness = itemGroups[i].brightness
                        sceneActions.colorTemperature = itemGroups[i].temperature
                        sceneActions.groupAddr = itemGroups[i].groupAddress
                        sceneActions.setColor(itemGroups[i].color)
                        sceneActions.deviceType = 0x10
                        sceneActions.isOn = itemGroups[i].isNo


                        nameList.add(itemGroups[i].groupAddress)
                        DBUtils.saveSceneActions(sceneActions)
                    }
                    OtherUtils.isConnector(DBUtils.getGroupByMeshAddr(itemGroups.get(i).groupAddress)) -> {
                        sceneActions.belongSceneId = idAction
                        sceneActions.brightness = itemGroups[i].brightness
                        sceneActions.colorTemperature = itemGroups[i].temperature
                        sceneActions.groupAddr = itemGroups[i].groupAddress
                        sceneActions.setColor(itemGroups[i].color)
                        sceneActions.deviceType = 0x05
                        sceneActions.isOn = itemGroups[i].isNo

                        nameList.add(itemGroups[i].groupAddress)
                        DBUtils.saveSceneActions(sceneActions)
                    }
                    OtherUtils.isRGBGroup(DBUtils.getGroupByMeshAddr(itemGroups[i].groupAddress)) -> {
                        sceneActions.belongSceneId = idAction
                        sceneActions.brightness = itemGroups[i].brightness
                        sceneActions.colorTemperature = itemGroups[i].temperature
                        sceneActions.groupAddr = itemGroups[i].groupAddress
                        sceneActions.setColor(itemGroups[i].color)

                        sceneActions.deviceType = 0x06
                        nameList.add(itemGroups[i].groupAddress)
                        DBUtils.saveSceneActions(sceneActions)
                    }
                    else -> {
                        sceneActions.belongSceneId = idAction
                        sceneActions.brightness = itemGroups[i].brightness
                        sceneActions.colorTemperature = itemGroups[i].temperature
                        sceneActions.groupAddr = itemGroups[i].groupAddress
                        sceneActions.setColor(itemGroups[i].color)
                        sceneActions.deviceType = 0x04

                        nameList.add(itemGroups[i].groupAddress)
                        DBUtils.saveSceneActions(sceneActions)
                    }
                }
            }


            isChange = compareList(nameList, groupMeshAddrArrayList)

            try {
                Thread.sleep(100)
                updateScene(idAction)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } finally {
                hideLoadingDialog()
                finish()
            }
        }.start()
    }

    @Throws(InterruptedException::class)
    private fun updateScene(id: Long) {
        deleteScene(id)
        val opcode = Opcode.SCENE_ADD_OR_DEL
        val list = DBUtils.getActionsBySceneId(id)
        var params: ByteArray
        for (i in list.indices) {
            Thread.sleep(100)
            var temperature = list[i].colorTemperature.toByte()
            if (temperature > 99)
                temperature = 99
            var light = list[i].brightness.toByte()
            if (light > 99)
                light = 99
            val color = list[i].getColor()
            var red = color and 0xff0000 shr 16
            var green = color and 0x00ff00 shr 8
            var blue = color and 0x0000ff
            var w = color shr 24

            val logStr = String.format("R = %x, G = %x, B = %x", red, green, blue)
            Log.d("RGBCOLOR", logStr)
            var type = list[i].deviceType
            if (type == SMART_CURTAIN) {
                if (list[i].isOn) {
                    params = byteArrayOf(0x01, id.toByte(), light, red.toByte(), green.toByte(), blue.toByte(), temperature, w.toByte(), 0x01)  //窗帘开是1
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, list[i].groupAddr, params)
                } else {
                    params = byteArrayOf(0x01, id.toByte(), light, red.toByte(), green.toByte(), blue.toByte(), temperature, w.toByte(), 0x02)  //窗帘关是2
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, list[i].groupAddr, params)
                }
            } else if (type == SMART_RELAY) {
                if (list[i].isOn) {
                    params = byteArrayOf(0x01, id.toByte(), light, red.toByte(), green.toByte(), blue.toByte(), temperature, w.toByte(), 0x01)  //接收器开是1
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, list[i].groupAddr, params)
                } else {
                    params = byteArrayOf(0x01, id.toByte(), light, red.toByte(), green.toByte(), blue.toByte(), temperature, w.toByte(), 0x02)  //接收器关是2
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, list[i].groupAddr, params)
                }
            } else if (type == LIGHT_RGB) {
                if (sceneGroupAdapter.getIsColorMode()) {
                    params = byteArrayOf(0x01, id.toByte(), light, red.toByte(), green.toByte(), blue.toByte(), list[i].brightness.toByte(), temperature)
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, list[i].groupAddr, params)
                } else {
                    params = byteArrayOf(0x01, id.toByte(), light, red.toByte(), green.toByte(), blue.toByte(), list[i].brightness.toByte(), temperature)
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, list[i].groupAddr, params)
                }
            } else {
                params = byteArrayOf(0x01, id.toByte(), light, red.toByte(), green.toByte(), blue.toByte(), temperature, w.toByte())
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, list[i].groupAddr, params)
            }
        }
    }

    private fun deleteScene(id: Long) {
        if (isChange) {
            val opcode = Opcode.SCENE_ADD_OR_DEL
            val params: ByteArray = byteArrayOf(0x00, id.toByte())
            try {
                Thread.sleep(100)
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, 0xFFFF, params)
                Thread.sleep(300)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

        }
    }

    private fun compareList(actionsList: List<Int>, actionsList1: java.util.ArrayList<Int>): Boolean {
        return if (actionsList.size == actionsList1.size) {
            !actionsList1.containsAll(actionsList)
        } else {
            true
        }
    }

    private fun checked(): Boolean {

        notCheckedGroupList = ArrayList()
        initChangeState()

        if (showGroupList!!.size == 0) {
            ToastUtils.showLong(R.string.add_scene_gp_tip)
            return false
        }

        for (i in showCheckListData!!.indices) {
            if (!showCheckListData!![i].checked) {
                if (showCheckListData!![i].meshAddr != 0xffff) {
                    val newItemGroup = ItemGroup()
                    newItemGroup.brightness = 0
                    newItemGroup.temperature = 0
                    newItemGroup.color = 0xffffff
                    newItemGroup.checked = true
                    newItemGroup.enableCheck = true
                    newItemGroup.gpName = showCheckListData!![i].name
                    newItemGroup.groupAddress = showCheckListData!![i].meshAddr
                    notCheckedGroupList!!.add(newItemGroup)
                }
            }
        }
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event?.action == KeyEvent.ACTION_DOWN)
            if (currentPageIsEdit) {
                if (currentPageIsEdit && !isToolbar)
                    showExitSaveDialog()
                else
                    finish()
            } else showExitSaveDialog()
        return super.onKeyDown(keyCode, event)
    }
}
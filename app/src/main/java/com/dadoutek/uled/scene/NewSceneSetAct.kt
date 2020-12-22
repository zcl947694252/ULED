package com.dadoutek.uled.scene

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
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
import com.dadoutek.uled.model.dbModel.*
import com.dadoutek.uled.model.DeviceType.LIGHT_RGB
import com.dadoutek.uled.model.DeviceType.SMART_CURTAIN
import com.dadoutek.uled.model.DeviceType.SMART_RELAY
import com.dadoutek.uled.model.ItemGroup
import com.dadoutek.uled.model.ItemRgbGradient
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkStatusCode
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.network.RouterTimeoutBean
import com.dadoutek.uled.othersview.SelectColorAct
import com.dadoutek.uled.router.SceneAddBodyBean
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.router.bean.RouteSceneBean
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.*
import com.telink.TelinkApplication
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_new_scene_set.*
import kotlinx.android.synthetic.main.scene_adapter_layout.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

/**
 * 描述	      ${设置场景颜色盘}$
 *
 * 更新者     zcl
 * 更新时间
 * 更新描述   ${设置场景颜色盘}$
 */
class NewSceneSetAct : TelinkBaseActivity() {
    private var brightness: Int = 0
    private var whiteLight: Int = 0
    private var sceneGroupAdapter: SceneGroupAdapter? = null
    private var rgbGradientId: Int = 1
    private var resId: Int = R.drawable.icon_out
    private var currentPosition: Int = 1000000
    private lateinit var currentRgbGradient: ItemRgbGradient
    private var rgbRecyclerView: RecyclerView? = null
    private lateinit var diyGradientList: MutableList<DbDiyGradient>
    private var buildInModeList: ArrayList<ItemRgbGradient> = ArrayList()
    private var isOpen: Boolean = true
    private var currentPageIsEdit = false
    private var dbScene: DbScene? = null
    private var isReconfig = false
    private var isChange = true
    private var isResult = false
    private var isToolbar = false
    private var notCheckedGroupList: ArrayList<ItemGroup>? = null
    private val showGroupList: ArrayList<ItemGroup> = arrayListOf()
    private var showCheckListData: MutableList<DbGroup>? = null
    private val rgbSceneModeAdapter: RgbSceneModeAdapter = RgbSceneModeAdapter(R.layout.scene_mode, buildInModeList)
    private var sceneEditListAdapter: SceneEditListAdapter? = null
    private var editSceneName: String? = null
    private val groupMeshAddrArrayList = java.util.ArrayList<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_scene_set)
        makePop()
        /*  if (savedInstanceState != null && savedInstanceState.containsKey(DATA_LIST_KEY)) {//获取以保存数据
              isResult = true
              val list = savedInstanceState.getSerializable(DATA_LIST_KEY) as ArrayList<ItemGroup>
              showGroupList.addAll(list)
              scene = savedInstanceState.getParcelable(SCENE_KEY) as? DbScene
          } else {
              isResult = false
          }*/

        select_icon_ly.setOnClickListener {
            startActivityForResult(Intent(this@NewSceneSetAct, SelectSceneIconActivity::class.java), 1100)
        }

        initChangeState()
        initScene()//获取传递过来的场景数据
        if (!isReconfig) {
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
            if (currentPosition != 1000000) {
                sceneGroupAdapter?.data?.get(currentPosition)?.gradientName = currentRgbGradient.name
                sceneGroupAdapter?.data?.get(currentPosition)?.gradientId = currentRgbGradient.id
                sceneGroupAdapter?.data?.get(currentPosition)?.gradientType = currentRgbGradient.gradientType //渐变类型 1：自定义渐变  2：内置渐变
            }
            // this.showGroupList[position].isOn = false//该group设备是开还是关
            sceneGroupAdapter?.notifyItemChanged(currentPosition)
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
                if (res)  //隐藏了输入法，则不再分发事件
                    return super.dispatchTouchEvent(ev)
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
            showCheckListData?.add(gp) //}
        }
        when {
            showGroupList!!.size != 0 -> {
                for (i in showCheckListData!!.indices) {
                    loop@ for (j in showGroupList!!.indices) {
                        when {
                            showCheckListData!![i].meshAddr == showGroupList!![j].groupAddress -> {
                                showCheckListData!![i].checked = true
                                break@loop
                            }
                            j == showGroupList!!.size - 1 && showCheckListData!![i].meshAddr != showGroupList!![j].groupAddress -> {
                                showCheckListData!![i].checked = false
                            }
                        }
                    }
                }
                changeCheckedViewData()
            }
            else -> {
                for (i in showCheckListData!!.indices) {
                    showCheckListData!![i].isCheckedInGroup = true
                    showCheckListData!![i].checked = false
                }
            }
        }
    }

    private fun initScene() {
        val intent = intent
        isReconfig = intent.extras!!.get(Constant.IS_CHANGE_SCENE) as Boolean
        if (isReconfig && !isResult) {
            showGroupList.clear()
            dbScene = intent.extras!!.get(Constant.CURRENT_SELECT_SCENE) as DbScene
            edit_name.setText(dbScene?.name)
            resId = if (TextUtils.isEmpty(dbScene?.imgName)) R.drawable.icon_out else OtherUtils.getResourceId(dbScene?.imgName, this)
            //获取场景具体信息
            val actions = DBUtils.getActionsBySceneId(dbScene!!.id)
            for (i in actions.indices) {
                val item = DBUtils.getGroupByMeshAddr(actions[i].groupAddr)
                if (item.id != 0L && item.meshAddr != 0) {
                    val itemGroup = ItemGroup()
                    itemGroup.gpName = item.name
                    itemGroup.enableCheck = false
                    itemGroup.groupAddress = actions[i].groupAddr
                    itemGroup.brightness = actions[i].brightness
                    itemGroup.temperature = actions[i].colorTemperature
                    itemGroup.color = actions[i].color
                    itemGroup.isOn = actions[i].isOn
                    itemGroup.isEnableBright = actions[i].isEnableBright
                    itemGroup.isEnableWhiteBright = actions[i].isEnableWhiteBright
                    item.checked = true
                    itemGroup.rgbType = actions[i].rgbType
                    itemGroup.gradientId = actions[i].gradientId
                    itemGroup.gradientType = actions[i].gradientType
                    itemGroup.gradientName = actions[i].gradientName
                    itemGroup.gradientSpeed = actions[i].gradientSpeed
                    itemGroup.deviceType = actions[i].deviceType
                    showGroupList.add(itemGroup)
                    groupMeshAddrArrayList.add(item.meshAddr)
                } else {
                    val mutableListOf = mutableListOf<DbSceneActions>()
                    mutableListOf.add(actions[i])
                    DBUtils.deleteSceneActionsList(mutableListOf)
                }
            }
            isReconfig = showGroupList.isNotEmpty() || groupMeshAddrArrayList.isNotEmpty()
            if (!isReconfig) {
                ToastUtils.showShort(getString(R.string.gp_deletescene))
                DBUtils.deleteScene(dbScene!!)
            }
        }

        when {
            isReconfig -> {
                toolbarTv.text = dbScene!!.name
                editSceneName = dbScene!!.name
                showGpDetailList()
            }
            else -> {
                toolbarTv.setText(R.string.create_scene)
                when {
                    isResult -> showGpDetailList()
                    else -> showEditListVew()
                }
            }
        }

        edit_data_view_layout.setOnClickListener { }
        confirm.setOnClickListener { save() }
        StringUtils.initEditTextFilter(edit_name)
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener {
            when {
                currentPageIsEdit -> {
                    if (currentPageIsEdit && !isToolbar)
                        showExitSaveDialog()
                    else
                        finish()
                }
                else -> showExitSaveDialog()
            }
        }
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

    internal var onItemChildClickListener: BaseQuickAdapter.OnItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        currentPosition = position
        if (DebouncedClickPredictor.shouldDoClick(view)) {
            when (view.id) {
                R.id.btn_delete -> delete(adapter, position)
                R.id.dot_rgb -> changeToColorSelect(position)
                R.id.dot_one_ly -> changeToColorSelect(position)
                R.id.rg_xx -> open(position)
                R.id.rg_yy -> close(position)
                R.id.alg_text -> showPopMode(position)
                R.id.cb_total -> switchTotal(position)
                R.id.cb_bright -> switchBright(position)
                R.id.cb_white_light -> switchWhiteLight(position)
                R.id.color_mode_rb -> setRGBJB(position, 0)
                R.id.gradient_mode_rb -> setRGBJB(position, 1)
            }
        }
    }

    private fun setRGBJB(pos: Int, rgbType: Int) {
        if (sceneGroupAdapter?.data == null)
            return
        sceneGroupAdapter?.data!![pos].rgbType = rgbType
        val itemGroup = sceneGroupAdapter?.data?.get(currentPosition)
        when (rgbType) {
            1 -> {
                val presetGradientList = resources.getStringArray(R.array.preset_gradient)
                itemGroup?.gradientName = presetGradientList[0]
                rgbGradientId = 1
                itemGroup?.gradientId = rgbGradientId
                itemGroup?.gradientType = 2 //渐变类型 1：自定义渐变  2：内置渐变
                itemGroup?.gradientSpeed = 1 //渐变类型 1：自定义渐变  2：内置渐变
            }
            0 -> {
                itemGroup?.brightness = 50
                itemGroup?.color = (50 shl 24) or (255 shl 16) or (255 shl 8) or 255
                itemGroup?.temperature = 50
                itemGroup?.checked = true
                itemGroup?.enableCheck = false
            }
        }
        showGroupList[pos].rgbType = rgbType
        sceneGroupAdapter?.notifyDataSetChanged()
    }

    private fun showPopMode(position: Int) {
        // val itemGroup = showGroupList[position]
        // if (itemGroup.rgbType == 1)
        //     buildInModeList.forEach {  it.isSceneModeSelect = it.id == itemGroup.gradientId }
        // rgbSceneModeAdapter.notifyDataSetChanged()
        // pop?.showAtLocation(window.decorView, Gravity.BOTTOM, 0, 0)
        startActivityForResult(Intent(this@NewSceneSetAct, SelectGradientActivity::class.java), 1000)
    }

    private fun switchTotal(position: Int) {
        if (showGroupList[position].isOn) {
            isOpen = false
            if (!Constant.IS_ROUTE_MODE)
                showGroupList[position].isOn = false
            showGroupList[position].isEnableBright = false
            showGroupList[position].isEnableWhiteBright = false
            cb_bright.isChecked = false
            cb_bright.isEnabled = false
            cb_white_light.isChecked = false
            cb_white_light.isEnabled = false
            dot_rgb.isEnabled = false
            // brightness = 0
            // temperature = 0
        } else {
            if (!Constant.IS_ROUTE_MODE)
                showGroupList[position].isOn = true
            isOpen = true
            showGroupList[position].isEnableBright = true
            showGroupList[position].isEnableWhiteBright = true
            cb_bright.isChecked = true
            cb_bright.isEnabled = true
            cb_white_light.isChecked = true
            cb_white_light.isEnabled = true
            dot_rgb.isEnabled = true
        }
        val addr = showGroupList[position].groupAddress
        if (!Constant.IS_ROUTE_MODE)
            Thread {
                Commander.openOrCloseLights(showGroupList[position].groupAddress, showGroupList[position].isOn)
                /* Thread.sleep(300)
                 TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.SET_LUM, addr, byteArrayOf(brightness.toByte()), true)
                 Thread.sleep(300)
                 TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.SET_W_LUM, addr, byteArrayOf(temperature.toByte()), true)*/
            }.start()
        else {
            var status = if (isOpen) 1 else 0
            routeOpenOrCloseBase(showGroupList[position].groupAddress, 97, status, "openOrCloseGp")
        }
        sceneGroupAdapter?.notifyItemChanged(position, showGroupList.size)
        //sceneSetGpAdapter()
    }

    private fun switchBright(position: Int) {
        when {
            showGroupList[position].isEnableBright -> {//  enableBright(false)
                showGroupList[position].isEnableBright = false
                brightness = 0
            }
            else -> {//  enableBright(true)
                showGroupList[position].isEnableBright = true
                brightness = showGroupList[position].brightness
            }
        }

        val addr = showGroupList[position].groupAddress
        if (Constant.IS_ROUTE_MODE) {
            var isEnableBright= if (showGroupList[position].isEnableBright) 1 else 0
            routeConfigBriGpOrLight(addr, 97, brightness, isEnableBright,"configBri")
        } else {
            val params: ByteArray = byteArrayOf(brightness.toByte())
            TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.SET_LUM, addr, params, true)
        }
        sceneGroupAdapter?.notifyItemChanged(position, showGroupList.size)
    }

    private fun switchWhiteLight(position: Int) {
        when {
            showGroupList[position].isEnableWhiteBright -> {//enableWhiteLight(false)
                whiteLight = 0
                showGroupList[position].isEnableWhiteBright = false
            }
            else -> {//enableWhiteLight(true)
                whiteLight = showGroupList[position].temperature
                showGroupList[position].isEnableWhiteBright = true
            }
        }
        val addr = showGroupList[position].groupAddress
        if (Constant.IS_ROUTE_MODE) {
            routeConfigWhiteGpOrLight(addr, 97, whiteLight, showGroupList[position].isEnableWhiteBright,"configWhite")
        } else {
            val params: ByteArray = byteArrayOf(whiteLight.toByte())//设置白色
            TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.SET_W_LUM, addr, params, true)
        }
        sceneGroupAdapter?.notifyItemChanged(position, showGroupList.size)
    }

    private fun close(position: Int) {
        isOpen = false
        if (Constant.IS_ROUTE_MODE) {//meshType普通灯 = 4 彩灯 = 6 连接器 = 5 组 = 97
            routeOpenOrCloseBase(showGroupList[position]!!.groupAddress, 97, 0, "newScene")//0关1开
        } else {
            this.showGroupList[position].isOn = false
            sceneGroupAdapter?.notifyItemChanged(position, showGroupList.size)
            Commander.openOrCloseLights(showGroupList[position].groupAddress, false)
        }
    }

    private fun open(position: Int) {
        isOpen = true
        if (Constant.IS_ROUTE_MODE) {//meshType普通灯 = 4 彩灯 = 6 连接器 = 5 组 = 97
            routeOpenOrCloseBase(showGroupList[position]!!.groupAddress, 97, 1, "newScene")//0关1开
        } else {
            this.showGroupList[position].isOn = true
            sceneGroupAdapter?.notifyItemChanged(position, showGroupList.size)
            Commander.openOrCloseLights(showGroupList[position].groupAddress, true)
        }
    }

    @SuppressLint("CheckResult")
    open fun routeConfigWhiteGpOrLight(meshAddr: Int, deviceType: Int, white: Int, isWhiteBright: Boolean, serId: String) {
        LogUtils.v("zcl----------- zcl-----------发送路由调白色参数-------$white-------")
        val group = DBUtils.getGroupByID(meshAddr.toLong())
        var gpColor = group?.color ?: 0
        val red = (gpColor and 0xff0000) shr 16
        val green = (gpColor and 0x00ff00) shr 8
        val blue = gpColor and 0x0000ff
        var color = (white shl 24) or (red shl 16) or (green shl 8) or blue
        var isEnableWhiteBright =  if (isWhiteBright) 1 else 0
        RouterModel.routeConfigWhiteNum(meshAddr, deviceType, color, isEnableWhiteBright,serId)?.subscribe({
            //    "errorCode": 90018"该设备不存在，请重新刷新数据"    "errorCode": 90008,"该设备没有绑定路由，无法操作"
            //    "errorCode": 90007,"该组不存在，请重新刷新数据    "errorCode": 90005"message": "该设备绑定的路由没在线"
            configBriOrColorTempResult(it, 2)
        }) {
            ToastUtils.showShort(it.message)
        }
    }

    override fun tzRouterConfigBriOrTemp(cmdBean: CmdBodyBean, isBri: Int) {
        LogUtils.v("zcl-----------收到路由亮度色温白光通知-------$cmdBean---$isBri")
        hideLoadingDialog()

        disposableRouteTimer?.dispose()
        hideLoadingDialog()
        when (cmdBean.status) {
            0 -> { //成功则不需处理
            }
            else -> {//失败则还原
                when (isBri) {
                    0 -> {
                        ToastUtils.showShort(getString(R.string.config_bri_fail))
                    }
                    1 -> {
                        showGroupList[currentPosition].isEnableBright = whiteLight == 0
                        ToastUtils.showShort(getString(R.string.config_color_temp_fail))
                    }
                    2 -> {
                        showGroupList[currentPosition].isEnableWhiteBright = whiteLight == 0
                        ToastUtils.showShort(getString(R.string.config_white_fail))
                    }
                }
                sceneGroupAdapter?.notifyDataSetChanged()
            }
        }
    }


    private fun changeToColorSelect(position: Int) {
        val intent = Intent(this, SelectColorAct::class.java)
        intent.putExtra(Constant.GROUPS_KEY, showGroupList[position])
        startActivityForResult(intent, position)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                1000 -> {
                    val itemRgbGradient = data?.getSerializableExtra("data") as ItemRgbGradient
                    if (currentPosition != 1000000) {
                        sceneGroupAdapter?.data?.get(currentPosition)?.gradientName = itemRgbGradient.name
                        rgbGradientId = itemRgbGradient.id
                        sceneGroupAdapter?.data?.get(currentPosition)?.gradientId = rgbGradientId
                        sceneGroupAdapter?.data?.get(currentPosition)?.gradientType = itemRgbGradient.gradientType //渐变类型 1：自定义渐变  2：内置渐变
                    }
                    sceneGroupAdapter?.notifyDataSetChanged()
                }
                1100 -> {
                    when (val intExtra = data?.getIntExtra("ID", 0)) {
                        0 -> ToastUtils.showShort(getString(R.string.invalid_data))
                        else -> {
                            intExtra?.let { resId = it }
                            scene_icon.setImageResource(resId);
                        }
                    }
                }
                else -> {
                    try {
                        val color = data?.getIntExtra("color", 0)
                        showGroupList!![requestCode].color = color!!
                        sceneGroupAdapter?.notifyItemChanged(requestCode)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
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

    @SuppressLint("StringFormatInvalid")
    private fun delete(adapter: BaseQuickAdapter<*, *>, position: Int) {
        if (position <= showGroupList!!.size - 1)
            android.support.v7.app.AlertDialog.Builder(Objects.requireNonNull<FragmentActivity>(this))
                    .setMessage(getString(R.string.delete_group_confirm, showGroupList!![position]?.gpName))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        this.showLoadingDialog(getString(R.string.deleting))
                        val itemGroup = showGroupList!![position]
                        for (index in showCheckListData!!.indices) {
                            if (showCheckListData!![index].meshAddr == itemGroup.groupAddress) {
                                showGroupList.removeAt(position)
                                showCheckListData!![index].isChecked = false
                            }
                        }
                        adapter.notifyDataSetChanged()
                        hideLoadingDialog()
                    }
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show()
    }

    //显示数据列表页面
    private fun showGpDetailList() {
        currentPageIsEdit = false

        data_view_layout.visibility = View.VISIBLE
        edit_data_view_layout.visibility = View.GONE
        // tv_function1.visibility = View.GONE
        //  tv_function1.text = getString(R.string.edit)
        // tv_function1.setOnClickListener { changeEditView() }
        img_function1.visibility = View.VISIBLE
        img_function1.setImageResource(R.drawable.icon_editor)
        img_function1.setOnClickListener { changeEditView() }

        val layoutmanager = LinearLayoutManager(this)
        layoutmanager.orientation = LinearLayoutManager.VERTICAL
        scene_gp_detail_list.layoutManager = layoutmanager
        scene_gp_detail_list?.addItemDecoration(SpacesItemDecorationScene(40))
        scene_gp_detail_list.itemAnimator!!.changeDuration = 0
        if (sceneGroupAdapter?.stompRecevice != null)
            unregisterReceiver(sceneGroupAdapter?.stompRecevice)
        sceneSetGpAdapter()
        registBrocaster()
        //sceneGroupAdapter?.initStompReceiver()

        sceneGroupAdapter?.onItemChildClickListener = onItemChildClickListener
    }

    private fun sceneSetGpAdapter() {
        sceneGroupAdapter = SceneGroupAdapter(R.layout.scene_adapter_layout, showGroupList)
        sceneGroupAdapter?.bindToRecyclerView(scene_gp_detail_list)
    }

    private fun registBrocaster() {
        val filter = IntentFilter()
        filter.addAction(Constant.LOGIN_OUT)
        filter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY - 1
        try {
            unregisterReceiver(sceneGroupAdapter?.stompRecevice)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        registerReceiver(sceneGroupAdapter?.stompRecevice, filter)
    }

    //显示配置数据页面
    private fun showEditListVew() {
        when {
            editSceneName != null -> edit_name.setText(editSceneName)
            dbScene != null -> edit_name.setText(dbScene!!.name)
        }
        resId = if (TextUtils.isEmpty(dbScene?.imgName)) R.drawable.icon_out else OtherUtils.getResourceId(dbScene?.imgName, this)
        scene_icon.setImageResource(resId)

        isToolbar = false
        currentPageIsEdit = true
        data_view_layout.visibility = View.GONE
        edit_data_view_layout.visibility = View.VISIBLE
        tv_function1.visibility = View.GONE
        edit_name.setSelection(edit_name.text.toString().length)

        edit_name!!.isFocusable = true
        edit_name.isFocusableInTouchMode = true
        edit_name.requestFocus()

        val inputManager = edit_name.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.showSoftInput(edit_name, 0)

        initChangeState()

        val layoutmanager = LinearLayoutManager(this)
        layoutmanager.orientation = LinearLayoutManager.VERTICAL
        // recyclerView_select_group_list_view.layoutManager = layoutmanager
        scene_gp_bottom_list.layoutManager = GridLayoutManager(this, 4)

        var list = ArrayList<DbGroup>()
        showCheckListData?.forEach {
            list.add(it)
        }
        this.sceneEditListAdapter = SceneEditListAdapter(R.layout.template_batch_small_item, list)
        sceneEditListAdapter?.bindToRecyclerView(scene_gp_bottom_list)
        sceneEditListAdapter?.onItemClickListener = onItemClickListenerCheck
    }

    private fun changeCheckedViewData() {
        var isAllCanCheck = true
        for (i in showCheckListData!!.indices) {
            when {
                showCheckListData!![0].meshAddr == 0xffff && showCheckListData!![0].checked -> {
                    showCheckListData!![0].isCheckedInGroup = true
                    if (showCheckListData!!.size > 1 && i > 0)
                        showCheckListData!![i].isCheckedInGroup = false
                }
                else -> {
                    showCheckListData!![0].isCheckedInGroup = false
                    if (showCheckListData!!.size > 1 && i > 0)
                        showCheckListData!![i].isCheckedInGroup = true
                    if (i > 0 && showCheckListData!![i].checked)
                        isAllCanCheck = false//有其他组选中的情况下不允许再次选中所有组
                }
            }
        }
        if (isAllCanCheck)
            showCheckListData!![0].isCheckedInGroup = true

    }

    private fun changeEditView() {
        toolbarTv.text = getString(R.string.create_scene)
        showEditListVew()
    }

    private fun save() {
        LogUtils.v("zcl-----------baocunchangjing-------")
        if (edit_name.text.toString().isEmpty()) {
            ToastUtils.showLong(getString(R.string.plaese_input_scene_name))
            return
        }
        saveCurrenEditResult()

        editSceneName = edit_name.text.toString()
        toolbarTv.text = editSceneName

        when {
            showGroupList!!.size <= 0 -> ToastUtils.showLong(R.string.add_scene_gp_tip)
            else -> {
                isToolbar = true
                if (!currentPageIsEdit)
                    saveScene()
                else {//添加场景选择分组
                    if (Constant.IS_ROUTE_MODE && isReconfig)
                        updateSceneName()
                    showGpDetailList()
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun updateSceneName() {
        val s = OtherUtils.getResourceName(resId!!, this@NewSceneSetAct).split("/")[1]
        RouterModel.routeUpdateSceneName((dbScene?.id ?: 0).toInt(), editSceneName!!, s)
                ?.subscribe({
                    when (it.errorCode) {
                        0 -> {}
                        else -> ToastUtils.showShort(getString(R.string.rename_faile))
                    }
                }, {
                    ToastUtils.showShort(getString(R.string.rename_faile))
                })

    }

    private fun saveCurrenEditResult() {
        val oldResultItemList = ArrayList<ItemGroup>()
        val newResultItemList = ArrayList<ItemGroup>()

        for (i in showCheckListData!!.indices) {
            if (showCheckListData!![i].checked) {
                if (showGroupList!!.size == 0) {
                    val newItemGroup = getNewItemGroup(i)
                    newResultItemList.add(newItemGroup)
                } else {
                    loop@ for (j in showGroupList!!.indices) {
                        when {
                            showCheckListData!![i].meshAddr == showGroupList!![j].groupAddress -> {
                                oldResultItemList.add(showGroupList!![j])
                                break@loop
                            }
                            j == showGroupList!!.size - 1 -> {
                                val newItemGroup = getNewItemGroup(i)
                                newResultItemList.add(newItemGroup)
                            }
                        }
                    }
                }
            }
        }
        showGroupList?.clear()
        showGroupList?.addAll(oldResultItemList)
        showGroupList?.addAll(newResultItemList)
    }

    private fun getNewItemGroup(i: Int): ItemGroup {
        val newItemGroup = ItemGroup()
        newItemGroup.brightness = 50
        newItemGroup.temperature = 50
        newItemGroup.color = (50 shl 24) or (0 shl 16) or (0 shl 8) or 0
        newItemGroup.checked = true
        newItemGroup.enableCheck = false
        newItemGroup.gpName = showCheckListData!![i].name
        newItemGroup.groupAddress = showCheckListData!![i].meshAddr

        return newItemGroup
    }

    private fun saveScene() {
        if (checked())
            saveAndFinish()
    }

    private fun saveAndFinish() {
        when {
            isReconfig -> updateOldScene()
            else -> saveNewScene()
        }
        setResult(Constant.RESULT_OK)
    }


    @SuppressLint("CheckResult")
    private fun saveNewScene() {
        val name = edit_name.text.toString()
        var sceneIcon: String = "icon_out"
        try {
            sceneIcon = OtherUtils.getResourceName(resId!!, this@NewSceneSetAct).split("/")[1]
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        dbScene = DbScene()
        dbScene?.id = getSceneId()
        dbScene?.name = name
        dbScene?.imgName = sceneIcon
        dbScene?.belongRegionId = SharedPreferencesUtils.getCurrentUseRegionId()

        val belongSceneId = dbScene?.id!!
        DBUtils.deleteSceneActionsList(DBUtils.getActionsBySceneId(belongSceneId))
        val actionsList = mutableListOf<DbSceneActions>()
        for (i in showGroupList!!.indices) {
            var sceneActions = DbSceneActions()
            val item = showGroupList[i]
            sceneActions = when {
                OtherUtils.isCurtain(DBUtils.getGroupByMeshAddr(item.groupAddress)) -> {
                    setSceneAc(sceneActions, belongSceneId, item, 0x10)
                }
                OtherUtils.isConnector(DBUtils.getGroupByMeshAddr(item.groupAddress)) -> {
                    setSceneAc(sceneActions, belongSceneId, item, 0x05)
                }
                OtherUtils.isRGBGroup(DBUtils.getGroupByMeshAddr(item.groupAddress)) -> {
                    setSceneAc(sceneActions, belongSceneId, item, 0x06, i)
                }
                else -> {
                    setSceneAc(sceneActions, belongSceneId, item, 0x04)
                }
            }
            DBUtils.saveSceneActions(sceneActions)
            actionsList.add(sceneActions)
        }
        if (Constant.IS_ROUTE_MODE)
            routerAddScene(name, sceneIcon, actionsList)
        else {
            showLoadingDialog(getString(R.string.saving))
            GlobalScope.launch {
                delay(100)
                addScene(belongSceneId)
            }
        }
    }

    private fun afterSaveScene() {
        dbScene?.let {
            DBUtils.saveScene(dbScene!!, false)
        }
        SyncDataPutOrGetUtils.syncPutDataStart(this, syncCallbackGet)
        hideLoadingDialog()
        finish()
    }

    @SuppressLint("CheckResult")
    private fun routerAddScene(name: String, sceneIcon: String, actionsList: MutableList<DbSceneActions>) {
        RouterModel.routeAddScene(SceneAddBodyBean(name, sceneIcon, actionsList, "addScene"))?.subscribe({
            when (it.errorCode) {
                NetworkStatusCode.OK -> {
                    LogUtils.v("zcl-----------收到创建场景请求-------$it")
                    showLoadingDialog(getString(R.string.saving))
                    startAddSceneTimeOut(it.t)
                }
                //该账号该区域下没有路由，无法操作 ROUTER_NO_EXITE= 90004
                // 以下路由没有上线，无法删除场景  ROUTER_ALL_OFFLINE= 90005
                NetworkStatusCode.CURRENT_GP_NOT_EXITE -> ToastUtils.showShort(getString(R.string.gp_not_exit))
                NetworkStatusCode.ROUTER_NO_EXITE -> ToastUtils.showShort(getString(R.string.region_no_router))
                NetworkStatusCode.ROUTER_ALL_OFFLINE -> ToastUtils.showShort(getString(R.string.router_offline))
                else -> ToastUtils.showShort(it.message)
            }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    private fun startAddSceneTimeOut(it: RouterTimeoutBean?) {
        disposableRouteTimer?.dispose()
        disposableRouteTimer = io.reactivex.Observable.timer((it?.timeout ?: 0).toLong() + 2, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    hideLoadingDialog()
                    ToastUtils.showShort(getString(R.string.add_scene_fail))
                }
    }

    private fun setSceneAc(sceneActions: DbSceneActions, belongSceneId: Long, item: ItemGroup, deviceType: Int, position: Int = 1000000): DbSceneActions {
        sceneActions.isOn = item.isOn
        sceneActions.deviceType = deviceType
        sceneActions.belongSceneId = belongSceneId
        sceneActions.groupAddr = item.groupAddress

        if (1 == item.rgbType && deviceType == 0x06) {//是彩灯并且是渐变模式
            if (position != 1000000) {
                val itemGroup = showGroupList[position]
                sceneActions.rgbType = itemGroup.rgbType
                sceneActions.gradientType = itemGroup.gradientType
                sceneActions.gradientId = itemGroup.gradientId
                sceneActions.gradientSpeed = itemGroup.gradientSpeed
                sceneActions.gradientName = itemGroup.gradientName
            }
        } else {
            sceneActions.setIsEnableBright(item.isEnableBright)
            sceneActions.setIsEnableWhiteBright(item.isEnableWhiteBright)
            sceneActions.brightness = item.brightness
            sceneActions.colorTemperature = item.temperature
            sceneActions.setColor(item.color)
            LogUtils.v("zcl--白色-${(item!!.color and 0xff000000.toInt()) shr 24}--亮度${item.brightness}---------色温${item.temperature}----")
        }
        return sceneActions
    }

    @Throws(InterruptedException::class)
    private fun addScene(id: Long) {
        val opcode = Opcode.SCENE_ADD_OR_DEL
        val list = DBUtils.getActionsBySceneId(id)
        var params: ByteArray
        var count = 0
        do {
            count++
            with(list) { shuffle() }
            for (i in list.indices) {
                if (i > 0)
                    Thread.sleep(300)
                var temperature: Byte = when {
                    list[i].getIsEnableWhiteBright() -> list[i].colorTemperature.toByte()
                    else -> 0
                }

                if (temperature > 99)
                    temperature = 99

                var light: Byte = when {
                    list[i].isOn && list[i].getIsEnableBright() -> list[i].brightness.toByte()
                    else -> 0
                }

                if (light > 99)
                    light = 99
                val meshAddress = TelinkApplication.getInstance().connectDevice?.meshAddress ?: 0
                val mesH = (meshAddress shr 8) and 0xff //相同为1 不同为0
                val mesL = meshAddress and 0xff
                val color = list[i].getColor()
                var red = color and 0xff0000 shr 16
                var green = color and 0x00ff00 shr 8
                var blue = color and 0x0000ff
                var w = color shr 24
                if (red==0&&green==0&&blue==0){
                    red = 255
                    green =255
                    blue =255
                }
                var type = list[i].deviceType
                params = when (type) {
                    SMART_CURTAIN, SMART_RELAY -> {
                        when {
                            list[i].isOn -> byteArrayOf(0x01, id.toByte(), light, red.toByte(), green.toByte(), blue.toByte(), temperature, w.toByte(), 0x01) //接收器开是1
                            else -> byteArrayOf(0x01, id.toByte(), light, red.toByte(), green.toByte(), blue.toByte(), temperature, w.toByte(), 0x02) //接收器关是2
                        }
                    }
                    LIGHT_RGB -> {
                        if (list[i].rgbType == 0)//rgbType 类型 0:颜色模式 1：渐变模式   gradientType 渐变类型 1：自定义渐变  2：内置渐变
                            byteArrayOf(0x01, id.toByte(), light, red.toByte(), green.toByte(), blue.toByte(), list[i].brightness.toByte(), temperature)
                        else//11:新增场景1添加2删除  12:场景id 13:渐变id  14:渐变速度 15:直连灯低八位 16:高八 17:无 18:渐变类型 1 自定义的 2系统的
                            byteArrayOf(0x03, id.toByte(), list[i].gradientId.toByte(), list[i].gradientSpeed.toByte(), mesL.toByte(), mesH.toByte(), 0, list[i].gradientType.toByte())
                    }
                    else -> byteArrayOf(0x01, id.toByte(), light, red.toByte(), green.toByte(), blue.toByte(), temperature, w.toByte())
                }
                //dest address
                LogUtils.v("zcl--color$color-red$red----green$green----------blue$blue-")
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, list[i].groupAddr, params)
            }
        } while (count < 3)
        afterSaveScene()
    }

    private fun getSceneId(): Long {
        val list = DBUtils.sceneList
        val idList = java.util.ArrayList<Int>()
        for (i in list.indices)
            idList.add(list[i].id!!.toInt())

        var id = 0
        loop@ for (i in 1..100) {
            when {
                !idList.contains(i) -> {
                    id = i
                    break@loop
                }
                else -> continue@loop
            }
        }

        if (list.size == 0)
            id = 1

        return java.lang.Long.valueOf(id.toLong())
    }

    private fun updateOldScene() {
        val name = edit_name.text.toString()
        val itemGroups = showGroupList
        val nameList = java.util.ArrayList<Int>()
        dbScene?.name = name
        dbScene?.imgName = OtherUtils.getResourceName(resId!!, this@NewSceneSetAct).split("/")[1]
        val belongSceneId = dbScene?.id!!

        if (!Constant.IS_ROUTE_MODE)
            showLoadingDialog(getString(R.string.saving))

            DBUtils.deleteSceneActionsList(DBUtils.getActionsBySceneId(dbScene?.id!!))
        val actionsList = mutableListOf<DbSceneActions>()
        for (i in itemGroups.indices) {
            var sceneActions = DbSceneActions()
            val groupByMeshAddr = DBUtils.getGroupByMeshAddr(itemGroups[i].groupAddress)
            sceneActions = when {
                OtherUtils.isCurtain(groupByMeshAddr) -> {
                    setSceneAc(sceneActions, belongSceneId, itemGroups[i], 0x10)
                }
                OtherUtils.isConnector(groupByMeshAddr) -> {
                    setSceneAc(sceneActions, belongSceneId, itemGroups[i], 0x05)
                }
                OtherUtils.isRGBGroup(groupByMeshAddr) -> setSceneAc(sceneActions, belongSceneId, itemGroups[i], 0x06, i)
                else -> setSceneAc(sceneActions, belongSceneId, itemGroups[i], 0x04)
            }
            actionsList.add(sceneActions)
            DBUtils.saveSceneActions(sceneActions)
            nameList.add(itemGroups[i].groupAddress)
        }
        isChange = compareList(nameList, groupMeshAddrArrayList)

        when {
            Constant.IS_ROUTE_MODE -> routerUpdateScene(belongSceneId, actionsList)
            else -> {
                DBUtils.updateScene(dbScene!!)
                Thread.sleep(100)
                updateSceneSendCommend(belongSceneId)
                finish()
            }
        }
//this@NewSceneSetAct.runOnUiThread {  hideLoadingDialog() }
    }

    @SuppressLint("CheckResult")
    private fun routerUpdateScene(belongSceneId: Long, actionsList: MutableList<DbSceneActions>) {
        LogUtils.v("zcl--------更新路由场景参数---belongSceneId---$belongSceneId----$actionsList")
        RouterModel.routeUpdateScene(belongSceneId, actionsList, "updateScene")?.subscribe({
            LogUtils.v("zcl-----------更新路由场景成功-------$it")
            when (it.errorCode) {
                NetworkStatusCode.OK -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    startAddSceneTimeOut(it.t)
                }//更新超时
                //该账号该区域下没有路由，无法操作 ROUTER_NO_EXITE= 90004
                // 以下路由没有上线，无法删除场景  ROUTER_ALL_OFFLINE= 90005
                NetworkStatusCode.ROUTER_DEL_SCENE_NOT_EXITE -> {//该场景不存在，刷新场景数据
                    SyncDataPutOrGetUtils.syncGetDataStart(DBUtils.lastUser!!, syncCallbackGet)
                    initScene()
                }
                NetworkStatusCode.ROUTER_NO_EXITE -> ToastUtils.showShort(getString(R.string.region_no_router))
                NetworkStatusCode.ROUTER_ALL_OFFLINE -> ToastUtils.showShort(getString(R.string.router_offline))
                else -> ToastUtils.showShort(it.message)
            }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    @Throws(InterruptedException::class)
    private fun updateSceneSendCommend(id: Long) {
        deleteSceneSendCommend(id)
        val opcode = Opcode.SCENE_ADD_OR_DEL
        //val list = DBUtils.getActionsBySceneId(id)
        val list = DBUtils.getActionsBySceneId(id)
        var params: ByteArray
        for (i in list.indices) {
            Thread.sleep(100)
            var temperature: Byte
            temperature = when {
                list[i].getIsEnableWhiteBright() -> list[i].colorTemperature.toByte()
                else -> 0
            }
            if (temperature > 99)
                temperature = 99

            var light: Byte = when {
                list[i].isOn && list[i].getIsEnableBright() -> list[i].brightness.toByte()
                else -> 0
            }

            if (light > 99)
                light = 99

            val color = list[i].getColor()
            var red = color and 0xff0000 shr 16
            var green = color and 0x00ff00 shr 8
            var blue = color and 0x0000ff
            if (red==0&&green==0&&blue==0){
                red = 255
                green =255
                blue =255
            }
            var w = color shr 24
            val connectDevice = TelinkApplication.getInstance().connectDevice
            connectDevice?.let {
                val meshAddress = connectDevice.meshAddress
                val mesH = (meshAddress shr 8) and 0xff //相同为1 不同为0
                val mesL = meshAddress and 0xff
                var type = list[i].deviceType
                params = when (type) {
                    SMART_CURTAIN, SMART_RELAY -> when {
                        list[i].isOn -> byteArrayOf(0x01, id.toByte(), light, red.toByte(), green.toByte(), blue.toByte(), temperature, w.toByte(), 0x01)  //窗帘开是1
                        else -> byteArrayOf(0x01, id.toByte(), light, red.toByte(), green.toByte(), blue.toByte(), temperature, w.toByte(), 0x02)  //窗帘关是2
                    }
//                    LIGHT_RGB -> if (list[i].rgbType == 0)//rgbType 类型 0:颜色模式 1：渐变模式   gradientType 渐变类型 1：自定义渐变  2：内置渐变
//                        byteArrayOf(0x01, id.toByte(), light, red.toByte(), green.toByte(), blue.toByte(), light, temperature)
//                    else//11:新增场景1添 加2删除  12:场景id 13:渐变id  14:渐变速度 15:直连灯低八位 16:高八 17:无 18:渐变类型 1 自定义的 2系统的
//                        byteArrayOf(0x01, id.toByte(), list[i].gradientId.toByte(), list[i].gradientSpeed.toByte(), mesL.toByte(), mesH.toByte(), 0, list[i].gradientType.toByte())
                    LIGHT_RGB -> {
                        if (list[i].rgbType == 0)//rgbType 类型 0:颜色模式 1：渐变模式   gradientType 渐变类型 1：自定义渐变  2：内置渐变
                            byteArrayOf(0x01, id.toByte(), light, red.toByte(), green.toByte(), blue.toByte(), list[i].brightness.toByte(), temperature)
                        else//11:新增场景1添加2删除  12:场景id 13:渐变id  14:渐变速度 15:直连灯低八位 16:高八 17:无 18:渐变类型 1 自定义的 2系统的
                            byteArrayOf(0x03, id.toByte(), list[i].gradientId.toByte(), list[i].gradientSpeed.toByte(), mesL.toByte(), mesH.toByte(), 0, list[i].gradientType.toByte())
                    }
                    else -> byteArrayOf(0x01, id.toByte(), light, red.toByte(), green.toByte(), blue.toByte(), temperature, w.toByte())
                }
                LogUtils.v("zcl--color$color-red$red----green$green----------blue$blue-")
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, list[i].groupAddr, params)
            }
        }
    }

    private fun deleteSceneSendCommend(id: Long) {
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
        return when (actionsList.size) {
            actionsList1.size -> !actionsList1.containsAll(actionsList)
            else -> true
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
                    newItemGroup.color = 0
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
                when {
                    currentPageIsEdit && !isToolbar -> showExitSaveDialog()
                    else -> finish()
                }
            } else showExitSaveDialog()
        return super.onKeyDown(keyCode, event)
    }

    override fun tzRouterAddScene(routerScene: RouteSceneBean?) {
        if (routerScene?.ser_id == "addScene") {
            hideLoadingDialog()
            LogUtils.v("zcl-----------收到路由添加通知-------$routerScene")
            when {
                routerScene?.finish && routerScene?.status == 0 -> {//-1 全部失败 1 部分成功
                    ToastUtils.showShort(getString(R.string.config_success))
                    disposableRouteTimer?.dispose()
                    hideLoadingDialog()
                    afterSaveScene()
                    getNewScenes()
                }
                else -> ToastUtils.showShort(routerScene?.msg)
            }
        }
    }

    override fun tzRouteUpdateScene(cmdBodyBean: CmdBodyBean) {
        if (cmdBodyBean?.ser_id == "updateScene") {
            hideLoadingDialog()
            LogUtils.v("zcl-----------收到路由添加通知-------$cmdBodyBean")
            when {
                cmdBodyBean?.finish && cmdBodyBean?.status == 0 -> {//-1 全部失败 1 部分成功
                    ToastUtils.showShort(getString(R.string.config_success))
                    disposableRouteTimer?.dispose()
                    hideLoadingDialog()
                    getNewScenes()
                }
                else -> ToastUtils.showShort(getString(R.string.update_scene_fail))
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun getNewScenes() {
        NetworkFactory.getApi()
                .getSceneList(DBUtils.lastUser?.token)
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())?.subscribe({
                    DBUtils.deleteAllScene()
                    for (item in it) {
                        DBUtils.saveScene(item, true)
                        DBUtils.deleteSceneActionsList(DBUtils.getActionsBySceneId(item?.id!!))
                        for (i in item.actions.indices)
                            DBUtils.saveSceneActions(item.actions[i], item.id)
                        LogUtils.v("zcl--------获取服务器场景---$item------${DBUtils.getActionsBySceneId(dbScene!!.id)}-")
                    }
                    finish()
                }, {
                    if (!TextUtils.isEmpty(it.message))
                        ToastUtils.showShort(it.message)
                    finish()
                })
    }

    override fun tzRouterOpenOrClose(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl------收到路由开关灯通知------------$cmdBean")
        hideLoadingDialog()
        disposableRouteTimer?.dispose()
        if (cmdBean.ser_id == "openOrCloseGp") {
            when (cmdBean.status) {
                0 -> {
                    this.showGroupList[currentPosition].isOn = isOpen//更新状态更新ui
                }
                else -> ToastUtils.showShort(getString(R.string.open_light_faile))//恢复原本状态
            }
            sceneGroupAdapter?.notifyItemChanged(currentPosition)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(sceneGroupAdapter?.stompRecevice)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
}
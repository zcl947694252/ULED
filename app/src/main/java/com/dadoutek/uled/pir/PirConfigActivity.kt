package com.dadoutek.uled.pir

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.text.InputType
import android.text.TextUtils
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.PopupWindow
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.intf.OtaPrepareListner
import com.dadoutek.uled.light.NightLightGroupRecycleViewAdapter
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DBUtils.saveSensor
import com.dadoutek.uled.model.DbModel.DbScene
import com.dadoutek.uled.model.DbModel.DbSensor
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.othersview.InstructionsForUsActivity
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.othersview.SelectDeviceTypeActivity
import com.dadoutek.uled.switches.ChooseGroupOrSceneActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.*
import com.dadoutek.uled.util.StringUtils.*
import com.telink.bluetooth.light.DeviceInfo
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_pir_new.*
import kotlinx.android.synthetic.main.template_radiogroup.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.singleLine
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


/**
 * 创建者     ZCL
 * 创建时间   2020/5/18 14:38
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class PirConfigActivity : TelinkBaseActivity(), View.OnClickListener {
    private var disposableReset: Disposable? = null
    private var currentSensor: DbSensor? = null
    private var fiVersion: MenuItem? = null
    private var version: String = ""
    private var timeDispsable: Disposable? = null
    private var connectDisposable: Disposable? = null
    private var timeUnitType: Int = 0// 1 代表分 0代表秒
    private var triggerAfterShow: Int = 0//0 开 1关 2自定义
    private var triggerKey: Int = 0//0全天    1白天   2夜晚
    private var customBrightnessNum: Int = 0
    private var currentScene: DbScene? = null
    private val REQUEST_CODE_CHOOSE: Int = 1000
    private var isReConfirm: Boolean = false
    private var mDeviceInfo: DeviceInfo? = null
    private var popupWindow: PopupWindow? = null
    private var popAdapter: PopupWindowAdapter? = null
    private var isGroupMode = true
    private var showGroupList: MutableList<ItemGroup> = mutableListOf()
    private var showBottomList: MutableList<ItemGroup> = mutableListOf()
    private var bottomGvAdapter: NightLightGroupRecycleViewAdapter = NightLightGroupRecycleViewAdapter(R.layout.template_batch_small_item, showBottomList)

    //private var bottomGvAdapter: NightLightGroupRecycleViewAdapter = NightLightGroupRecycleViewAdapter(R.layout.activity_night_light_groups_item, showBottomList)
    private val popDataList = mutableListOf<ItemCheckBean>()
    private val listTriggerTimes = mutableListOf<ItemCheckBean>()
    private val listTriggerAfterShow = mutableListOf<ItemCheckBean>()
    private val listTimeUnit = mutableListOf<ItemCheckBean>()
    private val listSelectTimes = mutableListOf<ItemCheckBean>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pir_new)
        initView()
        initData()
        initListener()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        DBUtils.lastUser?.let {
            if (it.id.toString() == it.last_authorizer_user_id) {
                menuInflater.inflate(R.menu.menu_rgb_light_setting, menu)
                menu?.findItem(R.id.toolbar_f_rename)?.isVisible = isReConfirm
                menu?.findItem(R.id.toolbar_f_ota)?.isVisible = isReConfirm
                menu?.findItem(R.id.toolbar_f_delete)?.isVisible = isReConfirm
                fiVersion = menu?.findItem(R.id.toolbar_f_version)
                if (TextUtils.isEmpty(version))
                    version = getString(R.string.number_no)
                fiVersion?.title = version
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    private fun initListener() {
        toolbar.setOnMenuItemClickListener(menuItemClickListener)
        top_rg_ly.setOnCheckedChangeListener { _, checkedId ->
            changeGroupType(checkedId == R.id.color_mode_rb)
            when (checkedId) {
                R.id.color_mode_rb -> {
                    isGroupMode = true
                    changeGroupType(true)
                }
                R.id.gradient_mode_rb -> {
                    isGroupMode = false
                    changeGroupType(false)
                }
            }
        }
        pir_config_see_help.setOnClickListener(this)
        pir_config_trigger_conditions.setOnClickListener(this)
        pir_config_trigger_after.setOnClickListener(this)
        pir_config_time_type.setOnClickListener(this)
        //pir_config_overtime_down_time.setOnClickListener(this)
        pir_config_choose_scene.setOnClickListener(this)
        pir_config_choose_group.setOnClickListener(this)
        pir_config_btn.setOnClickListener(this)
        bottomGvAdapter.onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
            if (view.id == R.id.template_device_batch_selected) adapter.remove(position)
        }
    }

    private fun changeGroupType(b: Boolean) {
        showBottomList.clear()
        if (b) {
            pir_config_choose_group_ly.visibility = View.VISIBLE
            pir_config_choose_scene_ly.visibility = View.GONE
            pir_config_trigger_after_ly.visibility = View.VISIBLE
            pir_config_trigger_after_view.visibility = View.VISIBLE
            pir_config_select_title.text = getString(R.string.selected_group)
            showBottomList.addAll(showGroupList)
        } else {
            pir_config_choose_group_ly.visibility = View.GONE
            pir_config_choose_scene_ly.visibility = View.VISIBLE
            pir_config_trigger_after_ly.visibility = View.GONE
            pir_config_trigger_after_view.visibility = View.GONE
            pir_config_select_title.text = getString(R.string.choosed_scene)
            setItemScene()
        }
        bottomGvAdapter.notifyDataSetChanged()
    }

    private fun initData() {
        mDeviceInfo = intent.getParcelableExtra("deviceInfo")
        version = intent.getStringExtra("version")
        pir_confir_tvPSVersion.text = version
        isReConfirm = mDeviceInfo?.isConfirm == 1//等于1代表是重新配置
        if (isReConfirm){
            currentSensor = DBUtils.getSensorByMeshAddr(mDeviceInfo!!.meshAddress)
            toolbarTv.text = currentSensor?.name
        }
        color_mode_rb.isChecked = true

        getTriggerTimeProide()
        getTriggerAfterShow()
        getTimeUnite()
        getSelectTimes()
    }

    private fun configReturn() {
        if (!isReConfirm)
            AlertDialog.Builder(this)
                    .setMessage(getString(R.string.config_return))
                    .setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
                        dialog.dismiss()
                        startActivity(Intent(this@PirConfigActivity, SelectDeviceTypeActivity::class.java))
                        finish()
                    }
                    .setNegativeButton(getString(R.string.btn_cancel)) { dialog, _ -> dialog.dismiss() }.show()
        else
            finish()
    }

    private fun getSelectTimes() {
        listSelectTimes.add(ItemCheckBean(getString(R.string.ten_second), true))
        listSelectTimes.add(ItemCheckBean(getString(R.string.twenty_second), false))
        listSelectTimes.add(ItemCheckBean(getString(R.string.thirty_second), false))
        listSelectTimes.add(ItemCheckBean(getString(R.string.forty_second), false))
        listSelectTimes.add(ItemCheckBean(getString(R.string.fifty_second), false))
        listSelectTimes.add(ItemCheckBean(getString(R.string.one_minute), false))
        listSelectTimes.add(ItemCheckBean(getString(R.string.two_minute), false))
        listSelectTimes.add(ItemCheckBean(getString(R.string.three_minute), false))
        listSelectTimes.add(ItemCheckBean(getString(R.string.four_minute), false))
        listSelectTimes.add(ItemCheckBean(getString(R.string.five_minute), false))
    }

    private fun getTimeUnite() {//1 代表分 0代表秒
        listTimeUnit.add(ItemCheckBean(getString(R.string.second), false))
        listTimeUnit.add(ItemCheckBean(getString(R.string.minute), true))
    }

    private fun getTriggerAfterShow() { //0 开 1关 2自定义
        listTriggerAfterShow.add(ItemCheckBean(getString(R.string.light_on), true))
        listTriggerAfterShow.add(ItemCheckBean(getString(R.string.light_off), false))
        listTriggerAfterShow.add(ItemCheckBean(getString(R.string.custom_brightness), false))
    }

    private fun getTriggerTimeProide() {
        listTriggerTimes.add(ItemCheckBean(getString(R.string.all_day), false))
        listTriggerTimes.add(ItemCheckBean(getString(R.string.day_time), false))
        listTriggerTimes.add(ItemCheckBean(getString(R.string.night), false))
    }

    private fun initView() {
        toolbarTv.text = getString(R.string.human_body)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener { configReturn() }
        val moreIcon = ContextCompat.getDrawable(toolbar.context, R.drawable.abc_ic_menu_overflow_material)
        if (moreIcon != null) {
            moreIcon.setColorFilter(ContextCompat.getColor(toolbar.context, R.color.black), PorterDuff.Mode.SRC_ATOP)
            toolbar.overflowIcon = moreIcon
        }

        color_mode_rb.text = getString(R.string.group_mode)
        gradient_mode_rb.text = getString(R.string.scene_mode)
        color_mode_rb.isChecked = true
        pir_config_overtime_tv.requestFocus()
        pir_config_overtime_tv.isFocusable = true
        pir_config_overtime_tv.isFocusableInTouchMode = true
        makePopView()
        changeGroupType(true)
        makeGrideView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CHOOSE && resultCode == Activity.RESULT_OK) {
            showBottomList.clear()
            if (isGroupMode) {
                showGroupList.clear()
                val arrayList = data?.getParcelableArrayListExtra<Parcelable>("data") as ArrayList<CheckItemBean>
                arrayList.forEach {
                    val gp = DBUtils.getGroupByID(it.id)
                    gp?.let { itGp ->
                        val newItemGroup = ItemGroup()
                        newItemGroup.brightness = 50
                        newItemGroup.temperature = 50
                        newItemGroup.color = R.color.white
                        newItemGroup.checked = true
                        newItemGroup.enableCheck = true
                        newItemGroup.gpName = itGp.name
                        newItemGroup.groupAddress = itGp.meshAddr
                        showGroupList.add(newItemGroup)
                    }
                }
                showBottomList.addAll(showGroupList)
            } else {
                currentScene = data?.getParcelableExtra(Constant.EIGHT_SWITCH_TYPE) as DbScene
                setItemScene()
            }
            bottomGvAdapter.notifyDataSetChanged()
        }
    }

    private fun setItemScene() {
        val newItemGroup = ItemGroup()
        newItemGroup.brightness = 50
        newItemGroup.temperature = 50
        newItemGroup.color = R.color.white
        newItemGroup.checked = true
        newItemGroup.enableCheck = true
        newItemGroup.gpName = currentScene?.name
        newItemGroup.sceneId = currentScene?.id ?: 0
        newItemGroup.icon = if (TextUtils.isEmpty(currentScene?.imgName)) R.drawable.icon_1 else OtherUtils.getResourceId(currentScene?.imgName, this)
        if (currentScene != null)
            showBottomList.add(newItemGroup)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.pir_config_trigger_conditions -> {//触发条件
                popDataList.clear()
                popDataList.addAll(listTriggerTimes)
                popAdapter?.notifyDataSetChanged()
                popAdapter?.setOnItemClickListener { _, _, position ->
                    triggerKey = position
                    changeTriggerTimeChecked(position, 0)
                    pir_config_triggering_conditions_text.text = popDataList[position].title
                    popupWindow?.dismiss()
                }
                popupWindow?.showAsDropDown(pir_config_trigger_conditions)
            }

            R.id.pir_config_trigger_after -> {//触发后
                popDataList.clear()
                popDataList.addAll(listTriggerAfterShow)
                popAdapter?.notifyDataSetChanged()
                popAdapter?.setOnItemClickListener { _, _, position ->
                    triggerAfterShow = position//0 开 1关 2自定义
                    popupWindow?.dismiss()
                    when (position) {
                        0, 1 -> {
                            trigger_time_text.text = popDataList[position].title
                            customBrightnessNum = 0
                            changeTriggerTimeChecked(position, 1)
                        }
                        2 -> setBrightness()
                    }
                }
                popupWindow?.showAsDropDown(pir_config_trigger_after)
            }

            R.id.pir_config_time_type -> {
                popDataList.clear()
                popDataList.addAll(listTimeUnit)
                popAdapter?.notifyDataSetChanged()
                popAdapter?.setOnItemClickListener { _, _, position ->
                    popupWindow?.dismiss()
                    pir_config_time_type_text.text = popDataList[position].title
                    timeUnitType = position
                }
                popupWindow?.showAsDropDown(pir_config_time_type)
            }

            R.id.pir_config_choose_group -> {
                val intent = Intent(this@PirConfigActivity, ChooseMoreGroupOrSceneActivity::class.java)
                intent.putExtra(Constant.EIGHT_SWITCH_TYPE, 0)
                startActivityForResult(intent, REQUEST_CODE_CHOOSE)
            }
            R.id.pir_config_choose_scene -> {
                val intent = Intent(this@PirConfigActivity, ChooseGroupOrSceneActivity::class.java)
                val bundle = Bundle()
                bundle.putInt(Constant.EIGHT_SWITCH_TYPE, 1)//传入0代表是群组
                bundle.putInt(Constant.DEVICE_TYPE, Constant.DEVICE_TYPE_LIGHT.toInt())
                intent.putExtras(bundle)
                startActivityForResult(intent, REQUEST_CODE_CHOOSE)
            }
            R.id.pir_config_see_help -> {
                var intent = Intent(this@PirConfigActivity, InstructionsForUsActivity::class.java)
                startActivity(intent)
            }

            R.id.pir_config_btn -> {
                configDevice()
            }
        }
    }

    private fun configDevice() {
        var time = pir_config_overtime_tv.text.toString()
        when {
            TextUtils.isEmpty(time) -> {
                TmtUtils.midToast(this, getString(R.string.timeout_period_is_empty))
                return
            }
            timeUnitType == 0 && time.toInt() < 10 -> {//1 代表分0代表秒
                TmtUtils.midToast(this, getString(R.string.timeout_time_less_ten))
                return
            }
            timeUnitType == 0 && time.toInt() > 255 -> {
                TmtUtils.midToast(this, getString(R.string.timeout_255))
                return
            }
            timeUnitType == 1 && time.toInt() < 1 -> {
                TmtUtils.midToast(this, getString(R.string.timeout_1m))
                return
            }
            timeUnitType == 1 && time.toInt() > 255 -> {
                TmtUtils.midToast(this, getString(R.string.timeout_255_big))
                return
            }
            isGroupMode && showGroupList.size == 0 -> {
                TmtUtils.midToast(this, getString(R.string.config_night_light_select_group))
                return
            }
            !isGroupMode && currentScene == null -> {
                TmtUtils.midToast(this, getString(R.string.please_select_scene))
                return
            }
            else -> {//符合所有条件
                val device = TelinkLightApplication.getApp().connectDevice
                if (device != null && device.macAddress == mDeviceInfo?.macAddress) {
                    GlobalScope.launch {
                        setLoadingVisbiltyOrGone(View.VISIBLE, this@PirConfigActivity.getString(R.string.configuring_sensor))
                        sendCommandOpcode(time.toInt())
                        delay(300)
                        //if (!isConfirm)//不是冲洗创建 更新mesh
                        mDeviceInfo?.meshAddress = MeshAddressGenerator().meshAddress.get()
                        Commander.updateMeshName(newMeshAddr = mDeviceInfo!!.meshAddress,
                                successCallback = {
                                    setLoadingVisbiltyOrGone()
                                    GlobalScope.launch {
                                        delay(timeMillis = 500)
                                        saveSensor()
                                    }
                                },
                                failedCallback = {
                                    snackbar(sensor_root, getString(R.string.pace_fail))
                                    setLoadingVisbiltyOrGone()
                                    TelinkLightService.Instance()?.idleMode(true)
                                })

                    }
                } else {
                    ToastUtils.showLong(getString(R.string.connect_fail))
                    autoConnect()
                    timeDispsable = Observable.timer(10000, TimeUnit.MILLISECONDS).subscribe {
                        runOnUiThread {
                            hideLoadingDialog()
                            ToastUtils.showShort(getString(R.string.connect_fail))
                        }
                    }
                }
            }
        }
    }

    private fun configureComplete() {
        allDispoables()
        TelinkLightService.Instance()?.idleMode(true)
        if (ActivityUtils.isActivityExistsInStack(MainActivity::class.java))
            ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
        else {
            finish()
        }
    }

    private fun allDispoables() {
        connectDisposable?.dispose()
        timeDispsable?.dispose()
        disposableReset?.dispose()
    }

    private fun saveSensor() {
        var dbSensor = DbSensor()
        if (isReConfirm) {
            dbSensor.index = mDeviceInfo!!.id.toInt()
            if ("none" != mDeviceInfo!!.id)
                dbSensor.id = mDeviceInfo!!.id.toLong()
        } else {//如果不是重新配置就保存进服务器
            saveSensor(dbSensor, isReConfirm)
            dbSensor.index = dbSensor.id.toInt()//拿到新的服务器id
        }

        dbSensor.controlGroupAddr = getControlGroup()
        dbSensor.macAddr = mDeviceInfo!!.macAddress
        if (TextUtils.isEmpty(version))
            version = mDeviceInfo!!.firmwareRevision
        dbSensor.version = version
        dbSensor.productUUID = mDeviceInfo!!.productUUID
        dbSensor.meshAddr = mDeviceInfo!!.meshAddress
        dbSensor.name = getString(R.string.sensor) + dbSensor.meshAddr

        saveSensor(dbSensor, isReConfirm)//保存进服务器

        dbSensor = DBUtils.getSensorByID(dbSensor.id)!!
        DBUtils.recordingChange(dbSensor.id, DaoSessionInstance.getInstance().dbSensorDao.tablename, Constant.DB_ADD)

        if (!isReConfirm)
            showRenameDialog(dbSensor)
        else
            configureComplete()
    }

    private val menuItemClickListener = Toolbar.OnMenuItemClickListener { item ->
        when (item?.itemId) {
            R.id.toolbar_f_rename -> showRenameDialog(currentSensor!!)
            R.id.toolbar_f_ota -> goOta()
            R.id.toolbar_f_delete -> deleteDevice()
        }
        true
    }

    private fun deleteDevice() {
        //mesadddr发0就是代表只发送给直连灯也就是当前连接灯 也可以使用当前灯的mesAdd 如果使用mesadd 有几个pir就恢复几个
        AlertDialog.Builder(Objects.requireNonNull<AppCompatActivity>(this)).setMessage(R.string.delete_switch_confirm)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    if (currentSensor == null)
                        ToastUtils.showShort(getString(R.string.invalid_data))
                    else {
                        GlobalScope.launch (Dispatchers.Main){
                            showLoadingDialog(getString(R.string.please_wait))
                            disposableReset = Commander.resetDevice(currentSensor!!.meshAddr, true)
                                    .subscribe({
                                    deleteData()
                                    }, {
                                        GlobalScope.launch(Dispatchers.Main){
                                            /*    showDialogHardDelete?.dismiss()
                                              showDialogHardDelete = android.app.AlertDialog.Builder(this).setMessage(R.string.delete_device_hard_tip)
                                                      .setPositiveButton(android.R.string.ok) { _, _ ->
                                                          showLoadingDialog(getString(R.string.please_wait))
                                                          deleteData()
                                                      }
                                                      .setNegativeButton(R.string.btn_cancel, null)
                                                      .show()*/
                                            deleteData()
                                        } })
                        }
                    }
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
    }

     fun deleteData() {
        hideLoadingDialog()
        ToastUtils.showShort(getString(R.string.reset_factory_success))
        if (currentSensor!=null)
        DBUtils.deleteSensor(currentSensor!!)
        TelinkLightService.Instance()?.idleMode(true)
        configureComplete()
        finish()
    }

    private fun goOta() {
        var isBoolean: Boolean = SharedPreferencesHelper.getBoolean(TelinkLightApplication.getApp(), Constant.IS_DEVELOPER_MODE, false)
        if (isBoolean) {
            transformView()
        } else {
            if (OtaPrepareUtils.instance().checkSupportOta(version)!!) {
                OtaPrepareUtils.instance().gotoUpdateView(this@PirConfigActivity, version, object : OtaPrepareListner {
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
                })
            } else {
                ToastUtils.showLong(getString(R.string.version_disabled))
                hideLoadingDialog()
            }
        }

    }

    private fun transformView() {
        val intent = Intent(this@PirConfigActivity, OTAUpdateActivity::class.java)
        intent.putExtra(Constant.OTA_MAC, currentSensor?.macAddr)
        intent.putExtra(Constant.OTA_MES_Add, currentSensor?.meshAddr)
        intent.putExtra(Constant.OTA_VERSION, currentSensor?.version)
        intent.putExtra(Constant.OTA_TYPE, DeviceType.SENSOR)
        startActivity(intent)
        finish()
    }


    @SuppressLint("SetTextI18n")
    fun showRenameDialog(dbSensor: DbSensor) {
        initEditTextFilter(textGp)
        if (dbSensor.name != "" && dbSensor.name != null)
            textGp?.setText(dbSensor.name)
        else
            textGp?.setText(getSwitchPirDefaultName(dbSensor.productUUID, this) + "-" + DBUtils.getAllSwitch().size)
        textGp?.setSelection(textGp?.text.toString().length)

        renameConfirm?.setOnClickListener {  // 获取输入框的内容
            if (StringUtils.compileExChar(textGp?.text.toString().trim { it <= ' ' })) {
                ToastUtils.showLong(getString(R.string.rename_tip_check))
            } else {
                dbSensor.name = textGp?.text.toString().trim { it <= ' ' }
                saveSensor(dbSensor, false)
                if (!this.isFinishing)
                    renameDialog.dismiss()
            }
        }
        renameCancel?.setOnClickListener {
            if (!this.isFinishing)
                renameDialog?.dismiss()
        }
        renameDialog.setOnDismissListener {
            configureComplete()
        }

        if (!this.isFinishing) {
            renameDialog.dismiss()
            runOnUiThread {
                renameDialog.show()
            }
        }
    }

    private fun getControlGroup(): String? {
        var controlGroupListStr = ""
        for (j in showGroupList!!.indices) {
            if (j == 0) {
                controlGroupListStr = showGroupList!![j].groupAddress.toString()
            } else {
                controlGroupListStr += "," + showGroupList!![j].groupAddress.toString()
            }
        }
        return controlGroupListStr
    }

    fun autoConnect() {
        val deviceTypes = mutableListOf(DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD, DeviceType.LIGHT_RGB)
        val size = DBUtils.getAllCurtains().size + DBUtils.allLight.size + DBUtils.allRely.size
        if (size > 0) {
            ToastUtils.showLong(getString(R.string.connecting_tip))
            connectDisposable?.dispose()
            connectDisposable = connect(meshAddress = mDeviceInfo!!.meshAddress, fastestMode = true, retryTimes = 2)
                    ?.subscribe({
                        runOnUiThread {
                            hideLoadingDialog()
                            TmtUtils.midToast(this, getString(R.string.connect_success))
                        }
                        LogUtils.d("connect success")
                    }, {
                        runOnUiThread {
                            hideLoadingDialog()
                            TmtUtils.midToast(this, getString(R.string.connect_fail))
                        }
                        LogUtils.d("connect failed")
                    })
        }
    }

    private fun sendCommandOpcode(durationTime: Int) {
        if (isGroupMode) {
            // 11固定1 12-13保留 14 持续时间 15最终亮度(自定义亮度)
            // 16触发照度(条件 全天) 17触发设置 最低位，0是开，1是关，次低位 1是分钟，0是秒钟
            // 例如，配置是触发开灯、延时时间是秒钟，则17位发送1。如果配置是触发关灯、延时时间是秒钟，则17位发送0x02
            // 触发功能选择功能
            var triggerSet = if (triggerAfterShow == 2) 0 else (timeUnitType shl 1) or triggerAfterShow
            val paramsSetSHow = byteArrayOf(1, 0, 0, durationTime.toByte(), customBrightnessNum.toByte(),
                    triggerKey.toByte(), triggerSet.toByte(), 0)
            TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.CONFIG_LIGHT_LIGHT, mDeviceInfo!!.meshAddress, paramsSetSHow)

            Thread.sleep(200)

            //组地址选择功能
            val paramsSetGroup = byteArrayOf(0x24, 2, 0, 0, 0, 0, 0, 0, 0)//1关闭 最多支持七个
            for (i in 0 until showGroupList.size) {
                val lowAdd = showGroupList[i].groupAddress and 0xff
                paramsSetGroup[i + 2] = lowAdd.toByte()
            }
            TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.CONFIG_LIGHT_LIGHT, mDeviceInfo!!.meshAddress, paramsSetGroup)

        } else {
            //11位固定为3。 12位的时间是纯数字时间，如果定时60分钟，就发送60 13场景ID 14 持续时间单位1是分钟，0是秒 15触发条件
            //4、 触发照度：0是任意，1是白天，2是黑夜
            val paramsSetSHow = byteArrayOf(3, durationTime.toByte(), currentScene!!.id.toByte(), timeUnitType.toByte(), triggerKey.toByte(), 0, 0, 0)
            TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.CONFIG_LIGHT_LIGHT, mDeviceInfo!!.meshAddress, paramsSetSHow)
        }
    }


    private fun setLoadingVisbiltyOrGone(visiablity: Int = View.GONE, text: String = "") {
        GlobalScope.launch(Dispatchers.Main) {
            pir_confir_progress_ly.visibility = visiablity
            pir_config_human_progress_tv.text = text
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setBrightness() {
        val textGp = EditText(this)
        textGp.inputType = InputType.TYPE_CLASS_NUMBER
        textGp.singleLine = true
        initEditTextFilter(textGp)
        AlertDialog.Builder(this)
                .setTitle(R.string.target_brightness)
                .setView(textGp)
                .setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
                    var brightness = textGp.text.toString()
                    if (brightness == "")
                        brightness = "0"
                    var brin = brightness.toInt()
                    if (brin == 0) {
                        ToastUtils.showShort(getString(R.string.brightness_cannot))
                        return@setPositiveButton
                    }
                    if (brin > 100) {
                        ToastUtils.showShort(getString(R.string.brightness_cannot_be_greater_than))
                        return@setPositiveButton
                    }
                    val time = textGp.text.toString()
                    customBrightnessNum = time.toInt()
                    trigger_time_text.text = "$time%"
                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, _ -> dialog.dismiss() }.show()

        GlobalScope.launch {
            delay(200)
            val inputManager = textGp.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.showSoftInput(textGp, 0)
        }
    }

    private fun changeTriggerTimeChecked(i: Int, isTriggerrTime: Int) {
        when (isTriggerrTime) {//0 触发条件
            0 -> {
                listTriggerTimes[0].checked = i == 0
                listTriggerTimes[1].checked = i == 1
                listTriggerTimes[2].checked = i == 2
            }
            1 -> {//触发后显示方式
                listTriggerAfterShow[0].checked = i == 0
                listTriggerAfterShow[1].checked = i == 1
                listTriggerAfterShow[2].checked = i == 2
            }
            2 -> {//超时单位
                listTriggerAfterShow[0].checked = i == 0
                listTriggerAfterShow[1].checked = i == 1
                listTriggerAfterShow[2].checked = i == 2
            }
        }
    }

    private fun makeGrideView() {
        pir_config_recyclerGroup.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        pir_config_recyclerGroup.layoutManager = GridLayoutManager(this, 2)
        bottomGvAdapter?.bindToRecyclerView(pir_config_recyclerGroup)
    }

    private fun makePopView() {
        var views = LayoutInflater.from(this).inflate(R.layout.pop_recycleview, null)
        popupWindow = PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        popupWindow?.contentView = views
        var recycleView = views.findViewById<RecyclerView>(R.id.pop_recycle)
        popAdapter = PopupWindowAdapter(R.layout.pop_item_sigle, popDataList)
        recycleView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recycleView.adapter = popAdapter
        popAdapter?.bindToRecyclerView(recycleView)
        popupWindow?.isFocusable = true
    }

    override fun onDestroy() {
        super.onDestroy()
        allDispoables()
        TelinkLightService.Instance()?.idleMode(true)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        configReturn()
    }
    /*  pir_config_switch.setOnCheckedChangeListener { _, checkedId ->
       val byteArrayOf = if (checkedId == R.id.pir_config_switch_open)
           byteArrayOf(2, 0, 0, 0, 0, 0, 0, 0)//0打开
       else
           byteArrayOf(2, 1, 0, 0, 0, 0, 0, 0)//1关闭
       TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.CONFIG_LIGHT_LIGHT,
               mDeviceInfo!!.meshAddress, byteArrayOf)
   }*/
}
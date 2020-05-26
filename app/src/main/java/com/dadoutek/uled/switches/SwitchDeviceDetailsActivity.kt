package com.dadoutek.uled.switches

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.util.DiffUtil
import android.support.v7.widget.*
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.group.InstallDeviceListAdapter
import com.dadoutek.uled.intf.OtaPrepareListner
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.light.DeviceScanningNewActivity
import com.dadoutek.uled.model.Constants
import com.dadoutek.uled.model.Constants.*
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbSwitch
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.InstallDeviceModel
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.pir.ScanningSensorActivity
import com.dadoutek.uled.scene.NewSceneSetAct
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.OtaPrepareUtils
import com.dadoutek.uled.util.OtherUtils
import com.dadoutek.uled.util.StringUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import com.telink.TelinkApplication
import com.telink.bluetooth.light.DeviceInfo
import com.telink.util.MeshUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_switch_device_details.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.startActivity
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 开关列表
 */
class SwitchDeviceDetailsActivity : TelinkBaseActivity(), View.OnClickListener {
    private var downloadDispoable: Disposable? = null
    private var mConnectDeviceDisposable: Disposable? = null
    private val SCENE_MAX_COUNT = 100
    private var isclickOTA: Boolean = false
    private var isOta: Boolean = false
    private var last_start_time = 0
    private var debounce_time = 1000
    private lateinit var switchData: MutableList<DbSwitch>
    private var mScanTimeoutDisposal: Disposable? = null
    private var adapter: SwitchDeviceDetailsAdapter? = null
    private var currentLight: DbSwitch? = null
    private var positionCurrent: Int = 0
    private var mConnectDevice: DeviceInfo? = null
    private var acitivityIsAlive = true
    private var mApplication: TelinkLightApplication? = null
    private var install_device: TextView? = null
    private var create_group: TextView? = null
    private var create_scene: TextView? = null
    private lateinit var stepOneText: TextView
    private lateinit var stepTwoText: TextView
    private lateinit var stepThreeText: TextView
    private lateinit var switchStepOne: TextView
    private lateinit var switchStepTwo: TextView
    private lateinit var swicthStepThree: TextView
    private lateinit var stepThreeTextSmall: TextView

    private var isRgbClick = false
    private var installId = 0
    private var currentSwitch: DbSwitch? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_switch_device_details)
        this.mApplication = this.application as TelinkLightApplication
    }

    override fun onResume() {
        super.onResume()
        initData()
        initView()
    }

    private fun initData() {
        switchData = DBUtils.getAllSwitch()

        setScanningMode(true)
        SyncDataPutOrGetUtils.syncPutDataStart(TelinkLightApplication.getApp(), object : SyncCallback {
            override fun start() {
                LogUtils.v("zcl____同步开关________start")
            }

            override fun complete() {
                LogUtils.v("zcl____同步开关________complete")
            }

            override fun error(msg: String?) {
                LogUtils.v("zcl____同步开关________error")
            }
        })
        if (switchData.size > 0) {
            no_device_relativeLayout.visibility = View.GONE
            recycleView.visibility = View.VISIBLE
            toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.GONE
        } else {
            no_device_relativeLayout.visibility = View.VISIBLE
            recycleView.visibility = View.GONE
            toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
            toolbar!!.findViewById<ImageView>(R.id.img_function1).setOnClickListener {
                val lastUser = DBUtils.lastUser
                lastUser?.let {
                    if (it.id.toString() != it.last_authorizer_user_id)
                        ToastUtils.showLong(getString(R.string.author_region_warm))
                    else {
                        if (dialog?.visibility == View.GONE) {
                            showPopupMenu()
                        } else {
                            hidePopupMenu()
                        }
                    }
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.add_device_btn -> {
                addDevice()
            }
        }
    }

    private fun onLogin(bestRSSIDevice: DeviceInfo) {
        mScanTimeoutDisposal?.dispose()
        hideLoadingDialog()
        if (TelinkApplication.getInstance().connectDevice != null)
            Commander.getDeviceVersion(bestRSSIDevice!!.meshAddress)
                    .subscribe(
                            { version ->
                                if (version != null && version != "") {
                                    when (bestRSSIDevice?.productUUID) {
                                        DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2 -> {
                                            startActivity<ConfigNormalSwitchActivity>("deviceInfo" to bestRSSIDevice!!, "group" to "true", "switch" to currentSwitch, "version" to version)
                                            finish()
                                        }
                                        DeviceType.DOUBLE_SWITCH -> {
                                            startActivity<DoubleTouchSwitchActivity>("deviceInfo" to bestRSSIDevice!!, "group" to "true", "switch" to currentSwitch, "version" to version)
                                            finish()
                                        }
                                        DeviceType.SCENE_SWITCH -> {
                                            if (version.contains(DeviceType.EIGHT_SWITCH_VERSION))
                                                startActivity<ConfigEightSwitchActivity>("deviceInfo" to bestRSSIDevice!!, "group" to "true",  "switch" to currentSwitch,"version" to version)
                                            else
                                                startActivity<ConfigSceneSwitchActivity>("deviceInfo" to bestRSSIDevice!!, "group" to "true", "switch" to currentSwitch, "version" to version)
                                            finish()
                                        }
                                        DeviceType.EIGHT_SWITCH -> {
                                            startActivity<ConfigEightSwitchActivity>("deviceInfo" to bestRSSIDevice!!, "group" to "true",  "switch" to currentSwitch,"version" to version)
                                            finish()
                                        }
                                        DeviceType.SMART_CURTAIN_SWITCH -> {
                                            startActivity<ConfigCurtainSwitchActivity>("deviceInfo" to bestRSSIDevice!!, "group" to "true", "switch" to currentSwitch, "version" to version)
                                            finish()
                                        }
                                    }
                                } else {
                                    ToastUtils.showLong(getString(R.string.get_version_fail))
                                    initData()
                                }
                            },
                            {
                                showToast(getString(R.string.get_version_fail))
                            }
                    )
    }

    private fun addDevice() {
        val lastUser = DBUtils.lastUser
        lastUser?.let {
            if (it.id.toString() != it.last_authorizer_user_id)
                ToastUtils.showLong(getString(R.string.author_region_warm))
            else {
                goSearchSwitch()
            }
        }
    }

    private fun initView() {
        mConnectDevice = TelinkLightApplication.getApp().connectDevice
        recycleView!!.layoutManager = GridLayoutManager(this, 3)
        recycleView!!.itemAnimator = DefaultItemAnimator()
        adapter = SwitchDeviceDetailsAdapter(R.layout.device_detail_adapter, switchData,this)
        adapter!!.bindToRecyclerView(recycleView)

        adapter!!.onItemChildClickListener = onItemChildClickListener
        for (i in switchData.indices)
            switchData[i].updateIcon()

        install_device = findViewById(R.id.install_device)
        create_group = findViewById(R.id.create_group)
        create_scene = findViewById(R.id.create_scene)
        install_device?.setOnClickListener(onClick)
        create_group?.setOnClickListener(onClick)
        create_scene?.setOnClickListener(onClick)

        add_device_btn.setOnClickListener(this)
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbar.title = getString(R.string.switch_name) + " (" + switchData!!.size + ")"
    }

    private fun showPopupMenu() {
        dialog?.visibility = View.VISIBLE
    }

    private fun hidePopupMenu() {
        dialog?.visibility = View.GONE
    }

    var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { _, view, position ->
        currentLight = switchData?.get(position)
        positionCurrent = position
        if (view.id == R.id.tv_setting) {
            val lastUser = DBUtils.lastUser
            lastUser?.let {
                if (it.id.toString() != it.last_authorizer_user_id)
                    ToastUtils.showLong(getString(R.string.author_region_warm))
                else {
                    showPopupWindow(view, position)
                }
            }
        }
    }

    private fun showPopupWindow(view: View?, position: Int) {
        val views = LayoutInflater.from(this).inflate(R.layout.popwindown_switch, null)
        val set = view!!.findViewById<ImageView>(R.id.tv_setting)
        val popupWindow = PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        popupWindow.contentView = views
        popupWindow.isFocusable = true
        popupWindow.showAsDropDown(set)
        currentSwitch = switchData[position]

        val reConfig = views.findViewById<TextView>(R.id.switch_group)
        val ota = views.findViewById<TextView>(R.id.ota)
        val delete = views.findViewById<TextView>(R.id.deleteBtn)
        val rename = views.findViewById<TextView>(R.id.rename)
        delete.text = getString(R.string.delete)

        rename.setOnClickListener {
            isOta = false
            isclickOTA = false
            popupWindow.dismiss()
            val textGp = EditText(this)
            StringUtils.initEditTextFilter(textGp)
            textGp.setText(currentSwitch?.name)
            textGp.setSelection(textGp.text.toString().length)
            android.app.AlertDialog.Builder(this@SwitchDeviceDetailsActivity)
                    .setTitle(R.string.rename)
                    .setView(textGp)
                    .setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
                        // 获取输入框的内容
                        if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                            ToastUtils.showLong(getString(R.string.rename_tip_check))
                        } else {
                            currentSwitch?.name = textGp.text.toString().trim { it <= ' ' }
                            DBUtils.updateSwicth(currentSwitch!!)
                            adapter!!.notifyDataSetChanged()
                            dialog.dismiss()
                        }
                    }
                    .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()
        }

        reConfig.setOnClickListener {
            popupWindow.dismiss()
            isOta = false
            isclickOTA = false

            if (currentSwitch != null) {
                TelinkLightService.Instance()?.idleMode(true)
                showLoadingDialog(getString(R.string.connecting))
                connect(macAddress = currentLight?.macAddr, retryTimes = 1)
                        ?.subscribe(
                                {
                                    onLogin(it)//判断进入那个开关设置界面
                                    LogUtils.d("login success")
                                },
                                {
                                    hideLoadingDialog()
                                    LogUtils.d(it)
                                }
                        )
            }else {
                LogUtils.d("currentSwitch = $currentSwitch")
            }
        }

        ota.setOnClickListener {
            popupWindow.dismiss()
            isOta = true
            isclickOTA = true
            if (currentSwitch != null) {
                TelinkLightService.Instance()?.idleMode(true)
                showLoadingDialog(getString(R.string.connecting))
                connect(macAddress = currentLight?.macAddr, retryTimes = 3)
                        ?.subscribe(
                                {
                                    getDeviceVersion(it)
                                    LogUtils.d("login success")
                                },
                                {
                                    hideLoadingDialog()
                                    LogUtils.d(it)
                                }
                        )
            } else {
                LogUtils.d("currentSwitch = $currentSwitch")
            }
        }
        delete.setOnClickListener {
            //恢复出厂设置
            isOta = false
            isclickOTA = true
            popupWindow.dismiss()
            var deleteSwitch = switchData[position]
            AlertDialog.Builder(Objects.requireNonNull<AppCompatActivity>(this)).setMessage(R.string.delete_switch_confirm)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        DBUtils.deleteSwitch(deleteSwitch)
                        notifyData()



                        Toast.makeText(this@SwitchDeviceDetailsActivity, R.string.delete_switch_success, Toast.LENGTH_LONG).show()
                        if (TelinkLightApplication.getApp().mesh.removeDeviceByMeshAddress(deleteSwitch.meshAddr)) {
                            TelinkLightApplication.getApp().mesh.saveOrUpdate(this)
                        }
                        if (mConnectDevice != null) {
                            if (deleteSwitch.meshAddr == mConnectDevice?.meshAddress) {
                                GlobalScope.launch {
                                    delay(2500)//踢灯后没有回调 状态刷新不及时 延时2秒获取最新连接状态
                                    if (this@SwitchDeviceDetailsActivity == null ||
                                            this@SwitchDeviceDetailsActivity.isDestroyed ||
                                            this@SwitchDeviceDetailsActivity.isFinishing || !acitivityIsAlive) {
                                    } else
                                        connect()
                                }
                            }
                        }
                    }
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show()
        }
    }

    @SuppressLint("CheckResult")
    private fun getDeviceVersion(deviceInfo: DeviceInfo) {
        if (TelinkApplication.getInstance().connectDevice != null) {
             downloadDispoable = Commander.getDeviceVersion(deviceInfo.meshAddress)
                    .subscribe({ s ->
                                if (OtaPrepareUtils.instance().checkSupportOta(s)!!) {
                                    currentLight!!.version = s
                                    isDirectConnectDevice()
                                } else {
                                    ToastUtils.showLong(getString(R.string.version_disabled))
                                }
                                hideLoadingDialog()
                            },
                            {
                                hideLoadingDialog()
                                ToastUtils.showLong(getString(R.string.get_version_fail))
                            }
                    )

        }
        isclickOTA = false
    }

    private fun isDirectConnectDevice() {
                var isBoolean: Boolean = SharedPreferencesHelper.getBoolean(TelinkLightApplication.getApp(), IS_DEVELOPER_MODE, false)
        if (TelinkLightApplication.getApp().connectDevice != null && TelinkLightApplication.getApp().connectDevice.macAddress == currentLight?.macAddr) {
                if (isBoolean) {
                    transformView()
                } else {
                    OtaPrepareUtils.instance().gotoUpdateView(this@SwitchDeviceDetailsActivity, currentLight?.version, otaPrepareListner)
                }
        } else {
            showLoadingDialog(getString(R.string.please_wait))
            TelinkLightService.Instance()?.idleMode(true)
            mConnectDeviceDisposable = Observable.timer(800, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap {
                        connect(currentLight!!.meshAddr,macAddress = currentLight!!.macAddr)
                    }
                    ?.subscribe(
                            {
                                onLogin(it)
                                hideLoadingDialog()
                                if (isBoolean) {
                                    transformView()
                                } else {
                                    OtaPrepareUtils.instance().gotoUpdateView(this@SwitchDeviceDetailsActivity, currentLight!!.version, otaPrepareListner)
                                }
                            }
                            ,
                            {
                                hideLoadingDialog()
                                runOnUiThread { ToastUtils.showLong(R.string.connect_fail2) }
                                LogUtils.d(it)
                            })
        }
    }

    private fun transformView() {
        mConnectDeviceDisposable?.dispose()
        val intent = Intent(this@SwitchDeviceDetailsActivity, OTAUpdateActivity::class.java)
        intent.putExtra(OTA_MAC, currentLight?.macAddr)
        intent.putExtra(OTA_MES_Add, currentLight?.meshAddr)
        intent.putExtra(OTA_VERSION, currentLight?.version)
        intent.putExtra(OTA_TYPE, DeviceType.NORMAL_SWITCH)
        val timeMillis = System.currentTimeMillis()
        if (last_start_time == 0 || timeMillis - last_start_time >= debounce_time)
            startActivity(intent)
    }

    private var otaPrepareListner: OtaPrepareListner = object : OtaPrepareListner {

        override fun downLoadFileStart() {}

        override fun startGetVersion() {}

        override fun getVersionSuccess(s: String) {}

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



    fun notifyData() {
        val mOldDatas: MutableList<DbSwitch>? = switchData
        val mNewDatas: MutableList<DbSwitch>? = getNewData()
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return mOldDatas?.get(oldItemPosition)?.id?.equals(mNewDatas?.get
                (newItemPosition)?.id) ?: false
            }

            override fun getOldListSize(): Int {
                return mOldDatas?.size ?: 0
            }

            override fun getNewListSize(): Int {
                return mNewDatas?.size ?: 0
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val beanOld = mOldDatas?.get(oldItemPosition)
                val beanNew = mNewDatas?.get(newItemPosition)
                return if (!beanOld?.name.equals(beanNew?.name)) {
                    return false//如果有内容不同，就返回false
                } else true
            }
        }, true)
        adapter?.let { diffResult.dispatchUpdatesTo(it) }
        switchData = mNewDatas!!
        toolbar.title = getString(R.string.switch_name) + " (" + switchData!!.size + ")"
        adapter!!.setNewData(switchData)
        initData()
        initView()
    }

    private fun getNewData(): MutableList<DbSwitch> {
        switchData = DBUtils.getAllSwitch()
        toolbar.title = (currentLight!!.name ?: "")
        return switchData
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadDispoable?.dispose()
        mConnectDeviceDisposable?.toString()
        acitivityIsAlive = false
        TelinkLightService.Instance()?.idleMode(true)
        //移除事件
    }

    private val onClick = View.OnClickListener {
        hidePopupMenu()
        when (it.id) {
            R.id.install_device -> {
                showInstallDeviceList()
            }
            R.id.create_group -> {
                dialog?.visibility = View.GONE
                if (TelinkLightApplication.getApp().connectDevice == null) {
                    ToastUtils.showLong(getString(R.string.device_not_connected))
                } else {
                    //addNewGroup()
                    popMain.showAtLocation(window.decorView, Gravity.CENTER,0,0)
                }
            }
            R.id.create_scene -> {
                dialog?.visibility = View.GONE
                val nowSize = DBUtils.sceneList.size
                if (TelinkLightApplication.getApp().connectDevice == null) {
                    ToastUtils.showLong(getString(R.string.device_not_connected))
                } else {
                    if (nowSize >= SCENE_MAX_COUNT) {
                        ToastUtils.showLong(R.string.scene_16_tip)
                    } else {
                        val intent = Intent(this, NewSceneSetAct::class.java)
                        intent.putExtra(Constants.IS_CHANGE_SCENE, false)
                        startActivityForResult(intent, CREATE_SCENE_REQUESTCODE)
                    }
                }
            }
        }
    }

    private fun showInstallDeviceList() {
        dialog.visibility = View.GONE
        showInstallDeviceList(isGuide, isRgbClick)
    }

    var installDialog: android.app.AlertDialog? = null
    var isGuide: Boolean = false
    var clickRgb: Boolean = false
    private fun showInstallDeviceList(isGuide: Boolean, clickRgb: Boolean) {
        this.clickRgb = clickRgb
        val view = View.inflate(this, R.layout.dialog_install_list, null)
        val close_install_list = view.findViewById<ImageView>(R.id.close_install_list)
        val install_device_recyclerView = view.findViewById<RecyclerView>(R.id.install_device_recyclerView)
        close_install_list.setOnClickListener { v -> installDialog?.dismiss() }

        val installList: ArrayList<InstallDeviceModel> = OtherUtils.getInstallDeviceList(this)

        val installDeviceListAdapter = InstallDeviceListAdapter(R.layout.item_install_device, installList)
        val layoutManager = LinearLayoutManager(this)
        install_device_recyclerView?.layoutManager = layoutManager
        install_device_recyclerView?.adapter = installDeviceListAdapter
        installDeviceListAdapter.bindToRecyclerView(install_device_recyclerView)
        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.divider)))
        //添加分割线
        install_device_recyclerView?.addItemDecoration(decoration)

        installDeviceListAdapter.onItemClickListener = onItemClickListenerInstallList

        installDialog = android.app.AlertDialog.Builder(this)
                .setView(view)
                .create()

        installDialog?.setOnShowListener {

        }

        if (isGuide) {
            installDialog?.setCancelable(false)
        }

        installDialog?.show()

        GlobalScope.launch {
            delay(100)
            GlobalScope.launch(Dispatchers.Main) {
            }
        }
    }

    val onItemClickListenerInstallList = BaseQuickAdapter.OnItemClickListener { _, _, position ->
        isGuide = false
        installDialog?.dismiss()
        when (position) {
            INSTALL_GATEWAY -> {
                installId = INSTALL_GATEWAY
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
            }
            INSTALL_NORMAL_LIGHT -> {
                installId = INSTALL_NORMAL_LIGHT
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this),position)
            }
            INSTALL_RGB_LIGHT -> {
                installId = INSTALL_RGB_LIGHT
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
            }
            INSTALL_CURTAIN -> {
                installId = INSTALL_CURTAIN
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
            }
            INSTALL_SWITCH -> {
                goSearchSwitch()
            }
            INSTALL_SENSOR -> {
                installId = INSTALL_SENSOR
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
            }
            INSTALL_CONNECTOR -> {
                installId = INSTALL_CONNECTOR
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
            }
        }
    }

    private fun goSearchSwitch() {
        installId = INSTALL_SWITCH
        showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), installId)

        stepOneText.visibility = View.GONE
        stepTwoText.visibility = View.GONE
        stepThreeText.visibility = View.GONE
        switchStepOne.visibility = View.VISIBLE
        switchStepTwo.visibility = View.VISIBLE
        swicthStepThree.visibility = View.VISIBLE
    }

    private fun showInstallDeviceDetail(describe: String, position: Int) {
        val view = View.inflate(this, R.layout.dialog_install_detail, null)
        val close_install_list = view.findViewById<ImageView>(R.id.close_install_list)
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        stepOneText = view.findViewById(R.id.step_one)
        stepTwoText = view.findViewById(R.id.step_two)
        stepThreeText = view.findViewById(R.id.step_three)
        stepThreeTextSmall = view.findViewById(R.id.step_three_small)
        switchStepOne = view.findViewById(R.id.switch_step_one)
        switchStepTwo = view.findViewById(R.id.switch_step_two)
        swicthStepThree = view.findViewById(R.id.switch_step_three)
        val install_tip_question = view.findViewById<TextView>(R.id.install_tip_question)
        val search_bar = view.findViewById<Button>(R.id.search_bar)
        close_install_list.setOnClickListener(dialogOnclick)
        btnBack.setOnClickListener(dialogOnclick)
        search_bar.setOnClickListener(dialogOnclick)

        val title = view.findViewById<TextView>(R.id.textView5)
        if (position==INSTALL_NORMAL_LIGHT){
            title.visibility =  View.GONE
            install_tip_question.visibility =  View.GONE
        }else{
            title.visibility =  View.VISIBLE
            install_tip_question.visibility =  View.VISIBLE
        }

        install_tip_question.text = describe
        install_tip_question.movementMethod = ScrollingMovementMethod.getInstance()
        installDialog = android.app.AlertDialog.Builder(this)
                .setView(view)
                .create()

        installDialog?.setOnShowListener {}
        installDialog?.show()
    }

    private val dialogOnclick = View.OnClickListener {
        var medressData = 0
        var allData = DBUtils.allLight
        var sizeData = DBUtils.allLight.size
        if (sizeData != 0) {
            var lightData = allData[sizeData - 1]
            medressData = lightData.meshAddr
        }

        when (it.id) {
            R.id.close_install_list -> {
                installDialog?.dismiss()
            }
            R.id.search_bar -> {
                when (installId) {
                    INSTALL_NORMAL_LIGHT -> {
                        if (medressData <= MeshUtils.DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constants.DEVICE_TYPE, DeviceType.LIGHT_NORMAL)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_RGB_LIGHT -> {
                        if (medressData <= MeshUtils.DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constants.DEVICE_TYPE, DeviceType.LIGHT_RGB)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_CURTAIN -> {
                        if (medressData <= MeshUtils.DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constants.DEVICE_TYPE, DeviceType.SMART_CURTAIN)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_SWITCH -> {
                        //intent = Intent(this, DeviceScanningNewActivity::class.java)
                        //intent.putExtra(Constant.DEVICE_TYPE, DeviceType.NORMAL_SWITCH)
                        //startActivityForResult(intent, 0)
                        startActivity(Intent(this, ScanningSwitchActivity::class.java))
                    }
                    INSTALL_SENSOR -> startActivity(Intent(this, ScanningSensorActivity::class.java))
                    INSTALL_CONNECTOR -> {
                        if (medressData <= MeshUtils.DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constants.DEVICE_TYPE, DeviceType.SMART_RELAY)       //connector也叫relay
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_GATEWAY -> {
                        if (medressData <= MeshUtils.DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constants.DEVICE_TYPE, DeviceType.GATE_WAY)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                }
                installDialog?.dismiss()
            }
            R.id.btnBack -> {
                installDialog?.dismiss()
                showInstallDeviceList(isGuide, clickRgb)
            }
        }
    }


    private fun addNewGroup() {
//        dialog?.visibility = View.GONE
        val textGp = EditText(this)
        StringUtils.initEditTextFilter(textGp)
        textGp.setText(DBUtils.getDefaultNewGroupName())
        //设置光标默认在最后
        textGp.setSelection(textGp.text.toString().length)
        android.app.AlertDialog.Builder(this)
                .setTitle(R.string.create_new_group)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(textGp)

                .setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
                    // 获取输入框的内容
                    if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                        ToastUtils.showLong(getString(R.string.rename_tip_check))
                    } else {
                        //往DB里添加组数据
                        DBUtils.addNewGroupWithType(textGp.text.toString().trim { it <= ' ' }, DEVICE_TYPE_DEFAULT_ALL)
                        dialog.dismiss()
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()
    }

    private val CREATE_SCENE_REQUESTCODE = 2
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREATE_SCENE_REQUESTCODE) {
//            callbackLinkMainActAndFragment?.changeToScene()
        }
    }
}

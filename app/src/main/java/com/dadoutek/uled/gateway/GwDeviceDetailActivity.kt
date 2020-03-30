package com.dadoutek.uled.gateway

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
import com.dadoutek.uled.communicate.Commander.getDeviceVersion
import com.dadoutek.uled.gateway.adapter.GwDeviceItemAdapter
import com.dadoutek.uled.gateway.bean.DbGateway
import com.dadoutek.uled.group.InstallDeviceListAdapter
import com.dadoutek.uled.light.DeviceScanningNewActivity
import com.dadoutek.uled.model.Constant.*
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.HttpModel.GwModel
import com.dadoutek.uled.model.InstallDeviceModel
import com.dadoutek.uled.network.GwGattBody
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.pir.ScanningSensorActivity
import com.dadoutek.uled.scene.NewSceneSetAct
import com.dadoutek.uled.switches.ScanningSwitchActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.OtherUtils
import com.dadoutek.uled.util.StringUtils
import com.telink.util.MeshUtils.DEVICE_ADDRESS_MAX
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.empty_view.*
import kotlinx.android.synthetic.main.template_device_detail_list.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.view.*
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 创建者     zcl
 * 创建时间   2020/3/13 16:27
 * 描述	     //从parmers内设置需要的类型从此处取出
 * {  List<Integer> targetDevices = mParams.getIntList(Parameters.PARAM_TARGET_DEVICE_TYPE);
 *如果有要连接的目标设备
 *if (targetDevices.size() > 0)
 *if (!targetDevices.contains(light.getProductUUID())) {    //如果目标设备list里不包含当前设备类型，就过滤掉，return false
 *return false;
 *}}
 *
 * 更新者    $author$
 * 更新时间   $Date$
 * 更新描述   ${TODO}$
 */
class GwDeviceDetailActivity : TelinkBaseActivity(), View.OnClickListener {
    private lateinit var popupWindow: PopupWindow
    private var disposableConnect: Disposable? = null
    private var disposable: Disposable? = null
    private var currentGw: DbGateway? = null
    private var type: Int? = null
    private val gateWayDataList: MutableList<DbGateway> = mutableListOf()
    private var adaper: GwDeviceItemAdapter? = GwDeviceItemAdapter(R.layout.device_detail_adapter, gateWayDataList, this)
    private var inflater: LayoutInflater? = null
    private var positionCurrent: Int = 0
    private var canBeRefresh = true
    private var acitivityIsAlive = true
    private var mConnectDisposal: Disposable? = null
    private var install_device: TextView? = null
    private var create_group: TextView? = null
    private var create_scene: TextView? = null
    private var isRgbClick = false
    private var installId = 0
    private lateinit var stepOneText: TextView
    private lateinit var stepTwoText: TextView
    private lateinit var stepThreeText: TextView
    private lateinit var switchStepOne: TextView
    private lateinit var switchStepTwo: TextView
    private lateinit var swicthStepThree: TextView
    private lateinit var stepThreeTextSmall: TextView
    private val SCENE_MAX_COUNT = 100

    override fun onCreate(savedInstanceState: Bundle?) {//其他界面添加扫描网关待做
        super.onCreate(savedInstanceState)
        setContentView(R.layout.template_device_detail_list)
        type = this.intent.getIntExtra(DEVICE_TYPE, 0)
        inflater = this.layoutInflater
        initView()
        initData()

    }

    override fun onResume() {
        super.onResume()
        inflater = this.layoutInflater
        initData()
    }

    private fun initView() {
        recycleView!!.layoutManager = GridLayoutManager(this, 3)
        recycleView!!.itemAnimator = DefaultItemAnimator()

        adaper!!.onItemChildClickListener = onItemChildClickListener
        adaper!!.bindToRecyclerView(recycleView)

        for (i in gateWayDataList.indices)
            gateWayDataList!![i].updateIcon()

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
        toolbar.title = getString(R.string.Gate_way) + " (" + gateWayDataList.size + ")"
    }

    private val onClick = View.OnClickListener {
        when (it.id) {
            R.id.install_device -> {
                showInstallDeviceList()
            }
            R.id.create_group -> {
                dialog_relay?.visibility = View.GONE
                if (TelinkLightApplication.getApp().connectDevice == null) {
                    ToastUtils.showLong(getString(R.string.device_not_connected))
                } else {
                    popMain.showAtLocation(window.decorView, Gravity.CENTER, 0, 0)
                }
            }
            R.id.create_scene -> {
                dialog_relay?.visibility = View.GONE
                val nowSize = DBUtils.sceneList.size
                if (TelinkLightApplication.getApp().connectDevice == null) {
                    ToastUtils.showLong(getString(R.string.device_not_connected))
                } else {
                    if (nowSize >= SCENE_MAX_COUNT) {
                        ToastUtils.showLong(R.string.scene_16_tip)
                    } else {
                        val intent = Intent(this, NewSceneSetAct::class.java)
                        intent.putExtra(IS_CHANGE_SCENE, false)
                        startActivity(intent)
                    }
                }
            }
        }
    }

    private fun showInstallDeviceList() {
        dialog_relay.visibility = View.GONE
        showInstallDeviceList(isGuide, isRgbClick)
    }

    var installDialog: android.app.AlertDialog? = null
    var isGuide: Boolean = false
    var clickRgb: Boolean = false
    private fun showInstallDeviceList(isGuide: Boolean, clickRgb: Boolean) {
        this.clickRgb = clickRgb
        val view = View.inflate(this, R.layout.dialog_install_list, null)
        val closeInstallList = view.findViewById<ImageView>(R.id.close_install_list)
        val installDeviceRecyclerview = view.findViewById<RecyclerView>(R.id.install_device_recyclerView)
        closeInstallList.setOnClickListener { v -> installDialog?.dismiss() }

        val installList: java.util.ArrayList<InstallDeviceModel> = OtherUtils.getInstallDeviceList(this)

        val installDeviceListAdapter = InstallDeviceListAdapter(R.layout.item_install_device, installList)
        val layoutManager = LinearLayoutManager(this)
        installDeviceRecyclerview?.layoutManager = layoutManager
        installDeviceRecyclerview?.adapter = installDeviceListAdapter
        installDeviceListAdapter.bindToRecyclerView(installDeviceRecyclerview)
        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.divider)))
        //添加分割线
        installDeviceRecyclerview?.addItemDecoration(decoration)
        installDeviceListAdapter.onItemClickListener = onItemClickListenerInstallList

        installDialog = android.app.AlertDialog.Builder(this).setView(view).create()

        if (isGuide)
            installDialog?.setCancelable(false)

        installDialog?.show()
    }

    private val onItemClickListenerInstallList = BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
        isGuide = false
        installDialog?.dismiss()
        when (position) {
            INSTALL_GATEWAY -> {
                installId = INSTALL_GATEWAY
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
            }
            INSTALL_NORMAL_LIGHT -> {
                installId = INSTALL_NORMAL_LIGHT
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
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
                installId = INSTALL_SWITCH
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
                stepOneText.visibility = View.GONE
                stepTwoText.visibility = View.GONE
                stepThreeText.visibility = View.GONE
                switchStepOne.visibility = View.VISIBLE
                switchStepTwo.visibility = View.VISIBLE
                swicthStepThree.visibility = View.VISIBLE
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

    private fun showInstallDeviceDetail(describe: String, position: Int) {
        val view = View.inflate(this, R.layout.dialog_install_detail, null)
        val closeInstallList = view.findViewById<ImageView>(R.id.close_install_list)
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        stepOneText = view.findViewById(R.id.step_one)
        stepTwoText = view.findViewById(R.id.step_two)
        stepThreeText = view.findViewById(R.id.step_three)
        stepThreeTextSmall = view.findViewById(R.id.step_three_small)
        switchStepOne = view.findViewById(R.id.switch_step_one)
        switchStepTwo = view.findViewById(R.id.switch_step_two)
        swicthStepThree = view.findViewById(R.id.switch_step_three)
        val title = view.findViewById<TextView>(R.id.textView5)
        val installTipQuestion = view.findViewById<TextView>(R.id.install_tip_question)
        val searchBar = view.findViewById<Button>(R.id.search_bar)
        closeInstallList.setOnClickListener(dialogOnclick)
        btnBack.setOnClickListener(dialogOnclick)
        searchBar.setOnClickListener(dialogOnclick)

        if (position == INSTALL_NORMAL_LIGHT) {
            title.visibility = View.GONE
            installTipQuestion.visibility = View.GONE
        } else {
            title.visibility = View.VISIBLE
            installTipQuestion.visibility = View.VISIBLE
        }
        if (position == INSTALL_SWITCH)
            stepThreeTextSmall.visibility = View.VISIBLE
        else
            stepThreeTextSmall.visibility = View.GONE



        installTipQuestion.text = describe
        installTipQuestion.movementMethod = ScrollingMovementMethod.getInstance()
        installDialog = android.app.AlertDialog.Builder(this)
                .setView(view)
                .create()
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
                        if (medressData <= DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(DEVICE_TYPE, DeviceType.LIGHT_NORMAL)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_RGB_LIGHT -> {
                        if (medressData <= DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(DEVICE_TYPE, DeviceType.LIGHT_RGB)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_CURTAIN -> {
                        if (medressData <= DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(DEVICE_TYPE, DeviceType.SMART_CURTAIN)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_SWITCH -> {
                        startActivity(Intent(this, ScanningSwitchActivity::class.java))
                    }
                    INSTALL_SENSOR -> startActivity(Intent(this, ScanningSensorActivity::class.java))
                    INSTALL_CONNECTOR -> {
                        if (medressData <= DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(DEVICE_TYPE, DeviceType.SMART_RELAY)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_GATEWAY -> {
                        if (medressData <= DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(DEVICE_TYPE, DeviceType.GATE_WAY)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                }
            }
            R.id.btnBack -> {
                installDialog?.dismiss()
                showInstallDeviceList(isGuide, clickRgb)
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.add_device_btn -> {
                val lastUser = DBUtils.lastUser
                lastUser?.let {
                    if (it.id.toString() != it.last_authorizer_user_id)
                        ToastUtils.showLong(getString(R.string.author_region_warm))
                    else {
                        addDevice()
                    }
                }
            }
        }
    }

    private fun addDevice() {//添加网关
        intent = Intent(this, DeviceScanningNewActivity::class.java)
        intent.putExtra(DEVICE_TYPE, DeviceType.GATE_WAY)
        startActivity(intent)
    }

    var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { _, view, position ->
        currentGw = gateWayDataList[position]
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
        /*      when {
                  view.id == R.id.tv_setting -> {
                      val lastUser = DBUtils.lastUser
                      lastUser?.let {
                          if (it.id.toString() != it.last_authorizer_user_id)
                              ToastUtils.showLong(getString(R.string.author_region_warm))
                          else {
                              if (TelinkLightApplication.getApp().connectDevice == null) {
                                  autoConnect()
                              } else {
                                  var intent = Intent(this@GwDeviceDetailActivity, GwEventListActivity::class.java)
                                  intent.putExtra("data", currentGw)
                                  startActivity(intent)
                              }
                          }
                      }
                  }
                  else -> ToastUtils.showLong(R.string.reconnecting)
              }*/
    }

    private fun showPopupWindow(view: View?, position: Int) {
        val views = LayoutInflater.from(this).inflate(R.layout.popwindown_switch, null)
        val set = view!!.findViewById<ImageView>(R.id.tv_setting)
        popupWindow = PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        popupWindow.contentView = views
        popupWindow.isFocusable = true
        popupWindow.showAsDropDown(set)
        currentGw = gateWayDataList[position]

        val reConfig = views.findViewById<TextView>(R.id.switch_group)
        val ota = views.findViewById<TextView>(R.id.ota)
        val delete = views.findViewById<TextView>(R.id.deleteBtn)
        val rename = views.findViewById<TextView>(R.id.rename)
        rename.text = getString(R.string.config_net)
        delete.text = getString(R.string.delete)

        rename.setOnClickListener {
            connectGw(1)//配置网络
        }

        reConfig.setOnClickListener {
            connectGw(0)//配置网关
        }

        ota.setOnClickListener {
            connectGw(2)
        }

        delete.setOnClickListener {
            //恢复出厂设置
            popupWindow.dismiss()
            val dbGateway = gateWayDataList[position]
            AlertDialog.Builder(Objects.requireNonNull<AppCompatActivity>(this)).setMessage(R.string.delete_switch_confirm)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        deleteGw(dbGateway)
                        if (TelinkLightApplication.getApp().mesh.removeDeviceByMeshAddress(dbGateway.meshAddr)) {
                            TelinkLightApplication.getApp().mesh.saveOrUpdate(this)
                        }
                    }
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show()
        }

    }

    private fun connectGw(configType: Int) {
        popupWindow.dismiss()
        if (currentGw != null) {
            TelinkLightService.Instance()?.idleMode(true)
            showLoadingDialog(getString(R.string.connecting))
            disposable?.dispose()
            disposable = Observable.timer(20, TimeUnit.SECONDS)
                    .subscribe {
                        hideLoadingDialog()
                        ToastUtils.showShort(getString(R.string.connect_fail2))
                    }
            disposableConnect?.dispose()
            disposableConnect = connect(macAddress = currentGw?.macAddr, retryTimes = 1)?.subscribe({
                if (configType == 2)
                    getDeviceVersion(currentGw!!.meshAddr)
                else
                    onLogin(configType)//判断进入那个开关设置界面
                LogUtils.d("login success")
            }, {
                hideLoadingDialog()
                LogUtils.d(it)
            })
        } else {
            LogUtils.d("currentGw = $currentGw")
        }
    }

    private fun deleteGw(dbGw: DbGateway) {
        DBUtils.deleteGateway(dbGw)
        val gattBody = GwGattBody()
        gattBody.idList = mutableListOf(dbGw.id.toInt())
        GwModel.deleteGwList(gattBody)?.subscribe(object : NetworkObserver<String?>() {
            override fun onNext(t: String) {
                LogUtils.v("zcl-----网关删除成功返回-------------$t")
                Toast.makeText(this@GwDeviceDetailActivity, R.string.delete_switch_success, Toast.LENGTH_LONG).show()
                notifyData()
            }

            override fun onError(e: Throwable) {
                super.onError(e)
                ToastUtils.showShort(e.message)
                LogUtils.v("zcl-----网关删除成功返回-------------${e.message}")
            }
        })
    }

    fun notifyData() {
        val mNewDatas: ArrayList<DbGateway> = getNewData()
        gateWayDataList.clear()
        gateWayDataList.addAll(mNewDatas)
        if (mNewDatas.size > 0) {
            recycleView.visibility = View.VISIBLE
            no_device_relativeLayout.visibility = View.GONE
        } else {
            recycleView.visibility = View.GONE
            no_device_relativeLayout.visibility = View.VISIBLE
        }
        adaper?.notifyDataSetChanged()
        toolbar.title = getString(R.string.Gate_way) + " (" + gateWayDataList.size + ")"
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return gateWayDataList[oldItemPosition].id?.equals(mNewDatas[newItemPosition].id)
                        ?: false
            }

            override fun getOldListSize(): Int {
                return gateWayDataList.size
            }

            override fun getNewListSize(): Int {
                return mNewDatas.size
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val beanOld = gateWayDataList[oldItemPosition]
                val beanNew = mNewDatas[newItemPosition]
                return if (beanOld.name != beanNew.name) {
                    return false//如果有内容不同，就返回false
                } else true
            }
        }, true)

    }

    private fun getNewData(): ArrayList<DbGateway> {
        val allGateWay = DBUtils.getAllGateWay()
        toolbar.title = (currentGw!!.name ?: "")
        return allGateWay
    }

    private fun onLogin(isConfigGw: Int) {
        hideLoadingDialog()
        val intent: Intent = if (isConfigGw == 0)
            Intent(this@GwDeviceDetailActivity, GwEventListActivity::class.java)
        else
            Intent(this@GwDeviceDetailActivity, GwLoginActivity::class.java)
        intent.putExtra("data", currentGw)
        startActivity(intent)
    }

    private fun initData() {
        setScanningMode(true)
        gateWayDataList.clear()
        val allDeviceData = DBUtils.getAllGateWay()
        if (allDeviceData.size > 0) {
            toolbar!!.tv_function1.visibility = View.VISIBLE
            recycleView.visibility = View.VISIBLE
            no_device_relativeLayout.visibility = View.GONE
            toolbar.findViewById<TextView>(R.id.tv_function1).visibility = View.GONE

            toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.VISIBLE
            toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.GONE

            gateWayDataList.addAll(allDeviceData)
            adaper?.notifyDataSetChanged()
        } else {
            recycleView.visibility = View.GONE
            no_device_relativeLayout.visibility = View.VISIBLE
            toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.GONE
            toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
            toolbar!!.findViewById<ImageView>(R.id.img_function1).setOnClickListener {
                val lastUser = DBUtils.lastUser
                lastUser?.let {
                    if (it.id.toString() != it.last_authorizer_user_id)
                        ToastUtils.showLong(getString(R.string.author_region_warm))
                    else {
                        if (dialog_relay?.visibility == View.GONE) {
                            showPopupMenu()
                        }
                    }
                }
            }
        }
        toolbar.title = getString(R.string.Gate_way) + " (" + gateWayDataList.size + ")"
    }

    override fun onPause() {
        super.onPause()
        disposableConnectTimer?.dispose()
    }

    private fun showPopupMenu() {
        dialog_relay?.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        mConnectDisposal?.dispose()
        canBeRefresh = false
        acitivityIsAlive = false
        installDialog?.dismiss()
    }

    fun autoConnect() {
        mConnectDisposal = connect()?.subscribe({ LogUtils.d(it) }, { LogUtils.d(it) })
    }

}

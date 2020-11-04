package com.dadoutek.uled.device

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.GridLayoutManager
import android.view.*
import android.widget.ImageView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.connector.ConnectorDeviceDetailActivity
import com.dadoutek.uled.curtains.CurtainsDeviceDetailsActivity
import com.dadoutek.uled.device.model.DeviceItem
import com.dadoutek.uled.gateway.GwDeviceDetailActivity
import com.dadoutek.uled.gateway.bean.GwStompBean
import com.dadoutek.uled.intf.CallbackLinkMainActAndFragment
import com.dadoutek.uled.light.DeviceDetailAct
import com.dadoutek.uled.model.Constants
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.othersview.BaseFragment
import com.dadoutek.uled.othersview.InstructionsForUsActivity
import com.dadoutek.uled.othersview.SelectDeviceTypeActivity
import com.dadoutek.uled.router.RouterDeviceDetailsActivity
import com.dadoutek.uled.scene.NewSceneSetAct
import com.dadoutek.uled.scene.SensorDeviceDetailsActivity
import com.dadoutek.uled.stomp.MqttBodyBean
import com.dadoutek.uled.switches.SwitchDeviceDetailsActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.GuideUtils
import kotlinx.android.synthetic.main.fragment_me.*
import kotlinx.android.synthetic.main.fragment_new_device.*
import kotlinx.android.synthetic.main.popwindow_install_deive_list.*
import kotlinx.android.synthetic.main.template_add_help.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

/**
 * 创建者     zcl
 * 创建时间   2019/7/27 11:31
 * 描述	      ${设备列表-灯暖灯全彩灯列表}$
 *
 * 更新者     $Author$
 * 更新时间   $Date$
 *
 */
class DeviceFragment : BaseFragment(), View.OnClickListener {
    private var emptyGroupView: View? = null
    private var viewContent: View? = null
    private var deviceTypeList: ArrayList<DeviceItem> = ArrayList()
    private var deviceAdapter: DeviceTypeRecycleViewAdapter = DeviceTypeRecycleViewAdapter(R.layout.template_device_type_item, deviceTypeList)
    private var isGuide = false
    private var isRgbClick = false
    private val SCENE_MAX_COUNT = 100

    val CREATE_SCENE_REQUESTCODE = 2
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return initLayout(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolBar()
        initData()
        initView()
        initListener()
    }

    private fun initListener() {
        main_go_help.setOnClickListener(this)
        main_add_device.setOnClickListener(this)
    }


    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser);
        if (userVisibleHint) {
            refreshView()
        }
    }

    override fun onResume() {
        super.onResume()
        initAdapterData()
    }

    private fun initToolBar() {
        toolbarTv!!.setText(R.string.device)

        toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.GONE
        toolbar!!.findViewById<ImageView>(R.id.img_function2).visibility = View.GONE
        toolbar!!.findViewById<ImageView>(R.id.img_function1).setOnClickListener {

            val lastUser = DBUtils.lastUser
            lastUser?.let {
                when {
                    it.id.toString() != it.last_authorizer_user_id -> ToastUtils.showLong(getString(R.string.author_region_warm))
                    else -> {
                        isGuide = false
                        when (dialog_pop?.visibility) {
                            View.GONE ->showPopupMenu()
                            else -> hidePopupMenu()
                        }
                    }
                }
            }
        }
    }




    private fun initView() {
        rvDevice?.layoutManager = GridLayoutManager(this.activity, 2)
        rvDevice?.itemAnimator = DefaultItemAnimator()
        emptyGroupView = LayoutInflater.from(context).inflate(R.layout.empty_box_view, null)
        deviceAdapter.onItemClickListener = onItemClickListener
        deviceAdapter.bindToRecyclerView(rvDevice)
        deviceAdapter.emptyView =emptyGroupView

        install_device?.setOnClickListener(onClick)
        create_group?.setOnClickListener(onClick)
        create_scene?.setOnClickListener(onClick)
    }


    /**
     * 刷新UI
     */
    fun refreshView() {
        when (TelinkLightApplication.getApp().connectDevice) {
            null -> bluetooth_image?.setImageResource(R.drawable.bluetooth_no)
            else -> bluetooth_image?.setImageResource(R.drawable.icon_bluetooth)
        }
        if (activity != null) {
            initAdapterData()
            deviceAdapter.notifyDataSetChanged()
        }
    }


    var onItemClickListener = BaseQuickAdapter.OnItemClickListener { _, _, position ->
        var intent = Intent()
        when (deviceTypeList[position].installType) {
            Constants.INSTALL_NORMAL_LIGHT -> {//跳转冷暖灯
                intent = Intent(activity, DeviceDetailAct::class.java)
                intent.putExtra(Constants.DEVICE_TYPE, Constants.INSTALL_NORMAL_LIGHT)
            }
            Constants.INSTALL_RGB_LIGHT -> {//跳转多彩灯
                intent = Intent(activity, DeviceDetailAct::class.java)
                intent.putExtra(Constants.DEVICE_TYPE, Constants.INSTALL_RGB_LIGHT)
            }
            Constants.INSTALL_SWITCH -> {//不存在分组
                intent = Intent(activity, SwitchDeviceDetailsActivity::class.java)
                intent.putExtra(Constants.DEVICE_TYPE, Constants.INSTALL_SWITCH)
            }
            Constants.INSTALL_SENSOR -> {//不存在分组
                intent = Intent(activity, SensorDeviceDetailsActivity::class.java)
                intent.putExtra(Constants.DEVICE_TYPE, Constants.INSTALL_SENSOR)
            }
            Constants.INSTALL_CURTAIN -> {//分组已修改 旧有联动逻辑存在
                intent = Intent(activity, CurtainsDeviceDetailsActivity::class.java)
                intent.putExtra(Constants.DEVICE_TYPE, Constants.INSTALL_CURTAIN)
            }
            Constants.INSTALL_CONNECTOR -> {//分组已更新  sandian
                intent = Intent(activity, ConnectorDeviceDetailActivity::class.java)
                intent.putExtra(Constants.DEVICE_TYPE, Constants.INSTALL_CONNECTOR)
            }
            Constants.INSTALL_GATEWAY -> {//不存在分组    sandian
                intent = Intent(activity, GwDeviceDetailActivity::class.java)
                intent.putExtra(Constants.DEVICE_TYPE, Constants.INSTALL_GATEWAY)
            }
            Constants.INSTALL_ROUTER -> {//不存在分组    sandian
                intent = Intent(activity, RouterDeviceDetailsActivity::class.java)
                intent.putExtra(Constants.DEVICE_TYPE, Constants.INSTALL_ROUTER)
            }
        }
        startActivityForResult(intent, Activity.RESULT_OK)
    }

    /**
     * 初始化RecyclerView的Adapter
     */
    private fun initAdapterData() {
        deviceTypeList.clear()
        isAddDevice(R.string.normal_light,DBUtils.getAllNormalLight().size,DeviceType.LIGHT_NORMAL    ,    Constants.INSTALL_NORMAL_LIGHT)
        isAddDevice(R.string.rgb_light,DBUtils.getAllRGBLight().size,DeviceType.LIGHT_RGB             , Constants.INSTALL_RGB_LIGHT)
        isAddDevice(R.string.switch_title,DBUtils.getAllSwitch().size,DeviceType.NORMAL_SWITCH         ,     Constants.INSTALL_SWITCH)
        isAddDevice(R.string.sensor,DBUtils.getAllSensor().size,DeviceType.SENSOR                     , Constants.INSTALL_SENSOR)
        isAddDevice(R.string.curtain,DBUtils.getAllCurtains().size,DeviceType.SMART_CURTAIN           , Constants.INSTALL_CURTAIN)
        isAddDevice(R.string.relay,DBUtils.getAllRelay().size,DeviceType.SMART_RELAY                  , Constants.INSTALL_CONNECTOR)
        isAddDevice(R.string.Gate_way,DBUtils.getAllGateWay().size,DeviceType.GATE_WAY                , Constants.INSTALL_GATEWAY)
        isAddDevice(R.string.router,DBUtils.getAllRouter().size,DeviceType.ROUTER                , Constants.INSTALL_ROUTER)

        deviceAdapter.notifyDataSetChanged()
    }

    private fun isAddDevice(strId: Int, size: Int, deviceType: Int, installType: Int) {
        if (size > 0)
            deviceTypeList.add(DeviceItem(getString(strId), size, deviceType,installType))
    }


    /**
     * 初始化所有数据
     */
    private fun initData() {
        initAdapterData()
    }


    /**
     * 初始化布局
     */
    private fun initLayout(inflater: LayoutInflater): View? {
        viewContent = inflater.inflate(R.layout.fragment_new_device, null)
        return viewContent
    }

    private val onClick = View.OnClickListener {
        hidePopupMenu()
        when (it.id) {
            R.id.install_device -> {
                showInstallDeviceList()
            }
            R.id.create_group -> {
                if (TelinkLightApplication.getApp().connectDevice == null) {
                    ToastUtils.showLong(activity!!.getString(R.string.device_not_connected))
                } else {
                    // addNewGroup()
                    popMain.showAtLocation(viewContent, Gravity.CENTER, 0, 0)
                }
            }
            R.id.create_scene -> {
                val nowSize = DBUtils.sceneList.size
                if (TelinkLightApplication.getApp().connectDevice == null) {
                    ToastUtils.showLong(activity!!.getString(R.string.device_not_connected))
                } else {
                    if (nowSize >= SCENE_MAX_COUNT) {
                        ToastUtils.showLong(R.string.scene_16_tip)
                    } else {
                        val intent = Intent(activity, NewSceneSetAct::class.java)
                        intent.putExtra(Constants.IS_CHANGE_SCENE, false)
                        startActivityForResult(intent, CREATE_SCENE_REQUESTCODE)
                    }
                }
            }
        }
    }

    fun myPopViewClickPosition(x: Float, y: Float) {
        if (x < dialog_pop?.left ?: 0 || y < dialog_pop?.top ?: 0 || y > dialog_pop?.bottom ?: 0) {
            if (dialog_pop?.visibility == View.VISIBLE) {
                Thread {
                    //避免点击过快点击到下层View
                    Thread.sleep(100)
                    GlobalScope.launch(Dispatchers.Main) {
                        hidePopupMenu()
                    }
                }.start()
            } else if (dialog_pop == null) {
                hidePopupMenu()
            }
        }
    }

    private fun showPopupMenu() {
        dialog_pop?.visibility = View.VISIBLE
    }

    private fun hidePopupMenu() {
        if (!isGuide || GuideUtils.getCurrentViewIsEnd(activity!!, GuideUtils.END_GROUPLIST_KEY, false)) {
            dialog_pop?.visibility = View.GONE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
//        refreshData()
        if (requestCode == CREATE_SCENE_REQUESTCODE) {
            callbackLinkMainActAndFragment?.changeToScene()
        }
    }

    var callbackLinkMainActAndFragment: CallbackLinkMainActAndFragment? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is CallbackLinkMainActAndFragment) {
            callbackLinkMainActAndFragment = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        callbackLinkMainActAndFragment = null
    }

    private fun showInstallDeviceList() {
        dialog_pop.visibility = View.GONE
        callbackLinkMainActAndFragment?.showDeviceListDialog(isGuide, isRgbClick)
    }

    override fun receviedGwCmd2500(gwStompBean: GwStompBean) {
        when (gwStompBean.ser_id.toInt()) {
            Constants.SER_ID_GROUP_ALLON -> {
                LogUtils.v("zcl-----------远程控制组全开成功-------")
                hideLoadingDialog()

            }
            Constants.SER_ID_GROUP_ALLOFF -> {
                LogUtils.v("zcl-----------远程控制组全关成功-------")
                hideLoadingDialog()
            }
        }
    }
    override fun receviedGwCmd2500M(gwStompBean: MqttBodyBean) {
        when (gwStompBean.ser_id.toInt()) {
            Constants.SER_ID_GROUP_ALLON -> {
                LogUtils.v("zcl-----------远程控制组全开成功-------")
                hideLoadingDialog()

            }
            Constants.SER_ID_GROUP_ALLOFF -> {
                LogUtils.v("zcl-----------远程控制组全关成功-------")
                hideLoadingDialog()
            }
        }
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.main_go_help->{
                var intent = Intent(context, InstructionsForUsActivity::class.java)
                intent.putExtra(Constants.WB_TYPE,"#add-and-configure")
                startActivity(intent)
            }
            R.id.main_add_device->{
                val lastUser = DBUtils.lastUser
                lastUser?.let {
                    if (it.id.toString() != it.last_authorizer_user_id)
                        ToastUtils.showLong(getString(R.string.author_region_warm))
                    else {
                        var intent = Intent(context, SelectDeviceTypeActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
        }
    }
}
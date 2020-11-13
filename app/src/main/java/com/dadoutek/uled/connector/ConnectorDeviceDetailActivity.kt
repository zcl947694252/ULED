package com.dadoutek.uled.connector

import android.content.Intent
import android.os.Bundle
import android.support.v7.util.DiffUtil
import android.support.v7.widget.*
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseToolbarActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.light.DeviceScanningNewActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbConnector
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.router.BindRouterActivity
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.scene.NewSceneSetAct
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.StringUtils
import com.telink.bluetooth.light.ConnectionStatus
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.empty_view.add_device_btn
import kotlinx.android.synthetic.main.empty_view.no_device_relativeLayout
import kotlinx.android.synthetic.main.template_device_detail_list.*
import kotlinx.android.synthetic.main.template_device_detail_list.recycleView
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.singleLine

/**
 * 蓝牙接收器列表
 */
class ConnectorDeviceDetailActivity : TelinkBaseToolbarActivity(), View.OnClickListener {
    private var launch: Job? = null
    private val relayDatas: MutableList<DbConnector> = mutableListOf()
    private var inflater: LayoutInflater? = null
    private var relayAdaper: DeviceDetailConnectorAdapter? = null
    private var currentDevice: DbConnector? = null
    private var positionCurrent: Int = 0
    private var canBeRefresh = true
    private val REQ_LIGHT_SETTING: Int = 0x01
    private var acitivityIsAlive = true
    private var mConnectDisposal: Disposable? = null
    private var install_device: TextView? = null
    private var create_group: TextView? = null
    private var create_scene: TextView? = null
    private val SCENE_MAX_COUNT = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = this.intent.getIntExtra(Constant.DEVICE_TYPE, 0)
        inflater = this.layoutInflater
        initData()
        initView()
    }

    //显示路由
    override fun bindRouterVisible(): Boolean {
        return true
    }

    override fun bindDeviceRouter() {
        val dbGroup = DbGroup()
        dbGroup.brightness=10000
        dbGroup.deviceType = DeviceType.SMART_RELAY.toLong()
        var intent = Intent(this, BindRouterActivity::class.java)
        intent.putExtra("group", dbGroup)
        startActivity(intent)
    }

    override fun batchGpVisible(): Boolean {
        return true
    }

    override fun setDeletePositiveBtn() {
        currentDevice?.let {
            DBUtils.deleteConnector(it)
            relayDatas.remove(it)
        }
        relayAdaper?.notifyDataSetChanged()
        isEmptyDevice()
    }

    private fun isEmptyDevice() {
        if (relayDatas.size > 0) {
            recycleView.visibility = View.VISIBLE
            no_device_relativeLayout.visibility = View.GONE
        } else {
            recycleView.visibility = View.GONE
            no_device_relativeLayout.visibility = View.VISIBLE
        }
    }

    override fun editeDeviceAdapter() {
        relayAdaper!!.changeState(isEdite)
        relayAdaper!!.notifyDataSetChanged()
    }

    override fun setToolbar(): Toolbar {
        return toolbar
    }

    override fun setDeviceDataSize(num: Int): Int {
        return relayDatas.size
    }

    override fun setLayoutId(): Int {
        return R.layout.template_device_detail_list
    }

    override fun onResume() {
        super.onResume()
        inflater = this.layoutInflater
        initData()
        initView()
    }

    private fun initView() {
        recycleView!!.layoutManager = GridLayoutManager(this, 2)
        recycleView!!.itemAnimator = DefaultItemAnimator()
        relayAdaper = DeviceDetailConnectorAdapter(R.layout.template_device_type_item, relayDatas)
        relayAdaper!!.onItemChildClickListener = onItemChildClickListener
        relayAdaper!!.bindToRecyclerView(recycleView)
        for (i in relayDatas?.indices!!) {
            relayDatas!![i].updateIcon()
        }
        install_device = findViewById(R.id.install_device)
        create_group = findViewById(R.id.create_group)
        create_scene = findViewById(R.id.create_scene)
        install_device?.setOnClickListener(onClick)
        create_group?.setOnClickListener(onClick)
        create_scene?.setOnClickListener(onClick)

        add_device_btn.setOnClickListener(this)
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbarTv.text = getString(R.string.relay)

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
                    // addNewGroup()
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
                        intent.putExtra(Constant.IS_CHANGE_SCENE, false)
                        startActivity(intent)
                    }
                }
            }
        }
    }

    private fun addNewGroup() {
        val textGp = EditText(this)
        textGp.singleLine = true
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
                        DBUtils.addNewGroupWithType(textGp.text.toString().trim { it <= ' ' }, Constant.DEVICE_TYPE_DEFAULT_ALL)
                        dialog.dismiss()
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, _ -> dialog.dismiss() }.show()
    }

    private fun showInstallDeviceList() {
        dialog_relay.visibility = View.GONE
        showInstallDeviceList(isGuide, isRgbClick)
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

    private fun addDevice() {
        intent = Intent(this, DeviceScanningNewActivity::class.java)
        intent.putExtra(Constant.DEVICE_TYPE, DeviceType.SMART_RELAY)
        startActivityForResult(intent, 0)
    }

    var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        currentDevice = relayDatas?.get(position)
        positionCurrent = position
        Opcode.LIGHT_ON_OFF
        val unit = when (view.id) {
            R.id.template_device_icon -> {
                if (TelinkLightApplication.getApp().connectDevice == null && !Constant.IS_ROUTE_MODE) {
                    autoConnect()
                } else {
                    canBeRefresh = true

                    when (currentDevice!!.connectionStatus) {
                        ConnectionStatus.OFF.value -> {
                            if (Constant.IS_ROUTE_MODE)
                                routeOpenOrCloseBase(currentDevice!!.meshAddr, currentDevice!!.productUUID, 1, "relayOpen")
                            else {
                                Commander.openOrCloseLights(currentDevice!!.meshAddr, true)
                                afterOpenOrClose(adapter!!)
                            }
                        }
                        else -> {
                            if (Constant.IS_ROUTE_MODE)
                                routeOpenOrCloseBase(currentDevice!!.meshAddr, currentDevice!!.productUUID, 0, "relayClose")
                            else {
                                Commander.openOrCloseLights(currentDevice!!.meshAddr, false)
                                afterOpenOrClose(adapter!!)
                            }
                        }
                    }
                }
        }
        R.id.template_device_setting -> {
        val lastUser = DBUtils.lastUser
        lastUser?.let {
            if (it.id.toString() != it.last_authorizer_user_id)
                ToastUtils.showLong(getString(R.string.author_region_warm))
            else {
                if (TelinkLightApplication.getApp().connectDevice == null&&!Constant.IS_ROUTE_MODE) {
                    autoConnect()
                } else {
                    var intent = Intent(this@ConnectorDeviceDetailActivity, ConnectorSettingActivity::class.java)
                    intent.putExtra(Constant.LIGHT_ARESS_KEY, currentDevice)
                    intent.putExtra(Constant.GROUP_ARESS_KEY, currentDevice!!.meshAddr)
                    intent.putExtra(Constant.LIGHT_REFRESH_KEY, Constant.LIGHT_REFRESH_KEY_OK)
                    startActivityForResult(intent, REQ_LIGHT_SETTING)
                }
            }
        }
    }
        R.id.template_device_card_delete -> {
        val string = getString(R.string.sure_delete_device, currentDevice?.name)
        builder?.setMessage(string)
        builder?.create()?.show()
    }
        else -> ToastUtils.showLong(R.string.connecting_tip)
    }
}

    override fun tzRouterOpenOrClose(cmdBean: CmdBodyBean) {
              LogUtils.v("zcl-----------收到路由relayOpen通知-------$cmdBean")
                      if (cmdBean.ser_id=="relayOpen"||cmdBean.ser_id=="relayClose"){
                          disposableRouteTimer?.dispose()
                          hideLoadingDialog()
                          if (cmdBean.status==0){
                              afterOpenOrClose(relayAdaper)
                          }else{
                              when(cmdBean.ser_id){
                                  "relayOpen"->ToastUtils.showShort(getString(R.string.open_faile))
                                  "relayClose"->ToastUtils.showShort(getString(R.string.close_faile))
                              }
                          }
                      }
    }

    private fun afterOpenOrClose(adapter: BaseQuickAdapter<*, *>?) {
        when (currentDevice!!.connectionStatus) {
            ConnectionStatus.OFF.value -> currentDevice!!.connectionStatus = ConnectionStatus.ON.value
            else -> currentDevice!!.connectionStatus = ConnectionStatus.OFF.value
        }
        currentDevice!!.updateIcon()
        DBUtils.updateConnector(currentDevice!!)
        runOnUiThread {
            adapter?.notifyDataSetChanged()
        }
    }

    private fun initData() {
    setScanningMode(true)
    relayDatas.clear()
    val allLightData = DBUtils.getAllRelay()
    if (allLightData.size > 0) {
        var listGroup: ArrayList<DbConnector> = ArrayList()
        var noGroup: ArrayList<DbConnector> = ArrayList()
        for (i in allLightData.indices)
            if (StringUtils.getConnectorGroupName(allLightData[i]) == TelinkLightApplication.getApp().getString(R.string.not_grouped))
                noGroup.add(allLightData[i])
            else
                listGroup.add(allLightData[i])

        if (noGroup.size > 0)
            for (i in noGroup.indices)
                relayDatas.add(noGroup[i])

        if (listGroup.size > 0)
            for (i in listGroup.indices)
                relayDatas.add(listGroup[i])

        recycleView.visibility = View.VISIBLE
        no_device_relativeLayout.visibility = View.GONE

        toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.GONE
    } else {
        recycleView.visibility = View.GONE
        no_device_relativeLayout.visibility = View.VISIBLE
        toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.GONE
        toolbar!!.findViewById<ImageView>(R.id.img_function1).setOnClickListener {
            val lastUser = DBUtils.lastUser
            lastUser?.let {
                if (it.id.toString() != it.last_authorizer_user_id)
                    ToastUtils.showLong(getString(R.string.author_region_warm))
                else {
                    if (dialog_relay?.visibility == View.GONE)
                        showPopupMenu()
                }
            }
        }
    }
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
    launch?.cancel()
}


override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    notifyData()
    launch?.cancel()
    launch = GlobalScope.launch {
        //踢灯后没有回调 状态刷新不及时 延时2秒获取最新连接状态
        delay(2500)
        if (this@ConnectorDeviceDetailActivity == null ||
                this@ConnectorDeviceDetailActivity.isDestroyed ||
                this@ConnectorDeviceDetailActivity.isFinishing || !acitivityIsAlive) {
        } else {
            autoConnect()
        }
    }
}

fun notifyData() {
    val mOldDatas: MutableList<DbConnector> = relayDatas
    val mNewDatas: MutableList<DbConnector> = getNewData()
    val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return mOldDatas.get(oldItemPosition).id?.equals(mNewDatas.get
            (newItemPosition).id) ?: false
        }

        override fun getOldListSize(): Int {
            return mOldDatas.size ?: 0
        }

        override fun getNewListSize(): Int {
            return mNewDatas.size ?: 0
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val beanOld = mOldDatas[oldItemPosition]
            val beanNew = mNewDatas[newItemPosition]
            return if (beanOld.name != beanNew.name) {
                return false//如果有内容不同，就返回false
            } else true

        }
    }, true)
    relayAdaper?.let { diffResult.dispatchUpdatesTo(it) }

    relayDatas.clear()
    relayDatas.addAll(mNewDatas)

//        adaper!!.setNewData(lightsData)
    relayAdaper?.notifyDataSetChanged()

}

private fun getNewData(): MutableList<DbConnector> {
    relayDatas.clear()
    relayDatas.addAll(DBUtils.getAllRelay())
    return relayDatas
}

fun autoConnect() {
    val size = DBUtils.getAllCurtains().size + DBUtils.allLight.size + DBUtils.allRely.size
    if (size < 0)
        return
    mConnectDisposal = connect()
            ?.subscribe(
                    {
                        LogUtils.d(it)
                    }
                    ,
                    {
                        LogUtils.d(it)
                    }
            )
}


}

package com.dadoutek.uled.othersview

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.gateway.bean.GwStompBean
import com.dadoutek.uled.group.TypeListAdapter
import com.dadoutek.uled.model.Cmd
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.router.bean.RouteGroupingOrDelOrGetVerBean
import com.dadoutek.uled.stomp.MqttBodyBean
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.BluetoothConnectionFailedDialog
import com.dadoutek.uled.util.PopUtil
import com.dadoutek.uled.util.StringUtils
import com.google.gson.Gson
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.LightAdapter
import com.telink.util.EventListener
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONException

open class BaseFragment : Fragment() {

    private var stompRecevice: StompReceiver? = null
    private lateinit var changeRecevicer: ChangeRecevicer
    private var loadDialog: Dialog? = null
    private lateinit var dialog: Dialog
    private var adapterType: TypeListAdapter? = null
    private var list: MutableList<String>? = null
    private var groupType: Long = 0L
    private var dialogGroupName: TextView? = null
    private var dialogGroupType: TextView? = null
    lateinit var popMain: PopupWindow


    fun showLoadingDialog(content: String) {
        val inflater = LayoutInflater.from(activity)
        val v = inflater.inflate(R.layout.dialogview, null)

        val layout = v.findViewById<View>(R.id.dialog_view) as LinearLayout
        val tvContent = v.findViewById<View>(R.id.tvContent) as TextView
        tvContent.text = content

        if (loadDialog == null) {
            loadDialog = Dialog(activity!!,
                    R.style.FullHeightDialog)
        }
        //loadDialog没显示才把它显示出来
        if (!loadDialog!!.isShowing) {
            loadDialog!!.setCancelable(false)
            loadDialog!!.setCanceledOnTouchOutside(false)
            loadDialog!!.setContentView(layout)
            loadDialog!!.show()
        }
    }

    private fun initStompReceiver() {
        stompRecevice = StompReceiver()
        val filter = IntentFilter()
        filter.addAction(Constant.LOGIN_OUT)
        //filter.addAction(Constant.GW_COMMEND_CODE)
        filter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY - 1
        context?.registerReceiver(stompRecevice, filter)
    }

    inner class StompReceiver : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onReceive(context: Context?, intent: Intent?) {
            val msg = intent?.getStringExtra(Constant.LOGIN_OUT) ?: ""
            val cmdBean: CmdBodyBean = Gson().fromJson(msg, CmdBodyBean::class.java)
            try {
                when (cmdBean.cmd) {
                    Cmd.singleLogin, Cmd.parseQR, Cmd.unbindRegion, Cmd.gwStatus, Cmd.gwCreateCallback, Cmd.gwControlCallback -> {
                        val codeBean = Gson().fromJson(msg, MqttBodyBean::class.java)
                        when (cmdBean.cmd) {
                            Cmd.gwStatus -> TelinkLightApplication.getApp().offLine = codeBean.state == 0//1上线了，0下线了
                            Cmd.gwControlCallback -> receviedGwCmd2500M(codeBean)//推送下发控制指令结果
                        }
                    }
                    Cmd.routeDeleteGroup -> {
                        val routerGroup = Gson().fromJson(msg, RouteGroupingOrDelOrGetVerBean::class.java)
                        tzRouterDelGroupResult(routerGroup)
                    }
                    Cmd.routeDeleteScenes ->{
                        tzRouterDelSceneResult(cmdBean)
                    }
                    Cmd.routeUpdateScenes ->{}

                    /**
                     * 控制指令下的通知
                     */
                    Cmd.tzRouteOpenOrClose  -> tzRouterOpenOrCloseFragment(cmdBean)

                    Cmd.routeApplyScenes -> tzRouterApplyScenes(cmdBean)
                }

            } catch (js: JSONException) {
                js.message
            }


            /*       when (intent?.action) {
                       Constant.GW_COMMEND_CODE -> {
                           val gwStompBean = intent.getSerializableExtra(Constant.GW_COMMEND_CODE) as GwStompBean
                           LogUtils.v("zcl-----------长连接接收网关数据-------$gwStompBean")
                           when (gwStompBean.cmd) {
                               2500 -> receviedGwCmd2500(gwStompBean)
                           }
                       }
                   }*/
        }
    }

    open fun tzRouterApplyScenes(cmdBean: CmdBodyBean) {

    }

    open fun tzRouterOpenOrCloseFragment(cmdBean: CmdBodyBean) {

    }

    open fun tzRouterDelSceneResult(cmdBean: CmdBodyBean) {//1 0 -1  部分成功 成功 失败

    }


    open fun tzRouterDelGroupResult(routerGroup: RouteGroupingOrDelOrGetVerBean) {

    }

    open fun receviedGwCmd2500M(codeBean: MqttBodyBean) {

    }

    open fun receviedGwCmd2500(gwStompBean: GwStompBean) {

    }

    private fun makeDialog() {
        dialog = Dialog(context)
        list = mutableListOf(getString(R.string.normal_light), getString(R.string.rgb_light), getString(R.string.curtain), getString(R.string.relay))
        adapterType = TypeListAdapter(R.layout.item_group, list!!)

        val popView = View.inflate(context, R.layout.dialog_add_group, null)
        popMain = PopupWindow(popView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        popMain.isFocusable = true // 设置PopupWindow可获得焦点
        popMain.isTouchable = true // 设置PopupWindow可触摸补充：
        popMain.isOutsideTouchable = false

        val recyclerView = popView.findViewById<RecyclerView>(R.id.pop_recycle)
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = adapterType

        adapterType?.bindToRecyclerView(recyclerView)

        val dialogGroupTypeArrow = popView.findViewById<ImageView>(R.id.dialog_group_type_arrow)
        val dialogGroupCancel = popView.findViewById<TextView>(R.id.dialog_group_cancel)
        val dialogGroupOk = popView.findViewById<TextView>(R.id.dialog_group_ok)
        dialogGroupType = popView.findViewById<TextView>(R.id.dialog_group_type)
        dialogGroupName = popView.findViewById<TextView>(R.id.dialog_group_name)

        dialogGroupTypeArrow.setOnClickListener {
            if (recyclerView.visibility == View.GONE)
                recyclerView.visibility = View.VISIBLE
            else
                recyclerView.visibility = View.GONE

        }
        dialogGroupType?.setOnClickListener {
            if (recyclerView.visibility == View.GONE)
                recyclerView.visibility = View.VISIBLE
            else
                recyclerView.visibility = View.GONE

        }
        dialogGroupCancel.setOnClickListener { PopUtil.dismiss(popMain) }
        dialogGroupOk.setOnClickListener {
            addNewTypeGroup()
        }

        adapterType?.setOnItemClickListener { _, _, position ->
            dialogGroupType?.text = list!![position]
            recyclerView.visibility = View.GONE
            when (position) {
                0 -> groupType = Constant.DEVICE_TYPE_LIGHT_NORMAL
                1 -> groupType = Constant.DEVICE_TYPE_LIGHT_RGB
                2 -> groupType = Constant.DEVICE_TYPE_CURTAIN
                3 -> groupType = Constant.DEVICE_TYPE_CONNECTOR
            }
        }
        popMain.setOnDismissListener {
            recyclerView.visibility = View.GONE
            dialogGroupType?.text = getString(R.string.not_type)
            dialogGroupName?.text = ""
            groupType = 0
        }
    }

    open fun addNewTypeGroup() {
        // 获取输入框的内容
        if (StringUtils.compileExChar(dialogGroupName?.text.toString().trim { it <= ' ' })) {
            ToastUtils.showLong(getString(R.string.rename_tip_check))
        } else {
            if (groupType == 0L) {
                ToastUtils.showLong(getString(R.string.select_type))
            } else {
                //往DB里添加组数据
                DBUtils.addNewGroupWithType(dialogGroupName?.text.toString().trim { it <= ' ' }, groupType)
                refreshGroupData()
                PopUtil.dismiss(popMain)
            }
        }
    }

    open fun refreshGroupData() {

    }

    /**
     * 改变Toolbar上的图片和状态
     * @param isConnected       是否是连接状态
     */
    open fun changeDisplayImgOnToolbar(isConnected: Boolean) {
        GlobalScope.launch(Dispatchers.Main) {
            if (Constant.IS_ROUTE_MODE)
                toolbar?.findViewById<ImageView>(R.id.image_bluetooth)?.setImageResource(R.drawable.icon_cloud)
            else {
                if (isConnected) {
                    toolbar?.findViewById<ImageView>(R.id.image_bluetooth)?.setImageResource(R.drawable.icon_bluetooth)
                    toolbar?.findViewById<ImageView>(R.id.image_bluetooth)?.isEnabled = false
                    //meFragment 不存在toolbar 所以要拉出来
                    setLoginChange()
                } else {
                    if (toolbar != null) {
                        toolbar?.findViewById<ImageView>(R.id.image_bluetooth)?.setImageResource(R.drawable.bluetooth_no)
                        toolbar?.findViewById<ImageView>(R.id.image_bluetooth)?.isEnabled = true
                        toolbar?.findViewById<ImageView>(R.id.image_bluetooth)?.setOnClickListener {
                            val dialog = BluetoothConnectionFailedDialog(activity, R.style.Dialog)
                            dialog.show()
                        }
                    }
                    setLoginOutChange()
                }
            }
        }
    }

    //打开基类的连接状态变化监听
    private fun enableConnectionStatusListener() {
        //先取消，这样可以确保不会重复添加监听
        TelinkLightApplication.getApp()?.removeEventListener(DeviceEvent.STATUS_CHANGED, StatusChangedListener)
        TelinkLightApplication.getApp()?.addEventListener(DeviceEvent.STATUS_CHANGED, StatusChangedListener)
    }

    //关闭基类的连接状态变化监听
    private fun disableConnectionStatusListener() {
        TelinkLightApplication.getApp()?.removeEventListener(DeviceEvent.STATUS_CHANGED, StatusChangedListener)
    }

    private val StatusChangedListener = EventListener<String?> { event ->
        when (event.type) {
            DeviceEvent.STATUS_CHANGED -> {
                onDeviceStatusChanged(event as DeviceEvent)
            }
        }
    }


    fun onDeviceStatusChanged(event: DeviceEvent) {
        val deviceInfo = event.args
        when (deviceInfo.status) {
            LightAdapter.STATUS_LOGIN -> {
                ToastUtils.showLong(getString(R.string.connect_success))
                changeDisplayImgOnToolbar(true)

            }
            LightAdapter.STATUS_LOGOUT -> {
                changeDisplayImgOnToolbar(false)
            }

            LightAdapter.STATUS_CONNECTING -> {
                ToastUtils.showLong(R.string.connecting_tip)
            }
        }
    }

    open fun setLoginChange() {

    }

    open fun setLoginOutChange() {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initChangeRecevicer()
        initStompReceiver()
        makeDialog()
    }

    private fun initChangeRecevicer() {
        changeRecevicer = ChangeRecevicer()
        val filter = IntentFilter()
        filter.addAction("STATUS_CHANGED")
        filter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY - 2
//        context?.registerReceiver(changeRecevicer, filter)
    }

    override fun onResume() {
        super.onResume()
        enableConnectionStatusListener()

        if (TelinkLightApplication.getApp().connectDevice != null) {
            changeDisplayImgOnToolbar(true)
        } else {
            changeDisplayImgOnToolbar(false)
        }
    }

    override fun onPause() {
        super.onPause()
        disableConnectionStatusListener()

    }


    fun hideLoadingDialog() {
        if (loadDialog != null) {
            loadDialog!!.dismiss()
        }
    }


    open fun endCurrentGuide() {}
    inner class ChangeRecevicer : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val deviceInfo = intent?.getParcelableExtra("STATUS_CHANGED") as DeviceInfo
//            LogUtils.e("zcl获取通知$deviceInfo")
            when (deviceInfo.status) {
                LightAdapter.STATUS_LOGIN -> {
                    ToastUtils.showLong(getString(R.string.connect_success))
                    changeDisplayImgOnToolbar(true)
                }
                LightAdapter.STATUS_LOGOUT -> {
                    changeDisplayImgOnToolbar(false)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        TelinkLightApplication.getApp().refWatcher?.watch(this)
        context?.unregisterReceiver(stompRecevice)
    }

}

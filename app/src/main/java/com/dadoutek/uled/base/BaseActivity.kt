package com.dadoutek.uled.base

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.gateway.bean.GwStompBean
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Cmd
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbConnector
import com.dadoutek.uled.model.dbModel.DbCurtain
import com.dadoutek.uled.model.dbModel.DbLight
import com.dadoutek.uled.model.httpModel.AccountModel
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.mqtt.IGetMessageCallBack
import com.dadoutek.uled.mqtt.MqttService
import com.dadoutek.uled.mqtt.MyServiceConnection
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.stomp.MqttBodyBean
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.NetWorkUtils
import com.dadoutek.uled.util.PopUtil
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import com.google.gson.Gson
import com.telink.TelinkApplication
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 创建者     zcl
 * 创建时间   2019/8/21 20:06
 * 描述	      ${19928748860   %1$s  %2$s  %1$d Android s代表string  d代表int  1-9  }$
 *   private  var netWorkChangReceiver: NetWorkChangReceiver? = null
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${//GlobalScope.launch { delay(1000) } // 使用协程替代thread看是否能解决溢出问题 delay想到与thread  所有内容要放入协程}$
 */

abstract class BaseActivity : AppCompatActivity(), IGetMessageCallBack {
    private var serviceConnection: MyServiceConnection? = null
    private var upDateTimer: Disposable? = null
    private var isResume: Boolean = false
    private lateinit var stompRecevice: StompReceiver
    private var pop: PopupWindow? = null
    private var popView: View? = null
    private var loadDialog: Dialog? = null
    private var singleLogin: AlertDialog? = null
    protected var foreground = false
    private var mApplication: TelinkLightApplication? = null
    private var netWorkChangReceiver: TelinkBaseActivity.NetWorkChangReceiver? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(setLayoutID())
        foreground = true
        this.mApplication = this.application as TelinkLightApplication
        //注册网络状态监听广播
        netWorkChangReceiver = TelinkBaseActivity.NetWorkChangReceiver()
        var filter = IntentFilter()
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(netWorkChangReceiver, filter)

        makeDialogAndPop()
        initView()
        initData()
        initListener()
        initOnLayoutListener()
        initStompReceiver()
        bindService()
    }

    fun bindService() {
        serviceConnection = MyServiceConnection()
        serviceConnection?.setIGetMessageCallBack(this)
        val intent = Intent(this, MqttService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    open fun unbindSe() { //解绑服务
        try {
            serviceConnection?.let {
                unbindService(it)
            }
        } catch (e: java.lang.Exception) {
            LogUtils.v("zcl-----------${e.message}-------${e.printStackTrace()}")
        }
    }

    private fun makeDialogAndPop() {
        singleLogin = AlertDialog.Builder(this)
                .setTitle(R.string.other_device_login)
                .setMessage(getString(R.string.single_login_warm))
                .setCancelable(false)
                .setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
                    dialog.dismiss()
                    restartApplication()
                }.create()

        popView = LayoutInflater.from(this).inflate(R.layout.code_warm, null)
        pop = PopupWindow(popView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        pop?.let {
            it.isFocusable = true // 设置PopupWindow可获得焦点
            it.isTouchable = true // 设置PopupWindow可触摸补充：
            it.isOutsideTouchable = false
        }
    }

    abstract fun initListener()
    abstract fun initData()
    abstract fun initView()
    abstract fun setLayoutID(): Int
    open fun notifyWSData(type: Int, rid: Int) {}
    private fun startTimerUpdate() {
        upDateTimer = Observable.interval(0, 5, TimeUnit.SECONDS).subscribe {
            SyncDataPutOrGetUtils.syncPutDataStart(this@BaseActivity, object : SyncCallback {
                override fun start() {}
                override fun complete() {
                    LogUtils.v("zcl-----------默认上传成功-------")
                }

                override fun error(msg: String?) {}
            })
        }
    }

    private fun stopTimerUpdate() {
        upDateTimer?.dispose()
        upDateTimer = null
    }


    @SuppressLint("SetTextI18n", "StringFormatInvalid", "StringFormatMatches")
    private fun makeCodeDialog(type: Int, phone: String, regionName: String, rid: Int = 0) {
        //移交码为0授权码为1
        var title: String? = null
        var recever: String? = null

        when (type) {
            -1 -> {
                title = getString(R.string.transfer_region_success)
                recever = getString(R.string.recevicer)
            }
            0 -> {
                title = getString(R.string.author_account_receviced)
                recever = getString(R.string.recevicer)
            }
            1 -> {
                title = getString(R.string.author_region_receviced)
                recever = getString(R.string.recevicer)
            }
            2 -> {
                title = getString(R.string.author_region_unAuthorized, regionName)
                recever = getString(R.string.licensor)
            }
        }

        runOnUiThread {
            popView?.let {
                it.findViewById<TextView>(R.id.code_warm_title).text = title

                it.findViewById<TextView>(R.id.code_warm_context).text = recever + phone

                it.findViewById<TextView>(R.id.code_warm_i_see).setOnClickListener {
                    PopUtil.dismiss(pop)
                    if (type == 0)
                        restartApplication()
                }
                LogUtils.v("zcl---------判断---${!this@BaseActivity.isFinishing}----- && --${!pop!!.isShowing} ---&&-- ${window.decorView != null}&&---$isResume")
                try {
                    if (!this@BaseActivity.isFinishing && !pop!!.isShowing && window.decorView != null && isResume)
                        pop!!.showAtLocation(window.decorView, Gravity.CENTER, 0, 0)
                } catch (e: Exception) {
                    LogUtils.v("zcl弹框出现问题${e.localizedMessage}")
                }

                notifyWSData(type, rid)
            }
        }

    }

    fun initOnLayoutListener() {//lan加载监听
        var view = window.decorView
        var viewTreeObserver = view.viewTreeObserver
        viewTreeObserver.addOnGlobalLayoutListener {
            view.viewTreeObserver.removeOnGlobalLayoutListener {}
        }
    }

    override fun onPause() {
        super.onPause()
        isResume = false
        foreground = false
        PopUtil.dismiss(pop)
        stopTimerUpdate()
        unbindSe()
    }

    internal var syncCallbackGet: SyncCallback = object : SyncCallback {
        override fun start() {}
        override fun complete() {}
        override fun error(msg: String) {}
    }

    /**
     * 检查网络上传数据
     * 如果没有网络，则弹出网络设置对话框
     */
    fun checkNetworkAndSync(activity: Activity?) {
        if (!NetWorkUtils.isNetworkAvalible(activity!!)) {
            AlertDialog.Builder(activity)
                    .setTitle(R.string.network_tip_title)
                    .setMessage(R.string.net_disconnect_tip_message)
                    .setPositiveButton(android.R.string.ok
                    ) { _, _ ->
                        // 跳转到设置界面
                        activity.startActivityForResult(Intent(
                                Settings.ACTION_WIRELESS_SETTINGS),
                                0)
                    }.create().show()
        } else {
            SyncDataPutOrGetUtils.syncPutDataStart(activity, syncCallback)
        }
    }

    /**
     * 上传回调
     */
    private var syncCallback: SyncCallback = object : SyncCallback {
        override fun error(msg: String?) {
            if (!this@BaseActivity.isResume && !singleLogin?.isShowing!!) {
                singleLogin?.dismiss()
                singleLogin?.show()
            }
        }

        override fun start() {
            showLoadingDialog(this@BaseActivity.getString(R.string.tip_start_sync))
        }

        override fun complete() {
            hideLoadingDialog()
            TelinkLightService.Instance()?.idleMode(true)
            if (this@BaseActivity.isResume && !singleLogin?.isShowing!!) {
                singleLogin?.dismiss()
                singleLogin?.show()
            }
        }
    }

    //重启app并杀死原进程
    fun restartApplication() {
        TelinkApplication.getInstance().removeEventListeners()
        SharedPreferencesHelper.putBoolean(this, Constant.IS_LOGIN, false)
        AppUtils.relaunchApp()
    }

    fun showLoadingDialog(content: String) {
        val inflater = LayoutInflater.from(this)
        val v = inflater.inflate(R.layout.dialogview, null)

        val layout = v.findViewById<View>(R.id.dialog_view) as LinearLayout
        val tvContent = v.findViewById<View>(R.id.tvContent) as TextView
        tvContent.text = content

        if (loadDialog == null)
            loadDialog = Dialog(this, R.style.FullHeightDialog)

        //loadDialog没显示才把它显示出来
        if (!loadDialog!!.isShowing) {
            loadDialog!!.setCancelable(false)
            loadDialog!!.setCanceledOnTouchOutside(false)
            loadDialog!!.setContentView(layout)
            if (!this.isFinishing) {
                loadDialog?.dismiss()
                loadDialog!!.show()
            }
        }
    }

    fun hideLoadingDialog() {
        GlobalScope.launch(Dispatchers.Main) {
            if (loadDialog != null && this.isActive) {
                loadDialog!!.dismiss()
            }
        }
    }

    val allLights: List<DbLight>
        get() {
            val groupList = DBUtils.groupList
            val lightList = ArrayList<DbLight>()

            for (i in groupList.indices) {
                lightList.addAll(DBUtils.getLightByGroupID(groupList[i].id!!))
            }
            return lightList
        }

    val allCutain: List<DbCurtain>
        get() {
            val groupList = DBUtils.groupList
            val lightList = ArrayList<DbCurtain>()

            for (i in groupList.indices) {
                lightList.addAll(DBUtils.getCurtainByGroupID(groupList[i].id!!))
            }
            return lightList
        }

    val allRely: List<DbConnector>
        get() {
            val groupList = DBUtils.groupList
            val lightList = ArrayList<DbConnector>()

            for (i in groupList.indices) {
                lightList.addAll(DBUtils.getRelayByGroupID(groupList[i].id!!))
            }
            return lightList
        }

    override fun onDestroy() {
        super.onDestroy()
        loadDialog?.dismiss()
        unregisterReceiver(stompRecevice)
        unregisterReceiver(netWorkChangReceiver)
    }

    private fun initStompReceiver() {
        stompRecevice = StompReceiver()
        val filter = IntentFilter()
        filter.addAction(Constant.LOGIN_OUT)
        //filter.addAction(Constant.GW_COMMEND_CODE)
        //filter.addAction(Constant.CANCEL_CODE)
        //filter.addAction(Constant.PARSE_CODE)
        filter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY - 1
        registerReceiver(stompRecevice, filter)
    }

    inner class StompReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val msg = intent?.getStringExtra(Constant.LOGIN_OUT) ?: ""
            var jsonObject = JSONObject(msg)
            LogUtils.v("zcl-----------收到路由总通知-------$jsonObject")
            try {
                val cmd = jsonObject.optInt("cmd")
                when (cmd) {
                    Cmd.singleLogin, Cmd.parseQR, Cmd.unbindRegion, Cmd.gwStatus, Cmd.gwCreateCallback, Cmd.gwControlCallback -> {
                        val codeBean: MqttBodyBean = Gson().fromJson(msg, MqttBodyBean::class.java)
                        when (cmd) {
                            Cmd.singleLogin -> singleDialog(codeBean)//单点登录
                            Cmd.parseQR -> makeCodeDialog(codeBean.type, codeBean.ref_user_phone, codeBean.region_name, codeBean.rid)//推送二维码扫描解析结果
                            Cmd.unbindRegion -> unbindDialog(codeBean)//解除授权信息
                            Cmd.gwStatus -> TelinkLightApplication.getApp().offLine = codeBean.state == 0//1上线了，0下线了
                            Cmd.gwCreateCallback -> if (codeBean.status == 0) receviedGwCmd2000(codeBean.ser_id)//下发标签结果
                            Cmd.gwControlCallback -> receviedGwCmd2500M(codeBean)//推送下发控制指令结果
                        }
                    }
                    Cmd.tzRouteUserReset -> {
                        val cmdBean: CmdBodyBean = getCmdBean(intent)
                        tzRouteUserReset(cmdBean)//用户复位
                    }
                    Cmd.tzRouteResetFactory -> {
                        val cmdBean: CmdBodyBean = getCmdBean(intent)
                        tzRouterResetFactory(cmdBean)
                    }
                }
            } catch (e: java.lang.Exception) {

            }
        }


        /*        when (intent?.action) {
                    Constant.GW_COMMEND_CODE -> {
                        val gwStompBean = intent.getSerializableExtra(Constant.GW_COMMEND_CODE) as GwStompBean
                        when (gwStompBean.cmd) {
                            700 -> TelinkLightApplication.getApp().offLine = false
                            701 -> TelinkLightApplication.getApp().offLine = true
                            2000 -> receviedGwCmd2000(gwStompBean.ser_id)
                            2500 -> receviedGwCmd2500(gwStompBean)
                        }
                    }
                    Constant.LOGIN_OUT -> {
                        checkNetworkAndSync(this@BaseActivity)
                        LogUtils.e("zcl_baseMe___________收到登出消息${intent.getBooleanExtra(Constant.LOGIN_OUT, false)}")
                    }
                    Constant.CANCEL_CODE -> {
                        val extra = intent.getSerializableExtra(Constant.CANCEL_CODE)
                        var cancelBean: CancelAuthorMsg? = null
                        if (extra != null)
                            cancelBean = extra as CancelAuthorMsg
                        val user = DBUtils.lastUser
                        user?.let {
                            if (user.last_authorizer_user_id == cancelBean?.authorizer_user_id.toString()
                                    && user.last_region_id == cancelBean?.rid.toString()) {
                                user.last_region_id = 1.toString()
                                user.last_authorizer_user_id = user.id.toString()
                                DBUtils.deleteAllData()
                                AccountModel.initDatBase(it)
                                //更新last—region-id
                                DBUtils.saveUser(user)
                                Log.e("zclbaseActivity", "zcl******" + DBUtils.lastUser)
                                SyncDataPutOrGetUtils.syncGetDataStart(user, syncCallbackGet)
                            }
                        }

                        LogUtils.e("zcl_baseMe_______取消授权$cancelBean")
                        cancelBean?.let { makeCodeDialog(2, it.authorizer_user_phone, cancelBean?.region_name, cancelBean.rid) }//2代表解除授权信息type

                    }
                    Constant.PARSE_CODE -> {
                        val codeBean: QrCodeTopicMsg = intent.getSerializableExtra(Constant.PARSE_CODE) as QrCodeTopicMsg
    //                    LogUtils.e("zcl_baseMe___________解析二维码")
                        makeCodeDialog(codeBean.type, codeBean.ref_user_phone, codeBean.region_name, codeBean.rid)
                    }
                }*/
    }

    private fun getCmdBean(intent: Intent?): CmdBodyBean {
        val msg = intent?.getStringExtra(Constant.LOGIN_OUT) ?: ""
        val cmdBean: CmdBodyBean = Gson().fromJson(msg, CmdBodyBean::class.java)
        return cmdBean
    }


    open fun tzRouterResetFactory(cmdBean: CmdBodyBean) {

    }

    open fun tzRouteUserReset(cmdBean: CmdBodyBean) {

    }

    private fun singleDialog(codeBean: MqttBodyBean) {
        LogUtils.e("zcl_baseMe___________收到登出消息")
        val boolean = SharedPreferencesHelper.getBoolean(TelinkLightApplication.getApp(), Constant.IS_LOGIN, false)
        if (codeBean.loginStateKey != DBUtils.lastUser?.login_state_key && boolean) //确保登录时成功的
            checkNetworkAndSync(this@BaseActivity)
    }

    private fun unbindDialog(codeBean: MqttBodyBean) {
        val user = DBUtils.lastUser
        user?.let {
            if (it.last_authorizer_user_id == codeBean.authorizer_user_id.toString() && it.last_region_id == codeBean.rid.toString()
                    && it.id.toString() == it.last_authorizer_user_id) {//是自己的区域
                user.last_region_id = 1.toString()
                user.last_authorizer_user_id = user.id.toString()
                DBUtils.deleteAllData()
                AccountModel.initDatBase(it)
                //更新last—region-id
                DBUtils.saveUser(user)
                SyncDataPutOrGetUtils.syncGetDataStart(user, syncCallbackGet)
            }
        }
        makeCodeDialog(2, codeBean.authorizer_user_phone, codeBean.region_name, codeBean.rid)//2代表解除授权信息type
        LogUtils.e("zcl_baseMe___________取消授权${codeBean == null}")
    }

    open fun receviedGwCmd2500M(codeBean: MqttBodyBean) {

    }

    open fun receviedGwCmd2500(gwStompBean: GwStompBean) {
    }

    override fun onResume() {
        super.onResume()
        unbindSe()
        isResume = true
        startTimerUpdate()
        bindService()
    }


    open fun receviedGwCmd2000(serId: String) {

    }

    class NetWorkChangReceiver : BroadcastReceiver() {
        private var isHaveNetwork: Boolean = false

        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                var connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                var networkInfo = connectivityManager.activeNetworkInfo
                if (networkInfo != null && networkInfo.isAvailable) {
                    if (!isHaveNetwork) {
                        val connected = TelinkLightApplication.getApp().mStompManager?.mStompClient?.isConnected
                        if (connected != true)
                            TelinkLightApplication.getApp().initStompClient()
                        LogUtils.v("zcl-----------base收到监听有网状态-------$connected")
                        isHaveNetwork = true
                    }
                } else {
                    LogUtils.v("zcl-----------base收到监听无网状态-------")
                    isHaveNetwork = false
                }
            } catch (e: Exception) {
                //ignore
            }
        }
    }

    override fun setMessage(cmd: Int, extCmd: Int, message: String) {

    }
}
package com.dadoutek.uled.base

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbConnector
import com.dadoutek.uled.model.DbModel.DbCurtain
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.HttpModel.AccountModel
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.othersview.SplashActivity
import com.dadoutek.uled.stomp.model.QrCodeTopicMsg
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.NetWorkUtils
import com.dadoutek.uled.util.PopUtil
import com.dadoutek.uled.util.SharedPreferencesUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.*

/**
 * 创建者     zcl
 * 创建时间   2019/8/21 20:06
 * 描述	      ${19928748860   %1$s  %2$s  %1$d Android s代表string  d代表int  1-9  }$
 *
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${//GlobalScope.launch { delay(1000) } //todo 使用协程替代thread看是否能解决溢出问题 delay想到与thread  所有内容要放入协程}$
 */

abstract class BaseActivity : AppCompatActivity() {
    private lateinit var stompRecevice: StompReceiver
    private var pop: PopupWindow? = null
    private var popView: View? = null
    var loadDialog: Dialog? = null
    val TAGS = "zcl_BaseActivity"
    private var singleLogin: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(setLayoutID())
        makeDialogAndPop()
        initView()
        initData()
        initListener()
        initOnLayoutListener()
        initStompReceiver()
        Constant.isTelBase = false
    }

    private fun makeDialogAndPop() {
        singleLogin = AlertDialog.Builder(this)
                .setTitle(R.string.other_device_login)
                .setMessage(getString(R.string.single_login_warm))
                .setCancelable(false)
                .setOnDismissListener {
                    restartApplication()
                }.setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
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


    abstract fun setLayoutID(): Int
    abstract fun initView()
    abstract fun initData()
    abstract fun initListener()
    open fun notifyWSData() {}


    @SuppressLint("SetTextI18n", "StringFormatInvalid", "StringFormatMatches")
    private fun makeCodeDialog(type: Int, phone: Any, account: Any, regionName: Any) {
        //移交码为0授权码为1
        var title: String? = null
        var recever: String? = null

        when (type) {
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
                    if (type == 0) {
                        restartApplication()
                    }
                }
                notifyWSData()
                if (!this@BaseActivity.isFinishing && !pop!!.isShowing && !Constant.isTelBase)
                    pop!!.showAtLocation(window.decorView, Gravity.CENTER, 0, 0)
            }
        }


        when (type) {
            0 -> { //修改密码
                val user = SharedPreferencesUtils.getLastUser()
                DBUtils.deleteAllData()
                user?.let {
                    val split = user.split("-")
                    if (split.size < 3)
                        return@let
                    Log.e("zcl", "zcl***修改密码***" + split[0] + "------" + split[1] + ":===" + split[2] + "====" + account)

                    NetworkFactory.getApi()
                            .putPassword(account.toString(), NetworkFactory.md5(split[1]))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                Log.e("zcl", "zcl修改密码******${it.message}")
                                SharedPreferencesUtils.saveLastUser(split[0] + "-" + split[1] + "-" + account)
                                TelinkLightService.Instance()?.disconnect()
                                TelinkLightService.Instance()?.idleMode(true)
                                SharedPreferencesHelper.putBoolean(this@BaseActivity, Constant.IS_LOGIN, false)
                            }
                }
            }
        }
    }

    fun initOnLayoutListener() {//lan加载监听
        var view = window.decorView
        var viewTreeObserver = view.viewTreeObserver
        viewTreeObserver.addOnGlobalLayoutListener {
            view.viewTreeObserver.removeOnGlobalLayoutListener{}
        }
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
        override fun error(msg: String?) {}

        override fun start() {
            showLoadingDialog(this@BaseActivity.getString(R.string.tip_start_sync))
        }

        override fun complete() {
            hideLoadingDialog()
            SharedPreferencesHelper.putBoolean(this@BaseActivity, Constant.IS_LOGIN, false)
            TelinkLightService.Instance()?.disconnect()
            TelinkLightService.Instance()?.idleMode(true)
            if (!this@BaseActivity.isFinishing&&!singleLogin?.isShowing!!)
                singleLogin!!.show()
        }
    }

    //重启app并杀死原进程
    private fun restartApplication() {
        ActivityUtils.finishAllActivities(true)
        ActivityUtils.startActivity(SplashActivity::class.java)
        TelinkLightApplication.getApp().releseStomp()
        Log.e("zcl", "zcl******重启app并杀死原进程")
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
            if (!this.isDestroyed)
                GlobalScope.launch(Dispatchers.Main) { loadDialog!!.show() }
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
                lightList.addAll(DBUtils.getConnectorByGroupID(groupList[i].id!!))
            }
            return lightList
        }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(stompRecevice)
    }

    private fun initStompReceiver() {
        stompRecevice = StompReceiver()
        val filter = IntentFilter()
        filter.addAction(Constant.LOGIN_OUT)
        filter.addAction(Constant.CANCEL_CODE)
        filter.addAction(Constant.PARSE_CODE)
        filter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY - 1
        registerReceiver(stompRecevice, filter)
    }

    inner class StompReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Constant.LOGIN_OUT -> {
                    LogUtils.e("zcl_baseMe___________收到登出消息${intent.getBooleanExtra(Constant.LOGIN_OUT, false)}")
                    checkNetworkAndSync(this@BaseActivity)
                }
                Constant.CANCEL_CODE -> {
                    val cancelBean = intent.getSerializableExtra(Constant.CANCEL_CODE) as CancelAuthorMsg
                    val user = DBUtils.lastUser
                    user?.let {
                        if (user.last_authorizer_user_id == cancelBean.authorizer_user_id.toString()
                                && user.last_region_id == cancelBean.rid.toString()) {
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
                    makeCodeDialog(2, cancelBean.authorizer_user_phone, "", cancelBean.region_name)//2代表解除授权信息type
                    LogUtils.e("zcl_baseMe_______取消授权")

                }
                Constant.PARSE_CODE -> {
                    val codeBean: QrCodeTopicMsg = intent.getSerializableExtra(Constant.PARSE_CODE) as QrCodeTopicMsg
                    Log.e(TAGS, "zcl_baseMe___________解析二维码")
                    makeCodeDialog(codeBean.type, codeBean.ref_user_phone, codeBean.account, "")
                }
            }
        }
    }
}
package com.dadoutek.uled.tellink

import android.annotation.SuppressLint
import android.content.Intent
import android.text.TextUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.blankj.utilcode.util.Utils
import com.dadoutek.uled.gateway.bean.GwStompBean
import com.dadoutek.uled.gateway.bean.GwTagBean
import com.dadoutek.uled.gateway.bean.GwTasksBean
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.stomp.StompManager
import com.dadoutek.uled.util.SharedPreferencesUtils
import com.dadoutek.uled.ble.RxBleManager
import com.google.gson.Gson
import com.mob.MobSDK
import com.squareup.leakcanary.RefWatcher
import com.telink.TelinkApplication
import com.telink.bluetooth.TelinkLog
import com.tencent.bugly.Bugly
import com.tencent.bugly.beta.Beta
import com.uuzuche.lib_zxing.activity.ZXingLibrary
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import ua.naiksoftware.stomp.dto.LifecycleEvent
import java.util.*


class TelinkLightApplication : TelinkApplication() {
    companion object {
        private var app: TelinkLightApplication? = null
        fun getApp(): TelinkLightApplication {
            return app!!
        }
    }

    open var refWatcher: RefWatcher? = null
    var currentGwTagBean: GwTagBean? = null
    var isConnectGwBle: Boolean = false
    private var gwCommendDisposable: Disposable? = null
    var offLine: Boolean = false
    var listTask: ArrayList<GwTasksBean> = ArrayList()
    val useIndex = mutableListOf<Int>()
    val freeIndex = mutableListOf<Int>()
    private var stompLifecycleDisposable: Disposable? = null
    open var mStompManager: StompManager? = null
    private var singleLoginTopicDisposable: Disposable? = null
    private var MIN_CLICK_DELAY_TIME = 10000
    private var lastClickTime: Long = 0
    private var mCancelAuthorTopicDisposable: Disposable? = null
    var paserCodedisposable: Disposable? = null
    var lastMeshAddress:Int = 0

    val isEmptyMesh: Boolean
        get() = TextUtils.isEmpty(mesh.name) || TextUtils.isEmpty(mesh.password)


    var mesh: Mesh = Mesh()
        get() {
            field.factoryName = Constant.DEFAULT_MESH_FACTORY_NAME
            field.factoryPassword = Constant.DEFAULT_MESH_FACTORY_PASSWORD
            return field
        }

    /**********************************************
     * Log api
     */
    override fun onCreate() {
        super.onCreate()
        app = this
        super.doInit()
        // LeakCanary内存泄漏第二步
        // This process is dedicated to LeakCanary for heap analysis.//1
        // You should not init your app in this process.
    /*    val inAnalyzerProcess = LeakCanary.isInAnalyzerProcess(this)
        if (!inAnalyzerProcess)
            refWatcher = LeakCanary.install(this)*/

        Utils.init(this)
        Bugly.init(applicationContext, "ea665087a5", false)
        Beta.enableHotfix = false
        RxBleManager.init(this)
        DaoSessionUser.checkAndUpdateDatabase()
        DaoSessionInstance.checkAndUpdateDatabase()
        ZXingLibrary.initDisplayOpinion(this)

        LogUtils.getConfig().setBorderSwitch(false)
        MobSDK.init(this)
    }

    open fun initData() {
        //super.doInit()
        //AES.Security = true;
        val currentRegionID = SharedPreferencesUtils.getCurrentUseRegionId()
        // 此处直接赋值是否可以 --->原逻辑 保存旧的区域信息 保存 区域id  通过区域id查询 再取出name pwd  直接赋值
        //切换区域记得断开连接
        if (currentRegionID != -1L) {
            val lastUser = DBUtils.lastUser ?: return
            val name = lastUser.controlMeshName//dbRegion.getControlMesh();
            val pwd = lastUser.controlMeshPwd//dbRegion.getControlMeshPwd();

            if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(pwd)) {
                mesh = Mesh()
                mesh.name = name
                mesh.password = name
                mesh.factoryName = Constant.DEFAULT_MESH_FACTORY_NAME
                mesh.factoryPassword = Constant.DEFAULT_MESH_FACTORY_PASSWORD
                setupMesh(mesh)
            }
        }
        //启动LightService
        this.startLightService(TelinkLightService::class.java)
    }


    override fun doDestroy() {
        TelinkLog.onDestroy()
        super.doDestroy()
    }

    fun releseStomp() {
        mStompManager?.mStompClient?.disconnect()
    }

    @SuppressLint("CheckResult")
    fun initStompClient() {
        GlobalScope.launch {
            if (SharedPreferencesHelper.getBoolean(this@TelinkLightApplication, Constant.IS_LOGIN, false)) {
                mStompManager = StompManager.get()
                mStompManager?.initStompClient()

                if (mStompManager?.mStompClient == null)
                    initStompClient()



                singleLoginTopicDisposable = mStompManager?.singleLoginTopic()?.subscribe({
                    LogUtils.e("zcl单点登录 It's time to cancel $it")

                    val boolean = SharedPreferencesHelper.getBoolean(getApp(), Constant.IS_LOGIN, false)
                    //LogUtils.e("zcl单点登录 It's time to cancel----$boolean------ $it------------${DBUtils.lastUser?.login_state_key}")
                    if (it != DBUtils.lastUser?.login_state_key && boolean) {//确保登录时成功的
                        val intent = Intent()
                        intent.action = Constant.LOGIN_OUT
                        intent.putExtra(Constant.LOGIN_OUT, it)
                        sendBroadcast(intent)
                    }
                }, {})

                gwCommendDisposable = mStompManager?.gwCommend()?.subscribe({

                    val msg = Gson().fromJson(it, GwStompBean::class.java)
                    LogUtils.e("zcl长连接网关接收 $it")
                    val intent = Intent()
                    intent.action = Constant.GW_COMMEND_CODE
                    intent.putExtra(Constant.GW_COMMEND_CODE, msg)
                    sendBroadcast(intent)
                }, {})


                if (DBUtils.lastUser?.id != null)
                    paserCodedisposable = mStompManager?.parseQRCodeTopic()?.subscribe({
                        LogUtils.e("zcl解析 It's time to parse $it")
                        val intent = Intent()
                        intent.action = Constant.PARSE_CODE
                        intent.putExtra(Constant.PARSE_CODE, it)
                        sendBroadcast(intent)
                    }, {})
                if (DBUtils.lastUser?.id != null)
                    mCancelAuthorTopicDisposable = mStompManager?.cancelAuthorization()?.subscribe({
                        LogUtils.e("zcl取消授权 It's time to cancel $it")
                        val intent = Intent()
                        intent.action = Constant.CANCEL_CODE
                        intent.putExtra(Constant.CANCEL_CODE, it)
                        sendBroadcast(intent)
                    }, {})

                /**
                 * stomp断联监听
                 */
                stompLifecycleDisposable = mStompManager?.lifeCycle()?.subscribe({ lifecycleEvent ->
                    when (lifecycleEvent.type) {
                        LifecycleEvent.Type.CLOSED -> {
//                            LogUtils.d("zcl_Stomp******Stomp connection closed")
                            var currentTime = Calendar.getInstance().timeInMillis
                            if (currentTime - lastClickTime > MIN_CLICK_DELAY_TIME) {
                                lastClickTime = currentTime
                                initStompClient()
                            }
                        }
                    }
                }, { ToastUtils.showLong(it.localizedMessage) })
            }
        }
    }

    fun setupMesh(mesh: Mesh) {
        this.mesh = mesh
    }

    override fun saveLog(action: String) {
        TelinkLog.w("SaveLog: $action")
    }
}



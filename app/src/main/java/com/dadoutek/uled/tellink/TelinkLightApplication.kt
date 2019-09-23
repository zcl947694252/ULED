package com.dadoutek.uled.tellink

import android.annotation.SuppressLint
import android.content.Intent
import android.text.TextUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.blankj.utilcode.util.Utils
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.stomp.StompManager
import com.dadoutek.uled.util.SharedPreferencesUtils
import com.mob.MobSDK
import com.telink.TelinkApplication
import com.telink.bluetooth.TelinkLog
import com.tencent.bugly.Bugly
import com.tencent.bugly.beta.Beta
import com.uuzuche.lib_zxing.activity.ZXingLibrary
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import ua.naiksoftware.stomp.dto.LifecycleEvent
import java.text.SimpleDateFormat
import java.util.*


class TelinkLightApplication : TelinkApplication() {
    companion object {
        private var app: TelinkLightApplication? = null

        fun getApp(): TelinkLightApplication {
            return app!!
        }
    }

    private var stompLifecycleDisposable: Disposable? = null
    private lateinit var mStompManager: StompManager
    private var singleLoginTopicDisposable: Disposable? = null
    var mCancelAuthorTopicDisposable: Disposable? = null
    var paserCodedisposable: Disposable? = null


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
        Utils.init(this)
//        CrashReport.initCrashReport(applicationContext, "ea665087a5", false)
        Bugly.init(applicationContext, "ea665087a5", false)
        Beta.enableHotfix = false

        DaoSessionUser.checkAndUpdateDatabase()
        DaoSessionInstance.checkAndUpdateDatabase()
        ZXingLibrary.initDisplayOpinion(this)
//        initStompClient()

        LogUtils.getConfig().setBorderSwitch(false)
        MobSDK.init(this)
    }

    override fun doInit() {
        super.doInit()
        //AES.Security = true;
        val currentRegionID = SharedPreferencesUtils.getCurrentUseRegion()
        // 此处直接赋值是否可以 --->原逻辑 保存旧的区域信息 保存 区域id  通过区域id查询 再取出name pwd  直接赋值
        //切换区域记得断开连接
        if (currentRegionID != -1L) {
            val dbRegion = DBUtils.getCurrentRegion(currentRegionID)
            //if (dbRegion != null) {
            val lastUser = DBUtils.lastUser ?: return
            val name = lastUser.controlMeshName//dbRegion.getControlMesh();
            val pwd = lastUser.controlMeshPwd//dbRegion.getControlMeshPwd();

            if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(pwd)) {
                //("mesh.setPassword = " + name);
                mesh = Mesh()
                mesh!!.name = name
                mesh!!.password = name
                mesh!!.factoryName = Constant.DEFAULT_MESH_FACTORY_NAME
                mesh!!.factoryPassword = Constant.DEFAULT_MESH_FACTORY_PASSWORD
                setupMesh(mesh)
            }
            //}
        }
        //启动LightService
        this.startLightService(TelinkLightService::class.java)
    }


    override fun doDestroy() {
        TelinkLog.onDestroy()
        super.doDestroy()
//        releseStomp()
    }

    open fun releseStomp() {
        stompLifecycleDisposable?.dispose()
        singleLoginTopicDisposable?.dispose()
        paserCodedisposable?.dispose()
        mCancelAuthorTopicDisposable?.dispose()
    }

    @SuppressLint("CheckResult")
    fun initStompClient() {
        GlobalScope.launch {
            if (SharedPreferencesHelper.getBoolean(this@TelinkLightApplication, Constant.IS_LOGIN, false)) {
                mStompManager = StompManager.get()
                mStompManager.initStompClient()

                singleLoginTopicDisposable = mStompManager.singleLoginTopic().subscribe({
                    val key = SharedPreferencesHelper.getString(this@TelinkLightApplication, Constant.LOGIN_STATE_KEY, "no_have_key")
                    LogUtils.d("STOMP key = $key receivedKey = $it")
                    if (it != key&&"no_have_key"!=it) {
                        val intent = Intent()
                        intent.action = Constant.LOGIN_OUT
                        intent.putExtra(Constant.LOGIN_OUT, key)
                        sendBroadcast(intent)
                    }
                }, {
                    ToastUtils.showShort(it.localizedMessage)
                })


                paserCodedisposable = mStompManager.parseQRCodeTopic().subscribe({
                    LogUtils.e("It's time to parse $it")
                    val intent = Intent()
                    intent.action = Constant.PARSE_CODE
                    intent.putExtra(Constant.PARSE_CODE, it)
                    sendBroadcast(intent)
                }, {
                    ToastUtils.showShort(it.localizedMessage)
                })

                mCancelAuthorTopicDisposable = mStompManager.cancelAuthorization().subscribe({
                    LogUtils.e("It's time to cancel $it")
                    val intent = Intent()
                    intent.action = Constant.CANCEL_CODE
                    intent.putExtra(Constant.CANCEL_CODE, it)
                    sendBroadcast(intent)
                }, { ToastUtils.showShort(it.localizedMessage) })


                stompLifecycleDisposable = mStompManager.lifeCycle()?.subscribe({ lifecycleEvent ->
                    when (lifecycleEvent.type) {
                        LifecycleEvent.Type.OPENED -> LogUtils.d("zcl_Stomp******Stomp connection opened")
                        LifecycleEvent.Type.ERROR -> LogUtils.d("zcl_Stomp******Error" + lifecycleEvent.exception)
                        LifecycleEvent.Type.CLOSED -> {
                            LogUtils.d("zcl_Stomp******Stomp connection closed")
                        }
                    }
                }, {
                    ToastUtils.showShort(it.localizedMessage)
                })
            }
        }
    }

    fun setupMesh(mesh: Mesh) {
        this.mesh = mesh
    }

    override fun saveLog(action: String) {
        val format = SimpleDateFormat("HH:mm:ss.S")
        val time = format.format(Calendar.getInstance().timeInMillis)
        TelinkLog.w("SaveLog: $action")
    }
}



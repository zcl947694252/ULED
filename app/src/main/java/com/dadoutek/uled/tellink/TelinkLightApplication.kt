package com.dadoutek.uled.tellink

import android.annotation.SuppressLint
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import com.blankj.utilcode.util.AppUtils
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
import com.tencent.bugly.crashreport.CrashReport
import com.uuzuche.lib_zxing.activity.ZXingLibrary
import io.reactivex.disposables.Disposable
import ua.naiksoftware.stomp.StompClient
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


    private val intent: Intent? = null
    private var mStompClient: StompClient? = null
    private var codeStompClient: Disposable? = null
    private var loginStompClient: Any? = null


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

    //    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    @SuppressLint("SdCardPath")
    override fun onCreate() {
        super.onCreate()
        app = this
        Utils.init(this)
        CrashReport.initCrashReport(applicationContext, "ea665087a5", false)
        DaoSessionUser.checkAndUpdateDatabase()
        DaoSessionInstance.checkAndUpdateDatabase()
        ZXingLibrary.initDisplayOpinion(this)
        initStompClient()

        LogUtils.getConfig().setBorderSwitch(false)
        if (!AppUtils.isAppDebug()) {
        } else {
            LogUtils.getConfig().setLog2FileSwitch(true)
        }
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
    }


    override fun onTerminate() {
        super.onTerminate()
        singleLoginTopicDisposable?.dispose()
        stompLifecycleDisposable?.dispose()
    }


    private fun initStompClient() {
        if (SharedPreferencesHelper.getBoolean(this, Constant.IS_LOGIN, false)) {
            mStompManager = StompManager.get()
            mStompManager.initStompClient()
            singleLoginTopicDisposable = mStompManager.singleLoginTopic().subscribe({
                val key = SharedPreferencesHelper.getString(this, Constant.LOGIN_STATE_KEY, "no_have_key")
                if (it != key) {
                    LogUtils.d("It's time to logout ")
                }
            }, {
                ToastUtils.showShort(it.localizedMessage)
            })

            stompLifecycleDisposable = mStompManager.lifeCycle()?.subscribe({ lifecycleEvent ->
                when (lifecycleEvent.type) {
                    LifecycleEvent.Type.OPENED -> LogUtils.d("zcl_Stomp******Stomp connection opened")
                    LifecycleEvent.Type.ERROR -> LogUtils.d("zcl_Stomp******Error" + lifecycleEvent.exception)
                    LifecycleEvent.Type.CLOSED -> LogUtils.d("zcl_Stomp******Stomp connection closed")
                }
            }, {
                ToastUtils.showShort(it.localizedMessage)
            })
//            longOperation = LongOperation()
//            longOperation!!.execute()
        }

    }

    fun setupMesh(mesh: Mesh) {
        this.mesh = mesh
    }

    override fun saveLog(action: String) {
        val format = SimpleDateFormat("HH:mm:ss.S")
        val time = format.format(Calendar.getInstance().timeInMillis)
//        logInfo!!.append("\n\t").append(time).append(":\t").append(action)
        /*if (Looper.myLooper() == Looper.getMainLooper()) {
            showToast(action);
        }*/

        TelinkLog.w("SaveLog: $action")
    }//        SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
    //        Date date = sdf.parse(dateInString);


}

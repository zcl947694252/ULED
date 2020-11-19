package com.dadoutek.uled.region

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.LinearLayoutManager.VERTICAL
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.blankj.utilcode.util.CleanUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.BaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.light.PhysicalRecoveryActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.Constant.downTime
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.httpModel.UserModel
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.model.dbModel.DbRegion
import com.dadoutek.uled.model.httpModel.RegionModel
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.othersview.InstructionsForUsActivity
import com.dadoutek.uled.region.adapter.SettingAdapter
import com.dadoutek.uled.region.bean.SettingItemBean
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.router.bean.MacResetBody
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.*
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.greendao.DbUtils
import org.jetbrains.anko.backgroundColor
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * 创建者     ZCL
 * 创建时间   2019/8/1 9:25
 * 描述	      ${清除数据上传数据以及全部恢复出厂}
 *
 * 更新者     $Author$
 * 更新时间   $Date$
 */
class DeveloperActivity : BaseActivity() {
    override fun setLayoutID(): Int {
        this.mApplication = this.application as TelinkLightApplication
        return R.layout.activity_setting
    }

    private var disposableRouteTimer: Disposable? = null
    private var disposable: Disposable? = null
    private var disposableTimer: Disposable? = null
    private var disposableFind: Disposable? = null
    private var disposableInterval: Disposable? = null
    private var mApplication: TelinkLightApplication? = null
    private lateinit var pop: PopupWindow
    private lateinit var cancel: Button
    private lateinit var confirm: Button
    private var compositeDisposable = CompositeDisposable()
    lateinit var tvOne: TextView
    lateinit var tvTwo: TextView
    lateinit var tvThree: TextView
    lateinit var hinitOne: TextView
    lateinit var hinitTwo: TextView
    lateinit var hinitThree: TextView
    lateinit var readTimer: TextView
    lateinit var cancelConfirmLy: LinearLayout
    lateinit var cancelConfirmVertical: View
    var isResetFactory = 0
    override fun initListener() {}

    override fun initData() {
        val list = arrayListOf<SettingItemBean>()
        list.add(SettingItemBean(R.drawable.icon_retrieve, getString(R.string.recovery_active_equipment)))
        list.add(SettingItemBean(R.drawable.icon_clear_data, getString(R.string.chear_cache)))
        list.add(SettingItemBean(R.drawable.icon_restore_factory, getString(R.string.one_click_reset)))
        list.add(SettingItemBean(R.drawable.icon_restore, getString(R.string.physical_recovery)))

        recycleView_setting.layoutManager = LinearLayoutManager(this, VERTICAL, false)
        val settingAdapter = SettingAdapter(R.layout.item_setting, list)
        recycleView_setting.adapter = settingAdapter
        settingAdapter.bindToRecyclerView(recycleView_setting)

        settingAdapter.setOnItemClickListener { _, _, position ->
            val lastUser = DBUtils.lastUser
            lastUser?.let {
                if (it.id.toString() != it.last_authorizer_user_id)
                    ToastUtils.showLong(getString(R.string.author_region_warm))
                else
                    when (position) {
                        0 -> startToRecoverDevices()
                        1 -> emptyTheCache()            //清除数据
                        2 -> showSureResetDialogByApp()      //恢复出厂设置
                        3 -> physicalRecovery()    //物理恢复(非本账户下的灯)
                        //3 -> userReset()
                        //1 -> checkNetworkAndSyncs(this)
                    }
            }
        }
        setting_masking.setOnClickListener {}
    }

    override fun onResume() {
        super.onResume()
        GlobalScope.launch(Dispatchers.Main) {
            delay(500)
            setting_masking.visibility = View.GONE
        }
    }

    private fun physicalRecovery() {  //恢复非本账户的灯
        if (Constant.IS_ROUTE_MODE){
            ToastUtils.showShort(getString(R.string.router_mode_cant_perform))
            return
        }
        isResetFactory = 3
        setFirstePopAndShow(R.string.physical_recovery_one, R.string.physical_recovery_two,
                R.string.user_reset_all3, isResetFactory)
    }

    private fun startToRecoverDevices() {
        if (Constant.IS_ROUTE_MODE) {
            ToastUtils.showShort(getString(R.string.router_mode_cant_perform))
            return
        }

        LogUtils.v("zcl------找回controlMeshName:${DBUtils.lastUser?.controlMeshName}")
        disposableTimer = Observable.timer(13, TimeUnit.SECONDS)
                 .subscribeOn(Schedulers.io())
                                 .observeOn(AndroidSchedulers.mainThread()).subscribe {
            hideLoadingDialog()
            disposableFind?.dispose()
            ToastUtils.showShort(getString(R.string.find_device_num) + 0)
        }
        TelinkLightService.Instance()?.idleMode(true)
        showLoadingDialog(getString(R.string.please_wait))
        val scanFilter = com.polidea.rxandroidble2.scan.ScanFilter.Builder().setDeviceName(DBUtils.lastUser?.controlMeshName).build()
        val scanSettings = ScanSettings.Builder().build()
        disposableFind = RecoverMeshDeviceUtil.findMeshDevice(DBUtils.lastUser?.controlMeshName)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    disposableTimer?.dispose()
                    ToastUtils.showShort(getString(R.string.find_device_num) + it)
                    hideLoadingDialog()
                }, {
                    disposableTimer?.dispose()
                    hideLoadingDialog()
                    LogUtils.d(it)
                })
        disposableTimer?.let {
            compositeDisposable.add(it)
        }
        disposableFind?.let {
            compositeDisposable.add(it)
        }
    }

    private fun userReset() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.user_reset))
        TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_EXTEND_OPCODE, 0,
                byteArrayOf(Opcode.CONFIG_EXTEND_ALL_CLEAR))
        builder.setPositiveButton(getString(R.string.confirm)) { _, _ ->
            clearData()//删除本地数据
            UserModel.clearUserData(DBUtils.lastRegion.id.toInt())?.subscribe({
                ToastUtils.showShort(getString(R.string.reset_user_success))
            }, {
                ToastUtils.showShort(it.message)
            })
        }
        builder.setNegativeButton(getString(R.string.cancel)) { _, _ ->
            ToastUtils.showShort(getString(R.string.cancel))
        }
        builder.show()
    }

    override fun initView() {
        image_bluetooth.visibility = View.GONE
        toolbarTv.text = getString(R.string.setting)
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener { finish() }
        makePop()
    }

    @SuppressLint("CheckResult", "SetTextI18n", "StringFormatMatches")
    private fun showSureResetDialogByApp() {//
        isResetFactory = 2;
        setFirstePopAndShow(R.string.please_sure_all_device_power_on, R.string.reset_factory_all_device
                , R.string.have_question_look_notice, isResetFactory)
    }

    @SuppressLint("CheckResult", "StringFormatMatches")
    private fun setFirstePopAndShow(pleaseSureAllDevicePowerOn: Int, resetFactoryAllDevice: Int, haveQuestionLookNotice: Int, isShowThree: Int) {
        setNormalPopSetting()

        if (isShowThree == 2 || isShowThree == 3) {
            tvThree.visibility = View.VISIBLE
            hinitThree.visibility = View.VISIBLE
        } else {
            tvThree.visibility = View.GONE
            hinitThree.visibility = View.GONE
        }

        hinitOne.text = getString(pleaseSureAllDevicePowerOn)
        hinitTwo.text = getString(resetFactoryAllDevice)
        //hinitThree.text = getString(haveQuestionLookNotice)
        disposableInterval?.dispose()
        disposableInterval = Observable.intervalRange(0, downTime, 0, 1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    var num = downTime - 1 - it
                    if (num == 0L) {
                        setTimerZero()
                    } else {
                        cancelConfirmVertical.backgroundColor = resources.getColor(R.color.white)
                        cancel.isClickable = false
                        confirm.isClickable = false
                        readTimer.text = getString(R.string.please_read_carefully, num)
                    }
                }


        pop.showAtLocation(window.decorView, Gravity.CENTER, 0, 0)
    }

    private fun setNormalPopSetting() {
        confirm.isClickable = false

        tvOne.visibility = View.VISIBLE
        tvThree.visibility = View.VISIBLE
        tvTwo.visibility = View.VISIBLE

        hinitOne.visibility = View.VISIBLE
        hinitThree.visibility = View.VISIBLE
        hinitTwo.visibility = View.VISIBLE

        hinitTwo.textSize = 16F
        hinitTwo.setTextColor(resources.getColor(R.color.gray_3))
        readTimer.visibility = View.VISIBLE

        cancel.text = ""
        confirm.text = ""

        cancelConfirmVertical.backgroundColor = resources.getColor(R.color.white)
    }

    private fun setTimerZero() {
        readTimer.visibility = View.GONE
        cancelConfirmLy.visibility = View.VISIBLE

        cancelConfirmVertical.backgroundColor = resources.getColor(R.color.gray)

        tvOne.visibility = View.INVISIBLE
        tvTwo.visibility = View.INVISIBLE

        when (isResetFactory) {
            3 -> {
                hinitTwo.visibility = View.VISIBLE
                hinitOne.text = getString(R.string.tip_reset_sure)
                hinitTwo.setTextColor(resources.getColor(R.color.red))
                hinitTwo.textSize = 13F
                hinitTwo.text = getString(R.string.reset_warm_red)
                tvThree.visibility = View.GONE
                hinitThree.visibility = View.GONE
            }
            1 -> {
                hinitTwo.visibility = View.GONE
                hinitOne.text = getString(R.string.empty_cache_tip)
                hinitTwo.textSize = 13F
                hinitTwo.text = getString(R.string.reset_warm_red)//在恢复出厂设置之后可以通过添加设备重新添加
                tvThree.visibility = View.GONE
                hinitThree.visibility = View.GONE
            }
            else -> {
                tvOne.visibility = View.VISIBLE
                tvTwo.visibility = View.VISIBLE
                tvThree.visibility = View.VISIBLE
                readTimer.visibility = View.GONE
                cancelConfirmLy.visibility = View.VISIBLE
                cancelConfirmVertical.backgroundColor = resources.getColor(R.color.gray)
            }
        }

        cancel.text = getString(R.string.cancel)
        confirm.text = getString(R.string.confirm)
        cancel.isClickable = true
        confirm.isClickable = true
    }


    //清空缓存初始化APP
    @SuppressLint("CheckResult", "SetTextI18n", "StringFormatMatches")
    private fun emptyTheCache() {
        //清除数据
        isResetFactory = 1
        setFirstePopAndShow(R.string.clear_one, R.string.clear_two,
                R.string.clear_one, isResetFactory)
    }

    /**
     * 清除数据
     */
    @SuppressLint("CheckResult")
    private fun clearData() {
        val dbUser = DBUtils.lastUser

        if (dbUser == null) {
            ToastUtils.showLong(R.string.data_empty)
            return
        }

        updateLastMeshZero()

        showLoadingDialog(getString(R.string.clear_data_now))
        UserModel.deleteAllData(dbUser.token)!!.subscribe({
            LogUtils.e("zcl-----------$it")
            SharedPreferencesHelper.putBoolean(this@DeveloperActivity, Constant.IS_LOGIN, false)
            DBUtils.deleteAllData()
            CleanUtils.cleanInternalSp()
            CleanUtils.cleanExternalCache()
            CleanUtils.cleanInternalFiles()
            CleanUtils.cleanInternalCache()
            ToastUtils.showLong(R.string.clean_tip)
            hideLoadingDialog()
            GlobalScope.launch(Dispatchers.Main) {
                delay(300)
                restartApplication()
            }
        }, {
            ToastUtils.showLong(R.string.clear_fail)
            hideLoadingDialog()
        })


    }

    @SuppressLint("CheckResult")
    private fun updateLastMeshZero() {
        var dbRegion = DbRegion()
        dbRegion.installMesh = /*"dadousmart"*/Constant.DEFAULT_MESH_FACTORY_NAME
        dbRegion.installMeshPwd = "123"
        dbRegion.lastGenMeshAddr = 0
        DBUtils.lastUser?.lastGenMeshAddr = 0
        DBUtils.saveUser(DBUtils.lastUser!!)
        RegionModel.updateMesh((DBUtils.lastUser?.last_region_id ?: "0").toInt(), dbRegion)?.subscribe({
            LogUtils.v("zcl-----------上传最新mesh地址-------成功")
        }, {
            LogUtils.v("zcl-----------上传最新mesh地址-------失败")
        })
    }

    /**
     *  恢复出厂设置Popwindow
     */
    private fun makePop() {
        var popView: View = LayoutInflater.from(this).inflate(R.layout.pop_time_cancel, null)
        tvOne = popView.findViewById(R.id.tv_one)
        tvTwo = popView.findViewById(R.id.tv_two)
        tvThree = popView.findViewById(R.id.tv_three)
        hinitOne = popView.findViewById(R.id.hinit_one)
        hinitTwo = popView.findViewById(R.id.hinit_two)
        hinitThree = popView.findViewById(R.id.hinit_three)
        readTimer = popView.findViewById(R.id.read_timer)
        cancel = popView.findViewById(R.id.tip_cancel)
        confirm = popView.findViewById(R.id.tip_confirm)
        cancelConfirmLy = popView.findViewById(R.id.cancel_confirm_ly)
        cancelConfirmVertical = popView.findViewById(R.id.tip_center_vertical)

        setNormalPopSetting()

        cancel.let {
            it.setOnClickListener { PopUtil.dismiss(pop) }
        }
        confirm.setOnClickListener {
            PopUtil.dismiss(pop)
            //恢复出厂设置
            when (isResetFactory) {
                1 -> clearData()
                3 -> startActivity(Intent(this@DeveloperActivity, PhysicalRecoveryActivity::class.java))
                2 -> if (TelinkLightApplication.getApp().connectDevice != null||Constant.IS_ROUTE_MODE)//恢复出厂设置恢复数据不恢复设备
                    resetAllLights()
                else
                    ToastUtils.showLong(R.string.device_not_connected)
            }
        }


        var cs: ClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                var intent = Intent(this@DeveloperActivity, InstructionsForUsActivity::class.java)
                when (isResetFactory) {
                    2 -> intent.putExtra(Constant.WB_TYPE, "#user-reset")
                    3 -> intent.putExtra(Constant.WB_TYPE, "#light-reset")
                }

                startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = Color.BLUE//设置超链接的颜色
                ds.isUnderlineText = true
            }
        }

        val str = getString(R.string.have_question_look_notice)
        var ss = SpannableString(str)
        val start: Int = if (Locale.getDefault().language.contains("zh")) str.length - 7 else str.length - 26
        val end = str.length
        ss.setSpan(cs, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        hinitThree.text = ss
        hinitThree.movementMethod = LinkMovementMethod.getInstance()

        pop = PopupWindow(popView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        confirm.isClickable = false
        pop.isOutsideTouchable = false
        pop.isFocusable = true // 设置PopupWindow可获得焦点
        pop.isTouchable = true // 设置PopupWindow可触摸补充：
    }

    /**
     * 所有灯恢复出厂设置
     */
    @SuppressLint("CheckResult")
    private fun resetAllLights() {
        showLoadingDialog(getString(R.string.reset_all_now))

        val lightList = DBUtils.allLight
        val curtainList =DBUtils.getAllCurtains()
        val relyList = DBUtils.allRely

        var meshAdre = ArrayList<Int>()
        if (lightList.isNotEmpty()) {
            for (k in lightList.indices)
                meshAdre.add(lightList[k].meshAddr)
        }

        if (curtainList.isNotEmpty()) {
            for (k in curtainList.indices)
                meshAdre.add(curtainList[k].meshAddr)
        }

        if (relyList.isNotEmpty()) {
            for (k in relyList.indices)
                meshAdre.add(relyList[k].meshAddr)
        }

        if (Constant.IS_ROUTE_MODE) {//todo 暂时没有一键恢复的接口看看怎么用 是否用单个的 窗帘 = 0x10
            RouterModel.routeResetFactory(MacResetBody("",0,100,"allFactory"))?.subscribe({
                LogUtils.v("zcl-----------发送路由一键物理恢复-------")
                when (it.errorCode) {
                    0 -> {
                        showLoadingDialog(getString(R.string.please_wait))
                        disposableRouteTimer?.dispose()
                        disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                                 .subscribeOn(Schedulers.io())
                                                 .observeOn(AndroidSchedulers.mainThread())
                                .subscribe {
                                    hideLoadingDialog()
                                    ToastUtils.showShort(getString(R.string.reset_factory_fail))
                                }
                    }
                    90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                    else-> ToastUtils.showShort(it.message)
                }
            }, {
                ToastUtils.showShort(it.message)
            })
        } else {
            updateLastMeshZero()
            Commander.resetAllDevices(meshAdre, {
                syncData()
                this@DeveloperActivity.bnve?.currentItem = 0
                null
            }, {
                null
            })
            if (meshAdre.isEmpty()) {
                hideLoadingDialog()
            }
        }
    }

    override fun tzRouterResetFactory(cmdBean: CmdBodyBean) {
        if (cmdBean.ser_id=="allFactory"){
            disposableRouteTimer?.dispose()
            hideLoadingDialog()
            when (cmdBean.status) {
                0 -> {
                    updateLastMeshZero()
                    syncData()
                }
                else ->ToastUtils.showShort(getString(R.string.reset_factory_fail))
            }
        }
    }

    private fun syncData() {
        SyncDataPutOrGetUtils.syncPutDataStart(this@DeveloperActivity!!, object : SyncCallback {
            override fun complete() {
                hideLoadingDialog()
                //时间太短会导致无法删除数据库数据故此设置1500秒
                if (compositeDisposable.isDisposed)
                    compositeDisposable = CompositeDisposable()
                disposable?.let {
                    compositeDisposable.add(disposable!!)
                }
            }
            override fun error(msg: String) {
                hideLoadingDialog()
                ToastUtils.showLong(R.string.backup_failed)
            }
            override fun start() {}
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
    }
}



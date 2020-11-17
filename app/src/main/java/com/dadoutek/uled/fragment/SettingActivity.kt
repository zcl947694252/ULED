package com.dadoutek.uled.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
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
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.httpModel.UserModel
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.othersview.InstructionsForUsActivity
import com.dadoutek.uled.region.adapter.SettingAdapter
import com.dadoutek.uled.region.bean.SettingItemBean
import com.dadoutek.uled.router.ChooseModeActivity
import com.dadoutek.uled.router.CloudAssistantActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.backgroundColor
import java.util.*
import java.util.concurrent.TimeUnit
/**
 * 创建者     ZCL
 * 创建时间   2020/8/17 19:46
 * 描述
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class SettingActivity : TelinkBaseActivity() {
    private var popUser: PopupWindow? = null
    private var cancelConfirmVertical: View? = null
    private var cancelConfirmLy: LinearLayout? = null
    private var readTimer: TextView? = null
    private var hinitOne1: TextView? = null
    private var hinitTwo: TextView? = null
    private var hinitThree: TextView? = null
    private var mConnectDisposal: Disposable? = null
    private var cancel: Button? = null
    private var confirm: Button? = null
    private var disposableInterval: Disposable? = null
    val list = arrayListOf<SettingItemBean>()
    private val settingAdapter = SettingAdapter(R.layout.item_setting, list, true)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        initView()
        initData()
        initListener()
    }

    private fun initListener() {
        settingAdapter.setOnItemClickListener { adapter, _, position ->
            val lastUser = DBUtils.lastUser
            lastUser?.let { it ->
                if (it.id.toString() != it.last_authorizer_user_id)
                    ToastUtils.showLong(getString(R.string.author_region_warm))
                else {
                    when (position) {
                        0 -> showResetTipPop()
                        1 -> {
                            var intent = Intent(this, SafeLockActivity::class.java)
                            startActivity(intent)
                        }
                        2 -> {
                            UserModel.updateModeStatus().subscribe({
                                Constant.IS_OPEN_AUXFUN = !Constant.IS_OPEN_AUXFUN
                                adapter.notifyDataSetChanged()
                            }, {
                                ToastUtils.showShort(it.message)
                            })
                        }
                        4 -> {
                            var intent = Intent(this, ChooseModeActivity::class.java)
                            startActivity(intent)
                        }
                        3-> {
                            var intent = Intent(this, CloudAssistantActivity::class.java)
                            startActivity(intent)
                        }
                        else->{}
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        settingAdapter.notifyDataSetChanged()
    }

    @SuppressLint("StringFormatMatches")
    fun initData() {
        list.clear()
        list.add(SettingItemBean(R.drawable.icon_reset, getString(R.string.user_reset)))
        list.add(SettingItemBean(R.drawable.icon_lock, getString(R.string.safe_lock)))
        list.add(SettingItemBean(R.drawable.icon_restore, getString(R.string.auxfun)))
        list.add(SettingItemBean(R.drawable.icon_assistant, getString(R.string.cloud_assistant)))
        //list.add(SettingItemBean(R.drawable.icon_internet, getString(R.string.work_mode)))

    }

    @SuppressLint("StringFormatMatches")
    private fun showResetTipPop() {
        if (TelinkLightApplication.getApp().connectDevice != null|| Constant.IS_ROUTE_MODE) {

            disposableInterval = Observable.intervalRange(0, Constant.downTime, 0, 1, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { it1 ->
                        var num = Constant.downTime - 1 - it1
                        when (num) {
                            0L -> setTimerZero()
                            else -> {
                                cancelConfirmVertical?.backgroundColor = resources.getColor(R.color.white)
                                cancel?.isClickable = false
                                confirm?.isClickable = false
                                readTimer?.visibility = View.VISIBLE
                                readTimer?.text = getString(R.string.please_read_carefully, num)
                            }
                        }
                    }
            popUser?.showAtLocation(window?.decorView, Gravity.CENTER, 0, 0)
        } else {
            val deviceTypes = mutableListOf(DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD,
                    DeviceType.LIGHT_RGB, DeviceType.SMART_RELAY, DeviceType.SMART_CURTAIN)
            val size = DBUtils.getAllCurtains().size + DBUtils.allLight.size + DBUtils.allRely.size
            if (size > 0) {
                //ToastUtils.showLong(R.string.connecting_tip)
                mConnectDisposal?.dispose()
                mConnectDisposal = Commander.connect(deviceTypes = deviceTypes, fastestMode = true, retryTimes = 10)
                        ?.subscribe({//找回有效设备
                            mConnectDisposal?.dispose()
                        },
                                {
                                    LogUtils.d("connect failed, reason = $it")
                                }
                        )
            } else {
                ToastUtils.showShort(getString(R.string.no_connect_device))
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun userSet() {
        TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_EXTEND_OPCODE, 0xffff, byteArrayOf(Opcode.CONFIG_EXTEND_ALL_CLEAR, 1, 1, 1, 1, 1, 1, 1))
        UserModel.clearUserData((DBUtils.lastUser?.last_region_id ?: "0").toInt())?.subscribe({  //删除服务器数据
                clearData()//删除本地数据
                ToastUtils.showShort(getString(R.string.reset_user_success))
            }, {
                ToastUtils.showShort(it.message)
        })
    }

    private fun clearData() {
        val dbUser = DBUtils.lastUser
        if (dbUser == null) {
            ToastUtils.showLong(R.string.data_empty)
            return
        }

        showLoadingDialog(getString(R.string.clear_data_now))
        SharedPreferencesHelper.putBoolean(this, Constant.IS_LOGIN, false)
        DBUtils.deleteAllData()
        CleanUtils.cleanInternalSp()
        CleanUtils.cleanExternalCache()
        CleanUtils.cleanInternalFiles()
        CleanUtils.cleanInternalCache()
        SyncDataPutOrGetUtils.syncGetDataStart(dbUser, syncCallbackUp)
        hideLoadingDialog()
    }

    private fun makePop2() {
        var popView: View = LayoutInflater.from(this).inflate(R.layout.pop_time_cancel, null)
        hinitOne1 = popView.findViewById(R.id.hinit_one)
        hinitTwo = popView.findViewById(R.id.hinit_two)
        hinitThree = popView.findViewById(R.id.hinit_three)
        readTimer = popView.findViewById(R.id.read_timer)
        cancel = popView.findViewById(R.id.tip_cancel)
        confirm = popView.findViewById(R.id.tip_confirm)
        cancelConfirmLy = popView.findViewById(R.id.cancel_confirm_ly)
        cancelConfirmVertical = popView.findViewById(R.id.tip_center_vertical)

        hinitOne?.text = getString(R.string.user_reset_all1)
        hinitTwo?.text = getString(R.string.user_reset_all2)
        hinitThree?.text = getString(R.string.user_reset_all3)

        var cs: ClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                var intent = Intent(this@SettingActivity, InstructionsForUsActivity::class.java)
                intent.putExtra(Constant.WB_TYPE, "#user-reset")
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
        hinitThree?.text = ss
        hinitThree?.movementMethod = LinkMovementMethod.getInstance()

        cancel?.setOnClickListener { popUser?.dismiss() }
        confirm?.setOnClickListener {
            popUser?.dismiss()
            userSet() //用户复位
        }
        popUser = PopupWindow(popView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        confirm?.isClickable = false
        pop.isOutsideTouchable = false
        pop.isFocusable = true // 设置PopupWindow可获得焦点
        pop.isTouchable = true // 设置PopupWindow可触摸补充：
    }

    private fun setTimerZero() {
        readTimer?.visibility = View.GONE
        cancelConfirmLy?.visibility = View.VISIBLE
        cancelConfirmVertical?.backgroundColor = resources.getColor(R.color.gray)
        cancel?.text = getString(R.string.cancel)
        confirm?.text = getString(R.string.confirm)
        cancel?.isClickable = true
        confirm?.isClickable = true
    }

    private fun initView() {
        image_bluetooth.visibility = View.VISIBLE
        toolbarTv.text = getString(R.string.setting)
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener { finish() }
        recycleView_setting.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recycleView_setting.adapter = settingAdapter
        settingAdapter.bindToRecyclerView(recycleView_setting)
        makePop2()
    }
}
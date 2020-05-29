package com.dadoutek.uled.othersview

import android.os.Bundle
import android.view.View
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.android.synthetic.main.activity_save_lock.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


/**
 * 创建者     ZCL
 * 创建时间   2020/5/12 18:06
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class SafeLockActivity : TelinkBaseActivity(), View.OnClickListener {
    private var mConnectDisposal: Disposable? = null
    private var allGroup: DbGroup? = null
    private var isFristUserClickCheckConnect: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save_lock)
        initView()
        initData()
        initListener()
    }

    private fun initData() {
        toolbarTv.text = getString(R.string.safe_lock)
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun initView() {
        allGroup = DBUtils.getGroupByMeshAddr(0xFFFF)
    }

    private fun initListener() {
        safe_open.setOnClickListener(this)
        safe_lock.setOnClickListener(this)
        safe_close.setOnClickListener(this)
        safe_unlock.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        isFristUserClickCheckConnect = true
        val dstAddr = this.allGroup!!.meshAddr
        if (TelinkLightApplication.getApp().connectDevice == null)
            checkConnect()
        else {
            when (v?.id) {
                R.id.safe_open -> {
                    ToastUtils.showShort(getString(R.string.open_light))
                    Commander.openOrCloseLights(dstAddr, true)
                    //safe_open.setBackgroundResource(R.drawable.rect_blue_60)
                    //safe_open_arrow.setImageResource(R.mipmap.icon_safe_arrow_blue)
                    //safe_close.setBackgroundResourc
                    // rawable.rect_gray_60)
                    //safe_close_arrow.setImageResource(R.mipmap.icon_arrow_safe)

                }

                R.id.safe_lock -> {
                    ToastUtils.showShort(getString(R.string.lock))
                    //safe_close.setBackgroundResource(R.drawable.rect_gray_60)
                    // safe_open.setBackgroundResource(R.drawable.rect_blue_60)
                    //1打开2关闭 12位
                    TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_EXTEND_OPCODE, 0xffff, byteArrayOf(Opcode.CONFIG_EXTEND_SAFE_LOCK, 1))
                }

                R.id.safe_close -> {
                    ToastUtils.showShort(getString(R.string.close_light))
                    // safe_close.setBackgroundResource(R.drawable.rect_blue_60)
                    //safe_close_arrow.setImageResource(R.mipmap.icon_safe_arrow_blue)
                    //safe_open.setBackgroundResource(R.drawable.rect_gray_60)
                    //safe_open_arrow.setImageResource(R.mipmap.icon_arrow_safe)
                    Commander.openOrCloseLights(dstAddr, false)
                }

                R.id.safe_unlock -> {
                    ToastUtils.showShort(getString(R.string.unlock))
                    //safe_open.setBackgroundResource(R.drawable.rect_gray_60)
                    //safe_close.setBackgroundResource(R.drawable.rect_blue_60)
                    //1打开2关闭 12位
                    TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_EXTEND_OPCODE, 0xffff, byteArrayOf(Opcode.CONFIG_EXTEND_SAFE_LOCK, 2))
                }
            }
        }
    }

    private fun checkConnect() {
        try {
            if (TelinkLightApplication.getApp().connectDevice == null) {
                ToastUtils.showShort(getString(R.string.connecting))
                val deviceTypes = mutableListOf(DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD,
                        DeviceType.LIGHT_RGB, DeviceType.SMART_RELAY, DeviceType.SMART_CURTAIN)
                mConnectDisposal = connect(deviceTypes = deviceTypes, fastestMode = true, retryTimes = 10)
                        ?.subscribe(
                                {
                                    ToastUtils.showShort(getString(R.string.connect_success))
                                },
                                {
                                    ToastUtils.showShort(getString(R.string.connect_fail))
                                }
                        )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
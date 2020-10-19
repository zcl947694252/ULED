package com.dadoutek.uled.othersview

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.View
import android.widget.Toast
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.device.model.DeviceItem
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.othersview.adapter.DeviceTypeAdapter
import com.dadoutek.uled.router.RoutingNetworkActivity
import com.dadoutek.uled.util.StringUtils
import com.jakewharton.rxbinding2.view.RxView
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uuzuche.lib_zxing.activity.CaptureActivity
import com.uuzuche.lib_zxing.activity.CodeUtils
import io.reactivex.internal.operators.observable.ObservableError
import kotlinx.android.synthetic.main.activity_select_device_type.*
import kotlinx.android.synthetic.main.template_recycleview.*
import kotlinx.android.synthetic.main.toolbar.*

class SelectDeviceTypeActivity : TelinkBaseActivity() {
    private lateinit var mRxPermission: RxPermissions
    private val deviceTypeList = mutableListOf<DeviceItem>()
    private val deviceAdapter = DeviceTypeAdapter(R.layout.select_device_type_item, deviceTypeList)
    private val REQUEST_CODE: Int = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_device_type)
        initView()
        initData()
        initListener()
    }

    fun initData() {
        deviceTypeList.clear()
        deviceTypeList.add(DeviceItem(getString(R.string.normal_light), 0, DeviceType.LIGHT_NORMAL))
        deviceTypeList.add(DeviceItem(getString(R.string.rgb_light), 0, DeviceType.LIGHT_RGB))
        deviceTypeList.add(DeviceItem(getString(R.string.switch_title), 0, DeviceType.NORMAL_SWITCH))
        deviceTypeList.add(DeviceItem(getString(R.string.sensor), 0, DeviceType.SENSOR))
        deviceTypeList.add(DeviceItem(getString(R.string.curtain), 0, DeviceType.SMART_CURTAIN))
        deviceTypeList.add(DeviceItem(getString(R.string.relay), 0, DeviceType.SMART_RELAY))
        deviceTypeList.add(DeviceItem(getString(R.string.Gate_way), 0, DeviceType.GATE_WAY))
        deviceTypeList.add(DeviceItem(getString(R.string.router), 0, DeviceType.ROUTER))

        deviceAdapter.notifyDataSetChanged()
    }

    fun initView() {
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setNavigationIcon(R.drawable.icon_return)
        image_bluetooth.visibility = View.GONE
        toolbarTv.text = getString(R.string.add_device_new)
        template_recycleView.layoutManager = GridLayoutManager(this@SelectDeviceTypeActivity, 3)
        template_recycleView.adapter = deviceAdapter
        mRxPermission = RxPermissions(this)
    }


    fun initListener() {
        install_see_helpe.setOnClickListener {
            seeHelpe("#add-and-configure")
        }
        deviceAdapter.setOnItemClickListener { _, _, position ->
            when (position) {
                INSTALL_GATEWAY -> {
                    installId = INSTALL_GATEWAY
                    showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position, getString(R.string.Gate_way))
                }
                INSTALL_NORMAL_LIGHT -> {
                    installId = INSTALL_NORMAL_LIGHT
                    showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position, getString(R.string.normal_light))
                }
                INSTALL_RGB_LIGHT -> {
                    installId = INSTALL_RGB_LIGHT
                    showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position, getString(R.string.rgb_light))
                }
                INSTALL_CURTAIN -> {
                    installId = INSTALL_CURTAIN
                    showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position, getString(R.string.curtain))
                }
                INSTALL_SWITCH -> {
                    installId = INSTALL_SWITCH
                    showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position, getString(R.string.switch_title))
                    stepOneText?.visibility = View.GONE
                    stepTwoText?.visibility = View.GONE
                    stepThreeText?.visibility = View.GONE
                    switchStepOne?.visibility = View.VISIBLE
                    switchStepTwo?.visibility = View.VISIBLE
                    swicthStepThree?.visibility = View.VISIBLE
                }
                INSTALL_SENSOR -> {
                    installId = INSTALL_SENSOR
                    showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position, getString(R.string.sensor))
                }
                INSTALL_CONNECTOR -> {
                    installId = INSTALL_CONNECTOR
                    showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position, getString(R.string.relay))
                }
                INSTALL_ROUTER -> openScan()
            }
        }
    }


    @SuppressLint("CheckResult")
    private fun openScan() {
        mRxPermission.request(Manifest.permission.CAMERA).subscribe({
            var intent = Intent(this@SelectDeviceTypeActivity, CaptureActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE)
        },{
            ToastUtils.showShort(it.message)
        })
    }

    override fun onPause() {
        super.onPause()
        installDialog?.dismiss()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE -> {  //处理扫描结果
                if (null != data) {
                    var bundle: Bundle? = data.extras ?: return
                    when {
                        bundle!!.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS -> {
                            var result = bundle.getString(CodeUtils.RESULT_STRING)
                            LogUtils.v("zcl-----------------解析路由器扫描的一维码-$result")
                            if (result != null) {
                                val intent = Intent(this@SelectDeviceTypeActivity, RoutingNetworkActivity::class.java)
                                intent.putExtra(Constant.ONE_QR, result)
                                startActivity(intent)
                                finish()
                            } else {
                                ToastUtils.showShort(getString(R.string.qr_not_null))
                            }
                        }
                        bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_FAILED -> {
                            Toast.makeText(this, getString(R.string.fail_parse_qr), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }
}

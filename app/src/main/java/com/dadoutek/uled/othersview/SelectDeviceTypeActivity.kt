package com.dadoutek.uled.othersview

import android.app.AlertDialog
import android.content.Intent
import android.support.v7.widget.GridLayoutManager
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.BaseActivity
import com.dadoutek.uled.device.model.DeviceItem
import com.dadoutek.uled.light.DeviceScanningNewActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.othersview.adapter.DeviceTypeAdapter
import com.dadoutek.uled.pir.ScanningSensorActivity
import com.dadoutek.uled.switches.ScanningSwitchActivity
import com.dadoutek.uled.util.StringUtils
import com.telink.util.MeshUtils
import kotlinx.android.synthetic.main.template_recycleview.*
import kotlinx.android.synthetic.main.toolbar.*

class SelectDeviceTypeActivity : BaseActivity() {

    private var installDialog: AlertDialog? = null
    private lateinit var stepOneText: TextView
    private lateinit var stepTwoText: TextView
    private lateinit var stepThreeText: TextView
    private lateinit var switchStepOne: TextView
    private lateinit var switchStepTwo: TextView
    private lateinit var swicthStepThree: TextView
    private lateinit var stepThreeTextSmall: TextView
    private val deviceTypeList = mutableListOf<com.dadoutek.uled.device.model.DeviceItem>()
    private val deviceAdapter = DeviceTypeAdapter(R.layout.select_device_type_item, deviceTypeList)
    private var installId = 0

    override fun initData() {
        deviceTypeList.clear()
        deviceTypeList.add(DeviceItem(getString(R.string.normal_light), 0, DeviceType.LIGHT_NORMAL))
        deviceTypeList.add(DeviceItem(getString(R.string.rgb_light), 0, DeviceType.LIGHT_RGB))
        deviceTypeList.add(DeviceItem(getString(R.string.switch_name), 0, DeviceType.NORMAL_SWITCH))
        deviceTypeList.add(DeviceItem(getString(R.string.sensor), 0, DeviceType.SENSOR))
        deviceTypeList.add(DeviceItem(getString(R.string.curtain), 0, DeviceType.SMART_CURTAIN))
        deviceTypeList.add(DeviceItem(getString(R.string.relay), 0, DeviceType.SMART_RELAY))
        deviceTypeList.add(DeviceItem(getString(R.string.Gate_way), 0, DeviceType.GATE_WAY))

        deviceAdapter.notifyDataSetChanged()
    }

    override fun initView() {
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setNavigationIcon(R.drawable.icon_return)
        image_bluetooth.visibility = View.GONE
        toolbarTv.text = getString(R.string.add_device)
        template_recycleView.layoutManager = GridLayoutManager(this@SelectDeviceTypeActivity, 3)
        template_recycleView.adapter = deviceAdapter
    }

    override fun setLayoutID(): Int {
        return R.layout.activity_select_device_type
    }


    override fun initListener() {
        deviceAdapter.setOnItemClickListener { _, _, position ->
            when (position) {
                Constant.INSTALL_NORMAL_LIGHT -> {
                    installId = Constant.INSTALL_NORMAL_LIGHT
                    showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
                }
                Constant.INSTALL_RGB_LIGHT -> {
                    installId = Constant.INSTALL_RGB_LIGHT
                    showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
                }
                Constant.INSTALL_SWITCH -> {
                    goSearchSwitch()
                }
                Constant.INSTALL_SENSOR -> {
                    installId = Constant.INSTALL_SENSOR
                    showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
                }
                Constant.INSTALL_CURTAIN -> {
                    installId = Constant.INSTALL_CURTAIN
                    showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
                }
                Constant.INSTALL_CONNECTOR -> {
                    installId = Constant.INSTALL_CONNECTOR
                    showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
                }
                Constant.INSTALL_GATEWAY -> {
                    installId = Constant.INSTALL_GATEWAY
                    showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
                }
            }
        }
    }

    private fun goSearchSwitch() {
        installId = Constant.INSTALL_SWITCH
        showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), installId)

        stepOneText.visibility = View.GONE
        stepTwoText.visibility = View.GONE
        stepThreeText.visibility = View.GONE
        switchStepOne.visibility = View.VISIBLE
        switchStepTwo.visibility = View.VISIBLE
        swicthStepThree.visibility = View.VISIBLE
    }

    private fun showInstallDeviceDetail(describe: String, position: Int) {
        val view = View.inflate(this, R.layout.dialog_install_detail, null)
        val close_install_list = view.findViewById<ImageView>(R.id.close_install_list)
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        stepOneText = view.findViewById(R.id.step_one)
        stepTwoText = view.findViewById(R.id.step_two)
        stepThreeText = view.findViewById(R.id.step_three)
        stepThreeTextSmall = view.findViewById(R.id.step_three_small)
        switchStepOne = view.findViewById(R.id.switch_step_one)
        switchStepTwo = view.findViewById(R.id.switch_step_two)
        swicthStepThree = view.findViewById(R.id.switch_step_three)
        val install_tip_question = view.findViewById<TextView>(R.id.install_tip_question)
        val search_bar = view.findViewById<Button>(R.id.search_bar)
        close_install_list.setOnClickListener(dialogOnclick)
        btnBack.setOnClickListener(dialogOnclick)
        search_bar.setOnClickListener(dialogOnclick)

        val title = view.findViewById<TextView>(R.id.textView5)

        title.visibility = View.VISIBLE
        install_tip_question.visibility = View.VISIBLE
        if (position == Constant.INSTALL_SWITCH)
            stepThreeTextSmall.visibility= View.VISIBLE
        else
            stepThreeTextSmall.visibility= View.GONE

        install_tip_question.text = describe
        install_tip_question.movementMethod = ScrollingMovementMethod.getInstance()
        installDialog = android.app.AlertDialog.Builder(this)
                .setView(view)
                .create()

        installDialog?.setOnShowListener {}
        installDialog?.show()
    }

    private val dialogOnclick = View.OnClickListener {
        var medressData = 0
        var allData = DBUtils.allLight
        var sizeData = DBUtils.allLight.size
        if (sizeData != 0) {
            var lightData = allData[sizeData - 1]
            medressData = lightData.meshAddr
        }

        when (it.id) {
            R.id.close_install_list -> {
                installDialog?.dismiss()
            }
            R.id.search_bar -> {
                when (installId) {
                    Constant.INSTALL_NORMAL_LIGHT -> {
                        if (medressData <= MeshUtils.DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.LIGHT_NORMAL)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    Constant.INSTALL_RGB_LIGHT -> {
                        if (medressData <= MeshUtils.DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.LIGHT_RGB)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    Constant.INSTALL_CURTAIN -> {
                        if (medressData <= MeshUtils.DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.SMART_CURTAIN)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    Constant.INSTALL_SWITCH -> {
                        startActivity(Intent(this, ScanningSwitchActivity::class.java))
                    }
                    Constant.INSTALL_SENSOR -> startActivity(Intent(this, ScanningSensorActivity::class.java))
                    Constant.INSTALL_CONNECTOR -> {
                        if (medressData <= MeshUtils.DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.SMART_RELAY)       //connector也叫relay
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    Constant.INSTALL_GATEWAY -> {
                        if (medressData <= MeshUtils.DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.GATE_WAY)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                }
                installDialog?.dismiss()
            }
            R.id.btnBack -> {
                installDialog?.dismiss()
            }
        }
    }

}

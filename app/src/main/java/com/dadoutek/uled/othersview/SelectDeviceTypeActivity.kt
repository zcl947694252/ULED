package com.dadoutek.uled.othersview

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.dadoutek.uled.R
import com.dadoutek.uled.light.DeviceScanningNewActivity
import com.dadoutek.uled.switches.ScanningSwitchActivity
import kotlinx.android.synthetic.main.activity_select_device_type.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.startActivity

class SelectDeviceTypeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_device_type)

        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.select_install_device)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        initListener()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    private fun initListener() {
        tvSmartSwitch.onClick {
            startActivity<ScanningSwitchActivity>()
           // var intent = Intent(this@SelectDeviceTypeActivity, DeviceScanningNewActivity::class.java)
           // intent.putExtra(Constant.DEVICE_TYPE, DeviceType.NORMAL_SWITCH)
           // startActivityForResult(intent, 0)
        }

        tvSmartLight.onClick {
            startActivity<DeviceScanningNewActivity>()
        }

    }

}

package com.dadoutek.uled.othersview

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.BluetoothConnectionFailedDialog
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.LightAdapter
import com.telink.util.EventListener
import kotlinx.android.synthetic.main.toolbar.*

open class BaseFragment : Fragment() {

    private lateinit var changeRecevicer: ChangeRecevicer
    private var loadDialog: Dialog? = null

    fun showLoadingDialog(content: String) {
        val inflater = LayoutInflater.from(activity)
        val v = inflater.inflate(R.layout.dialogview, null)

        val layout = v.findViewById<View>(R.id.dialog_view) as LinearLayout
        val tvContent = v.findViewById<View>(R.id.tvContent) as TextView
        tvContent.text = content

        if (loadDialog == null) {
            loadDialog = Dialog(activity!!,
                    R.style.FullHeightDialog)
        }
        //loadDialog没显示才把它显示出来
        if (!loadDialog!!.isShowing) {
            loadDialog!!.setCancelable(false)
            loadDialog!!.setCanceledOnTouchOutside(false)
            loadDialog!!.setContentView(layout)
            loadDialog!!.show()
        }
    }


    /**
     * 改变Toolbar上的图片和状态
     * @param isConnected       是否是连接状态
     */
    open fun changeDisplayImgOnToolbar(isConnected: Boolean) {
        if (isConnected) {
            if (toolbar != null) {
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.icon_bluetooth)
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = false
            }//meFragment 不存在toolbar 所以要拉出来
                setLoginChange()
        } else {
            if (toolbar != null) {
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = true
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setOnClickListener {
                    val dialog = BluetoothConnectionFailedDialog(activity, R.style.Dialog)
                    dialog.show()
                }
            }
                setLoginOutChange()
        }
    }

    //打开基类的连接状态变化监听
    private fun enableConnectionStatusListener() {
        //先取消，这样可以确保不会重复添加监听
        TelinkLightApplication.getApp()?.removeEventListener(DeviceEvent.STATUS_CHANGED, StatusChangedListener)
        TelinkLightApplication.getApp()?.addEventListener(DeviceEvent.STATUS_CHANGED, StatusChangedListener)
    }

    //关闭基类的连接状态变化监听
    private fun disableConnectionStatusListener() {
        TelinkLightApplication.getApp()?.removeEventListener(DeviceEvent.STATUS_CHANGED, StatusChangedListener)
    }

    private val StatusChangedListener = EventListener<String?> { event ->
        when (event.type) {
            DeviceEvent.STATUS_CHANGED -> {
                onDeviceStatusChanged(event as DeviceEvent)
            }
        }
    }



    fun onDeviceStatusChanged(event: DeviceEvent) {
        val deviceInfo = event.args
        when (deviceInfo.status) {
            LightAdapter.STATUS_LOGIN -> {
                ToastUtils.showLong(getString(R.string.connect_success))
                changeDisplayImgOnToolbar(true)

            }
            LightAdapter.STATUS_LOGOUT -> {
                changeDisplayImgOnToolbar(false)
            }

            LightAdapter.STATUS_CONNECTING -> {
                ToastUtils.showLong(R.string.connecting_please_wait)
            }
        }
    }

    open fun setLoginChange() {

    }

    open fun setLoginOutChange() {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initChangeRecevicer()
    }
    private fun initChangeRecevicer() {
         changeRecevicer = ChangeRecevicer()
        val filter = IntentFilter()
        filter.addAction("STATUS_CHANGED")
        filter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY - 2
//        context?.registerReceiver(changeRecevicer, filter)
    }

    override fun onResume() {
        super.onResume()
        enableConnectionStatusListener()

        if (TelinkLightApplication.getApp().connectDevice != null) {
            changeDisplayImgOnToolbar(true)
        } else {
            changeDisplayImgOnToolbar(false)
        }
    }

    override fun onPause() {
        super.onPause()
        disableConnectionStatusListener()

    }


    fun hideLoadingDialog() {
        if (loadDialog != null) {
            loadDialog!!.dismiss()
        }
    }


    open fun endCurrentGuide() {}
    inner class ChangeRecevicer : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            val deviceInfo = intent?.getParcelableExtra("STATUS_CHANGED") as DeviceInfo
//            LogUtils.e("zcl获取通知$deviceInfo")
            when (deviceInfo.status) {
                LightAdapter.STATUS_LOGIN -> {
                    ToastUtils.showLong(getString(R.string.connect_success))
                    changeDisplayImgOnToolbar(true)

                }
                LightAdapter.STATUS_LOGOUT -> {
                    changeDisplayImgOnToolbar(false)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
//        context?.unregisterReceiver(changeRecevicer)
    }
}

package com.dadoutek.uled.othersview

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.BluetoothConnectionFailedDialog
import com.dadoutek.uled.util.RecoverMeshDeviceUtil
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.light.LightAdapter
import com.telink.util.EventListener
import kotlinx.android.synthetic.main.toolbar.*

open class BaseFragment : Fragment() {

    private var loadDialog: Dialog? = null
//    private var mReceive: BluetoothStateBroadcastReceive? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        registerBluetoothReceiver()
    }

/*
    private fun registerBluetoothReceiver() {
        if (mReceive == null) {
            mReceive = BluetoothStateBroadcastReceive()
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        intentFilter.addAction("android.bluetooth.BluetoothAdapter.STATE_OFF")
        intentFilter.addAction("android.bluetooth.BluetoothAdapter.STATE_ON")
        activity?.registerReceiver(mReceive, intentFilter)
    }
*/

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

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
            }

        } else {
            if (toolbar != null) {
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = true
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setOnClickListener {
                    val dialog = BluetoothConnectionFailedDialog(activity, R.style.Dialog)
                    dialog.show()
                }
            }

        }
    }

    //打开基类的连接状态变化监听
    fun enableConnectionStatusListener() {
        //先取消，这样可以确保不会重复添加监听
        TelinkLightApplication.getApp()?.removeEventListener(DeviceEvent.STATUS_CHANGED, StatusChangedListener)
        TelinkLightApplication.getApp()?.addEventListener(DeviceEvent.STATUS_CHANGED, StatusChangedListener)
    }

    //关闭基类的连接状态变化监听
    fun disableConnectionStatusListener() {
        TelinkLightApplication.getApp()?.removeEventListener(DeviceEvent.STATUS_CHANGED, StatusChangedListener)
    }

    val StatusChangedListener = EventListener<String?> { event ->
        when (event.type) {
            DeviceEvent.STATUS_CHANGED -> {
                onDeviceStatusChanged(event as DeviceEvent)
            }
        }
    }

    private fun onDeviceStatusChanged(event: DeviceEvent) {
        val deviceInfo = event.args
        when (deviceInfo.status) {
            LightAdapter.STATUS_LOGIN -> {
                ToastUtils.showLong(getString(R.string.connect_success))
                changeDisplayImgOnToolbar(true)

                val connectDevice = TelinkLightApplication.getApp()?.connectDevice
                RecoverMeshDeviceUtil.addDevicesToDb(deviceInfo)//  如果已连接的设备不存在数据库，则创建。
            }
            LightAdapter.STATUS_LOGOUT -> {
                changeDisplayImgOnToolbar(false)
            }

            LightAdapter.STATUS_CONNECTING -> {
                ToastUtils.showLong(R.string.connecting_please_wait)
            }
        }


    }


    override fun onPause() {
        super.onPause()
        disableConnectionStatusListener()
    }

    override fun onResume() {
        super.onResume()
        enableConnectionStatusListener()

        val lightService: TelinkLightService? = TelinkLightService.Instance()

        if (lightService?.isLogin == true) {
            changeDisplayImgOnToolbar(true)
        } else {
            changeDisplayImgOnToolbar(false)
        }

//        val blueadapter = BluetoothAdapter.getDefaultAdapter()
//        if (blueadapter?.isEnabled == false) {
//            if(toolbar!=null){
//                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
//                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = true
//                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setOnClickListener(View.OnClickListener {
//                    var dialog = BluetoothConnectionFailedDialog(activity,R.style.Dialog)
//                    dialog.show()
//                })
//            }
//        }else{
//            if(toolbar!=null){
//                if (TelinkLightApplication.getApp().connectDevice == null) {
//                    toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
//                    toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = true
//                    toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setOnClickListener {
//                        var dialog = BluetoothConnectionFailedDialog(activity,R.style.Dialog)
//                        dialog.show()
//                    }
//                }else{
//                    toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.icon_bluetooth)
//                    toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = false
//                }
//            }
//        }

    }

/*
    inner class BluetoothStateBroadcastReceive : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    if (toolbar != null) {
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.icon_bluetooth)
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = false
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    if (toolbar != null) {
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = true
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setOnClickListener {
                            var dialog = BluetoothConnectionFailedDialog(activity,R.style.Dialog)
                            dialog.show()
                        }
                    }
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)) {
                        BluetoothAdapter.STATE_OFF -> {
                            if (toolbar != null) {
                                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = true
                                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setOnClickListener {
                                    var dialog = BluetoothConnectionFailedDialog(activity,R.style.Dialog)
                                    dialog.show()
                                }
                            }
                        }
                        BluetoothAdapter.STATE_ON -> {
                            if (toolbar != null) {
                                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).isEnabled = false
                            }
                        }
                    }
                }
            }
        }
    }
*/


    fun hideLoadingDialog() {
        if (loadDialog != null) {
            loadDialog!!.dismiss()
        }
    }

/*
    override fun onDestroy() {
        super.onDestroy()
//        unregisterBluetoothReceiver()
    }
*/

/*
    private fun unregisterBluetoothReceiver() {
        if (mReceive != null) {
            activity?.unregisterReceiver(mReceive)
            mReceive = null
        }
    }
*/

    open fun endCurrentGuide() {}
}

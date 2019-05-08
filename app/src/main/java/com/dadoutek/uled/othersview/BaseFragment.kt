package com.dadoutek.uled.othersview

import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.blankj.utilcode.util.ActivityUtils

import com.dadoutek.uled.R
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import kotlinx.android.synthetic.main.toolbar.*

open class BaseFragment : Fragment() {

    private var loadDialog: Dialog? = null
    private var mReceive: BluetoothStateBroadcastReceive? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerBluetoothReceiver()
    }

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

    override fun onResume() {
        super.onResume()
        val blueadapter = BluetoothAdapter.getDefaultAdapter()
        if (blueadapter?.isEnabled == false) {
            if(toolbar!=null){
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
            }
        }else{
            if(toolbar!=null){
                if (TelinkLightApplication.getInstance().connectDevice == null) {
                    toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                }else{
                    toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_yse)
                }
            }
        }

    }

    inner class BluetoothStateBroadcastReceive : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            when (action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    if (toolbar != null) {
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_yse)
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    if (toolbar != null) {
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                    }
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)
                    when (blueState) {
                        BluetoothAdapter.STATE_OFF -> {
                            if (toolbar != null) {
                                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                            }
                        }
                        BluetoothAdapter.STATE_ON -> {
                            if (toolbar != null) {
                                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_yse)
                            }
                        }
                    }
                }
            }
        }
    }


    fun hideLoadingDialog() {
        if (loadDialog != null) {
            loadDialog!!.dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterBluetoothReceiver()
    }

    private fun unregisterBluetoothReceiver() {
        if (mReceive != null) {
            activity?.unregisterReceiver(mReceive)
            mReceive = null
        }
    }

    open fun endCurrentGuide() {}
}

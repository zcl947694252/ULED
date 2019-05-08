package com.dadoutek.uled.tellink

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.util.StringUtils
import com.telink.bluetooth.LeBluetooth
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.anko.design.indefiniteSnackbar

import java.util.regex.Matcher
import java.util.regex.Pattern
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.widget.ImageView
import com.blankj.utilcode.util.LogUtils
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.toolbar


open class TelinkBaseActivity : AppCompatActivity() {

    protected var toast: Toast? = null
    protected var foreground = false
    private var loadDialog: Dialog? = null

    private var mReceive: BluetoothStateBroadcastReceive? = null

    @SuppressLint("ShowToast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.toast = Toast.makeText(this, "", Toast.LENGTH_SHORT)
        foreground = true
        registerBluetoothReceiver()
    }

    override fun onStart() {
        super.onStart()
//        registerBluetoothReceiver()
    }

    //增加全局监听蓝牙开启状态
    private fun showOpenBluetoothDialog(context: Context) {
        val dialogTip = AlertDialog.Builder(context)
        dialogTip.setMessage(R.string.openBluetooth)
        dialogTip.setPositiveButton(android.R.string.ok) { dialog, which ->
            LeBluetooth.getInstance().enable(applicationContext)
        }
        dialogTip.setCancelable(false)
        dialogTip.create().show()
    }

    override fun onPause() {
        super.onPause()
        foreground = false
    }

    private fun unregisterBluetoothReceiver() {
        if (mReceive != null) {
            unregisterReceiver(mReceive)
            mReceive = null
        }
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
        registerReceiver(mReceive, intentFilter)
    }


    override fun onResume() {
        super.onResume()
        foreground = true
        val blueadapter = BluetoothAdapter.getDefaultAdapter()
        if (blueadapter?.isEnabled == false) {
            showOpenBluetoothDialog(ActivityUtils.getTopActivity())
            if (toolbar != null) {
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
            }
        } else {
            if (toolbar != null) {
                if (TelinkLightApplication.getInstance().connectDevice == null) {
                    toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                } else {
                    toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_yse)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        this.toast!!.cancel()
        this.toast = null
        unregisterBluetoothReceiver()
    }

    fun showToast(s: CharSequence) {
        ToastUtils.showLong(s)
    }

    protected fun saveLog(log: String) {
        (application as TelinkLightApplication).saveLog(log)
    }

    fun showLoadingDialog(content: String) {
        val inflater = LayoutInflater.from(this)
        val v = inflater.inflate(R.layout.dialogview, null)

        val layout = v.findViewById<View>(R.id.dialog_view) as LinearLayout
        val tvContent = v.findViewById<View>(R.id.tvContent) as TextView
        tvContent.text = content


        //        ImageView spaceshipImage = (ImageView) v.findViewById(R.id.img);
        //
        //        @SuppressLint("ResourceType") Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(this,
        //                R.animator.load_animation);

        //        spaceshipImage.startAnimation(hyperspaceJumpAnimation);

        if (loadDialog == null) {
            loadDialog = Dialog(this,
                    R.style.FullHeightDialog)
        }
        //loadDialog没显示才把它显示出来
        if (!loadDialog!!.isShowing) {
            loadDialog!!.setCancelable(false)
            loadDialog!!.setCanceledOnTouchOutside(false)
            loadDialog!!.setContentView(layout)
            if (!this.isDestroyed) {
                GlobalScope.launch(Dispatchers.Main) {
                    loadDialog!!.show()
                }
            }
        }
    }

    fun hideLoadingDialog() {
        GlobalScope.launch(Dispatchers.Main) {
            if (loadDialog != null && this.isActive) {
                loadDialog!!.dismiss()
            }
        }
    }

    fun compileExChar(str: String): Boolean {
        return StringUtils.compileExChar(str)
    }

    fun endCurrentGuide() {}

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

}


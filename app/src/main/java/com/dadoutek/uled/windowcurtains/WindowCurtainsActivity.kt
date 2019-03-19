package com.dadoutek.uled.windowcurtains

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.View
import android.widget.EditText
import com.android.ehorizontalselectedview.EHorizontalSelectedView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.group.LightGroupingActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbCurtain
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.StringUtils
import com.telink.bluetooth.light.DeviceInfo
import kotlinx.android.synthetic.main.activity_window_curtains.*
import kotlinx.android.synthetic.main.toolbar.*
import java.util.*

class WindowCurtainsActivity : TelinkBaseActivity() ,View.OnClickListener{

    private var showList: List<DbCurtain>? = null

    private var curtain:DbCurtain?=null

    private var ctAdress:Int?=null

    private var currentShowGroupSetPage=true

    private lateinit var group:DbGroup

    private var mConnectDevice: DeviceInfo? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_window_curtains)
        initView()
        initToolbar()
    }


    fun remove() {
        AlertDialog.Builder(Objects.requireNonNull<Activity>(this)).setMessage(R.string.delete_light_confirm)
                .setPositiveButton(android.R.string.ok) { dialog, which ->

                    if (TelinkLightService.Instance().adapter.mLightCtrl.currentLight.isConnected) {
                        val opcode = Opcode.KICK_OUT
                        TelinkLightService.Instance().sendCommandNoResponse(opcode, curtain!!.meshAddr, null)
                        DBUtils.deleteCurtain(curtain!!)
                        if (TelinkLightApplication.getApp().mesh.removeDeviceByMeshAddress(curtain!!.meshAddr)) {
                            TelinkLightApplication.getApp().mesh.saveOrUpdate(this!!)
                        }
                        if (mConnectDevice != null) {
                            Log.d(this!!.javaClass.simpleName, "mConnectDevice.meshAddress = " + mConnectDevice!!.meshAddress)
                            Log.d(this!!.javaClass.simpleName, "light.getMeshAddr() = " + curtain!!.meshAddr)
                            if (curtain!!.meshAddr == mConnectDevice!!.meshAddress) {
                                this!!.setResult(Activity.RESULT_OK, Intent().putExtra("data", true))
                            }
                        }
                        this!!.finish()


                    } else {
                        ToastUtils.showLong("当前处于未连接状态，重连中。。。")
                        this!!.finish()
                    }
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
    }

    private val menuItemClickListener= Toolbar.OnMenuItemClickListener { item ->
        when(item?.itemId){
            R.id.toolbar_rename_light->{renameLight()}
            R.id.toolbar_reset->{remove()}
            R.id.toolbar_update_group->{updateGroup()}
        }
        true
    }

    private fun updateGroup() {
        val intent = Intent(this,
                LightGroupingActivity::class.java)
        intent.putExtra("curtain", curtain)
        intent.putExtra(Constant.TYPE_VIEW,Constant.CURTAINS_KEY)
        intent.putExtra("gpAddress", ctAdress)
        intent.putExtra("uuid",curtain!!.productUUID)
        intent.putExtra("belongId",curtain!!.belongGroupId)
        startActivity(intent)
    }

    private fun renameLight() {
        val textGp = EditText(this)
        StringUtils.initEditTextFilter(textGp)
        textGp.setText(curtain?.name)
        textGp.setSelection(textGp.getText().toString().length)
        android.app.AlertDialog.Builder(this@WindowCurtainsActivity)
                .setTitle(R.string.rename)
                .setView(textGp)

                .setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
                    // 获取输入框的内容
                    if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                        ToastUtils.showShort(getString(R.string.rename_tip_check))
                    } else {
                        curtain?.name=textGp.text.toString().trim { it <= ' ' }
                        DBUtils.updateCurtain(curtain!!)
                        toolbar.title=curtain?.name
                        dialog.dismiss()
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()
    }


    private fun initToolbar() {
        toolbar.title = ""
        toolbar.inflateMenu(R.menu.menu_rgb_light_setting)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setOnMenuItemClickListener(menuItemClickListener)
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener {
              finish()
        }
    }

    private fun initView() {
        this.ctAdress = this.intent.getIntExtra(Constant.CURTAINS_ARESS_KEY, 0)
        open.setOnClickListener(this)
        off.setOnClickListener(this)
        pause.setOnClickListener(this)
        commutation.setOnClickListener(this)
        setting.setOnClickListener(this)
        restart.setOnClickListener(this)
        setSpeed()
    }

    private fun setSpeed() {
        val horizontalSelectedView = findViewById<EHorizontalSelectedView>(R.id.hsv)
        val objects = ArrayList<String>()
        objects.add("0")
        objects.add("1")
        objects.add("2")
        objects.add("3")
        objects.add("4")
        horizontalSelectedView.setData(objects)
        horizontalSelectedView.setSeeSize(5)
        horizontalSelectedView.setOtherTextSize(25F)
        horizontalSelectedView.setSelectTextSize(25F)

        horizontalSelectedView.setOnRollingListener { _, s ->
            when (s) {
                "1" -> {
                    val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x15, 1, Opcode.CURTAIN_PACK_END)
                    val opcode = Opcode.CURTAIN_ON_OFF
                    TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
                }
                "2" -> {
                    val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x15, 2, Opcode.CURTAIN_PACK_END)
                    val opcode = Opcode.CURTAIN_ON_OFF
                    TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
                }
                "3" -> {
                    val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x15, 3, Opcode.CURTAIN_PACK_END)
                    val opcode = Opcode.CURTAIN_ON_OFF
                    TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
                }
                "4" -> {
                    val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x15, 4, Opcode.CURTAIN_PACK_END)
                    val opcode = Opcode.CURTAIN_ON_OFF
                    TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
                }
            }
        }
    }

    override fun onClick(v: View?) {
       when(v?.id){
           R.id.open->openWindow()
           R.id.off->offWindow()
           R.id.pause->pauseWindow()
           R.id.commutation->electricCommutation()
           R.id.setting->onceReset()
           R.id.restart->clickRestart()
       }
    }

    private fun clickRestart() {
        val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0xEA.toByte(), 0x00, Opcode.CURTAIN_PACK_END)
        val opcode = Opcode.CURTAIN_ON_OFF
        TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
    }

    private fun onceReset() {
        val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0xEC.toByte(), 0x00, Opcode.CURTAIN_PACK_END)
        val opcode = Opcode.CURTAIN_ON_OFF
        TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
    }

    private fun electricCommutation() {
        val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x11, 0x01, Opcode.CURTAIN_PACK_END)
        val opcode = Opcode.CURTAIN_ON_OFF
        TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
    }


    private fun pauseWindow() {
       val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x0B, 0x00, Opcode.CURTAIN_PACK_END)
       val opcode = Opcode.CURTAIN_ON_OFF
       TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
    }

    private fun offWindow() {
        val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x0C, 0x00, Opcode.CURTAIN_PACK_END)
        val opcode = Opcode.CURTAIN_ON_OFF
        TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
    }

    private fun openWindow() {
        val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x0A, 0x00, Opcode.CURTAIN_PACK_END)
        val opcode = Opcode.CURTAIN_ON_OFF
        TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
    }
}


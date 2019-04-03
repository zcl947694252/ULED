package com.dadoutek.uled.windowcurtains

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.android.ehorizontalselectedview.EHorizontalSelectedView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.group.CurtainGroupingActivity
import com.dadoutek.uled.group.LightGroupingActivity
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbCurtain
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.StringUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import com.telink.bluetooth.light.DeviceInfo
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.android.synthetic.main.activity_window_curtains.*
import kotlinx.android.synthetic.main.toolbar.*
import java.util.*
import java.util.concurrent.TimeUnit

class WindowCurtainsActivity : TelinkBaseActivity() ,View.OnClickListener{

    private var showList: List<DbCurtain>? = null

    private var curtain:DbCurtain?=null

    private var ctAdress:Int?=null

    private var curtainGroup:DbGroup?=null

    private var currentShowGroupSetPage=true

    private var mConnectDevice: DeviceInfo? = null

    private var compositeDisposable = CompositeDisposable()

    private var commutationBoolean:Boolean=true

    private var slowBoolean:Boolean=true

    private var handBoolean:Boolean=true

    private var type:String?=null

    private lateinit var group_delete:Button

    private lateinit var updateGroup:Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_window_curtains)
        initViewType()
        initView()
    }

    private fun initViewType() {
        this.type=this.intent.extras!!.getString(Constant.TYPE_VIEW)
        if(type==Constant.TYPE_GROUP){
            initGroupData()
            initToolGroupBar()
            group_delete=findViewById(R.id.delete_Group)
            this.group_delete.visibility=View.VISIBLE
        }else{
            initMeshDresData()
            initToolbar()
            updateGroup=findViewById(R.id.update_group)
            this.updateGroup.visibility=View.VISIBLE
        }
    }

    private fun initToolGroupBar() {
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

    private fun initMeshDresData() {
        this.ctAdress = this.intent.getIntExtra(Constant.CURTAINS_ARESS_KEY, 0)
        this.curtain = this.intent.extras!!.get(Constant.LIGHT_ARESS_KEY) as DbCurtain
    }

    private fun initGroupData() {
        this.curtainGroup=this.intent.extras!!.get("group") as DbGroup
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
            R.id.toolbar_delete_group->{removeGroup()}
            R.id.toolbar_rename_group->{renameGp()}
            R.id.toolbar_rename_light->{renameLight()}
            R.id.toolbar_reset->{remove()}
            R.id.toolbar_update_group->{updateGroup()}
        }
        true
    }

    private fun renameGp() {
        val textGp = EditText(this)
        textGp.setText(curtainGroup?.name)
        StringUtils.initEditTextFilter(textGp)
        textGp.setSelection(textGp.getText().toString().length)
        android.app.AlertDialog.Builder(this@WindowCurtainsActivity)
                .setTitle(R.string.rename)
                .setView(textGp)

                .setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
                    // 获取输入框的内容
                    if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                        ToastUtils.showShort(getString(R.string.rename_tip_check))
                    } else {
                        var name = textGp.text.toString().trim { it <= ' ' }
                        var canSave = true
                        val groups = DBUtils.allGroups
                        for (i in groups.indices) {
                            if (groups[i].name == name) {
                                ToastUtils.showLong(TelinkLightApplication.getInstance().getString(R.string.repeat_name))
                                canSave = false
                                break
                            }
                        }

                        if (canSave) {
                            curtainGroup?.name = textGp.text.toString().trim { it <= ' ' }
                            DBUtils.updateGroup(curtainGroup!!)
                            toolbar.title = curtainGroup?.name
                            dialog.dismiss()
                        }
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()
    }


    private fun removeGroup() {
        AlertDialog.Builder(Objects.requireNonNull<FragmentActivity>(this)).setMessage(R.string.delete_group_confirm)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    this.showLoadingDialog(getString(R.string.deleting))

                    deleteGroup(DBUtils.getCurtainByGroupID(curtainGroup!!.id), curtainGroup!!,
                            successCallback = {
                                this.hideLoadingDialog()
                                this.setResult(Constant.RESULT_OK)
                                this.finish()
                            },
                            failedCallback = {
                                this.hideLoadingDialog()
                                ToastUtils.showShort(R.string.move_out_some_lights_in_group_failed)
                            })
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
    }

    private fun updateGroup() {
        val intent = Intent(this,
                CurtainGroupingActivity::class.java)
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
        open.setOnClickListener(this)
        off.setOnClickListener(this)
        pause.setOnClickListener(this)
        commutation.setOnClickListener(this)
        setting.setOnClickListener(this)
        restart.setOnClickListener(this)
        delete_Group.setOnClickListener(this)
        update_group.setOnClickListener(this)
        slow_up.setOnClickListener(this)
        hand_recovery.setOnClickListener(this)
        software_restart.setOnClickListener(this)
        if(type==Constant.TYPE_GROUP){
            setting.visibility=View.GONE
        }
        setSpeed()
    }

    private fun setSpeed() {
        val horizontalSelectedView = findViewById<EHorizontalSelectedView>(R.id.hsv)
        val objects = ArrayList<String>()
        objects.add("1")
        objects.add("2")
        objects.add("3")
        objects.add("4")
        horizontalSelectedView.setData(objects)
        horizontalSelectedView.setSeeSize(4)
        horizontalSelectedView.setOtherTextSize(25F)
        horizontalSelectedView.setSelectTextSize(25F)

        if(type==Constant.TYPE_GROUP){
            horizontalSelectedView.setOnRollingListener { _, s ->
                when (s) {
                    "1" -> {
                        val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x15, 1, Opcode.CURTAIN_PACK_END)
                        val opcode = Opcode.CURTAIN_ON_OFF
                        TelinkLightService.Instance().sendCommandNoResponse(opcode, curtainGroup!!.meshAddr,params)
                    }
                    "2" -> {
                        val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x15, 2, Opcode.CURTAIN_PACK_END)
                        val opcode = Opcode.CURTAIN_ON_OFF
                        TelinkLightService.Instance().sendCommandNoResponse(opcode,curtainGroup!!.meshAddr,params)
                    }
                    "3" -> {
                        val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x15, 3, Opcode.CURTAIN_PACK_END)
                        val opcode = Opcode.CURTAIN_ON_OFF
                        TelinkLightService.Instance().sendCommandNoResponse(opcode,curtainGroup!!.meshAddr,params)
                    }
                    "4" -> {
                        val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x15, 4, Opcode.CURTAIN_PACK_END)
                        val opcode = Opcode.CURTAIN_ON_OFF
                        TelinkLightService.Instance().sendCommandNoResponse(opcode,curtainGroup!!.meshAddr,params)
                    }
                }
            }
        }else{
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
    }

    override fun onClick(v: View?) {
       when(v?.id){
           R.id.open->openWindow()
           R.id.off->offWindow()
           R.id.pause->pauseWindow()
           R.id.commutation->electricCommutation()
           R.id.setting->onceReset()
           R.id.restart->clickRestart()
           R.id.delete_Group->removeGroup()
           R.id.update_group->updateGroup()
           R.id.hand_recovery->handRecovery()
           R.id.software_restart->sofwareRestart()
           R.id.slow_up->slowUp()
       }
    }

    private fun slowUp() {
        if(type==Constant.TYPE_GROUP){
            if(slowBoolean){
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x21.toByte(), 0x01, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance().sendCommandNoResponse(opcode,curtainGroup!!.meshAddr,params)
                slowBoolean=false
                slow_up.setText(R.string.slow_up_the_cache_cancel)
            }else{
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x21.toByte(), 0x00, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance().sendCommandNoResponse(opcode,curtainGroup!!.meshAddr,params)
                slowBoolean=true
                slow_up.setText(R.string.slow_up_the_cache)
            }
        }else{
            if(slowBoolean){
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x21.toByte(), 0x01, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
                slowBoolean=false
                slow_up.setText(R.string.slow_up_the_cache_cancel)
            }else{
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x21.toByte(), 0x00, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
                slowBoolean=true
                slow_up.setText(R.string.slow_up_the_cache)
            }
        }

    }

    private fun sofwareRestart() {
        if(type==Constant.TYPE_GROUP){
            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0xEA.toByte(), 0x00, Opcode.CURTAIN_PACK_END)
            val opcode = Opcode.CURTAIN_ON_OFF
            TelinkLightService.Instance().sendCommandNoResponse(opcode,curtainGroup!!.meshAddr,params)
        }else{
            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0xEA.toByte(), 0x00, Opcode.CURTAIN_PACK_END)
            val opcode = Opcode.CURTAIN_ON_OFF
            TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
        }

    }

    private fun handRecovery() {
        if(type==Constant.TYPE_GROUP){
            if(handBoolean){
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x12, 0x01, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance().sendCommandNoResponse(opcode,curtainGroup!!.meshAddr,params)
                hand_recovery.setText(R.string.hand_cancel)
                handBoolean=false
            }else{
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x12, 0x00, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance().sendCommandNoResponse(opcode,curtainGroup!!.meshAddr,params)
                hand_recovery.setText(R.string.hand_recovery)
                handBoolean=true
            }
        }else{
            if(handBoolean){
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x12, 0x01, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
                handBoolean=false
                hand_recovery.setText(R.string.hand_cancel)
            }else{
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x12, 0x00, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
                handBoolean=true
                hand_recovery.setText(R.string.hand_recovery)
            }
        }
    }


    private fun clickRestart() {
        if(type==Constant.TYPE_GROUP){
            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0xEC.toByte(), 0x00, Opcode.CURTAIN_PACK_END)
            val opcode = Opcode.CURTAIN_ON_OFF
            TelinkLightService.Instance().sendCommandNoResponse(opcode,curtainGroup!!.meshAddr,params)
        }else{
            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0xEC.toByte(), 0x00, Opcode.CURTAIN_PACK_END)
            val opcode = Opcode.CURTAIN_ON_OFF
            TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
        }

    }

    private fun onceReset() {
        Log.d("===",type)
        if(type==Constant.TYPE_GROUP){
            val opcode = Opcode.KICK_OUT
            TelinkLightService.Instance().sendCommandNoResponse(opcode, curtainGroup!!.meshAddr, null)
            DBUtils.deleteGroupOnly(curtainGroup!!)
            Toast.makeText(this,R.string.successful_resumption,Toast.LENGTH_LONG).show()
            finish()
        }else{
            val opcode = Opcode.KICK_OUT
            TelinkLightService.Instance().sendCommandNoResponse(opcode, ctAdress!!, null)
            DBUtils.deleteCurtain(curtain!!)
            Toast.makeText(this,R.string.successful_resumption,Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun electricCommutation() {
        if(type==Constant.TYPE_GROUP){
            if(commutationBoolean){
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x11, 0x01, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance().sendCommandNoResponse(opcode,curtainGroup!!.meshAddr,params)
                commutationBoolean=false
            }else{
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x11, 0x00, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance().sendCommandNoResponse(opcode,curtainGroup!!.meshAddr,params)
                commutationBoolean=true
            }
        }else{
            if(commutationBoolean){
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x11, 0x01, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
                commutationBoolean=false
            }else{
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x11, 0x00, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
                commutationBoolean=true
            }

        }
    }


    private fun pauseWindow() {
       if(type==Constant.TYPE_GROUP){
           val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x0B, 0x00, Opcode.CURTAIN_PACK_END)
           val opcode = Opcode.CURTAIN_ON_OFF
           TelinkLightService.Instance().sendCommandNoResponse(opcode,curtainGroup!!.meshAddr,params)
       }else{
           val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x0B, 0x00, Opcode.CURTAIN_PACK_END)
           val opcode = Opcode.CURTAIN_ON_OFF
           TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
       }

    }

    private fun offWindow() {
        if(type==Constant.TYPE_GROUP){
            val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x0C, 0x00, Opcode.CURTAIN_PACK_END)
            val opcode = Opcode.CURTAIN_ON_OFF
            TelinkLightService.Instance().sendCommandNoResponse(opcode,curtainGroup!!.meshAddr,params)
        }else{
            val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x0C, 0x00, Opcode.CURTAIN_PACK_END)
            val opcode = Opcode.CURTAIN_ON_OFF
            TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
        }
    }

    private fun openWindow() {
        if(type==Constant.TYPE_GROUP){
            val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x0A, 0x00, Opcode.CURTAIN_PACK_END)
            val opcode = Opcode.CURTAIN_ON_OFF
            TelinkLightService.Instance().sendCommandNoResponse(opcode,curtainGroup!!.meshAddr,params)
        }else{
            val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x0A, 0x00, Opcode.CURTAIN_PACK_END)
            val opcode = Opcode.CURTAIN_ON_OFF
            TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
        }

    }

    /**
     * 删除组，并且把组里的灯的组也都删除。
     */
    private fun deleteGroup(lights: MutableList<DbCurtain>, group: DbGroup, retryCount: Int = 0,
                            successCallback: () -> Unit, failedCallback: () -> Unit) {
        Thread {
            if (lights.count() != 0) {
                val maxRetryCount = 3
                if (retryCount <= maxRetryCount) {
                    val light = lights[0]
                    val lightMeshAddr = light.meshAddr
                    Commander.deleteGroup(lightMeshAddr,
                            successCallback = {
                                light.belongGroupId = DBUtils.groupNull!!.id
                                DBUtils.updateCurtain(light)
                                lights.remove(light)
                                //修改分组成功后删除场景信息。
                                Thread.sleep(100)
                                if (lights.count() == 0) {
                                    //所有灯都删除了分组
                                    DBUtils.deleteGroupOnly(group)
                                    this?.runOnUiThread {
                                        successCallback.invoke()
                                    }
                                } else {
                                    //还有灯要删除分组
                                    deleteGroup(lights, group,
                                            successCallback = successCallback,
                                            failedCallback = failedCallback)
                                }
                            },
                            failedCallback = {
                                deleteGroup(lights, group, retryCount = retryCount + 1,
                                        successCallback = successCallback,
                                        failedCallback = failedCallback)
                            })
                } else {    //超过了重试次数
                    this?.runOnUiThread {
                        failedCallback.invoke()
                    }
                    LogUtils.d("retry delete group timeout")
                }
            } else {
                DBUtils.deleteGroupOnly(group)
                this?.runOnUiThread {
                    successCallback.invoke()
                }
            }
        }.start()

    }

    private fun deleteAllSceneByLightAddr(lightMeshAddr: Int) {
        val opcode = Opcode.SCENE_ADD_OR_DEL
        val params: ByteArray
        params = byteArrayOf(0x00, 0xff.toByte())
        TelinkLightService.Instance().sendCommandNoResponse(opcode, lightMeshAddr, params)
    }
}


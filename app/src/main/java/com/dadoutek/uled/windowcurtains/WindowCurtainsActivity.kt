package com.dadoutek.uled.windowcurtains

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.*
import com.android.ehorizontalselectedview.EHorizontalSelectedView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.group.CurtainGroupingActivity
import com.dadoutek.uled.group.LightGroupingActivity
import com.dadoutek.uled.intf.OtaPrepareListner
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbCurtain
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.ota.OTAUpdateSwitchActivity
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.OtaPrepareUtils
import com.dadoutek.uled.util.StringUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.TelinkApplication
import com.telink.bluetooth.light.DeviceInfo
import com.telink.util.Event
import com.telink.util.EventListener
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.android.synthetic.main.activity_window_curtains.*
import kotlinx.android.synthetic.main.fragment_device_setting.*
import kotlinx.android.synthetic.main.toolbar.*
import org.w3c.dom.Text
import java.util.*
import java.util.concurrent.TimeUnit

class WindowCurtainsActivity : TelinkBaseActivity(), EventListener<String>, View.OnClickListener {
    override fun performed(event: Event<String>?) {

    }

    private var showList: List<DbCurtain>? = null

    private var localVersion: String? = null

    private var curtain: DbCurtain? = null

    private var ctAdress: Int? = null

    private var curtainGroup: DbGroup? = null

    private var currentShowGroupSetPage = true

    private var mConnectDevice: DeviceInfo? = null

    private var compositeDisposable = CompositeDisposable()

    private var commutationBoolean: Boolean = true

    private var slowBoolean: Boolean = true

    private var handBoolean: Boolean = true

    private var type: String? = null

    private lateinit var group_delete: Button

    private lateinit var updateGroup: Button

    private lateinit var otaButton: Button

    private lateinit var versionText: TextView

    private val mDisposable = CompositeDisposable()

    private var mRxPermission: RxPermissions? = null

    private lateinit var openBtn: ImageView

    private lateinit var closeBtn: ImageView

    private lateinit var openText: TextView

    private lateinit var closeText: TextView

    private lateinit var pauseBtn: ImageView

    private lateinit var curtainImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_window_curtains)
        initView()
        initViewType()
    }

    private fun initViewType() {
        this.type = this.intent.extras!!.getString(Constant.TYPE_VIEW)
        if (type == Constant.TYPE_GROUP) {
            initGroupData()
            initViewGroup()
            initToolGroupBar()
        } else {
            currentShowGroupSetPage=false
            versionText=findViewById(R.id.versionText)
            initToolbar()
            initMeshDresData()
            getVersion()
        }
    }

    private fun initViewGroup() {
        if (curtainGroup != null) {
            if (curtainGroup!!.meshAddr == 0xffff) {
                toolbar.title = getString(R.string.allLight)
            } else {
                toolbar.title = curtainGroup?.name
            }
        }
    }

    private fun getVersion() {
        var dstAdress = 0
        if (TelinkApplication.getInstance().connectDevice != null) {
            Commander.getDeviceVersion(curtain!!.meshAddr, { s ->
                localVersion = s
//                if (txtTitle != null) {
                if (OtaPrepareUtils.instance().checkSupportOta(localVersion)!!) {
                    versionText.text = resources.getString(R.string.firmware_version, localVersion)
                    curtain!!.version = localVersion
                    this.versionText.visibility = View.VISIBLE
//                        tvOta!!.visibility = View.VISIBLE
                } else {
                    versionText.text = resources.getString(R.string.firmware_version, localVersion)
                    curtain!!.version = localVersion
                    this.versionText.visibility = View.VISIBLE
//                        tvOta!!.visibility = View.GONE
                }
//                }
                null
            }, {
                if (txtTitle != null) {
//                    txtTitle!!.visibility = View.GONE
//                    tvOta!!.visibility = View.GONE
                }
                null
            })
        } else {
            dstAdress = 0
        }
    }

    private fun initToolGroupBar() {
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
        toolbar.title = curtain?.name
    }

    private fun initGroupData() {
        this.curtainGroup = this.intent.extras!!.get("group") as DbGroup
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

    private val menuItemClickListener = Toolbar.OnMenuItemClickListener { item ->
        when (item?.itemId) {
            R.id.toolbar_delete_group -> {
                removeGroup()
            }
            R.id.toolbar_rename_group -> {
                renameGp()
            }
            R.id.toolbar_rename_light -> {
                renameLight()
            }
            R.id.toolbar_reset -> {
                onceReset()
            }
            R.id.toolbar_update_group -> {
                updateGroup()
            }
            R.id.toolbar_commutation->{
                electricCommutation()
            }
            R.id.toolbar_hand_recovery->{
                handRecovery()
            }
            R.id.toolbar_restart->{
                clickRestart()
            }
            R.id.toolbar_software_restart->{
                sofwareRestart()
            }
            R.id.toolbar_slow_up->{
                slowUp()
            }
            R.id.toolbar_ota->{
                updateOTA()
            }
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
        intent.putExtra(Constant.TYPE_VIEW, Constant.CURTAINS_KEY)
        intent.putExtra("gpAddress", ctAdress)
        intent.putExtra("uuid", curtain!!.productUUID)
        intent.putExtra("belongId", curtain!!.belongGroupId)
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
                        curtain?.name = textGp.text.toString().trim { it <= ' ' }
                        DBUtils.updateCurtain(curtain!!)
                        toolbar.title = curtain?.name
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (currentShowGroupSetPage) {
            getMenuInflater().inflate(R.menu.menu_curtain_group, menu)
        } else {
            getMenuInflater().inflate(R.menu.menu_curtain_setting, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if(handBoolean){
            if (menu != null) {
                menu.findItem(R.id.toolbar_hand_recovery).setTitle(R.string.hand_recovery)
            }
//            handBoolean=false
        }else{
            if (menu != null) {
                menu.findItem(R.id.toolbar_hand_recovery).setTitle(R.string.hand_cancel)
            }
//            handBoolean=true
        }

        if(slowBoolean){
            if(menu!=null){
               menu.findItem(R.id.toolbar_slow_up).setTitle(R.string.slow_up_the_cache)
            }
        }else{
            if(menu!=null){
                menu.findItem(R.id.toolbar_slow_up).setTitle(R.string.slow_up_the_cache_cancel)
            }
        }


        return super.onPrepareOptionsMenu(menu)
    }

    private fun initView() {
        openBtn = findViewById(R.id.open)
        openText = findViewById(R.id.open_text)
        closeBtn = findViewById(R.id.off)
        closeText = findViewById(R.id.off_text)
        pauseBtn = findViewById(R.id.pause)
        curtainImage = findViewById(R.id.curtain)
        open.setOnClickListener(this)
        off.setOnClickListener(this)
        pause.setOnClickListener(this)
        setSpeed()
    }

    private fun setSpeed() {
        val indicatorSeekBar = findViewById<View>(R.id.indicatorSeekBar) as IndicatorSeekBar

        if(type==Constant.TYPE_GROUP){
            indicatorSeekBar.onSeekChangeListener = object : OnSeekChangeListener {
                override fun onSeeking(seekParams: SeekParams) {
                    val i = seekParams.progress
                    Log.e("TAG", i.toString())
                    if(i==1){
                        val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x15, 1, Opcode.CURTAIN_PACK_END)
                        val opcode = Opcode.CURTAIN_ON_OFF
                        TelinkLightService.Instance().sendCommandNoResponse(opcode, curtainGroup!!.meshAddr,params)
                    }else if(i==2){
                        val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x15, 2, Opcode.CURTAIN_PACK_END)
                        val opcode = Opcode.CURTAIN_ON_OFF
                        TelinkLightService.Instance().sendCommandNoResponse(opcode,curtainGroup!!.meshAddr,params)
                    }else if(i==3){
                        val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x15, 3, Opcode.CURTAIN_PACK_END)
                        val opcode = Opcode.CURTAIN_ON_OFF
                        TelinkLightService.Instance().sendCommandNoResponse(opcode,curtainGroup!!.meshAddr,params)
                    }else if(i==4){
                        val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x15, 4, Opcode.CURTAIN_PACK_END)
                        val opcode = Opcode.CURTAIN_ON_OFF
                        TelinkLightService.Instance().sendCommandNoResponse(opcode,curtainGroup!!.meshAddr,params)
                    }
                }

                override fun onStartTrackingTouch(seekBar: IndicatorSeekBar) {

                }

                override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {

                }
            }
        }else{
            indicatorSeekBar.onSeekChangeListener = object : OnSeekChangeListener {
                override fun onSeeking(seekParams: SeekParams) {
                    val i = seekParams.progress
                    Log.e("TAG", i.toString())
                    if(i==1){
                        val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x15, 1, Opcode.CURTAIN_PACK_END)
                        val opcode = Opcode.CURTAIN_ON_OFF
                        TelinkLightService.Instance().sendCommandNoResponse(opcode, ctAdress!!,params)
                    }else if(i==2){
                        val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x15, 2, Opcode.CURTAIN_PACK_END)
                        val opcode = Opcode.CURTAIN_ON_OFF
                        TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
                    }else if(i==3){
                        val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x15, 3, Opcode.CURTAIN_PACK_END)
                        val opcode = Opcode.CURTAIN_ON_OFF
                        TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
                    }else if(i==4){
                        val params = byteArrayOf(Opcode.CURTAIN_PACK_START,0x15, 4, Opcode.CURTAIN_PACK_END)
                        val opcode = Opcode.CURTAIN_ON_OFF
                        TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
                    }
                }

                override fun onStartTrackingTouch(seekBar: IndicatorSeekBar) {

                }

                override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {

                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.open -> openWindow()
            R.id.off -> offWindow()
            R.id.pause -> pauseWindow()
        }
    }

    private fun updateOTA() {
        if(versionText.text.toString()!=null){
            checkPermission()
        }
    }

    private fun checkPermission() {
        mDisposable.add(
                mRxPermission!!.request(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe { granted ->
                    if (granted!!) {
                        var isBoolean: Boolean = SharedPreferencesHelper.getBoolean(TelinkLightApplication.getInstance(), Constant.IS_DEVELOPER_MODE, false)
                        if (isBoolean) {
                            transformView()
                        } else {
                            OtaPrepareUtils.instance().gotoUpdateView(this@WindowCurtainsActivity, localVersion, otaPrepareListner)
                        }
                    } else {
                        ToastUtils.showLong(R.string.update_permission_tip)
                    }
                })
//        }
    }

    internal var otaPrepareListner: OtaPrepareListner = object : OtaPrepareListner {

        override fun downLoadFileStart() {
            showLoadingDialog(getString(R.string.get_update_file))
        }

        override fun startGetVersion() {
            showLoadingDialog(getString(R.string.verification_version))
        }

        override fun getVersionSuccess(s: String) {
            //            ToastUtils.showLong(.string.verification_version_success);
            hideLoadingDialog()
        }

        override fun getVersionFail() {
            ToastUtils.showLong(R.string.verification_version_fail)
            hideLoadingDialog()
        }


        override fun downLoadFileSuccess() {
            hideLoadingDialog()
            transformView()
        }

        override fun downLoadFileFail(message: String) {
            hideLoadingDialog()
            ToastUtils.showLong(R.string.download_pack_fail)
        }
    }

    private fun transformView() {
        val intent = Intent(this@WindowCurtainsActivity, OTAUpdateSwitchActivity::class.java)
        intent.putExtra(Constant.UPDATE_LIGHT, curtain)
        startActivity(intent)
        finish()
    }

    private fun slowUp() {
        if(type==Constant.TYPE_GROUP){
            if(slowBoolean){
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x21.toByte(), 0x01, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance().sendCommandNoResponse(opcode,curtainGroup!!.meshAddr,params)
                slowBoolean=false
//                slow_up.setText(R.string.slow_up_the_cache_cancel)
            }else{
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x21.toByte(), 0x00, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance().sendCommandNoResponse(opcode,curtainGroup!!.meshAddr,params)
                slowBoolean=true
//                slow_up.setText(R.string.slow_up_the_cache)
            }
        }else{
            if(slowBoolean){
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x21.toByte(), 0x01, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
                slowBoolean=false
//                slow_up.setText(R.string.slow_up_the_cache_cancel)
            }else{
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x21.toByte(), 0x00, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
                slowBoolean=true
//                slow_up.setText(R.string.slow_up_the_cache)
            }
        }

    }

    private fun sofwareRestart() {
        if (type == Constant.TYPE_GROUP) {
            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0xEA.toByte(), 0x00, Opcode.CURTAIN_PACK_END)
            val opcode = Opcode.CURTAIN_ON_OFF
            TelinkLightService.Instance().sendCommandNoResponse(opcode, curtainGroup!!.meshAddr, params)
        } else {
            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0xEA.toByte(), 0x00, Opcode.CURTAIN_PACK_END)
            val opcode = Opcode.CURTAIN_ON_OFF
            TelinkLightService.Instance().sendCommandNoResponse(opcode, ctAdress!!, params)
        }

    }

    private fun handRecovery() {
        if(type==Constant.TYPE_GROUP){
            if(handBoolean){
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x12, 0x01, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance().sendCommandNoResponse(opcode,curtainGroup!!.meshAddr,params)
//                hand_recovery.setText(R.string.hand_cancel)
                handBoolean=false
            }else{
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x12, 0x00, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance().sendCommandNoResponse(opcode,curtainGroup!!.meshAddr,params)
//                hand_recovery.setText(R.string.hand_recovery)
                handBoolean=true
            }
        }else{
            if(handBoolean){
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x12, 0x01, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
                handBoolean=false
//                hand_recovery.setText(R.string.hand_cancel)
            }else{
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x12, 0x00, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance().sendCommandNoResponse(opcode,ctAdress!!,params)
                handBoolean=true
//                hand_recovery.setText(R.string.hand_recovery)
            }
        }
    }


    private fun clickRestart() {
        if (type == Constant.TYPE_GROUP) {
            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0xEC.toByte(), 0x00, Opcode.CURTAIN_PACK_END)
            val opcode = Opcode.CURTAIN_ON_OFF
            TelinkLightService.Instance().sendCommandNoResponse(opcode, curtainGroup!!.meshAddr, params)
        } else {
            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0xEC.toByte(), 0x00, Opcode.CURTAIN_PACK_END)
            val opcode = Opcode.CURTAIN_ON_OFF
            TelinkLightService.Instance().sendCommandNoResponse(opcode, ctAdress!!, params)
        }

    }

    private fun onceReset() {
        if (type == Constant.TYPE_GROUP) {
            val opcode = Opcode.KICK_OUT
            TelinkLightService.Instance().sendCommandNoResponse(opcode, curtainGroup!!.meshAddr, null)
            DBUtils.deleteGroupOnly(curtainGroup!!)
            Toast.makeText(this, R.string.successful_resumption, Toast.LENGTH_LONG).show()
            finish()
        } else {
            val opcode = Opcode.KICK_OUT
            TelinkLightService.Instance().sendCommandNoResponse(opcode, ctAdress!!, null)
            DBUtils.deleteCurtain(curtain!!)
            Toast.makeText(this, R.string.successful_resumption, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun electricCommutation() {
        if (type == Constant.TYPE_GROUP) {
            if (commutationBoolean) {
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x11, 0x01, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance().sendCommandNoResponse(opcode, curtainGroup!!.meshAddr, params)
                commutationBoolean = false
            } else {
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x11, 0x00, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance().sendCommandNoResponse(opcode, curtainGroup!!.meshAddr, params)
                commutationBoolean = true
            }
        } else {
            if (commutationBoolean) {
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x11, 0x01, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance().sendCommandNoResponse(opcode, ctAdress!!, params)
                commutationBoolean = false
            } else {
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x11, 0x00, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance().sendCommandNoResponse(opcode, ctAdress!!, params)
                commutationBoolean = true
            }

        }
    }


    private fun pauseWindow() {
        if (type == Constant.TYPE_GROUP) {
            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x0B, 0x00, Opcode.CURTAIN_PACK_END)
            val opcode = Opcode.CURTAIN_ON_OFF
            TelinkLightService.Instance().sendCommandNoResponse(opcode, curtainGroup!!.meshAddr, params)
            pauseBtn.setImageResource(R.drawable.icon_suspend_pre)
            closeBtn.setImageResource(R.drawable.icon_open)
            closeText.setTextColor(Color.parseColor("#333333"))
            openBtn.setImageResource(R.drawable.icon_open)
            openText.setTextColor(Color.parseColor("#333333"))
        } else {
            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x0B, 0x00, Opcode.CURTAIN_PACK_END)
            val opcode = Opcode.CURTAIN_ON_OFF
            TelinkLightService.Instance().sendCommandNoResponse(opcode, ctAdress!!, params)
            pauseBtn.setImageResource(R.drawable.icon_suspend_pre)
            closeBtn.setImageResource(R.drawable.icon_open)
            closeText.setTextColor(Color.parseColor("#333333"))
            openBtn.setImageResource(R.drawable.icon_open)
            openText.setTextColor(Color.parseColor("#333333"))
        }

    }

    private fun offWindow() {
        if (type == Constant.TYPE_GROUP) {
            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x0C, 0x00, Opcode.CURTAIN_PACK_END)
            val opcode = Opcode.CURTAIN_ON_OFF
            TelinkLightService.Instance().sendCommandNoResponse(opcode, curtainGroup!!.meshAddr, params)
            curtainImage.setImageResource(R.drawable.curtain_close)
            pauseBtn.setImageResource(R.drawable.icon_suspend)
            closeBtn.setImageResource(R.drawable.icon_open_yes)
            closeText.setTextColor(Color.parseColor("#0080EA"))
            openBtn.setImageResource(R.drawable.icon_open)
            openText.setTextColor(Color.parseColor("#333333"))
        } else {
            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x0C, 0x00, Opcode.CURTAIN_PACK_END)
            val opcode = Opcode.CURTAIN_ON_OFF
            TelinkLightService.Instance().sendCommandNoResponse(opcode, ctAdress!!, params)
            curtainImage.setImageResource(R.drawable.curtain_close)
            pauseBtn.setImageResource(R.drawable.icon_suspend)
            closeBtn.setImageResource(R.drawable.icon_open_yes)
            closeText.setTextColor(Color.parseColor("#0080EA"))
            openBtn.setImageResource(R.drawable.icon_open)
            openText.setTextColor(Color.parseColor("#333333"))
        }
    }

    private fun openWindow() {
        if (type == Constant.TYPE_GROUP) {
            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x0A, 0x00, Opcode.CURTAIN_PACK_END)
            val opcode = Opcode.CURTAIN_ON_OFF
            TelinkLightService.Instance().sendCommandNoResponse(opcode, curtainGroup!!.meshAddr, params)
            curtainImage.setImageResource(R.drawable.curtain)
            pauseBtn.setImageResource(R.drawable.icon_suspend)
            openBtn.setImageResource(R.drawable.icon_open_yes)
            openText.setTextColor(Color.parseColor("#0080EA"))
            closeBtn.setImageResource(R.drawable.icon_open)
            closeText.setTextColor(Color.parseColor("#333333"))
        } else {
            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x0A, 0x00, Opcode.CURTAIN_PACK_END)
            val opcode = Opcode.CURTAIN_ON_OFF
            TelinkLightService.Instance().sendCommandNoResponse(opcode, ctAdress!!, params)
            curtainImage.setImageResource(R.drawable.curtain)
            pauseBtn.setImageResource(R.drawable.icon_suspend)
            openBtn.setImageResource(R.drawable.icon_open_yes)
            openText.setTextColor(Color.parseColor("#0080EA"))
            closeBtn.setImageResource(R.drawable.icon_open)
            closeText.setTextColor(Color.parseColor("#333333"))
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

    override fun onDestroy() {
        super.onDestroy()
        mDisposable.dispose()
    }
}

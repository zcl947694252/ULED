package com.dadoutek.uled.curtains

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.intf.OtaPrepareListner
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbCurtain
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.switches.ChooseGroupOrSceneActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.OtaPrepareUtils
import com.dadoutek.uled.util.StringUtils
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.TelinkApplication
import com.telink.bluetooth.light.DeviceInfo
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_window_curtains.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

class WindowCurtainsActivity : TelinkBaseActivity(), View.OnClickListener {
    private var fiVersion: MenuItem? = null
    private val requestCodeNum: Int = 1000
    private var disposable: Disposable? = null
    private var mConnectDeviceDisposable: Disposable? = null
    private var localVersion: String? = null
    private var curtain: DbCurtain? = null
    private var ctAdress: Int? = null
    private var curtainGroup: DbGroup? = null
    private var currentShowGroupSetPage = true
    private var mConnectDevice: DeviceInfo? = null
    private var commutationBoolean: Boolean = true
    private var slowBoolean: Boolean = true
    private var handBoolean: Boolean = true
    private var typeStr: String? = null
    private val mDisposable = CompositeDisposable()
    private var mRxPermission: RxPermissions? = null
    private lateinit var openBtn: ImageView
    private lateinit var closeBtn: ImageView
    private lateinit var openText: TextView
    private lateinit var closeText: TextView
    private lateinit var pauseBtn: ImageView
    private lateinit var curtainImage: ImageView
    private lateinit var indicatorSeekBar: IndicatorSeekBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_window_curtains)
        initViewType()
        initView()
    }

    private fun initViewType() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setOnMenuItemClickListener(menuItemClickListener)
        toolbar.setNavigationOnClickListener { finish() }
        val moreIcon = ContextCompat.getDrawable(toolbar.context, R.drawable.abc_ic_menu_overflow_material)
        if (moreIcon != null) {
            moreIcon.setColorFilter(ContextCompat.getColor(toolbar.context, R.color.black), PorterDuff.Mode.SRC_ATOP)
            toolbar.overflowIcon = moreIcon
        }
        this.typeStr = this.intent.extras!!.getString(Constant.TYPE_VIEW)
        if (typeStr == Constant.TYPE_GROUP) {
            this.curtainGroup = this.intent.extras!!.get("group") as DbGroup
            if (curtainGroup != null)
                when (curtainGroup!!.meshAddr) {
                    0xffff -> toolbarTv.text = getString(R.string.allLight)
                    else -> toolbarTv.text = curtainGroup?.name
                }

            img_function1.setImageResource(R.drawable.icon_editor)
            img_function1.visibility = View.VISIBLE
            img_function1.setOnClickListener {
                renameGp()
            }
        } else {
            img_function1.visibility = View.GONE
            currentShowGroupSetPage = false
            initMeshDresData()
            getVersion()
        }
    }


    private fun getVersion() {
        if (TelinkApplication.getInstance().connectDevice != null) {
            Log.e("TAG", curtain!!.meshAddr.toString())
            val disposable = Commander.getDeviceVersion(curtain!!.meshAddr)
                    .subscribe({ s ->
                        localVersion = s
                        if (localVersion != "") {
                            if (versionText != null) {
                                if (OtaPrepareUtils.instance().checkSupportOta(localVersion)!!) {
                                    versionText.text = resources.getString(R.string.firmware_version) + localVersion
                                    curtain!!.version = localVersion
                                    this.versionText.visibility = View.GONE
                                } else {
                                    versionText.text = resources.getString(R.string.firmware_version) + localVersion
                                    curtain!!.version = localVersion
                                    this.versionText.visibility = View.GONE
                                }
                                DBUtils.saveCurtain(curtain!!, false)
                            }
                        }
                        null
                    }, {
                        LogUtils.d(it)
                    })
        }
    }


    private fun initMeshDresData() {
        this.ctAdress = this.intent.getIntExtra(Constant.CURTAINS_ARESS_KEY, 0)
        this.curtain = this.intent.extras!!.get(Constant.LIGHT_ARESS_KEY) as DbCurtain
        versionText.text = ""
        toolbarTv.text = curtain?.name
    }

    private val menuItemClickListener = Toolbar.OnMenuItemClickListener { item ->
        DBUtils.lastUser?.let {
            if (it.id.toString() != it.last_authorizer_user_id) {
                ToastUtils.showLong(getString(R.string.author_region_warm))
            } else {
                when (item?.itemId) {
                    R.id.toolbar_batch_gp -> removeGroup()
                    R.id.toolbar_on_line -> renameGp()
                    R.id.toolbar_c_rename -> renameLight()
                    R.id.toolbar_c_factory -> /*onceReset()*/remove()
                    R.id.toolbar_c_change_group -> updateGroup()
                    R.id.toolbar_c_commutation -> electricCommutation()
                    R.id.toolbar_c_hand_recovery -> handRecovery()
                    R.id.toolbar_c_software_restart -> sofwareRestart()
                    R.id.toolbar_c_slow_up -> slowUp()
                    R.id.toolbar_c_ota -> updateOTA()
                }
            }
        }
        true
    }

    private fun renameGp() {
        if (!TextUtils.isEmpty(curtainGroup?.name))
            renameEt?.setText(curtainGroup?.name)
        renameEt?.setSelection(renameEt?.text.toString().length)

        if (this != null && !this.isFinishing) {
            renameDialog?.dismiss()
            renameDialog?.show()
        }

        renameConfirm?.setOnClickListener {    // 获取输入框的内容
            if (StringUtils.compileExChar(renameEt?.text.toString().trim { it <= ' ' })) {
                ToastUtils.showLong(getString(R.string.rename_tip_check))
            } else {
                var name = renameEt?.text.toString().trim { it <= ' ' }
                var canSave = true
                val groups = DBUtils.allGroups
                for (i in groups.indices) {
                    if (groups[i].name == name) {
                        ToastUtils.showLong(TelinkLightApplication.getApp().getString(R.string.repeat_name))
                        canSave = false
                        break
                    }
                }

                if (canSave) {
                    curtainGroup?.name = renameEt?.text.toString().trim { it <= ' ' }
                    DBUtils.updateGroup(curtainGroup!!)
                    toolbarTv.text = curtainGroup?.name
                    renameDialog.dismiss()
                }
            }
        }
    }


    private fun removeGroup() {
        AlertDialog.Builder(Objects.requireNonNull<FragmentActivity>(this))
                .setMessage(getString(R.string.delete_group_confirm, curtainGroup?.name))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    showLoadingDialog(getString(R.string.deleting))
                    deleteGroup(DBUtils.getCurtainByGroupID(curtainGroup!!.id), curtainGroup!!,
                            successCallback = {
                                this.hideLoadingDialog()
                                this.setResult(Constant.RESULT_OK)
                                this.finish()
                            },
                            failedCallback = {
                                this.hideLoadingDialog()
                                ToastUtils.showLong(R.string.move_out_some_lights_in_group_failed)
                            })
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
    }

    private fun updateGroup() {//更新分组 断开提示
        val intent = Intent(this@WindowCurtainsActivity, ChooseGroupOrSceneActivity::class.java)
        val bundle = Bundle()
        bundle.putInt(Constant.EIGHT_SWITCH_TYPE, 0)//传入0代表是群组
        bundle.putInt(Constant.DEVICE_TYPE, Constant.DEVICE_TYPE_CURTAIN.toInt())
        intent.putExtras(bundle)
        startActivityForResult(intent, requestCodeNum)
        setResult(Constant.RESULT_OK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == requestCodeNum) {
            var group = data?.getSerializableExtra(Constant.EIGHT_SWITCH_TYPE) as DbGroup
            updateGroupResult(curtain!!, group)
        }
    }

    private fun updateGroupResult(light: DbCurtain, group: DbGroup) {
        if (group != null) {
            if (TelinkApplication.getInstance().connectDevice == null) {
                ToastUtils.showLong(R.string.group_fail)
            } else {
                /* showLoadingDialog(getString(R.string.grouping))
                 Thread {
                     val sceneIds = getRelatedSceneIds(group.meshAddr)
                     for (i in 0..1) {
                         deletePreGroup(curtain!!.meshAddr)
                         Thread.sleep(100)
                     }
                     for (i in 0..1) {
                         deleteAllSceneByLightAddr(curtain!!.meshAddr)
                         Thread.sleep(100)
                     }
                     for (i in 0..1) {
                         allocDeviceGroup(group)
                         Thread.sleep(100)
                     }
                     for (sceneId in sceneIds) {
                         val action = DBUtils.getActionBySceneId(sceneId, group.meshAddr)
                         if (action != null) {
                             for (i in 0..1) {
                                 Commander.addScene(sceneId, curtain!!.meshAddr, action.color)
                                 Thread.sleep(100)
                             }
                         }
                     }*/

                Commander.addGroup(light.meshAddr, group.meshAddr, {
                    group.deviceType = curtain!!.productUUID.toLong()
                    DBUtils.updateGroup(group)

                    light.hasGroup = true
                    light.belongGroupId = group.id
                    light.name = light.name
                    DBUtils.updateCurtain(light)
                    ToastUtils.showShort(getString(R.string.grouping_success_tip))
                    finish()
                }, {
                    ToastUtils.showShort(getString(R.string.grouping_fail))
                })
            }
        }
    }

    /**
     * start to group
     *  设置设备分组
     */
    private fun allocDeviceGroup(group: DbGroup) {
        val groupAddress = group.meshAddr
        val dstAddress = curtain!!.meshAddr
        val opcode = 0xD7.toByte()
        val params = byteArrayOf(0x01, (groupAddress and 0xFF).toByte(), (groupAddress shr 8 and 0xFF).toByte())
        params[0] = 0x01
        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, dstAddress, params)
        curtain!!.belongGroupId = group.id
    }
    /**
     * 删除指定灯的之前的分组
     * @param lightMeshAddr 灯的mesh地址
     */
    private fun deletePreGroup(lightMeshAddr: Int) {
        if (DBUtils.getGroupByID(curtain!!.belongGroupId!!) != null) {
            val groupAddress = DBUtils.getGroupByID(curtain!!.belongGroupId!!)?.meshAddr
            val opcode = Opcode.SET_GROUP
            val params = byteArrayOf(0x00, (groupAddress!! and 0xFF).toByte(), //0x00表示删除组
                    (groupAddress shr 8 and 0xFF).toByte())
            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, lightMeshAddr, params)
        }
    }

    private fun getRelatedSceneIds(groupAddress: Int): List<Long> {
        val sceneIds = ArrayList<Long>()
        val dbSceneList = DBUtils.sceneList
        sceneLoop@ for (dbScene in dbSceneList) {
            val dbActions = DBUtils.getActionsBySceneId(dbScene.id)
            for (action in dbActions) {
                if (groupAddress == action.groupAddr || 0xffff == action.groupAddr) {
                    sceneIds.add(dbScene.id)
                    continue@sceneLoop
                }
            }
        }
        return sceneIds
    }

    private fun renameLight() {
        if (!TextUtils.isEmpty(curtain?.name))
            renameEt?.setText(curtain?.name)
        renameEt?.setSelection(renameEt?.text.toString().length)

        if (this != null && !this.isFinishing) {
            renameDialog?.dismiss()
            renameDialog?.show()
        }

        renameConfirm?.setOnClickListener {    // 获取输入框的内容
            if (StringUtils.compileExChar(renameEt?.text.toString().trim { it <= ' ' })) {
                ToastUtils.showLong(getString(R.string.rename_tip_check))
            } else {
                curtain?.name = renameEt?.text.toString().trim { it <= ' ' }
                DBUtils.updateCurtain(curtain!!)
                toolbarTv.text = curtain?.name
                renameDialog.dismiss()
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (currentShowGroupSetPage) {
            //menuInflater.inflate(R.menu.menu_curtain_group, menu)
            // menuInflater.inflate(R.menu.menu_rgb_group_setting, menu)
            //toolbar.menu?.findItem(R.id.toolbar_batch_gp)?.isVisible = false
            //toolbar.menu?.findItem(R.id.toolbar_delete_device)?.isVisible = false
        } else {
            menuInflater.inflate(R.menu.menu_curtain_setting, menu)
            fiVersion = menu?.findItem(R.id.toolbar_c_version)
            if (TextUtils.isEmpty(localVersion))
                localVersion = getString(R.string.number_no)
            fiVersion?.title = localVersion
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (handBoolean) {
            if (currentShowGroupSetPage) {
                menu?.findItem(R.id.toolbar_c_hand_recovery)?.setTitle(R.string.hand_recovery)
            } else {
                menu?.findItem(R.id.toolbar_c_hand_recovery)?.setTitle(R.string.hand_recovery)
            }
        } else {
            if (currentShowGroupSetPage) {
                menu?.findItem(R.id.toolbar_c_hand_recovery)?.setTitle(R.string.hand_cancel)
            } else {
                menu?.findItem(R.id.toolbar_c_hand_recovery)?.setTitle(R.string.hand_cancel)
            }
        }

        if (slowBoolean) {
            if (currentShowGroupSetPage) {
                menu?.findItem(R.id.toolbar_c_slow_up)?.setTitle(R.string.slow_up_the_cache)
            } else {
                menu?.findItem(R.id.toolbar_c_slow_up)?.setTitle(R.string.slow_up_the_cache)
            }

        } else {
            if (currentShowGroupSetPage) {
                menu?.findItem(R.id.toolbar_c_slow_up)?.setTitle(R.string.slow_up_the_cache_cancel)
            } else {
                menu?.findItem(R.id.toolbar_c_slow_up)?.setTitle(R.string.slow_up_the_cache_cancel)
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
        indicatorSeekBar = findViewById(R.id.indicatorSeekBar)
        open.setOnClickListener(this)
        off.setOnClickListener(this)
        pause.setOnClickListener(this)
        setSpeed()

        if (!currentShowGroupSetPage) {

            if (curtain!!.status != null) {
                when (curtain!!.status) {
                    0 -> {
                        pauseBtn.setImageResource(R.drawable.icon_suspend_pre)
                        closeBtn.setImageResource(R.drawable.icon_curtain_close)
                        closeText.setTextColor(Color.parseColor("#333333"))
                        openBtn.setImageResource(R.drawable.icon_curtain_close)
                        openText.setTextColor(Color.parseColor("#333333"))
                    }
                    1 -> {
                        curtainImage.setImageResource(R.drawable.curtain_close)
                        pauseBtn.setImageResource(R.drawable.icon_suspend)
                        closeBtn.setImageResource(R.drawable.icon_open_yes)
                        closeText.setTextColor(Color.parseColor("#0080EA"))
                        openBtn.setImageResource(R.drawable.icon_curtain_close)
                        openText.setTextColor(Color.parseColor("#333333"))
                    }
                    2 -> {
                        curtainImage.setImageResource(R.drawable.curtain)
                        pauseBtn.setImageResource(R.drawable.icon_suspend)
                        openBtn.setImageResource(R.drawable.icon_open_yes)
                        openText.setTextColor(Color.parseColor("#0080EA"))
                        closeBtn.setImageResource(R.drawable.icon_curtain_close)
                        closeText.setTextColor(Color.parseColor("#333333"))
                    }
                }
            }

            if (curtain!!.inverse != null)
                curtain!!.inverse = commutationBoolean

            if (curtain!!.closeSlowStart != null)
                curtain!!.closeSlowStart = slowBoolean

            if (curtain!!.closePull != null)
                curtain!!.closePull = handBoolean

            if (curtain!!.speed != null)
                indicatorSeekBar.setProgress(curtain!!.speed.toFloat())
        }
    }

    private fun setSpeed() {
        if (typeStr == Constant.TYPE_GROUP) {
            indicatorSeekBar.onSeekChangeListener = object : OnSeekChangeListener {
                override fun onSeeking(seekParams: SeekParams) {
                    val i = seekParams.progress
                    Log.e("TAG", i.toString())
                    when (i) {
                        1 -> {
                            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x15, 1, Opcode.CURTAIN_PACK_END)
                            val opcode = Opcode.CURTAIN_ON_OFF
                            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, curtainGroup!!.meshAddr, params)
                        }
                        2 -> {
                            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x15, 2, Opcode.CURTAIN_PACK_END)
                            val opcode = Opcode.CURTAIN_ON_OFF
                            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, curtainGroup!!.meshAddr, params)
                        }
                        3 -> {
                            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x15, 3, Opcode.CURTAIN_PACK_END)
                            val opcode = Opcode.CURTAIN_ON_OFF
                            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, curtainGroup!!.meshAddr, params)
                        }
                        4 -> {
                            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x15, 4, Opcode.CURTAIN_PACK_END)
                            val opcode = Opcode.CURTAIN_ON_OFF
                            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, curtainGroup!!.meshAddr, params)
                        }
                    }
                }

                override fun onStartTrackingTouch(seekBar: IndicatorSeekBar) {}

                override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {}
            }
        } else {
            indicatorSeekBar.onSeekChangeListener = object : OnSeekChangeListener {
                override fun onSeeking(seekParams: SeekParams) {
                    val i = seekParams.progress
                    Log.e("TAG", i.toString())
                    when (i) {
                        1 -> {
                            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x15, 1, Opcode.CURTAIN_PACK_END)
                            val opcode = Opcode.CURTAIN_ON_OFF
                            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, ctAdress!!, params)
                            curtain!!.speed = 1
                            DBUtils.updateCurtain(curtain!!)
                        }
                        2 -> {
                            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x15, 2, Opcode.CURTAIN_PACK_END)
                            val opcode = Opcode.CURTAIN_ON_OFF
                            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, ctAdress!!, params)
                            curtain!!.speed = 2
                            DBUtils.updateCurtain(curtain!!)
                        }
                        3 -> {
                            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x15, 3, Opcode.CURTAIN_PACK_END)
                            val opcode = Opcode.CURTAIN_ON_OFF
                            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, ctAdress!!, params)
                            curtain!!.speed = 3
                            DBUtils.updateCurtain(curtain!!)
                        }
                        4 -> {
                            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x15, 4, Opcode.CURTAIN_PACK_END)
                            val opcode = Opcode.CURTAIN_ON_OFF
                            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, ctAdress!!, params)
                            curtain!!.speed = 4
                            DBUtils.updateCurtain(curtain!!)
                        }
                    }
                }

                override fun onStartTrackingTouch(seekBar: IndicatorSeekBar) {}

                override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {}
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
        when {
            versionText.text != null && versionText.text != "" -> checkPermission()
            else -> Toast.makeText(this, R.string.number_no, Toast.LENGTH_LONG).show()
        }
    }

    private fun checkPermission() {
        mRxPermission = RxPermissions(this)
        mDisposable.add(
                mRxPermission!!.request(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe { granted ->
                    if (granted!!) {
                        disposable = Commander.getDeviceVersion(curtain!!.meshAddr)
                                .subscribe(
                                        { s ->
                                            hideLoadingDialog()
                                            if (OtaPrepareUtils.instance().checkSupportOta(s)!!) {
                                                curtain!!.version = s
                                                isDirectConnectDevice()
                                            } else
                                                ToastUtils.showLong(getString(R.string.version_disabled))
                                        }, { hideLoadingDialog() }
                                )
                    } else {
                        ToastUtils.showLong(R.string.update_permission_tip)
                    }
                })
    }

    private fun isDirectConnectDevice() {
        var isBoolean: Boolean = SharedPreferencesHelper.getBoolean(TelinkLightApplication.getApp(), Constant.IS_DEVELOPER_MODE, false)
        if (TelinkLightApplication.getApp().connectDevice != null && TelinkLightApplication.getApp().connectDevice.meshAddress == curtain?.meshAddr) {
            when {
                isBoolean -> transformView()
                else -> OtaPrepareUtils.instance().gotoUpdateView(this@WindowCurtainsActivity, localVersion, otaPrepareListner)
            }
        } else {
            showLoadingDialog(getString(R.string.please_wait))
            TelinkLightService.Instance()?.idleMode(true)
            mConnectDeviceDisposable = Observable.timer(800, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap {
                        connect(curtain!!.meshAddr, macAddress = curtain!!.macAddr)
                    }
                    ?.subscribe(
                            {
                                hideLoadingDialog()
                                if (isBoolean)
                                    transformView()
                                else
                                    OtaPrepareUtils.instance().gotoUpdateView(this@WindowCurtainsActivity, localVersion, otaPrepareListner)
                            }
                            ,
                            {
                                hideLoadingDialog()
                                runOnUiThread { ToastUtils.showLong(R.string.connect_fail2) }
                                LogUtils.d(it)
                            })
        }
    }

    private var otaPrepareListner: OtaPrepareListner = object : OtaPrepareListner {

        override fun downLoadFileStart() {
            showLoadingDialog(getString(R.string.get_update_file))
        }

        override fun startGetVersion() {
            showLoadingDialog(getString(R.string.verification_version))
        }

        override fun getVersionSuccess(s: String) {
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
        mConnectDeviceDisposable?.dispose()
        disposable?.dispose()
        val intent = Intent(this@WindowCurtainsActivity, OTAUpdateActivity::class.java)
        intent.putExtra(Constant.OTA_MAC, curtain?.macAddr)
        intent.putExtra(Constant.OTA_MES_Add, curtain?.meshAddr)
        intent.putExtra(Constant.OTA_VERSION, curtain?.version)
        intent.putExtra(Constant.OTA_TYPE, DeviceType.SMART_CURTAIN)
        startActivity(intent)
        finish()
    }

    private fun slowUp() {
        if (typeStr == Constant.TYPE_GROUP) {
            slowBoolean = if (slowBoolean) {
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x21.toByte(), 0x01, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, curtainGroup!!.meshAddr, params)
                false
            } else {
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x21.toByte(), 0x00, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, curtainGroup!!.meshAddr, params)
                true
            }
        } else {
            if (slowBoolean) {
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x21.toByte(), 0x01, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, ctAdress!!, params)
                slowBoolean = false
                curtain!!.closeSlowStart = true
                DBUtils.updateCurtain(curtain!!)
            } else {
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x21.toByte(), 0x00, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, ctAdress!!, params)
                slowBoolean = true
                curtain!!.closeSlowStart = false
                DBUtils.updateCurtain(curtain!!)
            }
        }

    }

    private fun sofwareRestart() {
        if (typeStr == Constant.TYPE_GROUP) {
            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0xEA.toByte(), 0x00, Opcode.CURTAIN_PACK_END)
            val opcode = Opcode.CURTAIN_ON_OFF
            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, curtainGroup!!.meshAddr, params)
        } else {
            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0xEA.toByte(), 0x00, Opcode.CURTAIN_PACK_END)
            val opcode = Opcode.CURTAIN_ON_OFF
            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, ctAdress!!, params)
        }

    }

    private fun handRecovery() {
        if (typeStr == Constant.TYPE_GROUP) {
            when {
                handBoolean -> {
                    val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x12, 0x01, Opcode.CURTAIN_PACK_END)
                    val opcode = Opcode.CURTAIN_ON_OFF
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, curtainGroup!!.meshAddr, params)
                    handBoolean = false
                }
                else -> {
                    val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x12, 0x00, Opcode.CURTAIN_PACK_END)
                    val opcode = Opcode.CURTAIN_ON_OFF
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, curtainGroup!!.meshAddr, params)
                    handBoolean = true
                }
            }
        } else {
            if (handBoolean) {
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x12, 0x01, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, ctAdress!!, params)
                handBoolean = false
                curtain!!.closePull = true
                DBUtils.updateCurtain(curtain!!)
            } else {
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x12, 0x00, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, ctAdress!!, params)
                handBoolean = true
                curtain!!.closePull = false
                DBUtils.updateCurtain(curtain!!)
            }
        }
    }

    private fun onceReset() {
        if (typeStr == Constant.TYPE_GROUP) {
            val subscribe = Commander.resetDevice(curtainGroup!!.meshAddr)
                    .subscribe(
                            {
                                LogUtils.v("zcl-----恢复出厂成功")
                            }, {
                        LogUtils.v("zcl-----恢复出厂失败")
                    })

            DBUtils.deleteGroupOnly(curtainGroup!!)
            Toast.makeText(this, R.string.successful_resumption, Toast.LENGTH_LONG).show()
            finish()
        } else {
            val subscribe = Commander.resetDevice(curtain!!.meshAddr)
                    .subscribe(
                            {
                                LogUtils.v("zcl-----恢复出厂成功")
                            }, {
                        LogUtils.v("zcl-----恢复出厂失败")
                    })

            DBUtils.deleteCurtain(curtain!!)
            Toast.makeText(this, R.string.successful_resumption, Toast.LENGTH_LONG).show()
            finish()
        }
    }


    fun remove() {
        AlertDialog.Builder(Objects.requireNonNull<Activity>(this)).setMessage(getString(R.string.sure_delete_device, curtain?.name))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    if (TelinkLightService.Instance()?.adapter!!.mLightCtrl.currentLight.isConnected) {
                        val deviceMeshAddr = if (typeStr == Constant.TYPE_GROUP) curtainGroup?.meshAddr else curtain?.meshAddr
                        showLoadingDialog(getString(R.string.please_wait))
                        val dispose = Commander.resetDevice(deviceMeshAddr ?: 0)
                                .subscribe({
                                    LogUtils.v("zcl-----恢复出厂成功")
                                    //  deleteData()
                                }, {
                                    GlobalScope.launch(Dispatchers.Main) {
                                        /*    showDialogHardDelete?.dismiss()
                                          showDialogHardDelete = android.app.AlertDialog.Builder(this).setMessage(R.string.delete_device_hard_tip)
                                                  .setPositiveButton(android.R.string.ok) { _, _ ->
                                                      showLoadingDialog(getString(R.string.please_wait))
                                                      deleteData()
                                                  }
                                                  .setNegativeButton(R.string.btn_cancel, null)
                                                  .show()*/

                                    }
                                })

                        deleteData()

                    } else ToastUtils.showLong(getString(R.string.bluetooth_open_connet))

                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
    }

    fun deleteData() {
        hideLoadingDialog()
        if (typeStr == Constant.TYPE_GROUP) {
            if (curtainGroup != null)
                DBUtils.deleteGroupOnly(curtainGroup!!)
        } else
            if (curtain != null) {
                DBUtils.deleteCurtain(curtain!!)
                if (TelinkLightApplication.getApp().mesh.removeDeviceByMeshAddress(curtain!!.meshAddr))
                    TelinkLightApplication.getApp().mesh.saveOrUpdate(this)
                if (mConnectDevice != null) {
                    if (curtain!!.meshAddr == mConnectDevice!!.meshAddress)
                        setResult(Activity.RESULT_OK, Intent().putExtra("data", true))
                }
            }
        finish()
    }


    private fun electricCommutation() {
        if (typeStr == Constant.TYPE_GROUP) {
            if (commutationBoolean) {
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x11, 0x01, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, curtainGroup!!.meshAddr, params)
                commutationBoolean = false
            } else {
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x11, 0x00, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, curtainGroup!!.meshAddr, params)
                commutationBoolean = true
            }
        } else {
            if (commutationBoolean) {
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x11, 0x01, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, ctAdress!!, params)
                commutationBoolean = false
                curtain!!.inverse = true
                DBUtils.updateCurtain(curtain!!)
            } else {
                val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x11, 0x00, Opcode.CURTAIN_PACK_END)
                val opcode = Opcode.CURTAIN_ON_OFF
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, ctAdress!!, params)
                commutationBoolean = true
                curtain!!.inverse = false
                DBUtils.updateCurtain(curtain!!)
            }

        }
    }


    private fun pauseWindow() {
        if (typeStr == Constant.TYPE_GROUP) {
            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x0B, 0x00, Opcode.CURTAIN_PACK_END)
            val opcode = Opcode.CURTAIN_ON_OFF
            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, curtainGroup!!.meshAddr, params)
            pauseBtn.setImageResource(R.drawable.icon_suspend_pre)
            closeBtn.setImageResource(R.drawable.icon_curtain_close)
            closeText.setTextColor(Color.parseColor("#333333"))
            openBtn.setImageResource(R.drawable.icon_curtain_close)
            openText.setTextColor(Color.parseColor("#333333"))
        } else {
            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x0B, 0x00, Opcode.CURTAIN_PACK_END)
            val opcode = Opcode.CURTAIN_ON_OFF
            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, ctAdress!!, params)
            pauseBtn.setImageResource(R.drawable.icon_suspend_pre)
            closeBtn.setImageResource(R.drawable.icon_curtain_close)
            closeText.setTextColor(Color.parseColor("#333333"))
            openBtn.setImageResource(R.drawable.icon_curtain_close)
            openText.setTextColor(Color.parseColor("#333333"))
            curtain!!.status = 0
            DBUtils.updateCurtain(curtain!!)
        }

    }

    private fun offWindow() {
        if (typeStr == Constant.TYPE_GROUP) {
            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x0C, 0x00, Opcode.CURTAIN_PACK_END)
            val opcode = Opcode.CURTAIN_ON_OFF
            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, curtainGroup!!.meshAddr, params)
            curtainImage.setImageResource(R.drawable.curtain_close)
            pauseBtn.setImageResource(R.drawable.icon_suspend)
            closeBtn.setImageResource(R.drawable.icon_open_yes)
            closeText.setTextColor(Color.parseColor("#0080EA"))
            openBtn.setImageResource(R.drawable.icon_curtain_close)
            openText.setTextColor(Color.parseColor("#333333"))
        } else {
            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x0C, 0x00, Opcode.CURTAIN_PACK_END)
            val opcode = Opcode.CURTAIN_ON_OFF
            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, ctAdress!!, params)
            curtainImage.setImageResource(R.drawable.curtain_close)
            pauseBtn.setImageResource(R.drawable.icon_suspend)
            closeBtn.setImageResource(R.drawable.icon_open_yes)
            closeText.setTextColor(Color.parseColor("#0080EA"))
            openBtn.setImageResource(R.drawable.icon_curtain_close)
            openText.setTextColor(Color.parseColor("#333333"))
            curtain!!.status = 1
            DBUtils.updateCurtain(curtain!!)
        }
    }

    private fun openWindow() {
        if (typeStr == Constant.TYPE_GROUP) {
            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x0A, 0x00, Opcode.CURTAIN_PACK_END)
            val opcode = Opcode.CURTAIN_ON_OFF
            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, curtainGroup!!.meshAddr, params)
            curtainImage.setImageResource(R.drawable.curtain)
            pauseBtn.setImageResource(R.drawable.icon_suspend)
            openBtn.setImageResource(R.drawable.icon_open_yes)
            openText.setTextColor(Color.parseColor("#0080EA"))
            closeBtn.setImageResource(R.drawable.icon_curtain_close)
            closeText.setTextColor(Color.parseColor("#333333"))
        } else {
            val params = byteArrayOf(Opcode.CURTAIN_PACK_START, 0x0A, 0x00, Opcode.CURTAIN_PACK_END)
            val opcode = Opcode.CURTAIN_ON_OFF
            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, ctAdress!!, params)
            curtainImage.setImageResource(R.drawable.curtain)
            pauseBtn.setImageResource(R.drawable.icon_suspend)
            openBtn.setImageResource(R.drawable.icon_open_yes)
            openText.setTextColor(Color.parseColor("#0080EA"))
            closeBtn.setImageResource(R.drawable.icon_curtain_close)
            closeText.setTextColor(Color.parseColor("#333333"))
            curtain!!.status = 2
            DBUtils.updateCurtain(curtain!!)
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
                                    this.runOnUiThread {
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
                    this.runOnUiThread {
                        failedCallback.invoke()
                    }
                    //("retry delete group timeout")
                }
            } else {
                DBUtils.deleteGroupOnly(group)
                this.runOnUiThread {
                    successCallback.invoke()
                }
            }
        }.start()

    }

    private fun deleteAllSceneByLightAddr(lightMeshAddr: Int) {
        val opcode = Opcode.SCENE_ADD_OR_DEL
        val params: ByteArray
        params = byteArrayOf(0x00, 0xff.toByte())
        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, lightMeshAddr, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable?.dispose()
        mConnectDeviceDisposable?.dispose()
        mDisposable.dispose()
    }
}


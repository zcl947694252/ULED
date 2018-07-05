package com.dadoutek.uled.aboutuser

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.StringUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.aboutlight.DeviceScanningNewActivity
import com.dadoutek.uled.aboutlight.EmptyAddActivity
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.intf.NetworkObserver
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbUser
import com.dadoutek.uled.model.HttpModel.AccountModel
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.util.LogUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.toolbar.*

/**
 * Created by hejiajun on 2018/5/15.
 */

class LoginActivity : TelinkBaseActivity(), View.OnClickListener {
    private var dbUser: DbUser? = null
    private var phone: String? = null
    private var editPassWord: String? = null
    private var isFirstLauch: Boolean = false
    private var mWakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //页面存在耗时操作 需要保持屏幕常亮
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeLock")

        initData()
        initView()
    }

    private fun initData() {
        dbUser = DbUser()
        val intent = intent
        isFirstLauch = intent.getBooleanExtra(IS_FIRST_LAUNCH, true)
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.user_login_title)
    }

    override fun onPause() {
        super.onPause()
        if (this.mWakeLock != null) {
            mWakeLock!!.acquire()
        }
    }

    override fun onResume() {
        super.onResume()
        if (mWakeLock != null) {
            mWakeLock!!.acquire()
        }
    }

    private fun initView() {
        initToolbar()
        //        txtHeaderTitle.setText(R.string.user_login_title);
        if (SharedPreferencesHelper.getBoolean(this@LoginActivity, Constant.IS_LOGIN, false)) {
            TransformView()
        }

        btn_login.setOnClickListener(this)
        btn_register.setOnClickListener(this)
        forget_password.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_login -> login()
            R.id.btn_register -> {
                val intent = Intent(this@LoginActivity, PhoneVerificationActivity::class.java)
                intent.putExtra("fromLogin", "register")
                startActivity(intent)
            }
            R.id.forget_password -> forgetPassword()
        }
    }

    private fun forgetPassword() {
        val intent = Intent(this@LoginActivity, PhoneVerificationActivity::class.java)
        intent.putExtra("fromLogin", "forgetPassword")
        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> ActivityUtils.finishAllActivities(true)
        }
        return super.onOptionsItemSelected(item)
    }


    private fun login() {
        phone = edit_user_phone_or_email!!.editText!!.text.toString().trim { it <= ' ' }
        editPassWord = edit_user_password!!.editText!!.text.toString().trim { it <= ' ' }

        if (!StringUtils.isTrimEmpty(phone) && !StringUtils.isTrimEmpty(editPassWord)) {
            showLoadingDialog(getString(R.string.logging_tip))
            AccountModel.login(phone!!, editPassWord!!, dbUser!!.channel)
                    .subscribe(object : NetworkObserver<DbUser>() {
                        override fun onNext(dbUser: DbUser) {
                            LogUtils.d("logging: " + "登录成功")
                            DBUtils.deleteLocalData()
                            ToastUtils.showLong(R.string.login_success)
                            hideLoadingDialog()
                            //判断是否用户是首次在这个手机登录此账号，是则同步数据
                                showLoadingDialog(getString(R.string.sync_now))
                                SyncDataPutOrGetUtils.syncGetDataStart(dbUser,syncCallback)
                        }

                        override fun onError(e: Throwable) {
                            super.onError(e)
                            hideLoadingDialog()
                        }
                    })
        } else {
            Toast.makeText(this, getString(R.string.phone_or_password_can_not_be_empty), Toast.LENGTH_SHORT).show()
        }
    }

    internal var syncCallback: SyncCallback = object : SyncCallback {

        override fun start() {
            showLoadingDialog(getString(R.string.tip_start_sync))
        }

        override fun complete() {
            syncComplet()
        }

        override fun error(msg: String) {
           LogUtils.d("GetDataError:"+msg)
        }

    }

    private fun syncComplet() {
        ToastUtils.showLong(getString(R.string.download_data_success))
        hideLoadingDialog()
        TransformView()
    }

    private fun TransformView() {
        //        if (isFirstLauch) {
        //            startActivityForResult(new Intent(this, AddMeshActivity.class), REQ_MESH_SETTING);
        //        } else {
        if (DBUtils.getAllLight().size == 0) {
            startActivity(Intent(this@LoginActivity, EmptyAddActivity::class.java))
            //            startActivity(new Intent(LoginActivity.this, AddMeshActivity.class));
            finish()
        } else {
            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
        }
        //        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        //退出MeshSetting后进入DeviceScanning
        if (requestCode == REQ_MESH_SETTING) {
            gotoDeviceScanning()
        }
    }

    /**
     * 进入引导流程，也就是进入DeviceActivity。
     */
    private fun gotoDeviceScanning() {
        //首次进入APP才进入引导流程
        val intent = Intent(this@LoginActivity, DeviceScanningNewActivity::class.java)
        intent.putExtra("isInit", true)
        startActivity(intent)
        finish()
    }

    companion object {
        private val REQ_MESH_SETTING = 0x01
        val IS_FIRST_LAUNCH = "IS_FIRST_LAUNCH"
    }


}

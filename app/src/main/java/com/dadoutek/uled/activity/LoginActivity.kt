package com.dadoutek.uled.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.support.v7.app.ActionBar
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.StringUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.TelinkBaseActivity
import com.dadoutek.uled.intf.NetworkObserver
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbUser
import com.dadoutek.uled.model.HttpModel.AccountModel
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.util.LogUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils

import com.dadoutek.uled.TelinkLightApplication
import com.dadoutek.uled.intf.NetworkFactory
import com.dadoutek.uled.intf.NetworkTransformer
import com.dadoutek.uled.model.DbModel.DbScene
import com.dadoutek.uled.util.SharedPreferencesUtils
import com.mob.tools.utils.DeviceHelper
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.toolbar.*

/**
 * Created by hejiajun on 2018/5/15.
 */

class LoginActivity : TelinkBaseActivity(),View.OnClickListener {
    //    @BindView(R.id.img_header_menu_left)
    //    ImageView imgHeaderMenuLeft;
    //    @BindView(R.id.txt_header_title)
    //    TextView txtHeaderTitle;
//    var toolbar: Toolbar? = null
//    var editUserPassword: TextInputLayout? = null
//    var btnLogin: Button? = null
//    var editUserPhoneOrEmail: TextInputLayout? = null
//    var btnRegister: Button? = null
//    var forgetPassword: TextView? = null

    private var dbUser: DbUser? = null
    private val salt = ""
    private val MD5Password: String? = null
    private var phone: String? = null
    private var editPassWord: String? = null
    private var isFirstLauch: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        initData()
        initView()
    }

    private fun initData() {
        dbUser = DbUser()
        val intent = intent
        isFirstLauch = intent.getBooleanExtra(IS_FIRST_LAUNCH, true)
    }

    private fun initToolbar() {
//        toolbar!!.setTitle(R.string.user_login_title)
//        setSupportActionBar(toolbar)
//        val actionBar = supportActionBar
//        actionBar?.setDisplayHomeAsUpEnabled(true)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.user_login_title)
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
                            ToastUtils.showLong(R.string.login_success)
                            hideLoadingDialog()
                            //判断是否用户是首次在这个手机登录此账号，是则同步数据
                            if(!SharedPreferencesUtils.getCurrentUserList().contains(dbUser.account)){
                                showLoadingDialog(getString(R.string.sync_now))
                                syncGetDataStart(dbUser)
                            }
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

    fun syncGetDataStart(dbUser: DbUser) {
        val token = dbUser.token
        startGet(token,dbUser.account)
    }

    private fun startGet(token: String,accountNow :String) {
        NetworkFactory.getApi()
                .getRegionList(token)
                .compose(NetworkTransformer())
                .flatMap {
                    for (item in it) {
                        DBUtils.saveRegion(item,true)
                    }

                    if(it.size!=0){
                        setupMesh()
                        SharedPreferencesHelper.putString(TelinkLightApplication.getInstance(),
                                Constant.USER_TYPE, Constant.USER_TYPE_NEW)
                    }
                    NetworkFactory.getApi()
                            .getLightList(token)
                            .compose(NetworkTransformer())
                }
                .flatMap {
                    for (item in it) {
                        DBUtils.saveLight(item,true)
                    }
                    NetworkFactory.getApi()
                            .getGroupList(token)
                            .compose(NetworkTransformer())
                }
                .flatMap {
                    for (item in it) {
                        DBUtils.saveGroup(item,true)
                    }
                    NetworkFactory.getApi()
                            .getSceneList(token)
                            .compose(NetworkTransformer())
                }
                .observeOn(Schedulers.io())
                .doOnNext {
                    for (item in it) {
                        DBUtils.saveScene(item,true)
                        for (action in item.actions) {
                            DBUtils.saveSceneActions(action)
                        }
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())!!.subscribe(
                object : NetworkObserver<List<DbScene>>() {
                    override fun onNext(item: List<DbScene>) {
                        ToastUtils.showLong(getString(R.string.sync_complet))
                        SharedPreferencesUtils.saveCurrentUserList(accountNow)
                        hideLoadingDialog()
                        TransformView()
                    }

                    override fun onError(e: Throwable) {
                        super.onError(e)
                    }
                }
        )
    }

    private fun setupMesh() {
        val regionList = DBUtils.getRegionAll()

        //数据库有区域数据直接加载
        if(regionList.size!=0){
//            val usedRegionID=SharedPreferencesUtils.getCurrentUseRegion()
            val dbRegion=DBUtils.getLastRegion()
            val application = DeviceHelper.getApplication() as TelinkLightApplication
            val mesh = application.mesh
            mesh.name = dbRegion.controlMesh
            mesh.password = dbRegion.controlMeshPwd
            mesh.factoryName = dbRegion.installMesh
            mesh.password = dbRegion.installMeshPwd
//            mesh.saveOrUpdate(TelinkLightApplication.getInstance())
            application.setupMesh(mesh)
            SharedPreferencesUtils.saveCurrentUseRegion(dbRegion.id!!)
            return
        }
    }

}
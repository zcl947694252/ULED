package com.dadoutek.uled.user

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import butterknife.ButterKnife
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.TelinkBaseActivity
import com.dadoutek.uled.light.EmptyAddActivity
import com.dadoutek.uled.intf.NetworkFactory
import com.dadoutek.uled.intf.NetworkFactory.md5
import com.dadoutek.uled.intf.NetworkObserver
import com.dadoutek.uled.intf.NetworkTransformer
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbUser
import com.dadoutek.uled.model.HttpModel.AccountModel
import com.dadoutek.uled.util.LogUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.android.synthetic.main.toolbar.*

/**
 * Created by hejiajun on 2018/5/16.
 */

class RegisterActivity : TelinkBaseActivity(),View.OnClickListener {
//    @BindView(R.id.edit_user_password)
//    internal var editUserPassword: TextInputLayout? = null
//    @BindView(R.id.register_completed)
//    internal var registerCompleted: Button? = null
//    @BindView(R.id.edit_user_phone)
//    internal var editUserName: TextInputLayout? = null
//    @BindView(R.id.toolbar)
//    internal var toolbar: Toolbar? = null

    private var userName: String? = null
    private var userPassWord: String? = null
    private var MD5PassWord: String? = null

    //    private fun login() {
    //        phone = edit_user_phone_or_email!!.editText!!.text.toString().trim { it <= ' ' }
    //        editPassWord = edit_user_password!!.editText!!.text.toString().trim { it <= ' ' }
    //
    //        if (!StringUtils.isTrimEmpty(phone) && !StringUtils.isTrimEmpty(editPassWord)) {
    //            showLoadingDialog(getString(R.string.logging_tip))
    //            AccountModel.login(phone!!, editPassWord!!, dbUser!!.channel)
    //                    .subscribe(object : NetworkObserver<DbUser>() {
    //                override fun onNext(dbUser: DbUser) {
    //                    LogUtils.d("logging: " + "登录成功")
    //                    ToastUtils.showLong(R.string.login_success)
    //                    hideLoadingDialog()
    //                    //判断是否用户是首次在这个手机登录此账号，是则同步数据
    ////                            if(!SharedPreferencesUtils.getCurrentUserList().contains(dbUser.account)){
    //////                                showLoadingDialog(getString(R.string.sync_now))
    //////                                syncGetDataStart(dbUser)
    ////                            }else{
    //                    TransformView()
    ////                            }
    //                }
    //
    //                override fun onError(e: Throwable) {
    //                    super.onError(e)
    //                    hideLoadingDialog()
    //                }
    //            })
    //        } else {
    //            Toast.makeText(this, getString(R.string.phone_or_password_can_not_be_empty), Toast.LENGTH_SHORT).show()
    //        }
    //    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        ButterKnife.bind(this)
        initView()


    }

    private fun initView() {
        initToolbar()
        val phone = intent.getStringExtra("phone")
        if (!phone.isEmpty()) {
            if (edit_user_phone!!.editText != null)
                edit_user_phone!!.editText!!.setText(phone)
        }
        register_completed.setOnClickListener(this)
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.user_login_title)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.register_completed -> if (checkIsOK()) {
                register()
            }
        }
    }

    private fun register() {
        showLoadingDialog(getString(R.string.registing))
        MD5PassWord = md5(userPassWord)
        NetworkFactory.getApi()
                .register(userName, MD5PassWord, userName)
                .compose(NetworkTransformer())
                .flatMap { it: DbUser ->
                    hideLoadingDialog()
                    showLoadingDialog(getString(R.string.logging_tip))
                    AccountModel.login(userName!!, userPassWord!!, it!!.channel)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : NetworkObserver<DbUser>() {
                    override fun onNext(dbUser: DbUser) {
//                        LogUtils.d("logging: " + "登录成功")
//                        ToastUtils.showLong(R.string.login_success)
//                        hideLoadingDialog()
//                        TransformView()

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
//        ToastUtils.showLong(getString(R.string.upload_complete))
        hideLoadingDialog()
        TransformView()
    }

    private fun TransformView() {
        startActivity(Intent(this@RegisterActivity, EmptyAddActivity::class.java))
        finish()
    }

    private fun checkIsOK(): Boolean {
        userName = edit_user_phone!!.editText!!.text.toString().trim { it <= ' ' }
        userPassWord = edit_user_password!!.editText!!.text.toString().trim { it <= ' ' }

        if (compileExChar(userName)) {
            ToastUtils.showLong(R.string.phone_input_error)
            return false
        } else if (compileExChar(userName) || compileExChar(userPassWord)) {
            ToastUtils.showLong(R.string.tip_register_input_error)
            return false
        } else {
            return true
        }
    }
}

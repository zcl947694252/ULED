package com.dadoutek.uled.user

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.blankj.utilcode.util.StringUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbUser
import com.dadoutek.uled.model.HttpModel.AccountModel
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.network.bean.RegionAuthorizeBean
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.region.adapter.MultiItemAdapter
import com.dadoutek.uled.region.bean.MultiRegionBean
import com.dadoutek.uled.region.bean.RegionBean
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.SharedPreferencesUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import com.dadoutek.uled.util.ToastUtil
import com.telink.TelinkApplication
import kotlinx.android.synthetic.main.activity_enter_password.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.anko.toast

/**
 * 登录界面 输入密码
 */
class EnterPasswordActivity : Activity(), View.OnClickListener, TextWatcher {
    private var pop: PopupWindow? = null
    private var popRecycle: RecyclerView? = null
    private var popTitle: TextView? = null
    private var loadDialog: Dialog? = null
    private var mWakeLock: PowerManager.WakeLock? = null
    private var phone: String? = null
    private var type: String? = null
    private var isPassword = false
    private var editPassWord: String? = null
    private var dbUser: DbUser? = null

    @SuppressLint("InvalidWakeLockTag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_password)
        type = intent.extras!!.getString("USER_TYPE")

        makePop()

        initViewType()
        initView()
    }

    private fun makePop() {
        var popView = LayoutInflater.from(this).inflate(R.layout.pop_region_check, null)
        popRecycle = popView.findViewById(R.id.template_recycleView)

        pop = PopupWindow(popView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        pop?.let {
            it.isFocusable = true // 设置PopupWindow可获得焦点
            it.isTouchable = true // 设置PopupWindow可触摸补充：
            it.isOutsideTouchable = false
        }
    }

    private fun initViewType() {
        dbUser = DbUser()
        when (type) {
            Constant.TYPE_FORGET_PASSWORD -> {
            }

            Constant.TYPE_LOGIN -> phone = intent.extras!!.getString("phone")

            Constant.TYPE_REGISTER -> phone = intent.extras!!.getString("phone")

        }
        val boolean = SharedPreferencesHelper.getBoolean(TelinkApplication.getInstance(), Constant.NOT_SHOW, true)

        var user = SharedPreferencesUtils.getLastUser()
        user?.let {
            var list = it.split("-")
            //("zcl**********************${list.size}----$list")
            if (list.size > 1 && user != "-" && !boolean) {
                var s = list[1]
                edit_user_password.setText(s)
                edit_user_password.setSelection(s.length)
                btn_login.background = getDrawable(R.drawable.btn_rec_blue_bt)
            }
        }
    }

    private fun initView() {
        edit_user_password.addTextChangedListener(this)
        eye_btn.setOnClickListener(this)
        btn_login.setOnClickListener(this)
        image_return_password.setOnClickListener(this)
        forget_password.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.eye_btn -> {
                if (isPassword) {
                    eye_btn.setImageResource(R.drawable.icon_turn)
                    isPassword = false
                    edit_user_password.transformationMethod = PasswordTransformationMethod.getInstance()
                    edit_user_password.setSelection(edit_user_password.text.length)
                } else {
                    isPassword = true
                    eye_btn.setImageResource(R.drawable.icon_open_eye)
                    edit_user_password.transformationMethod = HideReturnsTransformationMethod.getInstance()
                    edit_user_password.setSelection(edit_user_password.text.length)
                }
            }

            R.id.btn_login -> {
                when (type) {
                    Constant.TYPE_LOGIN -> {
                        if (TextUtils.isEmpty(edit_user_password.editableText.toString())) {
                            toast(getString(R.string.please_password))
                            return
                        }
                        login()
                    }
                    Constant.TYPE_FORGET_PASSWORD -> forgetPassword()
                }
            }
            R.id.image_return_password -> finish()

            R.id.forget_password -> {
                var intent = Intent(this, ForgetPassWordActivity::class.java)
                intent.putExtra("fromLogin", "forgetPassword")
                startActivity(intent)
            }
        }
    }

    private fun forgetPassword() {
        editPassWord = edit_user_password!!.text.toString().trim { it <= ' ' }.replace(" ".toRegex(), "")
        if (!StringUtils.isTrimEmpty(editPassWord)) {
            val intent = Intent(this, AgainEnterPasswordActivity::class.java)
            intent.putExtra("phone", phone)
            intent.putExtra("password", editPassWord)
            startActivity(intent)
        } else {
            ToastUtil.showToast(this, getString(R.string.password_cannot))
        }
    }


    @SuppressLint("CheckResult")
    private fun login() {
        editPassWord = edit_user_password!!.text.toString().trim { it <= ' ' }.replace(" ".toRegex(), "")

        if (!StringUtils.isTrimEmpty(editPassWord)) {
            showLoadingDialog(getString(R.string.logging_tip))
            AccountModel.login(phone!!, editPassWord!!)
                    .subscribe(object : NetworkObserver<DbUser>() {
                        override fun onNext(dbUser: DbUser) {
                            SharedPreferencesHelper.putString(this@EnterPasswordActivity, Constant.LOGIN_STATE_KEY, dbUser.login_state_key)
                            DBUtils.deleteLocalData()
                            //判断是否用户是首次在这个手机登录此账号，是则同步数据
                            SyncDataPutOrGetUtils.syncGetDataStart(dbUser, syncCallback)
                            SharedPreferencesUtils.setUserLogin(true)
                            //("logging: " + "登录成功" + dbUser.toString())
                        }
                        override fun onError(e: Throwable) {
                            super.onError(e)
                            hideLoadingDialog()
                        }
                    })
        } else {
            ToastUtil.showToast(this, getString(R.string.password_cannot))
        }
    }

    var isSuccess: Boolean = true

    internal var syncCallback: SyncCallback = object : SyncCallback {
        override fun start() {}
        override fun complete() {
            if (isSuccess)
                syncComplet() }

        override fun error(msg: String) {
            val ishowRegionDialog = SharedPreferencesHelper.getBoolean(TelinkApplication.getInstance()
                    .mContext, Constant.IS_SHOW_REGION_DIALOG, false)
            Log.e("zcl", "zcl*****同步返回授权问题boolean*****$ishowRegionDialog-----------$dbUser")


            if (ishowRegionDialog) {
                val authorList = SharedPreferencesHelper.getObject(this@EnterPasswordActivity, Constant.REGION_AUTHORIZE_LIST) as List<RegionAuthorizeBean>
                val list = SharedPreferencesHelper.getObject(this@EnterPasswordActivity, Constant.REGION_LIST) as List<RegionBean>

                val arrayList = arrayListOf<MultiRegionBean>()


                arrayList.add(MultiRegionBean(Constant.REGION_TYPE, list))
                arrayList.add(MultiRegionBean(Constant.REGION_AUTHORIZE_TYPE,authorList))
                Log.e("zcl", "zcl*****同步返回授权问题boolean*****$list-----------$authorList-----${arrayList.size}")

                popRecycle?.layoutManager = LinearLayoutManager(this@EnterPasswordActivity, LinearLayoutManager.VERTICAL, false)
                popRecycle?.adapter = MultiItemAdapter(arrayList)

                pop?.showAtLocation(window.decorView,Gravity.CENTER,0,100)
            }
            isSuccess = false
            hideLoadingDialog()
            SharedPreferencesHelper.putBoolean(TelinkLightApplication.getInstance(), Constant.IS_LOGIN, false)
        }
    }

    private fun syncComplet() {
        SharedPreferencesHelper.putBoolean(TelinkLightApplication.getInstance(), Constant.IS_LOGIN, true)

        startActivity(Intent(this@EnterPasswordActivity, MainActivity::class.java))
        hideLoadingDialog()
    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

    override fun afterTextChanged(p0: Editable?) {
        if (TextUtils.isEmpty(p0.toString()))
            btn_login.background = getDrawable(R.drawable.btn_rec_black_bt)
        else
            btn_login.background = getDrawable(R.drawable.btn_rec_blue_bt)
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

    fun showLoadingDialog(content: String) {
        val inflater = LayoutInflater.from(this)
        val v = inflater.inflate(R.layout.dialogview, null)

        val layout = v.findViewById<View>(R.id.dialog_view) as LinearLayout
        val tvContent = v.findViewById<View>(R.id.tvContent) as TextView
        tvContent.text = content

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

}

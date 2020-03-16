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
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.StringUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbRegion
import com.dadoutek.uled.model.DbModel.DbUser
import com.dadoutek.uled.model.HttpModel.AccountModel
import com.dadoutek.uled.model.HttpModel.RegionModel
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.network.bean.RegionAuthorizeBean
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.region.adapter.RegionAuthorizeDialogAdapter
import com.dadoutek.uled.region.adapter.RegionDialogAdapter
import com.dadoutek.uled.region.bean.RegionBean
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.PopUtil
import com.dadoutek.uled.util.SharedPreferencesUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import com.mob.tools.utils.DeviceHelper
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
    private var itemAdapter: RegionDialogAdapter? = null
    private var itemAdapterAuthor: RegionAuthorizeDialogAdapter? = null
    private var popConfirm: TextView? = null
    private var mAuthorList: MutableList<RegionAuthorizeBean>? = null
    private var mList: MutableList<RegionBean>? = null
    private var regionBeanAuthorize: RegionAuthorizeBean? = null
    private var regionBean: RegionBean? = null
    private var poptitle: TextView? = null
    private var poptitleAuthorize: TextView? = null
    private var pop: PopupWindow? = null
    private var popRecycle: RecyclerView? = null
    private var popRecycleAuthorize: RecyclerView? = null
    private var loadDialog: Dialog? = null
    private var mWakeLock: PowerManager.WakeLock? = null
    private var phone: String? = null
    private var type: String? = null
    private var isPassword = false
    private var editPassWord: String? = null
    private var dbUser: DbUser? = null
    private var whoClick: Int = 0
    private var NONE: Int = 0
    private var ME: Int = 1
    private var AUTHOR: Int = 2
    var isRunning = false

    @SuppressLint("InvalidWakeLockTag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_password)
        type = intent.extras!!.getString("USER_TYPE")
        isRunning = true
        makePop()
        initViewType()
        initView()
    }

    private fun makePop() {
        var popView = LayoutInflater.from(this).inflate(R.layout.pop_region_check, null)
        poptitle = popView.findViewById(R.id.region_dialog_me_net_num)
        poptitleAuthorize = popView.findViewById(R.id.region_dialog_authorize_net_num)
        popRecycle = popView.findViewById(R.id.region_dialog_me_recycleview)
        popRecycleAuthorize = popView.findViewById(R.id.region_dialog_authorize_recycleview)

        popConfirm = popView.findViewById(R.id.region_dialog_confirm)

        popRecycle?.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        popRecycleAuthorize?.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        pop = PopupWindow(popView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        popConfirm?.setOnClickListener {
            showLoadingDialog(getString(R.string.logging_tip))
            downLoadDataAndChangeDbUser()
        }
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
                edit_user_password_line.background = getDrawable(R.drawable.line_blue)
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
                startActivityForResult(intent, 0)
            }
        }
    }

    private fun forgetPassword() {
        editPassWord = edit_user_password!!.text.toString().trim { it <= ' ' }.replace(" ".toRegex(), "")
        if (!StringUtils.isTrimEmpty(editPassWord)) {
            val intent = Intent(this, AgainEnterPasswordActivity::class.java)
            intent.putExtra("phone", phone)
            intent.putExtra("password", editPassWord)
            startActivityForResult(intent, 0)
        } else {
            ToastUtils.showShort( getString(R.string.password_cannot))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && requestCode == RESULT_FIRST_USER) {
            setResult(RESULT_FIRST_USER)
            finish()
        }
    }

    @SuppressLint("CheckResult")
    private fun login() {
        editPassWord = edit_user_password!!.text.toString().trim { it <= ' ' }.replace(" ".toRegex(), "")
        Log.e("zcl", "zcl登录******")
        if (!StringUtils.isTrimEmpty(editPassWord)) {
            showLoadingDialog(getString(R.string.logging_tip))
            AccountModel.login(phone!!, editPassWord!!)
                    .subscribe(object : NetworkObserver<DbUser>() {
                        override fun onNext(dbUser: DbUser) {
                            Log.e("zcl", "zcl登录成功返回******$dbUser")
                            SharedPreferencesHelper.putString(this@EnterPasswordActivity, Constant.LOGIN_STATE_KEY, dbUser.login_state_key)
                            DBUtils.deleteLocalData()
                            //判断是否用户是首次在这个手机登录此账号，是则同步数据
                            SyncDataPutOrGetUtils.syncGetDataStart(dbUser, syncCallback)
                            SharedPreferencesUtils.setUserLogin(true)
                        }

                        override fun onError(e: Throwable) {
                            super.onError(e)
                            Log.e("zcl", "zcl登录******${e.localizedMessage}")
                            hideLoadingDialog()
                        }
                    })
        } else {
            ToastUtils.showShort(getString(R.string.password_cannot))
        }
    }

    var isSuccess: Boolean = true

    internal var syncCallback: SyncCallback = object : SyncCallback {

        override fun start() {}
        override fun complete() {
            if (isSuccess)
                syncComplet()
        }

        @SuppressLint("CheckResult")
        override fun error(msg: String) {
            val ishowRegionDialog = SharedPreferencesHelper.getBoolean(TelinkApplication.getInstance()
                    .mContext, Constant.IS_SHOW_REGION_DIALOG, false)
            LogUtils.e("EnterPasswordActivity", "zcl*****同步返回授权问题boolean*****$ishowRegionDialog-----$msg")

            if (ishowRegionDialog) {
                initMe()
                initAuthor()
                pop?.showAtLocation(window.decorView, Gravity.CENTER, 0, 100)
            }
            isSuccess = false
            hideLoadingDialog()
            SharedPreferencesHelper.putBoolean(TelinkLightApplication.getApp(), Constant.IS_LOGIN, false)
        }
    }

    @SuppressLint("CheckResult")
    private fun initAuthor() {
        RegionModel.getAuthorizerList()?.subscribe(object : NetworkObserver<MutableList<RegionAuthorizeBean>?>() {
            override fun onNext(it: MutableList<RegionAuthorizeBean>) {
                setAuthorizeRegion(it)
            }

            override fun onError(it: Throwable) {
                super.onError(it)
                ToastUtils.showLong(it.message)
            }
        })
    }

    private fun initMe() {
        val disposable = RegionModel.get()?.subscribe(object : NetworkObserver<MutableList<RegionBean>?>() {
            override fun onNext(t: MutableList<RegionBean>) {
                    setMeRegion(t)
            }

            override fun onError(it: Throwable) {
                super.onError(it)
                ToastUtils.showLong(it.message)
            }
        })
    }

    @SuppressLint("StringFormatMatches")
    private fun setAuthorizeRegion(authorList: MutableList<RegionAuthorizeBean>) {
        poptitleAuthorize?.text = getString(R.string.received_net_num, authorList.size)
        mAuthorList = authorList
        if (authorList.isNotEmpty()) {
            itemAdapterAuthor = RegionAuthorizeDialogAdapter(R.layout.region_dialog_item, mAuthorList)
            popRecycleAuthorize?.adapter = itemAdapterAuthor
            itemAdapterAuthor?.setOnItemClickListener { _, _, position ->
                regionBeanAuthorize = authorList[position]
                when (whoClick) {
                    NONE -> itemAdapterAuthor?.data?.get(position)?.is_selected = true
                    //上次点击的自己不用便利其他人
                    AUTHOR ->
                        if (itemAdapterAuthor != null)
                            for (i in itemAdapterAuthor!!.data.indices)
                                itemAdapterAuthor!!.data[i].is_selected = i == position
                    //上次点击个人区域 这次自己 个人全是false
                    ME -> initMe()
                }
                itemAdapterAuthor?.notifyDataSetChanged()
                whoClick = AUTHOR
            }
        }
    }

    @SuppressLint("StringFormatInvalid", "StringFormatMatches")
    private fun setMeRegion(list: MutableList<RegionBean>) {
        poptitle?.text = getString(R.string.me_net_num, list.size)
        mList = list
        if (list.isNotEmpty()) {
            itemAdapter = RegionDialogAdapter(R.layout.region_dialog_item, mList!!)
            popRecycle?.adapter = itemAdapter
            itemAdapter?.setOnItemClickListener { _, _, position ->
                regionBean = list[position]
                if (itemAdapter != null)
                    when (whoClick) {//更新UI
                        NONE -> itemAdapter!!.data[position].is_selected = true

                        //上次点击的自己不用便利其他人 更改状态
                        ME -> for (i in itemAdapter!!.data.indices)
                            itemAdapter!!.data[i].is_selected = i == position

                        //上次点击收授权区域 这次自己 授权全是false
                        AUTHOR -> initAuthor()
                    }
                itemAdapter?.notifyDataSetChanged()
                whoClick = ME
            }
        }
    }

    private fun downLoadDataAndChangeDbUser() {
        if (regionBean == null && regionBeanAuthorize == null) {
            ToastUtils.showLong(getString(R.string.please_select_area))
            hideLoadingDialog()
            return
        }

        var lastUser = DBUtils.lastUser

        lastUser?.let {
            //更新user
            when (whoClick) {
                1 -> {
                    it.last_region_id = regionBean?.id.toString()
                    it.last_authorizer_user_id = regionBean?.authorizer_id.toString()
                }
                2 -> {
                    it.last_region_id = regionBeanAuthorize?.id.toString()
                    it.last_authorizer_user_id = regionBeanAuthorize?.authorizer_id.toString()
                }
            }

            PopUtil.dismiss(pop)

            //更新last—region-id
            DBUtils.saveUser(it)

            getRegioninfo()
        }
    }


    @SuppressLint("CheckResult")
    private fun getRegioninfo(){//在更新User的regionID 以及lastUserID后再拉取区域信息 赋值对应controlMesName 以及PWd
        NetworkFactory.getApi()
                .getRegionInfo(DBUtils.lastUser?.last_authorizer_user_id, DBUtils.lastUser?.last_region_id)
                .compose(NetworkTransformer())
                .subscribe(object : NetworkObserver<DbRegion?>() {
                    override fun onNext(it: DbRegion) {
                            //保存最后的区域信息到application
                            val application = DeviceHelper.getApplication() as TelinkLightApplication
                            val mesh = application.mesh
                            mesh.name = it.controlMesh
                            mesh.password = it.controlMeshPwd
                            mesh.factoryName = it.installMesh
                            mesh.factoryPassword = it.installMeshPwd

                            DBUtils.lastUser?.controlMeshName = it.controlMesh
                            DBUtils.lastUser?.controlMeshPwd = it.controlMeshPwd


                            SharedPreferencesUtils.saveCurrentUseRegionID(it.id)
                            application.setupMesh(mesh)
                            val lastUser = DBUtils.lastUser!!
                            DBUtils.saveUser(lastUser)

                            DBUtils.deleteLocalData()
                            DBUtils.deleteAllData()
                            //创建数据库
                            AccountModel.initDatBase(lastUser)

                            //判断是否用户是首次在这个手机登录此账号，是则同步数据
                            SyncDataPutOrGetUtils.syncGetDataStart(lastUser, syncCallback)

                            Log.e("zclenterpassword", "zcl***保存数据***" + DBUtils.lastUser?.last_authorizer_user_id + "--------------------" + DBUtils.lastUser?.last_region_id)

                            SharedPreferencesUtils.setUserLogin(true)
                            SharedPreferencesHelper.putBoolean(TelinkLightApplication.getApp(), Constant.IS_LOGIN, true)

                            hideLoadingDialog()
                            ActivityUtils.finishAllActivities(true)
                            ActivityUtils.startActivity(this@EnterPasswordActivity, MainActivity::class.java)
                    }

                    override fun onError(it: Throwable) {
                        super.onError(it)
                        LogUtils.v("zcl-------$it")
                        hideLoadingDialog()
                        ToastUtils.showLong(it.localizedMessage)
                    }
                })
    }



    private fun syncComplet() {
        hideLoadingDialog()
        SharedPreferencesHelper.putBoolean(TelinkLightApplication.getApp(), Constant.IS_LOGIN, true)

        ActivityUtils.finishAllActivities(true)
        ActivityUtils.startActivityForResult(this@EnterPasswordActivity, MainActivity::class.java, 0)
    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

    override fun afterTextChanged(p0: Editable?) {
        if (TextUtils.isEmpty(p0.toString())) {
            btn_login.background = getDrawable(R.drawable.btn_rec_black_bt)
            edit_user_password_line.background = getDrawable(R.drawable.line_gray)
        } else {
            edit_user_password_line.background = getDrawable(R.drawable.line_blue)
            btn_login.background = getDrawable(R.drawable.btn_rec_blue_bt)
        }
    }

    override fun onPause() {
        super.onPause()
        if (this.mWakeLock != null) {
            mWakeLock!!.acquire()
        }
    }

    override fun onResume() {
        super.onResume()
        isRunning = true
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
            if (loadDialog != null && this.isActive && isRunning) {
                loadDialog!!.dismiss()
            }
        }
    }


    override fun onStop() {
        super.onStop()
        isRunning = false
    }

}

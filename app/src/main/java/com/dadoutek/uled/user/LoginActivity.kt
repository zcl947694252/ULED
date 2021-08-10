package com.dadoutek.uled.user

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.*
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.StringUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbUser
import com.dadoutek.uled.model.httpModel.AccountModel
import com.dadoutek.uled.model.httpModel.RegionModel
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.network.bean.RegionAuthorizeBean
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.region.adapter.RegionAuthorizeDialogAdapter
import com.dadoutek.uled.region.adapter.RegionDialogAdapter
import com.dadoutek.uled.region.bean.RegionBean
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.PopUtil
import com.dadoutek.uled.util.SharedPreferencesUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import com.mob.tools.utils.DeviceHelper
import com.telink.TelinkApplication
import io.reactivex.Observable.*
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_login.facebook_btn
import kotlinx.android.synthetic.main.activity_login.google_btn
import kotlinx.android.synthetic.main.activity_login.qq_btn
import kotlinx.android.synthetic.main.activity_login.third_party_text
import org.jetbrains.anko.toast
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by hejiajun on 2018/5/15.
 */

private const val MIN_CLICK_DELAY_TIME = 500

class LoginActivity : TelinkBaseActivity(), View.OnClickListener, TextWatcher {

    private var dbUser: DbUser? = null
    private var phone: String? = null
    private var editPassWord: String? = null
    private var isFirstLauch: Boolean = false
    private var recyclerView: RecyclerView? = null
    private var adapter: PhoneListRecycleViewAdapter? = null
    private var phoneList: ArrayList<DbUser>? = null
    private var isPhone = true
    private var currentUser: DbUser? = null
    private var isPassword = false
    private var lastClickTime: Long = 0
    private var popConfirm: TextView? = null
    private var mAuthorList: MutableList<RegionAuthorizeBean>? = null
    private var mList: MutableList<RegionBean>? = null
    private var regionBeanAuthorize: RegionAuthorizeBean? = null
    private var regionBean: RegionBean? = null
    private var poptitle: TextView? = null
    private var poptitleAuthorize: TextView? = null
    private var popAuthor: PopupWindow? = null
    private var popRecycle: RecyclerView? = null
    private var popRecycleAuthorize: RecyclerView? = null
    private var whoClick: Int = 0
    private var itemAdapter: RegionDialogAdapter? = null
    private var itemAdapterAuthor: RegionAuthorizeDialogAdapter? = null
    private var NONE: Int = 0
    private var ME: Int = 1
    private var AUTHOR: Int = 2

    @SuppressLint("InvalidWakeLockTag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        phoneList = DBUtils.getAllUser()
        if (phoneList!!.size == 0)
            date_phone.visibility = View.GONE
        TelinkLightApplication.isLoginAccount = false
        makePop()
        initData()
        initView()
        initListener()
    }

    private fun makePop() {
        var popView = LayoutInflater.from(this).inflate(R.layout.pop_region_check, null)
        poptitle = popView.findViewById(R.id.region_dialog_me_net_num)
        poptitleAuthorize = popView.findViewById(R.id.region_dialog_authorize_net_num)
        popRecycle = popView.findViewById(R.id.region_dialog_me_recycleview)
        popRecycleAuthorize = popView.findViewById(R.id.region_dialog_authorize_recycleview)

        popConfirm = popView.findViewById(R.id.region_dialog_confirm)

        popRecycle?.layoutManager = LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.VERTICAL, false)
        popRecycleAuthorize?.layoutManager = LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.VERTICAL, false)

        popAuthor = PopupWindow(popView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        popConfirm?.setOnClickListener {
            showLoadingDialog(getString(R.string.logging_tip))
            downLoadDataAndChangeDbUser()
        }
        popAuthor?.let {
            it.isFocusable = true // 设置PopupWindow可获得焦点
            it.isTouchable = true // 设置PopupWindow可触摸补充：
            it.isOutsideTouchable = false
        }
    }

    private fun downLoadDataAndChangeDbUser() {
        if (regionBean == null && regionBeanAuthorize == null) {
            ToastUtils.showLong(getString(R.string.please_select_area))
            hideLoadingDialog()
            return
        }

        var lastUser = DBUtils.lastUser

        lastUser?.let { //更新user
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

            PopUtil.dismiss(popAuthor)
            //更新last—region-id
            DBUtils.saveUser(it)
            getRegioninfo()
        }
    }


    @SuppressLint("CheckResult")
    private fun getRegioninfo() {//在更新User的regionID 以及lastUserID后再拉取区域信息 赋值对应controlMesName 以及PWd
        NetworkFactory.getApi()
                .getRegionInfo(DBUtils.lastUser?.last_authorizer_user_id, DBUtils.lastUser?.last_region_id)
                .compose(NetworkTransformer())
                .subscribe({
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
                    Thread.sleep(200)
                    ActivityUtils.startActivity(this@LoginActivity, MainActivity::class.java)
                }, {
                    LogUtils.v("zcl-------$it")
                    hideLoadingDialog()
                    ToastUtils.showLong(it.localizedMessage)
                })
    }

    private fun initData() {
        dbUser = DbUser()
        val intent = intent
        isFirstLauch = intent.getBooleanExtra(IS_FIRST_LAUNCH, true)
        var user = SharedPreferencesUtils.getLastUser()
        user?.let {
            var list = it.split("-")
            if (list.isNotEmpty()) {
                var s = list[0]
                edit_user_phone_or_email.setText(s)
                edit_user_phone_or_email.post {
                    if (!TextUtils.isEmpty(s))
                        edit_user_phone_or_email.setSelection(s.length)
                }
                SharedPreferencesHelper.putBoolean(TelinkApplication.getInstance(), Constant.NOT_SHOW, false)
            }
        }
    }

    private fun initToolbar() {
        return_image.setOnClickListener {
            finish()
        }
    }

    private fun initListener() {
        // chown changed it
        is_test_version.setOnCheckedChangeListener { _, isChecked ->
            when {
                isChecked -> {
                    Constant.isDebug = true
                    SharedPreferencesUtils.setTestType(true)
                    tx_test_version.text = "测试版"
                }
                else -> {
                    Constant.isDebug = false
                    SharedPreferencesUtils.setTestType(true)
                    tx_test_version.text = "正式版"
                }
            }
        }
        is_dadousmart.setOnCheckedChangeListener { _, isChecked ->
            when {
                isChecked -> {
                    SharedPreferencesHelper.putInt(this, Constant.IS_SMART, 1)
                    tx_is_dadousmart.text = "dadoutek"
                }
                else -> {
                    SharedPreferencesHelper.putInt(this, Constant.IS_SMART, 0)
                    tx_is_dadousmart.text = "dadousmart"
                }
            }
        }
//        login_isTeck.setOnCheckedChangeListener { _, checkedId ->
//            if (Constant.isDebug) {//如果是debug则可以切换
//                when (checkedId) {
//                    R.id.login_smart -> {
//                        SharedPreferencesHelper.putInt(this, Constant.IS_SMART, 0)
//                    }
//                    R.id.login_Teck -> {
//                        SharedPreferencesHelper.putInt(this, Constant.IS_SMART, 1)
//                    }
//                    R.id.login_rd -> {
//                        SharedPreferencesHelper.putInt(this, Constant.IS_SMART, 2)
//                    }
//                }
//                //startActivity(Intent(this@LoginActivity, MainActivity::class.java))
//            }
//        }
        scan_gp.setOnCheckedChangeListener { group, checkedId ->
            TelinkApplication.getInstance().isNew = checkedId == R.id.scan_new
            SharedPreferencesUtils.setScanType(TelinkApplication.getInstance().isNew)
            LogUtils.v("zcl-----------选择新旧方法-------isNew-------${TelinkApplication.getInstance().isNew}")
        }

        btn_login.setOnClickListener(this)
        forget_password.setOnClickListener(this)
        date_phone.setOnClickListener(this)
        eye_btn.setOnClickListener(this)
        sms_login_btn.setOnClickListener(this)
        edit_user_phone_or_email.addTextChangedListener(this)
        com.dadoutek.uled.util.StringUtils.initEditTextFilterForRegister(edit_user_phone_or_email) //添加过滤器
        com.dadoutek.uled.util.StringUtils.initEditTextFilterForRegister(edit_user_password)
    }

    private fun initView() {
        isDebugVisible()
        if (Constant.isShow) {
            is_test_version.visibility = View.VISIBLE
            tx_test_version.visibility = View.VISIBLE
            is_dadousmart.visibility = View.VISIBLE
            tx_is_dadousmart.visibility = View.VISIBLE
        } else {
            is_test_version.visibility = View.GONE
            tx_test_version.visibility = View.GONE
            is_dadousmart.visibility = View.GONE
            tx_is_dadousmart.visibility = View.GONE
        }
        is_test_version.isChecked = SharedPreferencesHelper.getBoolean(TelinkLightApplication.getApp(),"IS_TEST_CHECK",Constant.isDebug)
        tx_test_version.text = if(is_test_version.isChecked) "测试版"  else "正式版"
        is_dadousmart.isChecked = SharedPreferencesHelper.getInt(this, Constant.IS_SMART, 1).toInt()==1 //为1 则为dadoutek 为0 则为dadousmart
        tx_is_dadousmart.text = if(is_dadousmart.isChecked) "dadoutek"  else "dadousmart"
        val scanType = SharedPreferencesUtils.getScanType()
        when {
            scanType -> scan_new.isChecked = true
            else -> scan_old.isChecked = true
        }
        initToolbar()
        if (SharedPreferencesHelper.getBoolean(TelinkLightApplication.getApp(), Constant.IS_LOGIN, false))
            transformView()

        recyclerView = findViewById(R.id.list_phone)
        val info = SharedPreferencesUtils.getLastUser()
        val havePhone = info != null && info.isNotEmpty()
        when {
            havePhone -> {
                val messge = info.split("-")
                if (messge.size > 1)
                    edit_user_phone_or_email!!.setText(messge[0])
                edit_user_password!!.setText(messge[1])
                edit_user_phone_or_email_line.background = getDrawable(R.drawable.line_blue)
                btn_login.background = getDrawable(R.drawable.btn_rec_blue_bt)
            }
            else -> {
                edit_user_phone_or_email_line.background = getDrawable(R.drawable.line_gray)
                btn_login.background = getDrawable(R.drawable.btn_rec_black_bt)
            }
        }
    }


    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_login -> {
                if (TextUtils.isEmpty(edit_user_phone_or_email.editableText.toString())) {
                    toast(getString(R.string.please_phone_number))
                    return
                }
                if (TextUtils.isEmpty(edit_user_password.editableText.toString())) {
                    toast(getString(R.string.please_password))
                    return
                }

                val currentTime = Calendar.getInstance().timeInMillis

                if (currentTime - lastClickTime > MIN_CLICK_DELAY_TIME) {
                    lastClickTime = currentTime
                    login()
                }
            }
            R.id.forget_password -> forgetPassword()
            R.id.date_phone -> phoneList()
            R.id.eye_btn -> eyePassword()
            R.id.sms_password_login -> verificationCode()
            R.id.sms_login_btn -> verificationCode()
            R.id.linearLayout_1 -> {
                list_phone.visibility = View.GONE
                edit_user_password.visibility = View.GONE
                btn_login.visibility = View.VISIBLE
                forget_password.visibility = View.GONE
                eye_btn.visibility = View.GONE
                btn_login.visibility = View.VISIBLE
                sms_password_login.visibility = View.GONE
                third_party_text.visibility = View.VISIBLE
                qq_btn.visibility = View.VISIBLE
                google_btn.visibility = View.VISIBLE
                facebook_btn.visibility = View.VISIBLE
                isPhone = true
                date_phone.setImageResource(R.drawable.icon_down_arr)
            }
        }
    }

    private fun verificationCode() {
        returnView()
        var intent = Intent(this@LoginActivity, VerificationCodeActivity::class.java)
        intent.putExtra("type", Constant.TYPE_VERIFICATION_CODE)
        returnView()
        startActivityForResult(intent, 0)
    }

    private fun returnView() {
        isPhone = false
        phoneList()
    }

    private fun eyePassword() {
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

    private fun phoneList() {
        if (isPhone) {
            isShowPhoneList(true)

            val layoutmanager = LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.VERTICAL, false)
            recyclerView!!.layoutManager = layoutmanager //一定要设置布局管理器，否则界面出不来
            adapter = PhoneListRecycleViewAdapter(R.layout.recyclerview_phone_list, phoneList!!)
            val decoration = DividerItemDecoration(this, androidx.recyclerview.widget.DividerItemDecoration.VERTICAL)
            decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.divider)))
            //添加分割线
            recyclerView?.addItemDecoration(decoration)
            recyclerView?.itemAnimator = DefaultItemAnimator()
            adapter!!.bindToRecyclerView(recyclerView)
            adapter!!.onItemChildClickListener = onItemChildClickListener
        } else {
            isShowPhoneList(false)
        }
    }

    private fun isShowPhoneList(b: Boolean) {
        eye_btn.visibility = View.GONE
        forget_password.visibility = View.GONE
        edit_user_password.visibility = View.GONE
        isPhone = !b
        if (b) {
            qq_btn.visibility = View.GONE
            scan_gp.visibility = View.GONE
            btn_login.visibility = View.GONE
            google_btn.visibility = View.GONE
            facebook_btn.visibility = View.GONE
//            login_isTeck.visibility = View.GONE
            sms_login_btn.visibility = View.GONE
            third_party_text.visibility = View.GONE
            sms_password_login.visibility = View.GONE
            list_phone.visibility = View.VISIBLE
            date_phone.setImageResource(R.drawable.icon_up)
            is_test_version.visibility = View.GONE
            tx_test_version.visibility = View.GONE
            is_dadousmart.visibility = View.GONE
            tx_is_dadousmart.visibility = View.GONE
        } else {
            qq_btn.visibility = View.VISIBLE
            scan_gp.visibility = View.VISIBLE
            btn_login.visibility = View.VISIBLE
            google_btn.visibility = View.VISIBLE
            facebook_btn.visibility = View.VISIBLE
            isDebugVisible()
            sms_login_btn.visibility = View.VISIBLE
            third_party_text.visibility = View.VISIBLE
            sms_password_login.visibility = View.GONE
            edit_user_password.visibility = View.VISIBLE
            forget_password.visibility = View.VISIBLE
            list_phone.visibility = View.GONE
            if(Constant.isShow) {
                is_test_version.visibility = View.VISIBLE
                tx_test_version.visibility = View.VISIBLE
                is_dadousmart.visibility = View.VISIBLE
                tx_is_dadousmart.visibility = View.VISIBLE
            }
            date_phone.setImageResource(R.drawable.icon_down_arr)
        }
    }

    var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { _, view, position ->
        currentUser = phoneList?.get(position)
        if (view.id == R.id.phone_text) {
            edit_user_phone_or_email!!.setText(currentUser!!.phone)
            edit_user_phone_or_email_line.background = getDrawable(R.drawable.line_blue)
            edit_user_password!!.setText(currentUser!!.password)
            list_phone?.visibility = View.GONE
            edit_user_password.visibility = View.VISIBLE
            btn_login.visibility = View.VISIBLE
            eye_btn.visibility = View.VISIBLE
            isDebugVisible()
            SharedPreferencesHelper.putBoolean(TelinkApplication.getInstance(), Constant.NOT_SHOW, true)
            login()
        }
        if (view.id == R.id.delete_image) {
            AlertDialog.Builder(Objects.requireNonNull<Activity>(this)).setMessage(R.string.delete_user)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        DBUtils.deleteUser(currentUser!!)
                        newPhoneList()
                    }
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show()
        }
    }

    private fun isDebugVisible() {
//        when {
//            Constant.isDebug -> login_isTeck.visibility = View.VISIBLE
//            else -> login_isTeck.visibility = View.GONE
//        }
    }

    private fun newPhoneList() {
        var message: List<String>? = null
        val info = SharedPreferencesUtils.getLastUser()
        if (info != null && info.isNotEmpty()) {
            message = info.split("-")
            edit_user_phone_or_email.setText(message[0])
            edit_user_phone_or_email_line.background = getDrawable(R.drawable.line_blue)
            edit_user_password!!.setText(message[1])
            edit_user_password!!.visibility = View.GONE

            if (currentUser?.phone == message!![0]) {
                SharedPreferencesHelper.removeKey(this, Constant.USER_INFO)
                edit_user_phone_or_email!!.setText("")
                edit_user_phone_or_email_line.background = getDrawable(R.drawable.line_gray)
                edit_user_password!!.setText("")
                list_phone.visibility = View.GONE
                edit_user_password.visibility = View.GONE
                btn_login.visibility = View.VISIBLE
                forget_password.visibility = View.GONE
                eye_btn.visibility = View.GONE
                sms_password_login.visibility = View.GONE
                third_party_text.visibility = View.VISIBLE
                qq_btn.visibility = View.VISIBLE
                google_btn.visibility = View.VISIBLE
                facebook_btn.visibility = View.VISIBLE
                isPhone = true
                date_phone.setImageResource(R.drawable.icon_down_arr)
            }
        }

        notifyData()
    }

    fun notifyData() {
        val mOldDatas: MutableList<DbUser>? = phoneList
        val mNewDatas: MutableList<DbUser>? = getNewData()
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return mOldDatas?.get(oldItemPosition)?.id?.equals(mNewDatas?.get
                (newItemPosition)?.id) ?: false
            }

            override fun getOldListSize(): Int {
                return mOldDatas?.size ?: 0
            }

            override fun getNewListSize(): Int {
                return mNewDatas?.size ?: 0
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val beanOld = mOldDatas?.get(oldItemPosition)
                val beanNew = mNewDatas?.get(newItemPosition)
                return if (!beanOld?.name.equals(beanNew?.name)) {
                    return false//如果有内容不同，就返回false
                } else true
            }
        }, true)
        adapter?.let { diffResult.dispatchUpdatesTo(it) }
        phoneList = (mNewDatas as ArrayList<DbUser>?)!!
        if (phoneList!!.size == 0) {
            list_phone.visibility = View.GONE
            edit_user_password.visibility = View.VISIBLE
            btn_login.visibility = View.VISIBLE
            forget_password.visibility = View.VISIBLE
            isPhone = true
            date_phone.visibility = View.VISIBLE
        } else {
            adapter!!.setNewData(phoneList)
        }
    }

    private fun getNewData(): MutableList<DbUser> {
        phoneList = DBUtils.getAllUser()
        return phoneList as ArrayList<DbUser>
    }

    private fun forgetPassword() {
        var intent = Intent(this@LoginActivity, ForgetPassWordActivity::class.java)
        intent.putExtra("fromLogin", "forgetPassword")
        returnView()
        startActivityForResult(intent, 0)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> ActivityUtils.finishAllActivities(true)
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("CheckResult")
    private fun login() {
        phone = edit_user_phone_or_email!!.text.toString().trim { it <= ' ' }.replace(" ".toRegex(), "")
        editPassWord = edit_user_password!!.text.toString().trim { it <= ' ' }.replace(" ".toRegex(), "")
        TelinkLightService.Instance()?.idleMode(true)
        if (!StringUtils.isTrimEmpty(phone) && !StringUtils.isTrimEmpty(editPassWord)) {
            showLoadingDialog(getString(R.string.logging_tip))
            userLogin()
        } else {
            Toast.makeText(this, getString(R.string.phone_or_password_can_not_be_empty), Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("CheckResult")
    private fun userLogin() {
        val subscribe = timer(15000, TimeUnit.MILLISECONDS).subscribe {
            hideLoadingDialog()
        }
        AccountModel.login(phone!!, editPassWord!!)
                .subscribe({
                    DBUtils.deleteLocalData()
                    SharedPreferencesUtils.saveLastUser("$phone-$editPassWord")
                    //判断是否用户是首次在这个手机登录此账号，是则同步数据
                    SyncDataPutOrGetUtils.syncGetDataStart(it, syncCallback)
                    SharedPreferencesUtils.setUserLogin(true)
                }, {
                    LogUtils.d("logging: " + "登录错误" + it.message)
                    ToastUtils.showShort(it.message)
//                    Toast.makeText(this,"账号密码错误",Toast.LENGTH_SHORT).show()
                    hideLoadingDialog()
                })
    }

    var isSuccess: Boolean = true
    internal var syncCallback: SyncCallback = object : SyncCallback {
        override fun start() {}
        override fun complete() {
            if (isSuccess) syncComplet()
        }

        override fun error(msg: String) {
            val ishowRegionDialog = SharedPreferencesHelper.getBoolean(TelinkApplication.getInstance().mContext, Constant.IS_SHOW_REGION_DIALOG, false)
            if (ishowRegionDialog) {
                initMe()
                initAuthor()
                popAuthor?.showAtLocation(window.decorView, Gravity.CENTER, 0, 100)
            }
            isSuccess = false
            hideLoadingDialog()
            SharedPreferencesHelper.putBoolean(TelinkLightApplication.getApp(), Constant.IS_LOGIN, false)
        }
    }

    private fun initMe() {
        val disposable = RegionModel.get()?.subscribe({
            setMeRegion(it)
        }, {
            ToastUtils.showLong(it.message)
        })
    }

    @SuppressLint("CheckResult")
    private fun initAuthor() {
        RegionModel.getAuthorizerList()?.subscribe({
            setAuthorizeRegion(it)
        }, {
            ToastUtils.showLong(it.message)
        })
    }

    @SuppressLint("StringFormatMatches")
    private fun setAuthorizeRegion(authorList: MutableList<RegionAuthorizeBean>) {
        poptitleAuthorize?.text = getString(R.string.received_net_num, authorList.size)
        mAuthorList = authorList
        itemAdapterAuthor = RegionAuthorizeDialogAdapter(R.layout.region_dialog_item, mAuthorList)
        popRecycleAuthorize?.adapter = itemAdapterAuthor
        itemAdapterAuthor?.setOnItemClickListener { _, _, position ->
            regionBeanAuthorize = authorList[position]
            when (whoClick) {
                NONE -> itemAdapterAuthor?.data?.get(position)?.is_selected = true
                AUTHOR ->//上次点击的自己区域不用遍历其他人
                    if (itemAdapterAuthor != null)
                        for (i in itemAdapterAuthor!!.data.indices)
                            itemAdapterAuthor!!.data[i].is_selected = i == position
                ME -> initMe() //上次点击个人区域 这次自己 个人全是false
            }
            itemAdapterAuthor?.notifyDataSetChanged()
            whoClick = AUTHOR
        }
    }

    @SuppressLint("StringFormatInvalid", "StringFormatMatches")
    private fun setMeRegion(list: MutableList<RegionBean>) {
        poptitle?.text = getString(R.string.me_net_num, list.size)
        mList = list
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

    private fun syncComplet() {
        SharedPreferencesHelper.putBoolean(TelinkLightApplication.getApp(), Constant.IS_LOGIN, true)
        transformView()
        hideLoadingDialog()
        TelinkLightApplication.getApp().lastMeshAddress = DBUtils.getlastDeviceMesh()
    }

    private fun transformView() {
        ActivityUtils.finishAllActivities(true)
        Thread.sleep(200)
        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_FIRST_USER) {
                setResult(Activity.RESULT_FIRST_USER)
                finish()
            }
        }
    }

    companion object {
        val IS_FIRST_LAUNCH = "IS_FIRST_LAUNCH"
    }

    override fun afterTextChanged(p0: Editable?) {
        when {
            TextUtils.isEmpty(p0.toString()) -> {
                btn_login.background = getDrawable(R.drawable.btn_rec_black_bt)
                edit_user_phone_or_email_line.background = getDrawable(R.drawable.line_gray)
            }
            else -> {
                edit_user_phone_or_email_line.background = getDrawable(R.drawable.line_blue)
                btn_login.background = getDrawable(R.drawable.btn_rec_blue_bt)
            }
        }
    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
    override fun loginOutMethod() {}
}

package com.dadoutek.uled.user

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.PowerManager
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.util.DiffUtil
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.MenuItem
import android.view.View
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.StringUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.light.DeviceScanningNewActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbUser
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.LogUtils
import com.dadoutek.uled.util.SharedPreferencesUtils
import com.dadoutek.uled.util.ToastUtil
import com.telink.TelinkApplication
import com.xiaomi.market.sdk.Log
import com.xiaomi.market.sdk.XiaomiUpdateAgent
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.toast
import java.util.*

/**
 * Created by hejiajun on 2018/5/15.
 */

private const val MIN_CLICK_DELAY_TIME = 500

class LoginActivity : TelinkBaseActivity(), View.OnClickListener, TextWatcher {

    private var dbUser: DbUser? = null
    private var phone: String? = null
    private var editPassWord: String? = null
    private var isFirstLauch: Boolean = false
    private var mWakeLock: PowerManager.WakeLock? = null
    private var SAVE_USER_NAME_KEY = "SAVE_USER_NAME_KEY"
    private var recyclerView: RecyclerView? = null
    private var adapter: PhoneListRecycleViewAdapter? = null
    private var phoneList: ArrayList<DbUser>? = null
    private var isPhone = true
    private var currentUser: DbUser? = null
    private var isPassword = false


    private var lastClickTime: Long = 0

    @SuppressLint("InvalidWakeLockTag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        phoneList = DBUtils.getAllUser()
        Log.d("dataSize", phoneList!!.size.toString())
        if (phoneList!!.size == 0) {
            date_phone.visibility = View.GONE
        }
//        detectUpdate()

        initData()
        initView()
        initListener()
    }

    /**
     * 检查App是否有新版本
     */
    private fun detectUpdate() {
        XiaomiUpdateAgent.setCheckUpdateOnlyWifi(true)
        XiaomiUpdateAgent.update(this)
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
                    edit_user_phone_or_email.setSelection(s.length)
                }
                SharedPreferencesHelper.putBoolean(TelinkApplication.getInstance(), Constant.NOT_SHOW, false)
            }
        }
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
        detectUpdate()
        if (mWakeLock != null) {
            mWakeLock!!.acquire()
        }
    }


    private fun initListener() {
        btn_login.setOnClickListener(this)
        btn_register.setOnClickListener(this)
        forget_password.setOnClickListener(this)
        date_phone.setOnClickListener(this)
        eye_btn.setOnClickListener(this)
        sms_password_login.setOnClickListener(this)
        edit_user_phone_or_email.addTextChangedListener(this)
        com.dadoutek.uled.util.StringUtils.initEditTextFilterForRegister(edit_user_phone_or_email)
        com.dadoutek.uled.util.StringUtils.initEditTextFilterForRegister(edit_user_password)
    }

    private fun initView() {
        initToolbar()
        if (SharedPreferencesHelper.getBoolean(this@LoginActivity, Constant.IS_LOGIN, false)) {
            transformView()
        }

        linearLayout_1.setOnClickListener(this)

        recyclerView = findViewById(R.id.list_phone)
        val info = SharedPreferencesUtils.getLastUser()
        if (info != null && !info.isEmpty()) {
            val messge = info.split("-")
            edit_user_phone_or_email!!.setText(messge[0])
            edit_user_password!!.setText(messge[1])
            btn_login.background = getDrawable(R.drawable.btn_rec_blue_bt)
        } else
            btn_login.background = getDrawable(R.drawable.btn_rec_black_bt)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_login -> {
                if (TextUtils.isEmpty(edit_user_phone_or_email.editableText.toString())) {
                    toast(getString(R.string.please_phone_number))
                    return
                }
                val currentTime = Calendar.getInstance().timeInMillis
                if (currentTime - lastClickTime > MIN_CLICK_DELAY_TIME) {
                    lastClickTime = currentTime
                    login()
                }
            }
            R.id.btn_register -> {
                var intent = Intent(this@LoginActivity, RegisterActivity::class.java)
                intent.putExtra("fromLogin", "register")
                startActivity(intent)
            }
            R.id.forget_password -> forgetPassword()
            R.id.date_phone -> phoneList()
            R.id.eye_btn -> eyePassword()
            R.id.sms_password_login -> verificationCode()
            R.id.linearLayout_1 -> {
                list_phone.visibility = View.GONE
                edit_user_password.visibility = View.GONE
                btn_login.visibility = View.VISIBLE
                btn_register.visibility = View.VISIBLE
                forget_password.visibility = View.GONE
                eye_btn.visibility = View.GONE
//                edit_user_password.visibility = View.VISIBLE
                btn_login.visibility = View.VISIBLE
                btn_register.visibility = View.VISIBLE
//                forget_password.visibility = View.VISIBLE
//                eye_btn.visibility = View.VISIBLE
                sms_password_login.visibility = View.VISIBLE
                third_party_text.visibility = View.VISIBLE
                qq_btn.visibility = View.VISIBLE
                google_btn.visibility = View.VISIBLE
                facebook_btn.visibility = View.VISIBLE
                isPhone = true
                date_phone.setImageResource(R.drawable.icon_down)
            }
        }
    }

    private fun verificationCode() {
        var intent = Intent(this@LoginActivity, VerificationCodeActivity::class.java)
        intent.putExtra("type", Constant.TYPE_VERIFICATION_CODE)
        startActivity(intent)
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
            list_phone.visibility = View.VISIBLE
            edit_user_password.visibility = View.GONE
            btn_login.visibility = View.GONE
            eye_btn.visibility = View.GONE
//            btn_register.visibility=View.GONE
            forget_password.visibility = View.GONE
            sms_password_login.visibility = View.GONE
            third_party_text.visibility = View.GONE
            qq_btn.visibility = View.GONE
            google_btn.visibility = View.GONE
            facebook_btn.visibility = View.GONE
            val layoutmanager = LinearLayoutManager(this)
            layoutmanager.orientation = LinearLayoutManager.VERTICAL
            recyclerView!!.layoutManager = layoutmanager
            this.adapter = PhoneListRecycleViewAdapter(R.layout.recyclerview_phone_list, phoneList!!)

            val decoration = DividerItemDecoration(this,
                    DividerItemDecoration
                            .VERTICAL)
            decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color
                    .divider)))
            //添加分割线
            recyclerView?.addItemDecoration(decoration)
            recyclerView?.itemAnimator = DefaultItemAnimator()

//        adapter!!.addFooterView(getFooterView())
            adapter!!.bindToRecyclerView(recyclerView)
            adapter!!.onItemChildClickListener = onItemChildClickListener
            isPhone = false
            date_phone.setImageResource(R.drawable.icon_up)
        } else {
            list_phone.visibility = View.GONE
            edit_user_password.visibility = View.GONE
            btn_login.visibility = View.VISIBLE
            btn_register.visibility = View.VISIBLE
            forget_password.visibility = View.GONE
            eye_btn.visibility = View.GONE
//            edit_user_password.visibility = View.VISIBLE
            btn_login.visibility = View.VISIBLE
            btn_register.visibility = View.VISIBLE
//            forget_password.visibility = View.VISIBLE
//            eye_btn.visibility = View.VISIBLE
            sms_password_login.visibility = View.VISIBLE
            third_party_text.visibility = View.VISIBLE
            qq_btn.visibility = View.VISIBLE
            google_btn.visibility = View.VISIBLE
            facebook_btn.visibility = View.VISIBLE
            isPhone = true
            date_phone.setImageResource(R.drawable.icon_down)
        }
    }

    var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        currentUser = phoneList?.get(position)
        if (view.id == R.id.phone_text) {
            edit_user_phone_or_email!!.setText(currentUser!!.phone)
            edit_user_password!!.setText(currentUser!!.password)
            edit_user_password!!.visibility = View.GONE
            list_phone.visibility = View.GONE
            SharedPreferencesHelper.putBoolean(TelinkApplication.getInstance(), Constant.NOT_SHOW, true)
            login()
        }
        if (view.id == R.id.delete_image) {
            AlertDialog.Builder(Objects.requireNonNull<Activity>(this)).setMessage(R.string.delete_user)
                    .setPositiveButton(android.R.string.ok) { dialog, which ->
                        DBUtils.deleteUser(currentUser!!)
                        newPhoneList()
                    }
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show()
        }
    }

    private fun newPhoneList() {
        var message: List<String>? = null
        val info = SharedPreferencesUtils.getLastUser()
        if (info != null && !info.isEmpty()) {
            message = info.split("-")
            edit_user_phone_or_email.setText(message[0])
            edit_user_password!!.setText(message[1])
            edit_user_password!!.visibility = View.GONE
        }
        if (currentUser!!.phone == message!![0]) {
            SharedPreferencesHelper.removeKey(this, Constant.USER_INFO)
            edit_user_phone_or_email!!.setText("")
            edit_user_password!!.setText("")
            list_phone.visibility = View.GONE
            edit_user_password.visibility = View.GONE
            btn_login.visibility = View.VISIBLE
            btn_register.visibility = View.VISIBLE
            forget_password.visibility = View.GONE
            eye_btn.visibility = View.GONE
            sms_password_login.visibility = View.VISIBLE
            third_party_text.visibility = View.VISIBLE
            qq_btn.visibility = View.VISIBLE
            google_btn.visibility = View.VISIBLE
            facebook_btn.visibility = View.VISIBLE
            isPhone = true
            date_phone.setImageResource(R.drawable.icon_down)
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
            edit_user_password.visibility = View.GONE
            btn_login.visibility = View.VISIBLE
            btn_register.visibility = View.VISIBLE
            forget_password.visibility = View.GONE
            isPhone = true
            date_phone.visibility = View.GONE
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
        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> ActivityUtils.finishAllActivities(true)
        }
        return super.onOptionsItemSelected(item)
    }


    private fun login() {
        phone = edit_user_phone_or_email!!.text.toString().trim { it <= ' ' }.replace(" ".toRegex(), "")
        editPassWord = edit_user_password!!.text.toString().trim { it <= ' ' }.replace(" ".toRegex(), "")

        if (!StringUtils.isTrimEmpty(phone)) {
            var intent = Intent(this, EnterPasswordActivity::class.java)
            intent.putExtra("USER_TYPE", Constant.TYPE_LOGIN)
            intent.putExtra("phone", phone)
            startActivity(intent)
            finish()
        } else {
            ToastUtil.showToast(this, getString(R.string.phone_or_password_can_not_be_empty))
        }
    }

    var isSuccess: Boolean = true
    internal var syncCallback: SyncCallback = object : SyncCallback {

        override fun start() {}

        override fun complete() {
            if (isSuccess) {
                syncComplet()
            }
        }

        override fun error(msg: String) {
            isSuccess = false
            hideLoadingDialog()
            SharedPreferencesHelper.putBoolean(TelinkLightApplication.getInstance(), Constant.IS_LOGIN, false)
            LogUtils.d("GetDataError:" + msg)
        }
    }

    private fun syncComplet() {
        SharedPreferencesHelper.putBoolean(TelinkLightApplication.getInstance(), Constant.IS_LOGIN, true)
        transformView()
        hideLoadingDialog()
    }

    private fun transformView() {
        if (DBUtils.allLight.isEmpty()) {
            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            finish()
        } else {
            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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

    fun addLayoutListener(main: View?, scroll: View?) {
        main!!.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            //1、获取main在窗体的可视区域
            main.getWindowVisibleDisplayFrame(rect)
            //2、获取main在窗体的不可视区域高度，在键盘没有弹起时，main.getRootView().getHeight()调节度应该和rect.bottom高度一样
            val mainInvisibleHeight = main.rootView.height - rect.bottom
            val screenHeight = main.rootView.height//屏幕高度
            //3、不可见区域大于屏幕本身高度的1/4：说明键盘弹起了
            if (mainInvisibleHeight > screenHeight / 4) {
                val location = IntArray(2)
                scroll!!.getLocationInWindow(location)
                // 4､获取Scroll的窗体坐标，算出main需要滚动的高度
                val srollHeight = location[1] + scroll.height - rect.bottom
                //5､让界面整体上移键盘的高度
                main.scrollTo(0, srollHeight)
            } else {
                //3、不可见区域小于屏幕高度1/4时,说明键盘隐藏了，把界面下移，移回到原有高度
                main.scrollTo(0, 0)
            }
        }
    }


    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
    }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

    override fun afterTextChanged(p0: Editable?) {
        if (TextUtils.isEmpty(p0.toString()))
            btn_login.background = getDrawable(R.drawable.btn_rec_black_bt)
        else
            btn_login.background = getDrawable(R.drawable.btn_rec_blue_bt)
    }

}

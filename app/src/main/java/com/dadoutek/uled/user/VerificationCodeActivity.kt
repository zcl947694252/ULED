package com.dadoutek.uled.user

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import cn.smssdk.EventHandler
import cn.smssdk.SMSSDK
import com.blankj.utilcode.util.StringUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DbUser
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.util.NetWorkUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_verification_code.*
import org.jetbrains.anko.toast
import org.json.JSONObject
import java.util.*

/**uu
 * 手机号短信登录第一步
 */
class VerificationCodeActivity : TelinkBaseActivity(), View.OnClickListener, TextWatcher {

    private lateinit var dbUser: DbUser
    private var countryCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification_code)
        initView()
        dbUser = DbUser()
        SMSSDK.registerEventHandler(eventHandler)
    }


    private fun initView() {
        countryCode = country_code_picker?.selectedCountryCode
        country_code_picker?.setOnCountryChangeListener {
            countryCode = country_code_picker?.selectedCountryCode
        }

        btn_register.setOnClickListener(this)
        btn_send_verification.setOnClickListener(this)
        sms_login.setOnClickListener(this)
        edit_user_phone.addTextChangedListener(this)
        password_login.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_register -> register()
            R.id.btn_send_verification -> verificationCode()
            R.id.sms_login ->{
                if (TextUtils.isEmpty(edit_user_phone.editableText.toString())) {
                    toast(getString(R.string.please_phone_number))
                    return
                }
            //login()
                verificationCode()
            }
            R.id.password_login -> passwordLogin()
            R.id.date_phone_list -> phoneList()
        }
    }

    private fun phoneList() {
//        if (isPhone) {
////            list_phone_code.visibility = View.VISIBLE
////            edit_verification.visibility = View.GONE
////            btn_login.visibility = View.GONE
////            eye_btn.visibility = View.GONE
//////            btn_register.visibility=View.GONE
////            forget_password.visibility = View.GONE
////            sms_password_login.visibility = View.GONE
////            third_party_text.visibility = View.GONE
////            qq_btn.visibility = View.GONE
////            google_btn.visibility = View.GONE
////            facebook_btn.visibility = View.GONE
////            val layoutmanager = LinearLayoutManager(this)
////            layoutmanager.orientation = LinearLayoutManager.VERTICAL
////            rvDevice!!.layoutManager = layoutmanager
////            this.adapter = PhoneListRecycleViewAdapter(R.layout.recyclerview_phone_list, phoneList!!)
////
////            val decoration = DividerItemDecoration(this,
////                    DividerItemDecoration
////                            .VERTICAL)
////            decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color
////                    .divider)))
////            //添加分割线
////            rvDevice?.addItemDecoration(decoration)
////            rvDevice?.itemAnimator = DefaultItemAnimator()
////
//////        adapter!!.addFooterView(getFooterView())
////            adapter!!.bindToRecyclerView(rvDevice)
////            adapter!!.onItemChildClickListener = onItemChildClickListener
////            isPhone = false
////            date_phone.setImageResource(R.drawable.icon_up)
////        } else {
////            list_phone.visibility = View.GONE
////            edit_user_password.visibility = View.VISIBLE
////            btn_login.visibility = View.VISIBLE
////            btn_register.visibility = View.VISIBLE
////            forget_password.visibility = View.VISIBLE
////            eye_btn.visibility = View.VISIBLE
////            sms_password_login.visibility = View.VISIBLE
////            third_party_text.visibility = View.VISIBLE
////            qq_btn.visibility = View.VISIBLE
////            google_btn.visibility = View.VISIBLE
////            facebook_btn.visibility = View.VISIBLE
////            isPhone = true
////            date_phone.setImageResource(R.drawable.icon_down)
////        }
    }

    private fun passwordLogin() {
        val intent = Intent(this@VerificationCodeActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    @SuppressLint("CheckResult")
    private fun getAccount() {

        if (NetWorkUtils.isNetworkAvalible(this)) {
            val userName = edit_user_phone.editableText.toString().trim { it <= ' '}
                    .replace(" ".toRegex(), "")
            if (TextUtils.isEmpty(userName)) {
                toast(getString(R.string.please_phone_number))
                return
            } else {
                val map = HashMap<String, String>()
                map["phone"] = userName!!
                map["channel"] = dbUser!!.channel
                NetworkFactory.getApi()
                        .getAccount(userName, dbUser!!.channel)
                        .subscribeOn(Schedulers.io())
                        .compose(NetworkTransformer())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            hideLoadingDialog()
                                dbUser.account =it
                                val intent = Intent(this@VerificationCodeActivity, EnterConfirmationCodeActivity::class.java)
                                intent.putExtra(Constant.TYPE_USER, Constant.TYPE_VERIFICATION_CODE)
                                intent.putExtra("country_code",countryCode)
                                intent.putExtra("phone", userName)
                                intent.putExtra("account", dbUser.account)
                                startActivity(intent)
                        },{
                            hideLoadingDialog()
                            ToastUtils.showShort(it.localizedMessage)
                        })
            }
        } else {
            ToastUtils.showLong(getString(R.string.net_work_error))
        }
    }


    val eventHandler = object : EventHandler() {
            //afterEvent会在子线程被调用，因此如果后续有UI相关操作，需要将数据发送到UI线程
        override fun afterEvent(event: Int, result: Int, data: Any?) {
            val msg = Message()
            msg.arg1 = event
            msg.arg2 = result
            msg.obj = data
            Handler(Looper.getMainLooper(), Handler.Callback { msg ->
                val event = msg.arg1
                val result = msg.arg2
                val data = msg.obj
                if (event == SMSSDK.EVENT_GET_VERIFICATION_CODE) {
                    if (result == SMSSDK.RESULT_COMPLETE) {
                        // TODO 处理成功得到验证码的结果
                        ToastUtils.showLong(R.string.send_message_success)
                        getAccount()
                    } else {
                        // TODO 处理错误的结果
                        if (result == SMSSDK.RESULT_ERROR) {
                            try {
                                val a = (data as Throwable)
                                val jsonObject = JSONObject(a.localizedMessage)
                                val message = jsonObject.opt("detail").toString()
                                ToastUtils.showLong(message)
                            }catch (ex:Exception){
                                ex.printStackTrace()
                            }
                        } else {
                            val a = (data as Throwable)
                            a.printStackTrace()
                            ToastUtils.showLong(a.message)
                        }
                    }
                    hideLoadingDialog()
                }
                false
            }).sendMessage(msg)
        }
    }

    internal var syncCallback: SyncCallback = object : SyncCallback {

        override fun start() {
            showLoadingDialog(getString(R.string.tip_start_sync))
        }

        override fun complete() {
            hideLoadingDialog()
            syncComplet()
        }

        override fun error(msg: String) {
            hideLoadingDialog()
            //("GetDataError:$msg")
            ToastUtils.showShort(msg)
        }

    }

    private fun syncComplet() {
//        ToastUtils.showLong(getString(R.string.upload_complete))
        hideLoadingDialog()
        TransformView()
    }

    private fun TransformView() {
        startActivity(Intent(this@VerificationCodeActivity, MainActivity::class.java))
        finish()
    }


    private fun verificationCode() {
        if (NetWorkUtils.isNetworkAvalible(this)) {
            val phoneNum = edit_user_phone.getText().toString().trim({ it <= ' ' })
            //("zcl**********************$phoneNum")
            if (StringUtils.isEmpty(phoneNum)) {
                ToastUtils.showShort(R.string.phone_cannot_be_empty)
            } else {
                showLoadingDialog(getString(R.string.get_code_ing))
                SMSSDK.getVerificationCode(countryCode, phoneNum)
            }
        } else {
            ToastUtils.showLong(getString(R.string.net_work_error))
        }
    }


    private fun register() {
        val intent = Intent(this@VerificationCodeActivity, RegisterActivity::class.java)
        intent.putExtra("fromLogin", "register")
        startActivity(intent)
    }

    override fun afterTextChanged(p0: Editable?) {}

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        if (TextUtils.isEmpty(p0.toString()))
            sms_login.background = getDrawable(R.drawable.btn_rec_black_bt)
        else
            sms_login.background = getDrawable(R.drawable.btn_rec_blue_bt)
    }
}

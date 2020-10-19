package com.dadoutek.uled.user

import android.annotation.SuppressLint
import android.app.Activity
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
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.StringUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constants
import com.dadoutek.uled.model.dbModel.DbUser
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.othersview.CountryActivity
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.othersview.RegisterActivity
import com.dadoutek.uled.util.NetWorkUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_verification_code.*
import kotlinx.android.synthetic.main.activity_verification_code.btn_send_verification
import kotlinx.android.synthetic.main.activity_verification_code.ccp_tv
import kotlinx.android.synthetic.main.activity_verification_code.country_code_arrow
import kotlinx.android.synthetic.main.activity_verification_code.edit_user_phone
import org.jetbrains.anko.toast
import org.json.JSONObject
import java.util.*

/**uu
 * 手机号短信登录第一步
 */
class VerificationCodeActivity : TelinkBaseActivity(), View.OnClickListener, TextWatcher {

    private lateinit var dbUser: DbUser
    private var countryCode: String = "86"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification_code)
        initView()
        dbUser = DbUser()
        SMSSDK.registerEventHandler(eventHandler)
    }


    private fun initView() {
        /*  countryCode = country_code_picker?.selectedCountryCode
          country_code_picker?.setOnCountryChangeListener {
              countryCode = country_code_picker?.selectedCountryCode
          }*/

        //btn_register.setOnClickListener(this)
        btn_send_verification.setOnClickListener(this)
        sms_login.setOnClickListener(this)
        edit_user_phone.addTextChangedListener(this)
        password_login.setOnClickListener(this)
        country_code_arrow.setOnClickListener(this)
        return_image.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.country_code_arrow -> {
                val intent = Intent()
                intent.setClass(this@VerificationCodeActivity, CountryActivity::class.java)
                startActivityForResult(intent, 10)
            }
            R.id.btn_register -> register()
            R.id.return_image -> finish()
            R.id.btn_send_verification -> verificationCode()
            R.id.sms_login -> {
                if (TextUtils.isEmpty(edit_user_phone.editableText.toString())) {
                    toast(getString(R.string.please_phone_number))
                    return
                }
                //login()
                verificationCode()
            }
            R.id.password_login -> passwordLogin()
            //R.id.date_phone_list -> phoneList()
        }
    }

    private fun passwordLogin() {
        val intent = Intent(this@VerificationCodeActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    @SuppressLint("CheckResult")
    private fun getAccount() {
        if (NetWorkUtils.isNetworkAvalible(this)) {
            val userName = edit_user_phone.editableText.toString().trim { it <= ' ' }
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
                        .subscribe( {
                                hideLoadingDialog()
                                dbUser.account = it

                                val intent = Intent(this@VerificationCodeActivity, EnterConfirmationCodeActivity::class.java)
                                intent.putExtra(Constants.TYPE_USER, Constants.TYPE_VERIFICATION_CODE)
                                intent.putExtra("country_code", countryCode)
                                intent.putExtra("phone", userName)
                                intent.putExtra("account", dbUser.account)
                                startActivity(intent)
                            }, {
                                hideLoadingDialog()
                                ToastUtils.showLong(it.localizedMessage)
                        })
            }
        } else {
            ToastUtils.showLong(getString(R.string.network_unavailable))
        }
    }

    private val eventHandler = object : EventHandler() {
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
                        // 处理成功得到验证码的结果
                        ToastUtils.showLong(R.string.send_message_success)
                        getAccount()
                    } else {
                        // 处理错误的结果
                        if (result == SMSSDK.RESULT_ERROR) {
                            try {
                                val a = (data as Throwable)
                                val jsonObject = JSONObject(a.localizedMessage)
                                val message = jsonObject.opt("detail").toString()
                                ToastUtils.showLong(message)
                            } catch (ex: Exception) {
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
            ToastUtils.showLong(msg)
        }

    }

    private fun syncComplet() {
//        ToastUtils.showLong(getString(R.string.upload_complete))
        hideLoadingDialog()
        transformView()
    }

    private fun transformView() {
        startActivity(Intent(this@VerificationCodeActivity, MainActivity::class.java))
        finish()
    }


    private fun verificationCode() {
        if (NetWorkUtils.isNetworkAvalible(this)) {
            val phoneNum = edit_user_phone.text.toString().trim { it <= ' ' }
            //("zcl**********************$phoneNum")
            if (StringUtils.isEmpty(phoneNum)) {
                ToastUtils.showLong(R.string.phone_cannot_be_empty)
            } else {
                showLoadingDialog(getString(R.string.get_code_ing))
                SMSSDK.getVerificationCode(countryCode, phoneNum)
            }
        } else {
            ToastUtils.showLong(getString(R.string.network_unavailable))
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
        if (TextUtils.isEmpty(p0.toString())) {
            sms_login.isClickable = false
            sms_login.background = getDrawable(R.drawable.btn_rec_black_c8)
        } else {
            sms_login.isClickable = true
            sms_login.background = getDrawable(R.drawable.btn_rec_blue_bt)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            10 -> if (resultCode == Activity.RESULT_OK) {
                val bundle = data?.extras
                val countryName = bundle?.getString("countryName")
                val countryNumber = bundle?.getString("countryNumber")
                val toString = countryNumber?.replace("+", "").toString()

                LogUtils.v("zcl------------------countryCode接手前$countryCode")
                if (TextUtils.isEmpty(countryCode))
                    return
                LogUtils.v("zcl------------------countryCode接收后$countryCode")
                countryCode = toString
                ccp_tv.text = countryName + countryNumber
            }
            else -> {
            }
        }
    }
}

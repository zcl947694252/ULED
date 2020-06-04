package com.dadoutek.uled.othersview

<<<<<<< HEAD:app/src/main/java/com/dadoutek/uled/othersview/RegisterActivity.kt
import android.annotation.SuppressLint
import android.app.Activity
=======
import android.content.Context
>>>>>>> 3b3ee83a8b42c273c18c9b1bfb8b91f8262b9d3e:app/src/main/java/com/dadoutek/uled/user/RegisterActivity.kt
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.*
import android.text.method.HideReturnsTransformationMethod
import android.text.method.LinkMovementMethod
import android.text.method.PasswordTransformationMethod
import android.text.style.ClickableSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
<<<<<<< HEAD:app/src/main/java/com/dadoutek/uled/othersview/RegisterActivity.kt
=======
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import butterknife.ButterKnife
>>>>>>> 3b3ee83a8b42c273c18c9b1bfb8b91f8262b9d3e:app/src/main/java/com/dadoutek/uled/user/RegisterActivity.kt
import cn.smssdk.EventHandler
import cn.smssdk.SMSSDK
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DbUser
import com.dadoutek.uled.model.HttpModel.UpdateModel
import com.dadoutek.uled.network.NetworkObserver
<<<<<<< HEAD:app/src/main/java/com/dadoutek/uled/othersview/RegisterActivity.kt
import com.dadoutek.uled.user.EnterConfirmationCodeActivity
=======
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.othersview.UserAgreementActivity
>>>>>>> 3b3ee83a8b42c273c18c9b1bfb8b91f8262b9d3e:app/src/main/java/com/dadoutek/uled/user/RegisterActivity.kt
import com.dadoutek.uled.util.NetWorkUtils
import com.dadoutek.uled.util.PopUtil
import com.dadoutek.uled.util.StringUtils
import kotlinx.android.synthetic.main.activity_forget_password.*
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.android.synthetic.main.activity_register.edit_user_phone
import kotlinx.android.synthetic.main.activity_register.register_completed
import org.json.JSONObject
import kotlinx.android.synthetic.main.activity_forget_password.register_phone_line as register_phone_line1

/**
 * Created by hejiajun on 2018/5/16.
 */

class RegisterActivity : TelinkBaseActivity(), View.OnClickListener, TextWatcher {
<<<<<<< HEAD:app/src/main/java/com/dadoutek/uled/othersview/RegisterActivity.kt
=======

    private var popUserAgreement: PopupWindow? = null
>>>>>>> 3b3ee83a8b42c273c18c9b1bfb8b91f8262b9d3e:app/src/main/java/com/dadoutek/uled/user/RegisterActivity.kt
    private var userName: String? = null
    private var countryCode: String = "86"
    private var isChangePwd = false
    private var dbUser: DbUser? = null
    private var isPassword = false
    private var isPasswordAgain = false
    private var isFrist = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setContentView(R.layout.activity_register)
<<<<<<< HEAD:app/src/main/java/com/dadoutek/uled/othersview/RegisterActivity.kt
        initView()
        initListener()
        SMSSDK.registerEventHandler(eventHandler)
    }

    private fun initListener() {
        register_completed.setOnClickListener(this)
        btn_send_verification.setOnClickListener(this)
        image_password_btn.setOnClickListener(this)
        image_again_password_btn.setOnClickListener(this)
        return_image.setOnClickListener(this)
        country_code_arrow.setOnClickListener(this)
}

    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            10 -> if (resultCode == Activity.RESULT_OK) {
                val bundle = data?.extras
                val countryName = bundle?.getString("countryName")
                val countryNumber = bundle?.getString("countryNumber")
                ccp_tv.text = countryName+countryNumber
                countryCode = countryNumber?.replace("+","").toString()

                LogUtils.v("zcl------------------countryCode$countryCode")
            }
            0->{
                if ( resultCode == Activity.RESULT_FIRST_USER) {
                    setResult(Activity.RESULT_FIRST_USER)
                    finish()
                }
            }
            else -> {
=======
        isFrist = true
        initView()
        makePop()
        SMSSDK.registerEventHandler(eventHandler)
    }

    private fun makePop() {
        popView?.let {
            val userAgreenment = it.findViewById<TextView>(R.id.code_warm_user_agreenment)
            val ss = SpannableString(getString(R.string.user_agreement_context))//已同意《用户协议及隐私说明》
            var cs: ClickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    var intent = Intent(this@RegisterActivity, UserAgreementActivity::class.java)
                    startActivity(intent)
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = Color.BLUE//设置超链接的颜色
                    ds.isUnderlineText = false
                }
            }
            var start = if (isZh(this)) 3 else 0
            var end = if (isZh(this)) ss.length else ss.length - 17
            ss.setSpan(cs, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            userAgreenment.text = ss
            userAgreenment?.movementMethod = LinkMovementMethod.getInstance()//必须要加

            it.findViewById<LinearLayout>(R.id.pop_view).background = getDrawable(R.drawable.rect_r15_w)
            it.findViewById<TextView>(R.id.code_warm_hinit).text = getString(R.string.privacy_statement)
            it.findViewById<TextView>(R.id.code_warm_title).visibility = View.GONE
            it.findViewById<LinearLayout>(R.id.code_warm_user_ly).visibility = View.VISIBLE
            it.findViewById<TextView>(R.id.code_warm_context).gravity = Gravity.CENTER_VERTICAL
            it.findViewById<TextView>(R.id.code_warm_context).text = getString(R.string.privacy_statement_content)
            val cb = it.findViewById<CheckBox>(R.id.code_warm_cb)
            val iSee = it.findViewById<TextView>(R.id.code_warm_i_see)
            cb.setOnCheckedChangeListener { _, isChecked ->
                iSee.text = if (isChecked)
                    getString(R.string.i_see)
                else
                    getString(R.string.read_agreen)
            }
            iSee.setOnClickListener {
                if (cb.isChecked) {
                    PopUtil.dismiss(popUserAgreement)
                } else
                    ToastUtils.showShort(getString(R.string.read_agreen))
            }

            popUserAgreement = PopupWindow(popView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            popUserAgreement!!.let { itp ->
                itp.isFocusable = true // 设置PopupWindow可获得焦点
                itp.isTouchable = true // 设置PopupWindow可触摸补充：
                itp.isOutsideTouchable = false
            }
        }
    }


    private fun isZh(context: Context): Boolean {
        val locale = context.resources.configuration.locale
        val language = locale.language
        return language.endsWith("zh")
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus&&isFrist){
            try {
                if (!this@RegisterActivity.isFinishing && !popUserAgreement!!.isShowing){
                 isFrist =false
                    popUserAgreement!!.showAtLocation(window.decorView, Gravity.CENTER, 0, 50)
                }
            } catch (e: Exception) {
                LogUtils.v("zcl弹框出现问题${e.localizedMessage}")
>>>>>>> 3b3ee83a8b42c273c18c9b1bfb8b91f8262b9d3e:app/src/main/java/com/dadoutek/uled/user/RegisterActivity.kt
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        PopUtil.dismiss(popUserAgreement)
        SMSSDK.unregisterEventHandler(eventHandler)
    }

    private fun initView() {
        val changeKey = intent.getStringExtra("fromLogin")
        isChangePwd = changeKey != "register"
        StringUtils.initEditTextFilterForRegister(edit_user_phone)
        StringUtils.initEditTextFilterForRegister(edit_user_password)
        StringUtils.initEditTextFilterForRegister(again_password)

        edit_user_phone.addTextChangedListener(this)

        if (isChangePwd) {
            dbUser = DbUser()
            register_completed.setText(R.string.confirm)
        }
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.country_code_arrow -> {
                val intent = Intent()
                intent.setClass(this@RegisterActivity, CountryActivity::class.java)
                startActivityForResult(intent, 10)
            }
            R.id.register_completed -> {
                makePop()
                return
                if (NetWorkUtils.isNetworkAvalible(this)) {
                    userName = edit_user_phone!!.text.toString().trim { it <= ' ' }
                    if (compileExChar(userName!!)) {
                        ToastUtils.showLong(R.string.phone_input_error)
                        return
                    }
                    if (com.blankj.utilcode.util.StringUtils.isEmpty(userName)) {
                        ToastUtils.showLong(R.string.phone_cannot_be_empty)
                        return
                    }

                    UpdateModel.isRegister(userName!!)?.subscribe(object : NetworkObserver<Boolean?>() {
                        override fun onNext(t: Boolean) {
                            if (!t) {
                                regist_frist_progress.visibility = View.GONE
                                SMSSDK.getVerificationCode(countryCode, userName)

                            } else {
                                ToastUtils.showLong(getString(R.string.account_exist))
                            }
                        }
                    })

                } else {
                    ToastUtils.showLong(getString(R.string.net_work_error))
                }
            }
            R.id.btn_send_verification ->
                if (NetWorkUtils.isNetworkAvalible(this)) {
                    sendVerification()
                } else {
                    ToastUtils.showLong(getString(R.string.net_work_error))
                }

            R.id.image_password_btn -> eyePassword()
            R.id.image_again_password_btn -> eyePasswordAgain()
            R.id.return_image -> {
                SMSSDK.unregisterEventHandler(eventHandler)
                finish()
            }

        }
    }

    private val eventHandler = object : EventHandler() {
        override fun afterEvent(event: Int, result: Int, data: Any?) {
            // afterEvent会在子线程被调用，因此如果后续有UI相关操作，需要将数据发送到UI线程
            val msg = Message()
            msg.arg1 = event
            msg.arg2 = result
            msg.obj = data
            Handler(Looper.getMainLooper(), Handler.Callback { msg ->
                val event = msg.arg1
                val result = msg.arg2
                val data = msg.obj

                regist_frist_progress.visibility = View.GONE
                register_completed.isClickable = true

                if (event == SMSSDK.EVENT_GET_VERIFICATION_CODE) {
                    if (result == SMSSDK.RESULT_COMPLETE) {
                        // 请注意，此时只是完成了发送验证码的请求，验证码短信还需要几秒钟之后才送达
                        ToastUtils.showLong(R.string.send_message_success)
                        goSkipActivity()
                    } else {
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
                }
                false
            }).sendMessage(msg)
        }
    }

    private fun goSkipActivity() {
        register_completed.isClickable = false
        var intent = Intent(this@RegisterActivity, EnterConfirmationCodeActivity::class.java)
        intent.putExtra(Constant.TYPE_USER, Constant.TYPE_REGISTER)
        intent.putExtra("country_code", countryCode)
        intent.putExtra("phone", edit_user_phone!!.text.toString().trim { it <= ' ' }.replace(" ".toRegex(), ""))

        startActivityForResult(intent,0)
    }
    private fun eyePasswordAgain() {
        if (isPasswordAgain) {
            image_again_password_btn.setImageResource(R.drawable.icon_turn)
            isPasswordAgain = false
            again_password.transformationMethod = PasswordTransformationMethod.getInstance()
            again_password.setSelection(again_password.text.length)
        } else {
            isPasswordAgain = true
            image_again_password_btn.setImageResource(R.drawable.icon_open_eye)
            again_password.transformationMethod = HideReturnsTransformationMethod.getInstance()
            again_password.setSelection(again_password.text.length)
        }
    }

    private fun eyePassword() {
        if (isPassword) {
            image_password_btn.setImageResource(R.drawable.icon_turn)
            isPassword = false
            edit_user_password.transformationMethod = PasswordTransformationMethod.getInstance()
            edit_user_password.setSelection(edit_user_password.text.length)
        } else {
            isPassword = true
            image_password_btn.setImageResource(R.drawable.icon_open_eye)
            edit_user_password.transformationMethod = HideReturnsTransformationMethod.getInstance()
            edit_user_password.setSelection(edit_user_password.text.length)
        }
    }

    private fun sendVerification() {
        val phoneNum = edit_user_phone.text.toString().trim { it <= ' ' }
        if (com.blankj.utilcode.util.StringUtils.isEmpty(phoneNum)) {
            ToastUtils.showLong(R.string.phone_cannot_be_empty)
        } else {
            SMSSDK.getVerificationCode(countryCode, phoneNum)
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
            ToastUtils.showLong(msg)
        }
    }

    private fun syncComplet() {
        hideLoadingDialog()
        transformView()
    }

    private fun transformView() {
        startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
        SMSSDK.unregisterEventHandler(eventHandler)
        finish()
    }

    override fun afterTextChanged(p0: Editable?) {}

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        if (TextUtils.isEmpty(p0.toString())) {
            register_completed.background = getDrawable(R.drawable.btn_rec_black_bt)
            register_phone_line_b.background = getDrawable(R.drawable.line_gray)
        } else {
            register_completed.background = getDrawable(R.drawable.btn_rec_blue_bt)
            register_phone_line_b.background = getDrawable(R.drawable.line_blue)
        }
    }
}


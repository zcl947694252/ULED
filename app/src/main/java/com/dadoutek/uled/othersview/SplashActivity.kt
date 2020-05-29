package com.dadoutek.uled.othersview

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.othersview.SplashActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkMeshErrorDealActivity
import com.dadoutek.uled.user.LoginActivity
import com.dadoutek.uled.util.PopUtil
import com.telink.bluetooth.TelinkLog
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.android.synthetic.main.activity_splash.*

/**
 * Created by hejiajun on 2018/3/22.
 */
class SplashActivity : TelinkMeshErrorDealActivity(), View.OnClickListener {
    private var mApplication: TelinkLightApplication? = null
    var mIsFirstData = true
    var mIsLogging = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        init()
        initListener()
    }

    private fun initListener() {
        splash_to_login.setOnClickListener(this)
        splash_to_regist.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        disableConnectionStatusListener()
    }

    private fun init() {
        mApplication = this.application as TelinkLightApplication
        TelinkLog.onDestroy()
        mApplication!!.initData()

        //判断是否是第一次使用app，启动导航页
        mIsFirstData = SharedPreferencesHelper.getBoolean(this@SplashActivity, IS_FIRST_LAUNCH, true)
        mIsLogging = SharedPreferencesHelper.getBoolean(this@SplashActivity, Constant.IS_LOGIN, false)
        if (mIsLogging) {
            ActivityUtils.startActivityForResult(this, MainActivity::class.java, 0)
            finish()
        } /* else {
            gotoLoginSetting(false);
        }*/
    }

    private fun gotoLoginSetting(isFrist: Boolean) {
        val intent = Intent(this@SplashActivity, LoginActivity::class.java)
        intent.putExtra(IS_FIRST_LAUNCH, isFrist)
        startActivityForResult(intent, 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == Activity.RESULT_FIRST_USER) {
            finish()
        }
    }

    override fun onLocationEnable() {}

    companion object {
        const val IS_FIRST_LAUNCH = "IS_FIRST_LAUNCH"
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.splash_to_login -> gotoLoginSetting(false)

            R.id.splash_to_regist -> {
                val style = SpannableStringBuilder(getString(R.string.user_agreement_context))//已同意《用户协议及隐私说明》
                if (isZh(this))
                        style.setSpan(ForegroundColorSpan(getColor(R.color.blue_text)), 3, style.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                else//《User agreement and privacy notes》have been agreed.
                        style.setSpan(ForegroundColorSpan(getColor(R.color.blue_text)), 0, style.length - 17, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                popView?.let {
                    it.findViewById<TextView>(R.id.code_warm_hinit).text = getString(R.string.privacy_statement)
                    it.findViewById<TextView>(R.id.code_warm_title).visibility = View.GONE
                    it.findViewById<TextView>(R.id.code_warm_user_ly).visibility = View.VISIBLE
                    it.findViewById<TextView>(R.id.code_warm_context).gravity = Gravity.CENTER_VERTICAL
                    val cb = it.findViewById<CheckBox>(R.id.code_warm_cb)
                    it.findViewById<TextView>(R.id.code_warm_context).text = getString(R.string.privacy_statement_content)
                    it.findViewById<TextView>(R.id.code_warm_i_see).setOnClickListener {
                        if (cb.isChecked) {
                            PopUtil.dismiss(pop)
                            goRegist()
                        } else {
                            ToastUtils.showShort(getString(R.string.please_select_scene))
                        }
                    }

                    try {
                        if (!this@SplashActivity.isFinishing && !pop!!.isShowing)
                            pop!!.showAtLocation(window.decorView, Gravity.CENTER, 0, 50)
                    } catch (e: Exception) {
                        LogUtils.v("zcl弹框出现问题${e.localizedMessage}")
                    }
                }
            }
        }
    }
    private fun isZh(context: Context): Boolean {
        val locale = context.resources.configuration.locale
        val language = locale.language
        return language.endsWith("zh")
    }


    private fun goRegist() {
        val intent = Intent(this@SplashActivity, RegisterActivity::class.java)
        intent.putExtra(IS_FIRST_LAUNCH, false)
        startActivityForResult(intent, 0)
    }
}
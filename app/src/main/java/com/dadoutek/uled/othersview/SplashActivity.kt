package com.dadoutek.uled.othersview

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
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
import java.util.*

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
                popView?.let {
                    val userAgreenment = it.findViewById<TextView>(R.id.code_warm_user_agreenment)
                    val ss = SpannableString(getString(R.string.user_agreement_context))//已同意《用户协议及隐私说明》
                    var cs: ClickableSpan = object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            var intent = Intent(this@SplashActivity, InstructionsForUsActivity::class.java)
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
                    userAgreenment?.movementMethod = LinkMovementMethod.getInstance()//必须要加否则不能点击

                    it.findViewById<LinearLayout>(R.id.pop_view).background = getDrawable(R.drawable.rect_r15_de)
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
                            PopUtil.dismiss(pop)
                            goRegist()
                        } else
                            ToastUtils.showShort(getString(R.string.read_agreen))
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

    override fun onDestroy() {
        super.onDestroy()
        PopUtil.dismiss(pop)
        PopUtil.dismiss(popMain)
    }
}
package com.dadoutek.uled.othersview.start

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.othersview.SplashActivity
import com.dadoutek.uled.othersview.UserAgreementActivity
import com.dadoutek.uled.util.PopUtil
import kotlinx.android.synthetic.main.dialog_install_list.*
import kotlin.system.exitProcess

class FirstActivity : AppCompatActivity() {

    private var mHandler: Handler? = null
    private var goTo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first)
        mHandler = Handler()
        if (!SharedPreferencesHelper.getBoolean(this, "PERMISSION_POLICY_AGREE", false)) {
            mHandler!!.post(runnable)
        } else {
            val intent = Intent(this@FirstActivity,SplashActivity::class.java)
            startActivity(intent)
        }
    }

    private val runnable = object : Runnable {
        override fun run() {
            val view = findViewById<TextView>(R.id.first_start)
            // 如何根元素的width和height大于0说明activity已经初始化完毕
            if (view != null && view.width > 0 && view.height > 0) {
                // 显示popwindow
                if (!goTo)
                    requestPer()
                else {
                    Thread.sleep(400)
                    gotoAnother()
                }
                mHandler!!.removeCallbacks(this);
            } else {
                // 如果activity没有初始化完毕则等待5毫秒再次检测
                mHandler!!.postDelayed(this, 10);
            }
        }
    }


    @SuppressLint("CutPasteId")
    private fun requestPer() {
        val popView: View = layoutInflater.inflate(R.layout.code_warm,null)
        val pop: PopupWindow = PopupWindow(popView,ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT)
        popView.let {
            val userAgreenment = it.findViewById<TextView>(R.id.code_warm_user_agreenment)
            val ss = SpannableString(getString(R.string.user_agreement_context))//已同意《用户协议及隐私说明》
            val cs: ClickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val intent = Intent(this@FirstActivity, UserAgreementActivity::class.java)
                    startActivity(intent)
                }
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = Color.BLUE//设置超链接的颜色
                    ds.isUnderlineText = false
                }
            }
            val start = if (isZh(this)) 3 else 0
            val end = if (isZh(this)) ss.length else ss.length - 17
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
            val cancle = it.findViewById<TextView>(R.id.cancel_tv)
            cancle.visibility = View.VISIBLE
            iSee.setOnClickListener {
                if (cb.isChecked) {
                    SharedPreferencesHelper.putBoolean(this, "PERMISSION_POLICY_AGREE", true)
                    PopUtil.dismiss(pop)
                    val intent = Intent(this@FirstActivity,SplashActivity::class.java)
                    startActivity(intent)
                } else
                    ToastUtils.showShort(getString(R.string.read_agreen))
            }
            cancle.setOnClickListener {
                ToastUtils.showShort(getString(R.string.must_agree))
                PopUtil.dismiss(pop)
                goTo = true
                mHandler!!.post(runnable)
            }
            try {
//                if (!this@SplashActivity.isFinishing && !pop.isShowing)
//                pop.showAtLocation(window.decorView, Gravity.CENTER, 0, 50)
                pop.showAsDropDown(popView)
            } catch (e: Exception) {
                PopUtil.dismiss(pop)
                LogUtils.v("chown弹框出现问题${e.localizedMessage}")
            }
        }
    }


    private fun gotoAnother() {
        val popView: View = layoutInflater.inflate(R.layout.code_warm,null)
        val pop: PopupWindow = PopupWindow(popView,ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT)
        popView.let {
            val codeWarm: TextView = it.findViewById(R.id.code_warm_context)
            val ss = SpannableString(getString(R.string.agree_and_continue))//请同意《用户协议及隐私说明》才能继续使用此软件
            val cs: ClickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val intent = Intent(this@FirstActivity, UserAgreementActivity::class.java)
                    startActivity(intent)
                }
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = Color.BLUE //设置超链接的颜色
                    ds.isUnderlineText = false
                }
            }
            val start = if (isZh(this)) 3 else 16
            val end = if (isZh(this)) ss.length - 9 else 50
            ss.setSpan(cs, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            codeWarm.text = ss
            codeWarm.movementMethod = LinkMovementMethod.getInstance()//必须要加否则不能点击

            it.findViewById<CheckBox>(R.id.code_warm_cb).visibility = View.GONE
            it.findViewById<TextView>(R.id.code_warm_title).visibility = View.GONE
            it.findViewById<LinearLayout>(R.id.code_warm_user_ly).visibility = View.GONE
            it.findViewById<TextView>(R.id.code_warm_user_agreenment).visibility = View.GONE
            it.findViewById<LinearLayout>(R.id.pop_view).background = getDrawable(R.drawable.rect_r15_de)
            it.findViewById<TextView>(R.id.code_warm_hinit).text = getString(R.string.reminder)
            codeWarm.gravity = Gravity.CENTER_VERTICAL
            val iSee = it.findViewById<TextView>(R.id.code_warm_i_see)
            val cancle = it.findViewById<TextView>(R.id.cancel_tv)
            cancle.visibility = View.VISIBLE
            cancle.text = getString(R.string.insist_on_quitting)
            iSee.text = getString(R.string.agree_to_continue)
            iSee.setOnClickListener {
                SharedPreferencesHelper.putBoolean(this, "PERMISSION_POLICY_AGREE", true)
                PopUtil.dismiss(pop)
                val intent = Intent(this@FirstActivity,SplashActivity::class.java)
                startActivity(intent)
            }
            cancle.setOnClickListener {
                exitProcess(0)
            }
            try {
                pop.showAsDropDown(popView)
            } catch (e: Exception) {
                PopUtil.dismiss(pop)
                LogUtils.v("chown弹框出现问题${e.localizedMessage}")
            }

        }
    }

    private fun isZh(context: Context): Boolean {
        val locale = context.resources.configuration.locale
        val language = locale.language
        return language.endsWith("zh")
    }

}
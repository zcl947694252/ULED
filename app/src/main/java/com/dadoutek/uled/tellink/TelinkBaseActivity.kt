package com.dadoutek.uled.tellink

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.util.StringUtils
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

import java.util.regex.Matcher
import java.util.regex.Pattern

open class TelinkBaseActivity : AppCompatActivity() {

    protected var toast: Toast? = null
    protected var foreground = false
    private var loadDialog: Dialog? = null

    @SuppressLint("ShowToast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.toast = Toast.makeText(this, "", Toast.LENGTH_SHORT)
        foreground = true
    }

    override fun onPause() {
        super.onPause()
        foreground = false
    }


    override fun onResume() {
        super.onResume()
        foreground = true
    }

    override fun onDestroy() {
        super.onDestroy()

        this.toast!!.cancel()
        this.toast = null
    }

    fun showToast(s: CharSequence) {
        ToastUtils.showLong(s)
    }

    protected fun saveLog(log: String) {
        (application as TelinkLightApplication).saveLog(log)
    }

    fun showLoadingDialog(content: String) {
        val inflater = LayoutInflater.from(this)
        val v = inflater.inflate(R.layout.dialogview, null)

        val layout = v.findViewById<View>(R.id.dialog_view) as LinearLayout
        val tvContent = v.findViewById<View>(R.id.tvContent) as TextView
        tvContent.text = content


        //        ImageView spaceshipImage = (ImageView) v.findViewById(R.id.img);
        //
        //        @SuppressLint("ResourceType") Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(this,
        //                R.animator.load_animation);

        //        spaceshipImage.startAnimation(hyperspaceJumpAnimation);

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
                launch(UI){
                    loadDialog!!.show()
                }
            }
        }
    }

    fun hideLoadingDialog() {
        launch(UI){
            if (loadDialog != null) {
                loadDialog!!.dismiss()
            }
        }
    }

    fun compileExChar(str: String): Boolean {
        return StringUtils.compileExChar(str)
    }

    fun endCurrentGuide() {}
}

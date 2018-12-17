package com.dadoutek.uled.util

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import com.app.hubert.guide.NewbieGuide
import com.app.hubert.guide.core.Builder
import com.app.hubert.guide.core.Controller
import com.app.hubert.guide.model.GuidePage
import com.app.hubert.guide.model.HighlightOptions
import com.dadoutek.uled.R
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.tellink.TelinkLightApplication

object GuideUtils {
    /**
     * 每次引导的标志
     */
    var STEP0_GUIDE_SELECT_DEVICE_KEY = "STEP0_GUIDE_SELECT_DEVICE_KEY"
    var STEP1_GUIDE_ADD_DEVICE_KEY = "STEP1_GUIDE_ADD_DEVICE_KEY"
    var STEP2_GUIDE_START_INSTALL_DEVICE = "STEP2_GUIDE_START_INSTALL_DEVICE"
    var STEP3_GUIDE_CREATE_GROUP = "STEP3_GUIDE_CREATE_GROUP"
    var STEP4_GUIDE_SELECT_GROUP = "STEP4_GUIDE_SELECT_GROUP"
    var STEP5_GUIDE_SELECT_SOME_LIGHT = "STEP5_GUIDE_SELECT_SOME_LIGHT"
    var STEP6_GUIDE_SURE_GROUP = "STEP6_GUIDE_SURE_GROUP"
    var STEP7_GUIDE_ADD_SCENE = "STEP7_GUIDE_ADD_SCENE"
    var STEP8_GUIDE_ADD_SCENE_ADD_GROUP = "STEP8_GUIDE_ADD_SCENE_ADD_GROUP"
    var STEP9_GUIDE_ADD_SCENE_SURE = "STEP9_GUIDE_ADD_SCENE_SURE"
    var STEP10_GUIDE_ADD_SCENE_CHANGE_BRIGHTNESS = "STEP10_GUIDE_ADD_SCENE_CHANGE_BRIGHTNESS"
    var STEP11_GUIDE_ADD_SCENE_CHANGE_TEMPERATURE = "STEP11_GUIDE_ADD_SCENE_CHANGE_TEMPERATURE"
    var STEP12_GUIDE_ADD_SCENE_CHANGE_COLOR = "STEP12_GUIDE_ADD_SCENE_CHANGE_COLOR"
    var STEP13_GUIDE_ADD_SCENE_SAVE = "STEP13_GUIDE_ADD_SCENE_SAVE"

    var STEP14_GUIDE_APPLY_SCENE = "STEP14_GUIDE_APPLY_SCENE"
    var STEP15_GUIDE_CONSTENT_QUESTION = "STEP15_GUIDE_CONSTENT_QUESTION"
    var MAIN_STEP0_GUIDE_TO_SCENE = "MAIN_STEP0_GUIDE_TO_SCENE"

    var ADDITIONAL_SCENE_GUIDE_KEY_INPUT_NAME="ADDITIONAL_SCENE_GUIDE_KEY_INPUT_NAME"
    var ADDITIONAL_GUIDE_SET_SCENE="ADDITIONAL_GUIDE_SET_SCENE"
    /**
     * 每个页面引导结束标志
     * 本次取值为true则当前页不引导
     */
    var END_GROUPLIST_KEY = "END_GROUPLIST_KEY"
    var END_MAIN_KEY = "END_MAIN_KEY"
    var END_INSTALL_LIGHT_KEY = "END_INSTALL_LIGHT_KEY"
    var END_ADD_SCENE_KEY = "END_ADD_SCENE_KEY"

    fun addGuidePage(guideTargetView: View,
                     res: Int, describeRes: String, onClickListener: View.OnClickListener,jumpViewContent: String,context: Context): GuidePage {
        val guide = GuidePage.newInstance()
                .setLayoutRes(res)
                .setEverywhereCancelable(false)
                .setOnLayoutInflatedListener { view, controller ->
                    val tvGuide = view.findViewById<TextView>(R.id.show_guide_content)
                    tvGuide.text = describeRes

                    val known = view.findViewById<TextView>(R.id.kown)
                    known.setOnClickListener { v -> controller.remove() }

                    val tvJump = view.findViewById<TextView>(R.id.jump_out)
                    tvJump.setOnClickListener { v ->
                        showExitGuideDialog(context,controller,jumpViewContent)
                    }
                }
        val highlightOptions = HighlightOptions.Builder()
                .setOnClickListener(onClickListener)
                .build()
        guide.addHighLightWithOptions(guideTargetView, highlightOptions)

        return guide
    }

    //显示退出引导弹窗
    fun showExitGuideDialog(context: Context, controller: Controller, jumpViewContent: String){
        val builder=AlertDialog.Builder(context)
        var alertDialog:AlertDialog?=null
        builder.setMessage(context.getString(R.string.jump_out_guide_tip))
        builder.setNegativeButton(R.string.btn_cancel) { dialog, which ->
            alertDialog?.dismiss()
        }
        builder.setPositiveButton(android.R.string.ok) { dialog, which ->
            controller.remove()
            if(jumpViewContent== END_GROUPLIST_KEY){
                changeCurrentViewIsEnd(TelinkLightApplication.getInstance(), END_GROUPLIST_KEY,true)
            }else if(jumpViewContent== END_INSTALL_LIGHT_KEY){
                changeCurrentViewIsEnd(TelinkLightApplication.getInstance(), END_INSTALL_LIGHT_KEY,true)
            }else if(jumpViewContent== END_ADD_SCENE_KEY){
                changeCurrentViewIsEnd(TelinkLightApplication.getInstance(), END_ADD_SCENE_KEY,true)
            }else if(jumpViewContent== END_MAIN_KEY){
                changeCurrentViewIsEnd(TelinkLightApplication.getInstance(), END_MAIN_KEY,true)
            }
            alertDialog?.dismiss()
        }
        alertDialog=builder.create()
        alertDialog.show()
    }


    fun addGuidePage(guideTargetView: View,
                     res: Int, describeRes: String): GuidePage {


        return GuidePage.newInstance()
                .addHighLight(guideTargetView)
                .setLayoutRes(res)
                .setEverywhereCancelable(false)
                .setOnLayoutInflatedListener { view, controller ->
                    val tvGuide = view.findViewById<TextView>(R.id.show_guide_content)
                    tvGuide.text = describeRes

                    val known = view.findViewById<TextView>(R.id.kown)
                    known.setOnClickListener { v -> controller.remove() }

                    val tvJump = view.findViewById<TextView>(R.id.jump_out)
                    tvJump.setOnClickListener { v -> controller.remove() }
                }
    }

    fun guideBuilder(fragment: Fragment, label: String): Builder {
        return NewbieGuide.with(fragment).setShowCounts(1).setLabel(label)
//        testMode()
//        return NewbieGuide.with(fragment).setShowCounts(1).setLabel(label).alwaysShow(true)
    }

    fun guideBuilder(activity: AppCompatActivity, label: String): Builder {
        return NewbieGuide.with(activity).setShowCounts(1).setLabel(label)
//        testMode()
//        return NewbieGuide.with(activity).setShowCounts(1).setLabel(label).alwaysShow(true)
    }

    fun guideBuilder(activity: AppCompatActivity,v: View, label: String): Builder {
//        return NewbieGuide.with(activity).setShowCounts(1).setLabel(label)
        return NewbieGuide.with(activity).setShowCounts(1).setLabel(label).alwaysShow(true).anchor(v)
    }

    fun guideBuilder(fragment: Fragment,label: String,v: View): Builder {
//        return NewbieGuide.with(activity).setShowCounts(1).setLabel(label)
        return NewbieGuide.with(fragment).setShowCounts(1).setLabel(label).alwaysShow(true).anchor(v)
    }

    fun testMode(){
        SharedPreferencesHelper.putBoolean(TelinkLightApplication.getInstance(), END_ADD_SCENE_KEY,false)
        SharedPreferencesHelper.putBoolean(TelinkLightApplication.getInstance(), END_GROUPLIST_KEY,false)
        SharedPreferencesHelper.putBoolean(TelinkLightApplication.getInstance(), END_INSTALL_LIGHT_KEY,false)
    }
    /**
     * 重置所有引导页
     */
    fun resetAllGuide(activity: Activity) {
        NewbieGuide.resetLabel(activity, STEP0_GUIDE_SELECT_DEVICE_KEY)
        NewbieGuide.resetLabel(activity, STEP1_GUIDE_ADD_DEVICE_KEY)
        NewbieGuide.resetLabel(activity, STEP2_GUIDE_START_INSTALL_DEVICE)
        NewbieGuide.resetLabel(activity, STEP3_GUIDE_CREATE_GROUP)
        NewbieGuide.resetLabel(activity, STEP4_GUIDE_SELECT_GROUP)
        NewbieGuide.resetLabel(activity, STEP5_GUIDE_SELECT_SOME_LIGHT)
        NewbieGuide.resetLabel(activity, STEP6_GUIDE_SURE_GROUP)
        NewbieGuide.resetLabel(activity, STEP7_GUIDE_ADD_SCENE)
        NewbieGuide.resetLabel(activity, STEP8_GUIDE_ADD_SCENE_ADD_GROUP)
        NewbieGuide.resetLabel(activity, STEP9_GUIDE_ADD_SCENE_SURE)
        NewbieGuide.resetLabel(activity, STEP10_GUIDE_ADD_SCENE_CHANGE_BRIGHTNESS)
        NewbieGuide.resetLabel(activity, STEP11_GUIDE_ADD_SCENE_CHANGE_TEMPERATURE)
        NewbieGuide.resetLabel(activity, STEP12_GUIDE_ADD_SCENE_CHANGE_COLOR)
        NewbieGuide.resetLabel(activity, STEP13_GUIDE_ADD_SCENE_SAVE)
        NewbieGuide.resetLabel(activity, STEP14_GUIDE_APPLY_SCENE)
        NewbieGuide.resetLabel(activity, STEP15_GUIDE_CONSTENT_QUESTION)
        NewbieGuide.resetLabel(activity, MAIN_STEP0_GUIDE_TO_SCENE)
        NewbieGuide.resetLabel(activity, ADDITIONAL_SCENE_GUIDE_KEY_INPUT_NAME)
        NewbieGuide.resetLabel(activity, ADDITIONAL_GUIDE_SET_SCENE)
        SharedPreferencesHelper.putBoolean(activity, END_ADD_SCENE_KEY,false)
        SharedPreferencesHelper.putBoolean(activity, END_GROUPLIST_KEY,false)
        SharedPreferencesHelper.putBoolean(activity, END_INSTALL_LIGHT_KEY,false)
        SharedPreferencesHelper.putBoolean(activity, END_MAIN_KEY,false)
    }

    /**
     * 重置组页面引导
     */
    fun resetGroupListGuide(activity: Activity){
        NewbieGuide.resetLabel(activity, STEP0_GUIDE_SELECT_DEVICE_KEY)
        NewbieGuide.resetLabel(activity, STEP1_GUIDE_ADD_DEVICE_KEY)
        NewbieGuide.resetLabel(activity, STEP2_GUIDE_START_INSTALL_DEVICE)
        SharedPreferencesHelper.putBoolean(activity, END_GROUPLIST_KEY,false)
    }

    fun resetMainGuide(activity: Activity){
        NewbieGuide.resetLabel(activity, MAIN_STEP0_GUIDE_TO_SCENE)
        SharedPreferencesHelper.putBoolean(activity, END_MAIN_KEY,false)
    }

    /**
     * 重置扫灯页面引导
     */
    fun resetDeviceScanningGuide(activity: Activity){
        NewbieGuide.resetLabel(activity, STEP3_GUIDE_CREATE_GROUP)
        NewbieGuide.resetLabel(activity, STEP4_GUIDE_SELECT_GROUP)
        NewbieGuide.resetLabel(activity, STEP5_GUIDE_SELECT_SOME_LIGHT)
        NewbieGuide.resetLabel(activity, STEP6_GUIDE_SURE_GROUP)
        SharedPreferencesHelper.putBoolean(activity, END_INSTALL_LIGHT_KEY,false)
    }

    /**
     * 重置场景引导
     */
    fun resetSceneGuide(activity: Activity){
        NewbieGuide.resetLabel(activity, STEP7_GUIDE_ADD_SCENE)
        NewbieGuide.resetLabel(activity, STEP8_GUIDE_ADD_SCENE_ADD_GROUP)
        NewbieGuide.resetLabel(activity, STEP9_GUIDE_ADD_SCENE_SURE)
        NewbieGuide.resetLabel(activity, STEP10_GUIDE_ADD_SCENE_CHANGE_BRIGHTNESS)
        NewbieGuide.resetLabel(activity, STEP11_GUIDE_ADD_SCENE_CHANGE_TEMPERATURE)
        NewbieGuide.resetLabel(activity, STEP12_GUIDE_ADD_SCENE_CHANGE_COLOR)
        NewbieGuide.resetLabel(activity, STEP13_GUIDE_ADD_SCENE_SAVE)
        NewbieGuide.resetLabel(activity, STEP14_GUIDE_APPLY_SCENE)
        NewbieGuide.resetLabel(activity, ADDITIONAL_SCENE_GUIDE_KEY_INPUT_NAME)
        NewbieGuide.resetLabel(activity, ADDITIONAL_GUIDE_SET_SCENE)
        SharedPreferencesHelper.putBoolean(activity, END_ADD_SCENE_KEY,false)
    }

    //控制以页面为单位是否结束引导
    fun changeCurrentViewIsEnd(context: Context,key:String,isEnd:Boolean){
        SharedPreferencesHelper.putBoolean(context,key,isEnd)
    }

    //获取页面是否引导已经结束
    fun getCurrentViewIsEnd(context: Context,key:String,isEnd:Boolean): Boolean {
        return SharedPreferencesHelper.getBoolean(context,key,isEnd)
    }
}

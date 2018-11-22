package com.dadoutek.uled.util

import android.app.Activity
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView

import com.app.hubert.guide.NewbieGuide
import com.app.hubert.guide.core.Builder
import com.app.hubert.guide.model.GuidePage
import com.app.hubert.guide.model.HighlightOptions
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.othersview.MainActivity

object GuideUtils {

    var STEP1_GUIDE_ADD_DEVICE_KEY = "STEP1_GUIDE_ADD_DEVICE_KEY"
    var STEP2_GUIDE_ADD_GROUP_KEY = "STEP2_GUIDE_ADD_GROUP_KEY"
    var STEP3_GUIDE_SELECT_GROUP = "STEP3_GUIDE_SELECT_GROUP"
    var STEP4_GUIDE_SELECT_SOME_LIGHT = "STEP4_GUIDE_SELECT_SOME_LIGHT"
    var STEP5_GUIDE_SURE_GROUP = "STEP5_GUIDE_SURE_GROUP"
    var STEP6_GUIDE_ADD_SCENE = "STEP6_GUIDE_ADD_SCENE"
    var STEP7_GUIDE_ADD_SCENE_ADD_GROUP = "STEP7_GUIDE_ADD_SCENE_ADD_GROUP"
    var STEP8_GUIDE_ADD_SCENE_SELECT_GROUP = "STEP8_GUIDE_ADD_SCENE_SELECT_GROUP"
    var STEP9_GUIDE_ADD_SCENE_SAVE = "STEP9_GUIDE_ADD_SCENE_SAVE"
    var STEP10_GUIDE_CONSTENT_QUESTION = "STEP10_GUIDE_CONSTENT_QUESTION"

    fun addGuidePage(guideTargetView: View,
                     res: Int, describeRes: String, options: HighlightOptions): GuidePage {
        return GuidePage.newInstance()
                .addHighLightWithOptions(guideTargetView, options)
                .setLayoutRes(res)
                .setEverywhereCancelable(false)
                .setOnLayoutInflatedListener { view, controller ->
                    val tvGuide = view.findViewById<TextView>(R.id.show_guide_content)
                    tvGuide.text = describeRes
                    val tvJump = view.findViewById<TextView>(R.id.jump_out)
                    tvJump.setOnClickListener { v -> controller.remove() }
                }
    }

    fun addGuidePage(guideTargetView: View?,
                     res: Int, describeRes: String): GuidePage {
        return GuidePage.newInstance()
                .addHighLight(guideTargetView)
                .setLayoutRes(res)
                .setEverywhereCancelable(false)
                .setOnLayoutInflatedListener { view, controller ->
                    val tvGuide = view.findViewById<TextView>(R.id.show_guide_content)
                    tvGuide.text = describeRes
                    val tvJump = view.findViewById<TextView>(R.id.jump_out)
                    tvJump.setOnClickListener { v -> controller.remove() }
                }
    }

    fun guideBuilder(fragment: Fragment, label: String): Builder {
        //        return NewbieGuide.with(fragment).setShowCounts(1).setLabel(label);
        return NewbieGuide.with(fragment).setShowCounts(1).setLabel(label).alwaysShow(true)
    }

    fun guideBuilder(activity: AppCompatActivity, label: String): Builder {
        //        return NewbieGuide.with(activity).setShowCounts(1).setLabel(label);
        return NewbieGuide.with(activity).setShowCounts(1).setLabel(label).alwaysShow(true)
    }

    fun resetAllGuide(activity: Activity) {
        NewbieGuide.resetLabel(activity, STEP1_GUIDE_ADD_DEVICE_KEY)
        NewbieGuide.resetLabel(activity, STEP2_GUIDE_ADD_GROUP_KEY)
        NewbieGuide.resetLabel(activity, STEP3_GUIDE_SELECT_GROUP)
        NewbieGuide.resetLabel(activity, STEP4_GUIDE_SELECT_SOME_LIGHT)
        NewbieGuide.resetLabel(activity, STEP5_GUIDE_SURE_GROUP)
        NewbieGuide.resetLabel(activity, STEP6_GUIDE_ADD_SCENE)
        NewbieGuide.resetLabel(activity, STEP7_GUIDE_ADD_SCENE_ADD_GROUP)
        NewbieGuide.resetLabel(activity, STEP8_GUIDE_ADD_SCENE_SELECT_GROUP)
        NewbieGuide.resetLabel(activity, STEP9_GUIDE_ADD_SCENE_SAVE)
        NewbieGuide.resetLabel(activity, STEP10_GUIDE_CONSTENT_QUESTION)
    }
}

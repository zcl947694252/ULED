package com.dadoutek.uled.util

import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import com.app.hubert.guide.NewbieGuide
import com.app.hubert.guide.core.Builder
import com.app.hubert.guide.model.GuidePage
import com.app.hubert.guide.model.HighlightOptions
import com.dadoutek.uled.R

object GuideUtils {

    fun addGuidePage(guideTargetView: View,
                     res: Int, describeRes: String, onClickListener: View.OnClickListener): GuidePage {
        val guide = GuidePage.newInstance()
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
        val highlightOptions = HighlightOptions.Builder()
                .setOnClickListener(onClickListener)
                .build()
        guide.addHighLightWithOptions(guideTargetView, highlightOptions)

        return guide
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
        //        return NewbieGuide.with(fragment).setShowCounts(1).setLabel(label).alwaysShow(true);
    }

    fun guideBuilder(activity: AppCompatActivity, label: String): Builder {
        return NewbieGuide.with(activity).setShowCounts(1).setLabel(label)
        //        return NewbieGuide.with(activity).setShowCounts(1).setLabel(label).alwaysShow(true);
    }
}

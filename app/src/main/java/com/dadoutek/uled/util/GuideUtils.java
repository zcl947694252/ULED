package com.dadoutek.uled.util;

import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.app.hubert.guide.NewbieGuide;
import com.app.hubert.guide.core.Builder;
import com.app.hubert.guide.model.GuidePage;
import com.dadoutek.uled.R;

public class GuideUtils {

    public static GuidePage addGuidePage( View guideTargetView,
                                     int res, String describeRes){
        return GuidePage.newInstance()
                        .addHighLight(guideTargetView)
                        .setLayoutRes(res)
                        .setOnLayoutInflatedListener((view, controller) -> {
                            TextView tvGuide = view.findViewById(R.id.show_guide_content);
                            tvGuide.setText(describeRes);

                            TextView tvJump = view.findViewById(R.id.jump_out);
                            tvJump.setOnClickListener(v -> {
                                controller.remove();
                            });
                        });
    }

    public static Builder guideBuilder(Fragment fragment,String label){
        return NewbieGuide.with(fragment).setShowCounts(1).setLabel(label);
//        return NewbieGuide.with(fragment).setShowCounts(1).setLabel(label).alwaysShow(true);
    }

    public static Builder guideBuilder(AppCompatActivity activity,String label){
        return NewbieGuide.with(activity).setShowCounts(1).setLabel(label);
//        return NewbieGuide.with(activity).setShowCounts(1).setLabel(label).alwaysShow(true);
    }
}

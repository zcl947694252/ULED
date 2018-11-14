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
                            TextView tv_Guide = view.findViewById(R.id.show_guide_content);
                            tv_Guide.setText(describeRes);
                        });
    }

//    public static Builder showGuide(AppCompatActivity activity){
//
//    }
}

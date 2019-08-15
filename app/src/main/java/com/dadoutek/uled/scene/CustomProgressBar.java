package com.dadoutek.uled.scene;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.dadoutek.uled.R;

/**
 * 创建者     ZCL
 * 创建时间   2019/8/15 10:03
 * 描述	      ${TODO}
 * <p>
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${TODO}
 */
public class CustomProgressBar extends Dialog {
    private Context context ;
    private String progressText ;

    public CustomProgressBar(Context context) {
        super(context , R.style.dialog_theme) ;
        this.context = context ;
    }
    public CustomProgressBar(Context context, String progressText) {
        super(context, R.style.dialog_theme) ;
        this.context = context ;
        this.progressText = progressText ;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_progressbar) ;
        TextView title =   findViewById(R.id.custom_imageview_progress_title);
        title.setText(progressText == null ? "加载数据中，请稍后..." : progressText) ;
    }
    /**
     * @see android.app.Dialog#show()
     */
    @Override
    public void show() {
        try{
            if(!isShowing()){
                super.show();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        ImageView im =   findViewById(R.id.custom_imageview_progress_bar);
        im.startAnimation(AnimationUtils.loadAnimation(context, R.anim.round_loading));
    }

}


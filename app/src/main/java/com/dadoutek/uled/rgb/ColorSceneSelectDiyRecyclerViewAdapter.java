package com.dadoutek.uled.rgb;

import android.graphics.drawable.GradientDrawable;
import android.support.annotation.Nullable;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.dadoutek.uled.R;
import com.dadoutek.uled.model.ItemColorPreset;

import java.util.List;

/**
 * Created by hejiajun on 2018/3/28.
 */

public class ColorSceneSelectDiyRecyclerViewAdapter extends BaseQuickAdapter<ItemColorPreset, BaseViewHolder> {

    public ColorSceneSelectDiyRecyclerViewAdapter(int layoutResId, @Nullable List<ItemColorPreset> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, ItemColorPreset item) {
        helper.addOnClickListener(R.id.btn_diy_preset)
                .addOnLongClickListener(R.id.btn_diy_preset)
                .setText(R.id.btn_diy_preset, "")
                .setBackgroundColor(R.id.btn_diy_preset,0xff000000|item.getColor());
    }
}

package com.dadoutek.uled.rgb;

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

public class ColorSelectDiyRecyclerViewAdapter extends BaseQuickAdapter<ItemColorPreset, BaseViewHolder> {

    public ColorSelectDiyRecyclerViewAdapter(int layoutResId, @Nullable List<ItemColorPreset> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, ItemColorPreset item) {
        helper.addOnClickListener(R.id.btn_diy_preset)
                .addOnLongClickListener(R.id.btn_diy_preset)
                .setText(R.id.btn_diy_preset, (item.getBrightness()==-1?"":item.getBrightness()+ "%"))
                .setBackgroundColor(R.id.btn_diy_preset,0xff000000|item.getColor());
    }
}

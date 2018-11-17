package com.dadoutek.uled.rgb;

import android.support.annotation.Nullable;

import com.chad.library.adapter.base.BaseItemDraggableAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.dadoutek.uled.R;
import com.dadoutek.uled.model.DbModel.DbGroup;
import com.dadoutek.uled.model.ItemRgbGradient;
import com.dadoutek.uled.tellink.TelinkLightApplication;

import java.util.List;

public class RGBGradientAdapter extends BaseItemDraggableAdapter<ItemRgbGradient, BaseViewHolder> {


    public RGBGradientAdapter(int layoutResId, @Nullable List<ItemRgbGradient> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, ItemRgbGradient item) {
            helper.setText(R.id.modeName,item.getName())
                    .addOnClickListener(R.id.modeName);
        }

    }

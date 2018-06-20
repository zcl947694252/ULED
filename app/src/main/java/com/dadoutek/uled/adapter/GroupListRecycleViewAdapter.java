package com.dadoutek.uled.adapter;

import android.support.annotation.Nullable;

import com.chad.library.adapter.base.BaseItemDraggableAdapter;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.dadoutek.uled.R;
import com.dadoutek.uled.model.DbModel.DbGroup;

import java.util.List;

public class GroupListRecycleViewAdapter extends BaseItemDraggableAdapter<DbGroup, BaseViewHolder> {


    public GroupListRecycleViewAdapter(int layoutResId, @Nullable List<DbGroup> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, DbGroup group) {

        if (group != null) {
            if (group.textColor == 0)
                group.textColor = mContext.getResources()
                        .getColor(R.color.black);

            helper.setText(R.id.txt_name, group.getName())
                    .setTextColor(R.id.txt_name, group.textColor)
                    .addOnClickListener(R.id.txt_name).
                    addOnClickListener(R.id.btn_on).
                    addOnClickListener(R.id.btn_off).
                    addOnClickListener(R.id.btn_set);
        }
    }
}

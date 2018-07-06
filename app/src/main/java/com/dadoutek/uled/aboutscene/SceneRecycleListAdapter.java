package com.dadoutek.uled.aboutscene;

import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseItemDraggableAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.dadoutek.uled.R;
import com.dadoutek.uled.model.DbModel.DbGroup;
import com.dadoutek.uled.model.DbModel.DbScene;

import java.util.List;

public class SceneRecycleListAdapter extends BaseItemDraggableAdapter<DbScene, BaseViewHolder> {

    boolean isDelete;

    public SceneRecycleListAdapter(int layoutResId, @Nullable List<DbScene> data,boolean isDelete) {
        super(layoutResId, data);
        this.isDelete=isDelete;
    }

    @Override
    protected void convert(BaseViewHolder helper, DbScene scene) {
        if (scene != null) {
            TextView deleteIcon=helper.getView(R.id.scene_delete);
            if(isDelete){
                deleteIcon.setVisibility(View.VISIBLE);
            }else{
                deleteIcon.setVisibility(View.GONE);
            }

            helper.setText(R.id.scene_name,scene.getName()).
                    addOnClickListener(R.id.scene_delete).
                    addOnClickListener(R.id.scene_edit);
        }
    }

    public void changeState(boolean isDelete) {
        this.isDelete = isDelete;
    }
}

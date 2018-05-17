package com.dadoutek.uled.adapter;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.dadoutek.uled.DbModel.DbScene;
import com.dadoutek.uled.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hejiajun on 2018/5/5.
 */

public class SwitchSceneGroupAdapter extends BaseQuickAdapter implements AdapterView.OnItemSelectedListener {

    private List<String> btList;
    private List<DbScene> sceneList;
    private List<String> sceneNameList;
    private Spinner spinner;
    private ArrayAdapter<String> arrayAdapter;
    private Context context;
    private Map<Integer, DbScene> map;

    public SwitchSceneGroupAdapter(int layoutResId, List<String> btList, List<DbScene> sceneList, Context context) {
        super(layoutResId, btList);
        this.btList = btList;
        this.sceneList = sceneList;
        sceneNameList = new ArrayList<>();
        map = new HashMap<>();
        this.context = context;
        init();
        this.arrayAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, sceneNameList);
    }

    private void init() {
        for (int i = 0; i < sceneList.size(); i++) {
            sceneNameList.add(sceneList.get(i).getName());
        }
    }

    @Override
    protected void convert(BaseViewHolder helper, Object item) {
        spinner = helper.getView(R.id.sp_scene);
        spinner.setAdapter(arrayAdapter);
        spinner.setTag(helper.getAdapterPosition());
        spinner.setSelection(0);
        helper.setOnItemSelectedClickListener(R.id.sp_scene, this);
        helper.setAdapter(R.id.sp_scene, arrayAdapter);

        String name = (String) item;
        helper.setText(R.id.tv_scene_button_name, name);

        DbScene dbScene = sceneList.get(0);
        map.put(helper.getAdapterPosition(), dbScene);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getTag() != null) {
            int rvPos = (Integer) parent.getTag();
            Log.d(TAG, "onItemSelected: rvPos = " + rvPos + "position = " + position);
//        spinner = (Spinner) getViewByPosition(position, R.id.sp_scene);
//        int positionScene = (int) spinner.getTag();
            DbScene dbScene = sceneList.get(position);
            map.put(rvPos, dbScene);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public Map<Integer, DbScene> getSceneMap() {
        return map;
    }
}

package com.dadoutek.uled.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.dadoutek.uled.DbModel.DbScene;
import com.dadoutek.uled.DbModel.DbSceneActions;
import com.dadoutek.uled.DbModel.DbSceneActionsUtils;
import com.dadoutek.uled.R;
import com.dadoutek.uled.TelinkLightService;
import com.dadoutek.uled.model.Group;
import com.dadoutek.uled.model.ItemGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hejiajun on 2018/5/5.
 */

public class SwitchSceneGroupAdapter extends BaseQuickAdapter implements AdapterView.OnItemSelectedListener{

    List<String> btList;
    List<DbScene> sceneList;
    List<String> sceneNameList;
    Spinner spinner;
    ArrayAdapter<String> arrayAdapter;
    Context context;
    public Map<String,DbScene> map;

    public SwitchSceneGroupAdapter(int layoutResId, List<String> btList, List<DbScene> sceneList, Context context) {
        super(layoutResId, btList);
        this.btList=btList;
        this.sceneList=sceneList;
        sceneNameList=new ArrayList<>();
        map=new HashMap<>();
        this.context=context;
        init();
        this.arrayAdapter=new ArrayAdapter<String>(context,android.R.layout.simple_spinner_dropdown_item,sceneNameList);
    }

    private void init() {
        for(int i=0;i<sceneList.size();i++){
            sceneNameList.add(sceneList.get(i).getName());
        }
    }

    @Override
    protected void convert(BaseViewHolder helper, Object item) {
        spinner=helper.getView(R.id.sp_scene);
        spinner.setAdapter(arrayAdapter);
        spinner.setOnItemSelectedListener(this);
        spinner.setTag(helper.getPosition());
        spinner.setSelection(0);

        String name= (String) item;
        helper.setText(R.id.tv_scene_button_name,name);

        DbScene dbScene=sceneList.get(0);
        map.put(btList.get(helper.getPosition()),dbScene);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        spinner= (Spinner) getViewByPosition(position,R.id.sp_scene);
        int positionScene= (int) spinner.getTag();
        DbScene dbScene=sceneList.get(position);
        Log.d(TAG, "onItemSelected: "+position+"-------"+positionScene);
        map.put(btList.get(positionScene),dbScene);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public Map<String,DbScene> getSceneMap(){
        return map;
    }
}

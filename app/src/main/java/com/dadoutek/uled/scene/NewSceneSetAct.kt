package com.dadoutek.uled.scene

import android.os.Bundle
import android.view.View
import com.dadoutek.uled.R
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.tellink.TelinkBaseActivity
import kotlinx.android.synthetic.main.activity_new_scene_set.*

class NewSceneSetAct :TelinkBaseActivity(){
    private var isChangeScene = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_scene_set)
        initData()
        initView()
    }

    private fun initView() {
        if(isChangeScene){
            showDataListView()
        }else{
            showEditListVew()
        }
    }

    private fun showDataListView() {
        data_view_layout.visibility= View.VISIBLE
        edit_data_view_layout.visibility=View.GONE
    }

    private fun showEditListVew() {
        data_view_layout.visibility= View.GONE
        edit_data_view_layout.visibility=View.VISIBLE
    }

    private fun initData() {
        val intent = intent
        isChangeScene = intent.extras!!.get(Constant.IS_CHANGE_SCENE) as Boolean
    }
}
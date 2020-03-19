package com.dadoutek.uled.gateway

import android.app.Activity
import android.content.Intent
import android.os.Parcelable
import android.support.v7.widget.LinearLayoutManager
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.BaseActivity
import com.dadoutek.uled.gateway.adapter.GwTpItemAdapter
import com.dadoutek.uled.gateway.bean.GwTasksBean
import com.dadoutek.uled.gateway.bean.GwTimePeriodsBean
import com.dadoutek.uled.model.DbModel.DbScene
import com.dadoutek.uled.switches.SelectSceneListActivity
import kotlinx.android.synthetic.main.template_recycleview.*
import kotlinx.android.synthetic.main.template_top_three.*


/**
 * 创建者     ZCL
 * 创建时间   2020/3/17 18:43
 * 描述 生成时间段列表 传回上一页经上一页 再返回上一页保存  传入task的bean
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GwTimerPeriodListActivity : BaseActivity() {
    private var tasksBean: GwTasksBean? = null
    private var isHaveLastOne = false
    override fun setLayoutID(): Int {
        return R.layout.activity_timer_period_list
    }

    private var scene: DbScene? = null
    private var timesList: ArrayList<GwTimePeriodsBean> = ArrayList()
    private var adapter = GwTpItemAdapter(R.layout.item_gw_time_scene, timesList)
    private val requestCodes: Int = 1000
    private var selectPosition: Int = 0

    override fun initListener() {
        adapter.setOnItemClickListener { _, _, position ->
            selectPosition = position
            startActivityForResult(Intent(this@GwTimerPeriodListActivity, SelectSceneListActivity::class.java), requestCodes)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {//获取场景返回值
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == requestCodes) {
            val par = data!!.getParcelableExtra<Parcelable>("data")
            scene = par as DbScene
            LogUtils.v("zcl获取场景信息scene" + scene.toString())
            if (scene != null) {
                isHaveLastOne = true
                timesList[selectPosition].sceneName = scene!!.name
                timesList[selectPosition].sceneId = scene!!.id
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun initData() {
        timesList.clear()
         tasksBean = intent.getParcelableExtra<GwTasksBean>("data")
        val tpList = tasksBean?.timingPeriods
        if (tpList==null||tpList.size<=0){
            ToastUtils.showShort(getString(R.string.invalid_data))
        }else{
            timesList.addAll(tpList)
            for (tp in tpList)
                if (tp.sceneId!=0L){
                    isHaveLastOne = true
                    break
                }
        }

        adapter.notifyDataSetChanged()
    }

    override fun initView() {
        toolbar_t_center.text = getString(R.string.timer_period_set)
        toolbar_t_cancel.setOnClickListener { finish()  }
        toolbar_t_confim.setOnClickListener {
            if (!isHaveLastOne){
                ToastUtils.showShort(getString(R.string.please_setting_least_one))
                return@setOnClickListener
            }
            intent.putParcelableArrayListExtra("data",timesList)
            setResult(Activity.RESULT_OK,intent)
            finish()
        }

        template_recycleView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        template_recycleView.adapter = adapter
    }
}
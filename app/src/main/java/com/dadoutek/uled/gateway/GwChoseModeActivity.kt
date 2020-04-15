package com.dadoutek.uled.gateway

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.gateway.adapter.WeeksItemAdapter
import com.dadoutek.uled.gateway.bean.WeekBean
import com.dadoutek.uled.model.Constant
import kotlinx.android.synthetic.main.template_recycleview.*
import kotlinx.android.synthetic.main.toolbar.*


/**
 * 创建者     ZCL
 * 创建时间   2020/3/4 10:13
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GwChoseModeActivity : TelinkBaseActivity() {

    private var list: MutableList<WeekBean>? = null
    private var adapter: WeeksItemAdapter? = null
    private var week: Int = 0
    private val checkedList = ArrayList<WeekBean>()
    fun initListener() {

    }

    fun initData() {
        week = intent.getIntExtra("data", 0);
        LogUtils.e(week)
        var tmpWeek = week;
        if (week  == 0b10000000)
            tmpWeek = Constant.SATURDAY or Constant.FRIDAY or Constant.THURSDAY or Constant.WEDNESDAY or Constant.TUESDAY or Constant.MONDAY or Constant.SUNDAY //每一天

//仅一次的，暂不做转换
//        else if (week == 0) //仅一次
//        {
//            var dayOfWeek = Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) - 1
//            tmpWeek = 1 shl dayOfWeek
//        }

        list = mutableListOf(
                WeekBean(getString(R.string.monday), 1, (tmpWeek and Constant.MONDAY) != 0),
                WeekBean(getString(R.string.tuesday), 2, (tmpWeek and Constant.TUESDAY) != 0),
                WeekBean(getString(R.string.wednesday), 3, (tmpWeek and Constant.WEDNESDAY) != 0),
                WeekBean(getString(R.string.thursday), 4, (tmpWeek and Constant.THURSDAY) != 0),
                WeekBean(getString(R.string.friday), 5, (tmpWeek and Constant.FRIDAY) != 0),
                WeekBean(getString(R.string.saturday), 6, (tmpWeek and Constant.SATURDAY) != 0),
                WeekBean(getString(R.string.sunday), 7, (tmpWeek and Constant.SUNDAY) != 0))

        for (i in 0 until list!!.size) {
            var weekBean = list!![i]
            if (weekBean.checked) {
                checkedList.add(weekBean)
            }
        }

        template_recycleView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        adapter = WeeksItemAdapter(R.layout.item_week_day_tick, list!!)
        template_recycleView.adapter = adapter
        adapter?.bindToRecyclerView(template_recycleView)
        adapter?.setOnItemClickListener { _, _, position ->
            if (checkedList.contains(list?.get(position)!!)) {

                list?.get(position)!!.checked = false
                checkedList.remove(list?.get(position)!!)
            } else {
                list?.get(position)!!.checked = true
                checkedList.add(list?.get(position)!!)
            }
            adapter?.notifyDataSetChanged()
        }
    }

    fun initView() {
        toolbar.setNavigationIcon(R.drawable.icon_top_tab_back)
        toolbarTv.text = getString(R.string.repetition)
        toolbar.setNavigationOnClickListener {
            var intent = Intent()
            var sb = StringBuilder()
            when {
                checkedList.size == 0 -> sb.append(getString(R.string.only_one))

                checkedList.size == 7 -> sb.append(getString(R.string.every_day))
                else -> {
                    checkedList.sortBy { it.pos }
                    for (i in 0 until checkedList.size) {//until 不包含 尾部 ..包含
                        if (i == checkedList.size - 1)
                            sb.append(checkedList[i].week)
                        else
                            sb.append(checkedList[i].week).append(",")
                        if (checkedList.size == 6)
                            sb.append("6")
                    }
                }
            }
            intent.putExtra("data", sb.toString())
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.template_activity_list)
        initView()
        initData()
        initListener()
    }
}
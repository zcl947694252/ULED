package com.dadoutek.uled.gateway

import android.app.Activity
import android.content.Intent
import android.support.v7.widget.LinearLayoutManager
import com.dadoutek.uled.R
import com.dadoutek.uled.base.BaseActivity
import com.dadoutek.uled.gateway.adapter.WeeksItemAdapter
import com.dadoutek.uled.gateway.bean.WeekBean
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
class GatewayModeChoseActivity : BaseActivity() {

    private var list: MutableList<WeekBean>? = null
    private var adapter: WeeksItemAdapter? = null

    private val checkedList = ArrayList<WeekBean>()
    override fun initListener() {
    }

    override fun initData() {
        list = mutableListOf(WeekBean(getString(R.string.monday), 1), WeekBean(getString(R.string.tuesday), 2),
                WeekBean(getString(R.string.wednesday), 3), WeekBean(getString(R.string.thursday), 4), WeekBean(getString(R.string.friday), 5)
                , WeekBean(getString(R.string.saturday), 6), WeekBean(getString(R.string.sunday), 7))
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

    override fun initView() {
        toolbar.setNavigationIcon(R.drawable.icon_top_tab_back)
        toolbarTv.text = getString(R.string.repetition)
        toolbar.setNavigationOnClickListener {
            var intent = Intent()
            var sb = StringBuilder()
            when {
                checkedList.size ==0 -> sb.append(getString(R.string.only_one))
                checkedList.size ==1 -> sb.append(getString(R.string.only_one))
                checkedList.size == 7 -> sb.append(getString(R.string.every_day))
                else -> {
                    checkedList.sortBy { it.pos }
                    for (i in 0 until checkedList.size) {//until 不包含 尾部 ..包含
                        if (i == checkedList.size - 1)
                            sb.append(checkedList[i].week)
                        else
                            sb.append(checkedList[i].week).append(",")
                          if (checkedList.size==6)
                              sb.append("6")
                    }
                }
            }
            intent.putExtra("data", sb.toString())
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    override fun setLayoutID(): Int {
        return R.layout.template_activity_list
    }

}
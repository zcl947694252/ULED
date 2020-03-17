package com.dadoutek.uled.gateway

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.gateway.adapter.GatewayTimeItemAdapter
import com.dadoutek.uled.gateway.bean.DbGateway
import com.dadoutek.uled.gateway.bean.GatewayTagBean
import com.dadoutek.uled.gateway.bean.GatewayTasksBean
import com.dadoutek.uled.gateway.util.GsonUtil
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.yanzhenjie.recyclerview.touch.OnItemMoveListener
import kotlinx.android.synthetic.main.activity_gate_way.*
import kotlinx.android.synthetic.main.template_bottom_add_no_line.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.toast
import java.util.*
import kotlin.collections.ArrayList


/**
 * 创建者     ZCL
 * 创建时间   2020/3/3 17:22
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GatewayConfigActivity : TelinkBaseActivity(), View.OnClickListener {
    private var dbGateway: DbGateway? = null
    private var tagBeans: ArrayList<GatewayTagBean> = arrayListOf()
    private var dbId: Long = 99999
    private var tagBean: GatewayTagBean? = null
    private var modeIsTimer: Boolean = true
    private val requestModeCode: Int = 1000//选择日期
    private val requestTimeCode: Int = 2000//时间段模式
    private var isCanEdite = false
    private var maxId = 0L

    val listTask = ArrayList<GatewayTasksBean>()
    private val adapter = GatewayTimeItemAdapter(R.layout.item_gata_way_event_timer, listTask)

    /**
     * 默认重复设置为仅一次
     */
    fun initView() {
        toolbarTv.text = getString(R.string.Gate_way)
        gate_way_repete_mode.textSize = 15F
        toolbar.setNavigationIcon(R.drawable.icon_top_tab_back)
        toolbar.setNavigationOnClickListener { finish() }
        tv_function1.text = getString(R.string.complete)
        tv_function1.visibility = View.VISIBLE
        gate_way_repete_mode.text = getString(R.string.only_one)

        isCanEdite = false
        isCanEdite()
        swipe_recycleView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        swipe_recycleView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.HORIZONTAL))
        swipe_recycleView.adapter = adapter
        swipe_recycleView.isItemViewSwipeEnabled = true //侧滑删除，默认关闭。
    }

    private fun getMaxTagId() {
        for (tag in tagBeans!!) {
            if (tag.tagId > maxId)
                maxId = tag.tagId
        }
    }

    fun initData() {
        //創建新tag任务
        dbGateway = intent.getParcelableExtra<DbGateway>("data")
        if (dbGateway != null) {//拿到有效数据
            modeIsTimer = dbGateway?.type == 0//判断是不是timer

            if (!TextUtils.isEmpty(dbGateway?.tags)) {//不要移动 此处有新tag和老tag需要的东西
                tagBeans.clear()
                tagBeans.addAll(GsonUtil.stringToList(dbGateway?.tags, GatewayTagBean::class.java))//获取该设备tag列表
                if (tagBeans.size > 0)
                    getMaxTagId()//更新maxid
            }

            tagBean = if ( SharedPreferencesHelper.getBoolean(this, Constant.IS_NEW_TAG,false)) {//是否是添加新tag
                //创建新的tag
                GatewayTagBean((maxId + 1), getString(R.string.tag1), getString(R.string.only_one), getWeek(getString(R.string.only_one)))
            } else {//编辑已有tag
                if (tagBeans.size>0)
                tagBeans[dbGateway?.pos ?: 0]
                else
                    GatewayTagBean((maxId + 1), getString(R.string.tag1), getString(R.string.only_one), getWeek(getString(R.string.only_one)))
            }
        } else {
            ToastUtils.showShort(getString(R.string.invalid_data))
            finish()
        }

        listTask.clear()
        val tasks = tagBean!!.tasks
        if (tasks != null)
            listTask.addAll(tasks)
        adapter.notifyDataSetChanged()

        gate_way_lable.setText(tagBean?.tagName)
        gate_way_repete_mode.text = tagBean?.weekStr

        if (modeIsTimer)
            add_group_btn?.findViewById<TextView>(R.id.add_group_btn_tv)?.text = getString(R.string.add_time)
        else
            add_group_btn?.findViewById<TextView>(R.id.add_group_btn_tv)?.text = getString(R.string.add_times)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.gate_way_repete_mode, R.id.gate_way_repete_mode_arrow -> {//选择模式是否重复
                val intent = Intent(this@GatewayConfigActivity, GatewayModeChoseActivity::class.java)
                startActivityForResult(intent, requestModeCode)
            }
            R.id.gate_way_edite -> {//是否编辑
                isCanEdite = !isCanEdite
                isCanEdite()
            }
            R.id.tv_function1 -> {
                dbGateway?.let { it ->
                    it.productUUID = DeviceType.GATE_WAY
                    val toJson = GsonUtils.toJson(listTask)//获取tasks字符串
                    LogUtils.v("zcl网关---$toJson")
                    tagBean?.tagName = gate_way_lable.text.toString()
                    tagBean?.tasks = listTask

                    //因为是新创建的对象所以直接创建list添加 如果不是需要查看是都有tags
                    if (tagBeans.size == 0) {
                        it.tags = GsonUtils.toJson(mutableListOf(tagBean))
                    } else {
                        var removeTag: GatewayTagBean? = null
                        for (tag in tagBeans) {
                            if (tag.tagId == tagBean?.tagId)
                                removeTag = tag
                        }
                        tagBeans.remove(removeTag)
                        tagBeans.add(tagBean!!)
                        it.tags = GsonUtils.toJson(tagBeans)
                    }
                    DBUtils.saveGateWay(it, true)
                }
                finish()
            }
        }
    }

    private fun getWeek(str: String): Int {
        var week = 0x00000000
        when (str) {
            getString(R.string.only_one) -> {
                week = 0x10000000
                return week
            }
            getString(R.string.every_day) -> {
                week = 0x01111111
                return week
            }
            else -> {
                val split = str.split(",").toMutableList()
                for (s in split) {
                    when (s) {//bit位 0-6 周日-周六 7代表当天
                        getString(R.string.sunday) -> week = week or 0x00000001
                        getString(R.string.monday) -> week = week or 0x00000010
                        getString(R.string.tuesday) -> week = week or 0x00000100
                        getString(R.string.wednesday) -> week = week or 0x00001000
                        getString(R.string.thursday) -> week = week or 0x00010000
                        getString(R.string.friday) -> week = week or 0x00100000
                        getString(R.string.saturday) -> week = week or 0x01000000
                    }
                }
                return week
            }
        }
    }

    fun initListener() {
        tv_function1.setOnClickListener(this)
        gate_way_edite.setOnClickListener(this)
        gate_way_repete_mode.setOnClickListener(this)
        gate_way_repete_mode_arrow.setOnClickListener(this)
        swipe_recycleView.setOnItemMoveListener(object : OnItemMoveListener {
            override fun onItemDismiss(srcHolder: RecyclerView.ViewHolder?) {
                // 从数据源移除该Item对应的数据，并刷新Adapter。
                val position = srcHolder?.adapterPosition
                listTask.removeAt(position!!)
                adapter.notifyItemRemoved(position)
                saveChangeTasks()
            }

            private fun saveChangeTasks() {
                DBUtils.getGatewayByID(dbId)?.let { it ->
                    tagBean?.tasks = listTask
                    //因为是新创建的对象所以直接创建list添加 如果不是需要查看是都有tags
                    val tJ = GsonUtils.toJson(mutableListOf(tagBean))
                    it.tags = tJ
                    DBUtils.saveGateWay(it, true)
                }
            }

            override fun onItemMove(srcHolder: RecyclerView.ViewHolder?, targetHolder: RecyclerView.ViewHolder?): Boolean {
                return false//表示数据移动不成功
            }
        })
        adapter.setOnItemClickListener { _, _, position ->
            val intent = Intent(this@GatewayConfigActivity, GatewayChoseTimeActivity::class.java)
            listTask[0].selectPos = position//默认使用第一个记录选中的pos
            intent.putExtra("data", listTask)
            startActivityForResult(intent, requestTimeCode)
        }
        add_group_btn?.setOnClickListener {
            //创建新的task传入可用的index
            val index = getIndex()
            when {
                listTask.size > 20 -> toast(getString(R.string.gate_way_time_max))
                index == 0 -> toast(getString(R.string.gate_way_time_max))
                modeIsTimer -> {//跳转时间选择界面
                    val intent = Intent(this@GatewayConfigActivity, GatewayChoseTimeActivity::class.java)
                    intent.putExtra("index", index)//传入已有时间防止重复
                    startActivityForResult(intent, requestTimeCode)
                }
                !modeIsTimer -> {//跳转时间段选择界面
                    val intent = Intent(this@GatewayConfigActivity, GatewayChoseTimesActivity::class.java)
                    intent.putExtra("index", index)//传入已有时间防止重复
                    startActivityForResult(intent, requestTimeCode)
                }
            }
        }
    }

    /**
     * 获取task下标
     */
    private fun getIndex(): Int {
        var index = 0
        if (listTask.size == 0) {
            index = 1
        } else {
            for (j in 1..20) {
                var isUsed = false
                for (i in listTask) {
                    if (i.index == j) {//已用 跳出循环
                        isUsed = true
                        break
                    }
                }
                if (!isUsed) {
                    index = j
                    break
                }
            }
        }
        return index
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == requestModeCode) {//选择重复日期
                var mode = data?.getStringExtra("data")
                if (mode!!.contains("6")) {
                    gate_way_repete_mode.textSize = 13F
                    gate_way_repete_mode.text = mode.replace("6", "")
                    tagBean?.week = getWeek(mode.replace("6", ""))
                } else {
                    gate_way_repete_mode.textSize = 15F
                    mode = isOnlyOne(mode)
                    gate_way_repete_mode.text = mode
                    tagBean?.weekStr = mode
                    tagBean?.week = getWeek(mode)
                }

            } else if (requestCode == requestTimeCode) {//获取task
                val bean = data?.getParcelableExtra<GatewayTasksBean>("data")
                bean?.let {
                    if (bean.isCreateNew) {
                        listTask.add(bean)
                    } else {
                        var targetPosition: Int
                        for (i in 0 until listTask.size) {
                            val timeBean = listTask[i]
                            if (timeBean.index == bean.index) {//存在
                                targetPosition = i
                                listTask.removeAt(targetPosition)
                                listTask.add(bean)
                                break
                            }
                        }
                        listTask.sortBy { it.startHour }
                    }
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun isOnlyOne(mode: String): String {
        val split = mode.split(",").toMutableList()
        var weekDay = when {
            Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1 == 0 -> getString(R.string.sunday)
            Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1 == 1 -> getString(R.string.monday)
            Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1 == 2 -> getString(R.string.tuesday)
            Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1 == 3 -> getString(R.string.wednesday)
            Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1 == 4 -> getString(R.string.thursday)
            Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1 == 5 -> getString(R.string.friday)
            Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1 == 6 -> getString(R.string.saturday)
            else -> getString(R.string.sunday)
        }//bit位 0-6 周日-周六 7代表当天
        return if (split.size == 1 && weekDay == split[0])
            getString(R.string.only_one)
        else
            mode
    }

    private fun isCanEdite() {
        if (isCanEdite) {
            gate_way_lable.isFocusableInTouchMode = true//不可编辑
            gate_way_lable.isClickable = true//不可点击，但是这个效果我这边没体现出来，不知道怎没用
            gate_way_lable.isFocusable = true//不可编辑
            gate_way_lable.isEnabled = true
            gate_way_lable.requestFocus()

            gate_way_lable.setSelection(gate_way_lable.text.length)//将光标移至文字末尾
            val im =gate_way_lable.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.showSoftInput(gate_way_lable,0)
        } else {
            gate_way_lable.isFocusableInTouchMode = false//不可编辑
            gate_way_lable.isClickable = false//不可点击
            gate_way_lable.isFocusable = false//不可编辑
            gate_way_lable.isEnabled = false
            gate_way_lable.background = null
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (currentFocus!=null)
            im.hideSoftInputFromWindow(currentFocus.windowToken,InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gate_way)
        initView()
        initData()
        initListener()
    }
}
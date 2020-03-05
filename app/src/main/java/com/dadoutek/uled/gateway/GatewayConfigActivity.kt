package com.dadoutek.uled.gateway

import android.content.Intent
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.BaseActivity
import kotlinx.android.synthetic.main.activity_gate_way.*
import kotlinx.android.synthetic.main.template_recycleview.*


/**
 * 创建者     ZCL
 * 创建时间   2020/3/3 17:22
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GatewayConfigActivity : BaseActivity(), View.OnClickListener {
    private var lin: View? = null
    private val requestModeCode: Int = 1000
    private val requestTimeCode: Int = 2000
    private var isCanEdite = false
    private val adapter = GatewayTimeItemAdapter(R.layout.event_timer_item, mutableListOf())
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.gate_way_repete_mode, R.id.gate_way_repete_mode_arrow -> {
                startActivityForResult(Intent(this@GatewayConfigActivity, GatewayModeChoseActivity::class.java), requestModeCode)
            }
            R.id.gate_way_edite -> {
                isCanEdite = !isCanEdite
                isCanEdite()
            }
        }
    }

    private fun isCanEdite() {
        if (isCanEdite) {
            gate_way_title.isFocusableInTouchMode = true//不可编辑
            gate_way_title.isClickable = true//不可点击，但是这个效果我这边没体现出来，不知道怎没用
            gate_way_title.isFocusable = true//不可编辑
            gate_way_title.isEnabled = true
            gate_way_title.requestFocus()
            gate_way_title.setSelection(gate_way_title.text.length)//将光标移至文字末尾
        } else {
            gate_way_title.isFocusableInTouchMode = false//不可编辑
            gate_way_title.isClickable = false//不可点击
            gate_way_title.isFocusable = false//不可编辑
            gate_way_title.isEnabled = false
            gate_way_title.background = null
        }
    }

    override fun setLayoutID(): Int {
        return R.layout.activity_gate_way
    }

    override fun initView() {
        isCanEdite = false
        isCanEdite()
        template_recycleView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        template_recycleView.adapter = adapter
        lin = View.inflate(this, R.layout.add_group, null)
        adapter.addFooterView(lin)
        adapter.onItemClickListener = BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
            startActivityForResult(Intent(this@GatewayConfigActivity, GatewayChoseTimeActivity::class.java), requestTimeCode)
        }
    }

    override fun initData() {

    }

    override fun initListener() {
        gate_way_edite.setOnClickListener(this)
        gate_way_repete_mode.setOnClickListener(this)
        gate_way_repete_mode_arrow.setOnClickListener(this)
        lin?.setOnClickListener {
            startActivityForResult(Intent(this@GatewayConfigActivity, GatewayChoseTimeActivity::class.java), requestTimeCode)
        }
    }
}
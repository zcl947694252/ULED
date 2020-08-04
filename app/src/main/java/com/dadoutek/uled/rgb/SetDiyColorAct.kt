package com.dadoutek.uled.rgb

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbColorNode
import com.dadoutek.uled.model.DbModel.DbDiyGradient
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.util.SharedPreferencesUtils
import com.dadoutek.uled.util.StringUtils
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import kotlinx.android.synthetic.main.activity_set_diy_color.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SetDiyColorAct : TelinkBaseActivity(), View.OnClickListener {
    var colorNodeList: ArrayList<DbColorNode>? = null
    private var rgbDiyColorListAdapter: RGBDiyColorCheckAdapter? = null
    private var isChange = false
    private var diyGradient: DbDiyGradient? = null
    private var speed = 50
    private var dstAddress: Int = 0
    private val NODE_MODE_RGB_GRADIENT = 0

    private var downTime: Long = 0//Button被按下时的时间
    private var thisTime: Long = 0//while每次循环时的时间
    internal var onBtnTouch = false//Button是否被按下
    private var tvValue = 0//TextView中的值

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_diy_color)
        initData()
        initView()
    }

    private fun initData() {
        isChange = intent.getBooleanExtra(Constant.IS_CHANGE_COLOR, false)
        dstAddress = intent.getIntExtra(Constant.TYPE_VIEW_ADDRESS, 0)
        if (isChange) {
            diyGradient = intent.getParcelableExtra(Constant.GRADIENT_KEY) as? DbDiyGradient
            colorNodeList = DBUtils.getColorNodeListByDynamicModeId(diyGradient!!.id)
        } else {
            creatNewData()
        }
    }

    @SuppressLint("StringFormatMatches", "SetTextI18n")
    private fun initView() {
        saveNode.setOnClickListener(this)
        val layoutmanager = GridLayoutManager(this, 4)
        StringUtils.initEditTextFilter(editName)
        selectColorRecyclerView.layoutManager = layoutmanager
        rgbDiyColorListAdapter = RGBDiyColorCheckAdapter(R.layout.item_color1, colorNodeList)
        rgbDiyColorListAdapter?.bindToRecyclerView(selectColorRecyclerView)
        rgbDiyColorListAdapter?.onItemClickListener = onItemClickListener
        rgbDiyColorListAdapter?.onItemLongClickListener = onItemLongClickListener
        sbSpeed.onSeekChangeListener = object : OnSeekChangeListener {
            override fun onSeeking(seekParams: SeekParams?) {
                speed = seekParams?.progress ?: 0
                if (speed == 0)
                    speed = 1

                speed_num.text = "$speed%"
                when {
                    speed >= 100 -> {
                        speed_add.isEnabled = false
                        speed_less.isEnabled = true
                    }
                    speed <= 0 -> {
                        speed_less.isEnabled = false
                        speed_add.isEnabled = true
                    }
                    else -> {
                        speed_less.isEnabled = true
                        speed_add.isEnabled = true
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: IndicatorSeekBar?) {}
            override fun onStopTrackingTouch(seekBar: IndicatorSeekBar?) {}
        }
        selectColorRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    val v = window.peekDecorView()
                    if (null != v) {
                        imm.hideSoftInputFromWindow(v.windowToken, 0)
                    }
                }
            }
        })

        editName.setSelection(editName.text.toString().length)
        set_diy_color.setOnTouchListener { _, _ ->
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val v = window.peekDecorView()
            if (null != v) {
                imm.hideSoftInputFromWindow(v.windowToken, 0)
            }
            true
        }

        speed_add.setOnTouchListener { _, event ->
            addSpeed(event)
            true
        }

        speed_less.setOnTouchListener { _, event ->
            lessSpeed(event)
            true
        }

        if (isChange) {
            toolbarTv.text = getString(R.string.update_gradient)
            editName.setText(diyGradient?.name)
            editName.setSelection(editName.text.toString().length)
            sbSpeed.setProgress(diyGradient?.speed?.toFloat()?:0f)
            speed_num.text = diyGradient?.speed.toString() + "%"
            when {
                sbSpeed.progress >= 100 -> {
                    speed_add.isEnabled = false
                    speed_less.isEnabled = true
                }
                sbSpeed.progress <= 0 -> {
                    speed_less.isEnabled = false
                    speed_add.isEnabled = true
                }
                else -> {
                    speed_less.isEnabled = true
                    speed_add.isEnabled = true
                }
            }
        } else {
            when {
                sbSpeed.progress >= 100 -> {
                    speed_add.isEnabled = false
                    speed_less.isEnabled = true
                }
                sbSpeed.progress <= 0 -> {
                    speed_less.isEnabled = false
                    speed_add.isEnabled = true
                }
                else -> {
                    speed_less.isEnabled = true
                    speed_add.isEnabled = true
                }
            }
            toolbarTv.text = getString(R.string.add_gradient)
            editName.setText(DBUtils.getDefaultModeName())
            editName.setSelection(editName.text.toString().length)
            sbSpeed.setProgress(50f)

            speed_num.text = 50.toString() + "%"
        }

        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener { finish() }
        see_help_ly.setOnClickListener { seeHelpe("#control-color-light-custom-gradient") }
    }

    private fun lessSpeed(event: MotionEvent?) {
        if (event!!.action == MotionEvent.ACTION_DOWN) {
            downTime = System.currentTimeMillis()
            onBtnTouch = true
            GlobalScope.launch {
                while (onBtnTouch) {
                    thisTime = System.currentTimeMillis()
                    if (thisTime - downTime >= 500) {
                        tvValue++
                        val msg = less_speed_handler.obtainMessage()
                        msg.arg1 = tvValue
                        less_speed_handler.sendMessage(msg)
                        Log.e("TAG_TOUCH", tvValue++.toString())
                        try {
                            delay(100)
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        } else if (event.action == MotionEvent.ACTION_UP) {
            onBtnTouch = false
            if (thisTime - downTime < 500) {
                tvValue++
                val msg = less_speed_handler.obtainMessage()
                msg.arg1 = tvValue
                less_speed_handler.sendMessage(msg)
            }
        } else if (event.action == MotionEvent.ACTION_CANCEL) {
            onBtnTouch = false
        }
    }

    private fun addSpeed(event: MotionEvent?) {
        if (event!!.action == MotionEvent.ACTION_DOWN) {
            downTime = System.currentTimeMillis()
            onBtnTouch = true
            GlobalScope.launch {
                while (onBtnTouch) {
                    thisTime = System.currentTimeMillis()
                    if (thisTime - downTime >= 500) {
                        tvValue++
                        val msg = add_speed_handler.obtainMessage()
                        msg.arg1 = tvValue
                        add_speed_handler.sendMessage(msg)
                        Log.e("TAG_TOUCH", tvValue++.toString())
                        try {
                            delay(100)
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }

                    }
                }
            }

        } else if (event.action == MotionEvent.ACTION_UP) {
            onBtnTouch = false
            if (thisTime - downTime < 500) {
                tvValue++
                val msg = add_speed_handler.obtainMessage()
                msg.arg1 = tvValue
                add_speed_handler.sendMessage(msg)
            }
        } else if (event.action == MotionEvent.ACTION_CANCEL) {
            onBtnTouch = false
        }
    }

    @SuppressLint("HandlerLeak")
    private val add_speed_handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            sbSpeed.setProgress(sbSpeed.progress+1f)
            when {
                sbSpeed.progress > 100 -> {
                    speed_add.isEnabled = false
                    onBtnTouch = false
                }
                sbSpeed.progress == 100 -> {
                    speed_add.isEnabled = false
                    speed_num.text = sbSpeed.progress.toString() + "%"
                    onBtnTouch = false
                    speed = sbSpeed.progress
                }
                else -> {
                    speed_add.isEnabled = true
                    speed = sbSpeed.progress
                }
            }

            if (sbSpeed.progress > 0) {
                speed_less.isEnabled = true
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val less_speed_handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            sbSpeed.setProgress(sbSpeed.progress-1f)
            when {
                sbSpeed.progress < 0 -> {
                    speed_less.isEnabled = false
                    onBtnTouch = false
                }
                sbSpeed.progress == 0 -> {
                    sbSpeed.setProgress(1f)
                    speed_num.text = sbSpeed.progress.toString() + "%"
                    speed = sbSpeed.progress
                }
                else -> {
                    speed_less.isEnabled = true
                    speed = sbSpeed.progress
                }
            }

            if (sbSpeed.progress < 100) {
                speed_add.isEnabled = true
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(v: View?) {
        if (checkIsCorrect()) {
            if (isChange) {
                updateNode()
            } else {
                saveNode()
            }
        }
    }

    private fun updateNode() {
        showLoadingDialog(getString(R.string.save_gradient_dialog_tip))

        GlobalScope.launch {
            diyGradient?.name = editName.text.toString().trim()
            diyGradient?.type = NODE_MODE_RGB_GRADIENT
            diyGradient?.speed = speed

            DBUtils.updateGradient(diyGradient!!)
            val belongDynamicModeId = diyGradient?.id!!
            DBUtils.deleteColorNodeList(DBUtils.getColorNodeListByDynamicModeId(belongDynamicModeId))

            for (item in colorNodeList!!) {
                item.belongDynamicChangeId = belongDynamicModeId
                DBUtils.saveColorNode(item)
            }

            deleteGradient(belongDynamicModeId)
            delay(200)
            startSendCmdToAddDiyGradient(diyGradient!!)
            hideLoadingDialog()

            setResult(Activity.RESULT_OK)

            finish()
        }
    }

    private fun deleteGradient(id: Long) {
        Commander.deleteGradient(dstAddress, id.toInt(), {}, {})
    }

    private fun saveNode() {
        showLoadingDialog(getString(R.string.save_gradient_dialog_tip))
        Thread {
            diyGradient = DbDiyGradient()

            diyGradient?.id = getGradientId()
            diyGradient?.name = editName.text.toString().trim()
            diyGradient?.type = NODE_MODE_RGB_GRADIENT
            diyGradient?.belongRegionId = SharedPreferencesUtils.getCurrentUseRegionId()
            diyGradient?.speed = speed

            DBUtils.saveGradient(diyGradient!!, false)

            val belongDynamicModeId = diyGradient!!.id
            for (item in colorNodeList!!) {
                item.belongDynamicChangeId = belongDynamicModeId
                DBUtils.saveColorNode(item)
            }

            Thread.sleep(100)
            startSendCmdToAddDiyGradient(diyGradient!!)
            hideLoadingDialog()

            setResult(Activity.RESULT_OK)
            finish()
        }.start()
    }

    private fun startSendCmdToAddDiyGradient(diyGradient: DbDiyGradient) {
        var addNodeList: ArrayList<DbColorNode> = ArrayList()
        for (item in colorNodeList!!) {
            addNodeList.add(item)
        }

        val address = dstAddress
        val id = diyGradient.id.toInt()
        var nodeId = 0
        var nodeMode = diyGradient.type
        var brightness = 0
        var r = 0
        var g = 0
        var b = 0
        var c = 0
        var w = 0

        var temperature = 0
        if (nodeMode < 3) {
            //rgb模式
            for (j in addNodeList.indices) {
                nodeId = j
                if (addNodeList[j].rgbw == -1) {
                } else {
                    var item = addNodeList[j]
                    brightness = item.brightness
                    r = (item.rgbw and 0xff0000) shr 16
                    g = (item.rgbw and 0x00ff00) shr 8
                    b = (item.rgbw and 0x0000ff)
                    w = Color.alpha(item.rgbw)

                    Thread.sleep(1000)
                    if (brightness > 99) {
                        brightness = 99
                    }
                    if (w > 99) {
                        w = 99
                    }
                    Commander.addGradient(address, id, nodeId, nodeMode, brightness, r, g, b, c, w)
                }
            }
        } else {
            //双色温模式
            for (j in addNodeList.indices) {
                nodeId = j + 1
                var item = addNodeList[j]
                brightness = item.brightness
                temperature = item.colorTemperature

                Thread.sleep(200)
                Commander.addGradient(address, id, nodeId, nodeMode, brightness, temperature, 0, 0, 0, 0)
            }
        }
//        val red = (color!! and 0xff0000) shr 16
//        val green = (color and 0x00ff00) shr 8
//        val blue = color and 0x0000ff
//        val w = progress
    }

    private fun getGradientId(): Long {
        val list = DBUtils.diyGradientList
        val idList = java.util.ArrayList<Int>()
        for (i in list.indices) {
            idList.add(list[i].id!!.toInt())
        }

        var id = 1
        for (i in 1..6) {
            if (idList.contains(i)) {
                Log.d("sceneID", "getSceneId: " + "aaaaa")
                continue
            } else {
                id = i
                Log.d("sceneID", "getSceneId: bbbbb$id")
                break
            }
        }

        if (list.size == 0)
            id = 1

        return java.lang.Long.valueOf(id.toLong())
    }

    private fun checkIsCorrect(): Boolean {
        if (editName.text.toString().trim().isEmpty()) {
            ToastUtils.showLong(getString(R.string.name_not_null_tip))
            return false
        }

        return true
    }

    val onItemClickListener = BaseQuickAdapter.OnItemClickListener { _, view, position ->
        val intent = Intent(this, SelectColorGradientAct::class.java)
        colorNodeList!![position].dstAddress = dstAddress
        intent.putExtra(Constant.COLOR_NODE_KEY, colorNodeList!![position])
        startActivityForResult(intent, position)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            colorNodeList!![requestCode] = data!!.extras["color"] as DbColorNode
            rgbDiyColorListAdapter?.notifyDataSetChanged()
        }
    }

    val onItemLongClickListener = BaseQuickAdapter.OnItemLongClickListener { adapter, view, position ->
        colorNodeList!![position].rgbw = -1
        adapter?.notifyDataSetChanged()
        true
    }

    private fun creatNewData() {
        colorNodeList = ArrayList()
        for (i in 0..7) {
            var colorNode = DbColorNode()
            when (i) {
                0 -> {
                    colorNode.index = 0
                    colorNode.rgbw = 0x00ff4f4f
                }
                1 -> {
                    colorNode.index = 1
                    colorNode.rgbw = 0x00ff439b
                }
                2 -> {
                    colorNode.index = 2
                    colorNode.rgbw = 0x004ffe0
                }
                3 -> {
                    colorNode.index = 3
                    colorNode.rgbw = 0x00fff94f
                }
                4 -> {
                    colorNode.index = 4
                    colorNode.rgbw = -1
                }
                5 -> {
                    colorNode.index = 5
                    colorNode.rgbw = -1
                }
                6 -> {
                    colorNode.index = 6
                    colorNode.rgbw = -1
                }
                7 -> {
                    colorNode.index = 7
                    colorNode.rgbw = -1
                }
            }
            colorNodeList!!.add(colorNode)
        }
    }

}
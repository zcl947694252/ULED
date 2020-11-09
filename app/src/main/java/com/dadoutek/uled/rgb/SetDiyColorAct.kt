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
import com.dadoutek.uled.model.Constants
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbColorNode
import com.dadoutek.uled.model.dbModel.DbDiyGradient
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.model.Cmd
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.model.routerModel.UpdateGradientBean
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.util.SharedPreferencesUtils
import com.dadoutek.uled.util.StringUtils
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_set_diy_color.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SetDiyColorAct : TelinkBaseActivity(), View.OnClickListener {
    private var disposableTimer: Disposable? = null
    private var deviceType: Int = 0
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
        isChange = intent.getBooleanExtra(Constants.IS_CHANGE_COLOR, false)
        dstAddress = intent.getIntExtra(Constants.TYPE_VIEW_ADDRESS, 0)
        deviceType = intent.getIntExtra(Constants.DEVICE_TYPE, DeviceType.LIGHT_RGB)
        if (isChange) {
            diyGradient = intent.getParcelableExtra(Constants.GRADIENT_KEY) as? DbDiyGradient
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

                speed_num.text = "$speed"
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
            sbSpeed.setProgress(diyGradient?.speed?.toFloat() ?: 0f)
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
        when {
            event!!.action == MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                onBtnTouch = true
                GlobalScope.launch {
                    while (onBtnTouch) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            val msg = lessSpeedHandler.obtainMessage()
                            msg.arg1 = tvValue
                            lessSpeedHandler.sendMessage(msg)
                            Log.e("TAG_TOUCH", tvValue++.toString())
                            try {
                                delay(100)
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
            event.action == MotionEvent.ACTION_UP -> {
                onBtnTouch = false
                if (thisTime - downTime < 500) {
                    tvValue++
                    val msg = lessSpeedHandler.obtainMessage()
                    msg.arg1 = tvValue
                    lessSpeedHandler.sendMessage(msg)
                }
            }
            event.action == MotionEvent.ACTION_CANCEL -> {
                onBtnTouch = false
            }
        }
    }

    private fun addSpeed(event: MotionEvent?) {
        when {
            event!!.action == MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                onBtnTouch = true
                GlobalScope.launch {
                    while (onBtnTouch) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            tvValue++
                            val msg = addSpeedHandler.obtainMessage()
                            msg.arg1 = tvValue
                            addSpeedHandler.sendMessage(msg)
                            Log.e("TAG_TOUCH", tvValue++.toString())
                            try {
                                delay(100)
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }

                        }
                    }
                }

            }
            event.action == MotionEvent.ACTION_UP -> {
                onBtnTouch = false
                if (thisTime - downTime < 500) {
                    tvValue++
                    val msg = addSpeedHandler.obtainMessage()
                    msg.arg1 = tvValue
                    addSpeedHandler.sendMessage(msg)
                }
            }
            event.action == MotionEvent.ACTION_CANCEL -> {
                onBtnTouch = false
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val addSpeedHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            sbSpeed.setProgress(sbSpeed.progress + 1f)
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
    private val lessSpeedHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            sbSpeed.setProgress(sbSpeed.progress - 1f)
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
          /*  for (item in colorNodeList!!) {
                item.belongDynamicChangeId = belongDynamicModeId
                DBUtils.saveColorNode(item)
            }*/
            for (i in 0..7){
                colorNodeList!![i].belongDynamicChangeId = belongDynamicModeId
                DBUtils.saveColorNode(colorNodeList!![i])
            }

            deleteGradient(belongDynamicModeId)
            delay(200)
            if (Constants.IS_ROUTE_MODE) {
                diyGradient?.let {
                    RouterModel.routerUpdateGradient(UpdateGradientBean(it.id.toInt(), it.type, it.colorNodes, dstAddress, deviceType, "updateGradient"))?.subscribe({ response ->
                        //"errorCode": 90020, "该自定义渐变不存在，请重新刷新数据"     "errorCode": 90018,"该设备不存在，请重新刷新数据"
                        //"errorCode": 90008,"该设备没有绑定路由，无法添加自定义渐变"  "errorCode": 90007,"该组不存在，无法操作"
                        //"errorCode": 90005,"以下路由没有上线，无法更新自定义渐变"    "errorCode": 90004, "账号下区域下没有路由，无法操作"
                        when (response.errorCode) {
                            0 -> {
                                disposableTimer?.dispose()
                                disposableTimer = io.reactivex.Observable.timer(1500, TimeUnit.MILLISECONDS)
                                        .subscribe {
                                            ToastUtils.showShort(getString(R.string.update_gradient_fail))
                                            hideLoadingDialog()
                                        }
                            }
                            90020 -> ToastUtils.showShort(getString(R.string.gradient_not_exit))
                            90018 -> ToastUtils.showShort(getString(R.string.device_not_exit))
                            90008 -> ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                            90007 -> ToastUtils.showShort(getString(R.string.gp_not_exit))
                            90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                            90004 -> ToastUtils.showShort(getString(R.string.region_no_router))
                        }
                    }, { it1 ->
                        ToastUtils.showShort(it1.message)
                    })
                }
            } else {
                startSendCmdToAddDiyGradient(diyGradient!!)
                hidAndResultAndFinish()
            }
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
          /*  for (item in colorNodeList!!) {
                item.belongDynamicChangeId = belongDynamicModeId
                DBUtils.saveColorNode(item)
            }*/
            for (i in 0..7){
                colorNodeList!![i].belongDynamicChangeId = belongDynamicModeId
                DBUtils.saveColorNode(colorNodeList!![i])
            }

            Thread.sleep(100)
            if (Constants.IS_ROUTE_MODE) {
                diyGradient?.let {
                    //@Field("name") String name,Field("type") int type,@Field("speed") int speed,Field("colorNodes") List<DbColorNode> colorNodes,
                    // @Field("meshAddr") int meshAddr, @Field("meshType") int meshType,@Field("ser_id") String ser_id
                    RouterModel.routerAddGradient(AddGradientBean(it.name, it.type, it.speed, it.colorNodes, dstAddress, deviceType, "addGra"))?.subscribe({ response ->
                        //    "errorCode": 90018,该设备不存在，请重新刷新数据"  "errorCode": 90008,该设备没有绑定路由，无法添加自定义渐变"
                        //    "errorCode": 90004 账号下区域下没有路由，无法操作""errorCode": 90007,该组不存在，无法操作" "errorCode": 90005,以下路由没有上线，无法添加自定义渐变"
                        when (response.errorCode) {
                            0 -> {
                                disposableTimer?.dispose()
                                disposableTimer = io.reactivex.Observable.timer(1500, TimeUnit.MILLISECONDS)
                                        .subscribe {
                                            showLoadingDialog(getString(R.string.add_gradient_fail))
                                        }
                            }
                            900018 -> ToastUtils.showShort(getString(R.string.device_not_exit))
                            90008 -> ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                            90007 -> ToastUtils.showShort(getString(R.string.gp_not_exit))
                            90004 -> ToastUtils.showShort(getString(R.string.region_no_router))
                            90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                        }
                    }, { it1 ->
                        ToastUtils.showShort(it1.message)
                    })
                }
            } else {
                startSendCmdToAddDiyGradient(diyGradient!!)
                hidAndResultAndFinish()
            }
        }.start()
    }

    private fun hidAndResultAndFinish() {
        hideLoadingDialog()
        setResult(Activity.RESULT_OK)
        finish()
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
        var r: Int
        var g: Int
        var b: Int
        var c = 0
        var w: Int

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
        intent.putExtra(Constants.COLOR_NODE_KEY, colorNodeList!![position])
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

    override fun onDestroy() {
        super.onDestroy()
        disposableTimer?.dispose()
    }

    override fun tzRouterAddOrDelOrUpdateGradientRecevice(cmdBean: CmdBodyBean) {
        hideLoadingDialog()
        if (cmdBean.status == 0) {
            disposableTimer?.dispose()
            setResult(Activity.RESULT_OK)
            finish()
        } else when (cmdBean.cmd) {
            Cmd.tzRouteAddGradient -> ToastUtils.showShort(getString(R.string.add_gradient_fail))
            Cmd.tzRouteUpdateGradient -> ToastUtils.showShort(getString(R.string.update_gradient_fail))
        }

    }

}
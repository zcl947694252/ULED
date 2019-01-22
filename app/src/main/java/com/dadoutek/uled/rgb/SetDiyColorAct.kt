package com.dadoutek.uled.rgb

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbColorNode
import com.dadoutek.uled.model.DbModel.DbDiyGradient
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.util.SharedPreferencesUtils
import com.dadoutek.uled.util.StringUtils
import kotlinx.android.synthetic.main.activity_set_diy_color.*
import kotlinx.android.synthetic.main.toolbar.*

class SetDiyColorAct : TelinkBaseActivity(), View.OnClickListener {
    var colorNodeList: ArrayList<DbColorNode>? = null
    private var rgbDiyColorListAdapter: RGBDiyColorCheckAdapter? = null
    private var isChange = false
    private var diyGradient: DbDiyGradient? = null
    private var speed=50
    private var dstAddress: Int = 0
    private val NODE_MODE_RGB_GRADIENT=0

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

    @SuppressLint("StringFormatMatches")
    private fun initView() {
        saveNode.setOnClickListener(this)
        val layoutmanager = GridLayoutManager(this, 4)
        StringUtils.initEditTextFilter(editName)
        selectColorRecyclerView.layoutManager = layoutmanager as RecyclerView.LayoutManager?
        rgbDiyColorListAdapter = RGBDiyColorCheckAdapter(
                R.layout.item_color1, colorNodeList)
        rgbDiyColorListAdapter?.bindToRecyclerView(selectColorRecyclerView)
        rgbDiyColorListAdapter?.onItemClickListener=onItemClickListener
        rgbDiyColorListAdapter?.onItemLongClickListener=onItemLongClickListener
        sbSpeed.setOnSeekBarChangeListener(barChangeListener)

        if (isChange) {
            toolbar.title = getString(R.string.update_gradient)
            editName.setText(diyGradient?.name)
            sbSpeed.progress= diyGradient?.speed!!
            tvSpeed.text = getString(R.string.speed_text, diyGradient?.speed!!)
        } else {
            toolbar.title = getString(R.string.add_gradient)
            editName.setText(DBUtils.getDefaultModeName())
            sbSpeed.progress= 50
            tvSpeed.text = getString(R.string.speed_text, 50)
        }

        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private val barChangeListener = object : SeekBar.OnSeekBarChangeListener {


        override fun onStopTrackingTouch(seekBar: SeekBar) {
            this.onValueChange(seekBar, seekBar.progress, true)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {

        }

        override fun onProgressChanged(seekBar: SeekBar, progress: Int,
                                       fromUser: Boolean) {
            tvSpeed.text = getString(R.string.speed_text, progress.toString())
        }

        @SuppressLint("StringFormatInvalid")
        private fun onValueChange(view: View, progress: Int, immediate: Boolean) {
            speed = progress
            if (speed == 0) {
                speed = 1
            }
            tvSpeed.text = getString(R.string.speed_text, speed.toString())
//            if (positionState != 0) {
//                stopGradient()
//                Thread.sleep(200)
//                Commander.applyGradient(dstAddress, positionState, speed, firstLightAddress, successCallback = {}, failedCallback = {})
//            }
        }
    }

    override fun onClick(v: View?) {
        if(checkIsCorrect()){
            if(isChange){
                updateNode()
            }else{
                saveNode()
            }
        }
    }

    private fun updateNode() {
        showLoadingDialog(getString(R.string.save_gradient_dialog_tip))

        Thread{
            diyGradient?.name=editName.text.toString().trim()
            diyGradient?.type=NODE_MODE_RGB_GRADIENT
            diyGradient?.speed=speed

            DBUtils.updateGradient(diyGradient!!)
            val belongDynamicModeId = diyGradient?.id!!
            DBUtils.deleteColorNodeList(DBUtils.getColorNodeListByDynamicModeId(belongDynamicModeId))

            for(item in colorNodeList!!){
                item.belongDynamicChangeId =belongDynamicModeId
                DBUtils.saveColorNode(item)
            }

            deleteGradient(belongDynamicModeId)
            Thread.sleep(200)
            startSendCmdToAddDiyGradient(diyGradient!!)
            hideLoadingDialog()

            setResult(Activity.RESULT_OK)
            finish()
        }.start()
    }

    private fun deleteGradient(id: Long) {
            Commander.deleteGradient(dstAddress,id.toInt(),{},{})
    }

    private fun saveNode(){
        showLoadingDialog(getString(R.string.save_gradient_dialog_tip))
        Thread{
            diyGradient = DbDiyGradient()

            diyGradient?.id = getGradientId()
            diyGradient?.name = editName.text.toString().trim()
            diyGradient?.type=NODE_MODE_RGB_GRADIENT
            diyGradient?.belongRegionId = SharedPreferencesUtils.getCurrentUseRegion()
            diyGradient?.speed=speed

            DBUtils.saveGradient(diyGradient!!, false)

            val belongDynamicModeId = diyGradient!!.id
            for(item in colorNodeList!!){
                item.belongDynamicChangeId =belongDynamicModeId
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
        var addNodeList:ArrayList<DbColorNode> = ArrayList()
        for(item in colorNodeList!!){
//            if(item.rgbw!=-1){
                addNodeList.add(item)
//            }
        }

        val address=dstAddress
        val id=diyGradient.id.toInt()
        var nodeId=0
        var nodeMode=diyGradient.type
        var brightness = 0
        var r=0
        var g=0
        var b=0
        var c=0
        var w=0

        var temperature=0
        if(nodeMode<3){
            //rgb模式
            for(j in addNodeList.indices){
                nodeId = j
                if(addNodeList[j].rgbw==-1){
//                    nodeId = 0xff
//                    brightness = 0
//                    r = 0
//                    g = 0
//                    b = 0
//                    w = 0
//
//                    Thread.sleep(1000)
//                    Commander.addGradient(address,id,nodeId,nodeMode,brightness,r,g,b,c,w,{},{})
                }else{
                    var item=addNodeList[j]
                    brightness = item.brightness
                    r = (item.rgbw and 0xff0000) shr 16
                    g = (item.rgbw and 0x00ff00) shr 8
                    b = (item.rgbw and 0x0000ff)
                    w = Color.alpha(item.rgbw)

                    Thread.sleep(1000)
                    if(brightness > 99){
                        brightness = 99
                    }
                    if(w>99){
                        w = 99
                    }
                    Commander.addGradient(address,id,nodeId,nodeMode,brightness,r,g,b,c,w,{},{})
                }
            }
        }else{
            //双色温模式
            for(j in addNodeList.indices){
                nodeId = j+1
                var item=addNodeList[j]
                brightness = item.brightness
                temperature = item.colorTemperature

                Thread.sleep(200)
                Commander.addGradient(address,id,nodeId,nodeMode,brightness,temperature,0,0,0,0,{},{})
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

        if (list.size == 0) {
            id = 1
        }

        return java.lang.Long.valueOf(id.toLong())
    }

    private fun checkIsCorrect(): Boolean {
        if(editName.text.toString().trim().isEmpty()){
            ToastUtils.showLong(getString(R.string.name_not_null_tip))
           return false
        }

//        var checkColorHaveCheck=false
//        for(i in colorNodeList!!.indices){
//            if(colorNodeList!![i].rgbw!=-1){
//                checkColorHaveCheck=true
//            }
//        }
//
//        if(checkColorHaveCheck){
//            return true
//        }else{
//            ToastUtils.showLong("请至少选择一个颜色")
//            return false
//        }
        return true
    }

    val onItemClickListener = BaseQuickAdapter.OnItemClickListener {
        adapter, view, position ->
        val intent= Intent(this,SelectColorGradientAct::class.java)
        colorNodeList!![position].dstAddress=dstAddress
        intent.putExtra(Constant.COLOR_NODE_KEY, colorNodeList!![position])
        startActivityForResult(intent, position)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            colorNodeList!![requestCode] = data!!.extras["color"] as DbColorNode
            rgbDiyColorListAdapter?.notifyDataSetChanged()
        }
    }

    val onItemLongClickListener = BaseQuickAdapter.OnItemLongClickListener { adapter, view, position ->
        colorNodeList!![position].rgbw=-1
        adapter?.notifyDataSetChanged()
        true
    }

    private fun creatNewData() {
        colorNodeList = ArrayList()
        for (i in 0..7) {
            var colorNode = DbColorNode()
            when (i) {
                0 -> {
                    colorNode.index=0
                    colorNode.rgbw=0x00ff0000
                }
                1 -> {
                    colorNode.index=1
                    colorNode.rgbw=0x0000ff00
                }
                2 -> {
                    colorNode.index=2
                    colorNode.rgbw=0x000000ff
                }
                3 -> {
                    colorNode.index=3
                    colorNode.rgbw=0x00ffffff
                }
                4 -> {
                    colorNode.index=4
                    colorNode.rgbw=-1
                }
                5 -> {
                    colorNode.index=5
                    colorNode.rgbw=-1
                }
                6 -> {
                    colorNode.index=6
                    colorNode.rgbw=-1
                }
                7 -> {
                    colorNode.index=7
                    colorNode.rgbw=-1
                }
            }
            colorNodeList!!.add(colorNode)
        }
    }

}
package com.dadoutek.uled.rgb

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import com.android.actionsheetdialog.ActionSheetDialog
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbDiyGradient
import com.dadoutek.uled.model.ItemRgbGradient
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.tellink.TelinkBaseActivity
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_rgb_gradient.*
import kotlinx.android.synthetic.main.toolbar.*
import java.util.*
import java.util.concurrent.TimeUnit

class RGBGradientActivity : TelinkBaseActivity(), View.OnClickListener {

    private var buildInModeList: ArrayList<ItemRgbGradient>? = null
    private var diyGradientList: MutableList<DbDiyGradient>? = null
    private var rgbGradientAdapter: RGBGradientAdapter? = null
    private var rgbDiyGradientAdapter: RGBDiyGradientAdapter? = null
    private var applyDisposable: Disposable? = null
    private var dstAddress: Int = 0
    private var firstLightAddress: Int = 0
    var type = Constant.TYPE_GROUP
    var speed = 50
    var positionState = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rgb_gradient)
        initToolbar()
        initData()
        initView()
    }

    override fun onPause() {
        super.onPause()
        applyDisposable?.dispose()
    }

    private fun initToolbar() {
        toolbar.title = getString(R.string.dynamic_gradient)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        btnStopGradient.visibility = View.VISIBLE
        btnStopGradient.setOnClickListener(this)
        normal_rgb.setOnClickListener(this)
    }

    private fun initData() {
        val intent = intent
        type = intent.getStringExtra(Constant.TYPE_VIEW)
        dstAddress = intent.getIntExtra(Constant.TYPE_VIEW_ADDRESS, 0)
        if (type == Constant.TYPE_GROUP) {
            val lightList = DBUtils.getLightByGroupMesh(dstAddress)
            if (lightList.size > 0) {
                firstLightAddress = lightList[0].meshAddr
            }
        } else {
            firstLightAddress = dstAddress
        }
        buildInModeList = ArrayList()
        val presetGradientList = resources.getStringArray(R.array.preset_gradient)
        for (i in 0..10) {
            var item = ItemRgbGradient()
            item.name = presetGradientList[i]
            buildInModeList?.add(item)
        }

        diyGradientList=DBUtils.diyGradientList
    }

    private fun initView() {
        changeToBuildInPage()
        diyButton.setOnClickListener(this)
        buildInButton.setOnClickListener(this)
        btnStopGradient.visibility = View.VISIBLE
        btnStopGradient.setOnClickListener(this)
    }

    private fun applyPresetView() {
        this.sbSpeed!!.progress = speed
        tvSpeed.text = getString(R.string.speed_text, speed.toString())
        this.sbSpeed!!.setOnSeekBarChangeListener(this.barChangeListener)
        val layoutmanager = LinearLayoutManager(this)
        layoutmanager.orientation = LinearLayoutManager.VERTICAL
        builtInModeRecycleView!!.layoutManager = layoutmanager
        this.rgbGradientAdapter = RGBGradientAdapter(R.layout.item_gradient_mode, buildInModeList)
        builtInModeRecycleView?.itemAnimator = DefaultItemAnimator()

        rgbGradientAdapter!!.onItemChildClickListener = onItemChildClickListener
        rgbGradientAdapter!!.bindToRecyclerView(builtInModeRecycleView)
    }

    private fun applyDiyView() {
        btnAdd.setOnClickListener(this)
        val layoutmanager = LinearLayoutManager(this)
        layoutmanager.orientation = LinearLayoutManager.VERTICAL
        builtDiyModeRecycleView!!.layoutManager = layoutmanager
        this.rgbDiyGradientAdapter = RGBDiyGradientAdapter(R.layout.activity_diy_gradient_item, diyGradientList)
        builtDiyModeRecycleView?.itemAnimator = DefaultItemAnimator()
        rgbDiyGradientAdapter!!.bindToRecyclerView(builtDiyModeRecycleView)
        rgbDiyGradientAdapter!!.onItemClickListener=onItemClickListener
        rgbDiyGradientAdapter!!.onItemChildClickListener=onItemChildDiyClickListener
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
            if (positionState != 0) {
                stopGradient()
                Thread.sleep(200)
                Commander.applyGradient(dstAddress, positionState, speed, firstLightAddress, successCallback = {}, failedCallback = {})
            }
        }
    }

    private var onItemClickListener = BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
        //应用自定义渐变
        Thread{
            stopGradient()
            Thread.sleep(200)
            Commander.applyGradient(dstAddress, diyGradientList!![position].id.toInt(),
                    diyGradientList!![position].speed, firstLightAddress, successCallback = {}, failedCallback = {})

        }.start()
       }

    private var onItemChildDiyClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        when(view.id){
            R.id.more->{
                showMoreSetDialog(adapter,position)
            }
        }
    }

    private fun showMoreSetDialog(adapter: BaseQuickAdapter<Any, BaseViewHolder>, position: Int) {
        val dialog = ActionSheetDialog.ActionSheetBuilder(this@RGBGradientActivity, R.style.ActionSheetDialogBase)
                .setItems(arrayOf<CharSequence>(getString(R.string.delete), getString(R.string.edit)), object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface, which: Int) {
                       when(which){
                           0->{
                               deleteGradient(position,adapter,dialog)
                           }
                           1->{
                               transChangeAct(diyGradientList!![position])
                               dialog.dismiss()
                           }
                       }
                    }
                })
                .setNegativeButton(getString(R.string.cancel),{ dialog, which ->  })
                .setCancelable(true)
                .create()
        dialog.show()
    }

    fun deleteGradient(position: Int, adapter: BaseQuickAdapter<Any, BaseViewHolder>, dialog: DialogInterface) {
        startDeleteGradientCmd(diyGradientList!![position].id)
        DBUtils.deleteGradient(diyGradientList!![position])
        DBUtils.deleteColorNodeList(DBUtils.getColorNodeListByIndex(diyGradientList!![position].id!!))
        adapter.remove(position)
        dialog.dismiss()
    }

    private fun startDeleteGradientCmd(id: Long) {
        Commander.deleteGradient(dstAddress,id.toInt(),{},{})
    }

    private var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        //应用内置渐变
        applyDisposable?.dispose()
        applyDisposable = Observable.timer(50, TimeUnit.MILLISECONDS, Schedulers.io()).subscribe {
            for(i in 0..2){
                stopGradient()
                Thread.sleep(50)
            }
            positionState = position + 1
            Commander.applyGradient(dstAddress, positionState, speed, firstLightAddress, successCallback = {}, failedCallback = {})
        }
    }


    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.diyButton -> {
                changeToDiyPage()
            }
            R.id.buildInButton -> {
                changeToBuildInPage()
            }
            R.id.btnStopGradient -> {
                stopGradient()
            }
            R.id.normal_rgb -> {
                finish()
            }
            R.id.btnAdd -> {
                transAddAct()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        diyGradientList=DBUtils.diyGradientList
        changeToDiyPage()
    }

    private fun transAddAct() {
        if(DBUtils.diyGradientList.size<6){
            val intent=Intent(this,SetDiyColorAct::class.java)
            intent.putExtra(Constant.IS_CHANGE_COLOR,false)
            intent.putExtra(Constant.TYPE_VIEW_ADDRESS,dstAddress)
            startActivityForResult(intent,0)
        }else{
            ToastUtils.showLong(getString(R.string.add_gradient_limit))
        }
    }

    private fun transChangeAct(dbDiyGradient: DbDiyGradient) {
        val intent=Intent(this,SetDiyColorAct::class.java)
        intent.putExtra(Constant.IS_CHANGE_COLOR,true)
        intent.putExtra(Constant.GRADIENT_KEY,dbDiyGradient)
        intent.putExtra(Constant.TYPE_VIEW_ADDRESS,dstAddress)
        startActivityForResult(intent,0)
    }

    private fun changeToDiyPage() {
        diyButton.setBackgroundColor(resources.getColor(R.color.primary))
        diyButton.setTextColor(resources.getColor(R.color.white))
        buildInButton.setBackgroundColor(resources.getColor(R.color.white))
        buildInButton.setTextColor(resources.getColor(R.color.primary))
        layoutModeDiy.visibility=View.VISIBLE
        layoutModePreset.visibility=View.GONE
        applyDiyView()
    }

    private fun changeToBuildInPage(){
        diyButton.setBackgroundColor(resources.getColor(R.color.white))
        diyButton.setTextColor(resources.getColor(R.color.primary))
        buildInButton.setBackgroundColor(resources.getColor(R.color.primary))
        buildInButton.setTextColor(resources.getColor(R.color.white))
        layoutModeDiy.visibility=View.GONE
        layoutModePreset.visibility=View.VISIBLE
        applyPresetView()
    }

    fun stopGradient() {
        Commander.closeGradient(dstAddress, positionState, speed, successCallback = {}, failedCallback = {})
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> ActivityUtils.finishToActivity(MainActivity::class.java, false,
                    true)
        }
        return super.onOptionsItemSelected(item)
    }

}
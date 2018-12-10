package com.dadoutek.uled.light

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.*
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.group.GroupListAdapter
import com.dadoutek.uled.group.GroupListRecycleViewAdapter
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.ItemGroup
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.GuideUtils
import com.dadoutek.uled.util.StringUtils
import com.telink.TelinkApplication
import com.telink.bluetooth.light.DeviceInfo
import kotlinx.android.synthetic.main.activity_config_light_light.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.design.snackbar
import java.util.regex.Pattern

class ConfigLightlightActivity :TelinkBaseActivity(), View.OnClickListener, AdapterView.OnItemSelectedListener{
    private lateinit var mDeviceInfo: DeviceInfo
    private var nightLightGroupRecycleViewAdapter: NightLightGroupRecycleViewAdapter? = null
    private var nightLightEditGroupAdapter: NightLightEditGroupAdapter? = null
    private var mSelectGroupAddr: Int = 0xFF  //代表所有灯
    private val CMD_OPEN_LIGHT=0X01
    private val CMD_CLOSE_LIGHT=0X00
    private val CMD_CONTROL_GROUP=0X02
    private var switchMode=0X00
    lateinit var secondsList:Array<String>
    private var selectTime=0
    private var currentPageIsEdit=false

    private var showGroupList: MutableList<ItemGroup>? = null
    private var showCheckListData: MutableList<DbGroup>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config_light_light)
        fabConfirm.setOnClickListener(this)
        initToolbar()
        initData()
        initView()
        getVersion()
    }

    private fun initView() {
        showDataListView()
        spDelay.onItemSelectedListener=this
        spSwitchMode.onItemSelectedListener=this
        secondsList=resources.getStringArray(R.array.light_light_time_list)
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, secondsList)
        spDelay.adapter = adapter
        btnSelectGroup.setOnClickListener(this)
    }

    //显示数据列表页面
    private fun showDataListView() {
        currentPageIsEdit=false
        data_view_layout.visibility = View.VISIBLE
        edit_data_view_layout.visibility = View.GONE

        val layoutmanager = GridLayoutManager(this,2)
        recyclerViewNightLightGroups.layoutManager = layoutmanager
        this.nightLightGroupRecycleViewAdapter = NightLightGroupRecycleViewAdapter(
                R.layout.activity_night_light_groups_item, showGroupList)
//        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
//        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.divider)))
//        //添加分割线
//        recyclerViewNightLightGroups.addItemDecoration(decoration)
        nightLightGroupRecycleViewAdapter?.bindToRecyclerView(recyclerViewNightLightGroups)
    }

    //显示配置数据页面
    private fun showEditListVew() {
        currentPageIsEdit=true
        data_view_layout.visibility = View.GONE
        edit_data_view_layout.visibility = View.VISIBLE

        showCheckListData = DBUtils.allGroups
        if(showGroupList!!.size!=0){
            for(i in showCheckListData!!.indices){
                for(j in showGroupList!!.indices){
                    if(showCheckListData!![i].meshAddr == showGroupList!![j].groupAress){
                        showCheckListData!![i].checked= true
                        break
                    }else if(j==showGroupList!!.size-1 && showCheckListData!![i].meshAddr != showGroupList!![j].groupAress){
                        showCheckListData!![i].checked= false
                    }
                }
            }
            changeCheckedViewData()
        }else{
            for(i in showCheckListData!!.indices){
                showCheckListData!![i].enableCheck=true
                showCheckListData!![i].checked=false
            }
        }

        val layoutmanager = LinearLayoutManager(this)
        layoutmanager.orientation = LinearLayoutManager.VERTICAL
        recyclerView_select_group_list_view.layoutManager = layoutmanager
        this.nightLightEditGroupAdapter = NightLightEditGroupAdapter(
                R.layout.activity_night_light_edit_group_item, showCheckListData!!)
        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.divider)))
        //添加分割线
        recyclerView_select_group_list_view.addItemDecoration(decoration)
        nightLightEditGroupAdapter?.bindToRecyclerView(recyclerView_select_group_list_view)
        nightLightEditGroupAdapter?.onItemChildClickListener=onItemChildClickListenerCheck
    }

    internal var onItemChildClickListenerCheck: BaseQuickAdapter.OnItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        when (view.id) {
            R.id.group_check_state -> {
                val item = showCheckListData!!.get(position)
                if(item.enableCheck){
                    showCheckListData!!.get(position).checked = !item.checked
                    changeCheckedViewData()
                    adapter?.notifyDataSetChanged()
                }
            }
        }
    }

    private fun changeCheckedViewData(){
        var isAllCanCheck=true
        for(i in showCheckListData!!.indices){
            if(showCheckListData!![0].meshAddr==0xffff && showCheckListData!![0].checked){
                showCheckListData!![0].enableCheck=true
                if(showCheckListData!!.size>1 && i>0){
                    showCheckListData!![i].enableCheck=false
                }
            }else{
                showCheckListData!![0].enableCheck=false
                if(showCheckListData!!.size>1 && i>0){
                    showCheckListData!![i].enableCheck=true
                }

                if(i>0 && showCheckListData!![i].checked){
                    isAllCanCheck=false
                }
            }
        }
        if(isAllCanCheck){
            showCheckListData!![0].enableCheck=true
        }
    }

    private fun saveCurrenEditResult() {
            val oldResultItemList= ArrayList<ItemGroup>()
            val newResultItemList= ArrayList<ItemGroup>()

            for(i in showCheckListData!!.indices){
                if(showCheckListData!![i].checked){
                    if(showGroupList!!.size==0){
                        val newItemGroup=ItemGroup()
                        newItemGroup.brightness=50
                        newItemGroup.temperature=50
                        newItemGroup.color=R.color.white
                        newItemGroup.checked=true
                        newItemGroup.enableCheck=true
                        newItemGroup.gpName=showCheckListData!![i].name
                        newItemGroup.groupAress=showCheckListData!![i].meshAddr
                        newResultItemList.add(newItemGroup)
                    }else{
                        for(j in showGroupList!!.indices){
                            if(showCheckListData!![i].meshAddr==showGroupList!![j].groupAress){
                                oldResultItemList.add(showGroupList!![j])
                                break
                            }else if(j == showGroupList!!.size-1){
                                val newItemGroup=ItemGroup()
                                newItemGroup.brightness=50
                                newItemGroup.temperature=50
                                newItemGroup.color=R.color.white
                                newItemGroup.checked=true
                                newItemGroup.enableCheck=true
                                newItemGroup.gpName=showCheckListData!![i].name
                                newItemGroup.groupAress=showCheckListData!![i].meshAddr
                                newResultItemList.add(newItemGroup)
                            }
                        }
                    }
                }
            }
            showGroupList?.clear()
            showGroupList?.addAll(oldResultItemList)
            showGroupList?.addAll(newResultItemList)

        if(showGroupList!!.size>8){
            ToastUtils.showLong(getString(R.string.tip_night_light_group_limite_tip))
        }else{
            showDataListView()
        }
    }

    private fun getVersion() {
        var dstAdress = 0
        if (TelinkApplication.getInstance().connectDevice != null) {
            dstAdress = mDeviceInfo.meshAddress
            Commander.getDeviceVersion(dstAdress,
                    successCallback = {
                        versionLayoutPS.visibility = View.VISIBLE
                        tvPSVersion.text = it
                    },
                    failedCallback = {
                        versionLayoutPS.visibility = View.GONE
                    })
        } else {
            dstAdress = 0
        }
    }

    private fun initToolbar() {
        toolbar.title = getString(R.string.sensor_title)
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener { finish() }

    }

    private fun initData() {
//        mDeviceInfo = intent.getParcelableExtra("deviceInfo")
        showCheckListData = DBUtils.allGroups
        for(i in showCheckListData!!.indices){
            showCheckListData!![i].checked=false
        }
        showGroupList= ArrayList<ItemGroup>()
    }


    private fun configLightlight() {
//        val spGroup = groupConvertSpecialValue(groupAddr)
        val groupH: Byte = (mSelectGroupAddr shr 8 and 0xff).toByte()

        val timeH: Byte = (selectTime shr 8 and 0xff).toByte()
        val timeL: Byte = (selectTime and 0xff).toByte()
        val paramBytes = byteArrayOf(
                DeviceType.LIGHT_LIGHT.toByte(),
                switchMode.toByte(),timeL,timeH
        )
        val paramBytesGroup:ByteArray
        paramBytesGroup = byteArrayOf(
                DeviceType.LIGHT_LIGHT.toByte(),CMD_CONTROL_GROUP.toByte(),0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
        )

        var canSendGroup=true
        for(i in showGroupList!!.indices){
            if(showGroupList!![i].groupAress==0xffff){
                canSendGroup=false
                break
            }
            val groupL: Byte = (showGroupList!![i].groupAress and 0xff).toByte()
            paramBytesGroup[i+2]=groupL
            LogUtils.d("groupL="+groupL+""+"-----"+showGroupList!![i].groupAress)
        }

        TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_LIGHT_LIGHT,
                mDeviceInfo.meshAddress,
                paramBytes)

        Thread.sleep(300)

        if(canSendGroup){
            TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_LIGHT_LIGHT,
                    mDeviceInfo.meshAddress,
                    paramBytesGroup)
        }

        Thread.sleep(300)
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        when(parent?.id){
            R.id.spDelay ->{
                val time=getTime(spDelay.selectedItem as String)
                if(position>4){
                    selectTime=time*60
                }else{
                    selectTime=time
                }
            }
            R.id.spSwitchMode->{
                if(position==0){
                    switchMode=CMD_OPEN_LIGHT
                }else{
                    switchMode=CMD_CLOSE_LIGHT
                }
            }
        }
    }

    private fun getTime(timeStr:String) : Int{
        val str = timeStr
        val p = Pattern.compile("\\d+")
        val m = p.matcher(str)
        m.find()
        return m.group().toInt()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        mSelectGroupAddr = 0xFF
    }

    private fun doFinish() {
        TelinkLightService.Instance().idleMode(true)
        TelinkLightService.Instance().disconnect()
        finish()
    }

    private fun configureComplete() {
        TelinkLightService.Instance().idleMode(true)
        TelinkLightService.Instance().disconnect()
        ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
    }

    override fun onBackPressed() {
        doFinish()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.fabConfirm -> {
                if(currentPageIsEdit){
                    stopEditMode()
                }else{
                    configDevice()
                }
            }
            R.id.btnSelectGroup -> {
                showEditListVew()
            }
        }
    }

    private fun stopEditMode() {
        saveCurrenEditResult()
    }

    private fun configDevice() {
        showLoadingDialog(getString(R.string.configuring_switch))
        Thread {

            val mApplication = this.application as TelinkLightApplication
            val mesh = mApplication.getMesh()

            configLightlight()
            Thread.sleep(300)

            Commander.updateMeshName(
                    successCallback = {
                        hideLoadingDialog()
                        configureComplete()
                    },
                    failedCallback = {
                        snackbar(configPirRoot, getString(R.string.pace_fail))
                        hideLoadingDialog()
                    })
        }.start()
    }
}
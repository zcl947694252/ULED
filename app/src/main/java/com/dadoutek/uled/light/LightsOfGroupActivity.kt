package com.dadoutek.uled.light

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.util.DiffUtil
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.group.BatchGroupActivity
import com.dadoutek.uled.group.GroupOTAListActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.rgb.RGBSettingActivity
import com.dadoutek.uled.rgb.RgbBatchGroupActivity
import com.dadoutek.uled.switches.ConfigSceneSwitchActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.DataManager
import com.telink.bluetooth.LeBluetooth
import com.telink.bluetooth.light.ConnectionStatus
import com.telink.bluetooth.light.DeviceInfo
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_lights_of_group.*
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.startActivity
import java.util.*
import kotlin.collections.ArrayList

/**
 * 创建者     zcl
 * 创建时间   2019/8/30 18:46
 * 描述	      ${点击组名跳转搜索设备}$
 *
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   显示X组里所有灯的页面
 */

class LightsOfGroupActivity : TelinkBaseActivity(), SearchView.OnQueryTextListener, View.OnClickListener {
    private var disposableConnect: Disposable? = null
    private val REQ_LIGHT_SETTING: Int = 0x01
    private var group: DbGroup? = null
    private var mDataManager: DataManager? = null
    private var mApplication: TelinkLightApplication? = null
    private lateinit var lightList: MutableList<DbLight>
    private var adapter: LightsOfGroupRecyclerViewAdapter? = null
    private var positionCurrent: Int = 0
    private var currentLight: DbLight? = null
    private var searchView: SearchView? = null
    private var canBeRefresh = true
    private var connectMeshAddress: Int = 0
    private var mConnectDisposal: Disposable? = null
    private var mScanDisposal: Disposable? = null
    private var mScanTimeoutDisposal: Disposable? = null
    private var mCheckRssiDisposal: Disposable? = null
    private var mNotFoundSnackBar: Snackbar? = null
    private var acitivityIsAlive = true
    private var recyclerView: RecyclerView? = null
    private var strLight: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lights_of_group)
        initToolbar()
        initParameter()
        initData()
        initView()
    }


    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.light_add_device_btn -> {
                if (strLight == "cw_light") {
                    if (DBUtils.getAllNormalLight().size == 0) {
                        intent = Intent(this, DeviceScanningNewActivity::class.java)
                        intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, false)
                        intent.putExtra(Constant.TYPE_VIEW, Constant.LIGHT_KEY)
                        startActivityForResult(intent, 0)
                    } else {
                        addDevice()
                    }
                } else if (strLight == "rgb_light") {
                    if (DBUtils.getAllRGBLight().size == 0) {
                        intent = Intent(this, DeviceScanningNewActivity::class.java)
                        intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                        intent.putExtra(Constant.TYPE_VIEW, Constant.RGB_LIGHT_KEY)
                        startActivityForResult(intent, 0)
                    } else {
                        addDevice()
                    }
                }
            }
        }
    }

    private fun addDevice() {
        if (strLight == "cw_light") {
            val intent = Intent(this,
                    BatchGroupActivity::class.java)
            intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
            intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
            intent.putExtra("lightType", "cw_light")
            intent.putExtra("cw_light_group_name", group?.name)
            startActivity(intent)
        } else if (strLight == "rgb_light") {
            val intent = Intent(this,
                    RgbBatchGroupActivity::class.java)
            intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
            intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
            intent.putExtra("lightType", "rgb_light")
            intent.putExtra("rgb_light_group_name", group?.name)
//            startActivity(intent)
            startActivity(intent)
        }
    }

    private fun initToolbar() {
        toolbar.setTitle(R.string.group_setting_header)
        tv_function1.visibility = View.VISIBLE
        tv_function1.setText(R.string.batch_group)
        tv_function1.setOnClickListener {
            when (strLight) {
                "cw_light" -> {
                    if (DBUtils.getAllNormalLight().size == 0) {
                        ToastUtils.showShort(getString(R.string.no_device))
                    } else {
                        val intent = Intent(this@LightsOfGroupActivity, GroupOTAListActivity::class.java)
                        intent.putExtra("group", group)
                        startActivity(intent)
                    }
                }
                "rgb_light" -> {
                    if (DBUtils.getAllRGBLight().size == 0) {
                        ToastUtils.showShort(getString(R.string.no_device))
                    } else {
                        startActivity<GroupOTAListActivity>("group" to group!!, "groupType" to DeviceType.LIGHT_RGB)
                    }
                }
            }
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        searchView!!.clearFocus()
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        if (newText != null && !newText.isEmpty()) {
            filter(newText, true)
            adapter!!.notifyDataSetChanged()
        } else {
            filter(newText, false)
            adapter!!.notifyDataSetChanged()
        }

        return false
    }

    private fun filter(groupName: String?, isSearch: Boolean) {
        val list = DBUtils.groupList
        if (lightList != null && lightList.size > 0) {
            lightList.clear()
        }


        when {
            isSearch -> {
                for (i in list.indices) {
                    if (groupName == list[i].name || (list[i].name).startsWith(groupName!!)) {
                        lightList.addAll(DBUtils.getLightByGroupID(list[i].id))
                    }
                }

            }
            else -> {
                for (i in list.indices) {
                    if (list[i].meshAddr == 0xffff) {
                        Collections.swap(list, 0, i)
                    }
                }

                for (j in list.indices) {
                    lightList.addAll(DBUtils.getLightByGroupID(list[j].id))
                }
            }
        }
    }

    private fun initParameter() {
        val gp = this.intent.extras!!.get("group")
        if (gp != null)
            this.group = gp as DbGroup
        val light = this.intent.extras!!.get("light")
        if (light != null)
            this.strLight = light as String
        this.mApplication = this.application as TelinkLightApplication
        mDataManager = DataManager(this, mApplication!!.mesh.name, mApplication!!.mesh.password)
    }

    override fun onResume() {
        super.onResume()
        initToolbar()
        initParameter()
        initData()
        initView()
        initOnLayoutListener()
    }

    override fun onStop() {
        super.onStop()
        TelinkLightService.Instance()?.disableAutoRefreshNotify()
    }

    override fun onDestroy() {
        super.onDestroy()
        canBeRefresh = false
        acitivityIsAlive = false
        mScanDisposal?.dispose()
        if (TelinkLightApplication.getApp().connectDevice == null) {
            TelinkLightService.Instance()?.idleMode(true)
            LeBluetooth.getInstance().stopScan()
        }
    }

    private fun initData() {
        lightList = ArrayList()
        if (group?.meshAddr == 0xffff) {
            filter("", false)
        } else {
            lightList = DBUtils.getLightByGroupID(group!!.id)
        }

        if (lightList.size > 0) {
            recycler_view_lights.visibility = View.VISIBLE
            no_light.visibility = View.GONE
            /*   if (strLight == "cw_light") {
                   var batchGroup = toolbar.findViewById<TextView>(R.id.tv_function1)
                   toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.VISIBLE
                   batchGroup.setText(R.string.batch_group)
                   batchGroup.visibility = View.GONE
                   batchGroup.setOnClickListener {
                       val intent = Intent(this,
                               BatchGroupActivity::class.java)
                       intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                       intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
                       intent.putExtra("lightType", "cw_light_group")
                       intent.putExtra("group", group?.id?.toInt())
                       startActivity(intent)
                   }
               } else if (strLight == "rgb_light") {
                   var batchGroup = toolbar.findViewById<TextView>(R.id.tv_function1)
                   toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.VISIBLE
                   batchGroup.setText(R.string.batch_group)
                   batchGroup.visibility = View.GONE
                   batchGroup.setOnClickListener {
                       val intent = Intent(this,
                               BatchGroupActivity::class.java)
                       intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                       intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
                       intent.putExtra("lightType", "rgb_light_group")
                       intent.putExtra("group", group?.id?:0)
                       startActivity(intent)
                   }
               }*/
        } else {
            toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.GONE
            recycler_view_lights.visibility = View.GONE
            no_light.visibility = View.VISIBLE
        }
    }

    private fun getNewData(): MutableList<DbLight> {
        if (group?.meshAddr == 0xffff) {
            filter("", false)
        } else {
            lightList = DBUtils.getLightByGroupID(group?.id ?: 100000000000)
        }

        if (group?.meshAddr == 0xffff) {
            toolbar.title = getString(R.string.allLight) + " (" + lightList.size + ")"
        } else {
            toolbar.title = (group?.name ?: "") + " (" + lightList.size + ")"
        }
        return lightList
    }

    fun notifyData() {
        val mOldDatas: MutableList<DbLight>? = lightList
        val mNewDatas: MutableList<DbLight>? = getNewData()
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return mOldDatas?.get(oldItemPosition)?.id?.equals(mNewDatas?.get
                (newItemPosition)?.id) ?: false;
            }

            override fun getOldListSize(): Int {
                return mOldDatas?.size ?: 0
            }

            override fun getNewListSize(): Int {
                return mNewDatas?.size ?: 0
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val beanOld = mOldDatas?.get(oldItemPosition)
                val beanNew = mNewDatas?.get(newItemPosition)
                return if (!beanOld?.name.equals(beanNew?.name)) {
                    return false//如果有内容不同，就返回false
                } else true

            }
        }, true)
        adapter?.let { diffResult.dispatchUpdatesTo(it) }
        lightList = mNewDatas!!
        adapter?.setNewData(lightList)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (group?.meshAddr == 0xffff) {
            menuInflater.inflate(R.menu.menu_search, menu)
            searchView = (menu!!.findItem(R.id.action_search)).actionView as SearchView
            searchView!!.setOnQueryTextListener(this)
            searchView!!.imeOptions = EditorInfo.IME_ACTION_SEARCH
            searchView!!.queryHint = getString(R.string.input_groupAdress)
            searchView!!.isSubmitButtonEnabled = true
            searchView!!.backgroundColor = resources.getColor(R.color.blue)
            searchView!!.alpha = 0.3f
            return super.onCreateOptionsMenu(menu)
        }
        return true
    }


    private fun initView() {
        if (group?.meshAddr == 0xffff) {
            toolbar.title = getString(R.string.allLight) + " (" + lightList.size + ")"
        } else {
            toolbar.title = (group?.name ?: "") + " (" + lightList.size + ")"
        }
        light_add_device_btn.setOnClickListener(this)
        recyclerView = findViewById(R.id.recycler_view_lights)
        recyclerView!!.layoutManager = GridLayoutManager(this, 3)
        recyclerView!!.itemAnimator = DefaultItemAnimator()
        adapter = LightsOfGroupRecyclerViewAdapter(R.layout.item_lights_of_group, lightList)
        adapter!!.onItemChildClickListener = onItemChildClickListener
        adapter!!.bindToRecyclerView(recyclerView)
        if (strLight == "cw_light") {
            for (i in lightList.indices) {
                lightList[i].updateIcon()
            }
        } else if (strLight == "rgb_light") {
            for (i in lightList.indices) {
                lightList[i].updateRgbIcon()
            }
        }

        if (strLight == "cw_light") {
            if (DBUtils.getAllNormalLight().size == 0) {
                light_add_device_btn.text = getString(R.string.device_scan_scan)
            } else {
                light_add_device_btn.text = getString(R.string.add_device)
            }
        } else if (strLight == "rgb_light") {
            if (DBUtils.getAllRGBLight().size == 0) {
                light_add_device_btn.text = getString(R.string.device_scan_scan)
            } else {
                light_add_device_btn.text = getString(R.string.add_device)
            }
        }
    }


    var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        currentLight = lightList[position]
        positionCurrent = position
        if (TelinkLightApplication.getApp().connectDevice == null) {
            ToastUtils.showShort(getString(R.string.device_disconnected))
            disposableConnect?.dispose()
            disposableConnect = connect(fastestMode = true)?.subscribe({
                ToastUtils.showShort(getString(R.string.connect_success))
            }, {
                ToastUtils.showShort(getString(R.string.connect_fail))
            })
            return@OnItemChildClickListener
        }
        when (view.id) {
            R.id.img_light -> {
                canBeRefresh = true
                if (currentLight!!.connectionStatus == ConnectionStatus.OFF.value) {
//                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, currentLight!!.meshAddr,
//                        byteArrayOf(0x01, 0x00, 0x00))
                    if (currentLight!!.productUUID == DeviceType.SMART_CURTAIN) {
                        Commander.openOrCloseCurtain(currentLight!!.meshAddr, true, false)
                    } else {
                        Commander.openOrCloseLights(currentLight!!.meshAddr, true)
                    }

                    currentLight!!.connectionStatus = ConnectionStatus.ON.value
                } else {
                    if (currentLight!!.productUUID == DeviceType.SMART_CURTAIN) {
                        Commander.openOrCloseCurtain(currentLight!!.meshAddr, false, false)
                    } else {
                        Commander.openOrCloseLights(currentLight!!.meshAddr, false)
                    }
                    currentLight!!.connectionStatus = ConnectionStatus.OFF.value
                }

                if (strLight == "cw_light") {
                    currentLight!!.updateIcon()
                } else if (strLight == "rgb_light") {
                    currentLight!!.updateRgbIcon()
                }
                DBUtils.updateLight(currentLight!!)
                runOnUiThread {
                    adapter?.notifyDataSetChanged()
                }
            }
            R.id.tv_setting -> {
                if (scanPb.visibility != View.VISIBLE) {
                    //判断是否为rgb灯
                    var intent = Intent(this@LightsOfGroupActivity, NormalSettingActivity::class.java)
                    if (currentLight?.productUUID == DeviceType.LIGHT_RGB) {
                        intent = Intent(this@LightsOfGroupActivity, RGBSettingActivity::class.java)
                        intent.putExtra(Constant.TYPE_VIEW, Constant.TYPE_LIGHT)
                    }
                    intent.putExtra(Constant.LIGHT_ARESS_KEY, currentLight)
                    intent.putExtra(Constant.GROUP_ARESS_KEY, group?.meshAddr)
                    intent.putExtra(Constant.LIGHT_REFRESH_KEY, Constant.LIGHT_REFRESH_KEY_OK)
                    startActivityForResult(intent, REQ_LIGHT_SETTING)
                } else {
                    ToastUtils.showLong(R.string.reconnecting)
                }
            }
        }
    }

    private fun stopConnectTimer() {
        mConnectDisposal?.dispose()
    }

    override fun onPause() {
        super.onPause()
        mScanTimeoutDisposal?.dispose()
        mConnectDisposal?.dispose()
        mNotFoundSnackBar?.dismiss()
        //移除事件
        stopConnectTimer()
        mCheckRssiDisposal?.dispose()
    }
}


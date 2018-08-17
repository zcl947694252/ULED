package com.dadoutek.uled.switches

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.MenuItem
import android.view.View
import com.blankj.utilcode.util.ActivityUtils
import com.dadoutek.uled.BuildConfig
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.telink.TelinkApplication
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.LightAdapter
import com.telink.bluetooth.light.LightAdapter.MODE_UPDATE_MESH
import com.telink.bluetooth.light.Parameters
import com.telink.util.Event
import com.telink.util.EventListener
import com.telink.util.Strings
import kotlinx.android.synthetic.main.activity_config_pir.*
import kotlinx.android.synthetic.main.activity_switch_group.*
import kotlinx.android.synthetic.main.content_switch_group.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.design.snackbar


class ConfigNormalSwitchActivity : AppCompatActivity(), EventListener<String> {

    private lateinit var mDeviceInfo: DeviceInfo
    private lateinit var mApplication: TelinkLightApplication
    private lateinit var mAdapter: SelectSwitchGroupRvAdapter
    private lateinit var mGroupArrayList: ArrayList<DbGroup>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_switch_group)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.select_group)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mApplication = application as TelinkLightApplication

        initView()
        initListener()
        getVersion()
    }

    private fun getVersion() {
        var dstAdress = 0
        if (TelinkApplication.getInstance().connectDevice != null) {
            dstAdress = mDeviceInfo.meshAddress
            Commander.getDeviceVersion(dstAdress,
                    successCallback = {
                        versionLayout.visibility = View.VISIBLE
                        tvLightVersion.text = it
                    },
                    failedCallback = {
                        versionLayout.visibility = View.GONE
                    })
        } else {
            dstAdress = 0
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                doFinish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun doFinish() {
        this.mApplication.removeEventListener(this)
        TelinkLightService.Instance().idleMode(true)
        TelinkLightService.Instance().disconnect()
        finish()
    }

    private fun configureComplete() {
        this.mApplication.removeEventListener(this)
        TelinkLightService.Instance().idleMode(true)
        TelinkLightService.Instance().disconnect()
        ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
    }

    override fun onBackPressed() {
        doFinish();
    }

    private fun initListener() {
//        this.mApplication.addEventListener(DeviceEvent.STATUS_CHANGED, this)

        fab.setOnClickListener { view ->
            if (mAdapter.selectedPos != -1) {
                progressBar.visibility = View.VISIBLE
                setGroupForSwitch()
//                updateNameForSwitch()
                Commander.updateMeshName(successCallback = {
                    launch(UI) {
                        progressBar.visibility = View.GONE
                    }
                    configureComplete()
                },
                        failedCallback = {
                            snackbar(configGroupRoot, getString(R.string.group_failed))
                            launch(UI) {
                                progressBar.visibility = View.GONE
                            }
                        })
            } else {
                snackbar(view, getString(R.string.please_select_group))
            }
        }

        mAdapter.setOnItemChildClickListener { adapter, view, position ->
            when (view.id) {
                R.id.checkBox -> {
                    if (mAdapter.selectedPos != position) {
                        //取消上个Item的勾选状态
                        mGroupArrayList[mAdapter.selectedPos].checked = false
                        mAdapter.notifyItemChanged(mAdapter.selectedPos)

                        //设置新的item的勾选状态
                        mAdapter.selectedPos = position
                        mGroupArrayList[mAdapter.selectedPos].checked = true
                        mAdapter.notifyItemChanged(mAdapter.selectedPos)
                    } else {
                        mGroupArrayList[mAdapter.selectedPos].checked = true
                        mAdapter.notifyItemChanged(mAdapter.selectedPos)
                    }
                }
            }
        }

    }


    override fun onDestroy() {
        super.onDestroy()
    }

    override fun performed(event: Event<String>?) {
        when (event?.type) {

        }

    }

    private fun setGroupForSwitch() {
        val mesh = this.mApplication.mesh
        val params = Parameters.createUpdateParameters()
        if (BuildConfig.DEBUG) {
            params.setOldMeshName(Constant.PIR_SWITCH_MESH_NAME)
        } else {
            params.setOldMeshName(mesh.factoryName)
        }
        params.setOldPassword(mesh.factoryPassword)
        params.setNewMeshName(mesh.name)
        val account = SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(),
                Constant.DB_NAME_KEY, "dadou")
        if (SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(),
                        Constant.USER_TYPE, Constant.USER_TYPE_OLD) == Constant.USER_TYPE_NEW) {
            params.setNewPassword(NetworkFactory.md5(
                    NetworkFactory.md5(mesh?.password) + account))
        } else {
            params.setNewPassword(mesh?.password)
        }

        params.setUpdateDeviceList(mDeviceInfo)
        val groupAddress = mGroupArrayList.get(mAdapter.selectedPos).meshAddr
        val paramBytes = byteArrayOf(0x01, (groupAddress and 0xFF).toByte(), //0x01 代表添加组
                (groupAddress shr 8 and 0xFF).toByte())
        TelinkLightService.Instance().sendCommandNoResponse(Opcode.SET_GROUP, mDeviceInfo.meshAddress,
                paramBytes)
//        Commander.addGroup(mDeviceInfo.meshAddress,groupAddress,)
    }

    private fun initView() {
        mDeviceInfo = intent.getParcelableExtra<DeviceInfo>("deviceInfo")
        val mesh = mApplication.mesh
//        val dataManager = DataManager(this, mesh.name, mesh.password)
        mGroupArrayList = ArrayList<DbGroup>()
        val groupList = DBUtils.groupList
//        mGroupArrayList.add(dataManager.createAllLightControllerGroup()) //添加全控

        for (group in groupList) {
//            if (group.containsLightList.size > 0 || group.meshAddress == 0xFFFF)
            group.checked = false
            mGroupArrayList.add(group)
        }
        if (groupList.size > 0) {
            groupList[0].checked = true
        }

        mAdapter = SelectSwitchGroupRvAdapter(R.layout.item_select_switch_group_rv, mGroupArrayList)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        mAdapter.bindToRecyclerView(recyclerView)

    }

}

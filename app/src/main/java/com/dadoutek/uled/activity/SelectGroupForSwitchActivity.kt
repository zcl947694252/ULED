package com.dadoutek.uled.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import com.blankj.utilcode.util.ActivityUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.TelinkLightApplication
import com.dadoutek.uled.TelinkLightService
import com.dadoutek.uled.adapter.SelectSwitchGroupRvAdapter
import com.dadoutek.uled.intf.NetworkFactory
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.util.DataManager
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.LightAdapter
import com.telink.bluetooth.light.LightAdapter.MODE_UPDATE_MESH
import com.telink.bluetooth.light.Parameters
import com.telink.util.Event
import com.telink.util.EventListener
import com.telink.util.Strings
import kotlinx.android.synthetic.main.activity_switch_group.*
import kotlinx.android.synthetic.main.content_switch_group.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.design.snackbar
import java.util.*


class SelectGroupForSwitchActivity : AppCompatActivity(), EventListener<String> {

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

        mApplication = getApplication() as TelinkLightApplication

        initView()
        initListener()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
    }

    private fun initListener() {
        this.mApplication.addEventListener(DeviceEvent.STATUS_CHANGED, this)

        fab.setOnClickListener { view ->
            if (mAdapter.selectedPos != -1) {
                progressBar.visibility = View.VISIBLE
                setGroupForSwitch()
                updateNameForSwitch()

            } else {
                snackbar(view, getString(R.string.please_select_group))
            }
        }

        mAdapter.setOnItemChildClickListener { _, view, position ->
            when (view.id) {
                R.id.btnOn -> {
                    val dstAddr = mGroupArrayList.get(position).meshAddr
                    TelinkLightService.Instance().sendCommandNoResponse(Opcode.LIGHT_ON_OFF, dstAddr,
                            byteArrayOf(0x01, 0x00, 0x00))
                }

                R.id.btnOff -> {
                    val dstAddr = mGroupArrayList.get(position).meshAddr
                    TelinkLightService.Instance().sendCommandNoResponse(Opcode.LIGHT_ON_OFF, dstAddr,
                            byteArrayOf(0x00, 0x00, 0x00))
                }
                R.id.checkBox -> {
                    val checkBox = view as CheckBox
                    if (checkBox.isChecked) {
//                        val oldPosition = mAdapter.selectedPos
                        mAdapter.selectedPos = position
//                        mAdapter.notifyItemChanged(oldPosition)
//                        mAdapter.notifyItemChanged(mAdapter.selectedPos)

                        for (i in 0..(mGroupArrayList.size - 1)) {
                            if (i != mAdapter.selectedPos) {
                                val cb = mAdapter.getViewByPosition(recyclerView, i, R.id.checkBox) as CheckBox
                                cb.isChecked = false
                            }
                        }
                    } else {

                        mAdapter.selectedPos = -1   // 设置成-1 代表没有选中任何item
                    }
                }
            }
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        this.mApplication.removeEventListener(DeviceEvent.STATUS_CHANGED, this)
    }

    override fun performed(event: Event<String>?) {
        when (event?.getType()) {
            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)
//            NotificationEvent.GET_GROUP -> this.onGetGroupEvent(event as NotificationEvent)
//            MeshEvent.ERROR -> this.onMeshEvent(event as MeshEvent)
        }

    }

    private fun onDeviceStatusChanged(deviceEvent: DeviceEvent) {
        val deviceInfo = deviceEvent.args

        when (deviceInfo.status) {
            LightAdapter.STATUS_UPDATE_MESH_COMPLETED -> {
                Log.d("Saw", "SelectGroupForSwitchActivity setStatus STATUS_UPDATE_MESH_COMPLETED")
                progressBar.visibility = View.GONE
                ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
            }
            LightAdapter.STATUS_UPDATE_MESH_FAILURE -> {
                snackbar(root, getString(R.string.group_failed))
                progressBar.visibility = View.GONE
            }
        }
    }


    private fun setGroupForSwitch() {
        val mesh = this.mApplication.mesh
        val params = Parameters.createUpdateParameters()
        params.setOldMeshName(mesh.factoryName)
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

    }

    private fun updateNameForSwitch() {
        val mesh = this.mApplication.mesh
        val params = Parameters.createUpdateParameters()
        params.setOldMeshName(mesh.factoryName)
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
//        TelinkLightService.Instance().updateMesh(params)


        val meshName = Strings.stringToBytes(mesh.name, 16)
        var password = Strings.stringToBytes(mesh.password, 16)
        if (SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(),
                        Constant.USER_TYPE, Constant.USER_TYPE_OLD) == Constant.USER_TYPE_NEW) {
             password = Strings.stringToBytes(NetworkFactory.md5(
                     NetworkFactory.md5(mesh?.password) + account), 16)
        } else {
            password = Strings.stringToBytes(mesh.password, 16)
        }

        TelinkLightService.Instance().adapter.mode = MODE_UPDATE_MESH
        TelinkLightService.Instance().adapter.mLightCtrl.reset(meshName, password, null)
    }

    private fun initView() {
        mDeviceInfo = intent.getParcelableExtra<DeviceInfo>("deviceInfo")
        val mesh = mApplication.mesh
//        val dataManager = DataManager(this, mesh.name, mesh.password)
        mGroupArrayList = ArrayList<DbGroup>()
        val groupList = DBUtils.getGroupList()
//        mGroupArrayList.add(dataManager.createAllLightControllerGroup()) //添加全控

        for (group in groupList) {
//            if (group.containsLightList.size > 0 || group.meshAddress == 0xFFFF)
                mGroupArrayList.add(group)
        }


        mAdapter = SelectSwitchGroupRvAdapter(R.layout.item_select_switch_group_rv, mGroupArrayList)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        recyclerView.adapter = mAdapter


    }

}

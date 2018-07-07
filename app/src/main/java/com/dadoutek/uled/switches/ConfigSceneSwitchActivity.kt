package com.dadoutek.uled.switches

import android.app.Dialog
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.MenuItem
import android.view.View
import com.blankj.utilcode.util.ActivityUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbScene
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.LightAdapter
import com.telink.bluetooth.light.Parameters
import com.telink.util.Event
import com.telink.util.EventListener
import com.telink.util.Strings
import kotlinx.android.synthetic.main.activity_switch_group.*
import kotlinx.android.synthetic.main.content_switch_group.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.design.snackbar

class ConfigSceneSwitchActivity : TelinkBaseActivity(), EventListener<String> {

    private lateinit var mDeviceInfo: DeviceInfo
    private lateinit var mApplication: TelinkLightApplication
    private lateinit var mAdapter: SwitchSceneGroupAdapter
    private lateinit var mSwitchList: ArrayList<String>
    private lateinit var mSceneList: List<DbScene>
    private var loadDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_switch_group)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.scene_set)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mApplication = getApplication() as TelinkLightApplication

        initData()
        initView()
        initListener()
    }

    private fun initListener() {
        this.mApplication.addEventListener(DeviceEvent.STATUS_CHANGED, this)

        fab.setOnClickListener { _ ->
            showLoadingDialog(getString(R.string.setting_switch))
            Thread {
                setSceneForSwitch()
                updateNameForSwitch()
            }.start()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
        }
        return super.onOptionsItemSelected(item)
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

        hideLoadingDialog()

        when (deviceInfo.status) {
            LightAdapter.STATUS_UPDATE_MESH_COMPLETED -> {
                ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
            }
            LightAdapter.STATUS_UPDATE_MESH_FAILURE -> {
                snackbar(configPirRoot, getString(R.string.pace_fail))
            }
        }
    }


    private fun setSceneForSwitch() {
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

        var keyNum: Int = 0
        val map: Map<Int, DbScene> = mAdapter.getSceneMap()
        for (key in map.keys) {
            when (key) {
                0 -> keyNum = 0x05          //左上按键
                1 -> keyNum = 0x03          //右上按键
                2 -> keyNum = 0x06          //左下按键
                3 -> keyNum = 0x04          //右下按键
            }
            val paramBytes = byteArrayOf(keyNum.toByte(), 7, 0x00, map.getValue(key).id.toByte(),
                    0x00)


            TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_SCENE_SWITCH,
                    mDeviceInfo.meshAddress,
                    paramBytes)

            Thread.sleep(200)
        }

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

        TelinkLightService.Instance().adapter.mode = LightAdapter.MODE_UPDATE_MESH
        val meshName = Strings.stringToBytes(mesh.name, 16)
        var password = Strings.stringToBytes(mesh.password, 16)
        if (SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(),
                        Constant.USER_TYPE, Constant.USER_TYPE_OLD) == Constant.USER_TYPE_NEW) {
            password = Strings.stringToBytes(NetworkFactory.md5(
                    NetworkFactory.md5(mesh?.password) + account), 16)
        } else {
            password = Strings.stringToBytes(mesh.password, 16)
        }

        TelinkLightService.Instance().adapter.mLightCtrl.reset(meshName, password, null)
    }

    private fun initView() {
        if (mSceneList.isEmpty()) {
//            ToastUtils.showLong(getString(R.string.tip_switch))
            fab.visibility = View.GONE
            indefiniteSnackbar(configPirRoot, R.string.tip_switch, R.string.btn_ok) {
                ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
            }
            return
        }
        mAdapter = SwitchSceneGroupAdapter(R.layout.item_select_switch_scene_rv, mSwitchList, mSceneList, this)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
//        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        mAdapter.bindToRecyclerView(recyclerView)
//        recyclerView.adapter = mAdapter
    }

    private fun initData() {
        mDeviceInfo = intent.getParcelableExtra("deviceInfo")
        mSwitchList = ArrayList()
        mSwitchList.add(getString(R.string.button1))
        mSwitchList.add(getString(R.string.button2))
        mSwitchList.add(getString(R.string.button3))
        mSwitchList.add(getString(R.string.button4))

        mSceneList = DBUtils.getSceneAll()
    }


}
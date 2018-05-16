package com.dadoutek.uled.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.DbModel.DbScene
import com.dadoutek.uled.DbModel.DbSceneUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.TelinkLightApplication
import com.dadoutek.uled.TelinkLightService
import com.dadoutek.uled.adapter.SelectSwitchGroupRvAdapter
import com.dadoutek.uled.adapter.SwitchSceneGroupAdapter
import com.dadoutek.uled.model.Group
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.util.DataManager
import com.dadoutek.uled.util.LogUtils
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
import org.jetbrains.anko.design.snackbar
import kotlin.collections.ArrayList

class SelectSceneForSwitchActivity : AppCompatActivity(), EventListener<String> {

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
            //            if (mAdapter.selectedPos != -1) {
//                progressBar.visibility = View.VISIBLE
            openLoadingDialog(getString(R.string.setting_switch))
            setSceneForSwitch()
//
//            }
//            else {
//                snackbar(view, getString(R.string.please_select_group))
//            }
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

        closeDialog()

        when (deviceInfo.status) {
            LightAdapter.STATUS_UPDATE_MESH_COMPLETED -> {
                progressBar.visibility = View.GONE
                ActivityUtils.finishToActivity(MainActivity::class.java, false, true)
            }
            LightAdapter.STATUS_UPDATE_MESH_FAILURE -> {
                snackbar(root, getString(R.string.pace_fail))
            }
        }
    }


    private fun setSceneForSwitch() {
        val mesh = this.mApplication.mesh
        val params = Parameters.createUpdateParameters()
        params.setOldMeshName(mesh.factoryName)
        params.setOldPassword(mesh.factoryPassword)
        params.setNewMeshName(mesh.name)
        params.setNewPassword(mesh.password)

        params.setUpdateDeviceList(mDeviceInfo)

        var keyNum: Int = 0
        val map: Map<String, DbScene> = mAdapter.getSceneMap()
        for (key in map.keys) {
            when (key) {
                getString(R.string.scene1) -> keyNum = 0x05
                getString(R.string.scene2) -> keyNum = 0x06
                getString(R.string.scene3) -> keyNum = 0x03
                getString(R.string.scene4) -> keyNum = 0x04
            }
            val paramBytes = byteArrayOf(keyNum.toByte(), 7, 0xff.toByte(), map.getValue(key).id.toByte(),
                    0xff.toByte())

            Thread.sleep(100)

            TelinkLightService.Instance().sendCommandNoResponse(Opcode.SET_SCENE_FOR_SWITCH,
                    mDeviceInfo.meshAddress,
                    paramBytes)

            Thread.sleep(100)
        }

        Thread.sleep(100)
        updateNameForSwitch()
    }

    private fun updateNameForSwitch() {
        val mesh = this.mApplication.mesh
        val params = Parameters.createUpdateParameters()
        params.setOldMeshName(mesh.factoryName)
        params.setOldPassword(mesh.factoryPassword)
        params.setNewMeshName(mesh.name)
        params.setNewPassword(mesh.password)

        params.setUpdateDeviceList(mDeviceInfo)
//        TelinkLightService.Instance().updateMesh(params)

        TelinkLightService.Instance().adapter.mode = LightAdapter.MODE_UPDATE_MESH
        val meshName = Strings.stringToBytes(mesh.name, 16)
        val password = Strings.stringToBytes(mesh.password, 16)

        TelinkLightService.Instance().adapter.mLightCtrl.reset(meshName, password, null)
    }

    private fun initView() {
        if (mSceneList.isEmpty()) {
            ToastUtils.showLong(getString(R.string.tip_switch))
            return
        }
        mAdapter = SwitchSceneGroupAdapter(R.layout.item_select_switch_scene_rv, mSwitchList, mSceneList, this)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
//        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        mAdapter.bindToRecyclerView(recyclerView)
//        recyclerView.adapter = mAdapter
    }

    private fun initData() {
        mDeviceInfo = intent.getParcelableExtra<DeviceInfo>("deviceInfo")
        mSwitchList = ArrayList<String>()
        mSwitchList.add(getString(R.string.scene1))
        mSwitchList.add(getString(R.string.scene2))
        mSwitchList.add(getString(R.string.scene3))
        mSwitchList.add(getString(R.string.scene4))

        mSceneList = DbSceneUtils.getAllScene()
    }


    @SuppressLint("ResourceType")
    fun openLoadingDialog(content: String) {
        val inflater = LayoutInflater.from(this)
        val v = inflater.inflate(R.layout.dialogview, null)

        val layout = v.findViewById<View>(R.id.dialog_view) as LinearLayout
        val tvContent = v.findViewById<View>(R.id.tvContent) as TextView
        tvContent.text = content

        val spaceshipImage = v.findViewById<View>(R.id.img) as ImageView

        @SuppressLint("ResourceType") val hyperspaceJumpAnimation = AnimationUtils.loadAnimation(this,
                R.animator.load_animation)

        spaceshipImage.startAnimation(hyperspaceJumpAnimation)

        if (loadDialog == null) {
            loadDialog = Dialog(this,
                    R.style.FullHeightDialog)
        }
        //loadDialog没显示才把它显示出来
        if (!loadDialog!!.isShowing) {
            loadDialog!!.setCancelable(true)
            loadDialog!!.setCanceledOnTouchOutside(false)
            loadDialog!!.setContentView(layout, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT))
            loadDialog!!.show()
        }
    }

    fun closeDialog() {
        if (loadDialog != null) {
            loadDialog!!.dismiss()
        }
    }
}
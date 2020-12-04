package com.dadoutek.uled.curtains

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.v7.util.DiffUtil
import android.support.v7.widget.*
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseToolbarActivity
import com.dadoutek.uled.light.DeviceScanningNewActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.Constant.*
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbCurtain
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.ItemTypeGroup
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.router.BindRouterActivity
import com.dadoutek.uled.scene.NewSceneSetAct
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.StringUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_curtains_device_details.*
import kotlinx.android.synthetic.main.activity_curtains_device_details.add_device_btn
import kotlinx.android.synthetic.main.activity_curtains_device_details.no_device_relativeLayout
import kotlinx.android.synthetic.main.activity_curtains_device_details.recycleView
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.view.*
import org.jetbrains.anko.singleLine
import kotlin.collections.ArrayList


/**
 * 窗帘列表
 */

class CurtainsDeviceDetailsActivity : TelinkBaseToolbarActivity(), View.OnClickListener {
    private var curtainDatas: MutableList<DbCurtain> = mutableListOf()
    private var adapter: CurtainDeviceDetailsAdapter? = null
    private var showList: ArrayList<ItemTypeGroup>? = arrayListOf()
    private var gpList: ArrayList<ItemTypeGroup>? = null
    private var inflater: LayoutInflater? = null
    private var currentDevice: DbCurtain? = null
    private var positionCurrent: Int = 0
    private var canBeRefresh = true
    private val REQ_LIGHT_SETTING: Int = 0x01
    private var acitivityIsAlive = true
    private var install_device: TextView? = null
    private var create_group: TextView? = null
    private var create_scene: TextView? = null
    private val SCENE_MAX_COUNT = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = this.intent.getIntExtra(DEVICE_TYPE, 0)
        inflater = this.layoutInflater
        initView()
        initData()
    }

    override fun batchGpVisible(): Boolean {
        return true
    }

    override fun setDeletePositiveBtn() {
        currentDevice?.let {
            showLoadingDialog(getString(R.string.please_wait))
            NetworkFactory.getApi().deleteCurtain(DBUtils.lastUser?.token, it.id.toInt())
                    ?.compose(NetworkTransformer())
                    ?.subscribeOn(Schedulers.io())
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.subscribe({ itr ->
                        DBUtils.deleteCurtain(it)
                        curtainDatas.remove(it)
                        adapter?.data?.remove(it)
                        adapter?.notifyDataSetChanged()
                        hideLoadingDialog()
                    }, { itt ->
                        ToastUtils.showShort(itt.message)
                    })
        }
        isEmptyDevice()
    }

    //显示路由
    override fun bindRouterVisible(): Boolean {
        return true
    }

    override fun bindDeviceRouter() {
        val dbGroup = DbGroup()
        dbGroup.brightness=10000
        dbGroup.deviceType = DeviceType.SMART_CURTAIN.toLong()
        var intent = Intent(this, BindRouterActivity::class.java)
        intent.putExtra("group", dbGroup)
        startActivity(intent)
    }

    override fun editeDeviceAdapter() {
        adapter!!.changeState(isEdite)
        adapter!!.notifyDataSetChanged()
    }

    override fun setToolbar(): Toolbar {
        return toolbar
    }

    override fun setDeviceDataSize(num: Int): Int {
        return curtainDatas.size
    }

    override fun setLayoutId(): Int {
        return R.layout.activity_curtains_device_details
    }

    override fun onResume() {
        super.onResume()
        initData()
    }

    private fun isEmptyDevice() {
        if (curtainDatas.size > 0) {
            recycleView.visibility = View.VISIBLE
            no_device_relativeLayout.visibility = View.GONE
        } else {
            recycleView.visibility = View.GONE
            no_device_relativeLayout.visibility = View.VISIBLE
        }
    }

    private fun initData() {
        gpList = DBUtils.getgroupListWithType(this)
        showList?.clear()
        curtainDatas.clear()
        gpList?.let {
            showList?.addAll(it)
        }
        setScanningMode(true)

        var allLightData = DBUtils.getAllCurtains()

        when (type) {
            INSTALL_CURTAIN -> {
                if (allLightData.size > 0) {
                    var listGroup: ArrayList<DbCurtain> = ArrayList()
                    var noGroup: ArrayList<DbCurtain> = ArrayList()
                    //判断窗帘是否有分组
                    for (i in allLightData.indices) {
                        when {
                            StringUtils.getCurtainGroupName(allLightData[i]) == TelinkLightApplication.getApp().getString(R.string.not_grouped) -> {
                                noGroup.add(allLightData[i])
                            }
                            else -> listGroup.add(allLightData[i])
                        }
                    }

                    if (noGroup.size > 0) {
                        for (i in noGroup.indices) {
                            curtainDatas.add(noGroup[i])
                        }
                    }

                    if (listGroup.size > 0) {
                        for (i in listGroup.indices) {
                            curtainDatas.add(listGroup[i])
                        }
                    }

                    toolbar!!.tv_function1.visibility = View.VISIBLE
                    recycleView.visibility = View.VISIBLE
                    no_device_relativeLayout.visibility = View.GONE
                    var batchGroup = toolbar.findViewById<TextView>(R.id.tv_function1)
                    toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.VISIBLE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.GONE
                    batchGroup.setText(R.string.batch_group)
                    batchGroup.visibility = View.GONE
                    batchGroup.setOnClickListener {
                        val intent = Intent(this, CurtainBatchGroupActivity::class.java)
                        intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                        intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
                        intent.putExtra("curtain", "all_curtain")
                        startActivity(intent)
                    }

                } else {
                    recycleView.visibility = View.GONE
                    no_device_relativeLayout.visibility = View.VISIBLE
                    toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.GONE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.GONE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).setOnClickListener {
                        if (dialog_curtain?.visibility == View.GONE)
                            showPopupMenu()
                    }
                }
            }
            INSTALL_CURTAIN_OF -> {
                if (allLightData.size > 0) {
                    var listGroup: ArrayList<DbCurtain> = ArrayList()
                    var noGroup: ArrayList<DbCurtain> = ArrayList()
                    for (i in allLightData.indices) {
                        if (StringUtils.getCurtainGroupName(allLightData[i]) == TelinkLightApplication.getApp().getString(R.string.not_grouped)) {
                            noGroup.add(allLightData[i])
                        } else {
                            listGroup.add(allLightData[i])
                        }
                    }

                    if (noGroup.size > 0) {
                        for (i in noGroup.indices) {
                            curtainDatas.add(noGroup[i])
                        }
                    }

                    if (listGroup.size > 0) {
                        for (i in listGroup.indices) {
                            curtainDatas.add(listGroup[i])
                        }
                    }
                    toolbar!!.tv_function1.visibility = View.VISIBLE
                    recycleView.visibility = View.VISIBLE
                    no_device_relativeLayout.visibility = View.GONE
                    var cwLightGroup = this.intent.getStringExtra("curtain_name")
                    var batchGroup = toolbar.findViewById<TextView>(R.id.tv_function1)
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.GONE
                    toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.VISIBLE
                    batchGroup.setText(R.string.batch_group)
                    batchGroup.setOnClickListener {
                        val intent = Intent(this, CurtainBatchGroupActivity::class.java)
                        intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                        intent.putExtra(Constant.IS_SCAN_CURTAIN, true)
                        intent.putExtra("curtain", "group_curtain")
                        intent.putExtra("curtain_group_name", cwLightGroup)
                        startActivity(intent)
                    }
                } else {
                    recycleView.visibility = View.GONE
                    no_device_relativeLayout.visibility = View.VISIBLE
                    toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.GONE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.GONE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).setOnClickListener {
                        val lastUser = DBUtils.lastUser
                        lastUser?.let {
                            if (it.id.toString() != it.last_authorizer_user_id)
                                ToastUtils.showLong(getString(R.string.author_region_warm))
                            else {
                                if (dialog_curtain?.visibility == View.GONE) {
                                    showPopupMenu()
                                }
                            }
                        }
                    }
                }
            }
        }
        toolbarTv.text = getString(R.string.curtain)

        adapter = CurtainDeviceDetailsAdapter(R.layout.template_device_type_item, curtainDatas)
        adapter!!.bindToRecyclerView(recycleView)
        adapter!!.onItemChildClickListener = onItemChildClickListener

        for (i in curtainDatas?.indices!!) {
            curtainDatas!![i].icon = R.drawable.icon_curtain
        }
    }

    private fun showPopupMenu() {
        dialog_curtain?.visibility = View.VISIBLE
    }

    private fun initView() {
        install_device = findViewById(R.id.install_device)
        create_group = findViewById(R.id.create_group)
        create_scene = findViewById(R.id.create_scene)
        install_device?.setOnClickListener(onClick)
        create_group?.setOnClickListener(onClick)
        create_scene?.setOnClickListener(onClick)

        add_device_btn.setOnClickListener(this)
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        recycleView!!.itemAnimator = DefaultItemAnimator()
        recycleView.layoutManager = GridLayoutManager(this, 2)
    }

    /**
     * 弹框添加设备
     */
    private val onClick = View.OnClickListener {
        when (it.id) {
            R.id.install_device -> {
                showInstallDeviceList()
            }
            R.id.create_group -> {
                dialog_curtain?.visibility = View.GONE
                if (TelinkLightApplication.getApp().connectDevice == null) {
                    ToastUtils.showLong(getString(R.string.device_not_connected))
                } else {
                    //addNewGroup()
                    popMain.showAtLocation(window.decorView, Gravity.CENTER, 0, 0)
                }
            }
            R.id.create_scene -> {
                dialog_curtain?.visibility = View.GONE
                val nowSize = DBUtils.sceneList.size
                if (TelinkLightApplication.getApp().connectDevice == null) {
                    ToastUtils.showLong(getString(R.string.device_not_connected))
                } else {
                    if (nowSize >= SCENE_MAX_COUNT) {
                        ToastUtils.showLong(R.string.scene_16_tip)
                    } else {
                        val intent = Intent(this, NewSceneSetAct::class.java)
                        intent.putExtra(Constant.IS_CHANGE_SCENE, false)
                        startActivity(intent)
                    }
                }
            }
        }
    }

    private fun addNewGroup() {
        val textGp = EditText(this)
        textGp.singleLine = true
        StringUtils.initEditTextFilter(textGp)
        textGp.setText(DBUtils.getDefaultNewGroupName())
        //设置光标默认在最后
        textGp.setSelection(textGp.text.toString().length)
        android.app.AlertDialog.Builder(this)
                .setTitle(R.string.create_new_group)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(textGp)
                .setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
                    // 获取输入框的内容
                    if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                        ToastUtils.showLong(getString(R.string.rename_tip_check))
                    } else {
                        //往DB里添加组数据
                        DBUtils.addNewGroupWithType(textGp.text.toString().trim { it <= ' ' }, Constant.DEVICE_TYPE_DEFAULT_ALL)
                        dialog.dismiss()
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()
    }

    private fun showInstallDeviceList() {
        dialog_curtain.visibility = View.GONE
        showInstallDeviceList(isGuide, isRgbClick)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.add_device_btn -> {
                val lastUser = DBUtils.lastUser
                lastUser?.let {
                    if (it.id.toString() != it.last_authorizer_user_id)
                        ToastUtils.showLong(getString(R.string.author_region_warm))
                    else {
                        addCurtainDevice()
                    }
                }
            }
        }
    }

    private fun addCurtainDevice() {
        intent = Intent(this, DeviceScanningNewActivity::class.java)
        intent.putExtra(Constant.DEVICE_TYPE, DeviceType.SMART_CURTAIN)
        startActivityForResult(intent, 0)
    }

    @SuppressLint("StringFormatInvalid")
    var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { _, view, position ->
        currentDevice = curtainDatas?.get(position)
        positionCurrent = position
        when {
            TelinkLightApplication.getApp().connectDevice == null&&!IS_ROUTE_MODE -> autoConnectAll()
            else -> {
                when (view.id) {
                    R.id.template_device_card_delete -> {
                        val string = getString(R.string.sure_delete_device, currentDevice?.name)
                        builder?.setMessage(string)
                        builder?.create()?.show()
                    }
                    R.id.template_device_setting -> skipSetting()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        canBeRefresh = false
        acitivityIsAlive = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        notifyData()
    }

    fun notifyData() {
        val mOldDatas: MutableList<DbCurtain>? = curtainDatas
        val mNewDatas: MutableList<DbCurtain>? = getNewData()
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return mOldDatas?.get(oldItemPosition)?.id?.equals(mNewDatas?.get
                (newItemPosition)?.id) ?: false
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
        adapter?.let {
            diffResult.dispatchUpdatesTo(it)
            adapter!!.setNewData(curtainDatas)
        }

        toolbarTv.text = getString(R.string.curtain)
    }

    private fun getNewData(): MutableList<DbCurtain> {
        curtainDatas.clear()
        curtainDatas.addAll(DBUtils.getAllCurtains())
        return curtainDatas
    }

    private fun skipSetting() {
        var intent = Intent(this@CurtainsDeviceDetailsActivity, WindowCurtainsActivity::class.java)
        intent.putExtra(TYPE_VIEW, TYPE_CURTAIN)
        intent.putExtra(LIGHT_ARESS_KEY, currentDevice)
        intent.putExtra(CURTAINS_ARESS_KEY, currentDevice!!.meshAddr)
        intent.putExtra(LIGHT_REFRESH_KEY, LIGHT_REFRESH_KEY_OK)
        Log.d("currentLight", currentDevice!!.meshAddr.toString())
        startActivityForResult(intent, REQ_LIGHT_SETTING)
    }
}

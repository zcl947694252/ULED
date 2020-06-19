package com.dadoutek.uled.curtains

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.util.DiffUtil
import android.support.v7.widget.*
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.group.InstallDeviceListAdapter
import com.dadoutek.uled.light.DeviceScanningNewActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.Constant.*
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbCurtain
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.InstallDeviceModel
import com.dadoutek.uled.model.ItemTypeGroup
import com.dadoutek.uled.pir.ScanningSensorActivity
import com.dadoutek.uled.scene.NewSceneSetAct
import com.dadoutek.uled.switches.ScanningSwitchActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.OtherUtils
import com.dadoutek.uled.util.StringUtils
import com.telink.util.MeshUtils.DEVICE_ADDRESS_MAX
import kotlinx.android.synthetic.main.activity_curtains_device_details.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.view.*


/**
 * 窗帘列表
 */

class CurtainsDeviceDetailsActivity : TelinkBaseActivity(), View.OnClickListener {
    private lateinit var curtain: MutableList<DbCurtain>
    private var adapter: CurtainDeviceDetailsAdapter? = null
    private var showList: List<ItemTypeGroup>? = null
    private var gpList: List<ItemTypeGroup>? = null
    private var type: Int? = null
    private var inflater: LayoutInflater? = null
    private var currentLight: DbCurtain? = null
    private var positionCurrent: Int = 0
    private var canBeRefresh = true
    private val REQ_LIGHT_SETTING: Int = 0x01
    private var acitivityIsAlive = true
    private var mApplication: TelinkLightApplication? = null
    private var install_device: TextView? = null
    private var create_group: TextView? = null
    private var create_scene: TextView? = null
    private var isRgbClick = false
    private var installId = 0
    private lateinit var stepOneText: TextView
    private lateinit var stepTwoText: TextView
    private lateinit var stepThreeText: TextView
    private lateinit var switchStepOne: TextView
    private lateinit var switchStepTwo: TextView
    private lateinit var swicthStepThree: TextView
    private lateinit var stepThreeTextSmall: TextView
    var installDialog: android.app.AlertDialog? = null
    var isGuide: Boolean = false
    var clickRgb: Boolean = false
    private val SCENE_MAX_COUNT = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_curtains_device_details)
        type = this.intent.getIntExtra(Constant.DEVICE_TYPE, 0)
        inflater = this.layoutInflater
        initView()
        initData()
    }

    override fun onResume() {
        super.onResume()
        initData()
    }

    private fun initData() {
        gpList = DBUtils.getgroupListWithType(this)
        showList = ArrayList()
        showList = gpList
        setScanningMode(true)

        curtain = ArrayList()
        var allLightData = DBUtils.getAllCurtains()

        when (type) {
            Constant.INSTALL_CURTAIN -> {
                if (allLightData.size > 0) {
                    var listGroup: ArrayList<DbCurtain> = ArrayList()
                    var noGroup: ArrayList<DbCurtain> = ArrayList()
                    //判断窗帘是否有分组
                    for (i in allLightData.indices) {
                        if (StringUtils.getCurtainName(allLightData[i]) == TelinkLightApplication.getApp().getString(R.string.not_grouped)) {
                            noGroup.add(allLightData[i])
                        } else {
                            listGroup.add(allLightData[i])
                        }
                    }

                    if (noGroup.size > 0) {
                        for (i in noGroup.indices) {
                            curtain.add(noGroup[i])
                        }
                    }

                    if (listGroup.size > 0) {
                        for (i in listGroup.indices) {
                            curtain.add(listGroup[i])
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
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
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
                        if (StringUtils.getCurtainName(allLightData[i]) == TelinkLightApplication.getApp().getString(R.string.not_grouped)) {
                            noGroup.add(allLightData[i])
                        } else {
                            listGroup.add(allLightData[i])
                        }
                    }

                    if (noGroup.size > 0) {
                        for (i in noGroup.indices) {
                            curtain.add(noGroup[i])
                        }
                    }

                    if (listGroup.size > 0) {
                        for (i in listGroup.indices) {
                            curtain.add(listGroup[i])
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
                        val intent = Intent(this,
                                CurtainBatchGroupActivity::class.java)
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
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
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
        toolbar.title = getString(R.string.curtain) + " (" + curtain.size + ")"

        adapter = CurtainDeviceDetailsAdapter(R.layout.template_device_type_item, curtain)
        adapter!!.bindToRecyclerView(recycleView)
        adapter!!.onItemChildClickListener = onItemChildClickListener

        for (i in curtain?.indices!!) {
            curtain!![i].icon = R.drawable.icon_curtain
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
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
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
                    popMain.showAtLocation(window.decorView, Gravity.CENTER,0,0)
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

    private fun showInstallDeviceList(isGuide: Boolean, clickRgb: Boolean) {
        this.clickRgb = clickRgb
        val view = View.inflate(this, R.layout.dialog_install_list, null)
        val closeInstallList = view.findViewById<ImageView>(R.id.close_install_list)
        val installDeviceRecyclerview = view.findViewById<RecyclerView>(R.id.install_device_recyclerView)
        closeInstallList.setOnClickListener { v -> installDialog?.dismiss() }

        val installList: java.util.ArrayList<InstallDeviceModel> = OtherUtils.getInstallDeviceList(this)

        val installDeviceListAdapter = InstallDeviceListAdapter(R.layout.item_install_device, installList)
        val layoutManager = LinearLayoutManager(this)
        installDeviceRecyclerview?.layoutManager = layoutManager
        installDeviceRecyclerview?.adapter = installDeviceListAdapter
        installDeviceListAdapter.bindToRecyclerView(installDeviceRecyclerview)
        val decoration = DividerItemDecoration(this,
                DividerItemDecoration
                        .VERTICAL)
        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color
                .divider)))
        //添加分割线
        installDeviceRecyclerview?.addItemDecoration(decoration)

        installDeviceListAdapter.onItemClickListener = onItemClickListenerInstallList

        installDialog = android.app.AlertDialog.Builder(this)
                .setView(view)
                .create()

        installDialog?.setOnShowListener {
        }

        if (isGuide) {
            installDialog?.setCancelable(false)
        }

        installDialog?.show()
    }

    private val onItemClickListenerInstallList = BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
        isGuide = false
        installDialog?.dismiss()
        when (position) {
            INSTALL_GATEWAY -> {
                installId = INSTALL_GATEWAY
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
            }
            INSTALL_NORMAL_LIGHT -> {
                installId = INSTALL_NORMAL_LIGHT
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this),position)
            }
            INSTALL_RGB_LIGHT -> {
                installId = INSTALL_RGB_LIGHT
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
            }
            INSTALL_CURTAIN -> {
                installId = INSTALL_CURTAIN
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
            }
            INSTALL_SWITCH -> {
                installId = INSTALL_SWITCH
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), position)
                stepOneText.visibility = View.GONE
                stepTwoText.visibility = View.GONE
                stepThreeText.visibility = View.GONE
                switchStepOne.visibility = View.VISIBLE
                switchStepTwo.visibility = View.VISIBLE
                swicthStepThree.visibility = View.VISIBLE
            }
            INSTALL_SENSOR -> {
                installId = INSTALL_SENSOR

                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this),position)
            }
            INSTALL_CONNECTOR -> {
                installId = INSTALL_CONNECTOR
                showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this),position)

            }
        }
    }

    private fun showInstallDeviceDetail(describe: String, position: Int) {
        val view = View.inflate(this, R.layout.dialog_install_detail, null)
        val closeInstallList = view.findViewById<ImageView>(R.id.close_install_list)
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        stepOneText = view.findViewById<TextView>(R.id.step_one)
        stepTwoText = view.findViewById<TextView>(R.id.step_two)
        stepThreeText = view.findViewById<TextView>(R.id.step_three)
        stepThreeTextSmall = view.findViewById(R.id.step_three_small)
        switchStepOne = view.findViewById<TextView>(R.id.switch_step_one)
        switchStepTwo = view.findViewById<TextView>(R.id.switch_step_two)
        swicthStepThree = view.findViewById<TextView>(R.id.switch_step_three)
        val installTipQuestion = view.findViewById<TextView>(R.id.install_tip_question)
        val searchBar = view.findViewById<Button>(R.id.search_bar)
        closeInstallList.setOnClickListener(dialogOnclick)
        btnBack.setOnClickListener(dialogOnclick)
        searchBar.setOnClickListener(dialogOnclick)
        val title = view.findViewById<TextView>(R.id.textView5)
        if (position==INSTALL_NORMAL_LIGHT){
            title.visibility =  View.GONE
            installTipQuestion.visibility =  View.GONE
        }else{
            title.visibility =  View.VISIBLE
            installTipQuestion.visibility =  View.VISIBLE
        }

        installTipQuestion.text = describe
        installTipQuestion.movementMethod = ScrollingMovementMethod.getInstance()
        installDialog = android.app.AlertDialog.Builder(this).setView(view).create()
        installDialog?.setOnShowListener {}
        installDialog?.show()
    }

    private val dialogOnclick = View.OnClickListener {
        var medressData = 0
        var allData = DBUtils.allLight
        var sizeData = DBUtils.allLight.size
        if (sizeData != 0) {
            var lightData = allData[sizeData - 1]
            medressData = lightData.meshAddr
        }

        when (it.id) {
            R.id.close_install_list -> {
                installDialog?.dismiss()
            }
            R.id.search_bar -> {//蓝牙搜索
                when (installId) {
                    INSTALL_NORMAL_LIGHT -> {
                        if (medressData <= DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.LIGHT_NORMAL)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_RGB_LIGHT -> {
                        if (medressData <= DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.LIGHT_RGB)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_CURTAIN -> {
                        if (medressData <= DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.SMART_CURTAIN)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                    INSTALL_SWITCH -> {
                        //intent = Intent(this, DeviceScanningNewActivity::class.java)
                        //intent.putExtra(Constant.DEVICE_TYPE, DeviceType.NORMAL_SWITCH)
                        //startActivityForResult(intent, 0)
                        startActivity(Intent(this, ScanningSwitchActivity::class.java))
                    }
                    INSTALL_SENSOR -> startActivity(Intent(this, ScanningSensorActivity::class.java))
                    INSTALL_CONNECTOR -> {
                        if (medressData <= DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.SMART_CURTAIN)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                   INSTALL_GATEWAY -> {
                        if (medressData <= DEVICE_ADDRESS_MAX) {
                            intent = Intent(this, DeviceScanningNewActivity::class.java)
                            intent.putExtra(Constant.DEVICE_TYPE, DeviceType.GATE_WAY)
                            startActivityForResult(intent, 0)
                        } else {
                            ToastUtils.showLong(getString(R.string.much_lamp_tip))
                        }
                    }
                }
            }
            R.id.btnBack -> {
                installDialog?.dismiss()
                showInstallDeviceList(isGuide, clickRgb)
            }
        }
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

    var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { _, _, position ->
        currentLight = curtain?.get(position)
        positionCurrent = position
                if (TelinkLightApplication.getApp().connectDevice == null)
                    ToastUtils.showLong(getString(R.string.connecting_tip))
                else
                    skipSetting()

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
        val mOldDatas: MutableList<DbCurtain>? = curtain
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
            adapter!!.setNewData(curtain)
        }

        toolbar.title = getString(R.string.curtain) + " (" + curtain.size + ")"
    }

    private fun getNewData(): MutableList<DbCurtain> {
        curtain = DBUtils.getAllCurtains()
        return curtain
    }

    private fun skipSetting() {
        var intent = Intent(this@CurtainsDeviceDetailsActivity, WindowCurtainsActivity::class.java)
        intent.putExtra(TYPE_VIEW, TYPE_CURTAIN)
        intent.putExtra(LIGHT_ARESS_KEY, currentLight)
        intent.putExtra(CURTAINS_ARESS_KEY, currentLight!!.meshAddr)
        intent.putExtra(LIGHT_REFRESH_KEY, LIGHT_REFRESH_KEY_OK)
        Log.d("currentLight", currentLight!!.meshAddr.toString())
        startActivityForResult(intent, REQ_LIGHT_SETTING)
    }
}

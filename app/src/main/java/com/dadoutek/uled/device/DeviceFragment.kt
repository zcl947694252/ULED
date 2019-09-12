package com.dadoutek.uled.device

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.GridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.app.hubert.guide.core.Controller
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.connector.ConnectorDeviceDetailActivity
import com.dadoutek.uled.curtains.CurtainsDeviceDetailsActivity
import com.dadoutek.uled.device.model.DeviceItem
import com.dadoutek.uled.intf.CallbackLinkMainActAndFragment
import com.dadoutek.uled.light.DeviceDetailAct
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.othersview.BaseFragment
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.scene.NewSceneSetAct
import com.dadoutek.uled.scene.SensorDeviceDetailsActivity
import com.dadoutek.uled.switches.SwitchDeviceDetailsActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.GuideUtils
import com.dadoutek.uled.util.StringUtils
import kotlinx.android.synthetic.main.fragment_new_device.*
import kotlinx.android.synthetic.main.popwindow_install_deive_list.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

/**
 * 创建者     zcl
 * 创建时间   2019/7/27 11:31
 * 描述	      ${设备列表-灯暖灯全彩灯列表}$
 *
 * 更新者     $Author$
 * 更新时间   $Date$
 *
 */
class DeviceFragment : BaseFragment() {
    private var deviceTypeList: ArrayList<DeviceItem> = ArrayList()
    private var deviceAdapter: DeviceTypeRecycleViewAdapter = DeviceTypeRecycleViewAdapter(R.layout.device_type_item, deviceTypeList)
    private var isGuide = false
    private var isRgbClick = false
    var firstShowGuide = true
    private var guideShowCurrentPage = false
    private val SCENE_MAX_COUNT = 16

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return initLayout(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolBar(view)
        initData()
        initView()
        if (firstShowGuide) {
            firstShowGuide = false
            initOnLayoutListener()
        }
    }



    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser);
        if (userVisibleHint) {
//            val act = activity as MainActivity?
//            act?.addEventListeners()
            initOnLayoutListener()
            refreshView()
//            refreshView()
        }
    }

    private fun initOnLayoutListener() {
        val view = activity?.window?.decorView
        val viewTreeObserver = view?.viewTreeObserver
        viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                lazyLoad()
            }
        })
    }

    fun lazyLoad() {
        guideShowCurrentPage = !GuideUtils.getCurrentViewIsEnd(activity!!, GuideUtils.END_GROUPLIST_KEY, false)
        if (guideShowCurrentPage) {
            GuideUtils.resetGroupListGuide(activity!!)
            val guide0 = toolbar!!.findViewById<TextView>(R.id.toolbarTv)
            GuideUtils.guideBuilder(this@DeviceFragment, GuideUtils.STEP0_GUIDE_SELECT_DEVICE_KEY)
                    .addGuidePage(GuideUtils.addGuidePage(guide0, R.layout.view_guide_0, getString(R.string.group_list_guide0), View.OnClickListener {}, GuideUtils.END_GROUPLIST_KEY, activity!!)
                            .setOnLayoutInflatedListener { view, controller ->
                                val normal = view.findViewById<TextView>(R.id.normal_light)
                                normal.setOnClickListener {
                                    controller.remove()
                                    guide1()
                                    isRgbClick = false
                                }
                                val rgb = view.findViewById<TextView>(R.id.rgb_light)
                                rgb.setOnClickListener {
                                    controller.remove()
                                    guide1()
                                    isRgbClick = true
                                }
                                val tvJump = view.findViewById<TextView>(R.id.jump_out)
                                tvJump.setOnClickListener { v ->
                                    GuideUtils.showExitGuideDialog(activity!!, controller, GuideUtils.END_GROUPLIST_KEY)
                                }
                            })
                    .show()
        }
    }

    private fun guide1() {
        guideShowCurrentPage = !GuideUtils.getCurrentViewIsEnd(activity!!, GuideUtils.END_GROUPLIST_KEY, false)
        if (guideShowCurrentPage) {
            val guide1 = toolbar!!.findViewById<ImageView>(R.id.img_function1)

            GuideUtils.guideBuilder(this@DeviceFragment, GuideUtils.STEP1_GUIDE_ADD_DEVICE_KEY)
                    .addGuidePage(GuideUtils.addGuidePage(guide1, R.layout.view_guide_simple_group1, getString(R.string.group_list_guide1), View.OnClickListener {
                        isGuide = true
                        showPopupMenu()
                        guide2()
                    }, GuideUtils.END_GROUPLIST_KEY, activity!!))
                    .show()
        }
    }

    private fun guide2(): Controller? {
        guideShowCurrentPage = !GuideUtils.getCurrentViewIsEnd(activity!!, GuideUtils.END_GROUPLIST_KEY, false)
        if (guideShowCurrentPage) {
            var guide3: TextView? = install_device

            return GuideUtils.guideBuilder(this@DeviceFragment, GuideUtils.STEP2_GUIDE_START_INSTALL_DEVICE)
                    .addGuidePage(GuideUtils.run {
                        addGuidePage(guide3!!, R.layout.view_guide_simple_group2, getString(R.string.group_list_guide2), View.OnClickListener { install_device?.performClick() }, END_GROUPLIST_KEY, activity!!)
                    })
                    .show()
        }


        return null
    }

    private fun initToolBar(view: View?) {
        toolbar!!.setTitle(R.string.device_list)

        toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
        toolbar!!.findViewById<ImageView>(R.id.img_function2).visibility = View.GONE
        toolbar!!.findViewById<ImageView>(R.id.img_function1).setOnClickListener {
            isGuide = false
            if (dialog_pop?.visibility == View.GONE) {
                showPopupMenu()
            } else {
                hidePopupMenu()
            }
        }
    }



    private fun initView() {
        rvDevice?.layoutManager = GridLayoutManager(this.activity, 2)
        rvDevice?.itemAnimator = DefaultItemAnimator()

        deviceAdapter.onItemClickListener = onItemClickListener
        deviceAdapter.bindToRecyclerView(rvDevice)

        install_device?.setOnClickListener(onClick)
        create_group?.setOnClickListener(onClick)
        create_scene?.setOnClickListener(onClick)
    }


    /**
     * 刷新UI
     */
    fun refreshView() {
        if (activity != null) {
            initAdapterData()
            deviceAdapter.notifyDataSetChanged()
        }
    }


    var onItemClickListener = BaseQuickAdapter.OnItemClickListener { _, view, position ->
        var intent: Intent? = null
        when (position) {
            Constant.INSTALL_NORMAL_LIGHT -> {//跳转冷暖灯
                intent = Intent(activity, DeviceDetailAct::class.java)
                intent.putExtra(Constant.DEVICE_TYPE, Constant.INSTALL_NORMAL_LIGHT)
            }
            Constant.INSTALL_RGB_LIGHT -> {//跳转多彩灯
                intent = Intent(activity, DeviceDetailAct::class.java)
                intent.putExtra(Constant.DEVICE_TYPE, Constant.INSTALL_RGB_LIGHT)
            }
            Constant.INSTALL_SWITCH -> {
                intent = Intent(activity, SwitchDeviceDetailsActivity::class.java)
                intent.putExtra(Constant.DEVICE_TYPE, Constant.INSTALL_SWITCH)
            }
            Constant.INSTALL_SENSOR -> {
                intent = Intent(activity, SensorDeviceDetailsActivity::class.java)
                intent.putExtra(Constant.DEVICE_TYPE, Constant.INSTALL_SENSOR)
            }
            Constant.INSTALL_CURTAIN -> {
                intent = Intent(activity, CurtainsDeviceDetailsActivity::class.java)
                intent.putExtra(Constant.DEVICE_TYPE, Constant.INSTALL_CURTAIN)
            }
            Constant.INSTALL_CONNECTOR -> {
                intent = Intent(activity, ConnectorDeviceDetailActivity::class.java)
                intent.putExtra(Constant.DEVICE_TYPE, Constant.INSTALL_CONNECTOR)
            }
        }
        startActivityForResult(intent, Activity.RESULT_OK)
    }


    /**
     * 初始化RecyclerView的Adapter
     */
    private fun initAdapterData() {
        deviceTypeList.clear()
        deviceTypeList.add(DeviceItem(getString(R.string.normal_light),
                DBUtils.getAllNormalLight().size,
                DeviceType.LIGHT_NORMAL))

        deviceTypeList.add(DeviceItem(getString(R.string.rgb_light),
                DBUtils.getAllRGBLight().size,
                DeviceType.LIGHT_RGB))


        deviceTypeList.add(DeviceItem(getString(R.string.switch_name),
                DBUtils.getAllSwitch().size,
                DeviceType.NORMAL_SWITCH))


        deviceTypeList.add(DeviceItem(getString(R.string.sensor),
                DBUtils.getAllSensor().size,
                DeviceType.SENSOR))

        deviceTypeList.add(DeviceItem(getString(R.string.curtain),
                DBUtils.getAllCurtains().size,
                DeviceType.SMART_CURTAIN))

        deviceTypeList.add(DeviceItem(getString(R.string.relay),
                DBUtils.getAllRelay().size,
                DeviceType.SMART_RELAY))


    }


    /**
     * 初始化所有数据
     */
    private fun initData() {
        initAdapterData()
    }


    /**
     * 初始化布局
     */
    private fun initLayout(inflater: LayoutInflater): View? {
        val view = inflater.inflate(R.layout.fragment_new_device, null)
        return view
    }

    private val onClick = View.OnClickListener {
        hidePopupMenu()
        when (it.id) {
            R.id.install_device -> {
                showInstallDeviceList()
            }
            R.id.create_group -> {
                if (TelinkLightApplication.getApp().connectDevice == null) {
                    ToastUtils.showLong(activity!!.getString(R.string.device_not_connected))
                } else {
                    addNewGroup()
                }
            }
            R.id.create_scene -> {
                val nowSize = DBUtils.sceneList.size
                if (TelinkLightApplication.getApp().connectDevice == null) {
                    ToastUtils.showLong(activity!!.getString(R.string.device_not_connected))
                } else {
                    if (nowSize >= SCENE_MAX_COUNT) {
                        ToastUtils.showLong(R.string.scene_16_tip)
                    } else {
                        val intent = Intent(activity, NewSceneSetAct::class.java)
                        intent.putExtra(Constant.IS_CHANGE_SCENE, false)
                        startActivityForResult(intent, CREATE_SCENE_REQUESTCODE)
                    }
                }
            }
        }
    }

    private fun addNewGroup() {
        val textGp = EditText(activity)
        StringUtils.initEditTextFilter(textGp)
        textGp.setText(DBUtils.getDefaultNewGroupName())
        //设置光标默认在最后
        textGp.setSelection(textGp.getText().toString().length)
        AlertDialog.Builder(activity)
                .setTitle(R.string.create_new_group)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(textGp)

                .setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
                    // 获取输入框的内容
                    if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                        ToastUtils.showShort(getString(R.string.rename_tip_check))
                    } else {
                        //往DB里添加组数据
                        DBUtils.addNewGroupWithType(textGp.text.toString().trim { it <= ' ' }, Constant.DEVICE_TYPE_DEFAULT_ALL)
                        callbackLinkMainActAndFragment?.changeToGroup()
                        dialog.dismiss()
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                val inputManager = textGp.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputManager.showSoftInput(textGp, 0)
            }
        }, 200)
    }

    fun myPopViewClickPosition(x: Float, y: Float) {
        if (x < dialog_pop?.left ?: 0 || y < dialog_pop?.top ?: 0 || y > dialog_pop?.bottom ?: 0) {
            if (dialog_pop?.visibility == View.VISIBLE) {
                Thread {
                    //避免点击过快点击到下层View
                    Thread.sleep(100)
                    GlobalScope.launch(Dispatchers.Main) {
                        hidePopupMenu()
                    }
                }.start()
            } else if (dialog_pop == null) {
                hidePopupMenu()
            }
        }
    }

    private fun showPopupMenu() {
        dialog_pop?.visibility = View.VISIBLE
    }

    private fun hidePopupMenu() {
        if (!isGuide || GuideUtils.getCurrentViewIsEnd(activity!!, GuideUtils.END_GROUPLIST_KEY, false)) {
            dialog_pop?.visibility = View.GONE
        }
    }

    val CREATE_SCENE_REQUESTCODE = 2
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
//        refreshData()
        if (requestCode == CREATE_SCENE_REQUESTCODE) {
            callbackLinkMainActAndFragment?.changeToScene()
        }
    }

    var callbackLinkMainActAndFragment: CallbackLinkMainActAndFragment? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is CallbackLinkMainActAndFragment) {
            callbackLinkMainActAndFragment = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        callbackLinkMainActAndFragment = null

    }

    private fun showInstallDeviceList() {
        dialog_pop.visibility = View.GONE
        callbackLinkMainActAndFragment?.showDeviceListDialog(isGuide, isRgbClick)
    }
}
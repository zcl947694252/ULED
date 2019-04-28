package com.dadoutek.uled.device

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.app.hubert.guide.core.Controller
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.connector.ConnectorDeviceDetailActivity
import com.dadoutek.uled.windowcurtains.CurtainsDeviceDetailsActivity
import com.dadoutek.uled.intf.CallbackLinkMainActAndFragment
import com.dadoutek.uled.light.DeviceDetailAct
//import com.dadoutek.uled.light.DeviceDetailAct
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.InstallDeviceModel
import com.dadoutek.uled.othersview.BaseFragment
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.scene.NewSceneSetAct
import com.dadoutek.uled.scene.SensorDeviceDetailsActivity
import com.dadoutek.uled.switches.SwitchDeviceDetailsActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.GuideUtils
import com.dadoutek.uled.util.OtherUtils
import com.dadoutek.uled.util.SpaceItemDecoration
import com.dadoutek.uled.util.StringUtils
import kotlinx.android.synthetic.main.fragment_new_device.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.ArrayList

class NewDevieFragment :BaseFragment(){

    private var inflater: LayoutInflater? = null
    var recyclerView:RecyclerView? = null
    var newDeviceAdapter:DeviceTypeRecycleViewAdapter? = null

    private var deviceTypeList : ArrayList<String>? = null
    private var allDeviceList : ArrayList<DbLight>? = null
    private var isGuide = false
    private var toolbar: Toolbar? = null
    private var isRgbClick = false
    var firstShowGuide = true
    private var guideShowCurrentPage = false
    private val SCENE_MAX_COUNT = 16

    //19-2-20 界面调整
    private var install_device: TextView? = null
    private var create_group: TextView? = null
    private var create_scene: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view=getViewThis(inflater)
        initToolBar(view)
        initData()
        initView(view)

        if (firstShowGuide) {
            firstShowGuide = false
            initOnLayoutListener()
        }

        return view
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        if (isVisibleToUser) {
            val act = activity as MainActivity?
            act?.addEventListeners()
            initOnLayoutListener()
        }
    }

    private fun initOnLayoutListener() {
        val view = activity?.getWindow()?.getDecorView()
        val viewTreeObserver = view?.getViewTreeObserver()
        viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this)
                lazyLoad()
            }
        })
    }

    fun lazyLoad() {
        guideShowCurrentPage = !GuideUtils.getCurrentViewIsEnd(activity!!, GuideUtils.END_GROUPLIST_KEY, false)
        if (guideShowCurrentPage) {
            GuideUtils.resetGroupListGuide(activity!!)
            val guide0 = toolbar!!.findViewById<TextView>(R.id.toolbarTv)
            GuideUtils.guideBuilder(this@NewDevieFragment, GuideUtils.STEP0_GUIDE_SELECT_DEVICE_KEY)
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

            GuideUtils.guideBuilder(this@NewDevieFragment, GuideUtils.STEP1_GUIDE_ADD_DEVICE_KEY)
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
            var guide3: TextView? = null
            guide3 = install_device

            return GuideUtils.guideBuilder(this@NewDevieFragment, GuideUtils.STEP2_GUIDE_START_INSTALL_DEVICE)
                    .addGuidePage(GuideUtils.addGuidePage(guide3!!, R.layout.view_guide_simple_group2, getString(R.string.group_list_guide2), View.OnClickListener {
                        install_device?.performClick()
                    }, GuideUtils.END_GROUPLIST_KEY, activity!!))
                    .show()
        }
        return null
    }

    private fun initToolBar(view: View?) {
        toolbar = view?.findViewById(R.id.toolbar)
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

    private fun initView(view: View?) {
        val layoutmanager = LinearLayoutManager(activity)
//        layoutmanager.orientation = LinearLayoutManager.VERTICAL
        recyclerView!!.layoutManager = GridLayoutManager(this.activity,2)
        newDeviceAdapter = DeviceTypeRecycleViewAdapter(R.layout.device_type_item,deviceTypeList!!)

//        val decoration = DividerItemDecoration(activity!!,
//                DividerItemDecoration
//                        .VERTICAL)
//        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(activity!!, R.color
//                .divider)))
//        //添加分割线
//        recyclerView?.addItemDecoration(decoration)
        recyclerView?.addItemDecoration(SpaceItemDecoration(40))
        recyclerView?.itemAnimator = DefaultItemAnimator()

        newDeviceAdapter!!.setOnItemClickListener(onItemClickListener)
//        adapter!!.addFooterView(getFooterView())
        newDeviceAdapter!!.bindToRecyclerView(recyclerView)


        install_device = view?.findViewById(R.id.install_device)
        create_group = view?.findViewById(R.id.create_group)
        create_scene = view?.findViewById(R.id.create_scene)
        install_device?.setOnClickListener(onClick)
        create_group?.setOnClickListener(onClick)
        create_scene?.setOnClickListener(onClick)
    }

//    allDeviceList!!.add(DBUtils.getAllNormalLight())
//    allDeviceList!!.add(DBUtils.getAllRGBLight())
//    allDeviceList!!.add(DBUtils.getAllSwitch())
//    allDeviceList!!.add(DBUtils.getAllSensor())
//    allDeviceList!!.add(DBUtils.getAllCurtain())

    var onItemClickListener = BaseQuickAdapter.OnItemClickListener {
        adapter, view, position ->
        if(TelinkLightApplication.getInstance().connectDevice==null){
            ToastUtils.showLong(R.string.device_not_connected)
        }else{
            var intent:Intent?=null
            when(position){
                Constant.INSTALL_NORMAL_LIGHT ->{
                    intent= Intent(activity, DeviceDetailAct::class.java)
                    intent.putExtra(Constant.DEVICE_TYPE,Constant.INSTALL_NORMAL_LIGHT)
                }
                Constant.INSTALL_RGB_LIGHT ->{
                    intent= Intent(activity, DeviceDetailAct::class.java)
                    intent.putExtra(Constant.DEVICE_TYPE,Constant.INSTALL_RGB_LIGHT)
                }
                Constant.INSTALL_SWITCH ->{
                    intent= Intent(activity,SwitchDeviceDetailsActivity::class.java)
                    intent.putExtra(Constant.DEVICE_TYPE,Constant.INSTALL_SWITCH)
                }
                Constant.INSTALL_SENSOR ->{
                    intent= Intent(activity,SensorDeviceDetailsActivity::class.java)
                    intent.putExtra(Constant.DEVICE_TYPE,Constant.INSTALL_SENSOR)
                }
                Constant.INSTALL_CURTAIN ->{
                    intent= Intent(activity,CurtainsDeviceDetailsActivity::class.java)
                    intent.putExtra(Constant.DEVICE_TYPE,Constant.INSTALL_CURTAIN)
                }
                Constant.INSTALL_CONNECTOR->{
                    intent= Intent(activity,ConnectorDeviceDetailActivity::class.java)
                    intent.putExtra(Constant.DEVICE_TYPE,Constant.INSTALL_CURTAIN)
                }
            }
            startActivityForResult(intent, Activity.RESULT_OK)
        }
    }

    private fun initData() {
        deviceTypeList = ArrayList<String>()
        val installList: ArrayList<InstallDeviceModel> = OtherUtils.getInstallDeviceList(activity)
        for(installDeviceModel in installList){
            deviceTypeList!!.add(installDeviceModel.deviceType)
        }

        allDeviceList = ArrayList()
    }

    private fun getViewThis(inflater: LayoutInflater): View?{
        this.inflater=inflater
        val view = inflater.inflate(R.layout.fragment_new_device,null)
        recyclerView = view.findViewById<RecyclerView>(R.id.deviceTypeList)
        return view
    }

    private val onClick = View.OnClickListener {
        var intent: Intent? = null
        //点击任何一个选项跳转页面都隐藏引导
//        val controller=guide2()
//            controller?.remove()
        hidePopupMenu()
        when (it.id) {
            R.id.install_device -> {
                showInstallDeviceList()
            }
            R.id.create_group -> {
                if (TelinkLightApplication.getInstance().connectDevice==null) {
                    ToastUtils.showLong(activity!!.getString(R.string.device_not_connected))
                } else {
                    addNewGroup()
                }
            }
            R.id.create_scene -> {
                val nowSize = DBUtils.sceneList.size
                if (TelinkLightApplication.getInstance().connectDevice==null) {
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
                        DBUtils.addNewGroupWithType(textGp.text.toString().trim { it <= ' ' }, DBUtils.groupList, Constant.DEVICE_TYPE_DEFAULT_ALL,activity!!)
                        callbackLinkMainActAndFragment?.changeToGroup()
                        dialog.dismiss()
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()
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
        if(requestCode==CREATE_SCENE_REQUESTCODE){
            callbackLinkMainActAndFragment?.changeToScene()
        }
    }

    var callbackLinkMainActAndFragment: CallbackLinkMainActAndFragment?=null

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if(context is CallbackLinkMainActAndFragment){
            callbackLinkMainActAndFragment = context as CallbackLinkMainActAndFragment
        }
    }

    override fun onDetach() {
        super.onDetach()
        callbackLinkMainActAndFragment = null
    }

    private fun showInstallDeviceList() {
        dialog_pop.visibility=View.GONE
        callbackLinkMainActAndFragment?.showDeviceListDialog(isGuide,isRgbClick)
    }
}
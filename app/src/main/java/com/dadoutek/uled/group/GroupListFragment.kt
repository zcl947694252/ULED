package com.dadoutek.uled.group

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.view.ViewPager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.connector.ConnectorOfGroupActivity
import com.dadoutek.uled.connector.ConnectorSettingActivity
import com.dadoutek.uled.curtain.CurtainOfGroupActivity
import com.dadoutek.uled.fragment.CWLightFragmentList
import com.dadoutek.uled.fragment.CurtainFragmentList
import com.dadoutek.uled.fragment.RGBLightFragmentList
import com.dadoutek.uled.fragment.RelayFragmentList
import com.dadoutek.uled.intf.CallbackLinkMainActAndFragment
import com.dadoutek.uled.intf.MyBaseQuickAdapterOnClickListner
import com.dadoutek.uled.intf.OnRecyclerviewItemClickListener
import com.dadoutek.uled.light.LightsOfGroupActivity
import com.dadoutek.uled.light.NormalSettingActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbDeviceName
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.ItemTypeGroup
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.othersview.BaseFragment
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.othersview.ViewPagerAdapter
import com.dadoutek.uled.rgb.RGBSettingActivity
import com.dadoutek.uled.scene.NewSceneSetAct
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.*
import com.dadoutek.uled.windowcurtains.WindowCurtainsActivity
import com.telink.bluetooth.light.ConnectionStatus
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_group_list.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class GroupListFragment : BaseFragment() {

    private var inflater: LayoutInflater? = null
    private var adapter: GroupListRecycleViewAdapter? = null
    private var mContext: Activity? = null
    private var mApplication: TelinkLightApplication? = null
    private var dataManager: DataManager? = null
    private var gpList: List<ItemTypeGroup>? = null
    private var application: TelinkLightApplication? = null
    private var toolbar: Toolbar? = null
    //    private var recyclerView: RecyclerView? = null
    internal var showList: List<ItemTypeGroup>? = null
    private var updateLightDisposal: Disposable? = null
    private val SCENE_MAX_COUNT = 16
    private var deviceRecyclerView: RecyclerView? = null

    private var viewPager: ViewPager? = null

    internal var deviceName: ArrayList<DbDeviceName>? = null

    private lateinit var cwLightFragment: CWLightFragmentList
    private lateinit var rgbLightFragment: RGBLightFragmentList
    private lateinit var curtianFragment: CurtainFragmentList
    private lateinit var relayFragment: RelayFragmentList

    //当前所选组index
    private var currentGroupIndex = -1

    //19-2-20 界面调整
    private var install_device: TextView? = null
    private var create_group: TextView? = null
    private var create_scene: TextView? = null

    private var sharedPreferences: SharedPreferences? = null

    //新用户选择的初始安装选项是否是RGB灯
    private var isRgbClick = false
    //是否正在引导
    private var isGuide = false
    var firstShowGuide = true
    private var isFristUserClickCheckConnect = true
    private var guideShowCurrentPage = false

    internal var lastPosition: Int = 0

    internal var lastOffset: Int = 0

    private var deviceNameAdapter: GroupNameAdapter? = null

    private var totalNum: TextView? = null

    private var btnOn: ImageView? = null

    private var btnOff: ImageView? = null

    private var btnSet: ImageView? = null

    private var allGroup: DbGroup? = null

    private var cwLightGroup: String? = null

    private var switchFragment: String? = null

    private var isDelete = false

    private lateinit var localBroadcastManager: LocalBroadcastManager

    private lateinit var br: BroadcastReceiver

    private var isFirst: Boolean = false

    private var allLightText: TextView? = null

    private var onText: TextView? = null

    private var offText: TextView? = null

    private var fragmentPosition = 0

    private var delete: String? = null

    private var deleteComplete: String? = null

//    private var cw_light_btn: TextView? = null
//    private var rgb_light_btn: TextView? = null
//    private var curtain_btn: TextView? = null
//    private var relay_btn: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.mContext = this.activity
        setHasOptionsMenu(true)
        localBroadcastManager = LocalBroadcastManager
                .getInstance(this!!.mContext!!)
        val intentFilter = IntentFilter()
        intentFilter.addAction("showPro")
        intentFilter.addAction("switch_fragment")
        intentFilter.addAction("isDelete")
        intentFilter.addAction("delete_true")
        br = object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                cwLightGroup = intent.getStringExtra("is_delete")
                switchFragment = intent.getStringExtra("switch_fragment")
                delete = intent.getStringExtra("isDelete")
                deleteComplete = intent.getStringExtra("delete_true")
                if (cwLightGroup == "true") {
                    toolbar!!.findViewById<ImageView>(R.id.img_function2).visibility = View.VISIBLE
                    toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).visibility = View.GONE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.GONE
                    toolbar!!.title = ""
                    setBack()
                }
                if (switchFragment == "true") {
                    toolbar!!.setTitle(R.string.group_title)
                    toolbar!!.findViewById<ImageView>(R.id.img_function2).visibility = View.GONE
                    toolbar!!.navigationIcon = null
                    toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).visibility = View.VISIBLE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
                }
                if (delete == "true") {
                    toolbar!!.setTitle(R.string.group_title)
                    toolbar!!.findViewById<ImageView>(R.id.img_function2).visibility = View.GONE
                    toolbar!!.navigationIcon = null
                    toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).visibility = View.VISIBLE
                    toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
                    SharedPreferencesUtils.setDelete(false)
                    val intent = Intent("back")
                    intent.putExtra("back", "true")
                    LocalBroadcastManager.getInstance(context)
                            .sendBroadcast(intent)
                }

                if (deleteComplete == "true") {
                    hideLoadingDialog()
                }
            }
        }
        localBroadcastManager.registerReceiver(br, intentFilter)
    }

    private fun setBack() {
        toolbar!!.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar!!.setNavigationOnClickListener {
            val intent = Intent("back")
            intent.putExtra("back", "true")
            LocalBroadcastManager.getInstance(this!!.mContext!!)
                    .sendBroadcast(intent)
            toolbar!!.setTitle(R.string.group_title)
            toolbar!!.findViewById<ImageView>(R.id.img_function2).visibility = View.GONE
            toolbar!!.navigationIcon = null
            toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).visibility = View.VISIBLE
            toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
            SharedPreferencesUtils.setDelete(false)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = getView(inflater)
        this.initData()
        return view
    }

    private fun scrollToPosition() {

        sharedPreferences = mContext!!.getSharedPreferences("key", Activity.MODE_PRIVATE)

        lastOffset = sharedPreferences!!.getInt("lastOffset", 0)

        lastPosition = sharedPreferences!!.getInt("lastPosition", 0)

//        if (recyclerView!!.layoutManager != null && lastPosition >= 0) {
//
//            (recyclerView!!.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(lastPosition, lastOffset)
//
//        }

    }


    override fun onResume() {
        super.onResume()
        isFristUserClickCheckConnect = true

        refreshView()
        scrollToPosition()
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        if (isVisibleToUser) {
            val act = activity as MainActivity?
            act?.addEventListeners()
            if (Constant.isCreat) {
                refreshAndMoveBottom()
                Constant.isCreat = false
            } else {
                refreshView()
            }

        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        if (!hidden) {
//            this.initData()
        }
    }

    override fun onStop() {
        super.onStop()
        isFristUserClickCheckConnect = false
    }

    private fun getView(inflater: LayoutInflater): View {
        this.inflater = inflater

        val view = inflater.inflate(R.layout.fragment_group_list, null)

        viewPager = view.findViewById(R.id.list_groups)

        toolbar = view.findViewById(R.id.toolbar)
        toolbar!!.setTitle(R.string.group_title)

        allLightText = view.findViewById(R.id.textView6)
        val btn_delete = toolbar!!.findViewById<ImageView>(R.id.img_function2)
//        btn_delete.visibility = View.VISIBLE

        toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
//        toolbar!!.findViewById<ImageView>(R.id.img_function2).visibility = View.GONE
        toolbar!!.findViewById<ImageView>(R.id.img_function1).setOnClickListener {
            isGuide = false
            if (dialog_pop?.visibility == View.GONE) {
                showPopupMenu()
            } else {
                hidePopupMenu()
            }
        }

        setHasOptionsMenu(true)

        btnOn = view.findViewById(R.id.btn_on)
        btnOff = view.findViewById(R.id.btn_off)
        btnSet = view.findViewById(R.id.btn_set)
        onText = view.findViewById(R.id.textView8)
        offText = view.findViewById(R.id.textView11)


        deviceRecyclerView = view.findViewById(R.id.recyclerView_name)
        totalNum = view.findViewById(R.id.total_num)

        install_device = view.findViewById(R.id.install_device)
        create_group = view.findViewById(R.id.create_group)
        create_scene = view.findViewById(R.id.create_scene)
        install_device?.setOnClickListener(onClick)
        create_group?.setOnClickListener(onClick)
        create_scene?.setOnClickListener(onClick)
        btnOn?.setOnClickListener(onClick)
        btnOff?.setOnClickListener(onClick)
        btnSet?.setOnClickListener(onClick)
        btn_delete.setOnClickListener(onClick)
        allLightText?.setOnClickListener(onClick)

        return view
    }

    fun isDelete(delete: Boolean) {
        if (delete) {
            toolbar!!.findViewById<ImageView>(R.id.img_function2).visibility = View.VISIBLE
        } else {
            toolbar!!.findViewById<ImageView>(R.id.img_function2).visibility = View.GONE
        }

    }

    @SuppressLint("SetTextI18n")
    private fun initData() {
        this.mApplication = activity!!.application as TelinkLightApplication
        gpList = DBUtils.getgroupListWithType(activity!!)


        var cwNum = DBUtils.getAllNormalLight().size
        var rgbNum = DBUtils.getAllRGBLight().size

        if (cwNum != 0 || rgbNum != 0) {
            totalNum?.text = getString(R.string.total) + (cwNum + rgbNum) + getString(R.string.piece)
        } else {
            totalNum?.text = getString(R.string.total) + (0) + getString(R.string.piece)
        }

        deviceName = ArrayList()
        var stringName = arrayOf(TelinkLightApplication.getInstance().getString(R.string.normal_light), TelinkLightApplication.getInstance().getString(R.string.rgb_light),
                TelinkLightApplication.getInstance().getString(R.string.curtain), TelinkLightApplication.getInstance().getString(R.string.connector))

        for (i in stringName.indices) {
            var device = DbDeviceName()
            device.name = stringName[i]
            deviceName!!.add(device)
        }

        initBottomNavigation()

        val layoutManager = LinearLayoutManager(activity)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        deviceRecyclerView!!.layoutManager = layoutManager
        deviceNameAdapter = GroupNameAdapter(deviceName, onRecyclerviewItemClickListener)
        deviceRecyclerView!!.setAdapter(deviceNameAdapter)
//        updateData(0, true)
//        updateData(1, false)
//        updateData(2, false)
//        updateData(3, false)
//        deviceNameAdapter?.notifyDataSetChanged()

        deviceRecyclerView!!.addItemDecoration(SpaceItemDecoration(130))

        showList = ArrayList()
        showList = gpList

        allGroup = DBUtils.getGroupByMesh(0xFFFF)


        if (allGroup != null) {
            if (allGroup!!.connectionStatus == ConnectionStatus.ON.value) {
                btnOn?.setBackgroundResource(R.drawable.icon_open_group)
                btnOff?.setBackgroundResource(R.drawable.icon_down_group)
                onText?.setTextColor(resources.getColor(R.color.white))
                offText?.setTextColor(resources.getColor(R.color.black_nine))
            } else if (allGroup!!.connectionStatus == ConnectionStatus.OFF.value) {
                btnOn?.setBackgroundResource(R.drawable.icon_down_group)
                btnOff?.setBackgroundResource(R.drawable.icon_open_group)
                onText?.setTextColor(resources.getColor(R.color.black_nine))
                offText?.setTextColor(resources.getColor(R.color.white))
            }
        }


//        val layoutmanager = LinearLayoutManager(activity)
//        layoutmanager.orientation = LinearLayoutManager.VERTICAL
//        recyclerView!!.layoutManager = layoutmanager
//        this.adapter = GroupListRecycleViewAdapter(R.layout.group_item, onItemChildClickListener, showList!!)
//
////        val decoration = DividerItemDecoration(activity!!,
////                DividerItemDecoration
////                        .VERTICAL)
////        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(activity!!, R.color
////                .divider)))
////        //添加分割线
////        recyclerView?.addItemDecoration(decoration)
//        recyclerView?.itemAnimator = DefaultItemAnimator()
//
////        adapter!!.addFooterView(getFooterView())
//        adapter!!.bindToRecyclerView(recyclerView)
//
////        getPositionAndOffset()
//
//        recyclerView!!.setOnScrollListener(object : RecyclerView.OnScrollListener() {
//            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
//                super.onScrollStateChanged(recyclerView, newState)
//                if (recyclerView!!.layoutManager != null) {
//                    getPositionAndOffset()
//                }
//            }
//        })

//        setMove()

        application = activity!!.application as TelinkLightApplication
        dataManager = DataManager(TelinkLightApplication.getInstance(),
                application!!.mesh.name, application!!.mesh.password)
    }

    private fun initBottomNavigation() {
        cwLightFragment = CWLightFragmentList()
        rgbLightFragment = RGBLightFragmentList()
        curtianFragment = CurtainFragmentList()
        relayFragment = RelayFragmentList()

        val fragments: List<Fragment> = listOf(cwLightFragment, rgbLightFragment, curtianFragment, relayFragment)
        val vpAdapter = ViewPagerAdapter(childFragmentManager, fragments)
        viewPager?.adapter = vpAdapter

        viewPager?.currentItem = fragmentPosition
        if (fragmentPosition == 0) {
            updateData(0, true)
            updateData(1, false)
            updateData(2, false)
            updateData(3, false)
            deviceNameAdapter?.notifyDataSetChanged()
        } else if (fragmentPosition == 1) {
            updateData(0, false)
            updateData(1, true)
            updateData(2, false)
            updateData(3, false)
            deviceNameAdapter?.notifyDataSetChanged()
        } else if (fragmentPosition == 2) {
            updateData(0, false)
            updateData(1, false)
            updateData(2, true)
            updateData(3, false)
            deviceNameAdapter?.notifyDataSetChanged()
        } else if (fragmentPosition == 3) {
            updateData(0, false)
            updateData(1, false)
            updateData(2, false)
            updateData(3, true)
            deviceNameAdapter?.notifyDataSetChanged()
        }

        viewPager?.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(p0: Int) {
//                Log.e("TAG_ScrollStateChanged",p0.toString())
            }

            override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
//                Log.e("TAG_p0",p0.toString())
//                if (!isFirst) {
//                    updateData(0, true)
//                    updateData(1, false)
//                    updateData(2, false)
//                    updateData(3, false)
//                    isFirst = true
//                    fragmentPosition = 0
//                    deviceNameAdapter?.notifyDataSetChanged()
//                }
            }

            override fun onPageSelected(p0: Int) {
                Log.e("TAG_Selected", p0.toString())
                if (p0 == 0) {
                    val intent = Intent("switch")
                    intent.putExtra("switch", "true")
                    mContext?.let {
                        LocalBroadcastManager.getInstance(it)
                                .sendBroadcast(intent)
                    }
                    fragmentPosition = 0
                    updateData(0, true)
                    updateData(1, false)
                    updateData(2, false)
                    updateData(3, false)
                } else if (p0 == 1) {
                    val intent = Intent("switch")
                    intent.putExtra("switch", "true")
                    mContext?.let {
                        LocalBroadcastManager.getInstance(it)
                                .sendBroadcast(intent)
                    }
                    fragmentPosition = 1
                    updateData(0, false)
                    updateData(1, true)
                    updateData(2, false)
                    updateData(3, false)
                } else if (p0 == 2) {
                    val intent = Intent("switch")
                    intent.putExtra("switch", "true")
                    mContext?.let {
                        LocalBroadcastManager.getInstance(it)
                                .sendBroadcast(intent)
                    }
                    updateData(2, true)
                    updateData(1, false)
                    updateData(0, false)
                    updateData(3, false)
                    fragmentPosition = 2
                } else if (p0 == 3) {
                    val intent = Intent("switch")
                    intent.putExtra("switch", "true")
                    mContext?.let {
                        LocalBroadcastManager.getInstance(it)
                                .sendBroadcast(intent)
                    }
                    fragmentPosition = 3
                    updateData(3, true)
                    updateData(0, false)
                    updateData(1, false)
                    updateData(2, false)
                }
                deviceNameAdapter?.notifyDataSetChanged()
            }
        })
    }


    private val onRecyclerviewItemClickListener = OnRecyclerviewItemClickListener { v, position ->
        currentGroupIndex = position
        for (i in deviceName!!.indices.reversed()) {
            if (i != position && deviceName!!.get(i).checked) {
                updateData(i, false)
            } else if (i == position && !deviceName!!.get(i).checked) {
                updateData(i, true)
            } else if (i == position && deviceName!!.get(i).checked) {
                updateData(i, true)
            }
        }

        viewPager?.currentItem = position
        fragmentPosition = position

        deviceNameAdapter?.notifyDataSetChanged()
//        SharedPreferencesHelper.putInt(TelinkLightApplication.getInstance(),
//                Constant.DEFAULT_GROUP_ID, currentGroupIndex)
    }

    private fun updateData(position: Int, checkStateChange: Boolean) {
        deviceName!![position].checked = checkStateChange
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
                        DBUtils.addNewGroupWithType(textGp.text.toString().trim { it <= ' ' }, DBUtils.groupList, Constant.DEVICE_TYPE_DEFAULT_ALL, activity!!)
                        refreshAndMoveBottom()
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

    private fun refreshAndMoveBottom() {
        refreshView()
//        recyclerView?.smoothScrollToPosition(showList!!.size)
    }

    private fun refreshView() {
        if (activity != null) {
            gpList = DBUtils.getgroupListWithType(activity!!)

            showList = ArrayList()
            showList = gpList

            deviceName = ArrayList()

            isFirst = false

            var cwNum = DBUtils.getAllNormalLight().size
            var rgbNum = DBUtils.getAllRGBLight().size

            if (cwNum != 0 || rgbNum != 0) {
                totalNum?.text = getString(R.string.total) + (cwNum + rgbNum) + getString(R.string.piece)
            } else {
                totalNum?.text = getString(R.string.total) + (0) + getString(R.string.piece)
            }

            var stringName = arrayOf(TelinkLightApplication.getInstance().getString(R.string.normal_light),
                    TelinkLightApplication.getInstance().getString(R.string.rgb_light),
                    TelinkLightApplication.getInstance().getString(R.string.curtain),
                    TelinkLightApplication.getInstance().getString(R.string.connector))

            for (i in stringName.indices) {
                var device = DbDeviceName()
                device.name = stringName[i]
                deviceName!!.add(device)
            }


            toolbar!!.findViewById<ImageView>(R.id.img_function2).visibility = View.GONE
            toolbar!!.navigationIcon = null
            toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).visibility = View.VISIBLE
            toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
            allGroup = DBUtils.getGroupByMesh(0xFFFF)
            toolbar!!.setTitle(R.string.group_title)
            SharedPreferencesUtils.setDelete(false)

            val layoutManager = LinearLayoutManager(activity)
            layoutManager.orientation = LinearLayoutManager.HORIZONTAL
            deviceRecyclerView!!.layoutManager = layoutManager
            deviceNameAdapter = GroupNameAdapter(deviceName, onRecyclerviewItemClickListener)
            deviceRecyclerView!!.setAdapter(deviceNameAdapter)

            if (allGroup != null) {
                if (allGroup!!.connectionStatus == ConnectionStatus.ON.value) {
                    btnOn?.setBackgroundResource(R.drawable.icon_open_group)
                    btnOff?.setBackgroundResource(R.drawable.icon_down_group)
                    onText?.setTextColor(resources.getColor(R.color.white))
                    offText?.setTextColor(resources.getColor(R.color.black_nine))
                    if (SharedPreferencesHelper.getBoolean(TelinkLightApplication.getInstance(), Constant.IS_ALL_LIGHT_MODE, false)) {
                        val intent = Intent("switch_here")
                        intent.putExtra("switch_here", "on")
                        LocalBroadcastManager.getInstance(this!!.mContext!!)
                                .sendBroadcast(intent)
                    }
                } else if (allGroup!!.connectionStatus == ConnectionStatus.OFF.value) {
                    btnOn?.setBackgroundResource(R.drawable.icon_down_group)
                    btnOff?.setBackgroundResource(R.drawable.icon_open_group)
                    onText?.setTextColor(resources.getColor(R.color.black_nine))
                    offText?.setTextColor(resources.getColor(R.color.white))
                    if (SharedPreferencesHelper.getBoolean(TelinkLightApplication.getInstance(), Constant.IS_ALL_LIGHT_MODE, false)) {
                        val intent = Intent("switch_here")
                        intent.putExtra("switch_here", "false")
                        LocalBroadcastManager.getInstance(this!!.mContext!!)
                                .sendBroadcast(intent)
                    }
                }
            }

            initBottomNavigation()
        }
    }

    var onItemChildClickListener = object : MyBaseQuickAdapterOnClickListner {
        override fun onItemChildClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int, groupPosition: Int) {
            if (dialog_pop.visibility == View.GONE || dialog_pop == null) {
                val group = showList!![groupPosition].list[position]
                val dstAddr = group.meshAddr
                var intent: Intent

                if (TelinkLightApplication.getInstance().connectDevice == null) {
                    ToastUtils.showLong(activity!!.getString(R.string.device_not_connected))
                    checkConnect()
                } else {
                    when (view!!.id) {
                        R.id.btn_on -> {
                            Commander.openOrCloseLights(dstAddr, true)
                            updateLights(true, group)
                        }
                        R.id.btn_off -> {
                            Commander.openOrCloseLights(dstAddr, false)
                            updateLights(false, group)
                        }
                        R.id.btn_set -> {
                            intent = Intent(mContext, NormalSettingActivity::class.java)
                            if (OtherUtils.isRGBGroup(group) && group.meshAddr != 0xffff) {
                                intent = Intent(mContext, RGBSettingActivity::class.java)
                            } else if (OtherUtils.isCurtain(group) && group.meshAddr != 0xffff) {
                                intent = Intent(mContext, WindowCurtainsActivity::class.java)
                            } else if (OtherUtils.isConnector(group) && group.meshAddr != 0xfffff) {
                                intent = Intent(mContext, ConnectorSettingActivity::class.java)
                            }
                            intent.putExtra(Constant.TYPE_VIEW, Constant.TYPE_GROUP)
                            intent.putExtra("group", group)
                            startActivityForResult(intent, 2)
                        }
                        R.id.txt_name -> {
                            if (group.meshAddr != 0xffff) {
                                if (group.deviceType == Constant.DEVICE_TYPE_CURTAIN) {
                                    intent = Intent(mContext, CurtainOfGroupActivity::class.java)
                                    intent.putExtra("group", group)
                                    startActivityForResult(intent, 2)
                                } else if (group.deviceType == Constant.DEVICE_TYPE_CONNECTOR) {
                                    intent = Intent(mContext, ConnectorOfGroupActivity::class.java)
                                    intent.putExtra("group", group)
                                    startActivityForResult(intent, 2)
                                } else {
                                    intent = Intent(mContext, LightsOfGroupActivity::class.java)
                                    intent.putExtra("group", group)
                                    startActivityForResult(intent, 2)
                                }
                            } else if (group.deviceType == Constant.DEVICE_TYPE_NO) {
                                Toast.makeText(activity, R.string.device_page, Toast.LENGTH_LONG).show()
                            }

//                        ActivityUtils.startActivityForResult(intent)
                        }
//                    R.id.add_group -> {
//                        addNewGroup()
//                    }
                    }
                }
            }
        }
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
                if (TelinkLightApplication.getInstance().connectDevice == null) {
                    ToastUtils.showLong(activity!!.getString(R.string.device_not_connected))
                } else {
                    addNewGroup()
                }
            }
            R.id.create_scene -> {
                val nowSize = DBUtils.sceneList.size
                if (TelinkLightApplication.getInstance().connectDevice == null) {
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

            R.id.btn_on -> {
                if (TelinkLightApplication.getInstance().connectDevice == null) {
                    ToastUtils.showLong(activity!!.getString(R.string.device_not_connected))
                    checkConnect()
                } else {
                    if (allGroup != null) {
                        val dstAddr = this.allGroup!!.meshAddr
                        Commander.openOrCloseLights(dstAddr, true)
                        btnOn?.setBackgroundResource(R.drawable.icon_open_group)
                        btnOff?.setBackgroundResource(R.drawable.icon_down_group)
                        onText?.setTextColor(resources.getColor(R.color.white))
                        offText?.setTextColor(resources.getColor(R.color.black_nine))
                        updateLights(true, this.allGroup!!)
                        val intent = Intent("switch_here")
                        intent.putExtra("switch_here", "on")
                        LocalBroadcastManager.getInstance(this!!.mContext!!)
                                .sendBroadcast(intent)
                    }
                }
            }

            R.id.btn_off -> {
                if (TelinkLightApplication.getInstance().connectDevice == null) {
                    ToastUtils.showLong(activity!!.getString(R.string.device_not_connected))
                    checkConnect()
                } else {
                    if (allGroup != null) {
                        val dstAddr = this.allGroup!!.meshAddr
                        Commander.openOrCloseLights(dstAddr, false)
                        btnOn?.setBackgroundResource(R.drawable.icon_down_group)
                        btnOff?.setBackgroundResource(R.drawable.icon_open_group)
                        onText?.setTextColor(resources.getColor(R.color.black_nine))
                        offText?.setTextColor(resources.getColor(R.color.white))
                        updateLights(false, this.allGroup!!)
                        val intent = Intent("switch_here")
                        intent.putExtra("switch_here", "false")
                        LocalBroadcastManager.getInstance(this!!.mContext!!)
                                .sendBroadcast(intent)
                    }
                }
            }

            R.id.btn_set -> {
                if (TelinkLightApplication.getInstance().connectDevice == null) {
                    ToastUtils.showLong(activity!!.getString(R.string.device_not_connected))
                    checkConnect()
                } else {
                    intent = Intent(mContext, NormalSettingActivity::class.java)
                    intent.putExtra(Constant.TYPE_VIEW, Constant.TYPE_GROUP)
                    intent.putExtra("group", allGroup)
                    startActivityForResult(intent, 2)
                }
            }

            R.id.img_function2 -> {
//                val intent = Intent("delete")
//                intent.putExtra("delete", "true")
//                LocalBroadcastManager.getInstance(this!!.mContext!!)
//                        .sendBroadcast(intent)
                var deleteList: ArrayList<DbGroup>? = null
                deleteList = ArrayList()

                var listLight = DBUtils.getAllGroupsOrderByIndex()

                if (listLight.size > 0) {
                    for (i in listLight.indices) {
                        if (listLight[i].isSelected) {
                            deleteList.add(listLight[i])
                        }
                    }
                }

                if (deleteList.size > 0) {
                    android.support.v7.app.AlertDialog.Builder(Objects.requireNonNull<FragmentActivity>(mContext as FragmentActivity?)).setMessage(R.string.delete_group_confirm)
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                showLoadingDialog(getString(R.string.deleting))
                                val intent = Intent("delete")
                                intent.putExtra("delete", "true")
                                LocalBroadcastManager.getInstance(this!!.mContext!!)
                                        .sendBroadcast(intent)
                            }
                            .setNegativeButton(R.string.btn_cancel, null)
                            .show()
                }
            }
            R.id.textView6 -> {
                Toast.makeText(activity, R.string.device_page, Toast.LENGTH_LONG).show()
            }
        }
    }

    val CREATE_SCENE_REQUESTCODE = 3
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
            callbackLinkMainActAndFragment = context as CallbackLinkMainActAndFragment
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

    private fun checkConnect() {
        try {
            if (TelinkLightApplication.getInstance().connectDevice == null) {
                if (isFristUserClickCheckConnect) {
                    val activity = activity as MainActivity
                    activity.autoConnect()
                    isFristUserClickCheckConnect = false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun endCurrentGuide() {
        super.endCurrentGuide()
    }

    private fun updateLights(isOpen: Boolean, group: DbGroup) {
        updateLightDisposal?.dispose()
        updateLightDisposal = Observable.timer(300, TimeUnit.MILLISECONDS, Schedulers.io())
                .subscribe ({
                    var lightList: MutableList<DbLight> = ArrayList()

                    if (group.meshAddr == 0xffff) {
                        //            lightList = DBUtils.getAllLight();
                        val list = DBUtils.groupList
                        for (j in list.indices) {
                            lightList.addAll(DBUtils.getLightByGroupID(list[j].id))
                        }
                    } else {
                        lightList = DBUtils.getLightByGroupID(group.id)
                    }

                    if (isOpen) {
                        group.connectionStatus = ConnectionStatus.ON.value
                        DBUtils.updateGroup(group)
                    } else {
                        group.connectionStatus = ConnectionStatus.OFF.value
                        DBUtils.updateGroup(group)
                    }

                    for (dbLight: DbLight in lightList) {
                        if (isOpen) {
                            dbLight.connectionStatus = ConnectionStatus.ON.value
                        } else {
                            dbLight.connectionStatus = ConnectionStatus.OFF.value
                        }
                        DBUtils.updateLightLocal(dbLight)
                    }
                },{})
    }

    fun notifyDataSetChanged() {
        this.adapter!!.notifyDataSetChanged()
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

    override fun onDestroy() {
        super.onDestroy()
        localBroadcastManager.unregisterReceiver(br)
    }
}

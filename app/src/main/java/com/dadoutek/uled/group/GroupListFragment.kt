package com.dadoutek.uled.group

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.CoordinatorLayout
import android.support.v4.content.ContextCompat
import android.support.v7.util.DiffUtil
import android.support.v7.widget.*
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.DisplayMetrics
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.app.hubert.guide.core.Controller
import com.app.hubert.guide.model.GuidePage
import com.app.hubert.guide.model.HighLight
import com.blankj.utilcode.util.ScreenUtils
import com.blankj.utilcode.util.SizeUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.callback.ItemDragAndSwipeCallback
import com.chad.library.adapter.base.listener.OnItemDragListener
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.light.DeviceScanningNewActivity
import com.dadoutek.uled.light.LightsOfGroupActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.othersview.BaseFragment
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.pir.ScanningSensorActivity
import com.dadoutek.uled.rgb.RGBGroupSettingActivity
import com.dadoutek.uled.switches.ScanningSwitchActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.DataManager
import com.dadoutek.uled.util.GuideUtils
import com.dadoutek.uled.util.OtherUtils
import com.telink.bluetooth.light.ConnectionStatus
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_group_list.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import java.util.*
import java.util.concurrent.TimeUnit

class GroupListFragment : BaseFragment() {
    private var inflater: LayoutInflater? = null
    private var adapter: GroupListRecycleViewAdapter? = null
    private var mContext: Activity? = null
    private var mApplication: TelinkLightApplication? = null
    private var dataManager: DataManager? = null
    private var gpList: List<DbGroup>? = null
    private var application: TelinkLightApplication? = null
    private var toolbar: Toolbar? = null
    private var recyclerView: RecyclerView? = null
    internal var showList: List<DbGroup>? = null
    private var updateLightDisposal: Disposable? = null
    private var install_light: TextView? = null
    private var install_rgb_light: TextView? = null
    private var install_switch: TextView? = null
    private var install_sensor: TextView? = null
    //新用户选择的初始安装选项是否是RGB灯
    private var isRgbClick = false
    //是否正在引导
    private var isGuide = false
    var firstShowGuide = true
    private var guideShowCurrentPage = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.mContext = this.activity
        setHasOptionsMenu(true)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = getView(inflater)
        this.initData()
        if (firstShowGuide) {
            firstShowGuide = false
            initOnLayoutListener()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        if (isVisibleToUser) {
            val act = activity as MainActivity?
            act?.addEventListeners()
            initOnLayoutListener()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        if (!hidden) {
//            this.initData()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        refreshData()

        if (requestCode == 0 && resultCode == Constant.RESULT_OK) {
        }
    }

    override fun onStop() {
        super.onStop()
    }

    private fun getView(inflater: LayoutInflater): View {
        this.inflater = inflater

        val view = inflater.inflate(R.layout.fragment_group_list, null)

        toolbar = view.findViewById(R.id.toolbar)
        toolbar!!.setTitle(R.string.group_title)

        toolbar!!.findViewById<TextView>(R.id.tv_function1).visibility = View.VISIBLE
        toolbar!!.findViewById<ImageView>(R.id.img_function2).visibility = View.GONE
        toolbar!!.findViewById<TextView>(R.id.tv_function1).setOnClickListener {
            isGuide = false
            if (dialog_pop?.visibility == View.GONE) {
                showPopupMenu()
            } else {
                hidePopupMenu()
            }
        }

        setHasOptionsMenu(true)

        recyclerView = view.findViewById(R.id.list_groups)

        install_light = view.findViewById(R.id.install_light)
        install_rgb_light = view.findViewById(R.id.install_rgb_light)
        install_switch = view.findViewById(R.id.install_switch)
        install_sensor = view.findViewById(R.id.install_sensor)
        install_light?.setOnClickListener(onClick)
        install_rgb_light?.setOnClickListener(onClick)
        install_switch?.setOnClickListener(onClick)
        install_sensor?.setOnClickListener(onClick)
        return view
    }

    private fun initData() {
        this.mApplication = activity!!.application as TelinkLightApplication
        gpList = DBUtils.groupList

        showList = ArrayList()

        val dbOldGroupList = SharedPreferencesHelper.getObject(TelinkLightApplication.getInstance(), Constant.OLD_INDEX_DATA) as? ArrayList<DbGroup>

        //如果有调整过顺序取本地数据，否则取数据库数据
        if (dbOldGroupList != null && dbOldGroupList.size > 0) {
            showList = dbOldGroupList
        } else {
            showList = gpList
        }

        val layoutmanager = LinearLayoutManager(activity)
        layoutmanager.orientation = LinearLayoutManager.VERTICAL
        recyclerView!!.layoutManager = layoutmanager
        this.adapter = GroupListRecycleViewAdapter(R.layout.group_item, showList)

        val decoration = DividerItemDecoration(activity!!,
                DividerItemDecoration
                        .VERTICAL)
        decoration.setDrawable(ColorDrawable(ContextCompat.getColor(activity!!, R.color
                .divider)))
        //添加分割线
        recyclerView?.addItemDecoration(decoration)
        recyclerView?.itemAnimator = DefaultItemAnimator()

        adapter!!.setOnItemChildClickListener(onItemChildClickListener)
        adapter!!.bindToRecyclerView(recyclerView)

        setMove()

        application = activity!!.application as TelinkLightApplication
        dataManager = DataManager(TelinkLightApplication.getInstance(),
                application!!.mesh.name, application!!.mesh.password)
    }

    var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        if(dialog_pop.visibility==View.GONE || dialog_pop==null){
            val group = showList!![position]
            val dstAddr = group.meshAddr
            var intent: Intent

            if (TelinkLightApplication.getInstance().connectDevice == null) {
                ToastUtils.showLong(activity!!.getString(R.string.device_not_connected))
            }else{
                when (view.getId()) {
                    R.id.btn_on -> {
                        Commander.openOrCloseLights(dstAddr, true)
                        updateLights(true, group)
                    }
                    R.id.btn_off -> {
                        Commander.openOrCloseLights(dstAddr, false)
                        updateLights(false, group)
                    }
                    R.id.btn_set -> {
                        intent = Intent(mContext, NormalGroupSettingActivity::class.java)
                        if (OtherUtils.isRGBGroup(group) && group.meshAddr != 0xffff) {
                            intent = Intent(mContext, RGBGroupSettingActivity::class.java)
                        }
                        intent.putExtra("group", group)
                        startActivityForResult(intent, 0)
                    }
                    R.id.txt_name -> {
                        intent = Intent(mContext, LightsOfGroupActivity::class.java)
                        intent.putExtra("group", group)
                        startActivityForResult(intent, 0)
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
        isGuide = false
        hidePopupMenu()
        when (it.id) {
            R.id.install_light -> {
                if (DBUtils.allLight.size < 254) {
                    intent = Intent(mContext, DeviceScanningNewActivity::class.java)
                    intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, false)
                    startActivityForResult(intent, 0)
                } else {
                    ToastUtils.showLong(getString(R.string.much_lamp_tip))
                }
            }
            R.id.install_rgb_light -> {
                if (DBUtils.allLight.size < 254) {
                    intent = Intent(mContext, DeviceScanningNewActivity::class.java)
                    intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
                    startActivityForResult(intent, 0)
                } else {
                    ToastUtils.showLong(getString(R.string.much_lamp_tip))
                }
            }
            R.id.install_switch -> startActivity(Intent(mContext, ScanningSwitchActivity::class.java))
            R.id.install_sensor -> startActivity(Intent(mContext, ScanningSensorActivity::class.java))
        }
    }

    override fun endCurrentGuide() {
        super.endCurrentGuide()
    }

    private fun updateLights(isOpen: Boolean, group: DbGroup) {
        updateLightDisposal?.dispose()
        updateLightDisposal = Observable.timer(300, TimeUnit.MILLISECONDS, Schedulers.io())
                .subscribe {
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

                    for (dbLight: DbLight in lightList) {
                        if (isOpen) {
                            dbLight.connectionStatus = ConnectionStatus.ON.value
                        } else {
                            dbLight.connectionStatus = ConnectionStatus.OFF.value
                        }
                        DBUtils.updateLightLocal(dbLight)
                    }
                }
    }

    fun loadData(): List<DbGroup> {
        var showList = mutableListOf<DbGroup>()

        val dbOldGroupList = SharedPreferencesHelper.getObject(TelinkLightApplication.getInstance(), Constant.OLD_INDEX_DATA) as? ArrayList<DbGroup>

        //如果有调整过顺序取本地数据，否则取数据库数据
        if (dbOldGroupList != null && dbOldGroupList.size > 0) {
            showList = dbOldGroupList
        } else {
            showList = DBUtils.groupList
        }
        return showList
    }

    fun refreshData() {
        val mOldDatas: List<DbGroup>? = showList
        val mNewDatas: List<DbGroup>? = loadData()
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
        showList = mNewDatas
        adapter?.setNewData(showList)
        diffResult.dispatchUpdatesTo(adapter)
    }


    fun lazyLoad() {
        guideShowCurrentPage = !GuideUtils.getCurrentViewIsEnd(activity!!,GuideUtils.END_GROUPLIST_KEY,false)
        if(guideShowCurrentPage){
            GuideUtils.resetGroupListGuide(activity!!)
            val guide0= toolbar!!.findViewById<TextView>(R.id.toolbarTv)
            GuideUtils.guideBuilder(this@GroupListFragment,GuideUtils.STEP0_GUIDE_SELECT_DEVICE_KEY)
                    .addGuidePage(GuideUtils.addGuidePage(guide0,R.layout.view_guide_0,getString(R.string.group_list_guide0), View.OnClickListener {},GuideUtils.END_GROUPLIST_KEY,activity!!)
                            .setOnLayoutInflatedListener { view, controller ->
                                val normal=view.findViewById<TextView>(R.id.normal_light)
                                normal.setOnClickListener {
                                    controller.remove()
                                    guide1()
                                    isRgbClick=false
                                }
                                val rgb=view.findViewById<TextView>(R.id.rgb_light)
                                rgb.setOnClickListener {
                                    controller.remove()
                                    guide1()
                                    isRgbClick=true
                                }
                                val tvJump = view.findViewById<TextView>(R.id.jump_out)
                                tvJump.setOnClickListener { v ->
                                    GuideUtils.showExitGuideDialog(activity!!,controller,GuideUtils.END_GROUPLIST_KEY)
                                }
                            })
                    .show()
        }
    }

    private fun guide1() {
        guideShowCurrentPage = !GuideUtils.getCurrentViewIsEnd(activity!!, GuideUtils.END_GROUPLIST_KEY, false)
        if (guideShowCurrentPage) {
            val guide1 = toolbar!!.findViewById<TextView>(R.id.tv_function1)

            GuideUtils.guideBuilder(this@GroupListFragment, GuideUtils.STEP1_GUIDE_ADD_DEVICE_KEY)
                    .addGuidePage(GuideUtils.addGuidePage(guide1, R.layout.view_guide_simple_group1, getString(R.string.group_list_guide1), View.OnClickListener {
                        isGuide = true
                        showPopupMenu()
                    }, GuideUtils.END_GROUPLIST_KEY,activity!!))
                    .show()
        }
    }

    private fun guide2(): Controller? {
        guideShowCurrentPage = !GuideUtils.getCurrentViewIsEnd(activity!!, GuideUtils.END_GROUPLIST_KEY, false)
        if (guideShowCurrentPage) {
            var guide3: TextView? = null
            if (isRgbClick) {
                guide3 = install_rgb_light
            } else {
                guide3 = install_light
            }

            return GuideUtils.guideBuilder(this@GroupListFragment, GuideUtils.STEP2_GUIDE_START_INSTALL_DEVICE)
                    .addGuidePage(GuideUtils.addGuidePage(guide3!!, R.layout.view_guide_simple_group2, getString(R.string.group_list_guide2), View.OnClickListener {
                        if (isRgbClick) {
                            install_rgb_light?.performClick()
                        } else {
                            install_light?.performClick()
                        }
                        GuideUtils.changeCurrentViewIsEnd(activity!!, GuideUtils.END_GROUPLIST_KEY, true)
                    }, GuideUtils.END_GROUPLIST_KEY,activity!!))
                    .show()
        }
        return null
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

    private fun setMove() {
        val onItemDragListener = object : OnItemDragListener {
            override fun onItemDragStart(viewHolder: RecyclerView.ViewHolder, pos: Int) {}

            override fun onItemDragMoving(source: RecyclerView.ViewHolder, from: Int,
                                          target: RecyclerView.ViewHolder, to: Int) {

            }

            override fun onItemDragEnd(viewHolder: RecyclerView.ViewHolder, pos: Int) {
                //                viewHolder.getItemId();
                val list = adapter!!.data
                SharedPreferencesHelper.putObject(activity, Constant.OLD_INDEX_DATA, list)
            }
        }

        val itemDragAndSwipeCallback = ItemDragAndSwipeCallback(adapter)
        val itemTouchHelper = ItemTouchHelper(itemDragAndSwipeCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        adapter!!.enableDragItem(itemTouchHelper, R.id.txt_name, true)
        adapter!!.setOnItemDragListener(onItemDragListener)
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
                    launch(UI) {
                        hidePopupMenu()
                    }
                }.start()
            }else if(dialog_pop==null){
                hidePopupMenu()
            }
        }
    }

    private fun showPopupMenu() {
        dialog_pop?.visibility = View.VISIBLE
        if (isGuide) {
            guide2()
        }
    }

    private fun hidePopupMenu() {
        if (!isGuide || GuideUtils.getCurrentViewIsEnd(activity!!, GuideUtils.END_GROUPLIST_KEY, false)) {
            dialog_pop?.visibility = View.GONE
        }
    }
}

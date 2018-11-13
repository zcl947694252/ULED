package com.dadoutek.uled.group

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.util.DiffUtil
import android.support.v7.widget.*
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import com.app.hubert.guide.NewbieGuide
import com.app.hubert.guide.model.GuidePage
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.callback.ItemDragAndSwipeCallback
import com.chad.library.adapter.base.listener.OnItemDragListener
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.light.LightsOfGroupActivity
import com.dadoutek.uled.light.DeviceScanningNewActivity
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
import com.dadoutek.uled.util.OtherUtils
import com.telink.bluetooth.light.ConnectionStatus
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit

class GroupListFragment : BaseFragment(){

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


//     var onItemChildClickListener = { adapter, view, position ->
//
//    }

    var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        val group = showList!![position]
        val dstAddr = group.meshAddr
        var intent: Intent

        if (!dataManager!!.getConnectState(activity)) {
            return@OnItemChildClickListener
        }

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        refreshData()

        if (requestCode == 0 && resultCode == Constant.RESULT_OK) {
//            this.initData()
//            this.notifyDataSetChanged()
//            refreshData()
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
        diffResult.dispatchUpdatesTo(adapter)
        showList = mNewDatas
        adapter?.setNewData(showList)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        this.mContext = this.activity
        setHasOptionsMenu(true)

    }

    override fun onResume() {
        super.onResume()
//        this.initData()
//        this.notifyDataSetChanged()
    }

    fun lazyLoad() {
        val guide1= toolbar!!.findViewById<ImageView>(R.id.img_function1)
        val guide2= adapter!!.getViewByPosition(0,R.id.txt_name)
        val guide3= adapter!!.getViewByPosition(0,R.id.btn_on)
        val guide4= adapter!!.getViewByPosition(0,R.id.btn_off)
        val guide5= adapter!!.getViewByPosition(0,R.id.btn_set)
        NewbieGuide.with(this@GroupListFragment)
                .setLabel("add_device")
                //.alwaysShow(true)
                .addGuidePage(GuidePage.newInstance()
                        .addHighLight(guide1)
                        .setLayoutRes(R.layout.view_guide_simple)
                        .setOnLayoutInflatedListener {
                            val tv_content: TextView = it.findViewById(R.id.show_guide_content)
                            tv_content.text = getString(R.string.group_list_guide1)
                        })
                .addGuidePage(GuidePage.newInstance()
                        .addHighLight(guide2)
                        .setLayoutRes(R.layout.view_guide_simple)
                        .setOnLayoutInflatedListener {
                            val tv_content: TextView = it.findViewById(R.id.show_guide_content)
                            tv_content.text = getString(R.string.group_list_guide2)
                        })
                .addGuidePage(GuidePage.newInstance()
                        .addHighLight(guide3)
                        .setLayoutRes(R.layout.view_guide_simple)
                        .setOnLayoutInflatedListener {
                            val tv_content: TextView = it.findViewById(R.id.show_guide_content)
                            tv_content.text = getString(R.string.group_list_guide3)
                        })
                .addGuidePage(GuidePage.newInstance()
                        .addHighLight(guide4)
                        .setLayoutRes(R.layout.view_guide_simple)
                        .setOnLayoutInflatedListener {
                            val tv_content: TextView = it.findViewById(R.id.show_guide_content)
                            tv_content.text = getString(R.string.group_list_guide4)
                        })
                .addGuidePage(GuidePage.newInstance()
                        .addHighLight(guide5)
                        .setLayoutRes(R.layout.view_guide_simple)
                        .setOnLayoutInflatedListener {
                            val tv_content: TextView = it.findViewById(R.id.show_guide_content)
                            tv_content.text = getString(R.string.group_list_guide5)
                        })
                .show()
    }

    private fun initOnLayoutListener() {
        val view = activity!!.getWindow().getDecorView()
        val viewTreeObserver = view.getViewTreeObserver()
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this)
                lazyLoad()
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = getView(inflater)
        this.initData()
        return view
    }

    private fun getView(inflater: LayoutInflater): View {
        this.inflater = inflater

        val view = inflater.inflate(R.layout.fragment_group_list, null)

        toolbar = view.findViewById(R.id.toolbar)
        toolbar!!.setTitle(R.string.group_list_header)

        toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility=View.VISIBLE
        toolbar!!.findViewById<ImageView>(R.id.img_function2).visibility=View.GONE
        toolbar!!.findViewById<ImageView>(R.id.img_function1).setOnClickListener {
            showPopupMenu(it)
        }

        setHasOptionsMenu(true)

        recyclerView = view.findViewById(R.id.list_groups)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initOnLayoutListener()
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        if (isVisibleToUser) {
            val act = activity as MainActivity?
            act?.addEventListeners()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        if (!hidden) {
//            this.initData()
        }
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

//    override fun onMenuItemClick(item: MenuItem): Boolean {
//        when (item.itemId) {
//            R.id.menu_setting -> {
//                val intent = Intent(mContext, AddMeshActivity::class.java)
//                startActivity(intent)
//            }
//
//            R.id.menu_install -> showPopupMenu(toolbar!!.findViewById(R.id.menu_install))
//        }
//        return false
//    }

    private fun showPopupMenu(view: View) {
        // 这里的view代表popupMenu需要依附的view
        val popupMenu = PopupMenu(activity!!, view)
        popupMenu.menuInflater.inflate(R.menu.menu_select_device_type, popupMenu.menu)
        popupMenu.show()
        popupMenu.setOnMenuItemClickListener { item ->
            var intent:Intent? = null
            when (item.itemId) {
                R.id.popup_install_light -> {
                    if (DBUtils.allLight.size < 254) {
                        intent=Intent(mContext, DeviceScanningNewActivity::class.java)
                        intent.putExtra(Constant.IS_SCAN_RGB_LIGHT,false)
                        startActivityForResult(intent,0)
                    } else {
                        ToastUtils.showLong(getString(R.string.much_lamp_tip))
                    }
                }
                R.id.popup_install_rgb_light -> {
                    if (DBUtils.allLight.size < 254) {
                        intent=Intent(mContext, DeviceScanningNewActivity::class.java)
                        intent.putExtra(Constant.IS_SCAN_RGB_LIGHT,true)
                        startActivityForResult(intent,0)
                    } else {
                        ToastUtils.showLong(getString(R.string.much_lamp_tip))
                    }
                }
                R.id.popup_install_switch -> startActivity(Intent(mContext, ScanningSwitchActivity::class.java))
                R.id.popup_install_sensor -> startActivity(Intent(mContext, ScanningSensorActivity::class.java))
            }
            true
        }

    }
}

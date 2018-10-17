package com.dadoutek.uled.group

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.*
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.callback.ItemDragAndSwipeCallback
import com.chad.library.adapter.base.listener.OnItemDragListener
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.light.LightsOfGroupActivity
import com.dadoutek.uled.light.NormalDeviceScanningNewActivity
import com.dadoutek.uled.rgb.RGBDeviceScanningNewActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.othersview.AddMeshActivity
import com.dadoutek.uled.othersview.BaseFragment
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uled.pir.ScanningSensorActivity
import com.dadoutek.uled.rgb.RGBGroupSettingActivity
import com.dadoutek.uled.switches.ScanningSwitchActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.util.DataManager
import com.dadoutek.uled.util.OtherUtils
import com.dadoutek.uled.util.SharedPreferencesUtils
import com.telink.bluetooth.light.ConnectionStatus
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit

class GroupListFragment : BaseFragment(), Toolbar.OnMenuItemClickListener {

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
                if (OtherUtils.isRGBGroup(group) && group.meshAddr!=0xffff) {
                    intent = Intent(mContext, RGBGroupSettingActivity::class.java)
                }
                intent.putExtra("group", group)
                startActivityForResult(intent, 0)
            }
            R.id.txt_name -> {
                intent = Intent(mContext, LightsOfGroupActivity::class.java)
                intent.putExtra("group", group)
                startActivity(intent)
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
        if (requestCode == 0 && resultCode == Constant.RESULT_OK) {
            this.initData()
            this.notifyDataSetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        this.mContext = this.activity
        setHasOptionsMenu(true)

    }

    override fun onResume() {
        super.onResume()
        this.initData()
        this.notifyDataSetChanged()
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
        toolbar!!.inflateMenu(R.menu.men_group)
        toolbar!!.setOnMenuItemClickListener(this)
        if (SharedPreferencesUtils.isDeveloperModel()) {
            toolbar!!.menu.findItem(R.id.menu_setting).isVisible = false
        } else {
            toolbar!!.menu.findItem(R.id.menu_setting).isVisible = false
        }

        setHasOptionsMenu(true)

        recyclerView = view.findViewById(R.id.list_groups)
        return view
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
            this.initData()
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

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_setting -> {
                val intent = Intent(mContext, AddMeshActivity::class.java)
                startActivity(intent)
            }

            R.id.menu_install -> showPopupMenu(toolbar!!.findViewById(R.id.menu_install))
        }
        return false
    }

    private fun showPopupMenu(view: View) {
        // 这里的view代表popupMenu需要依附的view
        val popupMenu = PopupMenu(activity!!, view)
        popupMenu.menuInflater.inflate(R.menu.menu_select_device_type, popupMenu.menu)
        popupMenu.show()
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.popup_install_light -> {
                    if (DBUtils.allLight.size < 254) {
                        startActivity(Intent(mContext, NormalDeviceScanningNewActivity::class.java))
                    } else {
                        ToastUtils.showLong(getString(R.string.much_lamp_tip))
                    }
                }
                R.id.popup_install_rgb_light -> {
                    if (DBUtils.allLight.size < 254) {
                        startActivity(Intent(mContext, RGBDeviceScanningNewActivity::class.java))
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

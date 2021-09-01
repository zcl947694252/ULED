package com.dadoutek.uled.scene

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.appcompat.widget.Toolbar
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.RequiresApi
import com.app.hubert.guide.core.Builder
//import com.app.hubert.guide.util.LogUtil
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.gateway.bean.GwStompBean
import com.dadoutek.uled.gateway.util.Base64Utils
import com.dadoutek.uled.intf.CallbackLinkMainActAndFragment
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbScene
import com.dadoutek.uled.model.dbModel.DbSceneActions
import com.dadoutek.uled.model.httpModel.GwModel
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.network.GwGattBody
import com.dadoutek.uled.network.NetworkStatusCode.OK
import com.dadoutek.uled.network.NetworkStatusCode.ROUTER_ALL_OFFLINE
import com.dadoutek.uled.network.NetworkStatusCode.ROUTER_DEL_SCENEACTION_CAN_NOT_PARSE
import com.dadoutek.uled.network.NetworkStatusCode.ROUTER_DEL_SCENE_NOT_EXITE
import com.dadoutek.uled.network.NetworkStatusCode.ROUTER_DEL_SCENE_NO_GP
import com.dadoutek.uled.network.NetworkStatusCode.ROUTER_NO_EXITE
import com.dadoutek.uled.network.RouterTimeoutBean
import com.dadoutek.uled.othersview.BaseFragment
import com.dadoutek.uled.othersview.InstructionsForUsActivity
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.stomp.MqttBodyBean
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.GuideUtils
import com.dadoutek.uled.util.StringUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_switch_device_details.*
import kotlinx.android.synthetic.main.fragment_scene.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.greendao.DbUtils
import org.jetbrains.anko.support.v4.runOnUiThread
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

/**
 * Created by hejiajun on 2018/5/2.
 * 场景fragment
 */

class SceneFragment : BaseFragment(), Toolbar.OnMenuItemClickListener, View.OnClickListener {
    private var viewBottom12: View? = null
    private var addNewScene: TextView? = null
    private var emptyAdd: Button? = null
    private var disposableRouteTimer: Disposable? = null
    private var currentDbScene: DbScene? = null
    private var goHelp: TextView? = null
    private var disposableTimer: Disposable? = null
    private lateinit var viewContent: View
    private var inflater: LayoutInflater? = null
    private var adaper: SceneRecycleListAdapter? = null
    private var toolbar: Toolbar? = null
    private var telinkLightApplication: TelinkLightApplication? = null
    private var scenesListData: MutableList<DbScene> = ArrayList()
    private var isDelete = false
    internal var builder: Builder? = null
    private var recyclerView: androidx.recyclerview.widget.RecyclerView? = null
    private var isGuide = false
    private var isRgbClick = false
    private var install_device: TextView? = null
    private var create_group: TextView? = null
    private var create_scene: TextView? = null

    override fun tzRouterApplyScenes(cmdBean: CmdBodyBean) {
        if (cmdBean.ser_id == "applyScene") {
            LogUtils.v("zcl-----------收到路由场景应用通知-------$cmdBean")
            disposableRouteTimer?.dispose()
            runOnUiThread {
                hideLoadingDialog()
                when (cmdBean.status) {
                    0 -> ToastUtils.showShort(getString(R.string.scene_apply_success))
                    else -> ToastUtils.showShort(getString(R.string.scene_apply_fail))
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    internal var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        if (position>scenesListData.size)
            return@OnItemChildClickListener
        currentDbScene = scenesListData[position]
        if (dialog_pop.visibility == View.GONE || dialog_pop == null) {
            Log.e("zcl场景", "zcl场景******onItemChildClickListener")
            when (view.id) {
                R.id.template_device_card_delete -> {
                    showDeleteSingleDialog(currentDbScene!!)
                }//scenesListData!![position].isSelected = !scenesListData!![position].isSelected

                R.id.template_device_setting -> {
                    if (TelinkLightApplication.getApp().connectDevice == null && !Constant.IS_ROUTE_MODE) {
                        ToastUtils.showLong(activity!!.getString(R.string.device_not_connected))
                    } else {
                        val lastUser = DBUtils.lastUser
                        lastUser?.let {
                            if (it.id.toString() != it.last_authorizer_user_id)
                                ToastUtils.showLong(getString(R.string.author_region_warm))
                            else {
                                Log.e("zcl场景", "zcl场景******scene_edit")
                                val scene = scenesListData[position]
                                val intent = Intent(activity, NewSceneSetAct::class.java)
                                intent.putExtra(Constant.CURRENT_SELECT_SCENE, scene)
                                intent.putExtra(Constant.IS_CHANGE_SCENE, true)

                                startActivityForResult(intent, 3)
                            }
                        }
                    }
                }
                R.id.template_device_icon -> {
                    Log.e("zcl场景", "zcl场景******scene_apply")
                    if (Constant.IS_ROUTE_MODE) {
                        try {
                            when {
                                position < adapter.data.size -> {
                                    val dbScene = scenesListData[position]
                                    when {
                                        Constant.IS_ROUTE_MODE -> {
                                            RouterModel.routeApplyScene(dbScene.id, "applyScene")?.subscribe({
                                                //    "errorCode": 90011,message": "场景不存在，请刷新场景数据"
                                                LogUtils.v("zcl-----------收到路由场景应用请求-------$dbScene")
                                                when (it.errorCode) {
                                                    0 -> {
                                                        showLoadingDialog(getString(R.string.please_wait))
                                                        disposableRouteTimer?.dispose()
                                                        disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                                                                .subscribeOn(Schedulers.io())
                                                                .observeOn(AndroidSchedulers.mainThread())
                                                                .subscribe {
                                                                    hideLoadingDialog()
                                                                    ToastUtils.showShort(getString(R.string.scene_apply_fail))
                                                                }
                                                    }
                                                    90011 -> ToastUtils.showShort(getString(R.string.scene_cont_exit_to_refresh))
                                                    90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                                                    90004 -> ToastUtils.showShort(getString(R.string.region_no_router))
                                                    else -> ToastUtils.showShort(it.message)
                                                }
                                            }, {
                                                ToastUtils.showShort(it.message)
                                            })
                                        }
                                        else -> {
                                            when (TelinkLightApplication.getApp().connectDevice) {
                                                null -> {
                                                    sendToGw(dbScene)
                                                }
                                                else -> setScene(dbScene.id!!)
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        when (TelinkLightApplication.getApp().connectDevice) {
                            null -> sendToGw(scenesListData[position])
                            else -> {
                                try {
                                    if (position < adapter.data.size) {
//                                        LogUtils.v("================chown=======${scenesListData[position].id!!}")
                                        setScene(scenesListData[position].id!!)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showDeleteSingleDialog(dbScene: DbScene) {
        val builder = AlertDialog.Builder(activity)
        builder.setMessage(R.string.sure_delete)
        builder.setPositiveButton(activity!!.getString(android.R.string.ok)) { _, _ ->
            val id = dbScene.id!!
            val list = DBUtils.getActionsBySceneId(id)
            if (Constant.IS_ROUTE_MODE) {
                RouterModel.routeDelScene(id.toInt())
                        ?.subscribe({
                            when (it.errorCode) {
                                OK->{
                                    startDelSceneTimeOut(it.t)
                                }
                                ROUTER_DEL_SCENE_NOT_EXITE, ROUTER_DEL_SCENEACTION_CAN_NOT_PARSE, ROUTER_DEL_SCENE_NO_GP -> deleteSceneSuccess(list, dbScene)
                                //该账号该区域下没有路由，无法操作 ROUTER_NO_EXITE= 90004
                                // 以下路由没有上线，无法删除场景  ROUTER_ALL_OFFLINE= 90005
                                ROUTER_NO_EXITE -> ToastUtils.showShort(getString(R.string.region_no_router))
                                ROUTER_ALL_OFFLINE -> ToastUtils.showShort(getString(R.string.router_offline))
                                else -> ToastUtils.showShort(it.message)
                            }
                        }, {
                            ToastUtils.showShort(it.message)
                        })
            } else {
                val opcode = Opcode.SCENE_ADD_OR_DEL
                Thread.sleep(300)
                val params: ByteArray = byteArrayOf(0x00, id.toByte())
                Thread { TelinkLightService.Instance()?.sendCommandNoResponse(opcode, 0xFFFF, params) }.start()
                deleteSceneSuccess(list, dbScene)
            }
        }
        builder.setNegativeButton(activity!!.getString(R.string.cancel)) { _, _ -> }
        val dialog = builder.show()
        dialog.show()
    }

    private fun startDelSceneTimeOut(it: RouterTimeoutBean?) {
        val i = it?.timeout ?: 0
        disposableRouteTimer?.dispose()
        disposableRouteTimer = Observable.timer(i.toLong()+2L, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    LogUtils.v("zcl-----------收到路由超时大汗出失败-------$i")
                    ToastUtils.showShort(getString(R.string.delete_scene_fail))
                }
    }

    private fun deleteSceneSuccess(list: ArrayList<DbSceneActions>, dbScene: DbScene) {
        DBUtils.deleteSceneActionsList(list)
        DBUtils.deleteScene(dbScene)
        adaper?.data?.remove(dbScene)
        scenesListData.remove(dbScene)
        adaper?.notifyDataSetChanged()
        //refreshAllData()
       // refreshView()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendToGw(dbScene: DbScene) {
        if (DBUtils.getAllGateWay().size > 0)
            GwModel.getGwList()?.subscribe({
                TelinkLightApplication.getApp().offLine = true
                hideLoadingDialog()
                it.forEach { db ->
                    //网关在线状态，1表示在线，0表示离线
                    if (db.state == 1)
                        TelinkLightApplication.getApp().offLine = false
                }

                if (!TelinkLightApplication.getApp().offLine) {
                    disposableTimer?.dispose() // rxjava 解除订阅
                    disposableTimer = Observable.timer(7000, TimeUnit.MILLISECONDS)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread()).subscribe {
                                hideLoadingDialog()
                                ToastUtils.showShort(getString(R.string.gate_way_offline))
                            }
                    val gattPar = byteArrayOf(0x11, 0x11, 0x11, 0, 0, 0xff.toByte(), 0xff.toByte(), Opcode.SCENE_LOAD, 0x11, 0x02,
                            dbScene.id.toByte(), 0, 0, 0, 0, 0, 0, 0, 0, 0)

                    val gattBody = GwGattBody()
                    gattBody.ser_id = Constant.SER_ID_SCENE_ON
                    val s = Base64Utils.encodeToStrings(gattPar)
                    gattBody.data = s
                    gattBody.cmd = Constant.CMD_MQTT_CONTROL
                    gattBody.meshAddr = Constant.SER_ID_SCENE_ON
                    sendToServer(gattBody)
                } else {
                    ToastUtils.showShort(getString(R.string.gw_not_online))
                    LogUtils.v("zcl-----------$it-------")
                }
            }, {
                hideLoadingDialog()
                ToastUtils.showShort(getString(R.string.gw_not_online))
            })
    }

    @SuppressLint("CheckResult")
    private fun sendToServer(gattBody: GwGattBody) {
        GwModel.sendDeviceToGatt(gattBody)?.subscribe({
            disposableTimer?.dispose()
            LogUtils.v("zcl-----------远程控制-------$it")
        }, {
            disposableTimer?.dispose()
            ToastUtils.showShort(it.message)
            LogUtils.v("zcl-----------远程控制-------${it.message}")
        })
    }


    @SuppressLint("InflateParams")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        this.inflater = inflater
        viewContent = inflater.inflate(R.layout.fragment_scene, null)
        recyclerView = viewContent.findViewById(R.id.recyclerView)
        viewBottom12 = viewContent.findViewById(R.id.view12)

        install_device = viewContent.findViewById(R.id.install_device)
        create_group = viewContent.findViewById(R.id.create_group)
        create_scene = viewContent.findViewById(R.id.create_scene)
        install_device?.setOnClickListener(onClick)
        create_group?.setOnClickListener(onClick)
        create_scene?.setOnClickListener(onClick)

        initToolBar(viewContent)
        initData()
        initView()
        return viewContent
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
                    //addNewGroup()
                    popMain.showAtLocation(viewContent, Gravity.CENTER, 0, 0)
                }
            }
            R.id.create_scene -> {
                addNewScenes()
            }
        }
    }

    private fun addNewGroup() {
        val textGp = EditText(activity)
        StringUtils.initEditTextFilter(textGp)
        textGp.setText(DBUtils.getDefaultNewGroupName())
        //设置光标默认在最后
        textGp.setSelection(textGp.text.toString().length)
        AlertDialog.Builder(activity)
                .setTitle(R.string.create_new_group)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(textGp)

                .setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
                    // 获取输入框的内容
                    if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                        ToastUtils.showLong(getString(R.string.rename_tip_check))
                    } else {
                        //往DB里添加组数据
                        DBUtils.addNewGroupWithType(textGp.text.toString().trim { it <= ' ' }, Constant.DEVICE_TYPE_DEFAULT_ALL)
                        callbackLinkMainActAndFragment?.changeToGroup()
                        dialog.dismiss()
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, _ -> dialog.dismiss() }.show()
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                val inputManager = textGp.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputManager.showSoftInput(textGp, 0)
            }
        }, 200)
    }


    private fun initToolBar(view: View) {
        setHasOptionsMenu(true)
        toolbar = view.findViewById(R.id.toolbar)
        toolbarTv?.setText(R.string.scene_name)

        val btnAdd = toolbar?.findViewById<ImageView>(R.id.img_function1)
        val btnDelete = toolbar?.findViewById<ImageView>(R.id.img_function2)
        val orderScene = toolbar?.findViewById<TextView>(R.id.order_scene)
//        orderScene?.visibility=View.VISIBLE // chown
        btnAdd?.visibility = View.GONE

        btnAdd?.setOnClickListener(this)
        btnDelete?.setOnClickListener(this)
        orderScene?.setOnClickListener(this)
    }


    private fun initOnLayoutListener() {
        if (activity != null) {
            val view = activity!!.window.decorView
            val viewTreeObserver = view.viewTreeObserver
            viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })
        }
    }

    private fun initData() {
        telinkLightApplication = this.activity!!.application as TelinkLightApplication
        scenesListData.clear()
        scenesListData = DBUtils.sceneList

        img_function2?.visibility = View.GONE
        toolbar?.navigationIcon = null
        image_bluetooth?.visibility = View.VISIBLE
        img_function1?.visibility = View.GONE
        toolbarTv?.setText(R.string.scene_name)
    }

    private fun initView() {
        recyclerView?.layoutManager = GridLayoutManager(activity, 2)

        adaper = SceneRecycleListAdapter(R.layout.template_device_type_item, scenesListData, isDelete)
        adaper?.onItemChildClickListener = onItemChildClickListener
        adaper?.onItemLongClickListener = onItemChildLongClickListener
        adaper?.bindToRecyclerView(recyclerView)

        val footer = View.inflate(context, R.layout.template_add_help, null)
        addNewScene = footer.findViewById(R.id.main_add_device)
        addNewScene?.text = getString(R.string.create_scene)
        goHelp = footer.findViewById(R.id.main_go_help)
        addNewScene?.setOnClickListener(this)
        goHelp?.setOnClickListener(this)


        val emptyView = View.inflate(context, R.layout.empty_view, null)
         emptyAdd = emptyView.findViewById(R.id.add_device_btn)
        emptyAdd?.text = getString(R.string.create_scene)
        emptyAdd?.setOnClickListener(this)
        adaper?.addFooterView(footer)
        adaper?.emptyView = emptyView

        isDelete = false
        adaper!!.changeState(isDelete)
        for (i in scenesListData.indices) {
            if (scenesListData[i].isSelected)
                scenesListData[i].isSelected = false
        }
        adaper?.notifyDataSetChanged()
        recyclerView?.scrollToPosition(scenesListData.size)
        viewBottom12?.requestFocus()
        LogUtils.v("zcl-----------滑动到-------${scenesListData.size}")
    }

    private var onItemChildLongClickListener = BaseQuickAdapter.OnItemLongClickListener { _, _, _ ->
        val lastUser = DBUtils.lastUser
        lastUser?.let {
            if (it.id.toString() != it.last_authorizer_user_id)
                ToastUtils.showLong(getString(R.string.author_region_warm))
            else {
                isDelete = !isDelete
                adaper!!.changeState(isDelete)
                img_function1?.visibility = View.GONE
                img_function2?.visibility = View.GONE
                image_bluetooth?.visibility = View.GONE
                setBack()
                adaper!!.notifyDataSetChanged()
            }
        }
        return@OnItemLongClickListener true
    }

    private fun setBack() {
        toolbar?.setNavigationIcon(R.drawable.icon_return)
        toolbar?.setNavigationOnClickListener {
            //toolbar?.setTitle(R.string.scene_name)
            toolbarTv?.setText(R.string.scene_name)
            img_function2?.visibility = View.GONE
            toolbar?.navigationIcon = null
            image_bluetooth?.visibility = View.VISIBLE
            img_function1?.visibility = View.GONE
            isDelete = false
            adaper!!.changeState(isDelete)
            for (i in scenesListData.indices) {
                if (scenesListData[i].isSelected) {
                    scenesListData[i].isSelected = false
                }
            }
            adaper!!.notifyDataSetChanged()
        }
    }

    private fun showDeleteDialog() {
        val builder = AlertDialog.Builder(activity)
        builder.setMessage(R.string.sure_delete)
        builder.setPositiveButton(activity!!.getString(android.R.string.ok)) { _, _ ->
            Log.e("TAG_SIZE", scenesListData.size.toString())
            for (i in scenesListData.indices) {
                if (scenesListData[i].isSelected) {
                    val opcode = Opcode.SCENE_ADD_OR_DEL
                    val params: ByteArray
                    if (scenesListData.size > 0) {
                        Thread.sleep(300)
                        val id = scenesListData[i].id!!
                        val list = DBUtils.getActionsBySceneId(id)
                        params = byteArrayOf(0x00, id.toByte())
                        Thread { TelinkLightService.Instance()?.sendCommandNoResponse(opcode, 0xFFFF, params) }.start()
                        DBUtils.deleteSceneActionsList(list)
                        DBUtils.deleteScene(scenesListData[i])
                    }
                }
            }
            refreshAllData()
            refreshView()
        }
        builder.setNegativeButton(activity!!.getString(R.string.cancel)) { _, _ -> }
        val dialog = builder.show()
        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        initData()
        initView()
    }

    private fun setScene(id: Long) {
        val opcode = Opcode.SCENE_LOAD


        GlobalScope.launch {
            val params: ByteArray = byteArrayOf(id.toByte())
            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, 0xFFFF, params)
            delay(80)

            DBUtils.getSceneByID(id)?.actions?.forEach {
                if (it.groupAddr == 0xffff) {
                    Commander.openOrCloseLights(it.groupAddr, it.isOn)
                }
            }
        }



        //ToastUtils.showShort(activity, getString(R.string.scene_apply_success))
        ToastUtils.showShort(getString(R.string.scene_apply_success))
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser) {
            refreshAllData()
            refreshView()
            initOnLayoutListener()
        } else {
            refreshView()
        }
    }

    fun refreshView() {
        if (activity != null) {
            scenesListData = DBUtils.sceneList

            toolbar?.let {
                it.navigationIcon = null
                toolbarTv?.setText(R.string.scene_name)
            }
            img_function2?.visibility = View.GONE
            image_bluetooth?.visibility = View.VISIBLE
            img_function1?.visibility = View.GONE

            isDelete = false
            adaper?.changeState(isDelete)
            for (i in scenesListData.indices) {
                if (scenesListData[i].isSelected) {
                    scenesListData[i].isSelected = false
                }
            }
            adaper?.notifyDataSetChanged()
        }
    }

    private fun refreshAllData() {
        val mOldDatas = scenesListData
        val mNewDatas = loadData()

        if (mOldDatas != null && mNewDatas != null) {

            val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int {
                    return mOldDatas.size
                }

                override fun getNewListSize(): Int {
                    return mNewDatas.size
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return mOldDatas[oldItemPosition].id == mNewDatas[newItemPosition].id
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val beanOld = mOldDatas[oldItemPosition]
                    val beanNew = mNewDatas[newItemPosition]
                    return if (beanOld.name != beanNew.name) false else false
                }
            }, true)

            scenesListData = mNewDatas
            adaper!!.setNewData(scenesListData)
            diffResult.dispatchUpdatesTo(adaper!!)
        }
    }

    private fun loadData(): MutableList<DbScene> {
        val showList: List<DbScene>
        showList = DBUtils.sceneList
        return showList
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        val instance = TelinkLightService.Instance()
        when (item.itemId) {
            R.id.menu_install -> if (!instance.isLogin) {
            } else {
                if (scenesListData.size >= SCENE_MAX_COUNT) {
                    ToastUtils.showLong(R.string.scene_16_tip)
                } else {
                    val intent = Intent(activity, NewSceneSetAct::class.java)
                    intent.putExtra(Constant.IS_CHANGE_SCENE, false)
                    startActivityForResult(intent, 3)
                }
            }
        }
        return false
    }

    override fun onClick(v: View) {
        val lastUser = DBUtils.lastUser
        lastUser?.let {
            when {
                it.id.toString() != it.last_authorizer_user_id -> ToastUtils.showLong(getString(R.string.author_region_warm))
                else -> {
                    when (v.id) {
                        R.id.img_function2 -> showDeleteDialog()
                        R.id.img_function1 -> {
                            when {
                                it.id.toString() != it.last_authorizer_user_id -> ToastUtils.showLong(getString(R.string.author_region_warm))
                                else -> {
                                    isGuide = false
                                    when (dialog_pop?.visibility) {
                                        View.GONE -> showPopupMenu()
                                        else -> hidePopupMenu()
                                    }
                                }
                            }
                        }
                        R.id.order_scene -> { //chown
                            val intent = Intent(activity,SceneSortActivity::class.java)

                            startActivityForResult(intent,4)
                        }
                        R.id.add_device_btn , R.id.main_add_device -> addNewScenes()
                        R.id.main_go_help -> seeHelpe()
                    }
                }
            }
        }
    }

    private fun addNewScenes() {
        val nowSize = DBUtils.sceneList.size

        if (TelinkLightApplication.getApp().connectDevice == null && !Constant.IS_ROUTE_MODE) {
            ToastUtils.showLong(activity!!.getString(R.string.device_not_connected))
        } else {
            if (nowSize >= SCENE_MAX_COUNT) {
                ToastUtils.showLong(R.string.scene_16_tip)
            } else {
                val intent = Intent(activity, NewSceneSetAct::class.java)
                intent.putExtra(Constant.IS_CHANGE_SCENE, false)
                startActivityForResult(intent, 3)
            }
        }
    }

    private fun seeHelpe() {
        val intent = Intent(context, InstructionsForUsActivity::class.java)
        intent.putExtra(Constant.WB_TYPE, "#control-scene")
        startActivity(intent)
    }

    private fun showPopupMenu() {
        dialog_pop?.visibility = View.VISIBLE
    }

    private fun hidePopupMenu() {
        if (!isGuide || GuideUtils.getCurrentViewIsEnd(activity!!, GuideUtils.END_GROUPLIST_KEY, false)) {
            dialog_pop?.visibility = View.GONE
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

    companion object {
        private const val SCENE_MAX_COUNT = 255
    }

    override fun receviedGwCmd2500(gwStompBean: GwStompBean) {
        when (gwStompBean.ser_id.toInt()) {
            Constant.SER_ID_SCENE_ON -> {
                LogUtils.v("zcl-----------远程控制场景开启成功-------")
                ToastUtils.showShort(getString(R.string.scene_apply_success))
                disposableTimer?.dispose()
                hideLoadingDialog()
            }
        }
    }

    override fun receviedGwCmd2500M(gwStompBean: MqttBodyBean) {
        when (gwStompBean.ser_id.toInt()) {
            Constant.SER_ID_SCENE_ON -> {
                LogUtils.v("zcl-----------远程控制场景开启成功-------")
                ToastUtils.showShort(getString(R.string.scene_apply_success))
                disposableTimer?.dispose()
                hideLoadingDialog()
            }
        }
    }

    override fun tzRouterDelSceneResult(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl-----------收到路由删除回调-------$cmdBean")
        disposableRouteTimer?.dispose()
        hideLoadingDialog()
        if (cmdBean.finish && cmdBean.status == 0) {////1 0 -1  部分成功 成功 失败
            currentDbScene?.let {
                val list = DBUtils.getActionsBySceneId(it.id)
                deleteSceneSuccess(list, it)
            }
        } else {
            ToastUtils.showShort(cmdBean.msg)
        }
    }
}

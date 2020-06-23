package com.dadoutek.uled.scene

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.app.hubert.guide.core.Builder
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.gateway.bean.DbGateway
import com.dadoutek.uled.gateway.bean.GwStompBean
import com.dadoutek.uled.intf.CallbackLinkMainActAndFragment
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbScene
import com.dadoutek.uled.model.HttpModel.GwModel
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.network.GwGattBody
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.othersview.BaseFragment
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.GuideUtils
import com.dadoutek.uled.util.StringUtils
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_scene.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by hejiajun on 2018/5/2.
 * 场景fragment
 */

class SceneFragment : BaseFragment(), Toolbar.OnMenuItemClickListener, View.OnClickListener {
    private var disposableTimer: Disposable? = null
    private lateinit var viewContent: View
    private var inflater: LayoutInflater? = null
    private var adaper: SceneRecycleListAdapter? = null
    private var toolbar: Toolbar? = null
    private var telinkLightApplication: TelinkLightApplication? = null
    private var scenesListData: MutableList<DbScene> = ArrayList()
    private var isDelete = false
    internal var builder: Builder? = null
    private var recyclerView: RecyclerView? = null
    private var no_scene: ConstraintLayout? = null
    private var isGuide = false
    private var isRgbClick = false
    private var add_scenes: Button? = null
    private var addNewScene: ConstraintLayout? = null
    private var install_device: TextView? = null
    private var create_group: TextView? = null
    private var create_scene: TextView? = null

    internal var onItemClickListener = BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
        try {
            if (position < adapter.data.size) {
                if (TelinkLightApplication.getApp().connectDevice == null) {
                    sendToGw(scenesListData!![position])
                } else {
                    setScene(scenesListData!![position].id!!)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    internal var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        if (dialog_pop.visibility == View.GONE || dialog_pop == null) {
            Log.e("zcl场景", "zcl场景******onItemChildClickListener")
            when (view.id) {
                R.id.scene_delete -> scenesListData!![position].isSelected = !scenesListData!![position].isSelected

                R.id.scene_edit -> {
                    if (TelinkLightApplication.getApp().connectDevice == null) {
                        ToastUtils.showLong(activity!!.getString(R.string.device_not_connected))
                    } else {
                        val lastUser = DBUtils.lastUser
                        lastUser?.let {
                            if (it.id.toString() != it.last_authorizer_user_id)
                                ToastUtils.showLong(getString(R.string.author_region_warm))
                            else {
                                Log.e("zcl场景", "zcl场景******scene_edit")
                                val scene = scenesListData!![position]
                                val intent = Intent(activity, NewSceneSetAct::class.java)
                                intent.putExtra(Constant.CURRENT_SELECT_SCENE, scene)
                                intent.putExtra(Constant.IS_CHANGE_SCENE, true)
                                startActivityForResult(intent, 3)
                            }
                        }
                    }
                }
                R.id.scene_apply -> {
                    Log.e("zcl场景", "zcl场景******scene_apply")
                    if (TelinkLightApplication.getApp().connectDevice == null) {
                        //ToastUtils.showLong(activity!!.getString(R.string.device_not_connected))
                        sendToGw(scenesListData!![position])
                    } else {
                        try {
                            if (position < adapter.data.size) {
                                setScene(scenesListData!![position].id!!)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private fun sendToGw(dbScene: DbScene) {
        if (DBUtils.getAllGateWay().size > 0)
            GwModel.getGwList()?.subscribe(object : NetworkObserver<List<DbGateway>?>() {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onNext(t: List<DbGateway>) {
                    TelinkLightApplication.getApp().offLine = true
                    hideLoadingDialog()
                    t.forEach { db ->
                        //网关在线状态，1表示在线，0表示离线
                        if (db.state == 1)
                            TelinkLightApplication.getApp().offLine = false
                    }

                    if (!TelinkLightApplication.getApp().offLine) {
                        disposableTimer?.dispose()
                        disposableTimer = Observable.timer(7000, TimeUnit.MILLISECONDS).subscribe {
                            hideLoadingDialog()
                            ToastUtils.showShort(getString(R.string.gate_way_offline))
                        }
                        var gattPar = byteArrayOf(0x11, 0x11, 0x11, 0, 0, 0xff.toByte(), 0xff.toByte(), Opcode.SCENE_LOAD, 0x11, 0x02,
                                dbScene.id.toByte(), 0, 0, 0, 0, 0, 0, 0, 0, 0)

                        val gattBody = GwGattBody()
                        gattBody.ser_id = Constant.SER_ID_SCENE_ON

                        val encoder = Base64.getEncoder()
                        val s = encoder.encodeToString(gattPar)
                        gattBody.data = s
                        gattBody.cmd = Constant.CMD_MQTT_CONTROL
                        gattBody.meshAddr = Constant.SER_ID_SCENE_ON
                        sendToServer(gattBody)
                    } else {
                        ToastUtils.showShort(getString(R.string.gw_not_online))
                    }
                }

                override fun onError(e: Throwable) {
                    super.onError(e)
                    hideLoadingDialog()
                    ToastUtils.showShort(getString(R.string.gw_not_online))
                }
            })
    }

    private fun sendToServer(gattBody: GwGattBody) {
        GwModel.sendDeviceToGatt(gattBody)?.subscribe(object : NetworkObserver<String?>() {
            override fun onNext(t: String) {
                disposableTimer?.dispose()
                LogUtils.v("zcl-----------远程控制-------$t")
            }

            override fun onError(e: Throwable) {
                super.onError(e)
                disposableTimer?.dispose()
                ToastUtils.showShort(e.message)
                LogUtils.v("zcl-----------远程控制-------${e.message}")
            }
        })
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.inflater = inflater
        viewContent = inflater.inflate(R.layout.fragment_scene, null)
        recyclerView = viewContent.findViewById(R.id.recyclerView)
        no_scene = viewContent.findViewById(R.id.no_scene)
        add_scenes = viewContent.findViewById(R.id.add_scenes)

        install_device = viewContent.findViewById(R.id.install_device)
        create_group = viewContent.findViewById(R.id.create_group)
        create_scene = viewContent.findViewById(R.id.create_scene)
        addNewScene = viewContent.findViewById(R.id.add_new_scene)
        install_device?.setOnClickListener(onClick)
        create_group?.setOnClickListener(onClick)
        create_scene?.setOnClickListener(onClick)

        add_scenes!!.setOnClickListener(this)
        addNewScene!!.setOnClickListener(this)

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
                val nowSize = DBUtils.sceneList.size
                if (TelinkLightApplication.getApp().connectDevice == null) {
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

                .setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
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
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()
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
        toolbar?.setTitle(R.string.scene_name)

        val btn_add = toolbar?.findViewById<ImageView>(R.id.img_function1)
        val btn_delete = toolbar?.findViewById<ImageView>(R.id.img_function2)

        btn_add?.visibility = View.VISIBLE

        btn_add?.setOnClickListener(this)
        btn_delete?.setOnClickListener(this)
    }

    private fun stepEndGuide2() {
        if (activity != null) {
            val view = activity!!.window.decorView
            val viewTreeObserver = view.viewTreeObserver
            viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    val guide2 = adaper!!.getViewByPosition(0, R.id.scene_edit) as TextView?
                    GuideUtils.guideBuilder(this@SceneFragment, GuideUtils.ADDITIONAL_GUIDE_SET_SCENE)
                            .addGuidePage(GuideUtils.addGuidePage(guide2!!, R.layout.view_guide_simple_scene_2, getString(R.string.click_update_scene),
                                    View.OnClickListener { v -> GuideUtils.changeCurrentViewIsEnd(activity!!, GuideUtils.END_ADD_SCENE_SET_KEY, true) }, GuideUtils.END_ADD_SCENE_SET_KEY, activity!!)).show()
                }
            })
        }
    }

    private fun stepEndGuide1() {
        if (activity != null && adaper!!.itemCount > 0) {
            val view = activity!!.window.decorView
            val viewTreeObserver = view.viewTreeObserver
            viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    val guide2 = adaper!!.getViewByPosition(0, R.id.scene_apply) as TextView?
                    if (guide2 != null) {//guide2代表拿取此处view的锚点区域  不代表使用该view
                        GuideUtils.guideBuilder(this@SceneFragment, GuideUtils.STEP14_GUIDE_APPLY_SCENE)
                                .addGuidePage(GuideUtils.addGuidePage(guide2, R.layout.view_guide_simple_scene_2, getString(R.string.apply_scene),
                                        View.OnClickListener { v -> stepEndGuide2() }, GuideUtils.END_ADD_SCENE_SET_KEY, activity!!)).show()
                    }
                }
            })
        }
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
        scenesListData = DBUtils.sceneList

        img_function2?.visibility = View.GONE
        toolbar?.navigationIcon = null
        image_bluetooth?.visibility = View.VISIBLE
        img_function1?.visibility = View.VISIBLE
        toolbar?.setTitle(R.string.scene_name)
    }

    private fun initView() {
        if (scenesListData.size > 0) {
            recyclerView!!.visibility = View.VISIBLE
            no_scene!!.visibility = View.GONE
            addNewScene!!.visibility = View.VISIBLE
        } else {
            recyclerView!!.visibility = View.GONE
            no_scene!!.visibility = View.VISIBLE
            addNewScene!!.visibility = View.GONE
        }
        recyclerView!!.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)

        adaper = SceneRecycleListAdapter(R.layout.item_scene, scenesListData, isDelete)
        adaper!!.onItemClickListener = onItemClickListener
        adaper!!.onItemChildClickListener = onItemChildClickListener
        adaper!!.onItemLongClickListener = onItemChildLongClickListener
        adaper!!.bindToRecyclerView(recyclerView)

        isDelete = false
        adaper!!.changeState(isDelete)
        for (i in scenesListData.indices) {
            if (scenesListData[i].isSelected) {
                scenesListData[i].isSelected = false
            }
        }
        adaper!!.notifyDataSetChanged()
    }

    var onItemChildLongClickListener = BaseQuickAdapter.OnItemLongClickListener { adapter, view, position ->
        val lastUser = DBUtils.lastUser
        lastUser?.let {
            if (it.id.toString() != it.last_authorizer_user_id)
                ToastUtils.showLong(getString(R.string.author_region_warm))
            else {
                isDelete = true
                adaper!!.changeState(isDelete)
                img_function1?.visibility = View.GONE
                img_function2?.visibility = View.VISIBLE
                image_bluetooth?.visibility = View.GONE
                toolbar?.title = ""
                setBack()
                adaper!!.notifyDataSetChanged()
            }
        }
        return@OnItemLongClickListener true
    }

    private fun setBack() {
        toolbar?.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar?.setNavigationOnClickListener {
            toolbar?.setTitle(R.string.scene_name)
            img_function2?.visibility = View.GONE
            toolbar?.navigationIcon = null
            image_bluetooth?.visibility = View.VISIBLE
            img_function1?.visibility = View.VISIBLE
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
        builder.setPositiveButton(activity!!.getString(android.R.string.ok)) { dialog, which ->
            Log.e("TAG_SIZE", scenesListData.size.toString())
            for (i in scenesListData.indices) {
                if (scenesListData[i].isSelected) {
                    val opcode = Opcode.SCENE_ADD_OR_DEL
                    val params: ByteArray
                    if (scenesListData!!.size > 0) {
                        Thread.sleep(300)
                        val id = scenesListData!![i].id!!
                        val list = DBUtils.getActionsBySceneId(id)
                        params = byteArrayOf(0x00, id.toByte())
                        Thread { TelinkLightService.Instance()?.sendCommandNoResponse(opcode, 0xFFFF, params) }.start()
                        DBUtils.deleteSceneActionsList(list)
                        DBUtils.deleteScene(scenesListData!![i])
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

    override fun onResume() {
        super.onResume()
        if (GuideUtils.getCurrentViewIsEnd(activity!!, GuideUtils.END_ADD_SCENE_KEY, false)) {
            stepEndGuide1()
        }
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
        }

        //ToastUtils.showShort(activity, getString(R.string.scene_apply_success))

        ToastUtils.showShort(getString(R.string.scene_apply_success))
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        builder = GuideUtils.guideBuilder(this, Constant.TAG_SceneFragment)
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

            if (scenesListData.size > 0) {
                if (recyclerView != null) {
                    recyclerView!!.visibility = View.VISIBLE
                    no_scene!!.visibility = View.GONE
                    addNewScene!!.visibility = View.VISIBLE
                }
            } else {
                if (recyclerView != null) {
                    recyclerView!!.visibility = View.GONE
                    no_scene!!.visibility = View.VISIBLE
                    addNewScene!!.visibility = View.GONE
                }
            }

            toolbar?.let {
                it.navigationIcon = null
                it.setTitle(R.string.scene_name)
            }
            img_function2?.visibility = View.GONE
            image_bluetooth?.visibility = View.VISIBLE
            img_function1?.visibility = View.VISIBLE

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
                    return mOldDatas!!.size
                }

                override fun getNewListSize(): Int {
                    return mNewDatas.size
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return mOldDatas!![oldItemPosition].id == mNewDatas[newItemPosition].id
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val beanOld = mOldDatas!![oldItemPosition]
                    val beanNew = mNewDatas[newItemPosition]
                    return if (beanOld.name != beanNew.name) {
                        false
                    } else
                        false
                }
            }, true)

            scenesListData = mNewDatas
            adaper!!.setNewData(scenesListData)
            diffResult.dispatchUpdatesTo(adaper!!)
        }

    }

    private fun loadData(): MutableList<DbScene> {
        var showList: List<DbScene>
        showList = DBUtils.sceneList
        return showList
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        val instance = TelinkLightService.Instance()
        when (item.itemId) {
            R.id.menu_install -> if (!instance.isLogin) {
            } else {
                if (scenesListData!!.size >= SCENE_MAX_COUNT) {
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
            if (it.id.toString() != it.last_authorizer_user_id)
                ToastUtils.showLong(getString(R.string.author_region_warm))
            else {

                when (v.id) {
                    R.id.img_function2 -> {
                        showDeleteDialog()
                    }
                    R.id.img_function1 -> {
                        if (it.id.toString() != it.last_authorizer_user_id)
                            ToastUtils.showLong(getString(R.string.author_region_warm))
                        else {
                            isGuide = false
                            if (dialog_pop?.visibility == View.GONE) {
                                showPopupMenu()
                            } else {
                                hidePopupMenu()
                            }
                        }
                    }

                    R.id.add_scenes -> {

                        val nowSize = DBUtils.sceneList.size
                        if (TelinkLightApplication.getApp().connectDevice == null) {
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

                    R.id.add_new_scene -> {
                        val nowSize = DBUtils.sceneList.size
                        if (TelinkLightApplication.getApp().connectDevice == null) {
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
                }
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
        private const val SCENE_MAX_COUNT = 100
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
}

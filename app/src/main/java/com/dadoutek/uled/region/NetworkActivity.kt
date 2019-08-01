package com.dadoutek.uled.region


import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.adapter.AreaItemAdapter
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbRegion
import com.dadoutek.uled.model.HttpModel.AccountModel
import com.dadoutek.uled.model.HttpModel.RegionModel
import com.dadoutek.uled.util.NetWorkUtils
import com.dadoutek.uled.util.PopUtil
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import kotlinx.android.synthetic.main.activity_network.*
import kotlinx.android.synthetic.main.toolbar.*


class NetworkActivity : AppCompatActivity(), View.OnClickListener {
    private  var loadDialog: Dialog? = null
    private lateinit var popAdd: PopupWindow
    private var viewAdd: View? = null
    var adapter: AreaItemAdapter? = null
    var list: MutableList<DbRegion>? = null
    var pop: PopupWindow? = null
    var view: View? = null
    var root: View? = null
    var isFrist: Boolean = true
    var dbRegion: DbRegion? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network)
        root = View.inflate(this, R.layout.activity_network, null)
        list = ArrayList()
        initView()
        initData()
    }

    private fun initView() {
        initToolBar()
        makePop()

        initListener()
    }

    private fun initToolBar() {
        toolbarTv.visibility = View.VISIBLE
        toolbarTv.text = getString(R.string.area)
        img_function1.visibility = View.VISIBLE
        image_bluetooth.visibility = View.VISIBLE
        image_bluetooth.setImageResource(R.mipmap.icon_scanning)
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun initListener() {
        img_function1.setOnClickListener { PopUtil.show(popAdd, window.decorView, Gravity.CENTER) }
        image_bluetooth.setOnClickListener { }
    }

    @SuppressLint("CheckResult")
    private fun addRegion(text: Editable) {
        val dbRegion = DBUtils.lastRegion

        Log.e("zcl", "zcl*****addRegion*${dbRegion.id}")
        dbRegion.installMesh = Constant.PIR_SWITCH_MESH_NAME
        dbRegion.installMeshPwd = "123"
        dbRegion.name = text.toString()
        dbRegion.id = DBUtils.lastRegion.id + 1
        RegionModel.addRegions(DBUtils.lastUser!!.token, dbRegion, dbRegion.id)!!.subscribe {
            if (it.isSucess) {
                initData()
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun initData() {
        if (DBUtils.lastUser != null) {
            RegionModel.get(DBUtils.lastUser!!.token)?.subscribe { it ->
                setData(it)
            }
        }
    }

    private fun setData(it: MutableList<DbRegion>?) {
        list = it
        area_account.text = getString(R.string.current_account) + ": +" + DBUtils.lastUser!!.phone
        area_recycleview.layoutManager = LinearLayoutManager(this@NetworkActivity, LinearLayoutManager.VERTICAL, false)
        adapter = AreaItemAdapter(R.layout.item_area_net, it!!, DBUtils.lastUser!!.last_region_id)
        adapter!!.setOnItemClickListener { _, _, position -> setPop(position) }
        area_recycleview.adapter = adapter
        list?.let {
            if (list!!.size > 0)
                DBUtils.saveRegion(it[list!!.size - 1], isFrist)//是第一次就是从服务器获取
            isFrist = false
        }
    }

    private fun makePop() {
        view = View.inflate(this, R.layout.popwindown_region, null)
        viewAdd = View.inflate(this, R.layout.popwindown_add_region, null)

        pop = PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        popAdd = PopupWindow(viewAdd, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        setPopSetting(pop!!)
        setPopSetting(popAdd!!)

        view?.let {
            it.findViewById<RelativeLayout>(R.id.pop_view).setOnClickListener(this)
            it.findViewById<ImageView>(R.id.pop_user_net).setOnClickListener(this)
            it.findViewById<ImageView>(R.id.pop_del_net).setOnClickListener(this)
            it.findViewById<ImageView>(R.id.pop_share_net).setOnClickListener(this)
            it.findViewById<ImageView>(R.id.pop_update_net).setOnClickListener(this)
            it.findViewById<TextView>(R.id.pop_creater_name).text = getString(R.string.creater_name) + DBUtils.lastUser!!.phone
            //it.findViewById<ImageView>(R.id.pop_qr_img).setOnClickListener(this)
        }

        viewAdd?.let {
            var name = it.findViewById<EditText>(R.id.pop_region_name)
            it.findViewById<Button>(R.id.btn_confirm).setOnClickListener {
                addRegion(name.text)
                PopUtil.dismiss(popAdd)
            }
            it.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
                PopUtil.dismiss(popAdd)
            }
        }
    }

    private fun setPopSetting(pop: PopupWindow) {
        pop?.let {
            pop!!.isOutsideTouchable = true
            pop!!.isFocusable = true // 设置PopupWindow可获得焦点
            pop!!.isTouchable = true // 设置PopupWindow可触摸补充：
        }
    }

    private fun setPop(position: Int) {
        dbRegion = list!![position]
        if (dbRegion == null)
            return
        view?.let {
            it.findViewById<TextView>(R.id.pop_net_name).text = dbRegion!!.name
            it.findViewById<TextView>(R.id.pop_equipment_num).text = getString(R.string.equipment_quantity) + list!!.size
            if (dbRegion!!.id.toString() == DBUtils.lastUser!!.last_region_id)
                it.findViewById<ImageView>(R.id.pop_user_net).setImageResource(R.drawable.icon_use_blue)
            else
                it.findViewById<ImageView>(R.id.pop_user_net).setImageResource(R.drawable.icon_use)
        }
        root?.let { PopUtil.show(pop, it, Gravity.BOTTOM) }
    }


    override fun onClick(v: View?) {
        DBUtils.deleteAll()
        when (v!!.id) {
            R.id.pop_view -> {
                PopUtil.dismiss(pop)
            }

            R.id.pop_user_net -> {
                //上传数据到服务器
                if (dbRegion!!.id.toString() != DBUtils.lastUser!!.last_region_id) {
                    checkNetworkAndSync(this)
                }
            }
            R.id.pop_del_net -> {

            }
            R.id.pop_share_net -> {
                view!!.findViewById<TextView>(R.id.pop_net_name).text = "444"
                getQr()
            }
            R.id.pop_update_net -> {

            }
        }
    }

    private fun getQr() {

    }


    /**
     * 检查网络上传数据
     * 如果没有网络，则弹出网络设置对话框
     */
    fun checkNetworkAndSync(activity: Activity?) {
        if (!NetWorkUtils.isNetworkAvalible(activity!!)) {
            AlertDialog.Builder(activity)
                    .setTitle(R.string.network_tip_title)
                    .setMessage(R.string.net_disconnect_tip_message)
                    .setPositiveButton(android.R.string.ok
                    ) { _, _ ->
                        // 跳转到设置界面
                        activity.startActivityForResult(Intent(
                                Settings.ACTION_WIRELESS_SETTINGS),
                                0)
                    }.create().show()
        } else {
            SyncDataPutOrGetUtils.syncPutDataStart(activity, syncCallback)
        }
    }

    /**
     * 上传回调
     */
    internal var syncCallback: SyncCallback = object : SyncCallback {
        override fun start() {
            showLoadingDialog(this@NetworkActivity!!.getString(R.string.start_change_region))
        }

        override fun complete() {
            ToastUtils.showLong(this@NetworkActivity!!.getString(R.string.success_change_region))
            //创建数据库
            val user = DBUtils.lastUser
            user?.let {
                //更新user
                it.last_region_id = dbRegion?.id.toString()
                //创建数据库
                AccountModel.initDatBase(it)
                //更新last—region-id
                DBUtils.saveUser(user)

                if (dbRegion!!.id.toString() == DBUtils.lastUser!!.last_region_id)
                    view!!.findViewById<ImageView>(R.id.pop_user_net).setImageResource(R.drawable.icon_use_blue)
                else
                    view!!.findViewById<ImageView>(R.id.pop_user_net).setImageResource(R.drawable.icon_use)
               initData()
            }
            hideLoadingDialog()
        }

        override fun error(msg: String) {
            ToastUtils.showLong(getString(R.string.error_change_region))
            hideLoadingDialog()
        }
    }


    fun showLoadingDialog(content: String) {
        val inflater = LayoutInflater.from(this)
        val v = inflater.inflate(R.layout.dialogview, null)

        val layout = v.findViewById<View>(R.id.dialog_view) as LinearLayout
        val tvContent = v.findViewById<View>(R.id.tvContent) as TextView
        tvContent.text = content


        if (loadDialog == null) {
            loadDialog = Dialog(this!!,
                    R.style.FullHeightDialog)
        }
        //loadDialog没显示才把它显示出来
        if (!loadDialog!!.isShowing) {
            loadDialog!!.setCancelable(false)
            loadDialog!!.setCanceledOnTouchOutside(false)
            loadDialog!!.setContentView(layout)
            loadDialog!!.show()
        }
    }

    fun hideLoadingDialog() {
        if (loadDialog != null) {
            loadDialog!!.dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PopUtil.dismiss(pop)
    }
}



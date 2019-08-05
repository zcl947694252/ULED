package com.dadoutek.uled.region


import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.support.constraint.ConstraintLayout
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
import com.dadoutek.uled.region.bean.RegionBean
import com.dadoutek.uled.util.DensityUtil
import com.dadoutek.uled.util.NetWorkUtils
import com.dadoutek.uled.util.PopUtil
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import com.uuzuche.lib_zxing.activity.CodeUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_network.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.textColor
import java.util.concurrent.TimeUnit


class NetworkActivity : AppCompatActivity(), View.OnClickListener {
    private var isAuthorizeRegionAll: Boolean = false
    private var isMeRegionAll: Boolean = false
    private var listTwo: MutableList<MutableList<RegionBean>>? = null
    var list: MutableList<RegionBean>? = null
    var listAll: MutableList<RegionBean>? = null
    var subMeList: MutableList<RegionBean>? = null

    var listAuthorize: MutableList<RegionBean>? = null
    var listAuthorizeAll: MutableList<RegionBean>? = null
    var subAuthorizeList: MutableList<RegionBean>? = null

    private var loadDialog: Dialog? = null
    private lateinit var popAdd: PopupWindow
    private var viewAdd: View? = null
    var adapter: AreaItemAdapter? = null
    var adapterAuthorize: AreaItemAdapter? = null
    var pop: PopupWindow? = null
    var view: View? = null
    var root: View? = null
    var regionBean: RegionBean? = null
    var mCompositeDisposable = CompositeDisposable()
    var mBuild: AlertDialog.Builder? = null
    var isShowType = 3 //1自己的区域  2接收区域  3 全部区域
    var mExpire: Long = 0
    private var isAddRegion = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network)
        list = ArrayList()
        listAuthorize = ArrayList()
        listTwo = ArrayList()
        initView()
        initData()
    }

    private fun initView() {
        initToolBar()
        makePop()
        initListener()
        mBuild = AlertDialog.Builder(this@NetworkActivity)
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
        img_function1.setOnClickListener {
            isAddRegion =true
            viewAdd!!.findViewById<EditText>(R.id.pop_region_name).setText("")
            viewAdd!!.findViewById<EditText>(R.id.pop_region_name).hint = getString(R.string.input_region_name)
            showPop(popAdd, Gravity.CENTER)
        }

        image_bluetooth.setOnClickListener {
            //S扫描
        }
        region_to_receive.setOnClickListener {

        }
        transfer_account.setOnClickListener {

        }
    }

    @SuppressLint("CheckResult")
    private fun addRegion(text: Editable) {
        val dbRegion = DbRegion()
        dbRegion.installMesh = Constant.PIR_SWITCH_MESH_NAME
        dbRegion.installMeshPwd = "123"
        dbRegion.name = text.toString()
        if (isAddRegion){
            if (list == null || list!!.size <= 0) {
                dbRegion.id = 1
            } else {
                val region = list?.get(list!!.size - 1)
                dbRegion.id = region!!.id + 1
            }
        }else{
            dbRegion.id = regionBean?.id
        }

        RegionModel.addRegions(DBUtils.lastUser!!.token, dbRegion, dbRegion.id)!!.subscribe {
            if (it.errorCode == 0) {
                initData()
            }
        }
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun initData() {
        if (DBUtils.lastUser != null){
            region_account_num.text = "+"+DBUtils.lastUser!!.phone
            Log.e("zcl","zcl******lastUser!!.account****${DBUtils.lastUser!!.phone}")
            when (isShowType) {
                1 -> RegionModel.get()?.subscribe { it -> setMeData(it) }
                2 -> RegionModel.getAuthorizerList()?.subscribe { it -> setMeData(it) }
                3 -> {
                    RegionModel.getAuthorizerList()?.subscribe { it -> setAuthorizeData(it) }
                    RegionModel.get()?.subscribe { it -> setMeData(it)}
                }
            }
        }
    }

    /**
     * 赋值我的区域数据
     */
    @SuppressLint("StringFormatInvalid", "StringFormatMatches")
    private fun setMeData(it: MutableList<RegionBean>) {
        listAll = it
        region_me_net_num.text = getString(R.string.me_net_num, it.size)
        if (it.size > 3) {
            region_me_more.visibility = View.VISIBLE
            subMeList = it.subList(0, 2)
        } else {
            subMeList = it
            region_me_more.visibility = View.GONE
        }

        list = if (!isMeRegionAll) subMeList else listAll

        Log.e("zcl", "zcl******me**${list!!.size}")
        region_me_recycleview.layoutManager = LinearLayoutManager(this@NetworkActivity, LinearLayoutManager.VERTICAL, false)
        adapter = AreaItemAdapter(R.layout.item_area_net, list!!, DBUtils.lastUser!!.last_region_id)
        adapter!!.setOnItemChildClickListener { _, view, position ->
            isShowType = 1
            setClickAction(position, view, list!!)
        }
        region_me_recycleview.adapter = adapter
        region_me_more.setOnClickListener {
            isMeRegionAll = !isMeRegionAll
            setMoreArr(isMeRegionAll, region_me_more_tv, region_me_more_arr)
            isShowType = 1
            initData()
        }
    }

    /**
     * 查看更多状态变化
     */
    private fun setMoreArr(isAll: Boolean, more_tv: TextView, more_arr: ImageView) {
        if (isAll) {
            more_arr.setImageResource(R.mipmap.icon_on)
            more_tv.text = getString(R.string.pick_up)
        } else {
            more_tv.text = getString(R.string.see_more)
            more_arr.setImageResource(R.mipmap.icon_under)
        }
    }


    /**
     * 赋值接受区域数据
     */
    @SuppressLint("StringFormatMatches")
    private fun setAuthorizeData(it: MutableList<RegionBean>) {
        listAuthorizeAll = it
        region_authorize_net_num.text = getString(R.string.received_net_num, it.size)

        if (it.size > 3) {
            region_authorize_more.visibility = View.VISIBLE
            subAuthorizeList = it.subList(0, 2)
        } else {
            subAuthorizeList = it
            region_authorize_more.visibility = View.GONE
        }
        listAuthorize = if (!isAuthorizeRegionAll) subAuthorizeList else listAuthorizeAll

        Log.e("zcl", "zcl******authorize**${listAuthorize!!.size}")

        region_authorize_recycleview.layoutManager = LinearLayoutManager(this@NetworkActivity, LinearLayoutManager.VERTICAL, false)
        adapterAuthorize = AreaItemAdapter(R.layout.item_area_net, listAuthorize!!, DBUtils.lastUser!!.last_region_id)
        adapterAuthorize!!.setOnItemChildClickListener { _, view, position ->
            isShowType = 2
            setClickAction(position, view, listAuthorize!!)
        }
        region_authorize_recycleview.adapter = adapter
        region_authorize_recycleview.setOnClickListener {
            isAuthorizeRegionAll = !isAuthorizeRegionAll
            setMoreArr(isAuthorizeRegionAll, region_authorize_more_tv, region_authorize_more_arr)
            isShowType = 2
            initData()
        }
    }

    /**
     * 点击Item事件
     */
    private fun setClickAction(position: Int, view: View, list: MutableList<RegionBean>) {
        regionBean = list[position]
        when (view.id) {
            R.id.item_area_state -> {
                mBuild?.let {
                    it.setMessage(getString(R.string.change_region))
                    it.setNegativeButton(getString(R.string.btn_ok)) { dialog, _ ->
                        changeRegion()
                        dialog.dismiss()
                    }
                    it.setPositiveButton(getString(R.string.cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    it.setCancelable(true)
                    it.show()
                }
            }
            R.id.item_area_more -> setPopData(position, list)
        }
    }

    override fun onClick(v: View?) {
        DBUtils.deleteAll()
        when (v!!.id) {
            R.id.pop_view -> PopUtil.dismiss(pop)
            R.id.pop_qr_ly -> {
            }
            R.id.pop_qr_cancel -> PopUtil.dismiss(pop)
            R.id.pop_user_net -> {
                //上传数据到服务器
                changeRegion()
            }
            R.id.pop_delete_net -> {
                //只能删除当前切换的区域
                RegionModel.removeRegion()!!.subscribe {
                    if (it.errorCode==0)
                        initData()
                }
            }
            R.id.pop_share_net -> {
                if (mExpire > 0) {
                    view?.findViewById<LinearLayout>(R.id.pop_qr_ly)?.visibility = View.VISIBLE
                } else {
                    getQr()
                }
            }
            R.id.pop_update_net -> {
                viewAdd!!.findViewById<EditText>(R.id.pop_region_name).setText("")
                viewAdd!!.findViewById<EditText>(R.id.pop_region_name).hint = getString(R.string.input_new_region_name)
                PopUtil.dismiss(pop)
                isAddRegion = false
                showPop(popAdd, Gravity.CENTER)
            }
            R.id.pop_qr_undo -> {
                //撤销二维码
                RegionModel.removeAuthorizationCode(regionBean!!.id, regionBean!!.code_info!!.type)!!.subscribe {
                    if (it.errorCode == 0) {
                        //设置二维码失效状态 倒计时颜色状态
                        view?.findViewById<TextView>(R.id.pop_qr_timer)?.text = getString(R.string.QR_expired)
                        view?.findViewById<TextView>(R.id.pop_qr_timer)?.textColor = getColor(R.color.red)

                        //设置刷新二维码变为二维码已撤销
                        view?.findViewById<TextView>(R.id.pop_qr_undo)?.textColor = getColor(R.color.black_three)
                        view?.findViewById<TextView>(R.id.pop_qr_undo)?.text = getString(R.string.QR_canceled)
                        view?.findViewById<TextView>(R.id.pop_qr_undo)?.isClickable = false
                        view?.findViewById<ImageView>(R.id.pop_qr_img)?.setImageResource(R.mipmap.icon_cancel)
                        view?.findViewById<TextView>(R.id.pop_qr_timer)?.visibility = View.GONE

                        setCreatShareCodeState()

                        view?.findViewById<View>(R.id.view19)?.visibility = View.GONE

                        mCompositeDisposable.clear()
                        mExpire = 0
                    }
                }
            }
        }
    }

    private fun changeRegion() {
        if (regionBean!!.id.toString() != DBUtils.lastUser!!.last_region_id) {
            checkNetworkAndSync(this)
        }
    }

    /**
     * 获取授权码
     */
    @SuppressLint("CheckResult")
    private fun getQr() {
        RegionModel.getAuthorizationCode(regionBean!!.id)!!.subscribe {
            if (it.errorCode == 0) {
                setQR(it.t.code)
                val expire = it.t.expire.toLong()
                downTimer(expire)
                view?.findViewById<LinearLayout>(R.id.pop_qr_ly)?.visibility = View.VISIBLE
            }
        }
    }

    private fun setQR(it: String) {
        var mBitmap = CodeUtils.createImage(it, DensityUtil.dip2px(this, 231f), DensityUtil.dip2px(this, 231f), null)
        view?.findViewById<ImageView>(R.id.pop_qr_img)?.setImageBitmap(mBitmap)
    }

    @SuppressLint("SetTextI18n")
    private fun downTimer(expire: Long) {
        mCompositeDisposable.clear()

        if (expire > 0) {
            view?.findViewById<ImageView>(R.id.pop_share_net)?.setImageResource(R.mipmap.icon_code)
            view?.findViewById<TextView>(R.id.pop_share_net_tv)?.text = getString(R.string.see_qr)
            view?.findViewById<TextView>(R.id.pop_qr_timer)?.textColor = getColor(R.color.black_three)
            view?.findViewById<TextView>(R.id.pop_qr_timer)?.visibility = View.VISIBLE
            view?.findViewById<TextView>(R.id.pop_qr_undo)?.text = getString(R.string.QR_cancel)
            view?.findViewById<TextView>(R.id.pop_qr_undo)?.isClickable = true
            view?.findViewById<View>(R.id.view19)?.visibility = View.VISIBLE

            mCompositeDisposable.add(Observable
                    //从0开始到expire 第一次0秒开始 以后一秒一次
                    .intervalRange(0, expire, 0, 1, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        mExpire = expire - it
                        if (mExpire == 0L) {
                            setShareAndTimerAndCancelState()
                        } else {
                            val s = mExpire % 60
                            val m = mExpire / 60 % 60
                            val h = mExpire / 60 / 60
                            view?.findViewById<TextView>(R.id.pop_qr_timer)?.text = "$h:$m:$s"
                        }
                    })
        }
    }

    private fun setShareAndTimerAndCancelState() {
        //设置分享网络状态
        setCreatShareCodeState()
        //设置二维码失效状态 倒计时颜色状态
        view?.findViewById<ImageView>(R.id.pop_qr_img)?.setImageResource(R.mipmap.icon_invalid)
        view?.findViewById<TextView>(R.id.pop_qr_timer)?.text = getString(R.string.QR_expired)
        view?.findViewById<TextView>(R.id.pop_qr_timer)?.textColor = getColor(R.color.red)
        //设置取消二维码变为刷新状态
        view?.findViewById<TextView>(R.id.pop_qr_undo)?.textColor = getColor(R.color.black_three)
        view?.findViewById<TextView>(R.id.pop_qr_undo)?.text = getString(R.string.refresh_qr_code)
        view?.findViewById<TextView>(R.id.pop_qr_undo)?.isClickable = true
    }

    private fun setCreatShareCodeState() {
        view?.findViewById<ImageView>(R.id.pop_share_net)?.setImageResource(R.drawable.icon_share)
        view?.findViewById<TextView>(R.id.pop_share_net_tv)?.text = getString(R.string.share_network)
        view?.findViewById<TextView>(R.id.pop_qr_timer)?.textColor = getColor(R.color.black_three)
    }

    private fun makePop() {
        view = View.inflate(this, R.layout.popwindown_region, null)
        viewAdd = View.inflate(this, R.layout.popwindown_add_region, null)

        pop = PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        //添加区域pop
        popAdd = PopupWindow(viewAdd, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        setPopSetting(pop!!)
        setPopSetting(popAdd!!)

        view?.let {
            it.findViewById<RelativeLayout>(R.id.pop_view).setOnClickListener(this)
            it.findViewById<ImageView>(R.id.pop_user_net).setOnClickListener(this)
            it.findViewById<ImageView>(R.id.pop_delete_net).setOnClickListener(this)
            it.findViewById<ImageView>(R.id.pop_share_net).setOnClickListener(this)
            it.findViewById<ImageView>(R.id.pop_update_net).setOnClickListener(this)
            it.findViewById<TextView>(R.id.pop_creater_name).text = getString(R.string.creater_name) + DBUtils.lastUser!!.phone
            it.findViewById<TextView>(R.id.pop_qr_cancel).setOnClickListener(this)
            it.findViewById<LinearLayout>(R.id.pop_qr_ly).setOnClickListener(this)
            it.findViewById<TextView>(R.id.pop_qr_undo).setOnClickListener(this)
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

    /**
     * 设置指定pop内容并弹框
     */
    private fun setPopData(position: Int, list: MutableList<RegionBean>) {
        regionBean = list[position]

        if (regionBean == null)
            return

        view?.let {
            it.findViewById<TextView>(R.id.pop_net_name).text = regionBean!!.name
            it.findViewById<TextView>(R.id.pop_equipment_num).text = getString(R.string.equipment_quantity) + list.size
            if (regionBean!!.id.toString() == DBUtils.lastUser!!.last_region_id)
                it.findViewById<ImageView>(R.id.pop_user_net).setImageResource(R.drawable.icon_use_blue)
            else
                it.findViewById<ImageView>(R.id.pop_user_net).setImageResource(R.drawable.icon_use)

            if (regionBean!!.id == 1L)
                it.findViewById<ImageView>(R.id.pop_delete_net).setImageResource(R.drawable.icon_delete)
            else
                it.findViewById<ImageView>(R.id.pop_delete_net).setImageResource(R.mipmap.icon_delete_bb)

            mExpire = regionBean!!.code_info!!.expire.toLong()
            Log.e("zcl", "zcl****regionBean**${regionBean.toString()}")
            if (mExpire > 0) {
                it.findViewById<ImageView>(R.id.pop_share_net).setImageResource(R.mipmap.icon_code)
                regionBean!!.code_info!!.code?.let { it1 -> setQR(it1) }
                downTimer(mExpire)
            } else {
                it.findViewById<ImageView>(R.id.pop_share_net).setImageResource(R.drawable.icon_share)
            }
        }
        view?.findViewById<LinearLayout>(R.id.pop_qr_ly)?.visibility = View.GONE
        view?.findViewById<ConstraintLayout>(R.id.pop_net_ly)?.visibility = View.VISIBLE
        showPop(pop!!, Gravity.BOTTOM)
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
                it.last_region_id = regionBean?.id.toString()

                DBUtils.deleteAllData()
                //创建数据库
                AccountModel.initDatBase(it)
                //更新last—region-id
                DBUtils.saveUser(user)
                //下拉数据
                SyncDataPutOrGetUtils.syncGetDataStart(user, syncCallbackGet)
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

    internal var syncCallbackGet: SyncCallback = object : SyncCallback {
        override fun start() {}
        override fun complete() {
            view?.findViewById<ImageView>(R.id.pop_user_net)?.setImageResource(R.drawable.icon_use_blue)
            initData()
        }

        override fun error(msg: String) {}
    }

    private fun showPop(pop: PopupWindow, gravity: Int) {
        if (pop != null && !pop.isShowing)
            pop.showAtLocation(window.decorView, gravity, 0, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        PopUtil.dismiss(pop)
        PopUtil.dismiss(popAdd)
        mCompositeDisposable.dispose()
    }
}



package com.dadoutek.uled.region


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.constraint.ConstraintLayout
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
import com.dadoutek.uled.base.BaseActivity
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbRegion
import com.dadoutek.uled.model.DbModel.DbUser
import com.dadoutek.uled.model.HttpModel.AccountModel
import com.dadoutek.uled.model.HttpModel.RegionModel
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.network.bean.RegionAuthorizeBean
import com.dadoutek.uled.region.adapter.AreaAuthorizeItemAdapter
import com.dadoutek.uled.region.adapter.AreaItemAdapter
import com.dadoutek.uled.region.bean.RegionBean
import com.dadoutek.uled.util.DensityUtil
import com.dadoutek.uled.util.NetWorkUtils
import com.dadoutek.uled.util.PopUtil
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import com.uuzuche.lib_zxing.activity.CaptureActivity
import com.uuzuche.lib_zxing.activity.CodeUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_network.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.textColor
import java.util.concurrent.TimeUnit


class NetworkActivity : BaseActivity(), View.OnClickListener {
    override fun setLayoutID(): Int {
        return R.layout.activity_network
    }

    private val REQUEST_CODE: Int = 1000
    private val REQUEST_CODE_CARMER: Int = 100
    private var isAuthorizeRegionAll: Boolean = false
    private var isMeRegionAll: Boolean = false

    var list: MutableList<RegionBean>? = null
    var listAll: MutableList<RegionBean>? = null
    var subMeList: MutableList<RegionBean>? = null

    var listAuthorize: MutableList<RegionAuthorizeBean>? = null
    var listAuthorizeAll: MutableList<RegionAuthorizeBean>? = null
    var subAuthorizeList: MutableList<RegionAuthorizeBean>? = null

    private var loadDialog: Dialog? = null
    private lateinit var popAdd: PopupWindow
    private var viewAdd: View? = null
    var adapter: AreaItemAdapter? = null
    var adapterAuthorize: AreaAuthorizeItemAdapter? = null
    var pop: PopupWindow? = null
    var view: View? = null
    var root: View? = null
    var regionBean: RegionBean? = null
    var regionBeanAuthorize: RegionAuthorizeBean? = null
    var mCompositeDisposable = CompositeDisposable()
    var mBuild: AlertDialog.Builder? = null
    var isShowType = 3 //1自己的区域  2接收区域  3 全部区域
    var mExpire: Long = 0
    private var isAddRegion = true
    override fun initView() {
        list = ArrayList()
        listAuthorize = ArrayList()

        initToolBar()
        makePop()
        initListener()
        mBuild = AlertDialog.Builder(this@NetworkActivity)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CODE_CARMER)
            }
        }
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

    override fun initListener() {
        img_function1.setOnClickListener {
            isAddRegion = true
            viewAdd!!.findViewById<EditText>(R.id.pop_region_name).setText("")
            viewAdd!!.findViewById<EditText>(R.id.pop_region_name).hint = getString(R.string.input_region_name)
            showPop(popAdd, Gravity.CENTER)
        }

        image_bluetooth.setOnClickListener {
            openScan()
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
        if (isAddRegion) {
            if (listAll == null || listAll!!.size <= 0) {
                dbRegion.id = 1
            } else {
                val region = listAll?.get(listAll!!.size - 1)
                dbRegion.id = region!!.id + 1
            }
        } else {
            dbRegion.id = regionBean?.id
        }

        RegionModel.addRegions(DBUtils.lastUser!!.token, dbRegion, dbRegion.id)!!.subscribe(
                { initData() },
                { ToastUtils.showShort(it.message) })
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    override fun initData() {
        if (DBUtils.lastUser != null) {
            region_account_num.text = "+" + DBUtils.lastUser!!.phone
            Log.e("zcl", "zcl******isShowType****$isShowType" + "user${DBUtils.lastUser.toString()}")
            when (isShowType) {
                1 -> RegionModel.get()?.subscribe ({ it -> setMeData(it) },{
                    ToastUtils.showShort(it.message)
                })
                2 -> RegionModel.getAuthorizerList()?.subscribe ({ it -> setAuthorizeData(it) },{
                    ToastUtils.showShort(it.message)
                })
                3 -> {
                    RegionModel.get()?.subscribe ({ it -> setMeData(it) },{
                        ToastUtils.showShort(it.message)
                    })
                    RegionModel.getAuthorizerList()?.subscribe ({ it -> setAuthorizeData(it) },{
                        ToastUtils.showShort(it.message)
                    })
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
        adapter = AreaItemAdapter(R.layout.item_area_net, list!!, DBUtils.lastUser)
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
    private fun setAuthorizeData(it: MutableList<RegionAuthorizeBean>) {
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
        adapterAuthorize = AreaAuthorizeItemAdapter(R.layout.item_area_net, listAuthorize!!, DBUtils.lastUser)
        adapterAuthorize!!.setOnItemChildClickListener { _, view, position ->
            isShowType = 2
            setClickActionAuthorize(position, view, listAuthorize!!)
        }
        region_authorize_recycleview.adapter = adapterAuthorize

        region_authorize_more.setOnClickListener {
            isAuthorizeRegionAll = !isAuthorizeRegionAll
            setMoreArr(isAuthorizeRegionAll, region_authorize_more_tv, region_authorize_more_arr)
            isShowType = 2
            initData()
        }
    }

    private fun setClickActionAuthorize(position: Int, view: View?, list: MutableList<RegionAuthorizeBean>) {
        regionBeanAuthorize = list[position]
        when (view?.id) {
            R.id.item_area_state -> {
                setChangeDialog()
            }
            R.id.item_area_more -> setPopDataAuthorize(position, list)
        }

    }

    private fun setPopDataAuthorize(position: Int, list: MutableList<RegionAuthorizeBean>) {
        isShowType = 2
        regionBeanAuthorize = list[position]

        if (regionBeanAuthorize == null)
            return

        view?.let {
            it.findViewById<TextView>(R.id.pop_net_name).text = regionBeanAuthorize!!.name
            it.findViewById<TextView>(R.id.pop_equipment_num).text = getString(R.string.equipment_quantity) + list.size
            //授权区域只有解绑和使用功能
            it.findViewById<ImageView>(R.id.pop_delete_net).isClickable = false
            it.findViewById<ImageView>(R.id.pop_delete_net).setImageResource(R.drawable.icon_delete)

            //分享
            it.findViewById<ImageView>(R.id.pop_share_net).setImageResource(R.mipmap.icon_share)
            it.findViewById<ImageView>(R.id.pop_share_net).isClickable = false
            //修改不能用
            it.findViewById<ImageView>(R.id.pop_update_net).setImageResource(R.mipmap.icon_modify)
            it.findViewById<ImageView>(R.id.pop_update_net).isClickable = false


            if (DBUtils.lastUser!!.authorizer_user_id == regionBeanAuthorize!!.authorizer_id.toString()
                    && DBUtils.lastUser!!.last_region_id == regionBeanAuthorize!!.id.toString()) {
                it.findViewById<LinearLayout>(R.id.pop_unbind_net_ly).isClickable = false
                it.findViewById<ImageView>(R.id.pop_unbind_net).setImageResource(R.mipmap.icon_untied)
                it.findViewById<ImageView>(R.id.pop_user_net).setImageResource(R.drawable.icon_use_blue)
            } else {
                it.findViewById<LinearLayout>(R.id.pop_unbind_net_ly).isClickable = true
                it.findViewById<ImageView>(R.id.pop_unbind_net).setImageResource(R.mipmap.icon_untied_b)
                it.findViewById<ImageView>(R.id.pop_user_net).setImageResource(R.drawable.icon_use)
            }
        }
        view?.findViewById<LinearLayout>(R.id.pop_qr_ly)?.visibility = View.GONE
        view?.findViewById<ConstraintLayout>(R.id.pop_net_ly)?.visibility = View.VISIBLE
        showPop(pop!!, Gravity.BOTTOM)
    }

    private fun setChangeDialog() {
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
            R.id.pop_unbind_net_ly -> {
                //解绑网络
                when (isShowType) {
                    1 -> {
                        val intent = Intent(this@NetworkActivity, UnbindMeNetActivity::class.java)
                        intent.putExtra(Constant.SHARE_PERSON, regionBean)
                        startActivity(intent)
                    }
                    2 -> {
                        showUnbindDialog()
                    }
                }
            }
            R.id.pop_qr_cancel -> PopUtil.dismiss(pop)
            R.id.pop_user_net -> {
                //上传数据到服务器
                changeRegion()
            }
            R.id.pop_delete_net -> {
                RegionModel.removeRegion(regionBean!!.id)!!.subscribe ({
                    PopUtil.dismiss(pop)
                    initData()
                },{
                    ToastUtils.showShort(it.message)
                })
            }
            R.id.pop_share_net -> {
                if (mExpire > 0)
                    view?.findViewById<LinearLayout>(R.id.pop_qr_ly)?.visibility = View.VISIBLE
                else
                    getQr()
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

    private fun changeRegion() {
        when (isShowType) {
            1 -> if (DBUtils.lastUser!!.authorizer_user_id != DBUtils.lastUser!!.id.toString() ||
                    regionBean!!.id.toString() != DBUtils.lastUser!!.last_region_id) {
                checkNetworkAndSync(this)
            }
            2 -> if (DBUtils.lastUser!!.authorizer_user_id != regionBeanAuthorize!!.authorizer_id.toString()
                    || regionBeanAuthorize!!.id.toString() != DBUtils.lastUser!!.last_region_id) {
                checkNetworkAndSync(this)
            }
        }
    }

    /**
     * 获取授权码
     */
    @SuppressLint("CheckResult")
    private fun getQr() {
        RegionModel.getAuthorizationCode(regionBean!!.id)!!.subscribe ({
            setQR(it.code)
            val expire = it.expire.toLong()
            downTimer(expire)
            view?.findViewById<LinearLayout>(R.id.pop_qr_ly)?.visibility = View.VISIBLE
        },{
            ToastUtils.showShort(it.message)
        })
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
            it.findViewById<ImageView>(R.id.pop_view).setOnClickListener(this)
            it.findViewById<ImageView>(R.id.pop_user_net).setOnClickListener(this)
            it.findViewById<ImageView>(R.id.pop_delete_net).setOnClickListener(this)
            it.findViewById<ImageView>(R.id.pop_share_net).setOnClickListener(this)
            it.findViewById<LinearLayout>(R.id.pop_unbind_net_ly).setOnClickListener(this)
            it.findViewById<ImageView>(R.id.pop_update_net).setOnClickListener(this)
            it.findViewById<TextView>(R.id.pop_creater_name).text = getString(R.string.creater_name) + DBUtils.lastUser?.phone
            it.findViewById<TextView>(R.id.pop_qr_cancel).setOnClickListener(this)
            it.findViewById<LinearLayout>(R.id.pop_qr_ly).setOnClickListener(this)
            it.findViewById<TextView>(R.id.pop_qr_undo).setOnClickListener(this)
        }

        viewAdd?.let {
            var name = it.findViewById<EditText>(R.id.pop_region_name)
            it.findViewById<Button>(R.id.btn_confirm).setOnClickListener {
                isShowType = 1
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
        isShowType = 1
        regionBean = list[position]

        if (regionBean == null)
            return
        view?.let {
            it.findViewById<TextView>(R.id.pop_net_name).text = regionBean!!.name
            it.findViewById<TextView>(R.id.pop_equipment_num).text = getString(R.string.equipment_quantity) + list.size
            val lastUser = DBUtils.lastUser!!
            if (regionBean!!.id.toString() == lastUser.last_region_id)//使用中不能删除
                if (regionBean!!.authorizer_id == lastUser!!.authorizer_user_id.toInt() || lastUser!!.authorizer_user_id == null) {
                    it.findViewById<ImageView>(R.id.pop_delete_net).isClickable = false
                    it.findViewById<ImageView>(R.id.pop_delete_net).setImageResource(R.drawable.icon_delete)
                    it.findViewById<ImageView>(R.id.pop_user_net).setImageResource(R.drawable.icon_use_blue)
                } else {
                    it.findViewById<ImageView>(R.id.pop_delete_net).isClickable = true
                    it.findViewById<ImageView>(R.id.pop_delete_net).setImageResource(R.mipmap.icon_delete_bb)
                    it.findViewById<ImageView>(R.id.pop_user_net).setImageResource(R.drawable.icon_use)
                }

            if (regionBean!!.id.toString() == "1") {//区域id为1不能删除只能清空数据
                it.findViewById<ImageView>(R.id.pop_delete_net).isClickable = false
                it.findViewById<ImageView>(R.id.pop_delete_net).setImageResource(R.drawable.icon_delete)
            } else {
                it.findViewById<ImageView>(R.id.pop_delete_net).isClickable = true
                it.findViewById<ImageView>(R.id.pop_delete_net).setImageResource(R.mipmap.icon_delete_bb)
            }

            if (regionBean!!.ref_users?.size!! <= 0) {//没有授权不能进入解绑
                it.findViewById<LinearLayout>(R.id.pop_unbind_net_ly).isClickable = false
                it.findViewById<ImageView>(R.id.pop_unbind_net).setImageResource(R.mipmap.icon_untied)
            } else {
                it.findViewById<LinearLayout>(R.id.pop_unbind_net_ly).isClickable = true
                it.findViewById<ImageView>(R.id.pop_unbind_net).setImageResource(R.mipmap.icon_untied_b)
            }

            it.findViewById<ImageView>(R.id.pop_update_net).isClickable = true
            it.findViewById<ImageView>(R.id.pop_update_net).setImageResource(R.drawable.icon_modify)


            mExpire = regionBean!!.code_info!!.expire.toLong()
            Log.e("zcl", "zcl****regionBean**${regionBean.toString()}")

            it.findViewById<ImageView>(R.id.pop_share_net).isClickable = true
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
            var user = DbUser()
            var lastUser = DBUtils.lastUser
            lastUser?.let {
                //更新user
                when (isShowType) {
                    1 -> {
                        it.last_region_id = regionBean?.id.toString()
                        it.authorizer_user_id = regionBean?.authorizer_id.toString()
                    }
                    2 -> {
                        it.last_region_id = regionBeanAuthorize?.id.toString()
                        it.authorizer_user_id = regionBeanAuthorize?.authorizer_id.toString()
                    }
                }

                isShowType = 3
                DBUtils.deleteAllData()
                //创建数据库
                AccountModel.initDatBase(it)
                //更新last—region-id
                DBUtils.saveUser(it)
                //下拉数据
                SyncDataPutOrGetUtils.syncGetDataStart(it, syncCallbackGet)
            }

            hideLoadingDialog()
        }

        override fun error(msg: String) {
            ToastUtils.showLong(getString(R.string.error_change_region))
            //ToastUtils.showLong(msg)
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

    private fun showUnbindDialog() {
        val builder = android.support.v7.app.AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.warm_unbind_authorize_config, regionBeanAuthorize!!.name))
        builder.setNegativeButton(getString(R.string.btn_ok)) { dialog, _ ->
            //解除授权
            //authorizer_id授权用户id  rid区域id
            RegionModel.dropAuthorizeRegion(regionBeanAuthorize!!.authorizer_id, regionBeanAuthorize!!.id)
                    ?.subscribe ({
                        isShowType = 2
                        initData()
                        ToastUtils.showShort(getString(R.string.unbundling_success))
                    },{
                        ToastUtils.showShort(it.message)
                    })
            dialog.dismiss()
        }
        builder.setPositiveButton(getString(R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }

        builder.create().show()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE -> {  //处理扫描结果
                if (null != data) {
                    var bundle: Bundle? = data.extras ?: return
                    if (bundle!!.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS) {
                        var result = bundle.getString(CodeUtils.RESULT_STRING)
                        Log.e("zcl", "zcl***parse_result***$result")
                        postParseCode(result)
                        ToastUtils.showShort(getString(R.string.scan_success))
                    } else if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_FAILED) {
                        Toast.makeText(this, getString(R.string.fail_parse_qr), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun postParseCode(result: String?) {
        result?.let { it1 ->
            RegionModel.parseQRCode(it1)?.subscribe(object : NetworkObserver<String>() {
                override fun onNext(t: String) {
                    isShowType = 2
                    initData()
                    ToastUtils.showShort(getString(R.string.unbundling_success))
                }

            })
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_CARMER) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openScan()
            } else {
                ToastUtils.showShort(getString(R.string.fail_parse_qr))
            }
        }
    }

    private fun openScan() {
        var intent = Intent(this@NetworkActivity, CaptureActivity::class.java)
        startActivityForResult(intent, REQUEST_CODE)
    }
}

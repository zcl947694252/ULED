package com.dadoutek.uled.region


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
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
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.BaseActivity
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DBUtils.lastUser
import com.dadoutek.uled.model.DbModel.DbRegion
import com.dadoutek.uled.model.HttpModel.AccountModel
import com.dadoutek.uled.model.HttpModel.RegionModel
import com.dadoutek.uled.model.HttpModel.RegionModel.lookAuthorizeCode
import com.dadoutek.uled.network.NetworkFactory
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

/**
 * 创建者     zcl
 * 创建时间   2019/8/12 10:51
 * 描述	      ${不论区域怎么切换永远不变的是user的ID}$
 *
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${TODO}$
 */
class NetworkActivity : BaseActivity(), View.OnClickListener {
    var TAG = "zcl-NetworkActivity"
    override fun setLayoutID(): Int {
        return R.layout.activity_network
    }

    private var isRefresh: Boolean = false
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

    private var popAdd: PopupWindow? = null
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
    var isTransferCode = false
    private var isAddRegion = true
    override fun initView() {
        list = ArrayList()
        listAuthorize = ArrayList()

        initToolBar()
        makePop()
        initListener()
        getQrInfo()

        mBuild = AlertDialog.Builder(this@NetworkActivity)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CODE_CARMER)
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun getQrInfo() {
        RegionModel.lookTransferCodeState().subscribe({
            val isNewQr = it.code == null || it.code.trim() == "" || it.expire <= 0
            transfer_account_tv.text = if (isNewQr) getString(R.string.transfer_accounts) else getString(R.string.to_receive)
        }, { ToastUtils.showShort(it.message) })
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
            popAdd?.let { it1 -> showPop(it1, Gravity.CENTER) }
        }

        image_bluetooth.setOnClickListener {
            openScan()
        }

        transfer_account.setOnClickListener {
            lookAndMaketransferCode()
        }
    }

    @SuppressLint("CheckResult")
    private fun lookAndMaketransferCode() {
        RegionModel.lookTransferCode(this).subscribe({
            isTransferCode = true
            setQR(it.code)
            downTimer(it.expire.toLong())
            view?.findViewById<LinearLayout>(R.id.pop_qr_ly)?.visibility = View.VISIBLE
            view?.findViewById<ConstraintLayout>(R.id.pop_net_ly)?.visibility = View.GONE
            showPop(pop!!, Gravity.BOTTOM)
            transfer_account_tv.text = getString(R.string.to_receive)
        }, {
            ToastUtils.showShort(it.message)
        })
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

        RegionModel.addRegions(lastUser!!.token, dbRegion, dbRegion.id)!!.subscribe(
                { initData() },
                { ToastUtils.showShort(it.message) })
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    override fun initData() {
        if (lastUser != null) {
            region_account_num.text = "+" + lastUser!!.phone
            Log.e(TAG, "zcl******isShowType****$isShowType" + "user${lastUser.toString()}")
            when (isShowType) {
                1 -> RegionModel.get()?.subscribe({ it -> setMeData(it) }, {
                    ToastUtils.showShort(it.message)
                })
                2 -> RegionModel.getAuthorizerList()?.subscribe({ it -> setAuthorizeData(it) }, {
                    ToastUtils.showShort(it.message)
                })
                3 -> {
                    RegionModel.get()?.subscribe({ it -> setMeData(it) }, {
                        ToastUtils.showShort(it.message)
                    })
                    RegionModel.getAuthorizerList()?.subscribe({ it -> setAuthorizeData(it) }, {
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

        Log.e(TAG, "zcl******me**${list!!.size}")
        region_me_recycleview.layoutManager = LinearLayoutManager(this@NetworkActivity, LinearLayoutManager.VERTICAL, false)
        adapter = AreaItemAdapter(R.layout.item_area_net, list!!, lastUser)
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

        Log.e(TAG, "zcl******authorize**${listAuthorize!!.size}")

        region_authorize_recycleview.layoutManager = LinearLayoutManager(this@NetworkActivity, LinearLayoutManager.VERTICAL, false)
        adapterAuthorize = AreaAuthorizeItemAdapter(R.layout.item_area_net, listAuthorize!!, lastUser)
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
                val lastUser = lastUser!!
                //不是使用中的
                if (lastUser.last_authorizer_user_id != regionBeanAuthorize!!.authorizer_id.toString() || regionBeanAuthorize!!.id.toString() != lastUser.last_region_id)
                    setChangeDialog()
            }
            R.id.item_area_more -> setPopDataAuthorize(position, list)
        }
    }

    private fun setPopDataAuthorize(position: Int, list: MutableList<RegionAuthorizeBean>) {
        if (lastUser!!.last_authorizer_user_id == regionBeanAuthorize!!.authorizer_id.toString() && regionBeanAuthorize!!.id.toString() == lastUser!!.last_region_id)
            return

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


            if (lastUser!!.last_authorizer_user_id == regionBeanAuthorize!!.authorizer_id.toString()
                    && lastUser!!.last_region_id == regionBeanAuthorize!!.id.toString()) {
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
                //不是使用中的
                if (lastUser!!.last_authorizer_user_id != lastUser!!.id.toString() || regionBean!!.id.toString() != lastUser!!.last_region_id)
                    setChangeDialog()
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
                        PopUtil.dismiss(pop)
                    }
                    2 -> showUnbindDialog()
                }
            }
            R.id.pop_qr_cancel -> {
                if (isTransferCode)
                    RegionModel.removeTransferCode()!!.subscribe { setCancel() }
                PopUtil.dismiss(pop)
            }
            R.id.pop_user_net -> {
                //上传数据到服务器
                changeRegion()
            }
            R.id.pop_delete_net -> {
                RegionModel.removeRegion(regionBean!!.id)!!.subscribe({
                    PopUtil.dismiss(pop)
                    initData()
                }, {
                    ToastUtils.showShort(it.message)
                })
            }
            R.id.pop_share_net -> {
                lookAndMakeAuthorCode()
            }
            R.id.pop_update_net -> {
                viewAdd!!.findViewById<EditText>(R.id.pop_region_name).setText("")
                viewAdd!!.findViewById<EditText>(R.id.pop_region_name).hint = getString(R.string.input_new_region_name)
                PopUtil.dismiss(pop)
                isAddRegion = false
                popAdd?.let { showPop(it, Gravity.CENTER) }
            }
            R.id.pop_qr_undo -> {
                if (isRefresh) {
                    //刷新二维码
                    if (isTransferCode)
                        lookAndMaketransferCode()
                    else {
                        lookAndMakeAuthorCode()
                    }

                } else {
                    //撤销二维码
                    if (!isTransferCode)
                        RegionModel.removeAuthorizationCode(regionBean!!.id, regionBean!!.code_info!!.type)!!.subscribe({
                            setCancel()
                            setCreatShareCodeState()
                        }, { ToastUtils.showShort(it.message) })
                    else {
                        RegionModel.removeTransferCode()!!.subscribe {
                            //设置二维码失效状态 倒计时颜色状态
                            setCancel()
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun lookAndMakeAuthorCode() {
        lookAuthorizeCode(regionBean!!.id, this).subscribe({
            mExpire = it.expire.toLong()
            Log.e(TAG, "zcl****判断**" + (it.code == ""))
            isTransferCode = false
            setQR(it.code)
            val expire = it.expire.toLong()

            downTimer(expire)
            view?.findViewById<LinearLayout>(R.id.pop_qr_ly)?.visibility = View.VISIBLE
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    private fun setCancel() {
        view?.let {
            //设置二维码失效状态 倒计时颜色状态
            it.findViewById<TextView>(R.id.pop_qr_timer)?.text = getString(R.string.QR_expired)
            it.findViewById<TextView>(R.id.pop_qr_timer)?.textColor = getColor(R.color.red)

            //设置刷新二维码变为二维码已撤销
            it.findViewById<TextView>(R.id.pop_qr_undo)?.textColor = getColor(R.color.black_three)
            it.findViewById<TextView>(R.id.pop_qr_undo)?.text = getString(R.string.QR_canceled)
            it.findViewById<TextView>(R.id.pop_qr_undo)?.isClickable = false
            it.findViewById<ImageView>(R.id.pop_qr_img)?.setImageResource(R.mipmap.icon_revoked)
            it.findViewById<TextView>(R.id.pop_qr_timer)?.visibility = View.GONE

            it.findViewById<View>(R.id.view19)?.visibility = View.GONE
            mCompositeDisposable.clear()
        }

    }

    private fun changeRegion() {
        when (isShowType) {
            1 -> if (lastUser!!.last_authorizer_user_id != lastUser!!.id.toString() || regionBean!!.id.toString() != lastUser!!.last_region_id) {
                checkNetworkAndSyncRegion(this)
            }
            2 -> if (lastUser!!.last_authorizer_user_id != regionBeanAuthorize!!.authorizer_id.toString() || regionBeanAuthorize!!.id.toString() != lastUser!!.last_region_id) {
                checkNetworkAndSyncRegion(this)
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
            isRefresh = false//有时间就不是刷新状态
            view?.findViewById<ImageView>(R.id.pop_share_net)?.setImageResource(R.mipmap.icon_code)
            view?.findViewById<TextView>(R.id.pop_share_net_tv)?.text = getString(R.string.see_qr)
            view?.findViewById<TextView>(R.id.pop_qr_timer)?.textColor = getColor(R.color.black_three)
            view?.findViewById<TextView>(R.id.pop_qr_timer)?.visibility = View.VISIBLE
            view?.findViewById<TextView>(R.id.pop_qr_undo)?.text = getString(R.string.QR_cancel)
            view?.findViewById<TextView>(R.id.pop_qr_undo)?.isClickable = true
            view?.findViewById<View>(R.id.view19)?.visibility = View.VISIBLE

            mCompositeDisposable.add(Observable
                    //从0开始发射11个数字为：0-10依次输出，延时0s执行，每1s发射一次。 (0, 11, 0, 1, TimeUnit.SECONDS
                    //从1开始发送ex个数字 1-ex  从0开始发射11个数字为：0-10依次输出
                    .intervalRange(1, expire, 0, 1, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        //Log.e("zcl", "zcl******$expire----$it")
                        mExpire = expire - it
                        var b = mExpire == 0L
                        // Log.e(TAG,"zcl***倒计时***$mExpire----------$expire---------$it-----$b")
                        if (mExpire == 0L) {
                            isRefresh = true //二维码已过期就是刷新状态
                            if (isTransferCode) {
                                setRefreshState()
                                transfer_account_tv.text = getString(R.string.transfer_accounts)
                            } else
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
        setRefreshState()
    }

    private fun setRefreshState() {
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

    @SuppressLint("SetTextI18n")
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
            it.findViewById<TextView>(R.id.pop_creater_name).text = getString(R.string.creater_name) + lastUser?.phone
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
        pop.isOutsideTouchable = true
        pop.isFocusable = true // 设置PopupWindow可获得焦点
        pop.isTouchable = true // 设置PopupWindow可触摸补充：
    }

    /**
     * 设置指定pop内容并弹框
     */
    @SuppressLint("SetTextI18n")
    private fun setPopData(position: Int, list: MutableList<RegionBean>) {
        isShowType = 1
        regionBean = list[position]

        Log.e(TAG, "zcl******" + regionBean.toString())

        if (regionBean == null)
            return
        view?.let {
            it.findViewById<TextView>(R.id.pop_net_name).text = regionBean!!.name
            it.findViewById<TextView>(R.id.pop_equipment_num).text = getString(R.string.equipment_quantity) + list.size
            val lastUser = lastUser!!
            if (regionBean!!.id.toString() == lastUser.last_region_id && regionBean!!.id.toString() == lastUser.last_region_id)//使用中不能删除
                if (lastUser.last_authorizer_user_id == null || regionBean!!.authorizer_id == lastUser.last_authorizer_user_id.toInt()) {
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
            Log.e(TAG, "zcl****regionBean**${regionBean.toString()}")

            it.findViewById<ImageView>(R.id.pop_share_net).isClickable = true
            if (mExpire > 0) {
                it.findViewById<ImageView>(R.id.pop_share_net).setImageResource(R.mipmap.icon_code)
                it.findViewById<TextView>(R.id.pop_share_net_tv)?.text = getString(R.string.see_qr)
                regionBean!!.code_info!!.code?.let { it1 -> setQR(it1) }
                downTimer(mExpire)
            } else {
                it.findViewById<ImageView>(R.id.pop_share_net).setImageResource(R.drawable.icon_share)
                it.findViewById<TextView>(R.id.pop_share_net_tv)?.text = getString(R.string.share_network)
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
    fun checkNetworkAndSyncRegion(activity: Activity?) {
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
            val lastUser = lastUser
            lastUser?.let {
                //如果是自己的区域切换到其他区域 那么就要上传自己数据 否则不上传
                if (it.id.toString() == it.last_authorizer_user_id) {
                    SyncDataPutOrGetUtils.syncPutDataStart(activity, syncCallback)
                } else {
                    downLoadDataAndChangeDbUser()
                }
            }

        }
    }

    /**
     * 上传回调
     */
    internal var syncCallback: SyncCallback = object : SyncCallback {
        override fun start() {
            showLoadingDialog(this@NetworkActivity.getString(R.string.start_change_region))
        }

        override fun complete() {
            ToastUtils.showLong(this@NetworkActivity.getString(R.string.success_change_region))
            Log.e(TAG, "zcl******上传数据成功")
            downLoadDataAndChangeDbUser()
            hideLoadingDialog()
        }

        override fun error(msg: String) {
            ToastUtils.showLong(getString(R.string.error_change_region))
            hideLoadingDialog()
        }
    }

    private fun downLoadDataAndChangeDbUser() {
        //创建数据库
        var lastUser = lastUser

        lastUser?.let {
            //更新user
            when (isShowType) {
                1 -> {
                    it.last_region_id = regionBean?.id.toString()
                    it.last_authorizer_user_id = regionBean?.authorizer_id.toString()
                }
                2 -> {
                    it.last_region_id = regionBeanAuthorize?.id.toString()
                    it.last_authorizer_user_id = regionBeanAuthorize?.authorizer_id.toString()
                }
            }

            DBUtils.deleteAllData()

            //创建数据库
            AccountModel.initDatBase(it)
            //更新last—region-id
            DBUtils.saveUser(it)
            //下拉数据
            SyncDataPutOrGetUtils.syncGetDataStart(it, syncCallbackGet)

            view?.findViewById<LinearLayout>(R.id.pop_unbind_net_ly)?.isClickable = false
            view?.findViewById<ImageView>(R.id.pop_unbind_net)?.setImageResource(R.mipmap.icon_untied)

            isShowType = 3
        }
    }

    private fun showUnbindDialog() {
        val builder = android.support.v7.app.AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.warm_unbind_authorize_config, regionBeanAuthorize!!.name))
        builder.setNegativeButton(getString(R.string.btn_ok)) { dialog, _ ->
            //解除授权
            //authorizer_id授权用户id  rid区域id
            RegionModel.dropAuthorizeRegion(regionBeanAuthorize!!.authorizer_id, regionBeanAuthorize!!.id)
                    ?.subscribe({
                        isShowType = 2
                        initData()
                        ToastUtils.showShort(getString(R.string.unbundling_success))
                        PopUtil.dismiss(pop)
                    }, {
                        ToastUtils.showShort(it.message)
                    })
            dialog.dismiss()
        }
        builder.setPositiveButton(getString(R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }

        builder.create().show()
    }


    var syncCallbackGet: SyncCallback = object : SyncCallback {
        override fun start() {}
        override fun complete() {
            Log.e(TAG, "zcl******下拉数据成功")
            view?.findViewById<ImageView>(R.id.pop_user_net)?.setImageResource(R.drawable.icon_use_blue)
            initData()
        }

        override fun error(msg: String) {}
    }

    private fun showPop(pop: PopupWindow, gravity: Int) {
        if (!pop.isShowing)
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
                        Log.e(TAG, "zcl***parse_result***$result")
                        postParseCode(result)
                    } else if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_FAILED) {
                        Toast.makeText(this, getString(R.string.fail_parse_qr), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun postParseCode(result: String?) {
        result?.let { it1 ->
            RegionModel.parseQRCode(it1, NetworkFactory.md5(lastUser!!.password))?.subscribe(object : NetworkObserver<String>() {
                override fun onNext(t: String) {
                    isShowType = 3
                    initData()
                    ToastUtils.showShort(getString(R.string.scan_success))
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

    override fun onResume() {
        super.onResume()
        isShowType = 3
        initData()
    }

    override fun notifyWSData() {
        PopUtil.dismiss(pop)
        PopUtil.dismiss(popAdd)
        isShowType = 3
        initData()
    }

    @SuppressLint("CheckResult")
    override fun notifyWSTransferData() {
        super.notifyWSTransferData()

    }

}

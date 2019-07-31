package com.dadoutek.uled.region


import android.annotation.SuppressLint
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.dadoutek.uled.R
import com.dadoutek.uled.adapter.AreaItemAdapter
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbRegion
import com.dadoutek.uled.model.DbModel.DbUser
import com.dadoutek.uled.model.HttpModel.RegionModel
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.util.PopUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_network.*
import kotlinx.android.synthetic.main.toolbar.*


class NetworkActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var popAdd: PopupWindow
    private var viewAdd: View? = null
    var dbUser: DbUser? = null
    var adapter: AreaItemAdapter? = null
    var list: MutableList<DbRegion>? = null
    var pop: PopupWindow? = null
    var view: View? = null
    var root: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network)
        root = View.inflate(this, R.layout.activity_network, null)
        list = ArrayList()
        initView()
        initData()
    }

    private fun initView() {
        toolbarTv.visibility = View.VISIBLE
        toolbarTv.text = getString(R.string.area)
        img_function1.visibility = View.VISIBLE
        image_bluetooth.visibility = View.VISIBLE
        image_bluetooth.setImageResource(R.mipmap.icon_scanning)

        dbUser = DBUtils.lastUser
        makePop()
        initListener()
    }

    private fun initListener() {
        img_function1.setOnClickListener { PopUtil.show(popAdd, window.decorView, Gravity.CENTER) }
        image_bluetooth.setOnClickListener { }
    }

    @SuppressLint("CheckResult")
    private fun addRegion(text: Editable) {
        val dbRegion = DbRegion()

        dbRegion.installMesh = Constant.PIR_SWITCH_MESH_NAME
        dbRegion.installMeshPwd = "123"
        dbRegion.name = text.toString()
        dbRegion.id = DBUtils.lastRegion.id+1
        DbUser()

    /*    RegionModel.addRegions(dbUser!!.token, dbRegion, dbRegion.id)!!.subscribe {
            if (it.isSucess) {
                initData()
            }
        }*/

        NetworkFactory.getApi()
                .addRegionNew(dbUser!!.token, dbRegion, dbRegion.id)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    if (it.isSucess) {
                        initData()
                    }
                }
               // .observeOn(AndroidSchedulers.mainThread())
    }

    @SuppressLint("CheckResult")
    private fun initData() {
        if (dbUser != null) {
            RegionModel.get(dbUser!!.token)?.subscribe { it ->
                setData(it)
            }
        }
    }

    private fun setData(it: MutableList<DbRegion>?) {
        if (list!!.isEmpty()){
            list = it
            area_account.text = getString(R.string.current_account) + ": +" + dbUser!!.phone
            area_recycleview.layoutManager = LinearLayoutManager(this@NetworkActivity, LinearLayoutManager.VERTICAL, false)
            adapter = AreaItemAdapter(R.layout.item_area_net, it!!, dbUser!!.last_region_id)
            adapter!!.setOnItemClickListener { _, _, position -> setPop(position) }
            area_recycleview.adapter = adapter
        }else{
            list!!.addAll(it!!)
            adapter!!.notifyDataSetChanged()
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
            it.findViewById<ConstraintLayout>(R.id.pop_net_ly).setOnClickListener(this)
            it.findViewById<ImageView>(R.id.pop_user_net).setOnClickListener(this)
            it.findViewById<ImageView>(R.id.pop_del_net).setOnClickListener(this)
            it.findViewById<ImageView>(R.id.pop_share_net).setOnClickListener(this)
            it.findViewById<ImageView>(R.id.pop_update_net).setOnClickListener(this)
            it.findViewById<TextView>(R.id.pop_creater_name).text = getString(R.string.creater_name) + dbUser!!.phone
            //  it.findViewById<ImageView>(R.id.pop_qr_img).setOnClickListener(this)
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
        val dbRegion = list!![position]
        view?.let {
            it.findViewById<TextView>(R.id.pop_net_name).text = dbRegion.name
            it.findViewById<TextView>(R.id.pop_equipment_num).text = getString(R.string.equipment_quantity) + list!!.size
            if (dbRegion.id.toString() == dbUser!!.last_region_id)
                it.findViewById<ImageView>(R.id.pop_user_net).setImageResource(R.drawable.icon_use_blue)
            else
                it.findViewById<ImageView>(R.id.pop_user_net).setImageResource(R.drawable.icon_use)
        }
        root?.let { PopUtil.show(pop, it, Gravity.BOTTOM) }
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.pop_view -> {
                PopUtil.dismiss(pop)
            }
            R.id.pop_net_ly -> {
                view!!.findViewById<TextView>(R.id.pop_net_name).text = "11"
            }
            R.id.pop_user_net -> {
                view!!.findViewById<TextView>(R.id.pop_net_name).text = "22"
            }
            R.id.pop_del_net -> {
                view!!.findViewById<TextView>(R.id.pop_net_name).text = "33"
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

    override fun onDestroy() {
        super.onDestroy()
        PopUtil.dismiss(pop)
    }
}



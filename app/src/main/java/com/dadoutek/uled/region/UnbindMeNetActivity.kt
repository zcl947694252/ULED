package com.dadoutek.uled.region

import android.annotation.SuppressLint
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.BaseActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.httpModel.RegionModel
import com.dadoutek.uled.region.adapter.UnbindNetWorkAdapter
import com.dadoutek.uled.region.bean.RegionBean
import kotlinx.android.synthetic.main.template_recycleview_with_title.*
import kotlinx.android.synthetic.main.toolbar.*


class UnbindMeNetActivity : BaseActivity() {
    var adapter: UnbindNetWorkAdapter? = null
    var unbindBean: RegionBean.RefUsersBean? = null
    var regionbBean: RegionBean? = null
    var list: List<RegionBean.RefUsersBean>? = null
    override fun setLayoutID(): Int {
        return R.layout.activity_unbind_network
    }

    override fun initView() {
        image_bluetooth.visibility = View.GONE
        toolbarTv.text = getString(R.string.unbind_network)
        toolbar.setNavigationIcon(R.mipmap.icon_return)
        toolbar.setNavigationOnClickListener { finish() }
    }

    override fun initData() {
        regionbBean = intent.getSerializableExtra(Constant.SHARE_PERSON) as RegionBean
        recycleview_title_recycle.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        list = regionbBean?.ref_users
        setData()
    }

    @SuppressLint("StringFormatMatches")
    private fun setData() {
        list?.let {
            recycleview_title_title.text = getString(R.string.share_person_num_b, it.size)
            adapter = UnbindNetWorkAdapter(R.layout.item_unbind_network, it)
            adapter!!.setOnItemChildClickListener { _, _, position ->
                showUnbindDialog(position)
            }
            recycleview_title_recycle.adapter = adapter
        }
    }


    @SuppressLint("StringFormatMatches")
    private fun showUnbindDialog(position:Int) {
        unbindBean = list?.get(position)
        val builder = AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.warm_unbind_config, unbindBean?.phone))
        builder.setNegativeButton(getString(R.string.confirm)) { dialog, _ ->
            unbindBean?.id?.let {
                regionbBean?.id?.let { it1 ->
                    RegionModel.cancelAuthorize(it, it1.toInt())?.subscribe( {
                            adapter?.remove(position)
                            adapter!!.notifyDataSetChanged()
                            recycleview_title_title.text = getString(R.string.share_person_num_b, adapter!!.itemCount)
                            ToastUtils.showLong(getString(R.string.unbundling_success))
                        }, {
                            ToastUtils.showLong(it.localizedMessage)
                    })
                }
            }
            dialog.dismiss()
        }
        builder.setPositiveButton(getString(R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    override fun initListener() {}
}

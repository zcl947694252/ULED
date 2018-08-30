package com.dadoutek.uled.model.HttpModel

import android.content.Context
import android.os.Environment
import android.util.Log
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.laojiang.retrofithttp.weight.downfilesutils.FinalDownFiles
import com.laojiang.retrofithttp.weight.downfilesutils.action.FinalDownFileResult
import com.laojiang.retrofithttp.weight.downfilesutils.downfiles.DownInfo
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object DownLoadFileModel {
     fun download(url:String,context: Context) {
        val downUrl = arrayOf(url)
        val localUrl = Environment.getExternalStorageDirectory().toString()+"/bjhj/accessory/izaodao_app2.bin";

        val finalDownFiles = FinalDownFiles(true, context, downUrl[0],
                 localUrl, object : FinalDownFileResult() {
            override fun onSuccess(downInfo: DownInfo?) {
                super.onSuccess(downInfo)
                Log.i("成功==", downInfo!!.toString())
            }

            override fun onCompleted() {
                super.onCompleted()
                Log.i("完成==", "./...")
            }

            override fun onStart() {
                super.onStart()
                Log.i("开始==", "./...")
            }

            override fun onPause() {
                super.onPause()
                Log.i("暂停==", "./...")
            }

            override fun onStop() {
                super.onStop()
                Log.i("结束了一切", "是的没错")
            }

            override fun onLoading(readLength: Long, countLength: Long) {
                super.onLoading(readLength, countLength)
                Log.i("下载过程==", countLength.toString() + "")
            }
        })
    }

    fun getUrl(): Observable<String>? {
        return NetworkFactory.getApi()
                .getFirmwareUrl()
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                }
                .observeOn(AndroidSchedulers.mainThread())
    }
}
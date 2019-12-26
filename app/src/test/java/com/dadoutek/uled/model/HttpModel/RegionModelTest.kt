package com.dadoutek.uled.model.HttpModel

import com.dadoutek.uled.network.NetworkFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.junit.Test

/**
 * 创建者     ZCL
 * 创建时间   2019/12/25 12:02
 * 描述
 *
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class RegionModelTest{
    @Test
    fun  test(){
        NetworkFactory.getApi()
                .allCodeRemove("sdfsdfsdf")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        {

                        },
                        {

                        }
                )
    }
}
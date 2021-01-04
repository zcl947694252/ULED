package com.dadoutek.uled.model.httpModel

import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbEightSwitch
import com.dadoutek.uled.model.dbModel.DbSwitch
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.Response
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * 创建者     ZCL
 * 创建时间   2020/1/16 15:18
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */

object EightSwitchMdodel {

    fun add(switch: DbSwitch, changeId: Long): Observable<String>? {
        /**
         * 6、添加/更新八键开关（new） POST
         * https://dev.dadoutek.com/smartlight_java/switch/8ks/add/{swid}
         * firmwareVersion	否	   string	固件版本号
         * meshAddr	    是	   int	    mesh地址
         * name	       是	 string	   名字
         * macAddr	      是	string	   mac地址
         * productUUID	 是	    int	productUUID
         * index	否	int	排序
         * keys	是	list or string	key数组或者json格式的字符串
         * key既可以是对象数组，也可以是json格式的字符串。
         *
         * {"featureId":28,"reserveValue_A":0,"reserveValue_B":0,"keyId":1}
         */
        return NetworkFactory.getApi()
                .addSwitch8k(switch.id, switch.version, switch.meshAddr, switch.name, switch.macAddr, switch.productUUID, 0, switch.keys)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(changeId)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun add8k(switch: DbEightSwitch, changeId: Long): Observable<String>? {
        return NetworkFactory.getApi()
                // .addSwitch8k(token,dbChild,changeId!!.toInt())
                .addSwitch8k(switch.id, switch.firmwareVersion, switch.meshAddr, switch.name, switch.macAddr, switch.productUUID, 0, switch.keys)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(changeId)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun update(switch: DbSwitch, changeId: Long): Observable<String>? {
        val list = mutableListOf(1)
        val body = BatchRemove8kBody()
        body.idList = list

        return NetworkFactory.getApi()
                // .addSwitch8k(token,dbChild,changeId!!.toInt())
                .addSwitch8k(switch.id, switch.version, switch.meshAddr, switch.name, switch.macAddr, switch.productUUID, 0, switch.keys)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(changeId)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }
    fun update8k(switch: DbEightSwitch, changeId: Long): Observable<String>? {
        val list = mutableListOf(1)
        val body = BatchRemove8kBody()
        body.idList = list

        return NetworkFactory.getApi()
                // .addSwitch8k(token,dbChild,changeId!!.toInt())
                .addSwitch8k(switch.id, DeviceType.EIGHT_SWITCH_VERSION, switch.meshAddr, switch.name, switch.macAddr, DeviceType.SCENE_SWITCH, 1, switch.keys)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(changeId)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun delete(swid: Long, changeId: Long): Observable<String>? {
        return NetworkFactory.getApi()
                .removeSwitch8k(swid)
                .compose(NetworkTransformer())
                .observeOn(Schedulers.io())
                .doOnNext {
                    DBUtils.deleteDbDataChange(changeId)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun get(): Observable<Response<MutableList<DbSwitch>>>? {
        return NetworkFactory.getApi().getSwitch8kList(true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }
}
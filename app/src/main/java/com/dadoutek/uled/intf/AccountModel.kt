package com.dadoutek.uled.intf

import android.text.TextUtils
import com.dadoutek.uled.DbModel.DBUtils
import com.dadoutek.uled.DbModel.DbUser
import com.dadoutek.uled.TelinkLightApplication
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DaoSessionInstance
import com.dadoutek.uled.model.Response
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.util.NetworkUtils
import com.mob.tools.utils.DeviceHelper.getApplication
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

//看起来像存放静态方法的静态类，实际上就是单例模式。
object AccountModel {
    fun login(phone: String, password: String, channel: String): Observable<DbUser> {
        lateinit var account: String
        return NetworkUtils.getApi()
                .getAccount(phone, channel)
                .compose(NetworkTransformer())
                .flatMap { response: String ->
                    account = response
                    NetworkUtils.getApi()
                            .getsalt(response)
                            .compose(NetworkTransformer())

                }
                .flatMap { response: String ->
                    val salt = response
                    val md5Pwd = NetworkUtils.md5(
                            NetworkUtils.md5(
                                    NetworkUtils.md5(password) + account) + salt)
                    NetworkUtils.getApi().login(account, md5Pwd)
                            .compose(NetworkTransformer())
                }
                .observeOn(Schedulers.io())
                .doOnNext {
                    setIsLogin(true)
                    initDatBase(it)
                    setupMesh(phone)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }


    private fun initDatBase(user: DbUser) {
        //首先保存当前数据库名
        SharedPreferencesHelper.putString(TelinkLightApplication.getInstance(), Constant.DB_NAME_KEY, user.account)

        //数据库分库
        DaoSessionInstance.destroySession()
        DaoSessionInstance.getInstance()

        //从云端用户表同步数据到本地
//        dbUser = user
        DBUtils.saveUser(user)
        DBUtils.createAllLightControllerGroup()
    }

    private fun setIsLogin(isLogin: Boolean) {
        SharedPreferencesHelper.putBoolean(TelinkLightApplication.getInstance(), Constant.IS_LOGIN, isLogin)
    }

    private fun setupMesh(phone: String) {


        val name = SharedPreferencesHelper.getMeshName(TelinkLightApplication.getInstance())
        val pwd = SharedPreferencesHelper.getMeshPassword(TelinkLightApplication.getInstance())

        if (TextUtils.isEmpty(name) && TextUtils.isEmpty(pwd)) {
            //                Mesh mesh = (Mesh) FileSystem.readAsObject(this, name + "." + pwd);
            val application = getApplication() as TelinkLightApplication
            val mesh = application.mesh
            mesh.name = phone
            mesh.password = Constant.NEW_MESH_PASSWORD
            mesh.factoryName = Constant.DEFAULT_MESH_FACTORY_NAME
            mesh.password = Constant.DEFAULT_MESH_FACTORY_PASSWORD
            mesh.saveOrUpdate(TelinkLightApplication.getInstance())
            application.setupMesh(mesh)
            SharedPreferencesHelper.saveMeshName(TelinkLightApplication.getInstance(), phone)
            SharedPreferencesHelper.saveMeshPassword(TelinkLightApplication.getInstance(), Constant.NEW_MESH_PASSWORD)
        }
    }


}
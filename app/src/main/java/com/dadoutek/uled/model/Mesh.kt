package com.dadoutek.uled.model

import android.content.Context
import android.text.TextUtils
import com.dadoutek.uled.util.FileSystem
import java.io.Serializable
import java.util.*

class Mesh : Serializable {

    var name: String? = null
    var password: String? = null
    var factoryName: String? = null
    var factoryPassword: String? = null
    var otaDevice: OtaDevice? = null
    private var lastMeshAddress: Int = 0

    //public String otaDevice;

    //    public List<Integer> allocDeviceAddress;
    var devices: MutableList<DadouDeviceInfo>? = ArrayList()

/*
    fun generateMeshAddr(): Int {
        val lights = DBUtils.allLight.map { it.meshAddr }
        val curtain = DBUtils.allCurtain.map { it.meshAddr }
        val relay = DBUtils.allRely.map { it.meshAddr }
        var meshAddress: Int = -1

        val addressList = mutableListOf<Int>()
        addressList.addAll(lights)
        addressList.addAll(curtain)
        addressList.addAll(relay)
        addressList.sortBy { it }


        when {
            addressList.isEmpty() -> meshAddress = DEVICE_ADDRESS_MIN
            else -> {
                LogUtils.d("generateMeshAddr addressList.last() = ${addressList.last()}")
                meshAddress = addressList.last() + 1
            }
        }
        LogUtils.d("generateMeshAddr = $meshAddress")
        return meshAddress
    }
*/

/*
    fun generateMeshAddr(): Int {
        val lights = DBUtils.allLight
        val curtain = DBUtils.allCurtain
        val relay = DBUtils.allRely
        var meshAddress: Int = -1

        when {
            lights.isEmpty() && curtain.isEmpty() && relay.isEmpty() -> meshAddress = 1
            lights.size > MeshUtils.DEVICE_ADDRESS_MAX && curtain.size > MeshUtils.DEVICE_ADDRESS_MAX
                    && relay.size > MeshUtils.DEVICE_ADDRESS_MAX -> meshAddress = -1
            else -> {
                var address = lights.map { it.meshAddr }
                var curtains = curtain.map { it.meshAddr }
                var relays = relay.map { it.meshAddr }

                for (meshAddr in MeshUtils.DEVICE_ADDRESS_MIN..MeshUtils.DEVICE_ADDRESS_MAX) {
                    if (!address.contains(meshAddr) && !curtains.contains(meshAddr) && !relays.contains(meshAddr)) {//判断该值是不是再已安装里面 如果不是说明该值就是当前设倍数
                        meshAddress = meshAddr
                        break
                    }
                }
            }
        }
        return meshAddress
    }
*/

    val isOtaProcessing: Boolean
        get() = if (name == null || password == null || otaDevice == null ||
                TextUtils.isEmpty(otaDevice!!.mac) || TextUtils.isEmpty(otaDevice!!.meshName) || TextUtils.isEmpty(otaDevice!!.meshPwd)) {
            false
        } else name == otaDevice!!.meshName && password == otaDevice!!.meshPwd

    fun getDevice(meshAddress: Int): DadouDeviceInfo? {
        if (this.devices == null)
            return null

        for (info in devices!!) {
            if (info.meshAddress == meshAddress)
                return info
        }
        return null
    }

    fun removeDeviceByMeshAddress(meshAddress: Int): Boolean {
        if (devices == null || devices!!.size == 0) {
            return false
        }

        val infoIterator = devices!!.iterator()
        while (infoIterator.hasNext()) {
            if (infoIterator.next().meshAddress == meshAddress) {
                infoIterator.remove()
                return true
            }
        }
        return false
    }

    fun saveOrUpdate(context: Context): Boolean {
        return FileSystem.writeAsObject(context, "$name.$password", this)
    }

    override fun toString(): String {
        return "Mesh(name=$name, password=$password, factoryName=$factoryName, factoryPassword=$factoryPassword, otaDevice=$otaDevice, lastMeshAddress=$lastMeshAddress, devices=$devices)"
    }

    companion object {
        private const val serialVersionUID = 1L
    }


}



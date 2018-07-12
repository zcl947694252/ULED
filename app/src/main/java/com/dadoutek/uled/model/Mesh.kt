package com.dadoutek.uled.model

import android.content.Context
import android.text.TextUtils
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.util.FileSystem
import com.telink.util.MeshUtils
import java.io.Serializable
import java.util.*

class Mesh : Serializable {

    var name: String? = null
    var password: String? = null
    var factoryName: String? = null
    var factoryPassword: String? = null
    var otaDevice: OtaDevice? = null

    //public String otaDevice;

    //    public List<Integer> allocDeviceAddress;
    var devices: MutableList<DeviceInfo>? = ArrayList()

    /*int address = MeshUtils.allocDeviceAddress(this.allocDeviceAddress);

        if (address != -1) {
            if (this.allocDeviceAddress == null)
                this.allocDeviceAddress = new ArrayList<>();
            this.allocDeviceAddress.add(address);
        }

        return address;*/
    val deviceAddress: Int
        get() {
            val lights = DBUtils.getAllLight()
            if (lights == null || lights.size == 0) {
                return 1
            }

            flag_index@ for (i in MeshUtils.DEVICE_ADDRESS_MIN until MeshUtils.DEVICE_ADDRESS_MAX) {
                for (light in lights) {
                    if (light.meshAddr == i) {
                        continue@flag_index
                    }
                }
                return i
            }

            return -1
        }

    fun generateMeshAddr(): Int {
        val lights = DBUtils.getAllLight()

        var meshAddress: Int = -1
        when {
            lights.isEmpty() -> meshAddress = 1
            lights.size >= MeshUtils.DEVICE_ADDRESS_MAX -> meshAddress = -1
            else -> {
                for (meshAddr in MeshUtils.DEVICE_ADDRESS_MIN until MeshUtils.DEVICE_ADDRESS_MAX) {
                    val address = lights.map { it.meshAddr }
                    if (!address.contains(meshAddr)) {
                        meshAddress = meshAddr
                        break
                    }
                }
            }
        }
        return meshAddress
    }

    val isOtaProcessing: Boolean
        get() = if (name == null || password == null || otaDevice == null ||
                TextUtils.isEmpty(otaDevice!!.mac) || TextUtils.isEmpty(otaDevice!!.meshName) || TextUtils.isEmpty(otaDevice!!.meshPwd)) {
            false
        } else name == otaDevice!!.meshName && password == otaDevice!!.meshPwd

    fun getDevice(meshAddress: Int): DeviceInfo? {
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
        //        return FileSystem.writeAsObject("telink.meshs", this);
    }

    companion object {

        private const val serialVersionUID = 1L
    }
}

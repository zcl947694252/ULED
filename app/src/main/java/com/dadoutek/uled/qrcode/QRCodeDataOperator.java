package com.dadoutek.uled.qrcode;

import com.dadoutek.uled.model.DeviceInfo;
import com.dadoutek.uled.model.Mesh;
import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kee on 2017/12/27.
 */

public class QRCodeDataOperator {

    public String provideStr() {
        Mesh mesh = TelinkLightApplication.Companion.getApp().getMesh();
        if (mesh == null) {
            return "{}";
        }
        TmpMesh tmpMesh = new TmpMesh();
        tmpMesh.n = mesh.getName();
        tmpMesh.p = mesh.getPassword();
        if (mesh.getDevices() != null) {
            List<TmpDeviceInfo> deviceInfoList = new ArrayList<>();

            TmpDeviceInfo tmpDeviceInfo;
            for (DeviceInfo deviceInfo : mesh.getDevices()) {
                tmpDeviceInfo = new TmpDeviceInfo();
                tmpDeviceInfo.m = deviceInfo.macAddress;
                tmpDeviceInfo.a = deviceInfo.meshAddress;
                tmpDeviceInfo.v = deviceInfo.firmwareRevision;
                tmpDeviceInfo.pu = deviceInfo.productUUID;

                deviceInfoList.add(tmpDeviceInfo);
            }
            tmpMesh.d = deviceInfoList;
        }

        Gson gson = new Gson();
        return gson.toJson(tmpMesh);
    }

    class TmpMesh {
        String n;
        String p;
        List<TmpDeviceInfo> d;
    }

    class TmpDeviceInfo {
        String m;
        int a;
        String v;
        int pu;
    }
}

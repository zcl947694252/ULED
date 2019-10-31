package com.dadoutek.uled.qrcode;

import com.dadoutek.uled.model.DadouDeviceInfo;
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
            for (DadouDeviceInfo dadouDeviceInfo : mesh.getDevices()) {
                tmpDeviceInfo = new TmpDeviceInfo();
                tmpDeviceInfo.m = dadouDeviceInfo.macAddress;
                tmpDeviceInfo.a = dadouDeviceInfo.meshAddress;
                tmpDeviceInfo.v = dadouDeviceInfo.firmwareRevision;
                tmpDeviceInfo.pu = dadouDeviceInfo.productUUID;

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

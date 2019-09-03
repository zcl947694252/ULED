package com.dadoutek.uled.qrcode;

import android.text.TextUtils;

import com.dadoutek.uled.tellink.TelinkLightApplication;
import com.dadoutek.uled.model.DeviceInfo;
import com.dadoutek.uled.model.Mesh;
import com.dadoutek.uled.model.SharedPreferencesHelper;
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

    public boolean parseData(String data) {
        Gson gson = new Gson();
        TmpMesh tmpMesh = gson.fromJson(data, TmpMesh.class);
        if (tmpMesh != null && !TextUtils.isEmpty(tmpMesh.n) && !TextUtils.isEmpty(tmpMesh.p)) {
            Mesh newMesh = new Mesh();
            Mesh oldMesh = TelinkLightApplication.Companion.getApp().getMesh();

            newMesh.setName(tmpMesh.n);
            newMesh.setPassword(tmpMesh.p);
            newMesh.setFactoryName(oldMesh.getFactoryName());
            newMesh.setFactoryPassword(oldMesh.getFactoryPassword());

            newMesh.setDevices(new ArrayList<>());
            if (tmpMesh.d != null) {
                DeviceInfo deviceInfo;
                for (TmpDeviceInfo tmpDeviceInfo : tmpMesh.d) {
                    deviceInfo = new DeviceInfo();
                    deviceInfo.macAddress = tmpDeviceInfo.m;
                    deviceInfo.meshAddress = tmpDeviceInfo.a;
                    deviceInfo.firmwareRevision = tmpDeviceInfo.v;
                    deviceInfo.productUUID = tmpDeviceInfo.pu;
                    newMesh.getDevices().add(deviceInfo);
                }
            }
            newMesh.saveOrUpdate(TelinkLightApplication.Companion.getApp());
            SharedPreferencesHelper.saveMeshName(TelinkLightApplication.Companion.getApp(), newMesh.getName());
            SharedPreferencesHelper.saveMeshPassword(TelinkLightApplication.Companion.getApp(), newMesh.getPassword());
            TelinkLightApplication.Companion.getApp().setupMesh(newMesh);
            return true;
        }

        return false;
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

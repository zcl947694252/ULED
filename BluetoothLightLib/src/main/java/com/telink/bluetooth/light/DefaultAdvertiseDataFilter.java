/*
 * Copyright (C) 2015 The Telink Bluetooth Light Project
 *
 */
package com.telink.bluetooth.light;

import android.bluetooth.BluetoothDevice;

/**
 * 默认的广播过滤器
 * <p>根据VendorId识别设备.
 */
public final class DefaultAdvertiseDataFilter implements AdvertiseDataFilter<LightPeripheral> {

    private DefaultAdvertiseDataFilter() {
    }

    public static DefaultAdvertiseDataFilter create() {
        return new DefaultAdvertiseDataFilter();
    }

    @Override
    public LightPeripheral filter(BluetoothDevice device, int rssi, byte[] scanRecord) {
        int length = scanRecord.length;
        int packetPosition = 0;
        int packetContentLength;
        int packetSize;
        int position;
        int type;
        byte[] meshName = null;

        int rspData = 0;

        while (packetPosition < length) {

            packetSize = scanRecord[packetPosition];

            if (packetSize == 0)
                break;

            position = packetPosition + 1;
            type = scanRecord[position] & 0xFF;
            position++;

            if (type == 0x09) {

                packetContentLength = packetSize - 1;

                if (packetContentLength > 16 || packetContentLength <= 0)
                    return null;
                meshName = new byte[16];
                System.arraycopy(scanRecord, position, meshName, 0, packetContentLength);

            } else if (type == 0xFF) {
                rspData++;
                if (rspData == 2) {
                    int vendorId = ((scanRecord[position++] & 0xFF) << 8) + (scanRecord[position++] & 0xFF);
                    if (vendorId != Manufacture.getDefault().getVendorId())
                        return null;
                    int meshUUID = (scanRecord[position++] & 0xFF) + ((scanRecord[position++] & 0xFF) << 8);
                    position += 4;
                    int productUUID = (scanRecord[position++] & 0xFF) + ((scanRecord[position++] & 0xFF) << 8);
                    int status = scanRecord[position++] & 0xFF;
                    int meshAddress = (scanRecord[position++] & 0xFF) + ((scanRecord[position++] & 0xFF) << 8);
//                    String version = valueOf((char) scanRecord[39])+ (char) scanRecord[40] + (char) scanRecord[41]
//                            + (char) scanRecord[42] + (char) scanRecord[43] + (char) scanRecord[44];
                    String version = "";
                    byte b = scanRecord[position++];
                    int revType = b & 0xFF; //0XDD为版本号标识
                    final int VERSION_MARK = 0xDD;
                    //0xDD 后面15个Byte 为版本号
                    if(revType == VERSION_MARK) {
                        for(int i = position; i<position+15; i++)
                        {
                            if(scanRecord[i] != 0)
                            {
                                version += (char) scanRecord[i];
                            }
                        }
                    }
                    //TelinkLog.d(" scan mesh name service = "+ Arrays.bytesToString(meshName));
                    LightPeripheral light = new LightPeripheral(device, scanRecord, rssi, meshName, meshAddress,version);
                    light.putAdvProperty(LightPeripheral.ADV_MESH_NAME, meshName);
                    light.putAdvProperty(LightPeripheral.ADV_MESH_ADDRESS, meshAddress);
                    light.putAdvProperty(LightPeripheral.ADV_MESH_UUID, meshUUID);
                    light.putAdvProperty(LightPeripheral.ADV_PRODUCT_UUID, productUUID);
                    light.putAdvProperty(LightPeripheral.ADV_STATUS, status);

                    return light;
                }
            }

            packetPosition += packetSize + 1;
        }

        return null;
    }

    private String formatHex(String one) {
        if (one.length() > 2) {
            one = one.substring(one.length() - 2);
        } else if (one.length() == 1) {
            one = "0" + one;
        }
        return one;
    }
}

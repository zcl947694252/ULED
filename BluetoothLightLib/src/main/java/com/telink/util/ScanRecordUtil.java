package com.telink.util;

import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * 创建者     ZCL
 * 创建时间   2019/11/29 10:54
 * 描述
 * <p>
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
public class ScanRecordUtil {

    private static final String TAG = "---ScanRecordUtil";
    private static final int DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE = 0x03;
    private static final int DATA_TYPE_LOCAL_NAME_COMPLETE = 0x09;
    // Flags of the advertising data.
    private final int mAdvertiseFlags;
    // Transmission power level(in dB).
    private final int mTxPowerLevel;
    // Local name of the Bluetooth LE device.
    private final String mDeviceName;
    //Raw bytes of scan record.
    private final byte[] mBytes;
    private List<String> mUuids16S;             //16位UUID，之前有其他的UUID占着名称我就取的这名称

    /**
     * Returns the advertising flags indicating the discoverable mode and capability of the device. * Returns -1 if the flag field is not set.
     */
    public int getAdvertiseFlags() {
        return mAdvertiseFlags;
    }

    /**
     * Returns the transmission power level of the packet in dBm. Returns {@link Integer#MIN_VALUE} * if the field is not set. This value can be used to calculate the path loss of a received * packet using the following equation: * <p> * <code>pathloss = txPowerLevel - rssi</code>
     */
    public int getTxPowerLevel() {
        return mTxPowerLevel;
    }

    /**
     * Returns the local name of the BLE device. The is a UTF-8 encoded string.
     * 拿到设备的名称
     */
    @Nullable
    public String getDeviceName() {
        return mDeviceName;
    }

    /**
     * Returns raw bytes of scan record.
     */
    public byte[] getBytes() {
        return mBytes;
    }

    private ScanRecordUtil( List<String> mUuids16S,  int advertiseFlags, int txPowerLevel, String localName, byte[] bytes) {
        mDeviceName = localName;
        mAdvertiseFlags = advertiseFlags;
        mTxPowerLevel = txPowerLevel;
        mBytes = bytes;
        this.mUuids16S = mUuids16S;
    }

    /**
     * 获取16位UUID
     * @return
     */
    public List<String> getUuids16S() {
        return mUuids16S;
    }

    /**
     * 字节数组转换为十六进制字符串
     *
     * @param b
     *            byte[] 需要转换的字节数组
     * @return String 十六进制字符串
     */
    public static final String byte2hex(byte b[]) {
        if (b == null) {
            throw new IllegalArgumentException(
                    "Argument b ( byte array ) is null! ");
        }
        String hs = "";
        String stmp = "";
        for (int n = 0; n < b.length; n++) {
            stmp = Integer.toHexString(b[n] & 0xff);
            if (stmp.length() == 1) {
                hs = hs + "0" + stmp;
            } else {
                hs = hs + stmp;
            }
        }
        return hs.toUpperCase();
    }

    /**
     * 得到ScanRecordUtil 对象，主要逻辑
     * @param scanRecord
     * @return
     */
    public static ScanRecordUtil parseFromBytes(byte[] scanRecord) {
        if (scanRecord == null) {
            return null;
        }
        int currentPos = 0;
        int advertiseFlag = -1;
        List<String> uuids16 = new ArrayList<>();
        String localName = null;
        int txPowerLevel = Integer.MIN_VALUE;
        try {
            while (currentPos < scanRecord.length) {
                // length is unsigned int.
                int length = scanRecord[currentPos++] & 0xFF;
                if (length == 0) {
                    break;
                }
                // / Note the length includes the length of the field type itself.
                int dataLength = length - 1;
                // fieldType is unsigned int.
//                获取广播AD type
                int fieldType = scanRecord[currentPos++] & 0xFF;
                switch (fieldType) {
                    case DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE:
                        parseServiceUuid16(scanRecord, currentPos, dataLength, uuids16);
                        break;
                    case DATA_TYPE_LOCAL_NAME_COMPLETE:
                        localName = new String(extractBytes(scanRecord, currentPos, dataLength));
                        break;
                    default:
                        break;
                }
                currentPos += dataLength;
            }
          /*  if (uuids_16.isEmpty()){
                uuids_16 = null;
            }*/
            return new ScanRecordUtil(uuids16,  advertiseFlag, txPowerLevel, localName, scanRecord);
        } catch (Exception e) {
            Log.e(TAG, "unable to parse scan record: " + Arrays.bytesToString(scanRecord));
            // As the record is invalid, ignore all the parsed results for this packet
            // and return an empty record with raw scanRecord bytes in results
            return new ScanRecordUtil( null, -1, Integer.MIN_VALUE, null, scanRecord);
        }
    }


    /**
     * byte数组转16进制
     * @param bytes
     * @return
     */
    public static String bytesToHexFun3(byte[] bytes) {
        StringBuilder buf = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) { // 使用String的format方法进行转换
            buf.append(String.format("%02x", new Integer(b & 0xff)));
        }
        return buf.toString();
    }

    // 16位UUID
    private static int parseServiceUuid16(byte[] scanRecord, int currentPos, int dataLength, List<String> serviceUuids) {
        while (dataLength > 0) {
            byte[] uuidBytes = extractBytes(scanRecord, currentPos, dataLength);
            final byte[] bytes = new byte[uuidBytes.length];
            Log.v(TAG, "dataLength==uuidBytes.length" + (dataLength == uuidBytes.length));
            for (int i = 0; i < uuidBytes.length; i++) {
                bytes[i] = uuidBytes[uuidBytes.length - 1 - i];
            }

            serviceUuids.add(bytesToHexFun3(bytes));
            dataLength -= dataLength;
            currentPos += dataLength;
        }
        return currentPos;
    }

    // Helper method to extract bytes from byte array.
    //b帮助我们解析byte数组
    private static byte[] extractBytes(byte[] scanRecord, int start, int length) {
        byte[] bytes = new byte[length];
        System.arraycopy(scanRecord, start, bytes, 0, length);
        return bytes;
    }

}


        /*
         * Copyright (C) 2015 The Telink Bluetooth Light Project
         *
         */
        package com.telink.bluetooth;

        import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.blankj.utilcode.util.LogUtils;
import com.telink.util.Arrays;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.valueOf;

        /**
         * 蓝牙发送数据相关类
         * 蓝牙操作类
         * todo 更改mac地址4位
         */
        public class Peripheral extends BluetoothGattCallback {

            public static final int CONNECTION_PRIORITY_BALANCED = 0;
            public static final int CONNECTION_PRIORITY_HIGH = 1;
            public static final int CONNECTION_PRIORITY_LOW_POWER = 2;

            private static final int CONN_STATE_IDLE = 1;
            private static final int CONN_STATE_CONNECTING = 2;
            private static final int CONN_STATE_CONNECTED = 4;
            private static final int CONN_STATE_DISCONNECTING = 8;
            private static final int CONN_STATE_CLOSED = 16;

            private static final int RSSI_UPDATE_TIME_INTERVAL = 2000;

            protected final Queue<CommandContext> mInputCommandQueue =
                    new ConcurrentLinkedQueue<>();
            protected final Queue<CommandContext> mOutputCommandQueue =
                    new ConcurrentLinkedQueue<>();
            protected final Map<String, CommandContext> mNotificationCallbacks =
                    new ConcurrentHashMap<>();

            protected final Handler mTimeoutHandler = new Handler(Looper.getMainLooper());
            protected final Handler mRssiUpdateHandler = new Handler(Looper.getMainLooper());
            protected final Handler mDelayHandler = new Handler(Looper.getMainLooper());
            protected final Runnable mRssiUpdateRunnable = new RssiUpdateRunnable();
            protected final Runnable mCommandTimeoutRunnable = new CommandTimeoutRunnable();
            protected final Runnable mCommandDelayRunnable = new CommandDelayRunnable();

            private final Object mStateLock = new Object();
            //    private final Object mProcessLock = new Object();

            protected BluetoothDevice device;
            protected BluetoothGatt gatt;
            protected int rssi;
            protected byte[] scanRecord;
            protected String name;
            protected String mac;
            private boolean isConnecting = false;

            public String getVersion() {
                return version;
            }

            public void setVersion(String version) {
                this.version = version;
            }

            protected String version = "";
            protected String sixByteMac;
            protected byte[] macBytes;
            protected int type;

            protected int gwVoipState = 10;
            protected int gwWifiState = 10;
            protected List<BluetoothGattService> mServices;

            protected AtomicBoolean processing = new AtomicBoolean(false);

            protected boolean monitorRssi;
            protected int updateIntervalMill = 5 * 1000;
            protected int commandTimeoutMill = 10 * 1000;
            protected long lastTime;
            //    private int mConnState = CONN_STATE_IDLE;
            private AtomicInteger mConnState = new AtomicInteger(CONN_STATE_IDLE);

            public int getGwVoipState() {
                return gwVoipState;
            }

            public void setGwVoipState(int gwVoipState) {
                this.gwVoipState = gwVoipState;
            }

            private String
            get4ByteMac(String macString) {
                String[] strArray = macString.split(":");
                this.macBytes = new byte[4];
                this.macBytes[0] = (byte) (Integer.parseInt(strArray[5], 16) & 0xFF);//78-9C-E7
                // -08-A9-3D
                this.macBytes[1] = (byte) (Integer.parseInt(strArray[4], 16) & 0xFF);
                this.macBytes[2] = (byte) (Integer.parseInt(strArray[3], 16) & 0xFF);
                this.macBytes[3] = (byte) (Integer.parseInt(strArray[2], 16) & 0xFF);

                long mac4Byte =
                        (long) ((macBytes[0] << 24) & 0xFF000000 | (macBytes[1] << 16) & 0x00FF0000
                                | (macBytes[2] << 8) & 0x0000FF00 | macBytes[3] & 0xFF) & 0xFFFFFFFFL;

                String mac =
                        strArray[5] + ":" + strArray[4] + ":" + strArray[3] + ":" + strArray[2];
                return valueOf(mac4Byte);
            }

            public Peripheral(BluetoothDevice device, byte[] scanRecord, int rssi) {
                this.device = device;
                this.scanRecord = scanRecord;
                // LogUtils.v("zcl-----------收到广播包-------" + Arrays.bytesToHexString(scanRecord,
                // ","));
                this.rssi = rssi;
                this.name = device.getName();
                this.type = device.getType(); //ble
                // this.mac = get4ByteMac2(scanRecord);
                this.version = getVersion(scanRecord);
                this.mac = get4ByteMac(device.getAddress());
                this.sixByteMac = device.getAddress();
            }

            private String formatHex(String one) {
                if (one.length() > 2) {
                    one = one.substring(one.length() - 2);
                } else if (one.length() == 1) {
                    one = "0" + one;
                }
                return one;
            }

            /********************************************************************************
             * Public API
             *******************************************************************************/

            public BluetoothDevice getDevice() {
                return this.device;
            }

            public String getDeviceName() {
                return this.name;
            }

            public String getMacAddress() {
                return this.mac;
            }

            public String getSixByteMacAddress() {
                return this.sixByteMac;
            }

            public void setSixByteMacAddress(String mac) {
                this.sixByteMac = mac;
            }

            public List<BluetoothGattService> getServices() {
                return mServices;
            }

            public int getGwWifiState() {
                return gwWifiState;
            }

            public void setGwWifiState(int gwWifiState) {
                this.gwWifiState = gwWifiState;
            }

            public byte[] getMacBytes() {
                if (this.macBytes == null) {

                    long macLong = Long.valueOf(mac);
                    macBytes = new byte[4];
                    macBytes[0] = (byte) (macLong >> 24 & 0xFF);
                    macBytes[1] = (byte) (macLong >> 16 & 0xFF);
                    macBytes[2] = (byte) (macLong >> 8 & 0xFF);
                    macBytes[3] = (byte) (macLong & 0xFF);
                }

                return this.macBytes;
            }

            public int getType() {
                return this.type;
            }

            public int getRssi() {
                return this.rssi;
            }

            public boolean isConnected() {
                return this.mConnState.get() == CONN_STATE_CONNECTED;
            }

            public void connect(Context context) {
                this.lastTime = 0;

                if (this.mConnState.get() == CONN_STATE_IDLE/*&&!isConnecting*/) {
                    isConnecting = true;
                    synchronized (this) {
                        disconnect();
                        TelinkLog.d("Peripheral#connect " + this.getDeviceName() + " -- " + this.getMacAddress());
                        this.mConnState.set(CONN_STATE_CONNECTING);
                        this.gatt = this.device.connectGatt(context, false, this,
                                BluetoothDevice.TRANSPORT_LE);
                        if (this.gatt == null) {
                            isConnecting = false;
                            this.disconnect();
                            this.mConnState.set(CONN_STATE_IDLE);
                            TelinkLog.d("Peripheral# gatt NULL onDisconnect:" + this.getDeviceName() + " -- "
                                    + this.getMacAddress());
                            this.disconnect();
                        }
                    }
                }
            }

            public void disconnect() {
                if (this.gatt != null) {
                    this.gatt.disconnect();
                    refreshDeviceCache(gatt);
                    closeGatt();
                    LogUtils.v("zcl--------蓝牙相关---disconnect-------refreshDeviceCache");
                }

                //        TelinkLog.d("disconnect " + this.getDeviceName() + " -- " + this
                //        .getMacAddress());
            }

            private void clear() {

                this.processing.set(false);
                this.stopMonitoringRssi();
                this.cancelCommandTimeoutTask();
                this.mInputCommandQueue.clear();
                this.mOutputCommandQueue.clear();
                this.mNotificationCallbacks.clear();
                this.mDelayHandler.removeCallbacksAndMessages(null);
            }

            public boolean sendCommand(Command.Callback callback, Command command) {

                if (this.mConnState.get() != CONN_STATE_CONNECTED)
                    return false;

                CommandContext commandContext = new CommandContext(callback, command);
                this.postCommand(commandContext);

                return true;
            }

            public final void startMonitoringRssi(int interval) {

                this.monitorRssi = true;

                if (interval <= 0)
                    this.updateIntervalMill = RSSI_UPDATE_TIME_INTERVAL;
                else
                    this.updateIntervalMill = interval;
            }

            public final void stopMonitoringRssi() {
                this.monitorRssi = false;
                this.mRssiUpdateHandler.removeCallbacks(this.mRssiUpdateRunnable);
                this.mRssiUpdateHandler.removeCallbacksAndMessages(null);
            }

            public final boolean requestConnectionPriority(int connectionPriority) {
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && this.gatt.requestConnectionPriority(connectionPriority);
            }

            /********************************************************************************
             * Protected API
             *******************************************************************************/

            protected void onConnect() {
                this.requestConnectionPriority(CONNECTION_PRIORITY_HIGH);
                this.enableMonitorRssi(this.monitorRssi);
            }

            protected void onDisconnect() {
                this.enableMonitorRssi(false);
            }

            protected void onServicesDiscovered(List<BluetoothGattService> services) {
            }

            protected void onNotify(byte[] data, UUID serviceUUID, UUID characteristicUUID,
                                    Object tag) {
            }

            protected void onRssiChanged() {
            }

            protected void enableMonitorRssi(boolean enable) {

                if (enable) {
                    this.mRssiUpdateHandler.removeCallbacks(this.mRssiUpdateRunnable);
                    this.mRssiUpdateHandler.postDelayed(this.mRssiUpdateRunnable,
                            this.updateIntervalMill);
                } else {
                    this.mRssiUpdateHandler.removeCallbacks(this.mRssiUpdateRunnable);
                    this.mRssiUpdateHandler.removeCallbacksAndMessages(null);
                }
            }

            /********************************************************************************
             * Command Handler API
             *******************************************************************************/

            private void postCommand(CommandContext commandContext) {
                TelinkLog.d("postCommand");
                if (commandContext.command.delay < 0) {
                    synchronized (this.mOutputCommandQueue) {
                        this.mOutputCommandQueue.add(commandContext);
                        this.processCommand(commandContext);
                    }
                    return;
                }

                this.mInputCommandQueue.add(commandContext);
                //        synchronized (this.mProcessLock) {
                if (!this.processing.get()) {
                    this.processCommand();
                }
                //        }
            }

            private void processCommand() {
                TelinkLog.d("processing : " + this.processing);

                CommandContext commandContext;
                Command.CommandType commandType;

                synchronized (mInputCommandQueue) {
                    if (this.mInputCommandQueue.isEmpty())
                        return;
                    commandContext = this.mInputCommandQueue.poll();
                }

                if (commandContext == null || commandContext.command == null)
                    return;

                commandType = commandContext.command.type;
                if (commandType != Command.CommandType.ENABLE_NOTIFY && commandType != Command.CommandType.DISABLE_NOTIFY) {
                    synchronized (mOutputCommandQueue) {
                        this.mOutputCommandQueue.add(commandContext);
                    }

                    if (!this.processing.get())
                        this.processing.set(true);
                }

                int delay = commandContext.command.delay;
                if (delay > 0) {
                    long currentTime = System.currentTimeMillis();
                    if (lastTime == 0 || (currentTime - lastTime) >= delay)
                        this.processCommand(commandContext);
                    else
                        this.mDelayHandler.postDelayed(this.mCommandDelayRunnable, delay);
                } else {
                    this.processCommand(commandContext);
                }
            }

            synchronized private void processCommand(CommandContext commandContext) {
                if (commandContext != null) {
                    Command command = commandContext.command;
                    if (command == null)
                        return;

                    Command.CommandType commandType = command.type;
                    TelinkLog.d("processCommand : " + command.toString());

                    switch (commandType) {
                        case READ:
                            this.postCommandTimeoutTask();
                            this.readCharacteristic(commandContext, command.serviceUUID,
                                    command.characteristicUUID);
                            break;
                        case WRITE:
                            this.postCommandTimeoutTask();
                            this.writeCharacteristic(commandContext, command.serviceUUID,
                                    command.characteristicUUID,
                                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
                                    command.data);
                            break;
                        case WRITE_NO_RESPONSE:
                            this.postCommandTimeoutTask();
                            this.writeCharacteristic(commandContext, command.serviceUUID,
                                    command.characteristicUUID,
                                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE,
                                    command.data);//获取原始数据
                            break;
                        case ENABLE_NOTIFY:
                            this.enableNotification(commandContext, command.serviceUUID,
                                    command.characteristicUUID);
                            break;
                        case DISABLE_NOTIFY:
                            this.disableNotification(commandContext, command.serviceUUID,
                                    command.characteristicUUID);
                            break;
                    }
                }
            }

            private void commandCompleted() {
                TelinkLog.d("commandCompleted");

                if (this.processing.get())
                    this.processing.set(false);

                this.processCommand();
            }

            private void commandSuccess(CommandContext commandContext, Object data) {
                TelinkLog.d("commandSuccess");
                this.lastTime = System.currentTimeMillis();
                if (commandContext != null) {

                    Command command = commandContext.command;
                    Command.Callback callback = commandContext.callback;
                    commandContext.clear();

                    if (callback != null) {
                        callback.success(this, command,
                                data);
                    }
                }
            }

            private void commandSuccess(Object data) {
                CommandContext commandContext;
                commandContext = this.mOutputCommandQueue.poll();
                this.commandSuccess(commandContext, data);
            }

            private void commandError(CommandContext commandContext, String errorMsg) {
                TelinkLog.d("commandError:" + errorMsg);
                this.lastTime = System.currentTimeMillis();
                if (commandContext != null) {

                    Command command = commandContext.command;
                    Command.Callback callback = commandContext.callback;
                    commandContext.clear();

                    if (callback != null) {
                        callback.error(this, command,
                                errorMsg);
                    }
                }
            }

            private void commandError(String errorMsg) {
                CommandContext commandContext;
                commandContext = this.mOutputCommandQueue.poll();
                this.commandError(commandContext, errorMsg);
            }

            private boolean commandTimeout(CommandContext commandContext) {
                TelinkLog.d("commandTimeout");
                this.lastTime = System.currentTimeMillis();
                if (commandContext != null) {
                    Command command = commandContext.command;
                    Command.Callback callback = commandContext.callback;
                    commandContext.clear();

                    if (callback != null) {
                        return callback.timeout(this, command);
                    }
                }

                return false;
            }

            private void postCommandTimeoutTask() {

                if (this.commandTimeoutMill <= 0)
                    return;

                this.mTimeoutHandler.removeCallbacksAndMessages(null);
                this.mTimeoutHandler.postDelayed(this.mCommandTimeoutRunnable,
                        this.commandTimeoutMill);
            }

            private void cancelCommandTimeoutTask() {
                this.mTimeoutHandler.removeCallbacksAndMessages(null);
            }

            /********************************************************************************
             * Private API
             *******************************************************************************/
            /**
             * 蓝牙读取操作
             *
             * @param commandContext
             * @param serviceUUID
             * @param characteristicUUID
             */
            private void readCharacteristic(CommandContext commandContext,
                                            UUID serviceUUID, UUID characteristicUUID) {

                boolean success = true;
                String errorMsg = "";

                if (gatt == null)
                    return;
                BluetoothGattService service = this.gatt.getService(serviceUUID);

                if (service != null) {

                    BluetoothGattCharacteristic characteristic = service
                            .getCharacteristic(characteristicUUID);

                    if (characteristic != null) {

                        if (!this.gatt.readCharacteristic(characteristic)) {
                            success = false;
                            errorMsg = "read characteristic error";
                        }

                    } else {
                        success = false;
                        errorMsg = "read characteristic error: characteristic-null ";
                    }
                } else {
                    success = false;
                    errorMsg = "service is not offered by the remote device";
                }

                if (!success) {
                    this.commandError(errorMsg);
                    this.commandCompleted();
                }
            }

            /**
             * 蓝牙写入操作
             *
             * @param commandContext
             * @param serviceUUID
             * @param characteristicUUID
             * @param writeType
             * @param data
             */
            private void writeCharacteristic(CommandContext commandContext,
                                             UUID serviceUUID, UUID characteristicUUID,
                                             int writeType,
                                             byte[] data) {
                boolean success = true;
                String errorMsg = "";

                //        Log.d("dadou_writeCha", "writeCharacteristic: "+data.length);

                if (gatt == null)
                    return;
                BluetoothGattService service = this.gatt.getService(serviceUUID);

                if (service != null) {
                    BluetoothGattCharacteristic characteristic = this
                            .findWritableCharacteristic(service, characteristicUUID, writeType);
                    if (characteristic != null) {

                        if (data.length > 20) {
                            sendData(data, characteristic, writeType);
                            isOutOfSize = true;
                        } else {
                            isOutOfSize = false;
                            characteristic.setValue(data);
                            characteristic.setWriteType(writeType);
                            if (!this.gatt.writeCharacteristic(characteristic)) {
                                success = false;
                                errorMsg = "write characteristic error";
                                Log.d("myerror", "writeCharacteristic: ");
                            }
                        }

                    } else {
                        success = false;
                        errorMsg = "no characteristic";
                    }
                } else {
                    success = false;
                    errorMsg = "service is not offered by the remote device";
                }

                if (!success) {
                    this.commandError(errorMsg);
                    this.commandCompleted();
                    //            Log.d("checkXConnect-wrerro", "writeCharacteristic: ");
                }
            }

            private void enableNotification(CommandContext commandContext,
                                            UUID serviceUUID, UUID characteristicUUID) {

                boolean success = true;
                String errorMsg = "";

                BluetoothGattService service = this.gatt.getService(serviceUUID);

                if (service != null) {

                    BluetoothGattCharacteristic characteristic = this
                            .findNotifyCharacteristic(service, characteristicUUID);

                    if (characteristic != null) {

                        if (!this.gatt.setCharacteristicNotification(characteristic,
                                true)) {
                            success = false;
                            errorMsg = "enable notification error";
                        } else {
                            String key = this.generateHashKey(serviceUUID,
                                    characteristic);
                            this.mNotificationCallbacks.put(key, commandContext);
                        }

                    } else {
                        success = false;
                        errorMsg = "no characteristic";
                    }

                } else {
                    success = false;
                    errorMsg = "service is not offered by the remote device";
                }

                if (!success) {
                    this.commandError(commandContext, errorMsg);
                }

                this.commandCompleted();
            }

            private void disableNotification(CommandContext commandContext,
                                             UUID serviceUUID, UUID characteristicUUID) {

                boolean success = true;
                String errorMsg = "";

                BluetoothGattService service = this.gatt.getService(serviceUUID);

                if (service != null) {

                    BluetoothGattCharacteristic characteristic = this
                            .findNotifyCharacteristic(service, characteristicUUID);

                    if (characteristic != null) {

                        String key = this.generateHashKey(serviceUUID, characteristic);
                        this.mNotificationCallbacks.remove(key);

                        if (!this.gatt.setCharacteristicNotification(characteristic,
                                false)) {
                            success = false;
                            errorMsg = "disable notification error";
                        }

                    } else {
                        success = false;
                        errorMsg = "no characteristic";
                    }

                } else {
                    success = false;
                    errorMsg = "service is not offered by the remote device";
                }

                if (!success) {
                    this.commandError(commandContext, errorMsg);
                }

                this.commandCompleted();
            }

            private BluetoothGattCharacteristic findWritableCharacteristic(
                    BluetoothGattService service, UUID characteristicUUID, int writeType) {

                BluetoothGattCharacteristic characteristic = null;

                int writeProperty = BluetoothGattCharacteristic.PROPERTY_WRITE;

                if (writeType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
                    writeProperty = BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
                }

                List<BluetoothGattCharacteristic> characteristics = service
                        .getCharacteristics();

                for (BluetoothGattCharacteristic c : characteristics) {
                    if ((c.getProperties() & writeProperty) != 0
                            && characteristicUUID.equals(c.getUuid())) {
                        characteristic = c;
                        break;
                    }
                }

                return characteristic;
            }

            private BluetoothGattCharacteristic findNotifyCharacteristic(
                    BluetoothGattService service, UUID characteristicUUID) {

                BluetoothGattCharacteristic characteristic = null;

                List<BluetoothGattCharacteristic> characteristics = service
                        .getCharacteristics();

                for (BluetoothGattCharacteristic c : characteristics) {
                    if ((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
                            && characteristicUUID.equals(c.getUuid())) {
                        characteristic = c;
                        break;
                    }
                }

                if (characteristic != null)
                    return characteristic;

                for (BluetoothGattCharacteristic c : characteristics) {
                    if ((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                            && characteristicUUID.equals(c.getUuid())) {
                        characteristic = c;
                        break;
                    }
                }

                return characteristic;
            }

            private String generateHashKey(BluetoothGattCharacteristic characteristic) {
                return this.generateHashKey(characteristic.getService().getUuid(), characteristic);
            }

            private String generateHashKey(UUID serviceUUID,
                                           BluetoothGattCharacteristic characteristic) {
                return serviceUUID + "|" + characteristic.getUuid()
                        + "|" + characteristic.getInstanceId();
            }

            /********************************************************************************
             * Implements BluetoothGattCallback API
             *******************************************************************************/

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                int newState) {
                TelinkLog.d("蓝牙相关 onConnectionStateChange  status :" + status + " state : "
                        + newState);
                isConnecting = false;
                switch (newState) {
                    case BluetoothGatt.STATE_CONNECTED:
                        //            setMTU(103);
                        this.mConnState.set(CONN_STATE_CONNECTED);
                        //            Log.d("dadouhjj", "onConnectionStateChange: "+a);
                        if (this.gatt == null || !this.gatt.discoverServices()) {
                            TelinkLog.d("remote service discovery has been stopped status = " + newState);
                            this.disconnect();
                        } else {
                            this.onConnect();
                        }
                        break;
                    case BluetoothGatt.STATE_DISCONNECTED:
                        synchronized (this.mStateLock) {
                            refreshDeviceCache(gatt);
                            if (this.gatt != null) {
                                closeGatt();
                                TelinkLog.d("蓝牙相关Peripheral#onConnectionStateChange#onDisconnect " +
                                        "GattClose");
                            }
                            LogUtils.v("zcl--------蓝牙相关---STATE_DISCONNECTED----closeGatt---" + gatt != null);
                            this.clear();
                            this.mConnState.set(CONN_STATE_IDLE);
                            TelinkLog.d("Peripheral#onConnectionStateChange#onDisconnect");
                            this.onDisconnect();
                        }
                        break;
                }
            }

            /**
             * Clears the internal cache and forces a refresh of the services from the * remote
             * device.
             */

            public boolean refreshDeviceCache(BluetoothGatt mBluetoothGatt) {
                if (mBluetoothGatt != null) {
                    try {
                        BluetoothGatt localBluetoothGatt = mBluetoothGatt;
                        Method localMethod = localBluetoothGatt.getClass().getMethod("refresh",
                                new Class[0]);
                        if (localMethod != null) {
                            boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt,
                                    new Object[0])).booleanValue();
                            TelinkLog.d("refreshDeviceCache ret = " + bool);
                            return bool;
                        } else {
                            TelinkLog.d("refreshDeviceCache localMethod == null");
                        }
                    } catch (Exception localException) {
                        localException.printStackTrace();
                        TelinkLog.d("An exception occured while refreshing device");
                    }
                }
                return false;
            }


            public synchronized void closeGatt() {
                if (this.gatt != null) {
                    gatt.disconnect();
                    this.gatt.close();
                    TelinkLog.d("gatt closeGatt " + this.gatt);
                    this.mConnState.set(CONN_STATE_CLOSED);
                    this.gatt = null;
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt,
                                                BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                String key = this.generateHashKey(characteristic);
                CommandContext commandContext = this.mNotificationCallbacks.get(key);

                if (commandContext != null) {
                    this.onNotify(characteristic.getValue(),
                            commandContext.command.serviceUUID,
                            commandContext.command.characteristicUUID,
                            commandContext.command.tag);
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt,
                                             BluetoothGattCharacteristic characteristic,
                                             int status) {
                super.onCharacteristicRead(gatt, characteristic, status);

                this.cancelCommandTimeoutTask();

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    byte[] data = characteristic.getValue();
                    String s = Arrays.bytesToHexString(data, ",");
                    Log.v("获取蓝牙数据 ", "zcl------------------" + s);
                    this.commandSuccess(data);
                } else {
                    this.commandError("read characteristic failed");
                }

                this.commandCompleted();
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt,
                                              BluetoothGattCharacteristic characteristic,
                                              int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);

                if (packetInteration < packetSize && isOutOfSize) {
                    characteristic.setValue(packets[packetInteration]);
                    gatt.writeCharacteristic(characteristic);
                    packetInteration++;

                    this.cancelCommandTimeoutTask();

                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        byte[] data = characteristic.getValue();
                        String s = Arrays.bytesToHexString(data, ",");
                        this.commandSuccess(null);
                    } else {
                        this.commandError("write characteristic fail");
                    }

                    TelinkLog.d("onCharacteristicWrite newStatus : " + status);

                    this.commandCompleted();
                } else {
                    this.cancelCommandTimeoutTask();

                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        this.commandSuccess(characteristic.getValue());
                        String s = Arrays.bytesToHexString(characteristic.getValue(), ",");
                    } else {
                        this.commandError("write characteristic fail");
                    }

                    TelinkLog.d("onCharacteristicWrite newStatus : " + status);

                    this.commandCompleted();
                }
            }

            private int packetSize = 0;
            private int packetInteration = 0;
            private boolean isOutOfSize = false;
            private byte[][] packets;

            public void sendData(byte[] data, BluetoothGattCharacteristic characteristicData,
                                 int writeType) {
                int chunksize = 20; //20 byte chunk
                packetSize = (int) Math.ceil(data.length / (double) chunksize); //make this variable
                // public so we can access it on the other function

                //this is use as header, so peripheral device know ho much packet will be received.
                characteristicData.setValue(data);
                characteristicData.setWriteType(writeType);
                gatt.writeCharacteristic(characteristicData);
                gatt.executeReliableWrite();

                packets = new byte[packetSize][chunksize];
                packetInteration = 0;
                Integer start = 0;
                for (int i = 0; i < packets.length; i++) {
                    int end = start + chunksize;
                    if (end > data.length) {
                        end = data.length;
                    }
                    packets[i] = java.util.Arrays.copyOfRange(data, start, end);
                    start += chunksize;
                }
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt,
                                         BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorRead(gatt, descriptor, status);

                this.cancelCommandTimeoutTask();

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    byte[] data = descriptor.getValue();
                    this.commandSuccess(data);
                } else {
                    this.commandError("read description failed");
                }

                this.commandCompleted();
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt,
                                          BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);

                this.cancelCommandTimeoutTask();

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    this.commandSuccess(null);
                } else {
                    this.commandError("write description failed");
                }

                this.commandCompleted();
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    List<BluetoothGattService> services = gatt.getServices();
                    this.mServices = services;
                    this.onServicesDiscovered(services);
                } else {
                    TelinkLog.d("Service discovery failed");
                    this.disconnect();
                }
            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                super.onReadRemoteRssi(gatt, rssi, status);

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (rssi != this.rssi) {
                        this.rssi = rssi;
                        this.onRssiChanged();
                    }
                }
            }

            @Override
            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                super.onMtuChanged(gatt, mtu, status);

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d("mtu changed-->", "onMtuChanged: " + mtu);
                }
                TelinkLog.d("mtu changed : " + mtu + "    status==" + status);
            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            public boolean setMTU(int mtu) {
                Log.d("BLE", "setMTU " + mtu);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (mtu > 20) {
                        boolean ret = gatt.requestMtu(mtu);
                        Log.d("BLE", "requestMTU " + mtu + " ret=" + ret);
                        return ret;
                    }
                }

                return false;
            }


            @Override
            public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                super.onReliableWriteCompleted(gatt, status);
            }

            public String getVersion(byte[] scanRecord) {
                for (int i = 0; i < scanRecord.length - 5; i++) {
                    int i1 = scanRecord[i] & 0xff;
                    int i2 = scanRecord[i + 1] & 0xff;
                    int i3 = scanRecord[i + 2] & 0xff;
                    int i4 = scanRecord[i + 3] & 0xff;
                    if (i1 == 0x1e && i2 == 0xFF & i3 == 0x11 & i4 == 0x02) {
                        if ((scanRecord[i + 15] & 0xff) == 0xdd) {
                            for (int j = i + 16; j < scanRecord.length - 5; j++) {
                                int i5 = scanRecord[j] & 0xff;
                                if (i5 != 0)
                                    version = version + (char) scanRecord[j];
                            }
                                    /*
                                    valueOf((char) scanRecord[i + 16]) + (char) scanRecord[i +
                                    17] + (char) scanRecord[i + 18]
                                    + (char) scanRecord[i + 19] + (char) scanRecord[i + 20] +
                                    (char) scanRecord[i + 21];*/
                            LogUtils.v("zcl------TelinkBluetoothSDK-----首页相关获取广播版本-------" + version + "--" + Arrays.bytesToHexString(scanRecord, ","));
                        }
                    }
                }
        /*        String version =
                        valueOf((char) scanRecord[39]) + (char) scanRecord[40] + (char)
                        scanRecord[41]
                                + (char) scanRecord[42] + (char) scanRecord[43] + (char)
                                scanRecord[44];*/
                return version;
            }


            private final class CommandContext {

                public Command command;
                public Command.Callback callback;
                public long time;

                public CommandContext(Command.Callback callback, Command command) {
                    this.callback = callback;
                    this.command = command;
                }

                public void clear() {
                    this.command = null;
                    this.callback = null;
                }
            }

            private final class RssiUpdateRunnable implements Runnable {

                @Override
                public void run() {

                    if (!monitorRssi)
                        return;

                    if (!isConnected())
                        return;

                    if (gatt != null)
                        gatt.readRemoteRssi();

                    mRssiUpdateHandler.postDelayed(mRssiUpdateRunnable, updateIntervalMill);
                }
            }

            private final class CommandTimeoutRunnable implements Runnable {

                @Override
                public void run() {

                    synchronized (mOutputCommandQueue) {

                        CommandContext commandContext = mOutputCommandQueue.peek();

                        if (commandContext != null) {

                            Command command = commandContext.command;
                            Command.Callback callback = commandContext.callback;

                            boolean retry = commandTimeout(commandContext);

                            if (retry) {
                                commandContext.command = command;
                                commandContext.callback = callback;
                                processCommand(commandContext);
                            } else {
                                mOutputCommandQueue.poll();
                                commandCompleted();
                            }
                        }
                    }
                }
            }

            private final class CommandDelayRunnable implements Runnable {
                @Override
                public void run() {
                    synchronized (mOutputCommandQueue) {
                        CommandContext commandContext = mOutputCommandQueue.peek();
                        processCommand(commandContext);
                    }
                }
            }
        }

package com.etku.bluetoothdemo;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.coolu.blelibrary.impl.AndroidBle;
import com.coolu.blelibrary.inter.IBLE;
import com.coolu.blelibrary.inter.OnConnectionListener;
import com.coolu.blelibrary.inter.OnDeviceSearchListener;
import com.coolu.blelibrary.inter.OnResultListener;

import java.util.List;

import static com.coolu.blelibrary.config.Config.CLIENT_CHARACTERISTIC_CONFIG;
import static com.coolu.blelibrary.config.Config.bltServerUUID;
import static com.coolu.blelibrary.config.Config.readDataUUID;
import static com.coolu.blelibrary.config.Config.writeDataUUID;
import static com.etku.bluetoothdemo.Constants.ACTION_READ_CHARACTERISTIC;
import static com.etku.bluetoothdemo.Constants.ACTION_WRITE_CHARACTERISTIC;
import static com.etku.bluetoothdemo.Constants.ACTION_WRITE_DESCRIPTOR;
import static com.etku.bluetoothdemo.Constants.ACTION_WRITE_TOKEN;
import static com.etku.bluetoothdemo.Utils.byte2Hex;
import static com.etku.bluetoothdemo.Utils.decrypt;
import static com.etku.bluetoothdemo.Utils.getDefaultKey;
import static com.etku.bluetoothdemo.Utils.getTokenAcquisitionCommand;

public class BleService extends Service {

    private static final String TAG = "BLE_BleService";

    private final IBinder binder = new LocalBinder();
    private ServiceCallbacks serviceCallback;

    IBLE ible;
    BluetoothAdapter bluetoothAdapter;
    BluetoothGattCharacteristic writeCharacteristic;
    BluetoothGattCharacteristic readCharacteristic;
    BluetoothGatt mBluetoothGatt;

    BroadcastReceiver bleReceiver;

    int pending = 0;
    int finished = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        ible = AndroidBle.init(this);
        ible.setDebug(true);

        if (!ible.isSupportBluetooth())
            stopSelf();

        if (!ible.isEnable())
            ible.enableBluetooth();

        bleReceiver = new BleService.BleReceiver();
    }

    public BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            mBluetoothGatt = gatt;

            Log.i(TAG, "status:" + status + ";newState:" + newState);

            switch (newState){
                case BluetoothProfile.STATE_CONNECTED:{
                    gatt.discoverServices();
                    break;
                }
                case BluetoothProfile.STATE_DISCONNECTING:{
                    Log.i(TAG, gatt.getDevice().getName() + "is disconnecting.");
                    break;
                }
                case BluetoothProfile.STATE_DISCONNECTED:{
                    Log.i(TAG, gatt.getDevice().getName() + "is disconnected.");
                    break;
                }
                default:{
                    Log.i(TAG, "Connection state of " + gatt.getDevice().getName() + ": " + gatt.getConnectionState(gatt.getDevice()));
                    break;
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                final BluetoothGattService service = gatt.getService(bltServerUUID);

                if (service != null) {
                    List<BluetoothGattCharacteristic> characteristics;
                    List<BluetoothGattDescriptor> descriptors;

                    List<BluetoothGattService> services = gatt.getServices();
                    for (BluetoothGattService s : services) {
                        characteristics = s.getCharacteristics();
                        Log.i(TAG, s.getUuid() + "Service->getType " + s.getType());
                        for (BluetoothGattCharacteristic c : characteristics) {
                            descriptors = c.getDescriptors();
                            Log.i(TAG, c.getUuid().toString() + " Characteristic->getWriteType " + c.getWriteType());
                            Log.i(TAG, c.getUuid().toString() + " Characteristic->getPermissions " + c.getPermissions());
                            Log.i(TAG, c.getUuid().toString() + " Characteristic->getProperties " + c.getProperties());

                            for (BluetoothGattDescriptor d : descriptors) {
                                Log.i(TAG, d.getUuid().toString() + " Descriptor->describeContents " + d.describeContents());
                                Log.i(TAG, d.getUuid().toString() + " Descriptor->getPermissions " + d.getPermissions());
                            }
                        }
                    }
                    try {

                        pending = finished = 0;

                        for (BluetoothGattService s : services) {
                            characteristics = s.getCharacteristics();

                            for (BluetoothGattCharacteristic c : characteristics) {

                                while (pending != finished)
                                    Thread.sleep(100);

                                pending++;

                                if (c.getProperties() == BluetoothGattCharacteristic.PROPERTY_INDICATE)
                                    writeDescriptor(gatt, c, false);
                                else if (c.getProperties() == BluetoothGattCharacteristic.PROPERTY_NOTIFY)
                                    writeDescriptor(gatt, c, true);
                            }
                        }

                        for (BluetoothGattService s : services) {
                            characteristics = s.getCharacteristics();

                            for (BluetoothGattCharacteristic c : characteristics) {

                                while (pending != finished)
                                    Thread.sleep(100);

                                pending++;

                                if (c.getProperties() == BluetoothGattCharacteristic.PROPERTY_READ)
                                    readCharacteristic(gatt, c);
                            }
                        }

                        for (BluetoothGattService s : services) {
                            characteristics = s.getCharacteristics();

                            for (BluetoothGattCharacteristic c : characteristics) {

                                while (pending != finished)
                                    Thread.sleep(100);

                                pending++;

                                if (c.getProperties() == BluetoothGattCharacteristic.PROPERTY_WRITE)
                                    writeCharacteristic(gatt, c, getTokenAcquisitionCommand());
                                else if (c.getProperties() == BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)
                                    writeCharacteristic(gatt, c, getTokenAcquisitionCommand());
                            }
                        }

                        readCharacteristic = service.getCharacteristic(readDataUUID);
                        writeCharacteristic = service.getCharacteristic(writeDataUUID);
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }

                if (readCharacteristic != null && writeCharacteristic != null){
                    //writeDescriptor(gatt, readCharacteristic);
                    //Intent intent = new Intent();
                    //intent.setAction(ACTION_WRITE_DESCRIPTOR);
                    //sendBroadcast(intent);
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            byte[] values = descriptor.getValue();
            Log.i(TAG, "onDescriptorWrite: " + byte2Hex(values));

            finished++;

            //writeCharacteristic(gatt, writeCharacteristic);
            /*
            Intent intent = new Intent();
            intent.setAction(ACTION_WRITE_TOKEN);
            sendBroadcast(intent);
             */
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            byte[] values = descriptor.getValue();
            Log.i(TAG, "onDescriptorRead: " + byte2Hex(values));

            finished++;
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            byte[] values = characteristic.getValue();
            byte mingwen[] = decrypt(values, getDefaultKey());
            Log.i(TAG, "onCharacteristicRead: " + byte2Hex(mingwen));

            finished++;
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            byte[] values = characteristic.getValue();
            byte mingwen[] = decrypt(values, getDefaultKey());
            Log.i(TAG, "onCharacteristicWrite: " + byte2Hex(mingwen));

            finished++;

            //readCharacteristic(gatt, readCharacteristic);
            /*
            Intent intent = new Intent();
            intent.setAction(ACTION_READ_CHARACTERISTIC);
            sendBroadcast(intent);
             */
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] values = characteristic.getValue();
            byte mingwen[] = decrypt(values, getDefaultKey());
            Log.i(TAG, "onCharacteristicChanged: " + byte2Hex(mingwen));
        }
    };

    public void writeDescriptor(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean notify){
        gatt.setCharacteristicNotification(characteristic, true);

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);

        if (notify)
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        else
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);

        gatt.writeDescriptor(descriptor);
    }

    public void writeCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value){
        characteristic.setWriteType(characteristic.getWriteType());
        characteristic.setValue(value);
        gatt.writeCharacteristic(characteristic);
    }

    public void readCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
        gatt.readCharacteristic(characteristic);
    }

    public void setCallbacks(ServiceCallbacks callback) {
        serviceCallback = callback;
    }

    public interface ServiceCallbacks {
        BluetoothGattCallback getGattCallback();
    }

    class LocalBinder extends Binder {
        BleService getService(){
            return BleService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_READ_CHARACTERISTIC);
        filter.addAction(ACTION_WRITE_CHARACTERISTIC);
        filter.addAction(ACTION_WRITE_DESCRIPTOR);
        filter.addAction(ACTION_WRITE_TOKEN);
        registerReceiver(bleReceiver, filter);

        ible.startResultListener(new OnResultListener() {
            @Override
            public void DeviceResult(int i, boolean b, int i1) {
                Log.d(TAG, "DeviceResult: " + i + "-" + b + "-" + i1);
            }

            @Override
            public void errorResult(int i) {
                Log.e(TAG, "errorResult: " + i);
            }
        });

        bluetoothAdapter = ible.getBluetoothAdapter();

        ible.startScan(new OnDeviceSearchListener() {
            @Override
            public void onScanDevice(final BluetoothDevice bluetoothDevice, int i, byte[] bytes) {

                if (bluetoothDevice.getName() != null && bluetoothDevice.getName().contains("O16")){
                    ible.stopScan();

                    ible.connectDevice(bluetoothDevice, new OnConnectionListener() {

                        @Override
                        public void onDisconnect(int i) {
                            Log.i(TAG, "Device disconnected: " + i);
                        }

                        @Override
                        public void onServicesDiscovered(String s, String s1) {
                            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(bluetoothDevice.getAddress());
                            mBluetoothGatt = device.connectGatt(getApplicationContext(), true, mGattCallback);
                        }
                    });
                }
            }
        });

        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {

        if (bluetoothAdapter != null)
            if (bluetoothAdapter.isEnabled())
                bluetoothAdapter.disable();

        unregisterReceiver(bleReceiver);

        return super.onUnbind(intent);
    }

    public class BleReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction() == null)
                return;

            switch (intent.getAction()){
                case ACTION_WRITE_DESCRIPTOR:{
                    Log.d(TAG, "onReceive: ACTION_WRITE_DESCRIPTOR");
                    //writeDescriptor(mBluetoothGatt, readCharacteristic);
                    break;
                }
                case ACTION_WRITE_CHARACTERISTIC:{
                    Log.d(TAG, "onReceive: ACTION_WRITE_CHARACTERISTIC");
                    writeCharacteristic(mBluetoothGatt, writeCharacteristic, new byte[]{0x01, 0x00});
                    break;
                }
                case ACTION_WRITE_TOKEN:{
                    Log.d(TAG, "onReceive: ACTION_WRITE_TOKEN");
                    for(int i=0; i<10; i++)
                        writeCharacteristic(mBluetoothGatt, writeCharacteristic, getTokenAcquisitionCommand());
                    break;
                }
                case ACTION_READ_CHARACTERISTIC:{
                    Log.d(TAG, "onReceive: ACTION_READ_CHARACTERISTIC");
                    readCharacteristic(mBluetoothGatt, readCharacteristic);
                    break;
                }
                default:{
                    break;
                }
            }
        }
    }
}

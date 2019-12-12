package com.etku.bluetoothdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BLE_MainActivity";

    BleService bleService;
    boolean bound = false;

    //private static final UUID serverUUID = UUID.fromString("0000fee7-0000-1000-8000-00805f9b34fb");
    //private static final UUID readDataUUID = UUID.fromString("000036f6-0000-1000-8000-00805f9b34fb");
    //private static final UUID writeDataUUID = UUID.fromString("000036f5-0000-1000-8000-00805f9b34fb");
    //private static final UUID clientCharacteristicConfigUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    //private static final UUID oadServiceUUID = UUID.fromString("f000ffc0-0451-4000-b000-000000000000");
    //private static final UUID ccServiceUUID = UUID.fromString("f000ccc0-0451-4000-b000-000000000000");

    private static final int PERMISSION_REQUEST_FINE_LOCATION = 8888;
    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 9999;

    Button lockButton;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lockButton = findViewById(R.id.btn);
        textView = findViewById(R.id.tv_ble);



        requestPermissions();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent serviceIntent = new Intent(this, BleService.class);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();


        unbindService(serviceConnection);
        bound = false;
    }

    public void requestPermissions(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
            }
            if (this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BleService.LocalBinder binder = (BleService.LocalBinder) service;
            bleService = binder.getService();
            //bleService.setCallbacks(MainActivity.this);
            bound = true;
            Log.i(TAG, "onServiceConnected: " + name);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
            Log.i(TAG, "onServiceDisconnected: " + name);
        }
    };
}

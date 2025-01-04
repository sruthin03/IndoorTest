package com.example.indoortest;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WebSocketManager webSocketManager;
    private BluetoothLeScanner bluetoothLeScanner;
    private ScanCallback scanCallback;
    private TextView tvBleData;
    private Button btnScan;

    private static final int PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        tvBleData = findViewById(R.id.tvBleData);
        btnScan = findViewById(R.id.btnScan);
        Button btnClear = findViewById(R.id.btnClear);
        btnClear.setOnClickListener(v -> clearTextView());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkAndRequestPermissions();
        }


        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        setupScanCallback();


        webSocketManager = new WebSocketManager(tvBleData);
        webSocketManager.connectWebSocket("ws://192.168.1.4:8000/ws/nav/"); // Use your Django server's IP address


        btnScan.setOnClickListener(v -> startBLEScan());


    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, PERMISSION_REQUEST_CODE);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Log.e("Permission", "Required permission not granted");
                    return;
                }
            }
            Log.d("Permission", "All permissions granted");
        }
    }


    private final List<Integer> rssiList = new ArrayList<>();

    private void setupScanCallback() {
        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                BluetoothDevice device = result.getDevice();
                int rssi = result.getRssi();


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.e("Permission", "BLUETOOTH_CONNECT permission not granted");
                    return;
                }

                String deviceName = device.getName();
                String deviceAddress = device.getAddress();
                if ("INS".equals(deviceName)) {

                    rssiList.add(rssi);


                    if (rssiList.size() >= 10) {
                        try {

                            JSONArray rssiJsonArray = new JSONArray(rssiList);
                            JSONObject jsonObject = new JSONObject();
                            //jsonObject.put("deviceName", deviceName != null ? deviceName : "Unknown");
                            //jsonObject.put("deviceAddress", deviceAddress);
                            jsonObject.put("rssi", rssiJsonArray);
                            String logMessage = jsonObject.toString();
                            Log.d("BLE Scan", logMessage);
                            updateTextView(logMessage);
                            webSocketManager.sendMessage(logMessage);


                            rssiList.clear();
                        } catch (Exception e) {
                            Log.e("BLE Scan", "Error creating JSON", e);
                        }
                    }
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                String errorMessage = "Scan failed: " + errorCode;
                Log.e("BLE Scan", errorMessage);
                updateTextView(errorMessage);


                webSocketManager.sendMessage(errorMessage);
            }
        };
    }

    private void startBLEScan() {
        if (bluetoothLeScanner != null) {
            bluetoothLeScanner.startScan(scanCallback);
            Log.d("BLE Scan", "Scan started");


            new Handler().postDelayed(() -> {
                bluetoothLeScanner.stopScan(scanCallback);
                Log.d("BLE Scan", "Scan stopped after 5 seconds");
            }, 5000);
        } else {
            Log.e("BLE Scan", "BluetoothLeScanner is null");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothLeScanner != null && scanCallback != null) {
            bluetoothLeScanner.stopScan(scanCallback);
        }
    }

    private void clearTextView() {
        tvBleData.setText("");
    }


    public void updateTextView(String logMessage) {
        runOnUiThread(() -> {
            String currentText = tvBleData.getText().toString();
            String updatedText = logMessage + "\n" + currentText;
            tvBleData.setText(updatedText);
        });
    }
}

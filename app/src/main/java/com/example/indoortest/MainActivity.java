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
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private BluetoothLeScanner bluetoothLeScanner;
    private ScanCallback scanCallback;
    private TextView tvBleData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvBleData = findViewById(R.id.tvBleData); // Initialize the TextView

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.BLUETOOTH_SCAN
                }, 1);
            }
        }

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                BluetoothDevice device = result.getDevice();
                int rssi = result.getRssi(); // Get the RSSI value
                String deviceName = device.getName();
                String deviceAddress = device.getAddress();


                   String logMessage = "Device found: " + (deviceName != null ? deviceName : "Unknown") +
                            " [" + deviceAddress + "] RSSI: " + rssi;

                    Log.d("BLE Scan", logMessage);

                    updateTextView(logMessage); // Update TextView with the scan data

            }




            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                String errorMessage = "Scan failed: " + errorCode;
                Log.e("BLE Scan", errorMessage);
                updateTextView(errorMessage);
            }
        };

        // Start scanning
        bluetoothLeScanner.startScan(scanCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothLeScanner != null && scanCallback != null) {
            bluetoothLeScanner.stopScan(scanCallback);
        }
    }

    /**
     * Updates the TextView with the latest BLE scan result.
     *
     * @param logMessage the message to display.
     */
    private void updateTextView(String logMessage) {
        runOnUiThread(() -> {
            String currentText = tvBleData.getText().toString();
            String updatedText = logMessage + "\n" + currentText;
            tvBleData.setText(updatedText);
        });
    }
}

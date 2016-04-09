package com.beatboxers.bluetooth;

import android.bluetooth.BluetoothDevice;

public interface ScanListener {
    void onStart();
    void onStop();
    void onDiscover(BluetoothDevice device);
}
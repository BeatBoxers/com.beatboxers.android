package com.beatboxers.bluetooth;

import android.bluetooth.BluetoothGatt;

public interface ConnectionCallback {
    void connected(BluetoothGatt gatt);
    void connectionError(BluetoothGatt gatt);
    void disconnected(BluetoothGatt gatt);
}
package com.beatboxers.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.beatboxers.bluetooth.device.BBDevice;
import com.beatboxers.bluetooth.device.Microduino;
import com.beatboxers.bluetooth.device.Rfduino;

import java.util.ArrayList;
import java.util.Iterator;

public class ConnectionService extends Service {
    private final static String LOG_TAG = "bb_" + ConnectionService.class.getSimpleName();

    private final IBinder mBinder = new LocalBinder();
    protected final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());
    private BluetoothAdapter mBluetoothAdapter;
    private ConnectionCallback mConnectionCallback;

    private ArrayList<BluetoothGatt> mConnectedDevices = new ArrayList<>();

    private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(LOG_TAG, "Connected to GATT server. " + gatt.getDevice().getName());

                if (!mConnectedDevices.contains(gatt)) {
                    mConnectedDevices.add(gatt);
                }

                //start discovering services right away
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(LOG_TAG, "Disconnected from GATT server. " + gatt.getDevice().getName());

                if (mConnectedDevices.contains(gatt)) {
                    mConnectedDevices.remove(gatt);
                }

                mMainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (null != mConnectionCallback) {
                            mConnectionCallback.disconnected(gatt);
                        }
                    }
                });
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            String deviceName = gatt.getDevice().getName().trim();

            if (status == BluetoothGatt.GATT_SUCCESS) {
                try {
                    BluetoothGattService gattService = gatt.getService(BBDevice.getServiceUUID(deviceName));
                    BluetoothGattCharacteristic receiveCharacteristic = gattService.getCharacteristic(BBDevice.getReceiveUUID(deviceName));
                    BluetoothGattDescriptor receiveConfigDescriptor = receiveCharacteristic.getDescriptor(BBDevice.getClientConfigUUID(deviceName));

                    gatt.setCharacteristicNotification(receiveCharacteristic, true);
                    receiveConfigDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(receiveConfigDescriptor);

                    Log.i(LOG_TAG, "Found the TX/RX GATT characteristic. " + deviceName);

                    mMainThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (null != mConnectionCallback) {
                                mConnectionCallback.connected(gatt);
                            }
                        }
                    });
                } catch (NullPointerException e) {
                    Log.e(LOG_TAG, "Could not find the TX/RX GATT characteristic. " + deviceName);

                    mMainThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (null != mConnectionCallback) {
                                mConnectionCallback.connectionError(gatt);
                            }
                        }
                    });
                }
            } else {
                Log.w(LOG_TAG, "onServicesDiscovered received: " + status + " " + deviceName);

                mMainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (null != mConnectionCallback) {
                            mConnectionCallback.connectionError(gatt);
                        }
                    }
                });
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                dataReceived(gatt, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            dataReceived(gatt, characteristic);
        }

        private void dataReceived(final BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue();

            if (data == null || data.length < 1) {
                Log.e(LOG_TAG, "Received an empty message from BT");
                return;
            }

            final String received = String.format("%s", new String(data)).trim();

            mMainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    BluetoothDevice device = gatt.getDevice();
                    String deviceName = device.getName().trim();
                    String address = device.getAddress();

                    if (deviceName.equals(Microduino.DEVICE_NAME)) {
                        Microduino.hitReceived(ConnectionService.this, address, received);
                    } else if (deviceName.equals(Rfduino.DEVICE_NAME)) {
                        Rfduino.hitReceived(ConnectionService.this, address, received);
                    }
                }
            });
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        BluetoothManager bluetoothManager = (BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        for (BluetoothGatt device : mConnectedDevices) {
            device.close();
        }

        return super.onUnbind(intent);
    }

    public void setCallback(ConnectionCallback callback) {
        mConnectionCallback = callback;
    }

    public void connect(String address) {
        Log.i(LOG_TAG, "Attempting to connect to device: " + address);

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        device.connectGatt(getApplicationContext(), false, mBluetoothGattCallback);
    }

    public synchronized void disconnect() {
        Iterator<BluetoothGatt> iterator = mConnectedDevices.iterator();

        while (iterator.hasNext()) {
            BluetoothGatt device = iterator.next();

            device.disconnect();
            iterator.remove();
        }

        mConnectedDevices.clear();
    }

    public synchronized void disconnect(String address) {
        Iterator<BluetoothGatt> iterator = mConnectedDevices.iterator();

        while (iterator.hasNext()) {
            BluetoothGatt device = iterator.next();

            if (device.getDevice().getAddress().equals(address)) {
                device.disconnect();
                iterator.remove();
                break;
            }
        }
    }

    public class LocalBinder extends Binder {
        public ConnectionService getService() {
            return ConnectionService.this;
        }
    }
}
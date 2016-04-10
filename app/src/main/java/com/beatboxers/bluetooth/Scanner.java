package com.beatboxers.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.util.Log;

public class Scanner {
    static private final String LOG_TAG = "bb_"+Scanner.class.getSimpleName();

    static private final int SCAN_TIME = 10000;//10 seconds

    private Handler mHandler = new Handler();

    private boolean mIsScanning = false;
    private BluetoothAdapter mBluetoothAdapter;

    private ScanListener mScanListener;
    private Handler mMainThreadHandler;

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (null != mScanListener) {
                if (null != mMainThreadHandler) {
                    mMainThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mScanListener.onDiscover(device);
                        }
                    });
                }
                else {
                    mScanListener.onDiscover(device);
                }
            }
        }
    };

    public Scanner(BluetoothAdapter bluetoothAdapter) {
        mBluetoothAdapter = bluetoothAdapter;
    }

    public void setBluetoothScanListener(Handler mainThreadHandler, ScanListener scanListener) {
        mMainThreadHandler = mainThreadHandler;
        mScanListener = scanListener;
    }

    public void start() {
        if (mIsScanning) {
            stop();
        }

        //auto-stop scanning after our specified delay
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stop();
            }
        }, SCAN_TIME);

        mBluetoothAdapter.startLeScan(mLeScanCallback);
        mIsScanning = true;
        Log.i(LOG_TAG, "scan started");

        if (null != mScanListener) {
            if (null != mMainThreadHandler) {
                mMainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mScanListener.onStart();
                    }
                });
            }
            else {
                mScanListener.onStart();
            }
        }
    }

    public void stop() {
        if (!mIsScanning) {
            return;
        }

        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        mIsScanning = false;
        Log.i(LOG_TAG, "scan ended");

        if (null != mScanListener) {
            if (null != mMainThreadHandler) {
                mMainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mScanListener.onStop();
                    }
                });
            }
            else {
                mScanListener.onStop();
            }
        }
    }
}

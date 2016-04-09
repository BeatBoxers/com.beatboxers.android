package com.beatboxers.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.beatboxers.Broadcasts;
import com.beatboxers.R;
import com.beatboxers.adapter.BluetoothDevicesListAdapter;
import com.beatboxers.bluetooth.ScanListener;
import com.beatboxers.bluetooth.Scanner;

import java.util.ArrayList;

public class ScanDialog extends DialogFragment {
    static private final String LOG_TAG = "bb_" + ScanDialog.class.getSimpleName();

    static public final String TAG = "scanDialog";

    private Scanner mScanner;
    private ArrayList<BluetoothDevice> mDiscoveredDevices = new ArrayList<>();
    private BluetoothDevicesListAdapter mListViewAdapter;

    private ProgressBar mScanningProgressBar;
    private Button mButtonScan;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(LOG_TAG, "onCreate");

        mListViewAdapter = new BluetoothDevicesListAdapter(getActivity(), R.layout.cell_bluetooth_device, mDiscoveredDevices);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //device scanner setup
        mScanner = new Scanner(bluetoothAdapter);
        mScanner.setBluetoothScanListener(new Handler(getActivity().getMainLooper()), new ScanListener() {
            @Override
            public void onStart() {
                //empty our listview on start
                mDiscoveredDevices.clear();
                mListViewAdapter.notifyDataSetChanged();

                //disable our scan button...
                mButtonScan.setEnabled(false);
                mScanningProgressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onStop() {
                //enable our scan button...
                mButtonScan.setEnabled(true);
                mScanningProgressBar.setVisibility(View.INVISIBLE);
            }

            public void onDiscover(final BluetoothDevice device) {
                if (!mDiscoveredDevices.contains(device)) {
                    mDiscoveredDevices.add(device);
                    mListViewAdapter.notifyDataSetChanged();

                    Log.i(LOG_TAG, "discovered device: " + device.getName().trim());
                }
            }
        });
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        Log.d(LOG_TAG, "onCreateDialog");

        //setup our views first
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_scan, null);
        View titleView = getActivity().getLayoutInflater().inflate(R.layout.dialog_scan_title, null);
        mScanningProgressBar = (ProgressBar)titleView.findViewById(R.id.progressBarScanning);

        ListView listView = (ListView)view.findViewById(R.id.listDevices);
        listView.setEmptyView(view.findViewById(R.id.listDeviceNoResults));
        listView.setAdapter(mListViewAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
                Intent intent = new Intent();
                intent.setAction(Broadcasts.ACTION_CONNECT_DEVICE);
                intent.putExtra(Broadcasts.EXTRA_DEVICE_ADDRESS, mDiscoveredDevices.get(position).getAddress());
                intent.putExtra(Broadcasts.EXTRA_DEVICE_NAME, mDiscoveredDevices.get(position).getName().trim());
                getActivity().sendBroadcast(intent);

                dismiss();
            }
        });

        Button buttonCancel = (Button)view.findViewById(R.id.buttonCancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        mButtonScan = (Button)view.findViewById(R.id.buttonScan);
        mButtonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mScanner.start();
            }
        });

        //set our views to the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCustomTitle(titleView)
                .setView(view);

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();

        //empty our variables
        mDiscoveredDevices.clear();
        mListViewAdapter.notifyDataSetChanged();

        //start scanning
        mScanner.start();
    }

    @Override
    public void dismiss() {
        //stop scanning if we dismiss the dialog
        //we have to check null first since it seams the fragment manager _sometimes_ kills our variables before reaching here
        if (null != mScanner) {
            mScanner.stop();
        }

        super.dismiss();
    }
}
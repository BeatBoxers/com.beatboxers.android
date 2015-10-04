package com.beatboxers.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.beatboxers.R;

import java.util.List;

public class BluetoothDevicesListAdapter extends ArrayAdapter<BluetoothDevice> {
    static protected final String LOG_TAG = "bb_"+BluetoothDevicesListAdapter.class.getSimpleName();

    protected List<BluetoothDevice> mDevices;

    public BluetoothDevicesListAdapter(Context context, int view, List<BluetoothDevice> devices) {
        super(context, view, devices);

        Log.i(LOG_TAG, "init");
        mDevices = devices;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        BluetoothDevice device = mDevices.get(position);

        if (null == view) {
            LayoutInflater inflator = LayoutInflater.from(getContext());

            view = inflator.inflate(R.layout.cell_bluetooth_device, parent, false);
        }

        if (null != device) {
            TextView nameView = (TextView)view.findViewById(R.id.name);
            TextView addressView = (TextView)view.findViewById(R.id.address);

            if (null != nameView) {
                nameView.setText(device.getName().trim());
            }

            if (null != addressView) {
                addressView.setText(device.getAddress().trim());
            }
        }

        return view;
    }
}
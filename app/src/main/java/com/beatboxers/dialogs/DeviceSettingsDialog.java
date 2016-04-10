package com.beatboxers.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.beatboxers.Broadcasts;
import com.beatboxers.R;
import com.beatboxers.bluetooth.device.SavedDevices;

public class DeviceSettingsDialog extends DialogFragment {
    static private final String LOG_TAG = "bb_"+DeviceSettingsDialog.class.getSimpleName();

    static public final String TAG = "deviceSettingsDialog";

    static public final String EXTRAS_DEVICE_ADDRESS = "deviceAddress";
    static public final String EXTRAS_DEVICE_NAME = "deviceName";

    private boolean mIsSaved = false;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String address = getArguments().getString(EXTRAS_DEVICE_ADDRESS);
        final String name = getArguments().getString(EXTRAS_DEVICE_NAME);

        final SavedDevices savedDevices = new SavedDevices(getActivity());
        mIsSaved = savedDevices.isSaved(address);

        Log.d(LOG_TAG, "onCreateDialog");

        //setup our views first
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_device_settings, null);
        View titleView = getActivity().getLayoutInflater().inflate(R.layout.dialog_device_settings_title, null);

        Button closeButton = (Button)view.findViewById(R.id.buttonClose);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        Button removeButton = (Button)view.findViewById(R.id.buttonRemove);
        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Broadcasts.ACTION_REMOVE_DEVICE);
                intent.putExtra(Broadcasts.EXTRA_DEVICE_ADDRESS, address);
                intent.putExtra(Broadcasts.EXTRA_DEVICE_NAME, name);
                getActivity().sendBroadcast(intent);

                dismiss();
            }
        });

        Button saveStateButton = (Button)view.findViewById(R.id.buttonSaveState);
        saveStateButton.setText((mIsSaved ? R.string.forget : R.string.save));
        saveStateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsSaved) {
                    savedDevices.remove(address);
                } else {
                    savedDevices.add(address, name);
                }

                mIsSaved = !mIsSaved;

                ((Button) view).setText((mIsSaved ? R.string.forget : R.string.save));
            }
        });

        //set our views to the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCustomTitle(titleView)
                .setView(view);

        return builder.create();
    }
}
package com.beatboxers.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.beatboxers.Broadcasts;
import com.beatboxers.R;
import com.beatboxers.adapter.InstrumentsListAdapter;
import com.beatboxers.instruments.DeviceConfig;
import com.beatboxers.instruments.Instrument;
import com.beatboxers.instruments.Instruments;
import com.beatboxers.instruments.UnsetVariableException;

import java.util.ArrayList;
import java.util.List;

public class ChooseInstrumentDialog extends DialogFragment {
    static private final String LOG_TAG = "bb_"+ChooseInstrumentDialog.class.getSimpleName();

    static public final String TAG = "chooseInstrumentDialog";

    static public final String EXTRAS_ADDRESS = "address";
    static public final String EXTRAS_PAD_NUMBER = "padNumber";
    static public final String EXTRAS_SELECTED_INSTRUMENT_ID = "instrumentid";

    private ListView mListView;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        final List<Instrument> instruments = Instruments.sharedInstance().getInstruments();
        final String deviceAddress = getArguments().getString(EXTRAS_ADDRESS);
        final int padNumber = getArguments().getInt(EXTRAS_PAD_NUMBER, 0);
        int selectedInstrumentid = getArguments().getInt(EXTRAS_SELECTED_INSTRUMENT_ID, Instruments.sharedInstance().getDisabledId());

        //setup our views first
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_choose_instrument, null);
        View titleView = getActivity().getLayoutInflater().inflate(R.layout.dialog_choose_instrument_title, null);

        InstrumentsListAdapter adapter = new InstrumentsListAdapter(getActivity(), R.layout.cell_instrument, instruments);
        mListView = (ListView)view.findViewById(R.id.listInstruments);
        mListView.setAdapter(adapter);

        for (int i = 0; i < instruments.size(); i++) {
            if (instruments.get(i).instrumentid == selectedInstrumentid) {
                mListView.setItemChecked(i, true);
                //mListView.setSelection(i);//moves the list view to the position
                break;
            }
        }

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i(LOG_TAG, "clicked instrument position: " + position);

                try {
                    DeviceConfig.sharedInstance().savePadInstrument(deviceAddress, padNumber, instruments.get(position).instrumentid);

                    Intent intent = new Intent();
                    intent.setAction(Broadcasts.ACTION_PAD_CONFIG_UPDATED);
                    intent.putExtra(Broadcasts.EXTRA_DEVICE_ADDRESS, deviceAddress);
                    intent.putExtra(Broadcasts.EXTRA_PAD_NUMBER, padNumber);

                    getActivity().sendBroadcast(intent);
                    dismiss();
                } catch (UnsetVariableException e) {
                    e.printStackTrace();
                }
            }
        });

        //set the cancel button since we can't always rely on the user knowing to press back
        Button cancelButton = (Button)view.findViewById(R.id.buttonCancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        //set our views to the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCustomTitle(titleView)
                .setView(view);

        return builder.create();
    }
}
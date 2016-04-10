package com.beatboxers.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.beatboxers.R;
import com.beatboxers.instruments.Instrument;

import java.util.List;

public class InstrumentsListAdapter extends ArrayAdapter<Instrument> {
    static protected final String LOG_TAG = "bb_" + InstrumentsListAdapter.class.getSimpleName();

    protected List<Instrument> mInstruments;

    public InstrumentsListAdapter(Context context, int view, List<Instrument> instruments) {
        super(context, view, instruments);

        Log.i(LOG_TAG, "init");
        mInstruments = instruments;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        Instrument instrument = mInstruments.get(position);

        if (null == view) {
            LayoutInflater inflator = LayoutInflater.from(getContext());

            view = inflator.inflate(R.layout.cell_instrument, parent, false);
        }

        if (null != instrument) {
            TextView nameView = (TextView) view.findViewById(R.id.instrument);

            if (null != nameView) {
                nameView.setText(instrument.name);
                nameView.setCompoundDrawablesWithIntrinsicBounds(0, 0, instrument.imageResource, 0);
            }
        }

        return view;
    }
}
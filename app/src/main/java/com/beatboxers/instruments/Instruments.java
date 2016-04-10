package com.beatboxers.instruments;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;

import com.beatboxers.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Instruments {

    private Map<Integer, Instrument> mInstrumentsMap = new HashMap<>();
    private List<Instrument> mInstruments = new ArrayList<>();

    static private Instruments mSharedInstance = null;

    private Instruments(Context context) {
        Resources resources = context.getResources();
        populateInstruments(resources);
    }

    private void populateInstruments(Resources resources) {
        TypedArray instrumentsArray = resources.obtainTypedArray(R.array.instrumentsItems);
        for (int i = 0; i < instrumentsArray.length(); i++) {
            int resourceId = instrumentsArray.getResourceId(i, 0);
            if (resourceId > 0) {
                TypedArray instrumentProperties = resources.obtainTypedArray(resourceId);
                int instrumentId = instrumentProperties.getInt(2, 0);
                Instrument instrument = new Instrument(instrumentId,
                        instrumentProperties.getResourceId(3, -1),
                        instrumentProperties.getResourceId(0, 0),
                        instrumentProperties.getResourceId(1, -1)
                );
                mInstruments.add(instrument);
                mInstrumentsMap.put(instrumentId, instrument);
                instrumentProperties.recycle();
            }
        }
        instrumentsArray.recycle();
    }

    static public Instruments init(Context context) {
        if (mSharedInstance == null) {
            mSharedInstance = new Instruments(context);
        }
        return mSharedInstance;
    }

    static public Instruments sharedInstance() {
        return mSharedInstance;
    }

    public List<Instrument> getInstruments() {
        return mInstruments;
    }

    public Instrument getInstrument(int instrumentid) {
        Instrument foundInstrument = mInstrumentsMap.get(instrumentid);
        if (foundInstrument == null) {
            return getDisabled();
        }
        return foundInstrument;
    }

    public Instrument getDisabled() {
        return mInstrumentsMap.get(-1);
    }

    public int getDisabledId() {
        return getDisabled().instrumentid;
    }
}
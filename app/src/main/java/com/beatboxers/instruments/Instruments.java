package com.beatboxers.instruments;

import com.beatboxers.R;

import java.util.ArrayList;

public class Instruments {
    static public final int DISABLED = -1;
    static public final int LOOPBACK = 0;

    static public final int BASS = 1;
    static public final int CRASH = 2;
    static public final int FLOOR_TOM = 3;
    static public final int HIGH_HAT = 4;
    static public final int RIDE = 5;
    static public final int SNARE = 6;
    static public final int TOM_1 = 7;
    static public final int TOM_2 = 8;
    static public final int FART = 9;

    private ArrayList<Instrument> mInstruments = new ArrayList<>();

    static private final Instruments mSharedInstance = new Instruments();

    private Instruments() {
        mInstruments.add(new Instrument(DISABLED, R.string.instrument_disabled, R.mipmap.disabled));
        mInstruments.add(new Instrument(LOOPBACK, R.string.instrument_loopback, R.mipmap.loop));
        mInstruments.add(new Instrument(BASS, R.string.instrument_bass, R.mipmap.bass));
        mInstruments.add(new Instrument(CRASH, R.string.instrument_crash, R.mipmap.crash));
        mInstruments.add(new Instrument(FLOOR_TOM, R.string.instrument_floor_tom, R.mipmap.floor_tom));
        mInstruments.add(new Instrument(HIGH_HAT, R.string.instrument_high_hat, R.mipmap.high_hat));
        mInstruments.add(new Instrument(RIDE, R.string.instrument_ride, R.mipmap.ride));
        mInstruments.add(new Instrument(SNARE, R.string.instrument_snare, R.mipmap.snare));
        mInstruments.add(new Instrument(TOM_1, R.string.instrument_tom_1, R.mipmap.tom_1));
        mInstruments.add(new Instrument(TOM_2, R.string.instrument_tom_2, R.mipmap.tom_2));
        mInstruments.add(new Instrument(FART, R.string.fart, R.mipmap.tom_2));
    }

    static public Instruments sharedInstance() {
        return mSharedInstance;
    }

    public ArrayList<Instrument> getInstruments() {
        return mInstruments;
    }

    public Instrument getInstrument(int instrumentid) {
        for (Instrument instrument : mInstruments) {
            if (instrument.instrumentid == instrumentid) {
                return instrument;
            }
        }

        //did not find it! crazy, just return the disabled instrument then
        for (Instrument instrument : mInstruments) {
            if (DISABLED == instrument.instrumentid) {
                return instrument;
            }
        }

        return null;
    }
}
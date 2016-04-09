package com.beatboxers.instruments;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

public class DeviceConfig {
    static private DeviceConfig mSharedInstance;

    private Context mContext;
    //address: <padNumber: instrumentid>
    private HashMap<String, HashMap<Integer, Integer>> mDeviceConfigs = new HashMap<>();

    static public DeviceConfig sharedInstance() throws UnsetVariableException {
        if (null == mSharedInstance) {
            throw new UnsetVariableException();
        }

        return mSharedInstance;
    }

    public DeviceConfig(Context context) {
        mContext = context;

        mSharedInstance = this;
    }

    public void loadDevice(String address) {
        SharedPreferences settings = mContext.getSharedPreferences(address, 0);
        Map<String, ?> keys = settings.getAll();

        HashMap<Integer, Integer> deviceConfig = new HashMap<>();

        for (Map.Entry<String, ?> entry : keys.entrySet()) {
            deviceConfig.put(Integer.valueOf(entry.getKey()), Integer.valueOf(entry.getValue().toString()));
        }

        mDeviceConfigs.put(address, deviceConfig);
    }

    public void savePadInstrument(String address, int padNumber, int instrumentid) {
        SharedPreferences settings = mContext.getSharedPreferences(address, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(String.valueOf(padNumber), instrumentid);
        editor.commit();

        loadDevice(address);
    }

    public int getInstrumentid(String address, int padNumber) {
        try {
            return mDeviceConfigs.get(address).get(padNumber);
        }
        catch (NullPointerException e) {
            //return a default instrument for this pad number
            switch (padNumber) {
                case 1:
                    return Instruments.BASS;
                case 2:
                    return Instruments.FLOOR_TOM;
                case 3:
                    return Instruments.SNARE;
                case 4:
                    return Instruments.TOM_1;
                case 5:
                    return Instruments.CRASH;
                case 6:
                    return Instruments.HIGH_HAT;
                case 7:
                    return Instruments.RIDE;
                case 8:
                    return Instruments.TOM_2;
                default:
                    return Instruments.DISABLED;
            }
        }
    }
}
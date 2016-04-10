package com.beatboxers.bluetooth.device;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

public class SavedDevices {
    static private final String PREFS_NAME = "savedDevices";

    private Context mContext;

    public SavedDevices(Context context) {
        mContext = context;
    }

    public void add(String address, String name) {
        remove(address);

        SharedPreferences savedDevices = mContext.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = savedDevices.edit();
        editor.putString(address, name);
        editor.apply();
    }

    public void remove(String address) {
        SharedPreferences savedDevices = mContext.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = savedDevices.edit();
        editor.remove(address);
        editor.apply();
    }

    public boolean isSaved(String address) {
        SharedPreferences savedDevices = mContext.getSharedPreferences(PREFS_NAME, 0);
        String device = savedDevices.getString(address, null);

        return (null != device);
    }

    public HashMap<String, String> getAll() {
        SharedPreferences savedDevices = mContext.getSharedPreferences(PREFS_NAME, 0);

        Map<String, ?> keys = savedDevices.getAll();

        HashMap<String, String> devices = new HashMap<>();

        for (Map.Entry<String, ?> entry : keys.entrySet()) {
            devices.put(entry.getKey(), entry.getValue().toString());
        }

        return devices;
    }
}
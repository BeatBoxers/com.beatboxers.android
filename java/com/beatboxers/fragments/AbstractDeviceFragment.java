package com.beatboxers.fragments;

import android.app.Fragment;

abstract public class AbstractDeviceFragment extends Fragment {
    static public final int STATE_CONNECTING = 0;
    static public final int STATE_CONNECTED = 1;
    static public final int STATE_DISCONNECTED = 2;

    static public final String EXTRAS_DEVICE_ADDRESS = "deviceAddress";

    protected int mCurrentState = STATE_CONNECTING;
    protected String mDeviceAddress;

    abstract public void setState(int state);
    abstract protected void setViewFromState();
}
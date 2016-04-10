package com.beatboxers;

public class Broadcasts {
    static public final String ACTION_CONNECT_DEVICE = "broadcastConnectDevice";
    static public final String ACTION_RECONNECT_ALL_DEVICES = "broadcastReconnectAllDevices";
    static public final String ACTION_DISCONNECT_DEVICE = "broadcastDisconnectDevice";
    static public final String ACTION_REMOVE_DEVICE = "broadcastRemoveDevice";
    static public final String ACTION_HIT_RECEIVED = "broadcastHitReceived";
    static public final String ACTION_PAD_CONFIG_UPDATED = "broadcastPadConfigUpdated";
    static public final String ACTION_LOOPBACK_RECORDING = "broadcastLoopbackRecording";
    static public final String ACTION_LOOPBACK_PLAY_STARTED = "loopbackPlaying";
    static public final String ACTION_LOOPBACK_STOPPED = "loopbackStopped";

    static public final String EXTRA_DEVICE_ADDRESS = "deviceAddress";
    static public final String EXTRA_DEVICE_NAME = "deviceName";
    static public final String EXTRA_PAD_NUMBER = "padNumber";
}
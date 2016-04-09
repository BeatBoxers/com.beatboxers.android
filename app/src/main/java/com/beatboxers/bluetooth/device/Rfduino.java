package com.beatboxers.bluetooth.device;

import android.content.Context;
import android.content.Intent;

import com.beatboxers.Broadcasts;
import com.beatboxers.instruments.AudioPlayer;
import com.beatboxers.instruments.DeviceConfig;
import com.beatboxers.instruments.UnsetVariableException;

import java.util.UUID;

public class Rfduino extends BBDevice {
    //private final static String LOG_TAG = "bb_"+Rfduino.class.getSimpleName();

    static public final String DEVICE_NAME = "BeatS";

    static private final String HIT_STRING = "h";

    static public final UUID UUID_SERVICE = UUID.fromString("00002220-0000-1000-8000-00805F9B34FB");
    static public final UUID UUID_RECEIVE = UUID.fromString("00002221-0000-1000-8000-00805F9B34FB");
    static public final UUID UUID_CLIENT_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");

    static public void hitReceived(Context context, String address, String received) {
        if (received.equals(HIT_STRING)) {
            try {
                AudioPlayer.sharedInstance().play(DeviceConfig.sharedInstance().getInstrumentid(address, 1));
            }
            catch (UnsetVariableException e) {
                e.printStackTrace();
            }

            Intent intent = new Intent();
            intent.setAction(Broadcasts.ACTION_HIT_RECEIVED);
            intent.putExtra(Broadcasts.EXTRA_DEVICE_ADDRESS, address);

            context.sendBroadcast(intent);
        }
    }
}
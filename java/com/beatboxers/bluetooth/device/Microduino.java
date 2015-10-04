package com.beatboxers.bluetooth.device;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.beatboxers.Broadcasts;
import com.beatboxers.instruments.AudioPlayer;
import com.beatboxers.instruments.DeviceConfig;
import com.beatboxers.instruments.UnsetVariableException;

import java.util.ArrayList;
import java.util.UUID;

public class Microduino extends BBDevice {
    private final static String LOG_TAG = "bb_"+Microduino.class.getSimpleName();

    static public final String DEVICE_NAME = "BeatBoxers";

    static public final UUID UUID_SERVICE = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    static public final UUID UUID_RECEIVE = UUID.fromString("0000fff6-0000-1000-8000-00805f9b34fb");
    static public final UUID UUID_CLIENT_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    static public void hitReceived(Context context, String address, String received) {
        ArrayList<Intent> intents = new ArrayList<>();

        for (int i = 0; i < received.length(); i++) {
            char receivedChar = received.charAt(i);

            if (receivedChar != '0') {
                int padNumber = receivedChar - '0';//prevent ASCII conversion. -48 would also work but this is less magical.

                try {
                    AudioPlayer.sharedInstance().play(DeviceConfig.sharedInstance().getInstrumentid(address, padNumber));
                }
                catch (UnsetVariableException e) {
                    e.printStackTrace();
                }

                Intent intent = new Intent();
                intent.setAction(Broadcasts.ACTION_HIT_RECEIVED);
                intent.putExtra(Broadcasts.EXTRA_DEVICE_ADDRESS, address);
                intent.putExtra(Broadcasts.EXTRA_PAD_NUMBER, padNumber);

                intents.add(intent);
            }
        }

        for (Intent intent : intents) {
            context.sendBroadcast(intent);
        }
    }
}
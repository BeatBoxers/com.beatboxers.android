package com.beatboxers.bluetooth.device;

import java.util.UUID;

public class BBDevice {
    static public UUID getServiceUUID(String name) {
        if (name.equals(Rfduino.DEVICE_NAME)) {
            return Rfduino.UUID_SERVICE;
        }

        return Microduino.UUID_SERVICE;
    }

    static public UUID getReceiveUUID(String name) {
        if (name.equals(Rfduino.DEVICE_NAME)) {
            return Rfduino.UUID_RECEIVE;
        }

        return Microduino.UUID_RECEIVE;
    }

    static public UUID getClientConfigUUID(String name) {
        if (name.equals(Rfduino.DEVICE_NAME)) {
            return Rfduino.UUID_CLIENT_CONFIG;
        }

        return Microduino.UUID_CLIENT_CONFIG;
    }
}
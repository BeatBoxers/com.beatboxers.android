package com.beatboxers.instruments;

public class Instrument {
    public int instrumentid;
    public int sampleId;
    public int name;
    public int imageResource;

    public Instrument(int instrumentid, int sampleId, int name, int imageResource) {
        this.instrumentid = instrumentid;
        this.name = name;
        this.imageResource = imageResource;
        this.sampleId = sampleId;
    }

    public boolean isLoopback() {
        return instrumentid == 0;
    }
}
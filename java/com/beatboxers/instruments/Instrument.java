package com.beatboxers.instruments;

public class Instrument {
    public int instrumentid;
    public int name;
    public int imageResource;

    public Instrument(int instrumentid, int name, int imageResource) {
        this.instrumentid = instrumentid;
        this.name = name;
        this.imageResource = imageResource;
    }
}
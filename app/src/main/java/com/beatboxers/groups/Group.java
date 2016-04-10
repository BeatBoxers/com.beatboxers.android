package com.beatboxers.groups;

import com.beatboxers.instruments.Instrument;
import com.beatboxers.instruments.Instruments;

import java.util.ArrayList;
import java.util.List;

public class Group {
    public String name;
    public List<Integer> instrumentsIds;

    public Group(String name, List<Integer> instrumentsIds) {
        this.name = name;
        this.instrumentsIds = instrumentsIds;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public List<Instrument> getInstruments() {
        List<Instrument> fullList = Instruments.sharedInstance().getInstruments();
        if (this.name.equalsIgnoreCase("all")) {
            return fullList;
        }
        List<Instrument> filteredData = new ArrayList<>();
        for (Integer includedInstrument : instrumentsIds) {
            for (Instrument instrument : fullList) {
                if (instrument.instrumentid == includedInstrument) {
                    filteredData.add(instrument);
                }
            }
        }
        return filteredData;
    }
}

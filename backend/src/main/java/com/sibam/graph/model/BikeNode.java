package com.sibam.graph.model;

public class BikeNode extends Node {

    private final String stationName;
    private final int stationNumber;
    private final int freeBikes;
    private final int freeStands;

    public BikeNode(int id, double lat, double lon, String stationName) {
        this(id, lat, lon, stationName, id, 0, 0);
    }

    public BikeNode(
            int id,
            double lat,
            double lon,
            String stationName,
            int stationNumber,
            int freeBikes,
            int freeStands
    ) {
        super(id, lat, lon);
        this.stationName = stationName;
        this.stationNumber = stationNumber;
        this.freeBikes = freeBikes;
        this.freeStands = freeStands;
    }

    public String getStationName() {
        return stationName;
    }

    public int getStationNumber() {
        return stationNumber;
    }

    public int getFreeBikes() {
        return freeBikes;
    }

    public int getFreeStands() {
        return freeStands;
    }
}

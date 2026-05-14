package com.example.vidya_vahinitransportationassistance.models;

public class BusStop {
    private String name;
    private double latitude;
    private double longitude;
    private int travelTime;

    public BusStop(String name, double latitude, double longitude, int travelTime) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.travelTime = travelTime;
    }

    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public int getTravelTime() { return travelTime; }
}
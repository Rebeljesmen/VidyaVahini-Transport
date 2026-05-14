package com.example.vidya_vahinitransportationassistance.models;

public class BusRoute {
    private String id;
    private String name;
    private String busNumber;
    private String eta;

    public BusRoute(String id, String name, String busNumber, String eta) {
        this.id = id;
        this.name = name;
        this.busNumber = busNumber;
        this.eta = eta;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getBusNumber() { return busNumber; }
    public String getEta() { return eta; }
}
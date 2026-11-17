package com.vcet.transport.model;

public class Route extends Entity {
    private String source;
    private String destination;
    private double distanceKm;

    public Route(int id, String source, String destination, double distanceKm) {
        super(id);
        this.source = source;
        this.destination = destination;
        this.distanceKm = distanceKm;
    }
    public String getSource() { return source; }
    public String getDestination() { return destination; }
    public double getDistanceKm() { return distanceKm; }
    public void setSource(String s) { this.source = s; }
    public void setDestination(String d) { this.destination = d; }
    public void setDistanceKm(double d) { this.distanceKm = d; }

    @Override public String describe() {
        return String.format("Route #%d | %s -> %s | %.1f km", id, source, destination, distanceKm);
    }
}

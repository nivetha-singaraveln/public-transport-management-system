package com.vcet.transport.model;

public class Bus extends Entity {
    private String busNumber;
    private int capacity;
    private BusType type;

    public Bus(int id, String busNumber, int capacity, BusType type) {
        super(id);
        this.busNumber = busNumber;
        this.capacity = capacity;
        this.type = type;
    }
    public String getBusNumber() { return busNumber; }
    public int getCapacity() { return capacity; }
    public BusType getType() { return type; }
    public void setBusNumber(String n) { this.busNumber = n; }
    public void setCapacity(int c) { this.capacity = c; }
    public void setType(BusType t) { this.type = t; }

    @Override public String describe() {
        return String.format("Bus #%d | %s | %d seats | %s", id, busNumber, capacity, type.label());
    }
}

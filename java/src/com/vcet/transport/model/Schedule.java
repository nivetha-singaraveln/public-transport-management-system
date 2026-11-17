package com.vcet.transport.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Schedule extends Entity {
    private final int busId;
    private final int routeId;
    private final LocalDate travelDate;
    private final LocalTime departureTime;
    private final BigDecimal fare;

    public Schedule(int id, int busId, int routeId, LocalDate travelDate, LocalTime departureTime, BigDecimal fare) {
        super(id);
        this.busId = busId;
        this.routeId = routeId;
        this.travelDate = travelDate;
        this.departureTime = departureTime;
        this.fare = fare;
    }
    public int getBusId() { return busId; }
    public int getRouteId() { return routeId; }
    public LocalDate getTravelDate() { return travelDate; }
    public LocalTime getDepartureTime() { return departureTime; }
    public BigDecimal getFare() { return fare; }
    public LocalDateTime departure() { return LocalDateTime.of(travelDate, departureTime); }

    @Override public String describe() {
        return String.format("Schedule #%d | bus %d | route %d | %s %s | Rs.%s",
                id, busId, routeId, travelDate, departureTime, fare);
    }
}

package com.vcet.transport.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** Read-only joined view of a schedule (bus + route + live seat count). */
public record ScheduleInfo(int scheduleId, String busNumber, BusType busType, String source, String destination,
                           LocalDate travelDate, LocalTime departureTime, BigDecimal fare,
                           int capacity, int seatsBooked) {
    public int seatsAvailable() { return capacity - seatsBooked; }
}

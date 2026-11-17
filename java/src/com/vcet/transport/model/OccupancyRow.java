package com.vcet.transport.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record OccupancyRow(int scheduleId, String busNumber, String busType, String source, String destination,
                           LocalDate travelDate, LocalTime departureTime, int capacity,
                           int seatsBooked, int seatsAvailable, double occupancyPct) { }

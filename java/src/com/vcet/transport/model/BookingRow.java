package com.vcet.transport.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** One row of the booking report (a ticket with its journey details). */
public record BookingRow(int bookingId, String ticketCode, String passenger, String phone,
                         String source, String destination, LocalDate travelDate, LocalTime departureTime,
                         String busNumber, String busType, int seatNo, String status,
                         BigDecimal farePaid, BigDecimal refund, LocalDateTime bookedAt) {
    public String routeName() { return source + " -> " + destination; }
}

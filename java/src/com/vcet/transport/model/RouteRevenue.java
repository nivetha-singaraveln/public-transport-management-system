package com.vcet.transport.model;

import java.math.BigDecimal;

public record RouteRevenue(int routeId, String source, String destination, double distanceKm,
                           int ticketsSold, int ticketsCancelled, BigDecimal ticketRevenue,
                           BigDecimal cancellationFees, BigDecimal netRevenue) {
    public String routeName() { return source + " -> " + destination; }
}

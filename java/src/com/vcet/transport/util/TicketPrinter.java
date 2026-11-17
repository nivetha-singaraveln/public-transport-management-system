package com.vcet.transport.util;

import com.vcet.transport.model.BookingRow;
import java.time.format.DateTimeFormatter;

/** Builds a printable ticket using StringBuilder / String.format. */
public final class TicketPrinter {
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    private static final DateTimeFormatter T = DateTimeFormatter.ofPattern("hh:mm a");
    private TicketPrinter() { }

    public static String format(BookingRow b) {
        String bar = "=".repeat(46);
        StringBuilder sb = new StringBuilder("\n").append(bar).append('\n');
        sb.append(String.format("  PUBLIC TRANSPORT E-TICKET     %s%n", b.ticketCode()));
        sb.append(bar).append('\n');
        sb.append(String.format("  Passenger : %s (%s)%n", b.passenger(), b.phone()));
        sb.append(String.format("  Route     : %s%n", b.routeName()));
        sb.append(String.format("  Date/Time : %s, %s%n", b.travelDate().format(D), b.departureTime().format(T)));
        sb.append(String.format("  Bus       : %s (%s)%n", b.busNumber(), b.busType()));
        sb.append(String.format("  Seat No   : %d%n", b.seatNo()));
        sb.append(String.format("  Fare Paid : Rs. %s%n", b.farePaid().toPlainString()));
        sb.append(String.format("  Status    : %s%n", b.status()));
        if (b.refund().signum() > 0) sb.append(String.format("  Refund    : Rs. %s%n", b.refund().toPlainString()));
        sb.append(bar).append('\n');
        return sb.toString();
    }
}

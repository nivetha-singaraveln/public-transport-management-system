package com.vcet.transport.service;

import com.vcet.transport.dao.BookingDao;
import com.vcet.transport.db.DBConnection;
import com.vcet.transport.exception.TransportException;
import com.vcet.transport.model.*;
import com.vcet.transport.util.SqlErrors;
import com.vcet.transport.util.TablePrinter;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Reports: passenger bookings, route-wise revenue, seat occupancy (SQL views + Java Streams). */
public class ReportService {
    private final BookingDao bookingDao = new BookingDao();

    // ---------- 1. passenger bookings ----------
    public String bookingReport(Integer scheduleId, Integer passengerId, String status, int limit)
            throws TransportException {
        List<BookingRow> rows = bookingDao.find(scheduleId, passengerId, status, limit);
        List<List<String>> t = rows.stream().map(b -> List.of(
                String.valueOf(b.bookingId()), b.ticketCode(), b.passenger(), b.routeName(),
                b.travelDate().toString(), b.departureTime().toString().substring(0, 5), b.busNumber(),
                String.valueOf(b.seatNo()), b.status(), b.farePaid().toPlainString(), b.refund().toPlainString()))
                .collect(Collectors.toList());
        return TablePrinter.render("PASSENGER BOOKING REPORT (latest " + limit + ")",
                List.of("ID", "Ticket", "Passenger", "Route", "Date", "Dep", "Bus", "Seat", "Status", "Fare", "Refund"), t);
    }

    /** Stream-based summary: bookings and spend per passenger (top N). */
    public String topPassengers(int n) throws TransportException {
        List<BookingRow> rows = bookingDao.find(null, null, "CONFIRMED", 100000);
        Map<String, List<BookingRow>> byPax = rows.stream()
                .collect(Collectors.groupingBy(b -> b.passenger() + " (" + b.phone() + ")"));
        List<List<String>> t = byPax.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .limit(n)
                .map(e -> List.of(e.getKey(), String.valueOf(e.getValue().size()),
                        e.getValue().stream().map(BookingRow::farePaid).reduce(BigDecimal.ZERO, BigDecimal::add).toPlainString()))
                .collect(Collectors.toList());
        return TablePrinter.render("TOP " + n + " FREQUENT PASSENGERS", List.of("Passenger", "Tickets", "Total Fare (Rs.)"), t);
    }

    // ---------- 2. route-wise revenue ----------
    public List<RouteRevenue> routeRevenue() throws TransportException {
        List<RouteRevenue> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM v_route_revenue ORDER BY net_revenue DESC")) {
            while (rs.next()) {
                list.add(new RouteRevenue(rs.getInt("route_id"), rs.getString("source"), rs.getString("destination"),
                        rs.getDouble("distance_km"), rs.getInt("tickets_sold"), rs.getInt("tickets_cancelled"),
                        rs.getBigDecimal("ticket_revenue"), rs.getBigDecimal("cancellation_fees"),
                        rs.getBigDecimal("net_revenue")));
            }
            return list;
        } catch (SQLException e) { throw SqlErrors.translate(e); }
    }

    public String routeRevenueReport() throws TransportException {
        List<RouteRevenue> data = routeRevenue();
        BigDecimal total = data.stream().map(RouteRevenue::netRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<List<String>> t = data.stream().map(r -> List.of(r.routeName(),
                String.format("%.0f", r.distanceKm()), String.valueOf(r.ticketsSold()),
                String.valueOf(r.ticketsCancelled()), r.ticketRevenue().toPlainString(),
                r.cancellationFees().toPlainString(), r.netRevenue().toPlainString())).collect(Collectors.toList());
        String out = TablePrinter.render("ROUTE-WISE REVENUE",
                List.of("Route", "Km", "Sold", "Cancelled", "Ticket Rev", "Cancel Fees", "Net Revenue"), t);
        return out + "Total net revenue: Rs. " + total.toPlainString() + "\n"
                + data.stream().findFirst().map(r -> "Highest-demand route: " + r.routeName() + " ("
                    + r.ticketsSold() + " tickets)\n").orElse("");
    }

    // ---------- 3. seat occupancy ----------
    public List<OccupancyRow> occupancy(LocalDate from, LocalDate to) throws TransportException {
        String sql = "SELECT * FROM v_seat_occupancy WHERE travel_date BETWEEN ? AND ? ORDER BY travel_date, departure_time, schedule_id";
        List<OccupancyRow> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(from));
            ps.setDate(2, java.sql.Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new OccupancyRow(rs.getInt("schedule_id"), rs.getString("bus_number"),
                            rs.getString("bus_type"), rs.getString("source"), rs.getString("destination"),
                            rs.getDate("travel_date").toLocalDate(), rs.getTime("departure_time").toLocalTime(),
                            rs.getInt("capacity"), rs.getInt("seats_booked"), rs.getInt("seats_available"),
                            rs.getDouble("occupancy_pct")));
                }
            }
            return list;
        } catch (SQLException e) { throw SqlErrors.translate(e); }
    }

    public String occupancyReport(LocalDate from, LocalDate to, int limit) throws TransportException {
        List<OccupancyRow> data = occupancy(from, to);
        List<List<String>> t = data.stream().limit(limit).map(o -> List.of(
                String.valueOf(o.scheduleId()), o.travelDate().toString(), o.departureTime().toString().substring(0, 5),
                o.source() + " -> " + o.destination(), o.busNumber(), String.valueOf(o.capacity()),
                String.valueOf(o.seatsBooked()), String.valueOf(o.seatsAvailable()),
                String.format("%.1f%%", o.occupancyPct()))).collect(Collectors.toList());
        double avg = data.stream().mapToDouble(OccupancyRow::occupancyPct).average().orElse(0);
        Map<String, Double> byType = data.stream().collect(Collectors.groupingBy(OccupancyRow::busType,
                TreeMap::new, Collectors.averagingDouble(OccupancyRow::occupancyPct)));
        StringBuilder sb = new StringBuilder(TablePrinter.render("SEAT OCCUPANCY " + from + " to " + to
                + " (first " + limit + " of " + data.size() + ")",
                List.of("Sch", "Date", "Dep", "Route", "Bus", "Cap", "Booked", "Free", "Occ%"), t));
        sb.append(String.format("Average occupancy over %d trips: %.1f%%%n", data.size(), avg));
        byType.forEach((k, v) -> sb.append(String.format("  %-15s %.1f%%%n", k, v)));
        return sb.toString();
    }
}

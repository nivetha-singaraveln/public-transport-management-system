package com.vcet.transport.service;

import com.vcet.transport.dao.BookingDao;
import com.vcet.transport.db.DBConnection;
import com.vcet.transport.exception.*;
import com.vcet.transport.model.BookingRow;
import com.vcet.transport.model.CancellationResult;
import com.vcet.transport.util.SqlErrors;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Booking / cancellation business logic.
 *
 * Over-booking is prevented on three levels:
 *   1. SELECT ... FOR UPDATE on the schedule row serialises concurrent bookings of one trip,
 *   2. the capacity check below (confirmed seats  >=  bus capacity  ->  BusFullException),
 *   3. DB safety nets: trigger trg_booking_capacity and UNIQUE(schedule_id, seat_no, seat_lock).
 */
public class BookingService {
    private final BookingDao bookingDao = new BookingDao();

    /** Refund policy: >=24h before departure 90%, 2-24h 50%, <2h 0%. */
    static int refundPercent(long hoursBeforeDeparture) {
        if (hoursBeforeDeparture >= 24) return 90;
        if (hoursBeforeDeparture >= 2)  return 50;
        return 0;
    }

    /**
     * Books one seat. preferredSeat == null  ->  lowest free seat is allotted.
     * @return the issued ticket
     */
    public BookingRow bookTicket(int scheduleId, int passengerId, Integer preferredSeat) throws TransportException {
        int bookingId;
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int capacity;
                BigDecimal fare;
                LocalDateTime departure;
                String lockSql = "SELECT b.capacity, s.fare, s.travel_date, s.departure_time "
                        + "FROM schedule s JOIN bus b ON b.bus_id = s.bus_id WHERE s.schedule_id = ? FOR UPDATE";
                try (PreparedStatement ps = c.prepareStatement(lockSql)) {
                    ps.setInt(1, scheduleId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new ValidationException("Schedule " + scheduleId + " does not exist");
                        capacity = rs.getInt("capacity");
                        fare = rs.getBigDecimal("fare");
                        departure = LocalDateTime.of(rs.getDate("travel_date").toLocalDate(),
                                rs.getTime("departure_time").toLocalTime());
                    }
                }
                if (departure.isBefore(LocalDateTime.now()))
                    throw new ValidationException("Schedule " + scheduleId + " has already departed");

                try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM passenger WHERE passenger_id=?")) {
                    ps.setInt(1, passengerId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new ValidationException("Passenger " + passengerId + " does not exist");
                    }
                }

                SortedSet<Integer> taken = new TreeSet<>();
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT seat_no FROM booking WHERE schedule_id=? AND status='CONFIRMED'")) {
                    ps.setInt(1, scheduleId);
                    try (ResultSet rs = ps.executeQuery()) { while (rs.next()) taken.add(rs.getInt(1)); }
                }
                if (taken.size() >= capacity)
                    throw new BusFullException("Bus is full (" + capacity + "/" + capacity + " seats booked)");

                int seat;
                if (preferredSeat != null) {
                    if (preferredSeat < 1 || preferredSeat > capacity)
                        throw new SeatUnavailableException("Seat " + preferredSeat + " is invalid; this bus has seats 1-" + capacity);
                    if (taken.contains(preferredSeat))
                        throw new SeatUnavailableException("Seat " + preferredSeat + " is already booked");
                    seat = preferredSeat;
                } else {
                    seat = 1;
                    while (taken.contains(seat)) seat++;
                }

                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO booking (schedule_id, passenger_id, seat_no) VALUES (?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, scheduleId);
                    ps.setInt(2, passengerId);
                    ps.setInt(3, seat);
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) { k.next(); bookingId = k.getInt(1); }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO ticket (booking_id, ticket_code, fare_paid) VALUES (?,?,?)")) {
                    ps.setInt(1, bookingId);
                    ps.setString(2, String.format("TKT%06d", bookingId));
                    ps.setBigDecimal(3, fare);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException | TransportException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw SqlErrors.translate(e);
        }
        return bookingDao.findById(bookingId).orElseThrow(() -> new BookingNotFoundException("Ticket vanished"));
    }

    /** Cancels a confirmed booking, frees the seat and records the refund. */
    public CancellationResult cancelTicket(int bookingId) throws TransportException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                String sql = "SELECT bk.status, s.travel_date, s.departure_time, t.fare_paid, t.ticket_code "
                        + "FROM booking bk JOIN schedule s ON s.schedule_id = bk.schedule_id "
                        + "JOIN ticket t ON t.booking_id = bk.booking_id WHERE bk.booking_id = ? FOR UPDATE";
                String status, code;
                BigDecimal fare;
                LocalDateTime departure;
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, bookingId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new BookingNotFoundException("No booking with id " + bookingId);
                        status = rs.getString("status");
                        code = rs.getString("ticket_code");
                        fare = rs.getBigDecimal("fare_paid");
                        departure = LocalDateTime.of(rs.getDate("travel_date").toLocalDate(),
                                rs.getTime("departure_time").toLocalTime());
                    }
                }
                if ("CANCELLED".equals(status))
                    throw new ValidationException("Ticket " + code + " is already cancelled");
                LocalDateTime now = LocalDateTime.now();
                if (!departure.isAfter(now))
                    throw new ValidationException("Ticket " + code + " cannot be cancelled after departure");

                int pct = refundPercent(Duration.between(now, departure).toHours());
                BigDecimal refund = fare.multiply(BigDecimal.valueOf(pct))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                try (PreparedStatement ps = c.prepareStatement("UPDATE booking SET status='CANCELLED' WHERE booking_id=?")) {
                    ps.setInt(1, bookingId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement("UPDATE ticket SET refund_amount=? WHERE booking_id=?")) {
                    ps.setBigDecimal(1, refund);
                    ps.setInt(2, bookingId);
                    ps.executeUpdate();
                }
                c.commit();
                return new CancellationResult(bookingId, code, fare, refund, pct);
            } catch (SQLException | TransportException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw SqlErrors.translate(e);
        }
    }

    public Optional<BookingRow> lookup(String bookingIdOrCode) throws TransportException {
        String t = bookingIdOrCode.trim();
        if (t.matches("\\d+")) return bookingDao.findById(Integer.parseInt(t));
        return bookingDao.findByTicketCode(t);
    }
}

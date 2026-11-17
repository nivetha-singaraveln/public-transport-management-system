package com.vcet.transport.dao;

import com.vcet.transport.db.DBConnection;
import com.vcet.transport.exception.TransportException;
import com.vcet.transport.model.BookingRow;
import com.vcet.transport.util.SqlErrors;
import java.sql.*;
import java.util.*;

/** Read side of bookings (the write side is transactional and lives in BookingService). */
public class BookingDao {
    private static final String BASE = "SELECT * FROM v_booking_report";

    private static BookingRow map(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("booked_at");
        return new BookingRow(rs.getInt("booking_id"), rs.getString("ticket_code"), rs.getString("passenger"),
                rs.getString("phone"), rs.getString("source"), rs.getString("destination"),
                rs.getDate("travel_date").toLocalDate(), rs.getTime("departure_time").toLocalTime(),
                rs.getString("bus_number"), rs.getString("bus_type"), rs.getInt("seat_no"),
                rs.getString("status"), rs.getBigDecimal("fare_paid"), rs.getBigDecimal("refund_amount"),
                ts == null ? null : ts.toLocalDateTime());
    }

    /** Filters may be null. */
    public List<BookingRow> find(Integer scheduleId, Integer passengerId, String status, int limit)
            throws TransportException {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (scheduleId != null) {
            sql.append(" AND booking_id IN (SELECT booking_id FROM booking WHERE schedule_id=?)");
            params.add(scheduleId);
        }
        if (passengerId != null) {
            sql.append(" AND booking_id IN (SELECT booking_id FROM booking WHERE passenger_id=?)");
            params.add(passengerId);
        }
        if (status != null) { sql.append(" AND status=?"); params.add(status); }
        sql.append(" ORDER BY booked_at DESC, booking_id DESC LIMIT ").append(limit);
        List<BookingRow> out = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
            return out;
        } catch (SQLException e) { throw SqlErrors.translate(e); }
    }

    public Optional<BookingRow> findById(int bookingId) throws TransportException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE + " WHERE booking_id=?")) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        } catch (SQLException e) { throw SqlErrors.translate(e); }
    }

    public Optional<BookingRow> findByTicketCode(String code) throws TransportException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE + " WHERE ticket_code=?")) {
            ps.setString(1, code.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        } catch (SQLException e) { throw SqlErrors.translate(e); }
    }

    /** Seat numbers still free on a schedule. */
    public List<Integer> availableSeats(int scheduleId) throws TransportException {
        String cap = "SELECT b.capacity FROM schedule s JOIN bus b ON b.bus_id=s.bus_id WHERE s.schedule_id=?";
        String taken = "SELECT seat_no FROM booking WHERE schedule_id=? AND status='CONFIRMED'";
        try (Connection c = DBConnection.getConnection()) {
            int capacity;
            try (PreparedStatement ps = c.prepareStatement(cap)) {
                ps.setInt(1, scheduleId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new com.vcet.transport.exception.ValidationException("Unknown schedule " + scheduleId);
                    capacity = rs.getInt(1);
                }
            }
            Set<Integer> used = new HashSet<>();
            try (PreparedStatement ps = c.prepareStatement(taken)) {
                ps.setInt(1, scheduleId);
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) used.add(rs.getInt(1)); }
            }
            List<Integer> free = new ArrayList<>();
            for (int i = 1; i <= capacity; i++) if (!used.contains(i)) free.add(i);
            return free;
        } catch (SQLException e) { throw SqlErrors.translate(e); }
    }
}

package com.vcet.transport.dao;

import com.vcet.transport.db.DBConnection;
import com.vcet.transport.exception.TransportException;
import com.vcet.transport.model.*;
import com.vcet.transport.util.SqlErrors;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class ScheduleDao implements CrudDao<Schedule, Integer> {
    private static Schedule map(ResultSet rs) throws SQLException {
        return new Schedule(rs.getInt("schedule_id"), rs.getInt("bus_id"), rs.getInt("route_id"),
                rs.getDate("travel_date").toLocalDate(), rs.getTime("departure_time").toLocalTime(),
                rs.getBigDecimal("fare"));
    }

    @Override public Schedule add(Schedule s) throws TransportException {
        String sql = "INSERT INTO schedule (bus_id, route_id, travel_date, departure_time, fare) VALUES (?,?,?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getBusId());
            ps.setInt(2, s.getRouteId());
            ps.setDate(3, java.sql.Date.valueOf(s.getTravelDate()));
            ps.setTime(4, Time.valueOf(s.getDepartureTime()));
            ps.setBigDecimal(5, s.getFare());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) s.setId(k.getInt(1)); }
            return s;
        } catch (SQLException e) { throw SqlErrors.translate(e); }
    }

    @Override public Optional<Schedule> findById(Integer id) throws TransportException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM schedule WHERE schedule_id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        } catch (SQLException e) { throw SqlErrors.translate(e); }
    }

    @Override public List<Schedule> findAll() throws TransportException {
        List<Schedule> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM schedule ORDER BY travel_date, departure_time")) {
            while (rs.next()) list.add(map(rs));
            return list;
        } catch (SQLException e) { throw SqlErrors.translate(e); }
    }

    @Override public boolean update(Schedule s) throws TransportException {
        String sql = "UPDATE schedule SET bus_id=?, route_id=?, travel_date=?, departure_time=?, fare=? WHERE schedule_id=?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, s.getBusId());
            ps.setInt(2, s.getRouteId());
            ps.setDate(3, java.sql.Date.valueOf(s.getTravelDate()));
            ps.setTime(4, Time.valueOf(s.getDepartureTime()));
            ps.setBigDecimal(5, s.getFare());
            ps.setInt(6, s.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw SqlErrors.translate(e); }
    }

    @Override public boolean delete(Integer id) throws TransportException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM schedule WHERE schedule_id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw SqlErrors.translate(e); }
    }

    /** Joined search; any argument may be null (= no filter). */
    public List<ScheduleInfo> search(String source, String destination, LocalDate from, LocalDate to)
            throws TransportException {
        StringBuilder sql = new StringBuilder(
            "SELECT s.schedule_id, b.bus_number, b.bus_type, r.source, r.destination, s.travel_date, "
          + "s.departure_time, s.fare, b.capacity, "
          + "(SELECT COUNT(*) FROM booking k WHERE k.schedule_id = s.schedule_id AND uPPER(k.status)='CONFIRMED') AS seats_booked "
          + "FROM schedule s JOIN bus b ON b.bus_id = s.bus_id JOIN route r ON r.route_id = s.route_id WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (source != null)      { sql.append(" AND LOWER(r.source)=LOWER(?)");      params.add(source); }
        if (destination != null) { sql.append(" AND LOWER(r.destination)=LOWER(?)"); params.add(destination); }
        if (from != null)        { sql.append(" AND s.travel_date >= ?");            params.add(java.sql.Date.valueOf(from)); }
        if (to != null)          { sql.append(" AND s.travel_date <= ?");            params.add(java.sql.Date.valueOf(to)); }
        sql.append(" ORDER BY s.travel_date, s.departure_time, s.schedule_id");

        List<ScheduleInfo> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ScheduleInfo(rs.getInt("schedule_id"), rs.getString("bus_number"),
                            BusType.valueOf(rs.getString("bus_type")), rs.getString("source"),
                            rs.getString("destination"), rs.getDate("travel_date").toLocalDate(),
                            rs.getTime("departure_time").toLocalTime(), rs.getBigDecimal("fare"),
                            rs.getInt("capacity"), rs.getInt("seats_booked")));
                }
            }
            return list;
        } catch (SQLException e) { throw SqlErrors.translate(e); }
    }

    public Optional<ScheduleInfo> info(int scheduleId) throws TransportException {
        String sql = "SELECT s.schedule_id, b.bus_number, b.bus_type, r.source, r.destination, s.travel_date, "
          + "s.departure_time, s.fare, b.capacity, "
          + "(SELECT COUNT(*) FROM booking k WHERE k.schedule_id = s.schedule_id AND uPPER(k.status)='CONFIRMED') AS seats_booked "
          + "FROM schedule s JOIN bus b ON b.bus_id = s.bus_id JOIN route r ON r.route_id = s.route_id "
          + "WHERE s.schedule_id=?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, scheduleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new ScheduleInfo(rs.getInt("schedule_id"), rs.getString("bus_number"),
                        BusType.valueOf(rs.getString("bus_type")), rs.getString("source"),
                        rs.getString("destination"), rs.getDate("travel_date").toLocalDate(),
                        rs.getTime("departure_time").toLocalTime(), rs.getBigDecimal("fare"),
                        rs.getInt("capacity"), rs.getInt("seats_booked")));
            }
        } catch (SQLException e) { throw SqlErrors.translate(e); }
    }
}

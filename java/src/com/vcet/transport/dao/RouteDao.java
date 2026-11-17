package com.vcet.transport.dao;

import com.vcet.transport.db.DBConnection;
import com.vcet.transport.exception.TransportException;
import com.vcet.transport.model.Route;
import com.vcet.transport.util.SqlErrors;
import java.sql.*;
import java.util.*;

public class RouteDao implements CrudDao<Route, Integer> {
    private static Route map(ResultSet rs) throws SQLException {
        return new Route(rs.getInt("route_id"), rs.getString("source"),
                rs.getString("destination"), rs.getDouble("distance_km"));
    }

    @Override public Route add(Route r) throws TransportException {
        String sql = "INSERT INTO route (source, destination, distance_km) VALUES (?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getSource());
            ps.setString(2, r.getDestination());
            ps.setDouble(3, r.getDistanceKm());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) r.setId(k.getInt(1)); }
            return r;
        } catch (SQLException e) { throw SqlErrors.translate(e); }
    }

    @Override public Optional<Route> findById(Integer id) throws TransportException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM route WHERE route_id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        } catch (SQLException e) { throw SqlErrors.translate(e); }
    }

    @Override public List<Route> findAll() throws TransportException {
        List<Route> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM route ORDER BY route_id")) {
            while (rs.next()) list.add(map(rs));
            return list;
        } catch (SQLException e) { throw SqlErrors.translate(e); }
    }

    @Override public boolean update(Route r) throws TransportException {
        String sql = "UPDATE route SET source=?, destination=?, distance_km=? WHERE route_id=?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.getSource());
            ps.setString(2, r.getDestination());
            ps.setDouble(3, r.getDistanceKm());
            ps.setInt(4, r.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw SqlErrors.translate(e); }
    }

    @Override public boolean delete(Integer id) throws TransportException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM route WHERE route_id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw SqlErrors.translate(e); }
    }
}

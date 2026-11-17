package com.vcet.transport.dao;

import com.vcet.transport.db.DBConnection;
import com.vcet.transport.exception.TransportException;
import com.vcet.transport.model.*;
import com.vcet.transport.util.SqlErrors;
import java.sql.*;
import java.util.*;

public class BusDao implements CrudDao<Bus, Integer> {
    private static Bus map(ResultSet rs) throws SQLException {
        return new Bus(rs.getInt("bus_id"), rs.getString("bus_number"), rs.getInt("capacity"),
                BusType.valueOf(rs.getString("bus_type")));
    }

    @Override public Bus add(Bus b) throws TransportException {
        String sql = "INSERT INTO bus (bus_number, capacity, bus_type) VALUES (?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, b.getBusNumber());
            ps.setInt(2, b.getCapacity());
            ps.setString(3, b.getType().name());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) b.setId(k.getInt(1)); }
            return b;
        } catch (SQLException e) { throw SqlErrors.translate(e); }
    }

    @Override public Optional<Bus> findById(Integer id) throws TransportException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM bus WHERE bus_id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        } catch (SQLException e) { throw SqlErrors.translate(e); }
    }

    @Override public List<Bus> findAll() throws TransportException {
        List<Bus> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM bus ORDER BY bus_id")) {
            while (rs.next()) list.add(map(rs));
            return list;
        } catch (SQLException e) { throw SqlErrors.translate(e); }
    }

    @Override public boolean update(Bus b) throws TransportException {
        String sql = "UPDATE bus SET bus_number=?, capacity=?, bus_type=? WHERE bus_id=?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, b.getBusNumber());
            ps.setInt(2, b.getCapacity());
            ps.setString(3, b.getType().name());
            ps.setInt(4, b.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw SqlErrors.translate(e); }
    }

    @Override public boolean delete(Integer id) throws TransportException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM bus WHERE bus_id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw SqlErrors.translate(e); }
    }
}

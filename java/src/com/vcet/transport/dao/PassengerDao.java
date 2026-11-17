package com.vcet.transport.dao;

import com.vcet.transport.db.DBConnection;
import com.vcet.transport.exception.TransportException;
import com.vcet.transport.model.Passenger;
import com.vcet.transport.util.SqlErrors;
import java.sql.*;
import java.util.*;

public class PassengerDao implements CrudDao<Passenger, Integer> {
    private static Passenger map(ResultSet rs) throws SQLException {
        int age = rs.getInt("age");
        Integer ageObj = rs.wasNull() ? null : age;
        return new Passenger(rs.getInt("passenger_id"), rs.getString("name"), rs.getString("phone"),
                rs.getString("email"), ageObj);
    }

    @Override public Passenger add(Passenger p) throws TransportException {
        String sql = "INSERT INTO passenger (name, phone, email, age) VALUES (?,?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getPhone());
            ps.setString(3, p.getEmail());
            if (p.getAge() == null) ps.setNull(4, Types.INTEGER); else ps.setInt(4, p.getAge());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) p.setId(k.getInt(1)); }
            return p;
        } catch (SQLException e) { throw SqlErrors.translate(e); }
    }

    @Override public Optional<Passenger> findById(Integer id) throws TransportException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM passenger WHERE passenger_id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        } catch (SQLException e) { throw SqlErrors.translate(e); }
    }

    public Optional<Passenger> findByPhone(String phone) throws TransportException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM passenger WHERE phone=?")) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        } catch (SQLException e) { throw SqlErrors.translate(e); }
    }

    @Override public List<Passenger> findAll() throws TransportException {
        List<Passenger> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM passenger ORDER BY passenger_id")) {
            while (rs.next()) list.add(map(rs));
            return list;
        } catch (SQLException e) { throw SqlErrors.translate(e); }
    }

    @Override public boolean update(Passenger p) throws TransportException {
        String sql = "UPDATE passenger SET name=?, phone=?, email=?, age=? WHERE passenger_id=?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getPhone());
            ps.setString(3, p.getEmail());
            if (p.getAge() == null) ps.setNull(4, Types.INTEGER); else ps.setInt(4, p.getAge());
            ps.setInt(5, p.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw SqlErrors.translate(e); }
    }

    @Override public boolean delete(Integer id) throws TransportException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM passenger WHERE passenger_id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw SqlErrors.translate(e); }
    }
}

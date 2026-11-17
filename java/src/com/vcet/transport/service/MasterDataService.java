package com.vcet.transport.service;

import com.vcet.transport.dao.*;
import com.vcet.transport.exception.*;
import com.vcet.transport.model.*;
import com.vcet.transport.util.Validator;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;

/** Validated CRUD for buses, routes, schedules and passengers. */
public class MasterDataService {
    public final BusDao buses = new BusDao();
    public final RouteDao routes = new RouteDao();
    public final ScheduleDao schedules = new ScheduleDao();
    public final PassengerDao passengers = new PassengerDao();

    // ---- Bus ----
    public Bus addBus(String number, int capacity, BusType type) throws TransportException {
        return buses.add(new Bus(0, Validator.busNumber(number), Validator.capacity(capacity), type));
    }
    public boolean updateBus(int id, String number, int capacity, BusType type) throws TransportException {
        Bus b = buses.findById(id).orElseThrow(() -> new ValidationException("Bus " + id + " not found"));
        b.setBusNumber(Validator.busNumber(number));
        b.setCapacity(Validator.capacity(capacity));
        b.setType(type);
        return buses.update(b);
    }
    public boolean deleteBus(int id) throws TransportException { return buses.delete(id); }

    // ---- Route ----
    public Route addRoute(String src, String dst, double km) throws TransportException {
        String s = Validator.place(src), d = Validator.place(dst);
        if (s.equalsIgnoreCase(d)) throw new ValidationException("Source and destination must differ");
        return routes.add(new Route(0, s, d, Validator.distance(km)));
    }
    public boolean updateRoute(int id, String src, String dst, double km) throws TransportException {
        Route r = routes.findById(id).orElseThrow(() -> new ValidationException("Route " + id + " not found"));
        r.setSource(Validator.place(src));
        r.setDestination(Validator.place(dst));
        r.setDistanceKm(Validator.distance(km));
        return routes.update(r);
    }
    public boolean deleteRoute(int id) throws TransportException { return routes.delete(id); }

    // ---- Schedule ----
    public Schedule addSchedule(int busId, int routeId, LocalDate date, LocalTime time, BigDecimal fare)
            throws TransportException {
        if (LocalDateTime.of(date, time).isBefore(LocalDateTime.now()))
            throw new ValidationException("Cannot schedule a trip in the past");
        if (fare == null || fare.signum() <= 0) throw new ValidationException("Fare must be positive");
        buses.findById(busId).orElseThrow(() -> new ValidationException("Bus " + busId + " not found"));
        routes.findById(routeId).orElseThrow(() -> new ValidationException("Route " + routeId + " not found"));
        return schedules.add(new Schedule(0, busId, routeId, date, time, fare));
    }
    public boolean deleteSchedule(int id) throws TransportException { return schedules.delete(id); }

    // ---- Passenger ----
    public Passenger registerPassenger(String name, String phone, String email, String age)
            throws TransportException {
        return passengers.add(new Passenger(0, Validator.name(name), Validator.phone(phone),
                Validator.emailOrNull(email), Validator.ageOrNull(age)));
    }
    public Optional<Passenger> findPassengerByPhone(String phone) throws TransportException {
        return passengers.findByPhone(Validator.phone(phone));
    }
    public List<Passenger> allPassengers() throws TransportException { return passengers.findAll(); }
}

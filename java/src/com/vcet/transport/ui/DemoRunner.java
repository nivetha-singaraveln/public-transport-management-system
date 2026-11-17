package com.vcet.transport.ui;

import com.vcet.transport.dao.BookingDao;
import com.vcet.transport.db.DBConnection;
import com.vcet.transport.exception.*;
import com.vcet.transport.model.*;
import com.vcet.transport.service.*;
import com.vcet.transport.util.CsvExporter;
import com.vcet.transport.util.TablePrinter;
import com.vcet.transport.util.TicketPrinter;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/** Non-interactive, scripted end-to-end run:  java ... Main --demo */
public class DemoRunner {
    private final MasterDataService md = new MasterDataService();
    private final BookingService bs = new BookingService();
    private final ReportService rs = new ReportService();
    private final BookingDao bd = new BookingDao();

    private static void h(String s) { System.out.println("\n########## " + s + " ##########"); }
    private static void ok(String s) { System.out.println("  [OK]   " + s); }
    private static void expectFail(String what, TransportException e) {
        System.out.println("  [BLOCKED] " + what + "\n            -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
    }

    public void run() throws Exception {
        cleanup();
        LocalDate travel = LocalDate.now().plusDays(3);

        h("STEP 1  Maintain bus and route details (Requirements 1 & 2)");
        Bus bus = md.addBus("tn58-demo-0004", 4, BusType.AC_SEATER);          // lower-case -> normalised
        ok(bus.describe());
        Route route = md.addRoute("  madurai ", "SALEM", 135);                  // messy text -> normalised
        ok(route.describe());
        md.updateRoute(route.getId(), "Madurai", "Salem", 140);
        ok("route distance updated -> " + md.routes.findById(route.getId()).orElseThrow().describe());
        try { md.addBus("TN58-DEMO-0004", 40, BusType.DELUXE); }
        catch (TransportException e) { expectFail("duplicate bus number", e); }
        try { md.addBus("TN58-BAD-0001", 200, BusType.DELUXE); }
        catch (TransportException e) { expectFail("capacity 200 (max 80)", e); }

        h("STEP 2  Record schedule, travel date, departure time, fare (Requirement 3)");
        Schedule sch = md.addSchedule(bus.getId(), route.getId(), travel, LocalTime.of(10, 0), new BigDecimal("300.00"));
        ok(sch.describe());
        try { md.addSchedule(bus.getId(), route.getId(), LocalDate.now().minusDays(1), LocalTime.of(9, 0), new BigDecimal("100")); }
        catch (TransportException e) { expectFail("schedule in the past", e); }
        printSchedules(md.schedules.search("Madurai", "Salem", null, null));

        h("STEP 3  Register passengers");
        Passenger p1 = md.registerPassenger("Arun Kumar", "9000000001", "arun@example.com", "29");
        Passenger p2 = md.registerPassenger("Nivetha S", "9000000002", "", "21");
        Passenger p3 = md.registerPassenger("Karthik Raj", "9000000003", null, "");
        ok(p1.describe()); ok(p2.describe()); ok(p3.describe());
        try { md.registerPassenger("Bad Phone", "12345", null, null); }
        catch (TransportException e) { expectFail("invalid phone number", e); }

        h("STEP 4  Book tickets & seat availability (Requirements 4 & 5)");
        BookingRow t1 = bs.bookTicket(sch.getId(), p1.getId(), null);
        ok("Arun auto-allotted seat " + t1.seatNo());
        BookingRow t2 = bs.bookTicket(sch.getId(), p2.getId(), 3);
        ok("Nivetha chose seat " + t2.seatNo());
        try { bs.bookTicket(sch.getId(), p3.getId(), 3); }
        catch (TransportException e) { expectFail("seat 3 already taken", e); }
        try { bs.bookTicket(sch.getId(), p3.getId(), 9); }
        catch (TransportException e) { expectFail("seat 9 on a 4-seat bus", e); }
        BookingRow t3 = bs.bookTicket(sch.getId(), p3.getId(), null);
        ok("Karthik auto-allotted seat " + t3.seatNo());
        BookingRow t4 = bs.bookTicket(sch.getId(), p1.getId(), null);
        ok("Arun booked a 2nd seat: " + t4.seatNo() + "  (bus now full)");
        System.out.println("  free seats: " + bd.availableSeats(sch.getId()));
        try { bs.bookTicket(sch.getId(), p2.getId(), null); }
        catch (TransportException e) { expectFail("booking beyond bus capacity", e); }
        System.out.print(TicketPrinter.format(t2));

        h("STEP 5  Cancel ticket, refund, re-book freed seat (Requirement 4)");
        CancellationResult cr = bs.cancelTicket(t2.bookingId());
        ok(String.format("ticket %s cancelled, refund %d%% = Rs.%s", cr.ticketCode(), cr.refundPercent(), cr.refund()));
        try { bs.cancelTicket(t2.bookingId()); }
        catch (TransportException e) { expectFail("cancelling twice", e); }
        System.out.println("  free seats after cancellation: " + bd.availableSeats(sch.getId()));
        BookingRow t5 = bs.bookTicket(sch.getId(), p3.getId(), 3);
        ok("Karthik re-booked the freed seat " + t5.seatNo());
        ReportService unused = rs;

        h("STEP 6  Reports (Requirement 6)");
        System.out.print(rs.bookingReport(sch.getId(), null, null, 20));
        System.out.print(rs.routeRevenueReport());
        System.out.print(rs.occupancyReport(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), 8));
        System.out.print(rs.occupancyReport(travel, travel, 5));
        System.out.print(rs.topPassengers(5));

        h("STEP 7  Multithreading: 15 threads race for the last 5 seats");
        List<Integer> ids = md.allPassengers().stream().limit(40).map(Passenger::getId).collect(Collectors.toList());
        System.out.print(ConcurrencyDemo.run(5, 15, md, bs, ids));

        h("STEP 8  Clean-up of demo data and CSV export for Python analytics");
        cleanup();
        int n = CsvExporter.export("SELECT * FROM v_booking_report ORDER BY booking_id", Path.of("../outputs/data/bookings_export.csv"));
        ok(n + " booking rows exported to outputs/data/bookings_export.csv");
        System.out.println("\nDEMO COMPLETE");
    }

    private static void printSchedules(List<ScheduleInfo> list) {
        List<List<String>> t = list.stream().map(s -> List.of(String.valueOf(s.scheduleId()), s.source() + " -> " + s.destination(),
                s.travelDate().toString(), s.departureTime().toString().substring(0, 5), s.busNumber(), s.busType().label(),
                s.fare().toPlainString(), s.seatsAvailable() + "/" + s.capacity())).collect(Collectors.toList());
        System.out.print(TablePrinter.render("SCHEDULE SEARCH", List.of("Sch", "Route", "Date", "Dep", "Bus", "Type", "Fare", "Free/Cap"), t));
    }

    /** Removes everything the demo created (safe to call repeatedly). */
    private void cleanup() throws SQLException {
        try (Connection c = DBConnection.getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("DELETE FROM ticket WHERE booking_id IN (SELECT booking_id FROM booking WHERE passenger_id IN "
                    + "(SELECT passenger_id FROM passenger WHERE phone LIKE '900000000_'))");
            st.executeUpdate("DELETE FROM booking WHERE passenger_id IN (SELECT passenger_id FROM passenger WHERE phone LIKE '900000000_')");
            st.executeUpdate("DELETE FROM schedule WHERE bus_id IN (SELECT bus_id FROM bus WHERE bus_number LIKE '%DEMO-0004')");
            st.executeUpdate("DELETE FROM bus WHERE bus_number LIKE '%DEMO-0004'");
            st.executeUpdate("DELETE FROM route WHERE source='Madurai' AND destination='Salem'");
            st.executeUpdate("DELETE FROM passenger WHERE phone LIKE '900000000_'");
        }
    }
}

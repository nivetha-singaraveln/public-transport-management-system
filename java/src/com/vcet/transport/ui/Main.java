package com.vcet.transport.ui;

import com.vcet.transport.dao.BookingDao;
import com.vcet.transport.exception.*;
import com.vcet.transport.model.*;
import com.vcet.transport.service.*;
import com.vcet.transport.util.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/** Console front-end. Run with --demo for the scripted walkthrough. */
public class Main {
    private static final Scanner IN = new Scanner(System.in);
    private static final MasterDataService MD = new MasterDataService();
    private static final BookingService BS = new BookingService();
    private static final ReportService RS = new ReportService();
    private static final BookingDao BD = new BookingDao();

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--demo")) { new DemoRunner().run(); return; }
        System.out.println("=== Public Transport Management & Passenger Travel Analysis System ===");
        boolean running = true;
        while (running) {
            System.out.println("\n1 Buses  2 Routes  3 Schedules  4 Passengers  5 Book ticket  6 Cancel ticket\n"
                    + "7 Seat availability  8 Reports  9 Export CSV for Python  0 Exit");
            String choice = ask("Choice");
            try {
                switch (choice) {
                    case "1": busMenu(); break;
                    case "2": routeMenu(); break;
                    case "3": scheduleMenu(); break;
                    case "4": passengerMenu(); break;
                    case "5": book(); break;
                    case "6": cancel(); break;
                    case "7": seats(); break;
                    case "8": reportMenu(); break;
                    case "9": System.out.println(CsvExporter.export("SELECT * FROM v_booking_report ORDER BY booking_id",
                            Path.of("../outputs/data/bookings_export.csv")) + " rows exported."); break;
                    case "0": running = false; break;
                    default: System.out.println("Unknown option");
                }
            } catch (TransportException e) {
                System.out.println("ERROR: " + e.getMessage());
            } catch (java.time.format.DateTimeParseException | IllegalArgumentException e) {
                System.out.println("Invalid input: " + e.getMessage());
            }
        }
        System.out.println("Goodbye.");
    }

    // ---------- helpers ----------
    private static String ask(String p) { System.out.print(p + ": "); return IN.hasNextLine() ? IN.nextLine().trim() : "0"; }
    private static int askInt(String p) { return Integer.parseInt(ask(p)); }
    private static <T extends Entity> void list(List<T> l) { l.forEach(System.out::println); }

    // ---------- menus ----------
    private static void busMenu() throws TransportException {
        switch (ask("1 add  2 list  3 update  4 delete")) {
            case "1": System.out.println(MD.addBus(ask("Bus number"), askInt("Capacity"),
                    BusType.parse(ask("Type (1 AC Sleeper, 2 AC Seater, 3 Non-AC, 4 Deluxe)")))); break;
            case "2": list(MD.buses.findAll()); break;
            case "3": System.out.println(MD.updateBus(askInt("Bus id"), ask("New number"), askInt("New capacity"),
                    BusType.parse(ask("New type"))) ? "Updated" : "Not found"); break;
            case "4": System.out.println(MD.deleteBus(askInt("Bus id")) ? "Deleted" : "Not found"); break;
            default: System.out.println("Unknown option");
        }
    }

    private static void routeMenu() throws TransportException {
        switch (ask("1 add  2 list  3 update  4 delete")) {
            case "1": System.out.println(MD.addRoute(ask("Source"), ask("Destination"), Double.parseDouble(ask("Distance km")))); break;
            case "2": list(MD.routes.findAll()); break;
            case "3": System.out.println(MD.updateRoute(askInt("Route id"), ask("Source"), ask("Destination"),
                    Double.parseDouble(ask("Distance km"))) ? "Updated" : "Not found"); break;
            case "4": System.out.println(MD.deleteRoute(askInt("Route id")) ? "Deleted" : "Not found"); break;
            default: System.out.println("Unknown option");
        }
    }

    private static void scheduleMenu() throws TransportException {
        switch (ask("1 add  2 search  3 delete")) {
            case "1": System.out.println(MD.addSchedule(askInt("Bus id"), askInt("Route id"), LocalDate.parse(ask("Date yyyy-mm-dd")),
                    LocalTime.parse(ask("Departure HH:mm")), new BigDecimal(ask("Fare")))); break;
            case "2": {
                String s = ask("Source (blank=any)"), d = ask("Destination (blank=any)"), f = ask("From date (blank=today)");
                LocalDate from = f.isEmpty() ? LocalDate.now() : LocalDate.parse(f);
                List<ScheduleInfo> r = MD.schedules.search(s.isEmpty() ? null : s, d.isEmpty() ? null : d, from, from.plusDays(7));
                System.out.print(TablePrinter.render("SCHEDULES", List.of("Sch", "Route", "Date", "Dep", "Bus", "Type", "Fare", "Free/Cap"),
                        r.stream().map(x -> List.of(String.valueOf(x.scheduleId()), x.source() + " -> " + x.destination(),
                                x.travelDate().toString(), x.departureTime().toString().substring(0, 5), x.busNumber(),
                                x.busType().label(), x.fare().toPlainString(), x.seatsAvailable() + "/" + x.capacity()))
                                .collect(Collectors.toList())));
                break;
            }
            case "3": System.out.println(MD.deleteSchedule(askInt("Schedule id")) ? "Deleted" : "Not found"); break;
            default: System.out.println("Unknown option");
        }
    }

    private static void passengerMenu() throws TransportException {
        switch (ask("1 register  2 find by phone  3 list (first 20)")) {
            case "1": System.out.println(MD.registerPassenger(ask("Name"), ask("Phone"), ask("Email (optional)"), ask("Age (optional)"))); break;
            case "2": System.out.println(MD.findPassengerByPhone(ask("Phone")).map(Object::toString).orElse("Not found")); break;
            case "3": MD.allPassengers().stream().limit(20).forEach(System.out::println); break;
            default: System.out.println("Unknown option");
        }
    }

    private static void book() throws TransportException {
        int sch = askInt("Schedule id");
        int pax = askInt("Passenger id");
        String seat = ask("Seat no (blank = auto)");
        BookingRow t = BS.bookTicket(sch, pax, seat.isEmpty() ? null : Integer.valueOf(seat));
        System.out.print(TicketPrinter.format(t));
    }

    private static void cancel() throws TransportException {
        String key = ask("Booking id or ticket code");
        BookingRow b = BS.lookup(key).orElseThrow(() -> new BookingNotFoundException("No such ticket"));
        CancellationResult r = BS.cancelTicket(b.bookingId());
        System.out.printf("Ticket %s cancelled. Refund %d%% = Rs.%s%n", r.ticketCode(), r.refundPercent(), r.refund());
    }

    private static void seats() throws TransportException {
        int sch = askInt("Schedule id");
        ScheduleInfo i = MD.schedules.info(sch).orElseThrow(() -> new ValidationException("Unknown schedule"));
        System.out.printf("%s -> %s on %s %s | capacity %d | booked %d | available %d%n", i.source(), i.destination(),
                i.travelDate(), i.departureTime(), i.capacity(), i.seatsBooked(), i.seatsAvailable());
        System.out.println("Free seats: " + BD.availableSeats(sch));
    }

    private static void reportMenu() throws TransportException {
        switch (ask("1 bookings  2 route revenue  3 seat occupancy  4 top passengers")) {
            case "1": System.out.print(RS.bookingReport(null, null, null, 25)); break;
            case "2": System.out.print(RS.routeRevenueReport()); break;
            case "3": {
                LocalDate from = LocalDate.parse(ask("From date yyyy-mm-dd")), to = LocalDate.parse(ask("To date yyyy-mm-dd"));
                System.out.print(RS.occupancyReport(from, to, 25));
                break;
            }
            case "4": System.out.print(RS.topPassengers(10)); break;
            default: System.out.println("Unknown option");
        }
    }
}

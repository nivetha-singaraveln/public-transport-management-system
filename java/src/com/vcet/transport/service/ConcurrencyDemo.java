package com.vcet.transport.service;

import com.vcet.transport.exception.*;
import com.vcet.transport.model.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Multithreading demo: many threads fight for the last seats of a small bus.
 * Exactly `capacity` bookings may succeed - the rest must get BusFullException.
 */
public class ConcurrencyDemo {
    public static String run(int capacity, int threads, MasterDataService md, BookingService bs,
                             List<Integer> passengerIds) throws Exception {
        Bus bus = md.addBus("TN58-CC-" + (System.currentTimeMillis() % 100000), capacity, BusType.DELUXE);
        Route route = md.routes.findAll().get(0);
        Schedule sch = md.addSchedule(bus.getId(), route.getId(), LocalDate.now().plusDays(5),
                LocalTime.of(23, 45), new BigDecimal("500.00"));

        AtomicInteger ok = new AtomicInteger(), full = new AtomicInteger(), other = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGun = new CountDownLatch(1);
        List<Future<?>> fs = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final int pid = passengerIds.get(i % passengerIds.size());
            fs.add(pool.submit(() -> {
                try {
                    startGun.await();
                    bs.bookTicket(sch.getId(), pid, null);
                    ok.incrementAndGet();
                } catch (BusFullException e) { full.incrementAndGet();
                } catch (Exception e) { other.incrementAndGet(); System.out.println("  unexpected: " + e.getMessage()); }
            }));
        }
        startGun.countDown();                       // all threads start together
        for (Future<?> f : fs) f.get();
        pool.shutdown();

        int confirmed = md.schedules.info(sch.getId()).orElseThrow().seatsBooked();
        String result = String.format(
            "Bus capacity = %d, competing threads = %d%n  successful bookings : %d%n  rejected (bus full) : %d%n"
          + "  other errors        : %d%n  confirmed seats in DB: %d  -> %s%n",
            capacity, threads, ok.get(), full.get(), other.get(), confirmed,
            confirmed == capacity && ok.get() == capacity ? "NO OVERBOOKING" : "PROBLEM!");

        // clean up the temporary trip so the demo can be re-run
        try (var c = com.vcet.transport.db.DBConnection.getConnection(); var st = c.createStatement()) {
            st.executeUpdate("DELETE FROM ticket WHERE booking_id IN (SELECT booking_id FROM booking WHERE schedule_id=" + sch.getId() + ")");
            st.executeUpdate("DELETE FROM booking WHERE schedule_id=" + sch.getId());
        }
        md.deleteSchedule(sch.getId());
        md.deleteBus(bus.getId());
        return result;
    }
}

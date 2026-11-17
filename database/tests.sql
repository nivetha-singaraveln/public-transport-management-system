-- DB-level tests: prove the database refuses bad data even without the Java layer.
-- Run:  mariadb -uroot --force --table < database/tests.sql   (everything is rolled back at the end)
USE transport_db;
START TRANSACTION;
INSERT INTO bus (bus_number, capacity, bus_type) VALUES ('TEST-0002', 2, 'DELUXE');
SET @bus = LAST_INSERT_ID();
INSERT INTO schedule (bus_id, route_id, travel_date, departure_time, fare)
       VALUES (@bus, 1, '2026-12-31', '10:00:00', 400);
SET @sch = LAST_INSERT_ID();

SELECT 'T1: book seat 1 and seat 2 on a 2-seat bus (must succeed)' AS test;
INSERT INTO booking (schedule_id, passenger_id, seat_no) VALUES (@sch, 1, 1);
INSERT INTO booking (schedule_id, passenger_id, seat_no) VALUES (@sch, 2, 2);
SELECT seats_booked, seats_available FROM v_seat_occupancy WHERE schedule_id = @sch;

SELECT 'T2: third booking beyond capacity (must FAIL: Bus is full)' AS test;
INSERT INTO booking (schedule_id, passenger_id, seat_no) VALUES (@sch, 3, 1);

SELECT 'T3: seat number above capacity (must FAIL)' AS test;
INSERT INTO booking (schedule_id, passenger_id, seat_no) VALUES (@sch, 3, 7);

SELECT 'T4: cancel seat 2, then re-book it (must succeed)' AS test;
UPDATE booking SET status='CANCELLED' WHERE schedule_id=@sch AND seat_no=2;
SELECT seat_no, status, cancelled_at IS NOT NULL AS cancel_time_set FROM booking WHERE schedule_id=@sch ORDER BY seat_no;
INSERT INTO booking (schedule_id, passenger_id, seat_no) VALUES (@sch, 3, 2);

SELECT 'T5: reactivate a cancelled booking (must FAIL)' AS test;
UPDATE booking SET status='CONFIRMED' WHERE schedule_id=@sch AND status='CANCELLED';

SELECT 'T6: invalid data - negative fare, duplicate route, bad age (all must FAIL)' AS test;
INSERT INTO schedule (bus_id, route_id, travel_date, departure_time, fare) VALUES (@bus, 1, '2026-12-30', '10:00:00', -5);
INSERT INTO route (source, destination, distance_km) VALUES ('Madurai', 'Chennai', 460);
INSERT INTO passenger (name, phone, age) VALUES ('X', '9999999999', 150);
ROLLBACK;
SELECT 'ROLLED BACK - database unchanged' AS result, (SELECT COUNT(*) FROM bus) AS buses;

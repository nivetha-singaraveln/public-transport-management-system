-- ============================================================
-- Public Transport Management and Passenger Travel Analysis System
-- Scenario 10 | Database: MariaDB / MySQL
-- ============================================================
DROP DATABASE IF EXISTS transport_db;
CREATE DATABASE transport_db CHARACTER SET utf8mb4;
USE transport_db;

-- 1. BUS --------------------------------------------------------
CREATE TABLE bus (
    bus_id      INT AUTO_INCREMENT PRIMARY KEY,
    bus_number  VARCHAR(20) NOT NULL UNIQUE,
    capacity    INT         NOT NULL,
    bus_type    ENUM('AC_SLEEPER','AC_SEATER','NON_AC_SEATER','DELUXE') NOT NULL,
    CONSTRAINT chk_bus_capacity CHECK (capacity BETWEEN 1 AND 80)
) ENGINE=InnoDB;

-- 2. ROUTE ------------------------------------------------------
CREATE TABLE route (
    route_id     INT AUTO_INCREMENT PRIMARY KEY,
    source       VARCHAR(50) NOT NULL,
    destination  VARCHAR(50) NOT NULL,
    distance_km  DECIMAL(7,2) NOT NULL,
    CONSTRAINT uq_route UNIQUE (source, destination),
    CONSTRAINT chk_route_distance CHECK (distance_km > 0),
    CONSTRAINT chk_route_diff CHECK (source <> destination)
) ENGINE=InnoDB;

-- 3. SCHEDULE ---------------------------------------------------
CREATE TABLE schedule (
    schedule_id     INT AUTO_INCREMENT PRIMARY KEY,
    bus_id          INT  NOT NULL,
    route_id        INT  NOT NULL,
    travel_date     DATE NOT NULL,
    departure_time  TIME NOT NULL,
    fare            DECIMAL(8,2) NOT NULL,
    CONSTRAINT fk_sch_bus   FOREIGN KEY (bus_id)   REFERENCES bus(bus_id),
    CONSTRAINT fk_sch_route FOREIGN KEY (route_id) REFERENCES route(route_id),
    CONSTRAINT uq_bus_slot  UNIQUE (bus_id, travel_date, departure_time),
    CONSTRAINT chk_fare CHECK (fare > 0)
) ENGINE=InnoDB;

-- 4. PASSENGER --------------------------------------------------
CREATE TABLE passenger (
    passenger_id INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(80) NOT NULL,
    phone        VARCHAR(15) NOT NULL UNIQUE,
    email        VARCHAR(100) NULL,
    age          INT NULL,
    CONSTRAINT chk_age CHECK (age IS NULL OR age BETWEEN 1 AND 110)
) ENGINE=InnoDB;

-- 5. BOOKING ----------------------------------------------------
-- seat_lock is 1 while a booking is CONFIRMED and NULL once cancelled.
-- UNIQUE(schedule_id, seat_no, seat_lock) therefore forbids two active
-- bookings of one seat, yet lets a cancelled seat be re-booked.
CREATE TABLE booking (
    booking_id    INT AUTO_INCREMENT PRIMARY KEY,
    schedule_id   INT NOT NULL,
    passenger_id  INT NOT NULL,
    seat_no       INT NOT NULL,
    status        ENUM('CONFIRMED','CANCELLED') NOT NULL DEFAULT 'CONFIRMED',
    booked_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at  DATETIME NULL,
    seat_lock     TINYINT AS (IF(status='CONFIRMED',1,NULL)) PERSISTENT,
    CONSTRAINT fk_bk_sch FOREIGN KEY (schedule_id)  REFERENCES schedule(schedule_id),
    CONSTRAINT fk_bk_pax FOREIGN KEY (passenger_id) REFERENCES passenger(passenger_id),
    CONSTRAINT uq_active_seat UNIQUE (schedule_id, seat_no, seat_lock),
    CONSTRAINT chk_seat CHECK (seat_no > 0)
) ENGINE=InnoDB;

-- 6. TICKET -----------------------------------------------------
CREATE TABLE ticket (
    ticket_id     INT AUTO_INCREMENT PRIMARY KEY,
    booking_id    INT NOT NULL UNIQUE,
    ticket_code   VARCHAR(20) NOT NULL UNIQUE,
    fare_paid     DECIMAL(8,2) NOT NULL,
    refund_amount DECIMAL(8,2) NOT NULL DEFAULT 0,
    issued_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tk_bk FOREIGN KEY (booking_id) REFERENCES booking(booking_id)
) ENGINE=InnoDB;

CREATE INDEX idx_sched_route ON schedule(route_id, travel_date);
CREATE INDEX idx_booking_sched ON booking(schedule_id, status);

-- ---------------- TRIGGERS: second line of defence ----------------
DELIMITER //
CREATE TRIGGER trg_booking_capacity
BEFORE INSERT ON booking
FOR EACH ROW
BEGIN
    DECLARE v_cap INT;
    DECLARE v_taken INT;
    SELECT b.capacity INTO v_cap
      FROM schedule s JOIN bus b ON b.bus_id = s.bus_id
     WHERE s.schedule_id = NEW.schedule_id;
    SELECT COUNT(*) INTO v_taken FROM booking
     WHERE schedule_id = NEW.schedule_id AND status = 'CONFIRMED';
    IF NEW.seat_no > v_cap THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Seat number exceeds bus capacity';
    END IF;
    IF NEW.status = 'CONFIRMED' AND v_taken >= v_cap THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Bus is full: booking exceeds capacity';
    END IF;
END//

CREATE TRIGGER trg_booking_cancel_time
BEFORE UPDATE ON booking
FOR EACH ROW
BEGIN
    IF OLD.status = 'CANCELLED' AND NEW.status = 'CONFIRMED' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'A cancelled booking cannot be reactivated';
    END IF;
    IF OLD.status = 'CONFIRMED' AND NEW.status = 'CANCELLED' THEN
        SET NEW.cancelled_at = NOW();
    END IF;
END//
DELIMITER ;

-- ---------------- VIEWS: reports ----------------
CREATE VIEW v_booking_report AS
SELECT bk.booking_id, t.ticket_code, p.name AS passenger, p.phone,
       r.source, r.destination, s.travel_date, s.departure_time,
       b.bus_number, b.bus_type, bk.seat_no, bk.status,
       t.fare_paid, t.refund_amount, bk.booked_at, bk.cancelled_at
  FROM booking bk
  JOIN ticket t    ON t.booking_id = bk.booking_id
  JOIN passenger p ON p.passenger_id = bk.passenger_id
  JOIN schedule s  ON s.schedule_id = bk.schedule_id
  JOIN route r     ON r.route_id = s.route_id
  JOIN bus b       ON b.bus_id = s.bus_id;

CREATE VIEW v_route_revenue AS
SELECT r.route_id, r.source, r.destination, r.distance_km,
       COUNT(CASE WHEN bk.status = 'CONFIRMED' THEN 1 END)  AS tickets_sold,
       COUNT(CASE WHEN bk.status = 'CANCELLED' THEN 1 END)  AS tickets_cancelled,
       COALESCE(SUM(CASE WHEN bk.status = 'CONFIRMED' THEN t.fare_paid END), 0) AS ticket_revenue,
       COALESCE(SUM(CASE WHEN bk.status = 'CANCELLED' THEN t.fare_paid - t.refund_amount END), 0) AS cancellation_fees,
       COALESCE(SUM(t.fare_paid - t.refund_amount), 0)       AS net_revenue
  FROM route r
  LEFT JOIN schedule s ON s.route_id = r.route_id
  LEFT JOIN booking bk ON bk.schedule_id = s.schedule_id
  LEFT JOIN ticket t   ON t.booking_id = bk.booking_id
 GROUP BY r.route_id, r.source, r.destination, r.distance_km;

CREATE VIEW v_seat_occupancy AS
SELECT s.schedule_id, b.bus_number, b.bus_type, r.source, r.destination,
       s.travel_date, s.departure_time, b.capacity,
       COUNT(bk.booking_id)                       AS seats_booked,
       b.capacity - COUNT(bk.booking_id)          AS seats_available,
       ROUND(100 * COUNT(bk.booking_id) / b.capacity, 1) AS occupancy_pct
  FROM schedule s
  JOIN bus b   ON b.bus_id = s.bus_id
  JOIN route r ON r.route_id = s.route_id
  LEFT JOIN booking bk ON bk.schedule_id = s.schedule_id AND bk.status = 'CONFIRMED'
 GROUP BY s.schedule_id, b.bus_number, b.bus_type, r.source, r.destination,
          s.travel_date, s.departure_time, b.capacity;

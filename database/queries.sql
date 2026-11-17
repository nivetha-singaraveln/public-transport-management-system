-- Report & analysis queries (DBMS course: joins, aggregates, sub-queries, views, window functions)
USE transport_db;

-- Q1  Seat availability for one schedule (capacity - confirmed bookings)
SELECT s.schedule_id, b.capacity,
       COUNT(bk.booking_id) AS booked,
       b.capacity - COUNT(bk.booking_id) AS available
  FROM schedule s JOIN bus b ON b.bus_id = s.bus_id
  LEFT JOIN booking bk ON bk.schedule_id = s.schedule_id AND bk.status='CONFIRMED'
 WHERE s.schedule_id = 206
 GROUP BY s.schedule_id, b.capacity;

-- Q2  Seats still free on schedule 206 (set difference using a number generator)
WITH RECURSIVE n(seat) AS (SELECT 1 UNION ALL SELECT seat+1 FROM n WHERE seat < 80)
SELECT n.seat FROM n
  JOIN schedule s ON s.schedule_id = 206 JOIN bus b ON b.bus_id = s.bus_id AND n.seat <= b.capacity
 WHERE n.seat NOT IN (SELECT seat_no FROM booking WHERE schedule_id = 206 AND status='CONFIRMED')
 LIMIT 10;

-- Q3  Passenger booking report: bookings per passenger (HAVING)
SELECT p.name, p.phone, COUNT(*) AS tickets, SUM(t.fare_paid) AS spent
  FROM booking bk JOIN passenger p ON p.passenger_id = bk.passenger_id
  JOIN ticket t ON t.booking_id = bk.booking_id
 WHERE bk.status='CONFIRMED'
 GROUP BY p.passenger_id, p.name, p.phone
HAVING COUNT(*) >= 25 ORDER BY tickets DESC LIMIT 5;

-- Q4  Route-wise revenue (view)
SELECT source, destination, tickets_sold, tickets_cancelled, net_revenue FROM v_route_revenue ORDER BY net_revenue DESC;

-- Q5  Seat occupancy: ten fullest completed trips (view)
SELECT schedule_id, source, destination, travel_date, capacity, seats_booked, occupancy_pct
  FROM v_seat_occupancy WHERE travel_date < CURDATE() ORDER BY occupancy_pct DESC, schedule_id LIMIT 10;

-- Q6  Sub-query: routes earning more than the average route
SELECT source, destination, net_revenue FROM v_route_revenue
 WHERE net_revenue > (SELECT AVG(net_revenue) FROM v_route_revenue) ORDER BY net_revenue DESC;

-- Q7  Window function: rank routes by tickets within each bus type
SELECT bus_type, route, tickets, RANK() OVER (PARTITION BY bus_type ORDER BY tickets DESC) AS rnk FROM (
  SELECT b.bus_type, CONCAT(r.source,' -> ',r.destination) AS route, COUNT(*) AS tickets
    FROM booking bk JOIN schedule s ON s.schedule_id = bk.schedule_id
    JOIN bus b ON b.bus_id = s.bus_id JOIN route r ON r.route_id = s.route_id
   WHERE bk.status='CONFIRMED' GROUP BY b.bus_type, r.route_id, r.source, r.destination) x
 ORDER BY bus_type, rnk;

-- Q8  Busy-day analysis: tickets per weekday
SELECT DAYNAME(s.travel_date) AS weekday, COUNT(*) AS tickets
  FROM booking bk JOIN schedule s ON s.schedule_id = bk.schedule_id
 WHERE bk.status='CONFIRMED' AND s.travel_date < CURDATE()
 GROUP BY DAYNAME(s.travel_date), WEEKDAY(s.travel_date) ORDER BY WEEKDAY(s.travel_date);

-- Q9  Integrity audit: confirmed bookings must never exceed capacity (expect 0 rows)
SELECT schedule_id, capacity, seats_booked FROM v_seat_occupancy WHERE seats_booked > capacity;

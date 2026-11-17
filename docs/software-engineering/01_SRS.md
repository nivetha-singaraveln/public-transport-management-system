# Software Requirements Specification (SRS)
**Project:** Public Transport Management and Passenger Travel Analysis System · **Scenario 10** · Version 1.0

## 1. Introduction
**Purpose.** Specify the requirements of a system that manages bus, route and schedule data, handles passenger
bookings and cancellations, and analyses travel patterns.
**Scope.** Back-office console application (Java + relational database) and an offline analytics module (Python).
Online payment, GPS tracking and a web/mobile front end are out of scope.
**Problem.** Manual records make it hard to track seat availability, manage bookings, and see which routes have
high demand.

## 2. Stakeholders / users
| User | Needs |
|---|---|
| Booking clerk | Search trips, book / cancel tickets quickly, print ticket |
| Transport administrator | Maintain buses, routes, schedules; view reports |
| Transport analyst / manager | Demand, revenue and occupancy insights to plan capacity |

## 3. Functional requirements
| ID | Requirement | Scenario req. |
|---|---|---|
| FR1 | Add, view, update and delete **buses** (number, capacity, type) | 1 |
| FR2 | Add, view, update and delete **routes** (source, destination, distance) | 2 |
| FR3 | Create **schedules** (bus, route, travel date, departure time, fare); search by route/date | 3 |
| FR4 | Register **passengers** (name, phone, optional e-mail/age) | 4 |
| FR5 | **Book** a ticket (auto-allot or choose seat) and issue a ticket record with a unique code | 4 |
| FR6 | **Cancel** a ticket, free the seat, compute and record the refund | 4 |
| FR7 | Show **seat availability** and **prevent bookings beyond bus capacity** | 5 |
| FR8 | Reports: passenger **bookings**, **route-wise revenue**, **seat occupancy** | 6 |
| FR9 | Export booking data and **analyse/visualise** it with Python | 7 |

## 4. Business rules
BR1 One active booking per seat per trip. BR2 Seat number must lie in `1..capacity`. BR3 No booking/cancellation after
departure. BR4 No schedule in the past; fare > 0. BR5 Refund: ≥ 24 h → 90 %, 2–24 h → 50 %, < 2 h → 0 %.
BR6 A cancelled booking can never be re-activated (a new booking is made instead). BR7 Phone numbers are unique
10-digit Indian mobiles.

## 5. Non-functional requirements
| ID | Requirement | How it is met |
|---|---|---|
| NFR1 Integrity | No over-booking, even with simultaneous users | Row lock + capacity check + trigger + unique index; verified with 15 threads |
| NFR2 Reliability | Booking is all-or-nothing | JDBC transaction (booking + ticket), rollback on any error |
| NFR3 Usability | Clear messages for every rejected action | Custom exception hierarchy, input validation |
| NFR4 Performance | Reports on ~6k bookings < 1 s | Indexes on `schedule(route_id, travel_date)` and `booking(schedule_id, status)` |
| NFR5 Maintainability | Layered code, one class per concern | model / dao / service / ui packages |
| NFR6 Portability | MariaDB / MySQL, Java 16+ | Standard JDBC and SQL |
| NFR7 Reproducibility | Same data on every machine | Seeded data generator |

## 6. Use cases
| UC | Actor | Main flow | Alternate / exception flows |
|---|---|---|---|
| UC1 Book ticket | Clerk | Search schedule → pick passenger → choose seat or auto → system locks trip, checks capacity, inserts booking + ticket → ticket printed | Seat taken → `SeatUnavailableException`; bus full → `BusFullException`; trip departed → `ValidationException` |
| UC2 Cancel ticket | Clerk | Enter booking id or ticket code → system calculates refund → seat freed | Already cancelled / departed → `ValidationException`; unknown id → `BookingNotFoundException` |
| UC3 Manage master data | Admin | Add/update/delete bus, route, schedule | Duplicate / invalid / referenced record → message |
| UC4 View reports | Admin | Choose report → table shown | – |
| UC5 Analyse travel patterns | Analyst | Run `analysis.py` → charts + insight report | DB unreachable → clear error |

## 7. Assumptions & constraints
Single depot, single currency (Rs.), a seat is identified by its number only, one fare per schedule. Java, DBMS and
Python are mandated by the assessment.

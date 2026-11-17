# Public Transport Management and Passenger Travel Analysis System

**Department of Information Technology – Department-Level Comprehensive Assessment (AY 2026-27 Odd)**
**Scenario 10** · Register No.: 10, 40, 70, 100 · Student: *<your name / register number>*

A system that maintains buses, routes and schedules, lets passengers book and cancel tickets
without ever exceeding bus capacity, produces booking / revenue / occupancy reports, and analyses
travel patterns with Python.

| Course | Role in this project | Where |
|---|---|---|
| **Java Programming** (25ITT32) | Application: OOP model, JDBC, exceptions, threads, streams | `java/` |
| **Database Management Systems** (25ITT31) | Schema, constraints, triggers, views, transactions, queries | `database/` |
| **Foundations of Data Science** (25ITC31) | Retrieve → clean → explore → visualise booking data | `python/` |
| **Software Engineering** (25ITT33) | SRS, design (UML), testing, estimation, traceability | `docs/software-engineering/` |
| **Web front end** (bonus) | Live website with an interactive Tamil Nadu district map | `website/` |

## Repository layout

```
README.md                     this file
run_all.sh                    one command: create DB → tests → build → Java demo → Python analytics
database/
  schema.sql                  tables, constraints, triggers, views
  sample_data.sql             12 buses, 10 routes, 231 schedules, 320 passengers, 5,800 bookings
  generate_sample_data.py     reproducible generator for sample_data.sql (seed = 10)
  create_user.sql             application DB account
  queries.sql                 report / analysis queries
  tests.sql                   DB-level tests (constraints, triggers) - rolled back afterwards
java/
  src/com/vcet/transport/     model · exception · db · dao · service · ui · util  (34 Java files)
  build.sh  run.sh            compile / run  (run.sh --demo = scripted walkthrough)
  db.properties               JDBC settings
python/
  analysis.py                 retrieval, preprocessing, EDA, charts, insights
  render_console.py           renders console logs to PNG screenshots
  requirements.txt
lib/mariadb-java-client.jar   JDBC driver
website/index.html            live website: Tamil Nadu map, booking, seats, My trips, dashboard, admin
.github/workflows/pages.yml   auto-deploys website/ to GitHub Pages
docs/                         course-wise documents + diagrams (ER, class, architecture)
outputs/
  screenshots/                console screenshots of the live Java run
  charts/                     11 Python visualisations (PNG)
  reports/                    raw text outputs of every run (Java, SQL, tests, analysis)
  data/                       CSV export + cleaned datasets
```

## Technologies used

Java 21 (needs Java 16+ because of `record` types) · JDBC with MariaDB Connector/J 2.7 · MariaDB 10.11 (MySQL-compatible) ·
Python 3.12 · pandas · NumPy · matplotlib · seaborn · PyMySQL · Graphviz (diagrams)

## Database setup

```bash
# 1. start MariaDB / MySQL, then (as a DB admin)
mariadb -uroot < database/schema.sql          # creates transport_db, tables, triggers, views
mariadb -uroot < database/sample_data.sql     # loads the sample data
mariadb -uroot < database/create_user.sql     # user: transport / transport123 (matches java/db.properties)
```

* **MySQL 8 users:** in `schema.sql` change `PERSISTENT` to `STORED` (generated column) and use the MySQL
  Connector/J driver + `jdbc:mysql://...` URL. Everything else is standard SQL (CHECK needs MySQL ≥ 8.0.16).
* On Windows use `mysql -u root -p < database\schema.sql` etc.
* Connection settings can be overridden with environment variables `DB_URL`, `DB_USER`, `DB_PASSWORD`
  (Java) and `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, `DB_NAME` (Python).

## How to run

```bash
# Java (needs JDK 16+, tested on 21)
cd java
./build.sh                  # javac -> java/out
./run.sh                    # interactive console menu
./run.sh --demo             # scripted end-to-end demonstration (re-runnable, cleans up after itself)
# Windows:  javac -d out -cp ..\lib\mariadb-java-client.jar (dir /s /b src\*.java)
#           java -cp "out;..\lib\mariadb-java-client.jar" com.vcet.transport.ui.Main --demo

# Python (from project root)
pip install -r python/requirements.txt
python3 python/analysis.py  # writes outputs/charts/*.png and outputs/reports/analysis_report.txt
```

Or simply `./run_all.sh`.

## Live website (GitHub Pages, free 24x7)

`website/index.html` is a single self-contained page (no build step, no server needed):

* **Interactive Tamil Nadu map** (37 districts, real boundaries). Tap two cities to choose a route; direct routes are drawn, unreachable cities fade out. City names are shown in English and Tamil.
* **Guided booking:** choose route and day, filter by bus type, pick seats on a bus-layout seat map (up to 4), enter name and mobile, get an e-ticket you can download.
* **My trips:** find tickets by mobile number, cancel with a clear refund preview (90% / 50% / 0%).
* **Dashboard:** KPIs, a demand map (line thickness = tickets), route revenue, occupancy, tickets per day, latest bookings (names masked).
* **Admin:** add buses, routes and special schedules; add or remove sample bookings for demos; export tickets as CSV.
* Responsive (phone to desktop), light and dark theme, keyboard accessible.

The website runs on its own **extended network** (12 cities, 17 routes, 16 buses, 7 days of generated trips); the Java/SQL project keeps the 10-route dataset required by the scenario.
On GitHub Pages the site stores bookings in the visitor's own browser (localStorage), so it works without a server but data is not shared between visitors. For shared bookings, connect the same rules to a real backend: the Java + MariaDB system in this repo is that backend.

**Publish it:**
1. Push this repo to GitHub (commands below).
2. Repo **Settings → Pages → Build and deployment → Source: GitHub Actions**.
3. Open **Actions → Deploy website to GitHub Pages**; after it turns green your site is at `https://<your-username>.github.io/<repo-name>/`.

## Push to GitHub

```bash
cd transport-system
git init
git add .
git commit -m "Public Transport Management and Passenger Travel Analysis System - Scenario 10"
git branch -M main
# create an EMPTY repository named e.g. transport-management-system on github.com, then:
git remote add origin https://github.com/<your-username>/transport-management-system.git
git push -u origin main
```
Then share the repository link with your course faculty before the assessment date, as the circular requires.

## Requirement coverage

| # | Requirement (from scenario) | Implementation | Evidence |
|---|---|---|---|
| 1 | Bus details: number, capacity, type | `bus` table · `Bus`, `BusDao`, `MasterDataService.addBus/updateBus` | demo step 1 |
| 2 | Route info: source, destination, distance | `route` table · `Route`, `RouteDao` | demo step 1 |
| 3 | Schedules, travel dates, departure times, fares | `schedule` table · `ScheduleDao.search` | demo step 2 |
| 4 | Book & cancel tickets | `BookingService.bookTicket / cancelTicket` (transactional, refund policy) | demo steps 4-5 |
| 5 | Track seats, block over-booking | row lock + capacity check + trigger + unique seat index | demo steps 4 & 7, `db_tests_output.txt` |
| 6 | Reports: bookings, route revenue, occupancy | `ReportService` + views `v_booking_report`, `v_route_revenue`, `v_seat_occupancy` | demo step 6 |
| 7 | Python: retrieve, preprocess, visualise | `python/analysis.py` | `outputs/charts/`, `analysis_report.txt` |
| – | Web interface for the same workflows | `website/index.html` | `outputs/screenshots/05…09_website_*.png` |

## Business rules implemented

* One active booking per seat per trip; a cancelled seat becomes free and can be re-booked.
* A booking beyond capacity is rejected (`BusFullException`); a seat number outside `1..capacity` is rejected.
* No booking or cancellation after departure; no schedule in the past.
* Refund on cancellation: **≥ 24 h before departure 90 %, 2–24 h 50 %, < 2 h 0 %**. The retained part is counted as revenue.
* Ticket code format `TKT000123`, generated from the booking id.

## Key results of the live run (14-Nov-2026)

* 5,800 bookings (5,325 confirmed, 475 cancelled = 8.2 %), net revenue ≈ Rs. 23.7 lakh.
* Highest-demand and highest-revenue route: **Madurai → Chennai** (959 tickets, 25.7 % of revenue).
* Fri–Sun trips run 76 % full vs 64 % Mon–Thu. Concurrency test: 15 threads vs 5 seats → exactly 5 bookings, 0 over-booking.

See `docs/` for the full documents and `outputs/` for every captured run.

# Foundations of Data Science – workflow (25ITC31)

Script: `python/analysis.py` · Outputs: `outputs/charts/`, `outputs/reports/analysis_report.txt`, `outputs/data/*.csv`

| Unit | Technique | In this project |
|---|---|---|
| 1 Data description | Types of data/variables, averages, variability, distributions | Categorical (route, bus_type, status), numeric (fare, distance, occupancy), temporal (date, hour); mean/median occupancy, IQR; age distribution (chart 11) |
| 2 Python foundations | Functions, dicts/lists, file handling, CSV, libraries | `retrieve()`, `preprocess()`, `charts()`, `insights()`; CSV in/out |
| 3 Data wrangling | NumPy, Pandas Series/DataFrame, selection, groupby, pivot, merge | `groupby`, `pivot_table` (route × weekday), `join` of schedule-level and booking-level frames |
| 4 Cleaning & transformation | Missing values, duplicates, type conversion, outliers, string ops, feature creation, integration | See table below |
| 5 EDA & visualisation | Descriptive stats; Matplotlib line/bar/hist/scatter; Seaborn box/heat/correlation | 11 charts below |

## Preprocessing log (real output)
| Step | Finding | Action |
|---|---|---|
| Retrieval | 5,800 bookings × 19 columns, 231 schedules (PyMySQL) | – |
| Types | Decimals / dates arrive as `Decimal` / `date` | Converted to `float` / `datetime` |
| Duplicates | 0 | `drop_duplicates(booking_id)` |
| Missing | age 328 (5.7 %), e-mail 1,076 (18.6 %) | age → median (33) for age statistics; e-mail → `has_email` flag |
| Text | Place names | strip + Title Case, `route` label |
| Features | – | revenue, is_cancelled, weekday, lead_days, time_slot, fare_per_km, completed-trip flag |
| Outliers | 0 by IQR on fare-per-km per bus type | Kept (fares are set per schedule) |
| Validity | 0 bookings after travel date; 0 cancellations before booking | – |

Only **completed trips** (travel date before today) are used for occupancy statistics so that trips still open for
booking do not distort the averages.

## Charts
| # | File | Shows |
|---|---|---|
| 1 | `01_bookings_by_route.png` | Demand by route |
| 2 | `02_revenue_by_route.png` | Net revenue by route |
| 3 | `03_occupancy_by_bus_type.png` | Occupancy distribution per bus type (box plot) |
| 4 | `04_daily_trend.png` | Daily tickets + 7-day average (line) |
| 5 | `05_route_weekday_heatmap.png` | Route × weekday occupancy (heat map) |
| 6 | `06_cancellation_rate.png` | Cancellation rate per route |
| 7 | `07_fare_vs_distance.png` | Fare vs distance by bus type (scatter) |
| 8 | `08_booking_lead_time.png` | How early passengers book (histogram) |
| 9 | `09_occupancy_by_departure_hour.png` | Occupancy by departure hour |
| 10 | `10_correlation.png` | Correlation matrix |
| 11 | `11_passenger_age.png` | Passenger age profile |

## Findings (from the run on 14-Nov-2026)
* **Madurai → Chennai** is the top route: 959 tickets, Rs. 6.10 lakh net (25.7 % of all revenue).
* Weakest-filled route: **Tiruchirappalli → Coimbatore** (59.7 % average occupancy) → candidate for fewer trips.
* **Fri–Sun** trips are 76.0 % full vs 63.9 % Mon–Thu; Saturday is busiest (78.2 %), Wednesday quietest (63.5 %).
* Cancellation rate 8.2 % overall; highest on Madurai → Tirunelveli (10.1 %).
* Median booking lead time 5 days; distance and occupancy are uncorrelated (r = +0.02) – demand depends on the route
  and weekday, not on how far the route is.
* 7 of 188 completed trips were ≥ 95 % full and 8 were < 40 % full.

> The sample data is synthetic (generated with a fixed seed to follow plausible demand patterns), so these findings
> demonstrate the method; with real booking data the same script produces real insights.

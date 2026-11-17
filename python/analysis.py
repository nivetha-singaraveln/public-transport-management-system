"""
Passenger Travel Analysis  (Python part of Scenario 10)

Pipeline:  MariaDB  ->  pandas (retrieve)  ->  clean / preprocess  ->  EDA  ->  charts + insight report

Usage:  python3 python/analysis.py              (run from the project root)
Env:    DB_HOST DB_PORT DB_USER DB_PASSWORD DB_NAME   (defaults match java/db.properties)
"""
import os, sys, datetime as dt
import pandas as pd
import pymysql
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import seaborn as sns

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CHARTS = os.path.join(ROOT, "outputs", "charts")
REPORTS = os.path.join(ROOT, "outputs", "reports")
DATA = os.path.join(ROOT, "outputs", "data")
for d in (CHARTS, REPORTS, DATA):
    os.makedirs(d, exist_ok=True)

sns.set_theme(style="whitegrid", palette="deep", font_scale=0.95)
LOG = []
def log(msg=""):
    print(msg); LOG.append(msg)

# ---------------------------------------------------------------- 1. RETRIEVE
QUERY = """
SELECT bk.booking_id, bk.schedule_id, bk.seat_no, bk.status, t.booked_at, NULL AS cancelled_at,
       t.fare_paid, t.refund_amount,
       p.passenger_id, p.age, p.email,
       r.source, r.destination, r.distance_km,
       s.travel_date, s.departure_time,
       b.bus_number, b.bus_type, b.capacity
  FROM booking bk
  JOIN ticket t    ON t.booking_id   = bk.booking_id
  JOIN passenger p ON p.passenger_id = bk.passenger_id
  JOIN schedule s  ON s.schedule_id  = bk.schedule_id
  JOIN route r     ON r.route_id     = s.route_id
  JOIN bus b       ON b.bus_id       = s.bus_id
"""
SCHED_QUERY = "SELECT * FROM v_seat_occupancy"

def fetch(conn, sql):
    with conn.cursor() as cur:
        cur.execute(sql)
        cols = [c[0] for c in cur.description]
        return pd.DataFrame(cur.fetchall(), columns=cols)

def retrieve():
    conn = pymysql.connect(
        host=os.getenv("DB_HOST", "127.0.0.1"), port=int(os.getenv("DB_PORT", "3306")),
user=os.getenv("DB_USER", "transport_app"),
password=os.getenv("DB_PASSWORD", "TransportApp2026"),
database=os.getenv("DB_NAME", "transport_management")) 
    try:
        return fetch(conn, QUERY), fetch(conn, SCHED_QUERY)
    finally:
        conn.close()

# ---------------------------------------------------------------- 2. PREPROCESS
def preprocess(raw, sched, today):
    log("=" * 64); log("DATA PREPROCESSING"); log("=" * 64)
    log(f"Retrieved {len(raw):,} booking rows x {raw.shape[1]} cols; {len(sched):,} schedules")

    df = raw.copy()
    # (a) data types
    for c in ("fare_paid", "refund_amount", "distance_km"):
        df[c] = pd.to_numeric(df[c]).astype(float)
    df["travel_date"] = pd.to_datetime(df["travel_date"])
    df["booked_at"] = pd.to_datetime(df["booked_at"])
    df["cancelled_at"] = pd.to_datetime(df["cancelled_at"])
    df["departure_hour"] = df["departure_time"].apply(lambda x: x.seconds // 3600 if hasattr(x, "seconds") else int(str(x)[:2]))
    log("[1] converted decimals to float and date columns to datetime")

    # (b) duplicates
    dup = df.duplicated(subset=["schedule_id", "seat_no", "status", "passenger_id", "booked_at"]).sum()
    df = df.drop_duplicates(subset=["booking_id"])
    log(f"[2] duplicate bookings found: {dup} (booking_id is unique -> none dropped)")

    # (c) missing values
    miss = df[["age", "email"]].isna().sum()
    log(f"[3] missing values: age={miss['age']} ({miss['age']/len(df):.1%}), email={miss['email']} ({miss['email']/len(df):.1%})")
    df["age_known"] = df["age"].notna()
    median_age = df["age"].median()
    df["age_filled"] = df["age"].fillna(median_age)          # median imputation for age-based stats
    df["has_email"] = df["email"].notna()
    log(f"    age imputed with median ({median_age:.0f}); e-mail kept as 'has_email' flag (not needed for analysis)")

    # (d) text normalisation
    for c in ("source", "destination"):
        df[c] = df[c].str.strip().str.title()
    df["route"] = df["source"] + " -> " + df["destination"]
    log("[4] normalised place names (strip + Title Case) and built 'route' label")

    # (e) feature engineering
    df["revenue"] = df["fare_paid"] - df["refund_amount"]
    df["is_cancelled"] = (df["status"] == "CANCELLED").astype(int)
    df["weekday"] = df["travel_date"].dt.day_name()
    df["lead_days"] = (df["travel_date"] - df["booked_at"].dt.normalize()).dt.days
    slot = pd.cut(df["departure_hour"], [-1, 5, 11, 16, 20, 24],
                  labels=["Night (0-5)", "Morning (6-11)", "Afternoon (12-16)", "Evening (17-20)", "Late night (21-23)"])
    df["time_slot"] = slot
    df["fare_per_km"] = df["fare_paid"] / df["distance_km"]
    df["completed"] = df["travel_date"] <= today
    log("[5] derived: revenue, is_cancelled, weekday, lead_days, time_slot, fare_per_km, completed")

    # (f) outliers (IQR on fare_per_km per bus type)
    out = 0
    for bt, g in df.groupby("bus_type"):
        q1, q3 = g["fare_per_km"].quantile([.25, .75]); iqr = q3 - q1
        out += ((g["fare_per_km"] < q1 - 1.5 * iqr) | (g["fare_per_km"] > q3 + 1.5 * iqr)).sum()
    log(f"[6] fare-per-km outliers (IQR rule, per bus type): {out} -> kept (fares are set by schedule, not errors)")
    bad_lead = (df["lead_days"] < 0).sum()
    log(f"[7] validity: bookings made after travel date = {bad_lead}; cancelled before booked = "
        f"{(df['cancelled_at'] < df['booked_at']).sum()}")

    # schedule-level frame
    s = sched.copy()
    s["travel_date"] = pd.to_datetime(s["travel_date"])
    for c in ("occupancy_pct", "capacity", "seats_booked"):
        s[c] = pd.to_numeric(s[c]).astype(float)
    s["route"] = s["source"] + " -> " + s["destination"]
    s["weekday"] = s["travel_date"].dt.day_name()
    s["completed"] = s["travel_date"] <= today
    dist = df.drop_duplicates("schedule_id").set_index("schedule_id")[["distance_km"]]
    fare = df.drop_duplicates("schedule_id").set_index("schedule_id")[["fare_paid"]].rename(columns={"fare_paid": "fare"})
    s = s.join(dist, on="schedule_id").join(fare, on="schedule_id")
    log(f"Clean data: {len(df):,} bookings ({df['completed'].sum():,} on completed trips), {len(s):,} schedules")
    df.to_csv(os.path.join(DATA, "bookings_clean.csv"), index=False)
    s.to_csv(os.path.join(DATA, "schedules_clean.csv"), index=False)
    return df, s

# ---------------------------------------------------------------- 3. CHARTS
WEEK = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
def save(fig, name):
    fig.tight_layout(); fig.savefig(os.path.join(CHARTS, name), dpi=130); plt.close(fig)
    log(f"   saved outputs/charts/{name}")

def charts(df, s):
    log(""); log("=" * 64); log("VISUALISATIONS"); log("=" * 64)
    ok = df[df["status"] == "CONFIRMED"]
    done = ok[ok["completed"]]

    # 1 bookings per route
    d = ok.groupby("route").size().sort_values()
    fig, ax = plt.subplots(figsize=(8, 5))
    bars = ax.barh(d.index, d.values, color=sns.color_palette("Blues", len(d)))
    ax.bar_label(bars, padding=3); ax.set_xlabel("Confirmed tickets"); ax.set_title("Passenger Demand by Route")
    save(fig, "01_bookings_by_route.png")

    # 2 revenue per route
    r = df.groupby("route")["revenue"].sum().sort_values()
    fig, ax = plt.subplots(figsize=(8, 5))
    bars = ax.barh(r.index, r.values / 1e3, color=sns.color_palette("Greens", len(r)))
    ax.bar_label(bars, fmt="%.0fk", padding=3); ax.set_xlabel("Net revenue (Rs. thousand)")
    ax.set_title("Route-wise Net Revenue (incl. cancellation fees)")
    save(fig, "02_revenue_by_route.png")

    # 3 occupancy by bus type (completed trips)
    c = s.copy()
    fig, ax = plt.subplots(figsize=(7, 4.5))
    sns.boxplot(data=c, x="bus_type", y="occupancy_pct", ax=ax, hue="bus_type", legend=False)
    ax.set_xlabel(""); ax.set_ylabel("Seat occupancy (%)"); ax.set_title("Seat Occupancy by Bus Type (completed trips)")
    save(fig, "03_occupancy_by_bus_type.png")

    # 4 daily trend
    t = ok.groupby("travel_date").size().rename("tickets").to_frame()
    t = t[t.index < pd.Timestamp(dt.date.today())]
    t["7-day average"] = t["tickets"].rolling(7).mean()
    fig, ax = plt.subplots(figsize=(9, 4.5))
    ax.plot(t.index, t["tickets"], marker="o", ms=3, lw=1, alpha=.6, label="Tickets per day")
    ax.plot(t.index, t["7-day average"], lw=2.5, color="crimson", label="7-day average")
    ax.set_title("Daily Travel Volume"); ax.set_ylabel("Confirmed tickets"); ax.legend()
    fig.autofmt_xdate(); save(fig, "04_daily_trend.png")

    # 5 route x weekday heatmap (avg occupancy)
    h = c.pivot_table(index="route", columns="weekday", values="occupancy_pct", aggfunc="mean").reindex(columns=WEEK)
    fig, ax = plt.subplots(figsize=(9, 5.5))
    sns.heatmap(h, annot=True, fmt=".0f", cmap="YlOrRd", cbar_kws={"label": "avg occupancy %"}, ax=ax)
    ax.set_title("Average Occupancy: Route x Weekday"); ax.set_xlabel(""); ax.set_ylabel(""); ax.set_xticklabels([w[:3] for w in WEEK])
    save(fig, "05_route_weekday_heatmap.png")

    # 6 cancellation rate by route
    cr = df.groupby("route")["is_cancelled"].mean().mul(100).sort_values()
    fig, ax = plt.subplots(figsize=(8, 5))
    bars = ax.barh(cr.index, cr.values, color="#d9534f"); ax.bar_label(bars, fmt="%.1f%%", padding=3)
    ax.axvline(df["is_cancelled"].mean() * 100, ls="--", color="k", label="overall")
    ax.set_xlabel("Cancellation rate (%)"); ax.set_title("Cancellation Rate by Route"); ax.legend()
    save(fig, "06_cancellation_rate.png")

    # 7 fare vs distance
    f = s.dropna(subset=["fare"])
    fig, ax = plt.subplots(figsize=(8, 5))
    sns.scatterplot(data=f, x="distance_km", y="fare", hue="bus_type", s=45, ax=ax)
    ax.set_xlabel("Route distance (km)"); ax.set_ylabel("Ticket fare (Rs.)"); ax.set_title("Fare vs Distance by Bus Type")
    save(fig, "07_fare_vs_distance.png")

    # 8 booking lead time
    fig, ax = plt.subplots(figsize=(8, 4.5))
    sns.histplot(done["lead_days"], bins=range(0, 12), discrete=True, ax=ax, color="steelblue")
    ax.set_xlabel("Days between booking and travel"); ax.set_title("How Early Do Passengers Book?")
    save(fig, "08_booking_lead_time.png")

    # 9 occupancy by departure slot
    c2 = c.copy(); c2["hour"] = pd.to_datetime(c2["departure_time"].astype(str).str[-8:], format="%H:%M:%S").dt.hour
    o = c2.groupby("hour")["occupancy_pct"].mean()
    fig, ax = plt.subplots(figsize=(8, 4.5))
    ax.bar([f"{h:02d}:00" for h in o.index], o.values, color=sns.color_palette("viridis", len(o)))
    ax.set_ylabel("Avg occupancy (%)"); ax.set_title("Occupancy by Departure Hour")
    save(fig, "09_occupancy_by_departure_hour.png")

    # 10 correlation
    cc = c[["distance_km", "fare", "capacity", "seats_booked", "occupancy_pct"]].corr()
    fig, ax = plt.subplots(figsize=(6, 5))
    sns.heatmap(cc, annot=True, fmt=".2f", cmap="coolwarm", center=0, ax=ax)
    ax.set_title("Correlation (completed trips)")
    save(fig, "10_correlation.png")

    # 11 passenger age distribution
    fig, ax = plt.subplots(figsize=(8, 4.5))
    ages = df.drop_duplicates("passenger_id")["age"].dropna()
    sns.histplot(ages, bins=15, kde=True, ax=ax, color="teal")
    ax.set_xlabel("Passenger age"); ax.set_title(f"Passenger Age Profile (n={len(ages)}, missing values excluded)")
    save(fig, "11_passenger_age.png")

# ---------------------------------------------------------------- 4. INSIGHTS
def insights(df, s):
    log(""); log("=" * 64); log("KEY INSIGHTS"); log("=" * 64)
    ok = df[df["status"] == "CONFIRMED"]
    c = s.copy()
    rev = df.groupby("route")["revenue"].sum().sort_values(ascending=False)
    cnt = ok.groupby("route").size().sort_values(ascending=False)
    occ_route = c.groupby("route")["occupancy_pct"].mean().sort_values(ascending=False)
    log(f"Total bookings          : {len(df):,}  (confirmed {len(ok):,}, cancelled {df['is_cancelled'].sum():,}"
        f" = {df['is_cancelled'].mean():.1%})")
    log(f"Net revenue             : Rs. {df['revenue'].sum():,.0f}")
    log(f"Highest-demand route    : {cnt.index[0]} ({cnt.iloc[0]:,} tickets)" if not cnt.empty else "Highest-demand route    : No confirmed bookings")
    log(f"Top revenue route       : {rev.index[0]} (Rs. {rev.iloc[0]:,.0f}, {rev.iloc[0]/rev.sum():.1%} of total)")
    log(f"Best-filled route       : {occ_route.index[0]} ({occ_route.iloc[0]:.1f}% avg occupancy)")
    log(f"Lowest-demand route     : {cnt.index[-1]} ({cnt.iloc[-1]:,} tickets)" if not cnt.empty else "Lowest-demand route     : No confirmed bookings")
    log(f"Weakest-filled route    : {occ_route.index[-1]} ({occ_route.iloc[-1]:.1f}% avg occupancy) -> candidate for fewer trips")
    wd = c.groupby("weekday")["occupancy_pct"].mean().reindex(WEEK)
    log(f"Busiest weekday         : {wd.idxmax()} ({wd.max():.1f}%), quietest: {wd.idxmin()} ({wd.min():.1f}%)")
    bt = c.groupby("bus_type")["occupancy_pct"].mean().sort_values(ascending=False)
    log("Occupancy by bus type   : " + ", ".join(f"{k} {v:.1f}%" for k, v in bt.items()))
    full = (c["occupancy_pct"] >= 95).sum(); low = (c["occupancy_pct"] < 40).sum()
    log(f"Trips >=95% full        : {full} of {len(c)}  | trips <40% full: {low}")
    cr = df.groupby("route")["is_cancelled"].mean().sort_values(ascending=False)
    log(f"Highest cancellations   : {cr.index[0]} ({cr.iloc[0]:.1%})")
    log(f"Median booking lead time: {df[df['completed']]['lead_days'].median():.0f} days")
    corr = c[["distance_km", "occupancy_pct"]].corr().iloc[0, 1]
    log(f"Correlation distance vs occupancy: {corr:+.2f}")
    log("\nRecommendations:")
    log(f"  1. Add capacity on {cnt.index[0]} - it earns the most and sells the most tickets." if not cnt.empty else "  1. No confirmed bookings yet; collect more booking data before adding capacity.")
    log(f"  2. Review frequency on {occ_route.index[-1]} where buses run emptiest.")
    wk = c[c["weekday"].isin(["Friday", "Saturday", "Sunday"])]["occupancy_pct"].mean()
    wkd = c[~c["weekday"].isin(["Friday", "Saturday", "Sunday"])]["occupancy_pct"].mean()
    if wk > wkd:
        log(f"  3. Fri-Sun trips run {wk:.1f}% full vs {wkd:.1f}% Mon-Thu: add weekend trips / weekend pricing.")
    else:
        log(f"  3. Weekend trips ({wk:.1f}%) are not busier than Mon-Thu ({wkd:.1f}%): no weekend uplift needed.")

if __name__ == "__main__":
    today = pd.Timestamp(dt.date.today())
    try:
        raw, sched = retrieve()
    except pymysql.MySQLError as e:
        sys.exit(f"Cannot connect to database: {e}\nStart MariaDB and load database/*.sql first (see README).")
    df, s = preprocess(raw, sched, today)
    charts(df, s)
    insights(df, s)
    with open(os.path.join(REPORTS, "analysis_report.txt"), "w") as fh:
        fh.write("\n".join(LOG) + "\n")
    print("\nDone. Charts in outputs/charts/, report in outputs/reports/analysis_report.txt")

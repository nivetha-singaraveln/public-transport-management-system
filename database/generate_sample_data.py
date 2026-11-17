"""Generates database/sample_data.sql deterministically (seed fixed).
Run:  python3 database/generate_sample_data.py
"""
import random, datetime as dt
random.seed(10)  # Scenario 10

BUSES = [  # number, capacity, type
 ("TN58-AC-1001",30,"AC_SLEEPER"),("TN58-AC-1002",30,"AC_SLEEPER"),
 ("TN58-AS-2001",40,"AC_SEATER"),("TN58-AS-2002",40,"AC_SEATER"),("TN58-AS-2003",40,"AC_SEATER"),
 ("TN58-NA-3001",50,"NON_AC_SEATER"),("TN58-NA-3002",50,"NON_AC_SEATER"),
 ("TN58-NA-3003",50,"NON_AC_SEATER"),("TN58-NA-3004",45,"NON_AC_SEATER"),
 ("TN58-DX-4001",36,"DELUXE"),("TN58-DX-4002",36,"DELUXE"),("TN58-DX-4003",36,"DELUXE"),
]
# source, destination, km, demand weight
ROUTES = [
 ("Madurai","Chennai",462,1.00),("Madurai","Coimbatore",215,0.80),
 ("Madurai","Tiruchirappalli",135,0.70),("Madurai","Tirunelveli",160,0.55),
 ("Madurai","Bengaluru",435,0.60),("Chennai","Coimbatore",505,0.65),
 ("Chennai","Tiruchirappalli",330,0.75),("Coimbatore","Bengaluru",365,0.50),
 ("Madurai","Rameswaram",170,0.35),("Tiruchirappalli","Coimbatore",210,0.40),
]
RATE = {"AC_SLEEPER":2.1,"AC_SEATER":1.6,"NON_AC_SEATER":1.0,"DELUXE":1.3}
TIMES = ["05:30","07:00","09:30","13:00","18:30","21:00","22:30"]
FIRST=["Arun","Nivetha","Karthik","Divya","Suresh","Priya","Vignesh","Lakshmi","Manoj","Anitha","Ravi","Meena",
"Dinesh","Kavya","Harish","Swathi","Prakash","Janani","Gokul","Revathi","Sathish","Pooja","Naveen","Deepa","Bala","Keerthi"]
LAST=["Kumar","Raj","Selvam","Devi","Murugan","Lakshmi","Pandian","Subramanian","Krishnan","Iyer","Nair","Ganesan","Rajan","Sundaram"]

out=[]
def q(v): return "NULL" if v is None else ("'%s'"%v.replace("'","''") if isinstance(v,str) else str(v))
out.append("USE transport_db;\nSET FOREIGN_KEY_CHECKS=1;")
for i,(n,c,t) in enumerate(BUSES,1):
    out.append(f"INSERT INTO bus VALUES ({i},{q(n)},{c},{q(t)});")
for i,(s,d,k,_) in enumerate(ROUTES,1):
    out.append(f"INSERT INTO route VALUES ({i},{q(s)},{q(d)},{k});")

# passengers
pax=[]; phones=set()
for i in range(1,321):
    name=f"{random.choice(FIRST)} {random.choice(LAST)}"
    while True:
        ph="9"+"".join(random.choice("0123456789") for _ in range(9))
        if ph not in phones: phones.add(ph); break
    email = None if random.random()<0.18 else name.lower().replace(" ",".")+str(i)+"@example.com"
    age = None if random.random()<0.07 else int(min(80,max(12,random.gauss(33,12))))
    pax.append(i)
    out.append(f"INSERT INTO passenger VALUES ({i},{q(name)},{q(ph)},{q(email)},{q(age)});")

# schedules: 2026-10-15 .. 2026-11-13 (history) + 2026-11-15..20 (open for booking)
sched=[]; used=set(); sid=0
start=dt.date(2026,10,15)
days=[start+dt.timedelta(d) for d in range(30)] + [dt.date(2026,11,15)+dt.timedelta(d) for d in range(6)]
for day in days:
    for rid,(s,d,k,w) in enumerate(ROUTES,1):
        trips = 1 + (random.random() < 0.3*w)
        for _ in range(int(trips)):
            if random.random()>0.30+0.35*w: continue
            for _try in range(20):
                bid=random.randint(1,len(BUSES)); tm=random.choice(TIMES)
                if (bid,day,tm) not in used: used.add((bid,day,tm)); break
            else: continue
            btype=BUSES[bid-1][2]
            fare=round(k*RATE[btype]*random.uniform(0.95,1.08)/5)*5
            fare=max(fare,60)
            sid+=1; sched.append((sid,bid,rid,day,tm,fare))
            out.append(f"INSERT INTO schedule VALUES ({sid},{bid},{rid},'{day}','{tm}:00',{fare});")

# bookings + tickets (history: heavy; future: light)
bid_=0; today=dt.date(2026,11,14)
for (sid,bus,rid,day,tm,fare) in sched:
    cap=BUSES[bus-1][1]; w=ROUTES[rid-1][3]
    wk = 1.18 if day.weekday() in (4,5,6) else 1.0
    if day>today: occ=random.uniform(0.02,0.25)
    else: occ=min(1.0,max(0.1,random.gauss(0.45+0.4*w,0.17)*wk))
    n=int(cap*occ)
    seats=random.sample(range(1,cap+1),n)
    for seat in seats:
        bid_+=1; p=random.choice(pax)
        lead=random.randint(0,9) if day<=today else random.randint(0,3)
        bt=dt.datetime.combine(day,dt.time(0,0))-dt.timedelta(days=lead)+dt.timedelta(minutes=random.choice(range(420,1380)))
        if bt>dt.datetime(2026,11,14,12,0): bt=dt.datetime(2026,11,14,random.randint(6,11),random.randint(0,59))
        cancel = day>today and False or (random.random()<0.08)
        if cancel:
            ca=bt+dt.timedelta(hours=random.randint(2,48))
            if ca>dt.datetime.combine(day,dt.time(0,0)): ca=dt.datetime.combine(day,dt.time(0,0))-dt.timedelta(hours=3)
            if ca<bt: ca=bt+dt.timedelta(hours=1)
            out.append(f"INSERT INTO booking (booking_id,schedule_id,passenger_id,seat_no,status,booked_at,cancelled_at) VALUES ({bid_},{sid},{p},{seat},'CANCELLED','{bt}','{ca}');")
            refund=round(fare*0.8,2)
        else:
            out.append(f"INSERT INTO booking (booking_id,schedule_id,passenger_id,seat_no,status,booked_at) VALUES ({bid_},{sid},{p},{seat},'CONFIRMED','{bt}');")
            refund=0
        out.append(f"INSERT INTO ticket (booking_id,ticket_code,fare_paid,refund_amount,issued_at) VALUES ({bid_},'TKT{bid_:06d}',{fare},{refund},'{bt}');")

open("database/sample_data.sql","w").write("\n".join(out)+"\n")
print("schedules",len(sched),"bookings",bid_)

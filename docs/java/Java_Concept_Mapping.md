# Java Programming – syllabus-to-code mapping (25ITT32)

| Unit | Concept | Where it is used |
|---|---|---|
| 1 Intro | Classes, objects, constructors, access control, static, console I/O | `model/*`, `Main` (Scanner), `DBConnection` (static init, private ctor) |
| 2 Inheritance, packages, interfaces | `abstract class Entity` → `Bus`, `Route`, `Schedule`, `Passenger` (super, overriding `describe()`, dynamic dispatch via `toString`); `interface CrudDao<T,ID>` implemented by 4 DAOs; 7 packages | `model`, `dao` |
| 3 Exceptions & multithreading | Custom checked hierarchy `TransportException` → `SeatUnavailableException`, `BusFullException`, `BookingNotFoundException`, `ValidationException`; try/catch/finally via try-with-resources; multi-catch; rethrow after rollback. Threads: `ExecutorService`, `CountDownLatch`, `AtomicInteger`, `Future` – 15 threads race for 5 seats | `exception/*`, `BookingService`, `ConcurrencyDemo` |
| 4 Strings & DB connection | `StringBuilder` (tables, tickets, dynamic SQL), `String.format`, `split/replaceAll/toUpperCase`, regex `Pattern` validation, `String.valueOf`; JDBC (`DriverManager`, `PreparedStatement`, `ResultSet`, transactions `commit/rollback`, generated keys) | `util/Validator`, `TablePrinter`, `TicketPrinter`, `dao/*` |
| 5 Generics, collections, lambdas, streams | Generic interface `CrudDao<T,ID>`; `List`, `Map`, `Set`, `SortedSet(TreeSet)`, `Optional`; lambdas/method references; Stream API: `filter/map/sorted/limit/collect`, `Collectors.groupingBy/averagingDouble`, `reduce`; `record` types | `ReportService`, `BookingService`, `DemoRunner` |

**JPA note.** Unit 4 also lists JPA. The project uses plain JDBC because it makes the transaction/locking behaviour
(`SELECT … FOR UPDATE`) explicit and visible, which is the heart of requirement 5. The DAO layer isolates SQL, so
a JPA `EntityManager` implementation of `CrudDao` could replace it without touching services or UI.

## Package structure
```
com.vcet.transport
 ├─ model      Entity, Bus, BusType, Route, Schedule, Passenger, ScheduleInfo, BookingRow, RouteRevenue, OccupancyRow, CancellationResult
 ├─ exception  TransportException + 4 subclasses
 ├─ db         DBConnection
 ├─ dao        CrudDao<T,ID>, BusDao, RouteDao, ScheduleDao, PassengerDao, BookingDao
 ├─ service    MasterDataService, BookingService, ReportService, ConcurrencyDemo
 ├─ util       Validator, TablePrinter, TicketPrinter, CsvExporter, SqlErrors
 └─ ui         Main, DemoRunner
```

# Round Robin Load Balancing Using Spring Boot Microservices

## 1. Introduction

This project demonstrates **Round Robin load balancing** with Spring Boot. A **Driver Service** receives HTTP requests from a client and distributes them one by one across **three independent microservices**. The implementation is a simple custom Round Robin built on `AtomicInteger`, so it is easy to read, run and explain.

## 2. Objectives

- Implement the Round Robin algorithm without any heavy framework.
- Distribute requests equally across three microservices.
- Show which microservice handled every request (in the API response and in the console).
- Return request numbers and timestamps in every response.
- Handle the case where a microservice is down.
- Keep the code beginner-friendly.

## 3. Architecture

```
                      ┌──────────────────────────────┐
   Client / Postman   │        Driver Service        │
  ─────────────────►  │          Port 8080           │
 GET /driver/request  │  AtomicInteger + Round Robin │
                      └──────┬──────────┬─────────┬──┘
                             │          │         │
              request 1,4,7..│ 2,5,8..  │  3,6,9..│
                             ▼          ▼         ▼
                     ┌────────────┐ ┌────────────┐ ┌────────────┐
                     │Microservice│ │Microservice│ │Microservice│
                     │     1      │ │     2      │ │     3      │
                     │ Port 8081  │ │ Port 8082  │ │ Port 8083  │
                     │ GET/process│ │ GET/process│ │ GET/process│
                     └────────────┘ └────────────┘ └────────────┘
```

### Working flow

1. The client sends `GET /driver/request` to the Driver Service.
2. The Driver Service increments its request counter.
3. The Round Robin algorithm selects the next microservice.
4. The request is forwarded to `GET /process` on that microservice.
5. The microservice returns its name, port and a message.
6. The Driver Service wraps this in its own response (request number, selected service, timestamp).
7. The response is sent back to the client.

## 4. Technologies Used

| Technology | Purpose |
|---|---|
| Java 17 (21 also works) | Language |
| Spring Boot 3.3.5 | Application framework |
| Spring Web | REST controllers and `RestTemplate` |
| Maven | Build tool |
| `AtomicInteger` | Thread-safe request counter |
| Postman | API testing |

## 5. Folder Structure

```
round-robin-load-balancer/
├── driver-service/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/driver/
│       │   ├── DriverServiceApplication.java
│       │   ├── config/AppConfig.java              (RestTemplate + timeouts)
│       │   ├── controller/DriverController.java   (GET /driver/request)
│       │   └── service/
│       │       ├── RoundRobinService.java         (Round Robin logic)
│       │       └── ForwardingService.java         (calls service, error handling)
│       └── resources/application.properties
├── microservice-1/   (port 8081)
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/service1/
│       │   ├── Service1Application.java
│       │   └── controller/ProcessController.java
│       └── resources/application.properties
├── microservice-2/   (port 8082)  - same layout, package com.example.service2
├── microservice-3/   (port 8083)  - same layout, package com.example.service3
└── README.md
```

## 6. Round Robin Algorithm Explained

Round Robin hands out work in a fixed, repeating order: 1 → 2 → 3 → 1 → 2 → 3 ...

The Driver Service keeps one counter:

```java
private final AtomicInteger requestCounter = new AtomicInteger(0);

public Selection next() {
    int requestNumber = requestCounter.incrementAndGet();          // 1, 2, 3, 4, ...
    int index = Math.floorMod(requestNumber - 1, serviceUrls.size()); // 0, 1, 2, 0, ...
    return new Selection(requestNumber, index + 1, serviceUrls.get(index));
}
```

| Request number | `(n - 1) % 3` | Selected service |
|---|---|---|
| 1 | 0 | Microservice-1 |
| 2 | 1 | Microservice-2 |
| 3 | 2 | Microservice-3 |
| 4 | 0 | Microservice-1 |
| 5 | 1 | Microservice-2 |
| 6 | 2 | Microservice-3 |

**Why `AtomicInteger`?** Many clients may call the driver at the same moment. A normal `int++` is not thread-safe and could give two requests the same number. `incrementAndGet()` is atomic, so every request gets a unique number and the rotation stays exact. `Math.floorMod` keeps the index valid even if the counter ever overflows.

## 7. How to Run

**Prerequisites:** Java 17+ and Maven 3.6+ installed.

Open **four terminals**, one per service (start the three microservices first, the driver last):

```bash
# Terminal 1
cd microservice-1 && mvn spring-boot:run

# Terminal 2
cd microservice-2 && mvn spring-boot:run

# Terminal 3
cd microservice-3 && mvn spring-boot:run

# Terminal 4
cd driver-service && mvn spring-boot:run
```

Ports used: Driver 8080, Microservice-1 8081, Microservice-2 8082, Microservice-3 8083.

Quick check from a terminal:

```bash
curl http://localhost:8081/process
curl http://localhost:8080/driver/request
```

## 8. API Documentation

### Driver Service

`GET http://localhost:8080/driver/request`

Success response (`200 OK`):

```json
{
  "requestNumber": 1,
  "selectedService": "Microservice-1",
  "port": 8081,
  "message": "Request successfully processed",
  "serviceResponse": {
    "serviceName": "Microservice-1",
    "port": 8081,
    "message": "Request processed by Microservice-1",
    "timestamp": "2026-11-17T21:30:05.123"
  },
  "timestamp": "2026-11-17T21:30:05.140"
}
```

Error response when the selected service is down (`503 Service Unavailable`):

```json
{
  "requestNumber": 2,
  "selectedService": "Microservice-2",
  "port": 8082,
  "message": "Microservice-2 is currently unavailable. Please try again.",
  "error": "SERVICE_UNAVAILABLE",
  "timestamp": "2026-11-17T21:31:10.512"
}
```

If the service responds with an error status, the driver returns `502 Bad Gateway` with `"error": "BAD_GATEWAY"`.

### Microservices (8081, 8082, 8083)

`GET http://localhost:808X/process`

```json
{
  "serviceName": "Microservice-1",
  "port": 8081,
  "message": "Request processed by Microservice-1",
  "timestamp": "2026-11-17T21:30:05.123"
}
```

## 9. Testing with Postman

1. Start all four applications.
2. Open Postman and create a new request: **GET** `http://localhost:8080/driver/request`.
3. Click **Send** six times in a row.
4. Check `requestNumber` and `selectedService` in each response.

Expected result:

| Request | Selected Service | Port |
|---|---|---|
| 1 | Microservice-1 | 8081 |
| 2 | Microservice-2 | 8082 |
| 3 | Microservice-3 | 8083 |
| 4 | Microservice-1 | 8081 |
| 5 | Microservice-2 | 8082 |
| 6 | Microservice-3 | 8083 |

**Tip:** In Postman use *Collection Runner* with 6 (or 12) iterations to send them automatically.

**Test the error handling:** stop Microservice-2 (Ctrl+C in its terminal) and send the request again. Requests that land on Microservice-2 return `503`, while Microservice-1 and Microservice-3 keep working. Restart it and the rotation continues normally.

> The counter lives in memory. Restarting the Driver Service resets request numbering to 1.

## 10. Sample Console Output

**Driver Service console** after six requests:

```
[Driver] Request #1 -> Microservice-1 (http://localhost:8081)
[Driver] Request #2 -> Microservice-2 (http://localhost:8082)
[Driver] Request #3 -> Microservice-3 (http://localhost:8083)
[Driver] Request #4 -> Microservice-1 (http://localhost:8081)
[Driver] Request #5 -> Microservice-2 (http://localhost:8082)
[Driver] Request #6 -> Microservice-3 (http://localhost:8083)
```

**Microservice consoles:**

```
# microservice-1 (8081)
[Microservice-1] Request received on port 8081
[Microservice-1] Request received on port 8081

# microservice-2 (8082)
[Microservice-2] Request received on port 8082
[Microservice-2] Request received on port 8082

# microservice-3 (8083)
[Microservice-3] Request received on port 8083
[Microservice-3] Request received on port 8083
```

**When Microservice-2 is stopped:**

```
[Driver] Request #1 -> Microservice-1 (http://localhost:8081)
[Driver] Request #2 -> Microservice-2 (http://localhost:8082)
[Driver] Request #2 FAILED: Microservice-2 is unavailable (I/O error on GET request for "http://localhost:8082/process": Connection refused)
[Driver] Request #3 -> Microservice-3 (http://localhost:8083)
```

## 11. Configuration

All driver settings are in `driver-service/src/main/resources/application.properties`:

```properties
server.port=8080
driver.services.urls=http://localhost:8081,http://localhost:8082,http://localhost:8083
driver.http.connect-timeout-ms=2000
driver.http.read-timeout-ms=5000
```

To add a fourth service, start it on another port and append its URL to `driver.services.urls`; Round Robin adapts automatically.

## 12. Possible Improvements

- Skip unavailable services (health checks or automatic failover to the next service).
- Weighted Round Robin for servers with different capacity.
- Spring Cloud LoadBalancer with a service registry such as Eureka.
- Docker Compose to start all four services with one command.

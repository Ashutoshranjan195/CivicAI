# CivicAI - Smart City Microservices Platform (2025)

CivicAI is a production-ready, event-driven smart city backend architecture built on **Spring Boot 3**, **Spring Cloud**, **Apache Kafka**, **PostgreSQL**, **Docker**, and **Resilience4J**. It models city IoT sensors, citizen issues reporting, and real-time municipal alert dispatches under strict microservice guidelines (database-per-service, centralized configurations, fault-tolerance, and distributed observability).

---

## 🏗️ Architecture Design

```
                     +----------------------------+
                     |    Client / Postman        |
                     +--------------+-------------+
                                    |
                                    | [HTTP Requests with API Key]
                                    v
                     +--------------+-------------+
                     |   API Gateway (Port 8080)   | <---+
                     +--------------+-------------+     |
                                    |                   |
        +---------------------------+-------------------+
        | (Route Routing)           | (Route Routing)   | [Registers with]
        v                           v                   |
+-------+--------------+    +-------+--------------+    |    +----------------------+
|   Citizen Service    |    |    Sensor Service    |----+--->|    Eureka Server     |
|    (Port 8081)       |    |     (Port 8082)      |    |    |     (Port 8761)      |
+-------+--------------+    +-------+--------------+    |    +----------------------+
        |                           |                   |
        | [Rest Call with           | [Publishes        |
        |  Circuit Breaker]         |  Kafka Events]    |    +----------------------+
        |                           v                   |    |    Config Server     |
        |                    +------+-------+           +--->|     (Port 8888)      |
        |                    | Apache Kafka |                +----------+-----------+
        |                    +------+-------+                           |
        |                           |                                   | [Pulls config]
        v                           v [Asynchronously                   |
+-------+--------------+    +-------+--------------+                    |
|   postgres-citizen   |    | Notification Service | <------------------+
|    (Port 5432)       |    |     (Port 8083)      |
+----------------------+    +----------------------+
```

### Microservices Directory
1. **[Config Server](file:///C:/Users/ashutosh/.gemini/antigravity/scratch/civic-ai/config-server)** (Port 8888): Manages environment properties dynamically using the native configuration repository [config-repo](file:///C:/Users/ashutosh/.gemini/antigravity/scratch/civic-ai/config-repo).
2. **[Eureka Server](file:///C:/Users/ashutosh/.gemini/antigravity/scratch/civic-ai/eureka-server)** (Port 8761): Acts as service discovery to dynamic lookup service instances.
3. **[API Gateway](file:///C:/Users/ashutosh/.gemini/antigravity/scratch/civic-ai/api-gateway)** (Port 8080): Handles client routing, API Key Authentication, and rate-limiting (100 req/sec limit per client IP).
4. **[Citizen Service](file:///C:/Users/ashutosh/.gemini/antigravity/scratch/civic-ai/citizen-service)** (Port 8081): Manages public complaints and reports database. Triggers fault-tolerant circuit breakers on REST interactions.
5. **[Sensor Service](file:///C:/Users/ashutosh/.gemini/antigravity/scratch/civic-ai/sensor-service)** (Port 8082): Interacts with smart city IoT sensors. Publishes sensor readings to event-driven Kafka topics.
6. **[Notification Service](file:///C:/Users/ashutosh/.gemini/antigravity/scratch/civic-ai/notification-service)** (Port 8083): Asynchronously consumes Kafka events, applies idempotency checks, and retries failures using exponential backoff before sending them to the Dead Letter Queue (DLQ).

---

## 🛠️ Getting Started (Local Setup)

### Prerequisites
* Java 17 installed
* Maven installed
* Docker & Docker Compose installed

### Step 1: Build the Codebase
Build the entire parent project from the root folder:
```bash
mvn clean package -DskipTests
```

### Step 2: Start All Infrastructure & Containers
Start everything via docker-compose:
```bash
docker-compose up -d --build
```
This command spins up:
* Databases (`postgres-citizen` on 5432, `postgres-sensor` on 5433)
* Messaging Broker (`zookeeper` and `kafka` on 9092)
* Monitoring Dashboards (`AKHQ` on 8085, `Zipkin` on 9411)
* Spring Cloud Platforms (`config-server`, `eureka-server`, `api-gateway`)
* Core microservices (`citizen-service`, `sensor-service`, `notification-service`)

### Step 3: Verify Startup Status
Ensure all services register themselves on the Eureka Discovery dashboard:
👉 Visit: **[http://localhost:8761](http://localhost:8761)**

---

## 🧪 Testing and Verification Workflows

All APIs passing through the Gateway require the HTTP Header `X-API-Key: CivicAISecretKey2025`.

### 1. Register and List Citizen Complaints (Citizen Service)
* **Create a complaint**:
  ```bash
  curl -X POST http://localhost:8080/api/citizens/complaints \
    -H "X-API-Key: CivicAISecretKey2025" \
    -H "Content-Type: application/json" \
    -d '{
      "title": "Broken Street Light",
      "description": "The street light in Sector 5 is flickering and needs replacement.",
      "citizenEmail": "citizen@smartcity.org"
    }'
  ```
* **Fetch all complaints**:
  ```bash
  curl -X GET http://localhost:8080/api/citizens/complaints \
    -H "X-API-Key: CivicAISecretKey2025"
  ```

### 2. Simulate Sensor Event (Sensor Service & Kafka Event Hub)
Publish a mock air quality reading event:
```bash
curl -X POST http://localhost:8080/api/sensors/simulate \
  -H "X-API-Key: CivicAISecretKey2025" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "EPA Station 4",
    "location": "Times Square",
    "type": "AIR_QUALITY",
    "value": 140.50
  }'
```
* **Verify processing logs**:
  Check the docker logs for `notification-service`:
  ```bash
  docker logs -f notification-service
  ```
  You will see:
  `🚨 [ALERT] High Pollutant Count detected at Times Square! Air Quality Value: 140.5`

### 3. Verify Kafka Retry and Dead Letter Topic (DLQ)
We built a fail simulation mapping into the consumer. If a sensor's location matches `"FAIL_SIMULATION"`, the consumer throws an exception.
Run this mock request:
```bash
curl -X POST http://localhost:8080/api/sensors/simulate \
  -H "X-API-Key: CivicAISecretKey2025" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Broken Sensor Sim",
    "location": "FAIL_SIMULATION",
    "type": "TRAFFIC",
    "value": 300.00
  }'
```
* Observe `notification-service` logs: The service will retry processing 3 times (applying exponential backoff intervals of 1s, 2s, 4s) before routing the message to the DLQ.
* Verify DLQ routing in **AKHQ Kafka UI**:
  👉 Visit: **[http://localhost:8085/ui/docker-kafka/topic/city-sensors.dlq/messages](http://localhost:8085/ui/docker-kafka/topic/city-sensors.dlq/messages)** to view the dead-lettered message.

### 4. Verify Resilience4J Circuit Breaker
* Get sensor service status from Citizen Service (runs via RestTemplate):
  ```bash
  curl -X GET http://localhost:8080/api/citizens/sensors/status \
    -H "X-API-Key: CivicAISecretKey2025"
  ```
  *Response (Success)*: `{"status": "UP", "service": "Sensor Service" ...}`
* Stop the Sensor Service:
  ```bash
  docker-compose stop sensor-service
  ```
* Call the status endpoint again:
  ```bash
  curl -X GET http://localhost:8080/api/citizens/sensors/status \
    -H "X-API-Key: CivicAISecretKey2025"
  ```
  *Response (Fallback)*:
  ```json
  {
    "status": "TEMPORARILY_UNAVAILABLE",
    "message": "Sensor service is not reachable. This is a fallback response from Citizen Service.",
    "circuitBreakerTriggered": true
  }
  ```

---

## 💡 Senior-Level Architecture & Implementation Notes

### 1. Database per Service
* **Rationale**: Giving each service its own dedicated schema eliminates tight database-level coupling. Teams can update schemas independently, scale storage differently (e.g. TimescaleDB/NoSQL for timeseries sensor readings, relational PostgreSQL for transactional complaints), and avoid a single point of data store failure.
* **Consistency Trade-off**: Strong ACID transactions across service boundaries are lost.
  - *Mitigation*: We apply **Eventual Consistency** patterns. To query aggregated views across services, instead of running SQL joins, we publish state change events to Kafka and construct materialized query views (CQRS pattern) or ingest data into a central reporting store.

### 2. Kafka Idempotent Processing
* **Rationale**: Distributed networks guarantee *at-least-once* delivery, making duplicate events inevitable.
* **Mitigation**: The consumer tracks incoming message IDs in a thread-safe caching filter. In enterprise environments, this maps to a Redis caching filter with TTL or an idempotent database unique constraint check (`INSERT INTO processed_events ... ON CONFLICT DO NOTHING`).

### 3. Centralized Configurations & Security
* **Rationale**: Hardcoding passwords and bootstrap links in property files is a security hazard.
* **Production Recommendation**: Config Server native file profile is used here for simplicity. In production:
  - Spring Cloud Config Server should connect to a private Git repository with HTTPS.
  - Secrets (passwords, tokens) must be encrypted via Spring Cloud CLI or resolved dynamically using a secret engine vault like **HashiCorp Vault** or **AWS Secrets Manager**.

### 4. Production Observability
* Distributed tracing tracks client transactions from the API Gateway across multiple threads and network interfaces down to databases and messaging brokers.
* Every microservice exports micrometer spans to **Zipkin** (accessible at **[http://localhost:9411](http://localhost:9411)**) for analysis.

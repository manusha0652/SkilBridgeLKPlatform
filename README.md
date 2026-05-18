# SkillBridge Platform

An enterprise-grade, escrow-driven freelance marketplace designed to bridge the communication gap between non-technical retail shop owners (Clients) and Software Engineers (Freelancers). The system translates operational business needs into concrete, verifiable feature requirements.

---

## 🚀 Architectural Blueprint

The application is built around a **Stateless Multi-Tenant SaaS Pattern**, optimizing scale profiles and securing strict transactional data integrity.

* **Presentation Layer:** Form-driven Requirement Wizard UI translating non-technical requests into feature arrays.
* **Core Engine:** Spring Boot 3.x (Java 17 LTS) operating as a stateless API processor.
* **Persistence Layer:** PostgreSQL 15 managed via Hibernate ORM and Spring Data JPA.
* **Environment Isolation:** Containerized data layer powered by Docker Compose.

---

## 🛠️ Tech Stack & Dependencies

* **Backend Framework:** Spring Boot v4.0.6
* **Language Runtime:** Java 17 LTS (OpenJDK 17.0.11)
* **Database Engine:** PostgreSQL 15.18-Alpine
* **Connection Pool:** HikariCP
* **Build Automation:** Maven Wrapper (`mvnw`)

---

## 📊 Database Domain Models

The relational schema relies on strict foreign key constraints and ACID-compliant transactional tracking:

* **Users:** Manages authentication (BCrypt hashes) and system access controls split strictly into `CLIENT` or `FREELANCER` roles.
* **Job Requests:** Holds client postings, calculated budget parameters in LKR, and structured functional requirements.
* **Bids:** A many-to-many junction entity tracking developer proposals, custom project rates, and coverage terms pitched for open requests.

### Escrow State Machine Lifecycle
The operational status of contract lifecycles transitions through the strict, unidirectional pipeline defined in our core `OrderStatus` state machine:
`PENDING` ──► `IN_PROGRESS` (Funds Escrowed) ──► `DELIVERED` ──► `COMPLETED` (Payout Released)

---

## 🏁 Getting Started (Local Development Setup)

Follow these steps to launch the system environment locally on your machine.

### 1. Prerequisites
Ensure you have the following installed on your machine:
* **Docker Desktop**
* **Java 17 Development Kit (JDK)**

### 2. Launch the Isolated Database Infrastructure
Spin up the database container inside your root directory containing the `docker-compose.yml` file:
```bash
docker compose up -d

# Event Sourcing Workshop

## Setup
Slices are defined as packages in the root package (as specified in the generator).

Find the Swagger UI here:
http://localhost:8080/swagger-ui/index.html

You can run the Code Generator with Claude Code.

Use these Skills.

## Starting the application
To start the service, the `ApplicationStarter` class in `src/test/java` can be used. Why is it located in test?

This class starts the complete environment (including Postgres and, if necessary, Kafka via TestContainers).

## Package structure

Events are located in the events package.

Aggregates are located in the domain package.

Each slice has its own isolated package.

The common package contains several interfaces for the general structure.

## Tech stack

- Java 25
- Spring Boot 3.5
- Axon Framework 4.12
- Spring Modulith 1.4
- PostgreSQL
- Flyway (migrations)
- Testcontainers

---

## Workshop Requirements

### 1. Hardware & OS
- Laptop with macOS, Linux, or Windows
- Stable internet connection
- Admin rights to install tools (or install everything before the workshop)

### 2. Java / Backend Stack

**Java**
- Install JDK 25 via SDKMAN (macOS/Linux):
  ```
  curl -s "https://get.sdkman.io" | bash
  sdk install java 25.0.2-tem
  ```
- Windows: download Temurin 25 from https://adoptium.net/temurin/releases/ or use `winget`:
  ```
  winget install EclipseAdoptium.Temurin.25.JDK
  ```
- Verify: `java -version` — expected: Java 25.x

**Maven**
- No separate installation required
- Verify: `./mvnw -version` (or `mvn` if you have Maven installed)

**IDE (Strongly Recommended)**
- IntelliJ IDEA (Community Edition is fine)
- Enable: Maven support, Spring support

### 3. Spring & Axon
Axon-specific knowledge is not required. We'll introduce:
- Aggregates
- Commands
- Events
- Projections
- Event Store usage

### 4. Frontend Stack (React)
- Install Node.js 18+ via nvm (macOS/Linux):
  ```
  curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.1/install.sh | bash
  nvm install 18
  ```
- Windows: download the installer from https://github.com/coreybutler/nvm-windows/releases, then:
  ```
  nvm install 18
  nvm use 18
  ```
- Verify: `node -v` and `npm -v`

### 5. Git & Repository Access
- Install Git
- Verify: `git --version`
- Make sure you can: clone repositories, create branches, commit changes
- Repository access details will be shared before the workshop

### 6. Pre-Workshop Sanity Check (Important)
Before the workshop, please make sure you can:
1. Clone this repository
2. Build the app via Maven: `./mvnw clean package`

### 7. Optional (But Highly Recommended)
- Docker installed (for local experiments)
- Familiarity with:
  - Event-driven architecture concepts
  - CQRS basics (commands vs. queries)
- Headphones (for focused working phases)

### 8. What You Don't Need
You do not need to prepare:
- Deep Axon internals
- Advanced Spring configuration
- Production-grade security
- Infrastructure setup

We'll focus on modeling, event flows, and decision-making — not framework trivia.

# Slicing Workshop — Spring / Java

An event-sourced Java/Spring Boot project built with [Axon Framework](https://www.axoniq.io/axon-framework). Slices are designed in [EventModelers](https://eventmodelers.io) and implemented autonomously by the **Ralph** AI agent loop.

---

## Prerequisites

- JDK 25 (via SDKMAN: `sdk install java 25.0.2-tem` or [Temurin 25](https://adoptium.net/temurin/releases/))
- Docker (for TestContainers — started automatically)
- [Claude Code CLI](https://docs.anthropic.com/en/docs/claude-code) (`claude`) — required by the agent loop

---

## Setup

### 1. Build the project

```bash
./mvnw clean package -DskipTests
```

### 2. Start the application

Use the `ApplicationStarter` class in `src/test/java`. It starts the full environment (PostgreSQL and, if needed, Kafka) via TestContainers automatically — no manual Docker setup required.

Alternatively, run via Maven:

```bash
./mvnw spring-boot:run
```

The server starts on `http://localhost:8080`. Swagger UI is available at `http://localhost:8080/swagger-ui/index.html`.

---

## Testing

```bash
./mvnw test
```

Run tests for a specific slice only:

```bash
./mvnw test -Dtest="<SliceName>*"
```

Example:

```bash
./mvnw test -Dtest="CreateCatalogEntry*"
```

---

## Project Structure

```
src/main/java/de/eventmodelers/
├── common/                          # Shared interfaces (Command, Event, Query, etc.)
├── domain/                          # Aggregates and Commands
│   ├── <Aggregate>Aggregate.java
│   └── commands/
│       └── <CommandName>Command.java
├── events/                          # All events (shared across contexts)
│   └── <EventName>Event.java
└── <context>/                       # Bounded context module
    ├── ProcessingGroups.java
    └── slices/
        └── <slicename>/
            ├── <SliceName>Controller.java
            ├── <SliceName>ReadModel.java
            ├── <SliceName>ReadModelQuery.java
            └── internal/
                ├── <SliceName>ReadModelProjector.java
                ├── <SliceName>ReadModelQueryHandler.java
                ├── <SliceName>ReadModelRepository.java
                └── <SliceName>Processor.java
.slices/                             # Slice definitions exported from EventModelers
└── <Context>/
    ├── index.json                   # Slice list with status and priority
    └── <slicefolder>/
        └── slice.json               # Full slice definition (events, commands, specs)
config.json                          # Slice definitions used by the code generator
```

---

## Slice Types

| Type | Triggered by | Key files |
|---|---|---|
| **State Change** | HTTP command (POST) | `*Command.java`, `*Aggregate.java`, `*Controller.java` |
| **State View** | Events → read model (GET) | `*ReadModel.java`, `*Projector.java`, `*Controller.java`, migration |
| **Automation** | Events → internal command | `*Processor.java` (no controller) |

---

## Ralph — Autonomous Agent Loop

**Ralph** is a shell-based AI agent loop that reads slice definitions from `.slices/` and implements them one at a time using Claude Code.

### How it works

1. Reads `.slices/<Context>/index.json` and picks the highest-priority slice with `status: planned`.
2. Sets `"assigned": true` and delegates implementation to `prompt_backend.md`.
3. The backend agent picks the correct skill (`/state-change-slice`, `/state-view-slice`, or `/automation-slice`), implements the slice, runs tests, commits, and marks the slice `Done`.
4. Ralph loops until all slices are done or the iteration limit is reached.

### Running Ralph

```bash
./ralph.sh            # default: 10 iterations
./ralph.sh 20         # custom max iterations
```

Ralph automatically retries on transient Claude errors and waits when the spending limit is reached.

**Stop signals emitted by the agent:**

| Signal | Meaning |
|---|---|
| `<promise>COMPLETE</promise>` | All slices are done |
| `<promise>NO_TASKS</promise>` | No planned slices found — loop pauses 30 s |

Progress is logged to `progress.txt`.

---

## Adding a new slice

1. Design the slice in EventModelers and export the board config.
2. Save the slice definition to `.slices/<Context>/<slicefolder>/slice.json`.
3. Add the entry to `.slices/<Context>/index.json` with `"status": "planned"`.
4. Run Ralph — it will pick it up automatically.

Or implement manually by invoking a skill directly in Claude Code:

```
/state-change-slice
/state-view-slice
/automation-slice
```

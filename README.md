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

---

## Exercises

### Exercise 1 — Let Ralph implement a slice end-to-end

**Step 1 — Set the slice to `planned`**

Open `.slices/Library Management/index.json` and change the `status` of the *Create Catalog Entry* slice from `"Done"` to `"planned"`:

```json
{
  "id": "3458764674084680611",
  "slice": "slice: Create Catalog Entry",
  "index": 1,
  "context": "Library Management",
  "folder": "createcatalogentry",
  "status": "planned"
}
```

`planned` is the trigger status — Ralph will pick up any slice with that status on the next iteration.

**Step 2 — Start the agent loop**

On macOS / Linux (requires Bash):

```bash
./ralph.sh
```

On Windows (requires WSL or Node.js):

```bash
node ralph.js
```

Ralph reads the index, claims the slice, and delegates implementation to the backend agent via `prompt_backend.md`.

**Step 3 — Watch the agent work**

Ralph logs progress to `progress.txt`. You can tail it in a second terminal:

```bash
tail -f progress.txt
```

The agent invokes the appropriate skill (`/state-change-slice`, `/state-view-slice`, or `/automation-slice`), generates all files, runs the tests, commits the result, and marks the slice `Done` in `index.json`.

**Step 4 — Review the generated code**

Once Ralph emits `<promise>COMPLETE</promise>`, inspect what was generated under `src/`:

```
src/main/java/de/eventmodelers/
└── librarymanagement/
    └── slices/
        └── createcatalogentry/
            ├── CreateCatalogEntryController.java   # POST endpoint
            └── internal/
                └── (aggregate handler wired here)
src/main/java/de/eventmodelers/
├── domain/
│   ├── BookAggregate.java                          # Aggregate with @CommandHandler
│   └── commands/
│       └── CreateCatalogEntryCommand.java
└── events/
    └── CatalogEntryCreatedEvent.java
```

Run the tests to confirm everything passes:

```bash
./mvnw test -Dtest="CreateCatalogEntry*"
```

---

### Exercise 2 — Let Ralph implement a STATE_VIEW slice

The *Catalog Entries* slice is a read model — it listens to events and exposes a `GET` endpoint. The flow is identical to Exercise 1.

**Step 1 — Set the slice to `planned`**

In `.slices/Library Management/index.json`, change the `status` of *Catalog Entries* to `"planned"`:

```json
{
  "id": "3458764674084680601",
  "slice": "slice: Catalog Entries",
  "index": 3,
  "context": "Library Management",
  "folder": "catalogentries",
  "status": "planned"
}
```

**Step 2 — Run Ralph**

```bash
./ralph.sh       # macOS / Linux
node ralph.js    # Windows / Node.js
```

**Step 3 — Review the generated read model**

Because this is a `STATE_VIEW` slice, Ralph calls `/state-view-slice` and generates a different set of files:

```
src/main/java/de/eventmodelers/
└── librarymanagement/
    └── slices/
        └── catalogentries/
            ├── CatalogEntriesController.java          # GET endpoint
            ├── CatalogEntriesReadModel.java           # JPA entity + DTO
            ├── CatalogEntriesReadModelQuery.java      # Query record
            └── internal/
                ├── CatalogEntriesReadModelProjector.java     # @EventHandler
                ├── CatalogEntriesReadModelQueryHandler.java  # @QueryHandler
                └── CatalogEntriesReadModelRepository.java   # Spring Data repo
```

A Flyway migration script for the read model table is also created under `src/main/resources/db/migration/`.

Run the tests:

```bash
./mvnw test -Dtest="CatalogEntries*"
```

---

### Exercise 3 — Update a slice and let Ralph re-implement it

The *Catalog Entries* read model was extended in EventModelers: a **Catalogue entry removed** event was added as a second inbound trigger, and two BDD specifications were written to drive the projector behavior. Your task is to bring those changes into the project and let Ralph implement the updated slice.

**Step 1 — Merge the updated slice definition**

```bash
git merge exercise_3
```

This brings in the updated `.slices/Library Management/catalogentries/slice.json` and `config.json` with:
- `Catalogue entry removed` as a second inbound event on the read model
- Two specifications (Given/Then scenarios) covering *entry created* and *entry removed* projections

**Step 2 — Reset the slice status to `planned`**

In `.slices/Library Management/index.json`, change the `status` of *Catalog Entries* back to `"planned"`:

```json
{
  "id": "3458764674084680601",
  "slice": "slice: Catalog Entries",
  "index": 3,
  "context": "Library Management",
  "folder": "catalogentries",
  "status": "planned"
}
```

**Step 3 — Run Ralph**

```bash
./ralph.sh       # macOS / Linux
node ralph.js    # Windows / Node.js
```

Ralph picks up the slice, reads the updated spec, and re-generates the projector to handle both events. The specifications are used to generate the integration test scenarios.

**Step 4 — Review the changes**

The projector should now handle two events:

| Event | Action |
|---|---|
| `CatalogueEntryCreatedEvent` | Save entry to read model |
| `CatalogueEntryRemovedEvent` | Delete entry from read model |

The generated test class covers both scenarios from the spec.

Run the tests:

```bash
./mvnw test -Dtest="CatalogEntries*"
```

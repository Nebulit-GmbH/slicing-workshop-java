---
name: slice-builder
description: Transform slice definitions from config.json into executable Java code
---

# Slice Builder - Master Skill

## Overview

Transform slice definitions from `config.json` into Java code following CQRS/Event Sourcing patterns with Axon Framework.

## Input: config.json

```json
{
  "slices": [
    {
      "title": "slice: <Name>",
      "context": "<BoundedContext>",
      "sliceType": "STATE_CHANGE|STATE_VIEW|UNDEFINED",
      "commands": [...],
      "events": [...],
      "readmodels": [...],
      "processors": [...],
      "specifications": [...]
    }
  ]
}
```

## Slice Type Selection

| sliceType | Skill | Generates |
|-----------|-------|-----------|
| `STATE_CHANGE` | state-change-slice | Commands, Events, Aggregate, REST Controller (POST), Tests |
| `STATE_VIEW` | state-view-slice | ReadModel, Projector, Query, QueryHandler, REST Controller (GET), Migration, Tests |
| `UNDEFINED` | Skip | - |

If slice contains `processors` with `type: "AUTOMATION"`, also apply **automation-slice**.

## Package Structure

```
de.eventmodelers/
├── common/                          # Shared interfaces
├── domain/                          # Aggregates and Commands
│   ├── <Aggregate>Aggregate.java
│   └── commands/
│       └── <CommandName>Command.java
├── events/                          # All events (shared)
└── <context>/
    ├── ProcessingGroups.java
    └── slices/<slicename>/          # All slice artifacts
        ├── Controller
        ├── ReadModel, Query (STATE_VIEW)
        └── internal/ (Projector, QueryHandler, Processor)
```

## File Locations

| Type | Location |
|------|----------|
| Commands | `domain/commands/<CommandName>Command.java` |
| Events | `events/<EventName>Event.java` |
| Aggregates | `domain/<Aggregate>Aggregate.java` |
| Slice artifacts | `<context>/slices/<slicename>/` |
| Internal (Projector, QueryHandler, Processor) | `<context>/slices/<slicename>/internal/` |
| Migrations | `resources/db/migration/` |
| Tests | `test/<context>/slices/<slicename>/` |

## Field Type Mapping

| config.json Type | Java Type |
|------------------|-----------|
| UUID | UUID |
| String | String |
| Date | LocalDate |
| Integer | Integer |
| Boolean | Boolean |
| Decimal | BigDecimal |
| Multiple cardinality | List<T> |
| optional: true | @Nullable T |

## Name Transformations

| Slice Title | Class Name | Package Name |
|-------------|------------|--------------|
| "Register Customer" | RegisterCustomer | registercustomer |
| "Customer Registered" | CustomerRegistered | customerregistered |

Rules:
1. Remove "slice: " prefix
2. PascalCase for class names
3. Lowercase no separators for package names
4. Append suffix: Command, Event, ReadModel, Processor

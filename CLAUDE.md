# Project Guidelines

## Package Structure

```
de.eventmodelers/
├── common/                          # Shared interfaces (Command, Event, Query, etc.)
├── domain/                          # Aggregates and Commands
│   ├── <Aggregate>Aggregate.java
│   └── commands/
│       └── <CommandName>Command.java
├── events/                          # All events (shared across contexts)
│   └── <EventName>Event.java
└── <context>/                       # Bounded context module
    ├── ProcessingGroups.java
    └── slices/                      # All slice artifacts
        └── <slicename>/
            ├── <SliceName>Controller.java      # REST controller
            ├── <SliceName>ReadModel.java       # Entity + DTO (STATE_VIEW)
            ├── <SliceName>ReadModelQuery.java  # Query record (STATE_VIEW)
            └── internal/
                ├── <SliceName>ReadModelProjector.java   # Projector (STATE_VIEW)
                ├── <SliceName>ReadModelQueryHandler.java # QueryHandler (STATE_VIEW)
                ├── <SliceName>ReadModelRepository.java   # Repository (STATE_VIEW)
                └── <SliceName>Processor.java            # Automation (AUTOMATION)
```

## Slice Types

| Type | Generates | REST Method |
|------|-----------|-------------|
| STATE_CHANGE | Command, Event, Aggregate handler, Controller, Tests | POST |
| STATE_VIEW | ReadModel, Projector, Query, QueryHandler, Controller, Migration, Tests | GET |
| AUTOMATION | Processor (embedded in STATE_CHANGE slices) | - |

## Conventions

- **Commands**: `domain/commands/<CommandName>Command`
- **Events**: `events/<EventName>Event`
- **Aggregates**: `domain/<Aggregate>Aggregate`
- **Slice artifacts**: `<context>/slices/<slicename>/*`

## REST Endpoints

- STATE_CHANGE: `POST /api/<context>/<resource>`
- STATE_VIEW: `GET /api/<context>/<resource>/{id}`

## Config

Slice definitions are read from `config.json` at project root.

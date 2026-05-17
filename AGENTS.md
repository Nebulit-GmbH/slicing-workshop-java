# Agent Learnings

## Axon Framework Patterns

- `AggregateLifecycle.getVersion()` returns nullable `Long` — guard against NPE when constructing `CommandResult(String, long)` with `version != null ? version : 0L`
- Use `@CreationPolicy(AggregateCreationPolicy.ALWAYS)` on `@CommandHandler` methods for creation commands (even when `createsAggregate: false` in slice JSON — JSON flag may not reflect intent)
- Aggregates require a protected no-arg constructor for Axon event sourcing

## Slice Conventions

- Context package: context name lowercased with no spaces (e.g. "Library Management" → `librarymanagement`)
- Generated ID fields in STATE_CHANGE commands: use an inner `Request` record in the controller (without ID), generate `UUID.randomUUID()` server-side; the command carries `@TargetAggregateIdentifier UUID aggregateId`
- Debug endpoint uses kebab-case command name: `/api/<context>/debug/<command-name-kebab-case>` (GET with `@RequestParam`)
- REST path for STATE_CHANGE: `POST /api/<context-kebab>/<resource-plural>` (e.g. `/api/library-management/catalog-entries`)

## Testing

- Use `AggregateTestFixture` from `axon-test` for unit testing aggregates — no Spring context needed
- Run per-slice tests with: `mvn test -Dtest="<TestClassName>"`

---
name: state-change-slice
description: Generate commands, events, aggregates, REST controllers (POST), and tests for STATE_CHANGE slices
---

# STATE_CHANGE Slice Skill

## Overview

A STATE_CHANGE slice modifies aggregate state through commands and events. It generates:
- Commands (in `domain/commands`)
- Events (in `events`)
- Aggregate handlers (in `domain`)
- REST Controller (POST) (in `<context>/slices/<slicename>`)
- Test cases (from specifications)

**Note:** STATE_CHANGE slices do NOT generate projections. Those belong to STATE_VIEW slices.

## Flow Pattern

```
REST Controller (POST) → Command → Aggregate (@CommandHandler) → Event
```

## Code Generation Steps

### Step 1: Create Command

**Location:** `src/main/java/de/eventmodelers/domain/commands/<CommandName>Command.java`

```java
package de.eventmodelers.domain.commands;

import de.eventmodelers.common.Command;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.UUID;

public record <CommandName>Command(
    @TargetAggregateIdentifier UUID aggregateId
    // Add fields from slice command definition
) implements Command {}
```

### Step 2: Create Event

**Location:** `src/main/java/de/eventmodelers/events/<EventName>Event.java`

```java
package de.eventmodelers.events;

import de.eventmodelers.common.Event;

import java.util.UUID;

public record <EventName>Event(
    UUID aggregateId
    // Add fields from slice event definition
) implements Event {}
```

### Step 3: Add CommandHandler to Aggregate

**Location:** `src/main/java/de/eventmodelers/domain/<Aggregate>Aggregate.java`

**For new aggregate (createsAggregate: true or first command):**
```java
@CreationPolicy(AggregateCreationPolicy.ALWAYS)
@CommandHandler
public CommandResult handle(<CommandName>Command command) {
    AggregateLifecycle.apply(
        new <EventName>Event(
            command.aggregateId()
            // Map command fields to event fields
        )
    );
    return new CommandResult(command.aggregateId(), AggregateLifecycle.getVersion());
}
```

**For existing aggregate:**
```java
@CommandHandler
public CommandResult handle(<CommandName>Command command) {
    // Business validation logic
    AggregateLifecycle.apply(
        new <EventName>Event(
            command.aggregateId()
            // Map command fields to event fields
        )
    );
    return new CommandResult(command.aggregateId(), AggregateLifecycle.getVersion());
}
```

### Step 4: Add EventSourcingHandler to Aggregate

```java
@EventSourcingHandler
public void on(<EventName>Event event) {
    this.aggregateId = event.aggregateId();
    // Update other state fields
}
```

### Step 5: Create REST Controller (POST + Debug)

**Location:** `src/main/java/de/eventmodelers/<context>/slices/<slicename>/<SliceName>Controller.java`

```java
package de.eventmodelers.<context>.slices.<slicename>;

import de.eventmodelers.domain.commands.<CommandName>Command;
import de.eventmodelers.common.CommandResult;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/<context>")
public class <SliceName>Controller {
    private final CommandGateway commandGateway;

    public <SliceName>Controller(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @PostMapping("/<resource>")
    public ResponseEntity<CommandResult> execute(@RequestBody <CommandName>Command command) {
        var result = commandGateway.sendAndWait(command);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/debug/<command-name-kebab-case>")
    public ResponseEntity<CommandResult> debug(
            @RequestParam(required = false) UUID aggregateId,
            @RequestParam <Type> <fieldName>
            // Add @RequestParam for each command field
    ) {
        var command = new <CommandName>Command(
            aggregateId != null ? aggregateId : UUID.randomUUID(),
            <fieldName>
            // Map all parameters to command
        );
        var result = commandGateway.sendAndWait(command);
        return ResponseEntity.ok(result);
    }
}
```

**Debug endpoint notes:**
- Uses GET with query parameters for easy browser/curl testing
- Uses command name in kebab-case (e.g., `/debug/register-customer`) to avoid conflicts with STATE_VIEW endpoints
- `aggregateId` is optional - generates a new UUID if not provided
- All other fields are passed as `@RequestParam`

### Step 6: Create Test Cases (from specifications)

**Location:** `src/test/java/de/eventmodelers/<context>/slices/<slicename>/<CommandName>Test.java`

```java
package de.eventmodelers.<context>.slices.<slicename>;

import de.eventmodelers.domain.<Aggregate>Aggregate;
import de.eventmodelers.domain.commands.<CommandName>Command;
import de.eventmodelers.events.<EventName>Event;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class <CommandName>Test {
    private AggregateTestFixture<<Aggregate>Aggregate> fixture;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(<Aggregate>Aggregate.class);
    }

    @Test
    void <specTitleAsCamelCase>() {
        var aggregateId = UUID.randomUUID();

        fixture
            .given(/* events from spec.given */)
            .when(new <CommandName>Command(aggregateId, /* fields from spec.when */))
            .expectSuccessfulHandlerExecution()
            .expectEvents(new <EventName>Event(aggregateId, /* fields from spec.then */));
    }
}
```

## Field Type Mapping

| Slice Type | Java Type |
|------------|-----------|
| UUID | UUID |
| String | String |
| Date | LocalDate |
| Integer | Integer |
| Boolean | Boolean |
| Decimal | BigDecimal |
| Multiple cardinality | List<T> |
| optional: true | @Nullable T |

## ProcessingGroups

**Location:** `src/main/java/de/eventmodelers/<context>/ProcessingGroups.java`

```java
package de.eventmodelers.<context>;

public final class ProcessingGroups {
    public static final String <CONTEXT> = "<CONTEXT>";

    private ProcessingGroups() {}
}
```

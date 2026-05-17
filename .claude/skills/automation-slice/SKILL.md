---
name: automation-slice
description: Generate event-driven automation processors for AUTOMATION slices
---

# AUTOMATION Slice Skill

## Overview

An AUTOMATION slice contains processors that react to events and trigger commands automatically. Automation processors appear in the `processors` array of a STATE_CHANGE slice.

## Flow Pattern

```
Source Event → @EventHandler Processor → CommandGateway.send() → Target Command
```

## Key Characteristics

1. **Event-triggered** - Processors react to events via `@EventHandler`
2. **Command-emitting** - Use `CommandGateway` to send commands
3. **No state mutation** - Processors orchestrate, they don't own state
4. **Replay-safe** - Always use `@DisallowReplay` to prevent re-triggering on replay
5. **Implements Processor interface** - Marker interface for automation components

## Code Generation Steps

### Step 1: Identify Source Event

Look at processor dependencies:
- OUTBOUND to COMMAND = the command this processor sends
- INBOUND from EVENT = the event that triggers this processor
- If not explicit, match processor fields with event fields

### Step 2: Create Processor Class

**Location:** `src/main/java/de/eventmodelers/<context>/slices/<slicename>/internal/<ProcessorName>Processor.java`

```java
package de.eventmodelers.<context>.slices.<slicename>.internal;

import de.eventmodelers.common.Processor;
import de.eventmodelers.events.<SourceEvent>Event;
import de.eventmodelers.<context>.ProcessingGroups;
import de.eventmodelers.domain.commands.<TargetCommand>Command;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.DisallowReplay;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@ProcessingGroup(ProcessingGroups.<CONTEXT>)
@Component
public class <ProcessorName>Processor implements Processor {
    private static final Logger logger = LoggerFactory.getLogger(<ProcessorName>Processor.class);

    private final CommandGateway commandGateway;

    public <ProcessorName>Processor(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @DisallowReplay
    @EventHandler
    public void on(<SourceEvent>Event event) {
        commandGateway.send(
            new <TargetCommand>Command(
                event.aggregateId()
                // Map event fields to command fields
            )
        );
    }
}
```

### Step 3: Handle Field Mappings

When processor fields have a `mapping` attribute:
```json
{ "name": "userId", "type": "String", "mapping": "owner" }
```

This means map from the source event's `owner` field to the command's expected field.

### Step 4: Testing Processors

**Location:** `src/test/java/de/eventmodelers/<context>/slices/<slicename>/<ProcessorName>ProcessorTest.java`

```java
package de.eventmodelers.<context>.slices.<slicename>;

import de.eventmodelers.<context>.slices.<slicename>.internal.<ProcessorName>Processor;
import de.eventmodelers.events.<SourceEvent>Event;
import de.eventmodelers.domain.commands.<TargetCommand>Command;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

class <ProcessorName>ProcessorTest {

    @Test
    void shouldSendCommandWhenEventReceived() {
        var commandGateway = Mockito.mock(CommandGateway.class);
        var processor = new <ProcessorName>Processor(commandGateway);

        var event = new <SourceEvent>Event(UUID.randomUUID());

        processor.on(event);

        verify(commandGateway).send(argThat((<TargetCommand>Command cmd) ->
            cmd.aggregateId().equals(event.aggregateId())
        ));
    }
}
```

## Important Notes

- **Always use `@DisallowReplay`** - Prevents re-sending commands during event store replays
- **Use correct ProcessingGroup** - Determines which event processor handles these events
- **Handle errors gracefully** - Log and re-throw for DLQ handling, or catch and log for non-critical automations

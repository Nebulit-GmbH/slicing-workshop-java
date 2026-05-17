---
name: projection-test-builder
description: Generate projection integration tests from STATE_VIEW specifications
---

# Projection Test Builder Skill

## Overview

Generate projection integration tests from STATE_VIEW slice specifications. Tests verify read model projection behavior using given/then structure.

## When to Use

- STATE_VIEW slice with specifications defined
- Specifications contain `given` (events) and `then` (expected read model state)

## Input Structure

```json
{
  "specifications": [
    {
      "title": "spec: cart items with removed item",
      "given": [
        { "title": "Cart Created", "type": "SPEC_EVENT", "fields": [...] },
        { "title": "Item Added", "type": "SPEC_EVENT", "fields": [...] }
      ],
      "then": [
        { "title": "cart items", "type": "SPEC_READMODEL", "fields": [...] }
      ],
      "comments": [{ "description": "Read Model should display an empty list" }]
    }
  ]
}
```

## Generation Steps

### Step 1: Determine Test Location

**Location:** `src/test/java/de/eventmodelers/<context>/slices/<slicename>/<SliceName>ProjectionTest.java`

### Step 2: Convert Event Names

- "Item Added" → `ItemAddedEvent`
- "Cart Created" → `CartCreatedEvent`

### Step 3: Generate Test Class

```java
package de.eventmodelers.<context>.slices.<slicename>;

import de.eventmodelers.<context>.slices.<slicename>.<SliceName>ReadModel;
import de.eventmodelers.<context>.slices.<slicename>.<SliceName>ReadModelQuery;
import de.eventmodelers.common.support.BaseIntegrationTest;
import de.eventmodelers.common.support.ProjectionFixtureConfiguration;
import de.eventmodelers.common.support.RandomData;
import de.eventmodelers.domain.<Aggregate>Aggregate;
import de.eventmodelers.events.*;
import org.axonframework.modelling.command.Repository;
import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static de.eventmodelers.common.support.TestUtils.awaitUntilAsserted;
import static org.assertj.core.api.Assertions.assertThat;

class <SliceName>ProjectionTest extends BaseIntegrationTest {
    @Autowired
    private QueryGateway queryGateway;

    @Autowired
    private Repository<<Aggregate>Aggregate> repository;
}
```

### Step 4: Generate Test Methods

```java
@Test
void <specTitleAsCamelCase>() {
    var aggregateId = UUID.randomUUID();

    var fixture = ProjectionFixtureConfiguration.aggregateInstance(
        () -> repository.newInstance(<Aggregate>Aggregate::new)
    );

    // Apply events from given[]
    fixture.given(
        RandomData.newInstance(<EventName>Event.class, event -> {
            event.setAggregateId(aggregateId);
        })
    );
    fixture.apply();

    // Verify read model state from then[]
    awaitUntilAsserted(() -> {
        var readModel = queryGateway.query(
            new <SliceName>ReadModelQuery(aggregateId),
            <SliceName>ReadModel.class
        ).join();

        assertThat(readModel).isNotNull();
        // Assert expected field values from then[]
    });
}
```

## Field Handling

- **UUID with idAttribute: true** - Use shared UUID across related events
- **Example values** - Use for assertions: `"example": "9.99"` → `assertThat(price).isEqualTo(9.99)`
- **Mapping attribute** - `"mapping": "productId"` means map from `productId` field

## Assertion Patterns

| Scenario | Assertion |
|----------|-----------|
| Empty list | `assertThat(readModel.data()).isEmpty()` |
| List size | `assertThat(readModel.data()).hasSize(2)` |
| Field value | `assertThat(readModel.data().totalPrice()).isEqualTo(19.98)` |
| Existence | `assertThat(readModel).isNotNull()` |

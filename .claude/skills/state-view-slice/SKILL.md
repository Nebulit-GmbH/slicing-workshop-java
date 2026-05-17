---
name: state-view-slice
description: Generate read models, projectors, queries, and REST controllers (GET) for STATE_VIEW slices
---

# STATE_VIEW Slice Skill

## Overview

A STATE_VIEW slice projects event data into read models. It generates:
- Read Model Entity (in `slices.<slicename>`)
- Projector (@EventHandler) (in `slices.<slicename>.internal`)
- Query and QueryHandler (in `slices.<slicename>`)
- REST Controller (GET) (in `slices.<slicename>`)
- Database migration
- Test cases (from specifications)

**Note:** STATE_VIEW slices do NOT contain commands or events. Events come from STATE_CHANGE slices.

## Flow Pattern

```
Events (from STATE_CHANGE) → @EventHandler Projector → ReadModel Entity → @QueryHandler → REST Controller (GET)
```

## Code Generation Steps

### Step 1: Identify Source Events

Look at the readmodel's dependencies with `type: "INBOUND"` and `elementType: "EVENT"` to find which events populate the read model.

### Step 2: Create Read Model Entity

**Location:** `src/main/java/de/eventmodelers/<context>/slices/<slicename>/<SliceName>ReadModel.java`

**Single Key (one idAttribute):**
```java
package de.eventmodelers.<context>.slices.<slicename>;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Table(name = "<table_name>_read_model", schema = "public")
@Entity
public class <SliceName>ReadModelEntity {
    @Id
    @Column(name = "aggregate_id")
    private UUID aggregateId;

    @Column(name = "<field_name>")
    private <Type> <fieldName>;

    // Getters and setters
}

public record <SliceName>ReadModel(<SliceName>ReadModelEntity data) {}
```

**Composite Key (multiple idAttributes):**
```java
package de.eventmodelers.<context>.slices.<slicename>;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class <SliceName>Key implements Serializable {
    private UUID <firstId>;
    private UUID <secondId>;

    public <SliceName>Key() {}
    public <SliceName>Key(UUID <firstId>, UUID <secondId>) {
        this.<firstId> = <firstId>;
        this.<secondId> = <secondId>;
    }

    // Getters, setters, equals, hashCode
}

@IdClass(<SliceName>Key.class)
@Table(name = "<table_name>_read_model", schema = "public")
@Entity
public class <SliceName>ReadModelEntity {
    @Id
    @Column(name = "<first_id>")
    private UUID <firstId>;

    @Id
    @Column(name = "<second_id>")
    private UUID <secondId>;

    // Other fields...
}

public record <SliceName>ReadModel(List<<SliceName>ReadModelEntity> data) {}
```

### Step 3: Create Query

**Location:** `src/main/java/de/eventmodelers/<context>/slices/<slicename>/<SliceName>ReadModelQuery.java`

```java
package de.eventmodelers.<context>.slices.<slicename>;

import java.util.UUID;

public record <SliceName>ReadModelQuery(UUID aggregateId) {}
```

### Step 4: Create Projector

**Location:** `src/main/java/de/eventmodelers/<context>/slices/<slicename>/internal/<SliceName>ReadModelProjector.java`

```java
package de.eventmodelers.<context>.slices.<slicename>.internal;

import de.eventmodelers.<context>.ProcessingGroups;
import de.eventmodelers.<context>.slices.<slicename>.<SliceName>ReadModelEntity;
import de.eventmodelers.events.<EventName>Event;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

interface <SliceName>ReadModelRepository extends JpaRepository<<SliceName>ReadModelEntity, UUID> {}

@ProcessingGroup(ProcessingGroups.<CONTEXT>)
@Component
public class <SliceName>ReadModelProjector {
    private final <SliceName>ReadModelRepository repository;

    public <SliceName>ReadModelProjector(<SliceName>ReadModelRepository repository) {
        this.repository = repository;
    }

    @EventHandler
    public void on(<EventName>Event event) {
        var entity = repository.findById(event.aggregateId())
            .orElseGet(<SliceName>ReadModelEntity::new);
        entity.setAggregateId(event.aggregateId());
        // Map event fields to entity fields
        repository.save(entity);
    }
}
```

### Step 5: Create QueryHandler

**Location:** `src/main/java/de/eventmodelers/<context>/slices/<slicename>/internal/<SliceName>ReadModelQueryHandler.java`

```java
package de.eventmodelers.<context>.slices.<slicename>.internal;

import de.eventmodelers.<context>.slices.<slicename>.<SliceName>ReadModel;
import de.eventmodelers.<context>.slices.<slicename>.<SliceName>ReadModelQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

@Component
public class <SliceName>ReadModelQueryHandler {
    private final <SliceName>ReadModelRepository repository;

    public <SliceName>ReadModelQueryHandler(<SliceName>ReadModelRepository repository) {
        this.repository = repository;
    }

    @QueryHandler
    public <SliceName>ReadModel handleQuery(<SliceName>ReadModelQuery query) {
        var entity = repository.findById(query.aggregateId()).orElse(null);
        return entity != null ? new <SliceName>ReadModel(entity) : null;
    }
}
```

### Step 6: Create REST Controller (GET + Debug)

**Location:** `src/main/java/de/eventmodelers/<context>/slices/<slicename>/<SliceName>Controller.java`

```java
package de.eventmodelers.<context>.slices.<slicename>;

import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/<context>")
public class <SliceName>Controller {
    private final QueryGateway queryGateway;

    public <SliceName>Controller(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @GetMapping("/<resource>/{aggregateId}")
    public ResponseEntity<<SliceName>ReadModel> get(@PathVariable UUID aggregateId) {
        var result = queryGateway.query(
            new <SliceName>ReadModelQuery(aggregateId),
            <SliceName>ReadModel.class
        ).join();
        return result != null ? ResponseEntity.ok(result) : ResponseEntity.notFound().build();
    }

    @GetMapping("/debug/<resource>")
    public ResponseEntity<<SliceName>ReadModel> debug(@RequestParam UUID aggregateId) {
        var result = queryGateway.query(
            new <SliceName>ReadModelQuery(aggregateId),
            <SliceName>ReadModel.class
        ).join();
        return result != null ? ResponseEntity.ok(result) : ResponseEntity.notFound().build();
    }
}
```

**Debug endpoint notes:**
- Uses readmodel name (e.g., `/debug/customers`) for query endpoints
- STATE_CHANGE debug uses command name (e.g., `/debug/register-customer`) to avoid conflicts

### Step 7: Create Database Migration

**Location:** `src/main/resources/db/migration/V<version>__create_<table_name>.sql`

```sql
CREATE TABLE IF NOT EXISTS public.<table_name>_read_model (
    "aggregate_id" UUID NOT NULL,
    "<column_name>" <SQL_TYPE>,
    PRIMARY KEY ("aggregate_id")
);
```

**SQL Type Mapping:**
| Java Type | SQL Type |
|-----------|----------|
| UUID | UUID |
| String | VARCHAR(255) |
| LocalDate | DATE |
| Integer | INTEGER |
| Boolean | BOOLEAN |
| BigDecimal | DECIMAL(19,4) |

### Step 8: Create Test Cases (from specifications)

**Location:** `src/test/java/de/eventmodelers/<context>/slices/<slicename>/<SliceName>ProjectionTest.java`

```java
package de.eventmodelers.<context>.slices.<slicename>;

import de.eventmodelers.common.support.BaseIntegrationTest;
import de.eventmodelers.common.support.ProjectionFixtureConfiguration;
import de.eventmodelers.events.<EventName>Event;
import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static de.eventmodelers.common.support.TestUtils.awaitUntilAsserted;
import static org.assertj.core.api.Assertions.assertThat;

class <SliceName>ProjectionTest extends BaseIntegrationTest {

    @Autowired
    private ProjectionFixtureConfiguration projectionFixture;

    @Autowired
    private QueryGateway queryGateway;

    @Test
    void <specTitleAsCamelCase>() {
        var aggregateId = UUID.randomUUID();

        // Given: Apply events from spec.given
        projectionFixture.apply(new <EventName>Event(aggregateId, /* fields */));

        // Then: Verify read model state from spec.then
        awaitUntilAsserted(() -> {
            var result = queryGateway.query(
                new <SliceName>ReadModelQuery(aggregateId),
                <SliceName>ReadModel.class
            ).join();
            assertThat(result).isNotNull();
            // Assert expected field values
        });
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
| idAttribute: true | @Id field |

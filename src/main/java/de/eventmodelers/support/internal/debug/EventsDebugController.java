package de.eventmodelers.support.internal.debug;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;
import org.axonframework.config.Configuration;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

enum ElementType {
  EVENT,
  DEAD_LETTER
}

record FieldInfo(String name, String type, String example) {}

record EventMessageInfo(
    String id,
    String title,
    List<FieldInfo> fields,
    ElementType type,
    String description,
    boolean highlighted) {

  EventMessageInfo(
      String id, String title, List<FieldInfo> fields, ElementType type, String description) {
    this(id, title, fields, type, description, false);
  }
}

record EventsResponse(List<EventMessageInfo> events) {}

@ConditionalOnProperty(
    name = "application.debug.enabled",
    havingValue = "true",
    matchIfMissing = false)
@RestController
public class EventsDebugController {

  private final EventStorageEngine eventStorageEngine;
  private final Configuration configuration;

  public EventsDebugController(EventStorageEngine eventStorageEngine, Configuration configuration) {
    this.eventStorageEngine = eventStorageEngine;
    this.configuration = configuration;
  }

  @CrossOrigin
  @GetMapping("/internal/debug/events/{aggregateId}")
  public List<? extends DomainEventMessage<?>> resolveEvents(
      @PathVariable("aggregateId") String aggregateId) {
    return eventStorageEngine.readEvents(aggregateId).asStream().toList();
  }

  @CrossOrigin
  @GetMapping("/internal/debug/history/{aggregateId}")
  public EventsResponse resolveHistory(
      @RequestParam(value = "processingGroup", required = false) String processingGroup,
      @PathVariable("aggregateId") String aggregateId) {

    var domainEvents =
        StreamSupport.stream(
                eventStorageEngine.readEvents(aggregateId).asStream().spliterator(), false)
            .map(e -> (DomainEventMessage<?>) e)
            .toList();

    var mappedDomainEvents =
        domainEvents.stream()
            .map(
                event ->
                    new EventMessageInfo(
                        event.getIdentifier(),
                        camelCaseToWords(event.getPayloadType().getSimpleName()),
                        extractFields(event.getPayload()),
                        ElementType.EVENT,
                        ""))
            .toList();

    if (processingGroup != null) {
      var dlq =
          configuration
              .eventProcessingConfiguration()
              .deadLetterQueue(processingGroup)
              .orElse(null);
      if (dlq != null) {
        var letters = new ArrayList<EventMessageInfo>();
        for (var sequence : dlq.deadLetters()) {
          for (var letter : sequence) {
            if (letter.message() instanceof DomainEventMessage<?> msg) {
              var cause = letter.cause().orElse(null);
              letters.add(
                  new EventMessageInfo(
                      msg.getAggregateIdentifier(),
                      msg.getPayloadType().getSimpleName(),
                      List.of(),
                      ElementType.EVENT,
                      cause != null ? cause.message() : "",
                      true));
            }
          }
        }
        var combined = new ArrayList<>(mappedDomainEvents);
        combined.addAll(letters);
        return new EventsResponse(combined);
      }
    }
    return new EventsResponse(mappedDomainEvents);
  }

  private String camelCaseToWords(String str) {
    return str.replaceAll("([a-z])([A-Z])", "$1 $2")
        .replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2")
        .trim();
  }

  private List<FieldInfo> extractFields(Object payload) {
    var fields = new ArrayList<FieldInfo>();
    for (Field field : payload.getClass().getDeclaredFields()) {
      field.setAccessible(true);
      try {
        var value = field.get(payload);
        fields.add(
            new FieldInfo(
                field.getName(),
                field.getType().getSimpleName(),
                value != null ? value.toString() : ""));
      } catch (Exception e) {
        // skip inaccessible fields
      }
    }
    return fields;
  }
}

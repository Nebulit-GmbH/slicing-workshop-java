package de.eventmodelers.support.notifications.internal;

import java.util.Map;
import java.util.UUID;

public record Notification(
    String id, String type, String message, Map<String, Object> payload, long timestamp) {

  public Notification() {
    this(UUID.randomUUID().toString(), "message", "", Map.of(), System.currentTimeMillis());
  }

  public Notification(String type, String message, Map<String, Object> payload) {
    this(UUID.randomUUID().toString(), type, message, payload, System.currentTimeMillis());
  }
}

package de.eventmodelers.support.notifications.internal;

import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@CrossOrigin
public class UINotificationResource {

  private final SseNotificationService sseNotificationService;

  public UINotificationResource(SseNotificationService sseNotificationService) {
    this.sseNotificationService = sseNotificationService;
  }

  @GetMapping(value = "/subscribe/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter subscribe(
      @PathVariable String sessionId,
      @RequestParam(required = false, defaultValue = "0") long timeout) {
    return sseNotificationService.subscribe(sessionId, timeout);
  }

  @PostMapping("/send/{sessionId}")
  public ResponseEntity<NotificationResponse> sendNotification(
      @PathVariable String sessionId,
      @RequestBody(required = false) SendNotificationRequest request) {
    var notification =
        new Notification(
            request != null && request.type() != null ? request.type() : "message",
            request != null && request.message() != null ? request.message() : "",
            request != null && request.payload() != null ? request.payload() : Map.of());

    boolean sent = sseNotificationService.sendNotification(sessionId, notification);
    if (sent) {
      return ResponseEntity.ok(new NotificationResponse(true, "Notification sent"));
    } else {
      return ResponseEntity.ok(new NotificationResponse(false, "No active connection for session"));
    }
  }

  @PostMapping("/broadcast")
  public ResponseEntity<NotificationResponse> broadcast(
      @RequestBody SendNotificationRequest request) {
    var notification =
        new Notification(
            request.type() != null ? request.type() : "notification",
            request.message(),
            request.payload() != null ? request.payload() : Map.of());

    sseNotificationService.broadcast(notification);
    return ResponseEntity.ok(
        new NotificationResponse(
            true,
            "Notification broadcast to "
                + sseNotificationService.getActiveSessionCount()
                + " clients"));
  }

  @GetMapping("/status/{sessionId}")
  public ResponseEntity<ConnectionStatus> getConnectionStatus(@PathVariable String sessionId) {
    return ResponseEntity.ok(
        new ConnectionStatus(
            sessionId,
            sseNotificationService.isConnected(sessionId),
            sseNotificationService.getActiveSessionCount()));
  }
}

record SendNotificationRequest(String message, String type, Map<String, Object> payload) {}

record NotificationResponse(boolean success, String message) {}

record ConnectionStatus(String sessionId, boolean connected, int activeConnections) {}

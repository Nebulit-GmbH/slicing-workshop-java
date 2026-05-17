package de.eventmodelers.support.notifications.internal;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SseNotificationService {

  private static final Logger logger = LoggerFactory.getLogger(SseNotificationService.class);
  private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();

  public SseEmitter subscribe(String sessionId, long timeout) {
    var emitter = new SseEmitter(timeout);

    emitter.onCompletion(
        () -> {
          logger.info("SSE connection completed for session: {}", sessionId);
          emitters.remove(sessionId);
        });

    emitter.onTimeout(
        () -> {
          logger.info("SSE connection timed out for session: {}", sessionId);
          emitters.remove(sessionId);
        });

    emitter.onError(
        ex -> {
          logger.warn("SSE connection error for session: {} - {}", sessionId, ex.getMessage());
          emitters.remove(sessionId);
        });

    emitters.put(sessionId, emitter);
    logger.info("Client subscribed with session: {}", sessionId);
    return emitter;
  }

  public boolean sendNotification(String sessionId, Notification notification) {
    var emitter = emitters.get(sessionId);
    if (emitter == null) {
      logger.warn("No active SSE connection for session: {}", sessionId);
      return false;
    }
    try {
      emitter.send(
          SseEmitter.event().id(notification.id()).name(notification.type()).data(notification));
      logger.debug("Notification sent to session {}: {}", sessionId, notification.message());
      return true;
    } catch (Exception ex) {
      logger.error("Failed to send notification to session {}: {}", sessionId, ex.getMessage());
      emitters.remove(sessionId);
      return false;
    }
  }

  public void broadcast(Notification notification) {
    var deadEmitters = new ArrayList<String>();
    emitters.forEach(
        (sessionId, emitter) -> {
          try {
            emitter.send(
                SseEmitter.event()
                    .id(notification.id())
                    .name(notification.type())
                    .data(notification));
          } catch (Exception ex) {
            logger.warn("Failed to broadcast to session {}, marking for removal", sessionId);
            deadEmitters.add(sessionId);
          }
        });
    deadEmitters.forEach(emitters::remove);
    logger.debug("Broadcast notification to {} clients", emitters.size());
  }

  public boolean isConnected(String sessionId) {
    return emitters.containsKey(sessionId);
  }

  public int getActiveSessionCount() {
    return emitters.size();
  }
}

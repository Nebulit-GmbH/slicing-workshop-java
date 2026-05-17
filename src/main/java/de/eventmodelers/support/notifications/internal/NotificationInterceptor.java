package de.eventmodelers.support.notifications.internal;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.axonframework.messaging.unitofwork.CurrentUnitOfWork;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class NotificationInterceptor {

  private final SseNotificationService notificationService;

  public NotificationInterceptor(SseNotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @Around(
      "@annotation(de.eventmodelers.support.notifications.internal.NotifyClient) && @annotation(org.axonframework.eventhandling.EventHandler)")
  public Object aroundEventHandler(ProceedingJoinPoint joinPoint) throws Throwable {
    var metaData =
        CurrentUnitOfWork.get().getMessage() != null
            ? CurrentUnitOfWork.get().getMessage().getMetaData()
            : null;
    var result = joinPoint.proceed();
    notificationService.broadcast(new Notification("message", "", java.util.Map.of()));
    return result;
  }
}

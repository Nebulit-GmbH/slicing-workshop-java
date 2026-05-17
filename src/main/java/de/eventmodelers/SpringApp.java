package de.eventmodelers;

import java.util.List;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.commandhandling.gateway.DefaultCommandGateway;
import org.axonframework.config.EventProcessingConfigurer;
import org.axonframework.eventhandling.PropagatingErrorHandler;
import org.axonframework.messaging.MessageDispatchInterceptor;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.messaging.interceptors.BeanValidationInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.modulith.Modulith;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class CorsConfig {
  @Bean
  WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        registry
            .addMapping("/api/**")
            .allowedOrigins("https://em-workshop.lovable.app")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*");
      }
    };
  }
}

@Configuration
class ValidatorConfig {
  @Bean
  LocalValidatorFactoryBean localValidatorFactoryBean() {
    return new LocalValidatorFactoryBean();
  }
}

@Configuration
class ValidationConfig {
  @Bean
  BeanValidationInterceptor<?> beanValidationInterceptor(
      LocalValidatorFactoryBean validatorFactory) {
    return new BeanValidationInterceptor<>(validatorFactory);
  }
}

@Configuration
class AxonConfig {

  @Autowired
  void configurationEventHandling(EventProcessingConfigurer config) {
    config.registerDefaultListenerInvocationErrorHandler(c -> PropagatingErrorHandler.instance());
  }

  @Bean
  CommandGateway commandGateway(
      CommandBus commandBus,
      List<MessageDispatchInterceptor<CommandMessage<?>>> dispatchInterceptors,
      List<MessageHandlerInterceptor<CommandMessage<?>>> handlerInterceptors) {
    handlerInterceptors.forEach(commandBus::registerHandlerInterceptor);
    return DefaultCommandGateway.builder()
        .commandBus(commandBus)
        .dispatchInterceptors(dispatchInterceptors.toArray(new MessageDispatchInterceptor[0]))
        .build();
  }
}

@Modulith(
    systemName = "System",
    sharedModules = {"de.eventmodelers.common", "de.eventmodelers.domain"},
    useFullyQualifiedModuleNames = true)
@EnableJpaRepositories
@SpringBootApplication
@EnableScheduling
@EntityScan(
    basePackages = {
      "de.eventmodelers",
      "org.springframework.modulith.events.jpa",
      "org.axonframework.eventhandling.tokenstore",
      "org.axonframework.eventsourcing.eventstore.jpa",
      "org.axonframework.modelling.saga.repository.jpa",
      "org.axonframework.eventhandling.deadletter.jpa"
    })
public class SpringApp {

  public static void main(String[] args) {
    SpringApplication.run(SpringApp.class, args);
  }
}

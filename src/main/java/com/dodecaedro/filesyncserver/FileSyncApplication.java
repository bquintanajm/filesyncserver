package com.dodecaedro.filesyncserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RouterFunctions.route;

@SpringBootApplication
@EnableWebSecurity
public class FileSyncApplication {
  public static void main(String[] args) {
    SpringApplication.run(FileSyncApplication.class, args);
  }

  @Bean
  RouterFunction<ServerResponse> routes(FileSyncHandler handler) {
    return route()
      .POST("/push", handler::handlePushFile)
      .POST("/pull", handler::handlePullFile)
      .GET("/time", handler::handleGetTime)
      .POST("/sync", handler::handleSyncJson)
      .filter((serverRequest, handlerFunction) -> handlerFunction.handle(serverRequest))
      .build();
  }
}

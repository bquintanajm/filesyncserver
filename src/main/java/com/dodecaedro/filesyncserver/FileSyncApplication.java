package com.dodecaedro.filesyncserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(FileSyncProperties.class)
public class FileSyncApplication {
  public static void main(String[] args) {
    SpringApplication.run(FileSyncApplication.class, args);
  }
}

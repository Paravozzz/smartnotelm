package com.smartnotelm.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SmartnotelmApplication {

  public static void main(String[] args) {
    SpringApplication.run(SmartnotelmApplication.class, args);
  }
}

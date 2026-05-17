package de.eventmodelers;

import org.springframework.boot.SpringApplication;

public class ApplicationStarter {

  public static void main(String[] args) {
    SpringApplication.from(SpringApp::main).run(args);
  }
}

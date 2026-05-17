package de.eventmodelers;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModuleTest {

  @Test
  void verifyModules() {
    var modules = ApplicationModules.of(SpringApp.class);
    modules.verify();
  }
}

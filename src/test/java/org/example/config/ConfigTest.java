package org.example.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class ConfigTest {

  @Test
  void defaults_are_loaded_from_properties() {
    Config cfg = new Config();
    assertThat(cfg.getServerPort()).isEqualTo(8080);
    assertThat(cfg.getJdbcUrl()).contains("jdbc:h2:");
    assertThat(cfg.getDbUser()).isEqualTo("sa");
  }
}


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

  @Test
  void dbName_extracted_from_h2_url() {
    // Create a temporary config class to test URL parsing
    Config cfg = new Config() {
      @Override
      public String getJdbcUrl() {
        return "jdbc:h2:file:./target/demo-db;AUTO_SERVER=TRUE";
      }
    };
    assertThat(cfg.getDbName()).isEqualTo("demo-db");
  }

  @Test
  void dbName_extracted_from_mysql_url() {
    Config cfg = new Config() {
      @Override
      public String getJdbcUrl() {
        return "jdbc:mysql://localhost:3306/my_database?useSSL=false";
      }
    };
    assertThat(cfg.getDbName()).isEqualTo("my_database");
  }

  @Test
  void dbName_extracted_from_postgresql_url() {
    Config cfg = new Config() {
      @Override
      public String getJdbcUrl() {
        return "jdbc:postgresql://localhost:5432/postgres_db";
      }
    };
    assertThat(cfg.getDbName()).isEqualTo("postgres_db");
  }

  @Test
  void dbName_null_when_not_in_url() {
    Config cfg = new Config() {
      @Override
      public String getJdbcUrl() {
        return "jdbc:mysql://localhost:3306/?useSSL=false";
      }
    };
    assertThat(cfg.getDbName()).isNull();
  }

  @Test
  void dbName_extracted_from_mysql_url_without_port() {
    Config cfg = new Config() {
      @Override
      public String getJdbcUrl() {
        return "jdbc:mysql://localhost/testdb";
      }
    };
    assertThat(cfg.getDbName()).isEqualTo("testdb");
  }
}


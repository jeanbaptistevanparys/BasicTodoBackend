package org.example.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

public class Config {
  private final Properties props = new Properties();

  public Config() {
    try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
      if (in != null) props.load(in);
    } catch (IOException ignored) {}
  }

  private String prop(String key, String def) {
    String env = System.getenv(key.toUpperCase().replace('.', '_'));
    if (env != null && !env.isBlank()) return env;
    return props.getProperty(key, def);
  }

  public int getServerPort() { return Integer.parseInt(prop("server.port", "8080")); }

  public String getJdbcUrl() { return prop("jdbc.url", "jdbc:h2:file:./target/demo-db;AUTO_SERVER=TRUE"); }
  public String getDbUser() { return prop("db.user", "sa"); }
  public String getDbPass() { return prop("db.pass", ""); }
  public String getDbDriver() { return prop("db.driver", driverForUrl(getJdbcUrl())); }
  public int getDbPoolSize() { return Integer.parseInt(prop("db.pool.size", "10")); }

  private String driverForUrl(String url) {
    if (url.startsWith("jdbc:h2:")) return "org.h2.Driver";
    if (url.startsWith("jdbc:postgresql:")) return "org.postgresql.Driver";
    if (url.startsWith("jdbc:mysql:")) return "com.mysql.cj.jdbc.Driver";
    return null;
  }

  public String getJwtSecret() { return prop("jwt.secret", "dev-secret-change-me"); }
  public int getJwtExpMinutes() { return Integer.parseInt(prop("jwt.exp.minutes", "60")); }

  public boolean isFlywayRepairOnStart() { return Boolean.parseBoolean(prop("flyway.repairOnStart", "false")); }
  public boolean isFlywayCleanOnStart() { return Boolean.parseBoolean(prop("flyway.cleanOnStart", "false")); }
  public boolean isFlywayValidateOnMigrate() { return Boolean.parseBoolean(prop("flyway.validateOnMigrate", "true")); }
  public String getFlywayLocations() { return prop("flyway.locations", "classpath:db/migration"); }
  public boolean isFlywayAutoRepairOnMismatch() { return Boolean.parseBoolean(prop("flyway.autoRepairOnMismatch", "true")); }
}

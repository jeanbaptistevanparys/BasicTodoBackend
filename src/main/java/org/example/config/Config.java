package org.example.config;

import java.io.IOException;
import java.io.InputStream;
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

  public String getJdbcUrl() { return prop("jdbc.url", "jdbc:mysql://localhost:3306/todoapp"); }
  public String getDbUser() { return prop("db.user", "exampleuser"); }
  public String getDbPass() { return prop("db.pass", "examplepass"); }
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

  /**
   * Determine a sensible default schema for Flyway.
   * - If the property `flyway.defaultSchema` (or env FLYWAY_DEFAULTSCHEMA) is set, return it.
   * - Otherwise attempt to extract the database name from common JDBC URLs (MySQL, Postgres, H2).
   * Returns empty string when no schema could be determined.
   */
  public String getFlywayDefaultSchema() {
    String configured = prop("flyway.defaultSchema", "");
    if (configured != null && !configured.isBlank()) return configured;

    String url = getJdbcUrl();
    if (url == null) return "";
    try {
      int idx = url.indexOf("//");
      if (idx >= 0) {
        String rest = url.substring(idx + 2);
        int slash = rest.indexOf('/');
        if (slash >= 0) {
          String dbAndParams = rest.substring(slash + 1);
          if (dbAndParams.isBlank()) return "";
          int q = dbAndParams.indexOf('?');
          String db = q >= 0 ? dbAndParams.substring(0, q) : dbAndParams;
          if (db.contains("/")) db = db.substring(db.lastIndexOf('/') + 1);
          if (!db.isBlank()) return db;
        }
      }
      if (url.startsWith("jdbc:h2:")) {
        int col = url.indexOf(':', 6); // after jdbc:h2
        String remainder = col >= 0 ? url.substring(col + 1) : url.substring(6);
        if (!remainder.isBlank()) return remainder.replaceAll("[^A-Za-z0-9_]+", "_");
      }
    } catch (Exception e) {
      // ignore parsing failures and return empty
    }
    return "";
  }
}

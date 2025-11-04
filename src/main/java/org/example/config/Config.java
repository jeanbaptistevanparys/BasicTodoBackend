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
  public String getDbName() { return prop("db.name", extractDbNameFromUrl(getJdbcUrl())); }

  private String driverForUrl(String url) {
    if (url.startsWith("jdbc:h2:")) return "org.h2.Driver";
    if (url.startsWith("jdbc:postgresql:")) return "org.postgresql.Driver";
    if (url.startsWith("jdbc:mysql:")) return "com.mysql.cj.jdbc.Driver";
    return null;
  }

  private String extractDbNameFromUrl(String url) {
    if (url == null) return null;
    // Extract database name from JDBC URL
    // For MySQL: jdbc:mysql://host:port/dbname?params or jdbc:mysql://host/dbname
    // For PostgreSQL: jdbc:postgresql://host:port/dbname?params or jdbc:postgresql://host/dbname
    // For H2 file: jdbc:h2:file:./target/demo-db -> "demo-db"
    
    if (url.startsWith("jdbc:mysql://") || url.startsWith("jdbc:postgresql://")) {
      // Find the position after the protocol
      int afterProtocol = url.indexOf("://") + 3;
      // Find the first slash after the host (and optional port)
      int slashAfterHost = url.indexOf('/', afterProtocol);
      if (slashAfterHost == -1) return null;
      int questionMark = url.indexOf('?', slashAfterHost);
      String dbPart = questionMark == -1 
          ? url.substring(slashAfterHost + 1)
          : url.substring(slashAfterHost + 1, questionMark);
      return dbPart.isEmpty() ? null : dbPart;
    }
    
    if (url.startsWith("jdbc:h2:")) {
      // Extract from H2 URLs like jdbc:h2:file:./target/demo-db
      int lastSlash = url.lastIndexOf('/');
      int lastColon = url.lastIndexOf(':');
      int semicolon = url.indexOf(';');
      int start = Math.max(lastSlash, lastColon) + 1;
      int end = semicolon == -1 ? url.length() : semicolon;
      if (start < end) {
        return url.substring(start, end);
      }
    }
    
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

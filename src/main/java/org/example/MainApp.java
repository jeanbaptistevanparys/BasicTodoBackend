package org.example;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import org.example.service.AuthService;
import org.example.config.Config;
import org.example.db.Database;
import org.example.repository.TodoRepository;
import org.example.repository.UserRepository;
import org.example.service.TodoService;
import org.example.service.UserService;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainApp {
  private static final Logger log = LoggerFactory.getLogger(MainApp.class);

  public static void main(String[] args) {
    Config config = new Config();

    HikariConfig hcfg = new HikariConfig();
    hcfg.setJdbcUrl(config.getJdbcUrl());
    hcfg.setUsername(config.getDbUser());
    hcfg.setPassword(config.getDbPass());
    hcfg.setMaximumPoolSize(config.getDbPoolSize());
    String driver = config.getDbDriver();
    if (driver != null) hcfg.setDriverClassName(driver);

    HikariDataSource ds = new HikariDataSource(hcfg);

    Flyway flyway = Flyway.configure()
        .dataSource(ds)
        .locations(config.getFlywayLocations())
        .validateOnMigrate(config.isFlywayValidateOnMigrate())
        .load();

    if (config.isFlywayCleanOnStart()) {
      log.warn("Flyway cleanOnStart=true: dropping and recreating schema");
      flyway.clean();
    }
    if (config.isFlywayRepairOnStart()) {
      log.info("Flyway repairOnStart=true: repairing schema history checksums");
      flyway.repair();
    }

    try {
      flyway.migrate();
    } catch (FlywayValidateException vex) {
      if (config.isFlywayAutoRepairOnMismatch()) {
        log.warn("Flyway validation failed: {}. Attempting automatic repair and re-migrate...", vex.getMessage());
        flyway.repair();
        flyway.migrate();
      } else {
        throw vex;
      }
    }

    Vertx vertx = Vertx.vertx();
    Database database = new Database(ds, vertx);

    UserRepository userRepository = new UserRepository(database);
    UserService userService = new UserService(userRepository);
    AuthService authService = new AuthService(userRepository, config);

    TodoRepository todoRepository = new TodoRepository(database);
    TodoService todoService = new TodoService(todoRepository);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("Shutting down...");
      vertx.close();
      ds.close();
    }));

    DeploymentOptions options = new DeploymentOptions();
    vertx.deployVerticle(() -> new WebVerticle(config, userService, authService, todoService), options, res -> {
      if (res.succeeded()) {
        log.info("WebVerticle deployed: {}", res.result());
      } else {
        log.error("Failed to deploy WebVerticle", res.cause());
        vertx.close();
        ds.close();
      }
    });
  }
}

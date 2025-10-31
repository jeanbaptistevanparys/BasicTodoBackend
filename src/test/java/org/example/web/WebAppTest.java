package org.example.web;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import org.example.WebVerticle;
import org.example.service.AuthService;
import org.example.config.Config;
import org.example.db.Database;
import org.example.repository.TodoRepository;
import org.example.repository.UserRepository;
import org.example.service.TodoService;
import org.example.service.UserService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

public class WebAppTest {
  static class TestConfig extends Config {
    @Override public int getServerPort() { return 0; } // random
    @Override public String getJdbcUrl() { return "jdbc:h2:mem:webapptest;DB_CLOSE_DELAY=-1"; }
    @Override public String getDbUser() { return "sa"; }
    @Override public String getDbPass() { return ""; }
    @Override public String getDbDriver() { return "org.h2.Driver"; }
  }

  private static Vertx vertx;
  private static HikariDataSource ds;
  private static int port;

  @BeforeAll
  static void setup() throws Exception {
    vertx = Vertx.vertx();

    TestConfig cfg = new TestConfig();
    HikariConfig hc = new HikariConfig();
    hc.setJdbcUrl(cfg.getJdbcUrl());
    hc.setUsername(cfg.getDbUser());
    hc.setPassword(cfg.getDbPass());
    hc.setDriverClassName(cfg.getDbDriver());
    ds = new HikariDataSource(hc);

    Flyway flyway = Flyway.configure().dataSource(ds).locations("classpath:db/migration").load();
    flyway.migrate();

    Database database = new Database(ds, vertx);
    UserRepository userRepo = new UserRepository(database);
    UserService userSvc = new UserService(userRepo);
    AuthService authSvc = new AuthService(userRepo, cfg);
    TodoRepository todoRepo = new TodoRepository(database);
    TodoService todoSvc = new TodoService(todoRepo);

    WebVerticle verticle = new WebVerticle(cfg, userSvc, authSvc, todoSvc);
    var deploy = vertx.deployVerticle(() -> verticle, new DeploymentOptions());
    deploy.toCompletionStage().toCompletableFuture().get();

    Object p = vertx.sharedData().getLocalMap("app.info").get("port");
    assertThat(p).isInstanceOf(Integer.class);
    port = (Integer) p;
  }

  @AfterAll
  static void teardown() {
    if (ds != null) ds.close();
    if (vertx != null) vertx.close();
  }

  @Test
  void health_endpoint_works() throws Exception {
    WebClient client = WebClient.create(vertx);
    var fut = client.get(port, "localhost", "/health").as(BodyCodec.string()).send();
    var resp = fut.toCompletionStage().toCompletableFuture().get();
    assertThat(resp.statusCode()).isEqualTo(200);
    assertThat(resp.body()).contains("\"status\":\"ok\"");
  }

  @Test
  void openapi_served() throws Exception {
    WebClient client = WebClient.create(vertx);
    var fut = client.get(port, "localhost", "/openapi.json").as(BodyCodec.string()).send();
    var resp = fut.toCompletionStage().toCompletableFuture().get();
    assertThat(resp.statusCode()).isEqualTo(200);
    assertThat(resp.body()).contains("\"openapi\":");
    assertThat(resp.body()).contains("/api/users");
  }

  @Test
  void users_list_includes_seed_data() throws Exception {
    WebClient client = WebClient.create(vertx);
    var fut = client.get(port, "localhost", "/api/users").as(BodyCodec.string()).send();
    var resp = fut.toCompletionStage().toCompletableFuture().get();
    assertThat(resp.statusCode()).isEqualTo(200);
    assertThat(resp.body()).contains("Alice");
  }
}

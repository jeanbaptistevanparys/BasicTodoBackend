package org.example.web;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
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

public class AuthTodoTest {
  static class TestConfig extends Config {
    @Override public int getServerPort() { return 0; }
    @Override public String getJdbcUrl() { return "jdbc:h2:mem:authtodotest;DB_CLOSE_DELAY=-1"; }
    @Override public String getDbUser() { return "sa"; }
    @Override public String getDbPass() { return ""; }
    @Override public String getDbDriver() { return "org.h2.Driver"; }
    @Override public String getJwtSecret() { return "test-secret"; }
    @Override public int getJwtExpMinutes() { return 60; }
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
    port = (Integer) p;
  }

  @AfterAll
  static void teardown() {
    if (ds != null) ds.close();
    if (vertx != null) vertx.close();
  }

  @Test
  void register_login_and_use_protected_todos() throws Exception {
    WebClient client = WebClient.create(vertx);

    // register
    JsonObject reg = new JsonObject().put("name", "Tester").put("email", "tester@example.com").put("password", "secret12");
    var regResp = client.post(port, "localhost", "/api/auth/register").sendJsonObject(reg).toCompletionStage().toCompletableFuture().get();
    assertThat(regResp.statusCode()).isEqualTo(201);

    // login
    JsonObject login = new JsonObject().put("email", "tester@example.com").put("password", "secret12");
    var loginResp = client.post(port, "localhost", "/api/auth/login").as(BodyCodec.jsonObject()).sendJsonObject(login).toCompletionStage().toCompletableFuture().get();
    assertThat(loginResp.statusCode()).isEqualTo(200);
    String token = loginResp.body().getString("token");
    assertThat(token).isNotBlank();

    // create todo
    JsonObject todoReq = new JsonObject().put("title", "Write tests");
    var createResp = client.post(port, "localhost", "/api/todos")
        .putHeader("Authorization", "Bearer " + token)
        .as(BodyCodec.jsonObject())
        .sendJsonObject(todoReq)
        .toCompletionStage().toCompletableFuture().get();
    assertThat(createResp.statusCode()).isEqualTo(201);
    assertThat(createResp.body().getString("title")).isEqualTo("Write tests");

    // list todos
    var listResp = client.get(port, "localhost", "/api/todos")
        .putHeader("Authorization", "Bearer " + token)
        .as(BodyCodec.string())
        .send()
        .toCompletionStage().toCompletableFuture().get();
    assertThat(listResp.statusCode()).isEqualTo(200);
    assertThat(listResp.body()).contains("Write tests");
  }
}

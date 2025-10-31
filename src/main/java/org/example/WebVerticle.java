package org.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.example.service.AuthService;
import org.example.config.Config;
import org.example.service.TodoService;
import org.example.service.UserService;
import org.example.web.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebVerticle extends AbstractVerticle {
  private static final Logger log = LoggerFactory.getLogger(WebVerticle.class);

  private final Config config;
  private final UserService userService;
  private final AuthService authService;
  private final TodoService todoService;

  public WebVerticle(Config config, UserService userService, AuthService authService, TodoService todoService) {
    this.config = config;
    this.userService = userService;
    this.authService = authService;
    this.todoService = todoService;
  }

  @Override
  public void start(Promise<Void> startPromise) {
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    // Public controllers
    new HealthController().mount(router, vertx);
    new UsersController(userService).mount(router, vertx);
    new ExternalController().mount(router, vertx);
    new AuthController(authService).mount(router, vertx);

    // Protected routes: JWT required
    // Ensure both exact path and sub-paths are protected
    AuthHandler authHandler = new AuthHandler(config);
    router.route("/api/todos").handler(authHandler);
    router.route("/api/todos/*").handler(authHandler);
    new TodoController(todoService).mount(router, vertx);

    int desiredPort = config.getServerPort();
    HttpServer server = vertx.createHttpServer();
    server.requestHandler(router).listen(desiredPort, ar -> {
      if (ar.succeeded()) {
        int actualPort = ar.result().actualPort();
        String serverUrl = "http://localhost:" + actualPort;
        new OpenApiBridge(serverUrl).mount(router, vertx);
        vertx.sharedData().getLocalMap("app.info").put("port", actualPort);
        log.info("HTTP server running on port {}", actualPort);
        log.info("API docs available at {}/docs", serverUrl);
        startPromise.complete();
      } else {
        startPromise.fail(ar.cause());
      }
    });
  }
}

package org.example.web;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.example.service.TodoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TodoController implements Controller {
  private static final Logger log = LoggerFactory.getLogger(TodoController.class);
  private final TodoService todoService;

  public TodoController(TodoService todoService) { this.todoService = todoService; }

  @Override
  public void mount(Router router, Vertx vertx) {
    router.get("/api/todos").handler(ctx -> {
      Long userId = ctx.get("userId");
      if (userId == null) { log.warn("Todos list unauthorized: path={} remote={}", ctx.request().path(), ctx.request().remoteAddress()); unauthorized(ctx); return; }
      todoService.list(userId).onSuccess(list -> {
        JsonArray arr = new JsonArray();
        list.forEach(t -> arr.add(t.toJson()));
        log.info("Todos list userId={} count={}", userId, list.size());
        ctx.response().putHeader("content-type", "application/json").end(arr.encode());
      }).onFailure(err -> {
        log.warn("Todos list failed userId={}: {}", userId, err.getMessage());
        error(ctx, 500, err.getMessage());
      });
    });

    router.post("/api/todos").handler(ctx -> {
      Long userId = ctx.get("userId");
      if (userId == null) { log.warn("Todos create unauthorized: path={} remote={}", ctx.request().path(), ctx.request().remoteAddress()); unauthorized(ctx); return; }
      JsonObject body = ctx.body().asJsonObject();
      String title = body != null ? body.getString("title") : null;
      log.info("Todos create start userId={} title='{}'", userId, title);
      todoService.create(userId, title).onSuccess(t -> {
        log.info("Todos create ok userId={} id={} title='{}'", userId, t.getId(), t.getTitle());
        ctx.response().setStatusCode(201).putHeader("content-type", "application/json").end(t.toJson().encode());
      }).onFailure(err -> {
        log.warn("Todos create failed userId={}: {}", userId, err.getMessage());
        error(ctx, 400, err.getMessage());
      });
    });
  }

  private void error(io.vertx.ext.web.RoutingContext ctx, int code, String msg) {
    ctx.response().setStatusCode(code).putHeader("content-type", "application/json").end(new JsonObject().put("error", msg).encode());
  }
  private void unauthorized(io.vertx.ext.web.RoutingContext ctx) { error(ctx, 401, "Unauthorized"); }
}

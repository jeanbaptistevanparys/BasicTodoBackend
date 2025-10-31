package org.example.web;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.example.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsersController implements Controller {
  private static final Logger log = LoggerFactory.getLogger(UsersController.class);
  private final UserService service;

  public UsersController(UserService service) { this.service = service; }

  @Override
  public void mount(Router router, Vertx vertx) {
    router.get("/api/users").handler(ctx ->
        service.list().onSuccess(list -> {
          JsonArray arr = new JsonArray();
          list.forEach(u -> arr.add(u.toJson()));
          log.info("Users list returned {} users", list.size());
          ctx.response().putHeader("content-type", "application/json").end(arr.encode());
        }).onFailure(err -> {
          log.warn("Users list failed: {}", err.getMessage());
          error(ctx, 500, err.getMessage());
        })
    );

    router.get("/api/users/:id").handler(ctx -> {
      Long id = parseLong(ctx.pathParam("id"));
      if (id == null) { log.warn("Users get: invalid id '{}'", ctx.pathParam("id")); error(ctx, 400, "Invalid id"); return; }
      service.get(id).onSuccess(u -> {
        if (u == null) { log.warn("Users get: not found id={}", id); error(ctx, 404, "Not found"); }
        else { log.info("Users get: id={} found", id); ctx.response().putHeader("content-type", "application/json").end(u.toJson().encode()); }
      }).onFailure(err -> {
        log.warn("Users get failed id={}: {}", id, err.getMessage());
        error(ctx, 500, err.getMessage());
      });
    });
  }

  private void error(io.vertx.ext.web.RoutingContext ctx, int code, String msg) {
    ctx.response().setStatusCode(code).putHeader("content-type", "application/json").end(new JsonObject().put("error", msg).encode());
  }

  private Long parseLong(String s) { try { return Long.parseLong(s); } catch (Exception e) { return null; } }
}

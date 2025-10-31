package org.example.web;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

import java.time.Instant;

public class HealthController implements Controller {
  @Override
  public void mount(Router router, Vertx vertx) {
    router.get("/health").handler(ctx -> {
      JsonObject status = new JsonObject().put("status", "ok").put("time", Instant.now().toString());
      ctx.response().putHeader("content-type", "application/json").end(status.encode());
    });
    router.get("/api/ping").handler(ctx -> ctx.response().end("pong"));
  }
}


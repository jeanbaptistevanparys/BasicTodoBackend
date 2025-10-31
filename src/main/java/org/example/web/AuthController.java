package org.example.web;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.example.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthController implements Controller {
  private static final Logger log = LoggerFactory.getLogger(AuthController.class);
  private final AuthService auth;

  public AuthController(AuthService auth) { this.auth = auth; }

  @Override
  public void mount(Router router, Vertx vertx) {
    router.post("/api/auth/register").handler(ctx -> {
      JsonObject body = ctx.body().asJsonObject();
      String name = body != null ? body.getString("name") : null;
      String email = body != null ? body.getString("email") : null;
      String password = body != null ? body.getString("password") : null;
      // Avoid logging passwords; log minimal context
      log.info("Register attempt for {} from {}", email, ctx.request().remoteAddress());
      auth.register(name, email, password).onSuccess(row -> {
        JsonObject out = new JsonObject()
            .put("id", row.getLong("id"))
            .put("name", row.getString("name"))
            .put("email", row.getString("email"))
            .put("created_at", row.getValue("created_at"));
        log.info("Register success for {} (id={})", email, out.getLong("id"));
        ctx.response().setStatusCode(201).putHeader("content-type", "application/json").end(out.encode());
      }).onFailure(err -> {
        log.warn("Register failed for {}: {}", email, err.getMessage());
        error(ctx, 400, err.getMessage());
      });
    });

    router.post("/api/auth/login").handler(ctx -> {
      JsonObject body = ctx.body().asJsonObject();
      String email = body != null ? body.getString("email") : null;
      String password = body != null ? body.getString("password") : null;
      // Safe logging: do not log the password
      log.info("Login attempt for {} at {} from {}", email, ctx.request().path(), ctx.request().remoteAddress());
      auth.login(email, password).onSuccess(token -> {
        log.info("Login success for {}", email);
        ctx.response().putHeader("content-type", "application/json").end(new JsonObject().put("token", token).encode());
      }).onFailure(err -> {
        log.warn("Login failed for {}: {}", email, err.getMessage());
        error(ctx, 401, "invalid credentials");
      });
    });
  }

  private void error(io.vertx.ext.web.RoutingContext ctx, int code, String msg) {
    ctx.response().setStatusCode(code).putHeader("content-type", "application/json").end(new JsonObject().put("error", msg).encode());
  }
}

package org.example.web;

import io.vertx.ext.web.RoutingContext;
import org.example.config.Config;
import org.example.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class AuthHandler implements io.vertx.core.Handler<RoutingContext> {
  private static final Logger log = LoggerFactory.getLogger(AuthHandler.class);
  private final Config config;

  public AuthHandler(Config config) { this.config = config; }

  @Override
  public void handle(RoutingContext ctx) {
    String auth = ctx.request().getHeader("Authorization");
    if (auth == null || !auth.startsWith("Bearer ")) {
      log.warn("Auth denied: missing bearer token path={} remote={}", ctx.request().path(), ctx.request().remoteAddress());
      unauthorized(ctx, "Missing bearer token");
      return;
    }
    String token = auth.substring("Bearer ".length());
    try {
      Map<String,Object> claims = JwtUtil.verify(token, config.getJwtSecret());
      Object sub = claims.get("sub");
      if (sub == null) {
        log.warn("Auth denied: no sub claim path={} remote={}", ctx.request().path(), ctx.request().remoteAddress());
        unauthorized(ctx, "Invalid token");
        return;
      }
      long userId = Long.parseLong(sub.toString());
      ctx.put("userId", userId);
      log.info("Auth ok: userId={} path={} remote={}", userId, ctx.request().path(), ctx.request().remoteAddress());
      ctx.next();
    } catch (RuntimeException ex) {
      log.warn("Auth denied: invalid token path={} remote={} reason={}", ctx.request().path(), ctx.request().remoteAddress(), ex.getMessage());
      unauthorized(ctx, "Invalid token");
    }
  }

  private void unauthorized(RoutingContext ctx, String msg) {
    ctx.response().setStatusCode(401).putHeader("content-type", "application/json").end(new io.vertx.core.json.JsonObject().put("error", msg).encode());
  }
}

package org.example.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.example.config.Config;
import org.example.repository.UserRepository;
import org.example.utils.PasswordUtil;
import org.example.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class AuthService {
  private static final Logger log = LoggerFactory.getLogger(AuthService.class);
  private final UserRepository users;
  private final Config config;

  public AuthService(UserRepository users, Config config) {
    this.users = users;
    this.config = config;
  }

  public Future<JsonObject> register(String name, String email, String password) {
    log.info("Register start: email={}", email);
    if (name == null || name.isBlank()) return Future.failedFuture("name is required");
    if (email == null || email.isBlank()) return Future.failedFuture("email is required");
    if (password == null || password.length() < 6) return Future.failedFuture("password must be at least 6 chars");
    return users.findByEmail(email).compose(existing -> {
      if (existing != null) {
        log.warn("Register failed: email already in use: {}", email);
        return Future.failedFuture("email already in use");
      }
      String hash = PasswordUtil.hash(password);
      log.debug("Register creating user: email={} (hashLen={})", email, hash.length());
      return users.createWithPassword(name, email, hash)
          .compose(id -> users.findByEmail(email))
          .onSuccess(row -> log.info("Register success: email={} id={}", email, row != null ? coerceLong(row, "id", "ID") : null))
          .onFailure(err -> log.warn("Register DB failure for {}: {}", email, err.getMessage()));
    });
  }

  public Future<String> login(String email, String password) {
    log.info("Login start: email={}", email);
    if (email == null || password == null) return Future.failedFuture("invalid credentials");
    return users.findByEmail(email).compose(row -> {
      if (row == null) {
        log.warn("Login failed: user not found: {}", email);
        return Future.failedFuture("invalid credentials");
      }
      String stored = row.getString("password_hash");
      if (stored == null) stored = row.getString("password_hash");
      if (stored == null) {
        log.warn("Login failed: no password set for {} (seeded user?)", email);
        return Future.failedFuture("invalid credentials");
      }
      boolean ok = PasswordUtil.verify(password, stored);

      if (!ok) {
        log.warn("Login failed: bad password for {}", email);
        return Future.failedFuture("invalid credentials");
      }
      Long idBoxed = coerceLong(row, "id", "ID");
      if (idBoxed == null) {
        log.warn("Login failed: user id missing for {}", email);
        return Future.failedFuture("invalid credentials");
      }
      long userId = idBoxed;
      long exp = Instant.now().plus(config.getJwtExpMinutes(), ChronoUnit.MINUTES).getEpochSecond();
      Map<String, Object> claims = new HashMap<>();
      claims.put("sub", String.valueOf(userId));
      claims.put("exp", exp);
      String token = JwtUtil.sign(claims, config.getJwtSecret());
      log.info("Login success: email={} sub={} exp={}s", email, userId, exp);
      return Future.succeededFuture(token);
    });
  }

  private static Long coerceLong(JsonObject row, String... keys) {
    for (String k : keys) {
      Long v = row.getLong(k);
      if (v != null) return v;
      Integer i = row.getInteger(k);
      if (i != null) return i.longValue();
      Object o = row.getValue(k);
      if (o instanceof Number) return ((Number) o).longValue();
      if (o != null) {
        try { return Long.parseLong(o.toString()); } catch (Exception ignored) {}
      }
    }
    return null;
  }
}

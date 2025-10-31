package org.example.repository;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.example.db.Database;

import java.util.List;

public class UserRepository implements UserRepositoryPort {
  private final Database db;

  public UserRepository(Database db) { this.db = db; }

  @Override
  public Future<List<JsonObject>> list() {
    return db.query("SELECT id, name, email, created_at FROM users ORDER BY id");
  }

  @Override
  public Future<JsonObject> findById(long id) {
    return db.fetchOne("SELECT id, name, email, created_at FROM users WHERE id = ?", id);
  }

  @Override
  public Future<Long> create(String name, String email) {
    return db.insert("INSERT INTO users(name, email, created_at) VALUES(?, ?, CURRENT_TIMESTAMP())", name, email);
  }

  public Future<JsonObject> findByEmail(String email) {
    return db.fetchOne("SELECT id, name, email, created_at, password_hash FROM users WHERE email = ?", email);
  }

  public Future<Long> createWithPassword(String name, String email, String passwordHash) {
    return db.insert("INSERT INTO users(name, email, password_hash, created_at) VALUES(?, ?, ?, CURRENT_TIMESTAMP())", name, email, passwordHash);
  }
}

package org.example.repository;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.example.db.Database;

import java.util.List;

public class TodoRepository {
  private final Database db;

  public TodoRepository(Database db) { this.db = db; }

  public Future<List<JsonObject>> listByUser(long userId) {
    return db.query("SELECT id, user_id, title, completed, created_at FROM todos WHERE user_id = ? ORDER BY id", userId);
  }

  public Future<Long> create(long userId, String title) {
    return db.insert("INSERT INTO todos(user_id, title, completed, created_at) VALUES(?, ?, FALSE, CURRENT_TIMESTAMP())", userId, title);
  }

  public Future<Integer> markCompleted(long userId, long id, boolean completed) {
    return db.executeUpdate("UPDATE todos SET completed = ? WHERE id = ? AND user_id = ?", completed, id, userId);
  }

  public Future<JsonObject> findByIdForUser(long userId, long id) {
    return db.fetchOne("SELECT id, user_id, title, completed, created_at FROM todos WHERE id = ? AND user_id = ?", id, userId);
  }
}

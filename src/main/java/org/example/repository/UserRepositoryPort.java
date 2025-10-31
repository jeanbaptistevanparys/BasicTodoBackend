package org.example.repository;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface UserRepositoryPort {
  Future<java.util.List<io.vertx.core.json.JsonObject>> list();
  Future<JsonObject> findById(long id);
  Future<Long> create(String name, String email);
}


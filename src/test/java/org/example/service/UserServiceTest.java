package org.example.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.example.repository.UserRepositoryPort;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserServiceTest {
  static class FakeRepo implements UserRepositoryPort {
    List<JsonObject> store = new ArrayList<>();
    long idSeq = 1;

    @Override
    public Future<List<JsonObject>> list() { return Future.succeededFuture(store); }
    @Override
    public Future<JsonObject> findById(long id) {
      return Future.succeededFuture(store.stream().filter(j -> j.getLong("id") == id).findFirst().orElse(null));
    }
    @Override
    public Future<Long> create(String name, String email) {
      long id = idSeq++;
      store.add(new JsonObject().put("id", id).put("name", name).put("email", email).put("created_at", "2024-01-01T00:00:00Z"));
      return Future.succeededFuture(id);
    }
  }

  @Test
  void validate_and_create_user() {
    FakeRepo repo = new FakeRepo();
    UserService svc = new UserService(repo);

    var f1 = svc.create("", "x@x");
    assertThat(f1.failed()).isTrue();

    var f2 = svc.create("Alice", "");
    assertThat(f2.failed()).isTrue();

    var f3 = svc.create("Alice", "alice@example.com").result();
    assertThat(f3.getId()).isEqualTo(1L);

    var list = svc.list().result();
    assertThat(list).hasSize(1);
    assertThat(list.get(0).getName()).isEqualTo("Alice");
  }
}


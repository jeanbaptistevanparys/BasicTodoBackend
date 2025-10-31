package org.example.domain;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {
  @Test
  void fromRow_maps_fields_case_insensitively() {
    JsonObject row = new JsonObject()
        .put("ID", 42)
        .put("NAME", "Alice")
        .put("EMAIL", "alice@example.com")
        .put("CREATED_AT", Instant.parse("2024-01-01T00:00:00Z").toString());

    User u = User.fromRow(row);
    assertThat(u).isNotNull();
    assertThat(u.getId()).isEqualTo(42L);
    assertThat(u.getName()).isEqualTo("Alice");
    assertThat(u.getEmail()).isEqualTo("alice@example.com");
    assertThat(u.getCreatedAt()).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));
  }
}


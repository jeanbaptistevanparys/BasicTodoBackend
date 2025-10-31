package org.example.openapi;

import io.vertx.core.json.JsonObject;
import org.example.OpenApiSpec;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OpenApiSpecTest {
  @Test
  void spec_contains_core_paths_and_info() {
    JsonObject spec = OpenApiSpec.build("http://localhost:1234");
    assertThat(spec.getString("openapi")).startsWith("3.");
    JsonObject info = spec.getJsonObject("info");
    assertThat(info).isNotNull();
    assertThat(info.getString("title")).contains("Vert.x");
    JsonObject paths = spec.getJsonObject("paths");
    assertThat(paths).isNotNull();
    assertThat(paths.containsKey("/api/users")).isTrue();
    assertThat(paths.containsKey("/health")).isTrue();
  }
}


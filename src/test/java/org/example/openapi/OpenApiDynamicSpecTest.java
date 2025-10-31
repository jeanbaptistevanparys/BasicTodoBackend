package org.example.openapi;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.core.json.JsonObject;
import org.example.OpenApiSpec;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OpenApiDynamicSpecTest {
  @Test
  void dynamic_spec_contains_routes_from_router() {
    Vertx vertx = Vertx.vertx();
    try {
      Router router = Router.router(vertx);
      router.get("/api/new").handler(ctx -> ctx.response().end("ok"));
      router.post("/api/new").handler(ctx -> ctx.response().end("ok"));

      JsonObject spec = OpenApiSpec.build("http://localhost:1234", router);
      JsonObject paths = spec.getJsonObject("paths");

      assertThat(paths).isNotNull();
      assertThat(paths.containsKey("/api/new")).isTrue();

      JsonObject item = paths.getJsonObject("/api/new");
      assertThat(item.getJsonObject("get")).isNotNull();
      assertThat(item.getJsonObject("post")).isNotNull();
    } finally {
      vertx.close();
    }
  }
}


package org.example.web;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import org.example.OpenApiSpec;

public class OpenApiBridge implements Controller {
  private final String serverUrl;

  public OpenApiBridge(String serverUrl) { this.serverUrl = serverUrl; }

  @Override
  public void mount(Router router, Vertx vertx) {
    router.get("/openapi.json").handler(ctx -> {
      JsonObject spec = OpenApiSpec.build(serverUrl, router);
      ctx.response().putHeader("content-type", "application/json").end(spec.encode());
    });
    router.route("/docs/*").handler(StaticHandler.create("webroot"));
  }
}

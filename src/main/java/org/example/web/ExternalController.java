package org.example.web;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class ExternalController implements Controller {
  private static final Logger log = LoggerFactory.getLogger(ExternalController.class);
  @Override
  public void mount(Router router, Vertx vertx) {
    WebClient webClient = WebClient.create(vertx);

    router.get("/api/external").handler(ctx -> {
      String url = ctx.request().getParam("url");
      if (url == null || url.isBlank()) { log.warn("External: missing url param from {}", ctx.request().remoteAddress()); error(ctx, 400, "url query param required"); return; }
      try {
        URI u = URI.create(url);
        if (u.getScheme() == null || !u.getScheme().startsWith("http")) throw new IllegalArgumentException();
      } catch (Exception e) { log.warn("External: invalid url '{}' from {}", url, ctx.request().remoteAddress()); error(ctx, 400, "invalid url"); return; }
      log.info("External: fetching '{}'", url);
      webClient.getAbs(url).send(ar -> {
        if (ar.succeeded()) {
          HttpResponse<io.vertx.core.buffer.Buffer> resp = ar.result();
          JsonObject headers = new JsonObject();
          resp.headers().forEach(h -> headers.put(h.getKey(), h.getValue()));
          JsonObject out = new JsonObject()
              .put("status", resp.statusCode())
              .put("headers", headers)
              .put("bodyLength", resp.bodyAsBuffer() != null ? resp.bodyAsBuffer().length() : 0);
          log.info("External: ok url='{}' status={} bytes={}", url, resp.statusCode(), out.getInteger("bodyLength"));
          ctx.response().putHeader("content-type", "application/json").end(out.encode());
        } else {
          log.warn("External: fail url='{}' err={}", url, ar.cause().getMessage());
          error(ctx, 500, ar.cause().getMessage());
        }
      });
    });
  }

  private void error(io.vertx.ext.web.RoutingContext ctx, int code, String msg) {
    ctx.response().setStatusCode(code).putHeader("content-type", "application/json").end(new JsonObject().put("error", msg).encode());
  }
}

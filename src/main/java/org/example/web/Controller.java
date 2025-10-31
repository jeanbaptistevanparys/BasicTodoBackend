package org.example.web;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

public interface Controller {
  void mount(Router router, Vertx vertx);
}

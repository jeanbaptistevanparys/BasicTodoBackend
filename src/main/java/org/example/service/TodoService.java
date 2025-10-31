package org.example.service;

import io.vertx.core.Future;
import org.example.domain.Todo;
import org.example.repository.TodoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class TodoService {
  private static final Logger log = LoggerFactory.getLogger(TodoService.class);
  private final TodoRepository repo;

  public TodoService(TodoRepository repo) { this.repo = repo; }

  public Future<List<Todo>> list(long userId) {
    log.info("TodoService.list start userId={}", userId);
    return repo.listByUser(userId).map(rows -> rows.stream().map(Todo::fromRow).collect(Collectors.toList()))
        .onSuccess(list -> log.info("TodoService.list ok userId={} count={}", userId, list.size()))
        .onFailure(err -> log.warn("TodoService.list fail userId={}: {}", userId, err.getMessage()));
  }

  public Future<Todo> create(long userId, String title) {
    log.info("TodoService.create start userId={} title='{}'", userId, title);
    if (title == null || title.isBlank()) return Future.failedFuture("title is required");
    return repo.create(userId, title)
        .compose(id -> repo.findByIdForUser(userId, id))
        .map(Todo::fromRow)
        .onSuccess(t -> log.info("TodoService.create ok userId={} id={}", userId, t != null ? t.getId() : null))
        .onFailure(err -> log.warn("TodoService.create fail userId={}: {}", userId, err.getMessage()));
  }
}

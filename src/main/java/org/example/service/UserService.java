package org.example.service;

import io.vertx.core.Future;
import org.example.domain.User;
import org.example.repository.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class UserService {
  private static final Logger log = LoggerFactory.getLogger(UserService.class);
  private final UserRepositoryPort repo;

  public UserService(UserRepositoryPort repo) { this.repo = repo; }

  public Future<List<User>> list() {
    log.info("UserService.list start");
    return repo.list().map(rows -> rows.stream().map(User::fromRow).collect(Collectors.toList()))
        .onSuccess(list -> log.info("UserService.list ok count={}", list.size()))
        .onFailure(err -> log.warn("UserService.list fail: {}", err.getMessage()));
  }

  public Future<User> get(long id) {
    log.info("UserService.get start id={}", id);
    return repo.findById(id).map(User::fromRow)
        .onSuccess(u -> log.info("UserService.get ok id={} found={}", id, u != null))
        .onFailure(err -> log.warn("UserService.get fail id={}: {}", id, err.getMessage()));
  }

  public Future<User> create(String name, String email) {
    log.info("UserService.create start name={} email={}", name, email);
    if (name == null || name.isBlank()) return Future.failedFuture("name is required");
    if (email == null || email.isBlank()) return Future.failedFuture("email is required");
    return repo.create(name, email)
        .compose(id -> repo.findById(id))
        .map(User::fromRow)
        .onSuccess(u -> log.info("UserService.create ok id={}", u != null ? u.getId() : null))
        .onFailure(err -> log.warn("UserService.create fail email={}: {}", email, err.getMessage()));
  }
}

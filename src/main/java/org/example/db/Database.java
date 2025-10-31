package org.example.db;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Database {
  private static final Logger log = LoggerFactory.getLogger(Database.class);
  private final DataSource ds;
  private final Vertx vertx;

  public Database(DataSource ds, Vertx vertx) {
    this.ds = ds;
    this.vertx = vertx;
  }

  public Future<List<JsonObject>> query(String sql, Object... params) {
    return executeBlocking(() -> {
      long start = System.currentTimeMillis();
      try (Connection c = ds.getConnection(); PreparedStatement ps = prepare(c, sql, params); ResultSet rs = ps.executeQuery()) {
        List<JsonObject> out = new ArrayList<>();
        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();
        while (rs.next()) {
          JsonObject obj = new JsonObject();
          for (int i = 1; i <= cols; i++) {
            String name = md.getColumnLabel(i);
            Object val = rs.getObject(i);
            // Normalize column labels to lower-case to provide consistent keys (e.g. id, name, password_hash)
            if (name != null) name = name.toLowerCase();
            obj.put(name, val);
          }
          out.add(obj);
        }
        long took = System.currentTimeMillis() - start;
        log.info("DB query ok ({} ms): sql='{}' params={}", took, compact(sql), summarize(params));
        return out;
      } catch (Exception e) {
        long took = System.currentTimeMillis() - start;
        log.warn("DB query fail ({} ms): sql='{}' params={} err={}", took, compact(sql), summarize(params), e.toString());
        throw e;
      }
    });
  }

  public Future<JsonObject> fetchOne(String sql, Object... params) {
    return query(sql, params).map(list -> list.isEmpty() ? null : list.get(0));
  }

  public Future<Long> insert(String sql, Object... params) {
    return executeBlocking(() -> {
      long start = System.currentTimeMillis();
      try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        bind(ps, params);
        int updated = ps.executeUpdate();
        Long id = null;
        try (ResultSet keys = ps.getGeneratedKeys()) {
          if (keys.next()) id = keys.getLong(1);
        }
        long took = System.currentTimeMillis() - start;
        log.info("DB insert ok ({} ms): sql='{}' params={} rows={} id={}", took, compact(sql), summarize(params), updated, id);
        return id;
      } catch (Exception e) {
        long took = System.currentTimeMillis() - start;
        log.warn("DB insert fail ({} ms): sql='{}' params={} err={}", took, compact(sql), summarize(params), e.toString());
        throw e;
      }
    });
  }

  public Future<Integer> executeUpdate(String sql, Object... params) {
    return executeBlocking(() -> {
      long start = System.currentTimeMillis();
      try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
        bind(ps, params);
        int updated = ps.executeUpdate();
        long took = System.currentTimeMillis() - start;
        log.info("DB update ok ({} ms): sql='{}' params={} rows={}", took, compact(sql), summarize(params), updated);
        return updated;
      } catch (Exception e) {
        long took = System.currentTimeMillis() - start;
        log.warn("DB update fail ({} ms): sql='{}' params={} err={}", took, compact(sql), summarize(params), e.toString());
        throw e;
      }
    });
  }

  private PreparedStatement prepare(Connection c, String sql, Object... params) throws SQLException {
    PreparedStatement ps = c.prepareStatement(sql);
    bind(ps, params);
    return ps;
  }

  private void bind(PreparedStatement ps, Object... params) throws SQLException {
    if (params == null) return;
    for (int i = 0; i < params.length; i++) {
      ps.setObject(i + 1, params[i]);
    }
  }

  private <T> Future<T> executeBlocking(BlockingOperation<T> op) {
    return vertx.executeBlocking(promise -> {
      try {
        promise.complete(op.get());
      } catch (Exception e) {
        promise.fail(e);
      }
    }, false);
  }

  private interface BlockingOperation<T> {
    T get() throws Exception;
  }

  private static String summarize(Object[] params) {
    if (params == null) return "[]";
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < params.length; i++) {
      Object p = params[i];
      String v = (p == null) ? "null" : String.valueOf(p);
      // mask long secrets heuristically
      if (v.length() > 100) v = v.substring(0, 97) + "...";
      sb.append(v);
      if (i < params.length - 1) sb.append(", ");
    }
    sb.append("]");
    return sb.toString();
  }

  private static String compact(String sql) {
    return sql == null ? null : sql.replaceAll("\n+", " ").trim();
  }
}

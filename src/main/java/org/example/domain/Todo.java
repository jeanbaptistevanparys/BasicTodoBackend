package org.example.domain;

import io.vertx.core.json.JsonObject;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;

public class Todo {
  private Long id;
  private Long userId;
  private String title;
  private boolean completed;
  private Instant createdAt;

  public static Todo fromRow(JsonObject row) {
    Todo t = new Todo();
    t.id = row.getLong("ID") != null ? row.getLong("ID") : row.getLong("id");
    t.userId = row.getLong("USER_ID") != null ? row.getLong("USER_ID") : row.getLong("user_id");
    t.title = row.getString("TITLE") != null ? row.getString("TITLE") : row.getString("title");
    Object comp = row.getValue("COMPLETED") != null ? row.getValue("COMPLETED") : row.getValue("completed");
    t.completed = comp instanceof Boolean ? (Boolean) comp : (comp != null && Integer.valueOf(comp.toString()) != 0);
    Object ca = row.getValue("CREATED_AT") != null ? row.getValue("CREATED_AT") : row.getValue("created_at");
    t.createdAt = parseInstantSafely(ca);
    return t;
  }

  private static Instant parseInstantSafely(Object ca) {
    if (ca == null) return null;
    String s = ca.toString();
    // Try ISO instant first (expects 'T' and offset/Z)
    try {
      return Instant.parse(s);
    } catch (DateTimeParseException ignored) {
    }
    // Convert space to 'T' for DB timestamps like 'yyyy-MM-dd HH:mm:ss[.fraction]'
    String s2 = s.replace(' ', 'T');
    // If it already contains zone info (Z or ±HH:MM) try Instant.parse again
    boolean hasZone = s2.endsWith("Z") || s2.matches(".*[\\+\\-]\\d{2}:?\\d{2}$");
    try {
      if (hasZone) {
        return Instant.parse(s2);
      }
      // Parse as local date-time and treat it as UTC (DB TIMESTAMP without timezone)
      LocalDateTime ldt = LocalDateTime.parse(s2, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
      return ldt.toInstant(ZoneOffset.UTC);
    } catch (DateTimeParseException e) {
      // parsing failed - be tolerant and return null
      return null;
    }
  }

  public JsonObject toJson() {
    return new JsonObject()
        .put("id", id)
        .put("user_id", userId)
        .put("title", title)
        .put("completed", completed)
        .put("created_at", createdAt != null ? createdAt.toString() : null);
  }

  // getters
  public Long getId() { return id; }
  public Long getUserId() { return userId; }
  public String getTitle() { return title; }
  public boolean isCompleted() { return completed; }
  public Instant getCreatedAt() { return createdAt; }
}

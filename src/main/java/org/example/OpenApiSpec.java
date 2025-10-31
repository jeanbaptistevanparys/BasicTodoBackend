package org.example;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

public class OpenApiSpec {
  public static JsonObject build(String serverUrl) {
    // Static fallback (used by tests and to enrich dynamic generation)
    return new JsonObject()
        .put("openapi", "3.0.3")
        .put("info", new JsonObject()
            .put("title", "Vert.x Sample API")
            .put("version", "1.0.0")
            .put("description", "Sample Vert.x API with generated OpenAPI"))
        .put("servers", new JsonArray().add(new JsonObject().put("url", serverUrl)))
        .put("paths", paths())
        .put("components", components());
  }

  // New entry point for dynamic generation based on the current Router
  public static JsonObject build(String serverUrl, Router router) {
    JsonObject spec = new JsonObject()
        .put("openapi", "3.0.3")
        .put("info", new JsonObject()
            .put("title", "Vert.x Sample API")
            .put("version", "1.0.0")
            .put("description", "Sample Vert.x API with generated OpenAPI"))
        .put("servers", new JsonArray().add(new JsonObject().put("url", serverUrl)))
        .put("components", components());

    JsonObject dynamicPaths = (router != null) ? buildPathsFromRouter(router) : paths();

    // Enrich dynamic paths with details from the static spec when available
    JsonObject staticPaths = paths();
    for (String p : dynamicPaths.fieldNames()) {
      JsonObject dynItem = dynamicPaths.getJsonObject(p);
      JsonObject statItem = staticPaths.getJsonObject(p);
      if (dynItem == null) continue;
      if (statItem == null) continue;

      for (String method : new String[]{"get", "post", "put", "delete", "patch", "head", "options"}) {
        JsonObject dynOp = dynItem.getJsonObject(method);
        JsonObject statOp = statItem.getJsonObject(method);
        if (dynOp == null || statOp == null) continue;

        // Prefer static summary if present
        if (statOp.containsKey("summary")) dynOp.put("summary", statOp.getString("summary"));
        // Parameters and requestBody copied if missing
        if (statOp.containsKey("parameters") && !dynOp.containsKey("parameters")) dynOp.put("parameters", statOp.getJsonArray("parameters"));
        if (statOp.containsKey("requestBody") && !dynOp.containsKey("requestBody")) dynOp.put("requestBody", statOp.getJsonObject("requestBody"));
        // Responses: use static if present (richer than default 200 OK)
        if (statOp.containsKey("responses")) dynOp.put("responses", statOp.getJsonObject("responses"));
        // Security: keep dynamic if present; otherwise copy
        if (!dynOp.containsKey("security") && statOp.containsKey("security")) dynOp.put("security", statOp.getJsonArray("security"));
        dynItem.put(method, dynOp);
      }

      dynamicPaths.put(p, dynItem);
    }

    spec.put("paths", dynamicPaths);
    return spec;
  }

  // Build minimal path items by introspecting Vert.x Router routes
  private static JsonObject buildPathsFromRouter(Router router) {
    JsonObject paths = new JsonObject();

    for (Route route : router.getRoutes()) {
      String path = route.getPath();
      // Skip routes without path (e.g., global handlers) and internal/docs endpoints
      if (path == null || path.isBlank()) continue;
      if (path.equals("/openapi.json") || path.startsWith("/docs")) continue;

      // Only include HTTP method-bound routes
      java.util.Set<HttpMethod> methods = route.methods();
      if (methods == null || methods.isEmpty()) continue;

      String oasPath = toOasPath(path);
      JsonObject pathItem = paths.getJsonObject(oasPath);
      if (pathItem == null) pathItem = new JsonObject();

      for (HttpMethod method : methods) {
        String opKey = toOperationKey(method);
        if (opKey == null) continue;

        JsonObject op = new JsonObject()
            .put("summary", method.name() + " " + path)
            .put("responses", new JsonObject().put("200", json().put("description", "OK")));

        // Heuristic: mark JWT security for /api/todos endpoints
        if (path.startsWith("/api/todos")) {
          JsonArray bearer = new JsonArray().add(new JsonObject().put("bearerAuth", new JsonObject()));
          op.put("security", bearer);
        }

        // Add path parameters if present in the path template
        JsonArray params = extractPathParams(oasPath);
        if (params != null && !params.isEmpty()) op.put("parameters", params);

        pathItem.put(opKey, op);
      }

      if (!pathItem.isEmpty()) {
        paths.put(oasPath, pathItem);
      }
    }

    return paths;
  }

  private static JsonArray extractPathParams(String oasPath) {
    JsonArray params = new JsonArray();
    int idx = 0;
    while (idx < oasPath.length()) {
      int s = oasPath.indexOf('{', idx);
      if (s < 0) break;
      int e = oasPath.indexOf('}', s + 1);
      if (e < 0) break;
      String name = oasPath.substring(s + 1, e);
      // Heuristic: id-like params are integers, others strings
      JsonObject schema = new JsonObject().put("type", name.matches(".*(?i)id$") ? "integer" : "string");
      params.add(new JsonObject().put("name", name).put("in", "path").put("required", true).put("schema", schema));
      idx = e + 1;
    }
    return params;
  }

  private static String toOasPath(String vertxPath) {
    // Convert Vert.x ":param" segments to OAS "{param}"; keep other chars as-is
    String converted = vertxPath.replaceAll(":([a-zA-Z0-9_]+)", "{$1}");
    // Remove trailing wildcard used for mounting sub-routes (e.g., /api/todos*)
    if (converted.endsWith("*")) converted = converted.substring(0, converted.length() - 1);
    return converted;
  }

  private static String toOperationKey(HttpMethod method) {
    String name = method != null ? method.name() : null;
    if (name == null) return null;
    switch (name) {
      case "GET": return "get";
      case "POST": return "post";
      case "PUT": return "put";
      case "DELETE": return "delete";
      case "PATCH": return "patch";
      case "HEAD": return "head";
      case "OPTIONS": return "options";
      default: return null;
    }
  }

  private static JsonObject paths() {
    JsonObject paths = new JsonObject();

    paths.put("/health", new JsonObject()
        .put("get", new JsonObject()
            .put("summary", "Health check")
            .put("responses", new JsonObject()
                .put("200", json().put("description", "OK").put("content", appJson(schemaRef("Health")))))));

    paths.put("/api/ping", new JsonObject()
        .put("get", new JsonObject()
            .put("summary", "Ping endpoint")
            .put("responses", new JsonObject().put("200", json().put("description", "pong")))));

    // Users: list and get-by-id only (no POST)
    paths.put("/api/users", new JsonObject()
        .put("get", new JsonObject()
            .put("summary", "List users")
            .put("responses", new JsonObject()
                .put("200", json().put("description", "Users list").put("content", appJson(arrayOf(schemaRef("User"))))))
        )
    );

    paths.put("/api/users/{id}", new JsonObject()
        .put("get", new JsonObject()
            .put("summary", "Get user by id")
            .put("parameters", new JsonArray().add(new JsonObject()
                .put("name", "id").put("in", "path").put("required", true)
                .put("schema", new JsonObject().put("type", "integer"))))
            .put("responses", new JsonObject()
                .put("200", json().put("description", "User").put("content", appJson(schemaRef("User"))))
                .put("404", json().put("description", "Not found"))
            )));

    paths.put("/api/external", new JsonObject()
        .put("get", new JsonObject()
            .put("summary", "Fetch external URL metadata")
            .put("parameters", new JsonArray().add(new JsonObject()
                .put("name", "url").put("in", "query").put("required", true)
                .put("schema", new JsonObject().put("type", "string"))))
            .put("responses", new JsonObject()
                .put("200", json().put("description", "Response info").put("content", appJson(schemaRef("ExternalInfo")))))
        ));

    // Auth
    JsonArray bearer = new JsonArray().add(new JsonObject().put("bearerAuth", new JsonObject()));
    paths.put("/api/auth/register", new JsonObject()
        .put("post", new JsonObject()
            .put("summary", "Register a new user")
            .put("requestBody", new JsonObject().put("required", true)
                .put("content", appJson(schemaRef("RegisterRequest"))))
            .put("responses", new JsonObject()
                .put("201", json().put("description", "Created").put("content", appJson(schemaRef("User")))))
        ));

    paths.put("/api/auth/login", new JsonObject()
        .put("post", new JsonObject()
            .put("summary", "Login and get a JWT token")
            .put("requestBody", new JsonObject().put("required", true)
                .put("content", appJson(schemaRef("LoginRequest"))))
            .put("responses", new JsonObject()
                .put("200", json().put("description", "Token").put("content", appJson(schemaRef("TokenResponse")))))
        ));

    // Todos (protected)
    paths.put("/api/todos", new JsonObject()
        .put("get", new JsonObject()
            .put("summary", "List todos for the current user")
            .put("security", bearer)
            .put("responses", new JsonObject()
                .put("200", json().put("description", "Todos").put("content", appJson(arrayOf(schemaRef("Todo")))))))
        .put("post", new JsonObject()
            .put("summary", "Create todo for the current user")
            .put("security", bearer)
            .put("requestBody", new JsonObject().put("required", true)
                .put("content", appJson(schemaRef("NewTodo"))))
            .put("responses", new JsonObject()
                .put("201", json().put("description", "Created").put("content", appJson(schemaRef("Todo")))))
        ));

    return paths;
  }

  private static JsonObject components() {
    JsonObject schemas = new JsonObject();

    schemas.put("Health", new JsonObject()
        .put("type", "object")
        .put("properties", new JsonObject()
            .put("status", new JsonObject().put("type", "string"))
            .put("time", new JsonObject().put("type", "string"))
        ));

    schemas.put("User", new JsonObject()
        .put("type", "object")
        .put("properties", new JsonObject()
            .put("id", new JsonObject().put("type", "integer"))
            .put("name", new JsonObject().put("type", "string"))
            .put("email", new JsonObject().put("type", "string"))
            .put("created_at", new JsonObject().put("type", "string"))
        ));

    schemas.put("ExternalInfo", new JsonObject()
        .put("type", "object")
        .put("properties", new JsonObject()
            .put("status", new JsonObject().put("type", "integer"))
            .put("headers", new JsonObject().put("type", "object").put("additionalProperties", new JsonObject().put("type", "string")))
            .put("bodyLength", new JsonObject().put("type", "integer"))
        ));

    // Auth models
    schemas.put("RegisterRequest", new JsonObject()
        .put("type", "object")
        .put("required", new JsonArray().add("name").add("email").add("password"))
        .put("properties", new JsonObject()
            .put("name", new JsonObject().put("type", "string"))
            .put("email", new JsonObject().put("type", "string"))
            .put("password", new JsonObject().put("type", "string"))
        ));

    schemas.put("LoginRequest", new JsonObject()
        .put("type", "object")
        .put("required", new JsonArray().add("email").add("password"))
        .put("properties", new JsonObject()
            .put("email", new JsonObject().put("type", "string"))
            .put("password", new JsonObject().put("type", "string"))
        ));

    schemas.put("TokenResponse", new JsonObject()
        .put("type", "object")
        .put("properties", new JsonObject().put("token", new JsonObject().put("type", "string"))));

    // Todo models
    schemas.put("Todo", new JsonObject()
        .put("type", "object")
        .put("properties", new JsonObject()
            .put("id", new JsonObject().put("type", "integer"))
            .put("user_id", new JsonObject().put("type", "integer"))
            .put("title", new JsonObject().put("type", "string"))
            .put("completed", new JsonObject().put("type", "boolean"))
            .put("created_at", new JsonObject().put("type", "string"))
        ));

    schemas.put("NewTodo", new JsonObject()
        .put("type", "object")
        .put("required", new JsonArray().add("title"))
        .put("properties", new JsonObject().put("title", new JsonObject().put("type", "string"))));

    JsonObject components = new JsonObject().put("schemas", schemas);

    // Security scheme
    components.put("securitySchemes", new JsonObject().put("bearerAuth",
        new JsonObject().put("type", "http").put("scheme", "bearer").put("bearerFormat", "JWT")));

    return components;
  }

  private static JsonObject json() { return new JsonObject(); }

  private static JsonObject appJson(Object schemaOrContent) {
    JsonObject content = new JsonObject();
    if (schemaOrContent instanceof JsonObject && ((JsonObject) schemaOrContent).containsKey("schema")) {
      content.put("application/json", schemaOrContent);
    } else {
      content.put("application/json", new JsonObject().put("schema", schemaOrContent));
    }
    return content;
  }

  private static JsonObject schemaRef(String name) { return new JsonObject().put("$ref", "#/components/schemas/" + name); }

  private static JsonObject arrayOf(Object itemSchema) { return new JsonObject().put("schema", new JsonObject().put("type", "array").put("items", itemSchema)); }
}

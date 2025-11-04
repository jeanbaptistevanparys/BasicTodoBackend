# BasicTodoBackend

Simple Todo backend written in Java using Vert.x and Maven. Includes user authentication (JWT), password hashing, and a simple todo API.

## Requirements

- JDK 17+
- Maven 3.6+
- (Optional) IntelliJ IDEA for running and debugging

## Build

From the project root:

```bash
mvn clean package
```

This produces an executable jar in `target/`.

## Run

Run the packaged jar:

```bash
java -jar target/BasicTodoBackend-1.0-SNAPSHOT.jar
```

Or run from your IDE (run `org.example.MainApp`).

## Tests

Run unit tests:

```bash
mvn test
```

You can run just the new util tests like this:

```bash
mvn -Dtest=PasswordUtilTest,JwtUtilTest test
```

## Configuration

Application properties are in `src/main/resources/application.properties`. Common settings:

- Server port
- Database connection (H2 file used for demos)
- JWT secret and expiration

Edit `src/main/resources/application.properties` or set environment variables as needed. The project expects migrations under `src/main/resources/db/migration`.

## Database

By default, the application uses an H2 file database at `target/demo-db.mv.db` for local development. Migrations are in:

- `src/main/resources/db/migration`
- `src/main/resources/db/migration_single`

### Using MySQL

The application supports MySQL 8.x. To use MySQL:

1. **Create the database and user:**
   ```sql
   CREATE DATABASE my_app_db;
   CREATE USER 'appuser'@'%' IDENTIFIED BY 'apppassword';
   GRANT ALL PRIVILEGES ON my_app_db.* TO 'appuser'@'%';
   FLUSH PRIVILEGES;
   ```

2. **Configure the application** by setting environment variables or editing `src/main/resources/application.properties`:
   ```properties
   jdbc.url=jdbc:mysql://localhost:3306/my_app_db?useSSL=false&serverTimezone=UTC
   db.user=appuser
   db.pass=apppassword
   db.driver=com.mysql.cj.jdbc.Driver
   ```

   Note: The JDBC URL must include the database name (e.g., `/my_app_db`) for Flyway to properly detect the default schema.

3. **Run the application** - migrations will be applied automatically by Flyway on startup.

The application includes defensive driver loading, so if a driver class is not on the classpath, it will log a warning and attempt to continue with JDBC URL-based driver detection.


## API & OpenAPI

The application exposes an HTTP API and provides an OpenAPI spec (see `org.example.OpenApiSpec`). OpenAPI endpoints are available at runtime (e.g. `/openapi.json` or the path exposed by the app). Use the spec to discover exact routes and payloads.

Common endpoints (examples):

- POST `/api/auth/register` — register a new user
- POST `/api/auth/login` — obtain JWT token
- CRUD `/api/todos` — manage todos (authenticated)

Example: register a user

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com","password":"secret123"}'
```

Example: login and use token

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"secret123"}'

# use returned token in Authorization header:
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/todos
```

## Passwords & Security

Passwords are hashed before storage using the project's `PasswordUtil` (bcrypt). JWTs are signed using the configured secret (see properties). Keep the JWT secret private and rotate it as needed.

## Troubleshooting

- If you see timestamp parsing warnings in logs, check database timestamp formats and code that parses timestamps.
- For DB issues, ensure migrations applied and `application.properties` DB settings are correct.

## Contributing

Fork, create a feature branch, and open a pull request against `main`. Ensure new code has tests and passes `mvn test`.

## License

See repository license (if any).

# BasicTodoBackend

Simple Todo backend written in Java using Vert.x and Maven. Includes user authentication (JWT), password hashing, and a simple todo API.

## What's new

- Passwords are hashed using bcrypt via `org.example.utils.PasswordUtil` (backed by jBCrypt). Use `PasswordUtil.hash(password)` to create a hash and `PasswordUtil.verify(password, hash)` to verify.

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

If you don't have Maven available on your system, run the project from your IDE (IntelliJ/Idea) which will download dependencies automatically.

## Run

Run the packaged jar:

```bash
java -jar target/BasicTodoBackend-1.0-SNAPSHOT.jar
```

Or run from your IDE (run `org.example.MainApp`).

Notes about JDBC drivers:
- The project uses H2 by default for demos. If you want to connect to MySQL or PostgreSQL, ensure the correct JDBC driver is available on the classpath (the Maven `pom.xml` includes the MySQL connector dependency). If you manage drivers externally, the app will skip explicitly setting the driver class when it's not found on the classpath and rely on the runtime to provide it.

## Tests

Run unit tests:

```bash
mvn test
```

Example: run only password util tests:

```bash
mvn -Dtest=PasswordUtilTest test
```

The `PasswordUtilTest` covers hashing, verification, and null/invalid inputs.

## Configuration

Application properties are in `src/main/resources/application.properties`. Common settings:

- Server port
- Database connection (H2 file used for demos)
- JWT secret and expiration

Edit `src/main/resources/application.properties` or set environment variables as needed. The project expects migrations under `src/main/resources/db/migration`.

## Database

A demo H2 file `target/demo-db.mv.db` may be present when running locally. Migrations are in:

- `src/main/resources/db/migration`
- `src/main/resources/db/migration_single`

Use the provided SQL migration files to initialize the database for tests/local runs.

## Database requirements for Flyway

Flyway needs a database connection that has permission to create and modify the schema history table and to create/alter the schema objects defined in your migrations. For MySQL the minimal requirements are:

- A database (schema) to run migrations against (e.g. `todoapp`).
- A user with privileges on that database: `CREATE`, `ALTER`, `DROP`, `INDEX`, `ALTER ROUTINE`, `CREATE ROUTINE`, `LOCK TABLES`, `CREATE TEMPORARY TABLES`, `INSERT`, `UPDATE`, `DELETE`, `SELECT`.
- Permission to create the Flyway schema history table (Flyway creates and maintains `flyway_schema_history` by default) in the target database.

If you cannot grant all privileges, at minimum ensure the account can:

- CREATE, ALTER, DROP on tables
- SELECT, INSERT on the schema history table (Flyway will create it if missing)

Flyway will store a small table (by default `flyway_schema_history`) in the target database. You don't need to pre-create this table; Flyway will create it on first run, but your DB user needs permission to create tables in the target schema.

### Typical permissions SQL (MySQL)

```sql
-- Run as a privileged user (root) to create the db and user used by the app
CREATE DATABASE todoapp;
CREATE USER 'exampleuser'@'%' IDENTIFIED BY 'examplepass';
GRANT CREATE, ALTER, DROP, INDEX, CREATE ROUTINE, ALTER ROUTINE, LOCK TABLES, CREATE TEMPORARY TABLES, INSERT, UPDATE, DELETE, SELECT
  ON todoapp.* TO 'exampleuser'@'%';
FLUSH PRIVILEGES;
```

## Docker and docker-compose

A `Dockerfile` and `docker-compose.yml` are provided to run MySQL 8 and the app together for development. The compose file creates a `todoapp` database and an `exampleuser` user.

Start the stack:

```bash
# build app image and start db + app
docker compose up --build
```

Stop the stack:

```bash
docker compose down -v
```

Notes:
- The app `Config` supports overriding via environment variables (it uppercases property names). You can also set `JDBC_URL`, `DB_USER`, `DB_PASS` environment variables in `docker-compose.yml` if needed.
- Flyway will run on application startup and attempt to apply migrations found under `classpath:db/migration`.

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

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

A demo H2 file `target/demo-db.mv.db` may be present when running locally. Migrations are in:

- `src/main/resources/db/migration`
- `src/main/resources/db/migration_single`

Use the provided SQL migration files to initialize the database for tests/local runs.

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

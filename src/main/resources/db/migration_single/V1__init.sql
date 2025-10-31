-- Initialize schema: users (with password_hash), todos, and seed data

CREATE TABLE IF NOT EXISTS users (
  id IDENTITY PRIMARY KEY,
  name VARCHAR(200) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP()
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

CREATE TABLE IF NOT EXISTS todos (
  id IDENTITY PRIMARY KEY,
  user_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  completed BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
  CONSTRAINT fk_todos_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_todos_user ON todos(user_id);

-- Seed sample users (idempotent)
MERGE INTO users (name, email, password_hash) KEY(email) VALUES ('Alice', 'alice@example.com', NULL);
MERGE INTO users (name, email, password_hash) KEY(email) VALUES ('Bob', 'bob@example.com', NULL);
MERGE INTO users (name, email, password_hash) KEY(email) VALUES ('Charlie', 'charlie@example.com', NULL);


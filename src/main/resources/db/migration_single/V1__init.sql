-- Initialize schema: users (with password_hash), todos, and seed data

CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(200) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);

CREATE TABLE IF NOT EXISTS todos (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  completed BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_todos_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_todos_user ON todos(user_id);

-- Seed sample users (idempotent for MySQL using ON DUPLICATE KEY UPDATE)
INSERT INTO users (name, email, password_hash) VALUES
  ('Alice', 'alice@example.com', NULL)
ON DUPLICATE KEY UPDATE name = VALUES(name), password_hash = VALUES(password_hash);

INSERT INTO users (name, email, password_hash) VALUES
  ('Bob', 'bob@example.com', NULL)
ON DUPLICATE KEY UPDATE name = VALUES(name), password_hash = VALUES(password_hash);

INSERT INTO users (name, email, password_hash) VALUES
  ('Charlie', 'charlie@example.com', NULL)
ON DUPLICATE KEY UPDATE name = VALUES(name), password_hash = VALUES(password_hash);

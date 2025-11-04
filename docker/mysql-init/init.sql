-- Initializes the todoapp database and user for development
CREATE DATABASE IF NOT EXISTS todoapp;

CREATE USER IF NOT EXISTS 'exampleuser'@'%' IDENTIFIED BY 'examplepass';

GRANT CREATE, ALTER, DROP, INDEX, CREATE ROUTINE, ALTER ROUTINE, LOCK TABLES,
  CREATE TEMPORARY TABLES, INSERT, UPDATE, DELETE, SELECT
  ON todoapp.* TO 'exampleuser'@'%';

FLUSH PRIVILEGES;


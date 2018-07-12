
CREATE TABLE User
(
  id VARCHAR(100) PRIMARY KEY,
  password_hash VARCHAR(100),
  create_time DATETIME NOT NULL,
  last_login_time DATETIME
);

# --- !Ups

CREATE TABLE "user" (
  id VARCHAR(100) NOT NULL,
  provider VARCHAR(100),
  firstName VARCHAR(200),
  lastName VARCHAR(200),
  email VARCHAR(100),
  "password" VARCHAR(100)
);
CREATE TABLE token (
  uuid VARCHAR(100) NOT NULL PRIMARY KEY,
  email VARCHAR(100),
  createdAt TIMESTAMP,
  expireAt TIMESTAMP,
  isSignUp BOOLEAN
);

# --- !Downs

DROP TABLE token;
DROP TABLE "user";

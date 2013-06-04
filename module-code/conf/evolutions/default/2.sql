# --- !Ups

CREATE TABLE authenticator (
  id VARCHAR(512) NOT NULL,
  userId VARCHAR(128) NOT NULL,
  provider VARCHAR(64),
  creationDate timestamp NOT NULL,
  lastUsed timestamp not null,
  expirationDate timestamp not null,
  updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

# --- !Downs

DROP TABLE authenticator;

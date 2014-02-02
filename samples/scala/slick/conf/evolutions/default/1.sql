# --- Created by Slick DDL
# To stop Slick DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table `users` (`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,`userId` VARCHAR(254) NOT NULL,`providerId` VARCHAR(254) NOT NULL,`firstName` VARCHAR(254) NOT NULL,`lastName` VARCHAR(254) NOT NULL,`email` VARCHAR(254),`avatarUrl` VARCHAR(254),`authMethod` VARCHAR(254) NOT NULL,`oAuth1InfoToken` VARCHAR(254),`oAuth1InfoSecret` VARCHAR(254),`oAuth1InfoAccessToken` VARCHAR(254),`oAuth1InfoTokenType` VARCHAR(254),`oAuth1InfoExpiresIn` INTEGER,`oAuth1InfoRefreshToken` VARCHAR(254),`passwordInfoHasher` VARCHAR(254),`passwordInfoPassword` VARCHAR(254),`passwordInfoSalt` VARCHAR(254));
create unique index `userprovider_index` on `users` (`userId`,`providerId`);
create unique index `email_index` on `users` (`email`);
create table `usertokens` (`uuid` VARCHAR(254) NOT NULL PRIMARY KEY,`email` VARCHAR(254) NOT NULL,`creationTime` DATE NOT NULL,`expirationTime` DATE NOT NULL,`isSignup` BOOLEAN NOT NULL);

# --- !Downs

drop table `users`;
drop table `usertokens`;


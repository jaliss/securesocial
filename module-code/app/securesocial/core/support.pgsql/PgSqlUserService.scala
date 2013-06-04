/**
 * Copyright 2012 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package securesocial.core.support.pgsql


import _root_.java.util.{Date, UUID}

import securesocial.core._
import providers.Token
import securesocial.core.UserId
import securesocial.core.PasswordInfo
import scala.Some

import play.api.{Logger, Application}
import securesocial.core._
import org.joda.time.DateTime
import securesocial.core.providers.Token
import securesocial.core.providers.Token

import play.api.db._
import anorm._

import play.api.Play.current
import scala.Some


/**
 * A Database user service in Scala
 *
 */
class PgSqlUserService(application: Application) extends UserServicePlugin(application) {


  /**
   * find user
   *
   */
  def find(id: UserId) = {

    if (Logger.isDebugEnabled) {
      Logger.debug("find...")
      Logger.debug("id = %s".format(id.id))
    }

    DB.withConnection { implicit c =>

      val sqlQuery = SQL(
        """
          SELECT * FROM "user" WHERE id = {id};
        """).on("id" -> id.id)

      // Transform the resulting Stream[Row] to a List[SocialUser]
      val users = sqlQuery().map(row =>
        SocialUser(
          UserId(row[String]("id"), row[String]("provider")),
          row[String]("firstName"),
          row[String]("lastName"),
          row[String]("firstName") + " " + row[String]("lastName"),
          row[Option[String]]("email"),
          None,
          AuthenticationMethod("userPassword"),
          None,
          None,
          Some(PasswordInfo("bcrypt", row[String]("password"), None))
        )).toList

      val socialUser = if (users.size == 1) Some(users(0)) else None

      if (Logger.isDebugEnabled) {
        Logger.debug("socialUser = %s".format(socialUser))
      }

      socialUser

    } // end DB

  } // end find


  /**
   * findByEmailAndProvider user
   *
   */
  def findByEmailAndProvider(email: String, providerId: String): Option[SocialUser] = {

    if (Logger.isDebugEnabled) {
      Logger.debug("findByEmailAndProvider...")
      Logger.debug("email = %s".format(email))
      Logger.debug("providerId = %s".format(providerId))
    }

    DB.withConnection { implicit c =>

      val sqlQuery = SQL(
        """
		      SELECT * FROM "user" WHERE email = {email} AND provider = {provider}
        """).on(
          'email -> email,
          'provider -> providerId
      )

      // Transform the resulting Stream[Row] to a List[SocialUser]
      val users = sqlQuery().map(row =>
        SocialUser(
          UserId(row[String]("id"), row[String]("provider")),
          row[String]("firstName"),
          row[String]("lastName"),
          row[String]("firstName") + " " + row[String]("lastName"),
          row[Option[String]]("email"),
          None,
          AuthenticationMethod("userPassword"),
          None,
          None,
          Some(PasswordInfo("bcrypt", row[String]("password"), None))
        )).toList

      val socialUser = if (users.size == 1) Some(users(0)) else None

      if (Logger.isDebugEnabled) {
        Logger.debug("socialUser = %s".format(socialUser))
      }

      socialUser

    } // end DB

  } // end findByEmailAndProvider


  /**
   * save user
   * (actually save or update)
   *
   */
  def save(user: Identity):Identity = {

    if (Logger.isDebugEnabled) {
      Logger.debug("save...")
      Logger.debug("user = %s".format(user))
    }

    DB.withConnection { implicit c =>

      val sqlSelectQuery = SQL(
        """
          SELECT * FROM "user" WHERE id = {id};
        """).on("id" -> user.id.id)

      val users = sqlSelectQuery().map(row =>
        SocialUser(
          UserId(row[String]("id"), row[String]("provider")),
          row[String]("firstName"),
          row[String]("lastName"),
          row[String]("firstName") + " " + row[String]("lastName"),
          row[Option[String]]("email"),
          None,
          AuthenticationMethod("userPassword"),
          None,
          None,
          Some(PasswordInfo("bcrypt", row[String]("password"), None))
        )).toList

      val socialUser = if (users.size == 1) Some(users(0)) else None

      if (Logger.isDebugEnabled) {
        Logger.debug("socialUser = %s".format(socialUser))
      }

      if (socialUser == None) { // user not exists

        if (Logger.isDebugEnabled) {
          Logger.debug("INSERT")
        }

        // create a new user
        val sqlQuery = SQL(
          """
            INSERT INTO "user" (id, provider, firstName, lastName, email, "password")
            VALUES ({id}, {provider}, {firstName}, {lastName}, {email}, {password})
          """).on(
            'id -> user.id.id,
            'provider -> user.id.providerId,
            'firstName -> user.firstName,
            'lastName -> user.lastName,
            'email -> user.email,
            'password -> user.passwordInfo.get.password
        )

        val result: Int = sqlQuery.executeUpdate()

        if (Logger.isDebugEnabled) {
          Logger.debug("result = %s".format(result))
        }

      } else { // user exists

        if (Logger.isDebugEnabled) {
          Logger.debug("UPDATE")
        }

        // update the user
        val sqlQuery = SQL(
          """
            UPDATE "user"
            SET id = {id}, 
                provider = {provider},
                firstName = {firstName}, 
                lastName = {lastName}, 
                email = {email}, 
                "password" = {password}
            WHERE id = {id}
          """).on(
            'id -> user.id.id,
            'provider -> user.id.providerId,
            'firstName -> user.firstName,
            'lastName -> user.lastName,
            'email -> user.email,
            'password -> user.passwordInfo.get.password
        )

        val result: Int = sqlQuery.executeUpdate()

        if (Logger.isDebugEnabled) {
          Logger.debug("result = %s".format(result))
        }

      } // end else

      user
    } // end DB

  } // end save


  /**
   * save token
   *
   */
  def save(token: Token) {

    if (Logger.isDebugEnabled) {
      Logger.debug("save...")
      Logger.debug("token = %s".format(token))
    }

    DB.withConnection { implicit c =>

      if (Logger.isDebugEnabled) {
        Logger.debug("INSERT")
      }

      // create a new token
      val sqlQuery = SQL(
        """
          INSERT INTO token  (uuid, email, createdAt, expireAt, isSignUp)
          VALUES ({uuid}, {email}, to_timestamp({createdAt}, 'YYYY-MM-DD HH24:MI:SS'), to_timestamp({expireAt}, 'YYYY-MM-DD HH24:MI:SS'), {isSignUp})
        """).on(
          'uuid -> token.uuid,
          'email -> token.email,
          'createdAt -> token.creationTime.toString("yyyy-MM-dd HH:mm:ss"),
          'expireAt -> token.expirationTime.toString("yyyy-MM-dd HH:mm:ss"),
          'isSignUp -> token.isSignUp
      )

      val result: Int = sqlQuery.executeUpdate()

      if (Logger.isDebugEnabled) {
        Logger.debug("result = %s".format(result))
      }

    } // end DB

  } // end save


  /**
   * findToken
   *
   */
  def findToken(token: String): Option[Token] = {

    if (Logger.isDebugEnabled) {
      Logger.debug("findToken...")
      Logger.debug("token = %s".format(token))
    }

    DB.withConnection { implicit c =>

      val sqlSelectQuery = SQL(
        """
		    SELECT * FROM token
		    WHERE uuid = {uuid};
        		  """).on("uuid" -> token)

      val tokens = sqlSelectQuery().map(row => {
        val creationTime = row[Date]("createdAt")
        val expirationTime = row[Date]("expireAt")
        if (Logger.isDebugEnabled) {
          Logger.debug("creationTime = %s".format(creationTime))
          Logger.debug("expirationTime = %s".format(expirationTime))
        }
        Token(
          row[String]("uuid"),
          row[String]("email"),
          new DateTime(creationTime),
          new DateTime(expirationTime),
          row[Boolean]("isSignUp")
        )
      }).toList

      val foundToken = if (tokens.size == 1) Some(tokens(0)) else None

      if (Logger.isDebugEnabled) {
        Logger.debug("foundToken = %s".format(foundToken))
      }

      foundToken

    } // end DB

  } // end findToken


  /**
   * deleteToken
   *
   */
  def deleteToken(uuid: String) {

    if (Logger.isDebugEnabled) {
      Logger.debug("deleteToken...")
      Logger.debug("uuid = %s".format(uuid))
    }

    DB.withConnection { implicit c =>

    // delete token
      val sqlQuery = SQL(
        """
		      DELETE FROM token  WHERE uuid = {uuid};
        """).on("uuid" -> uuid)

      val result: Int = sqlQuery.executeUpdate()

      if (Logger.isDebugEnabled) {
        Logger.debug("result = %s".format(result))
      }

    } // end DB

  } // end deleteToken


  /**
   * deleteTokens
   *
   */
  def deleteTokens() {

    if (Logger.isDebugEnabled) {
      Logger.debug("deleteTokens...")
    }

    DB.withConnection { implicit c =>

    // delete all tokens
      val sqlQuery = SQL(
        """
		  DELETE FROM token;
        		""")

      val result: Int = sqlQuery.executeUpdate()

      if (Logger.isDebugEnabled) {
        Logger.debug("result = %s".format(result))
      }

    } // end DB

  } // end deleteTokens


  /**
   * deleteExpiredTokens
   *
   */
  def deleteExpiredTokens() {

    if (Logger.isDebugEnabled) {
      Logger.debug("deleteExpiredTokens...")
    }

    DB.withConnection { implicit c =>

    // delete expired tokens
      val sqlQuery = SQL(
        """
		  DELETE FROM token 
		  WHERE expireAt < current_timestamp;
        		""")

      val result: Int = sqlQuery.executeUpdate()

      if (Logger.isDebugEnabled) {
        Logger.debug("result = %s".format(result))
      }

    } // end DB

  } // end deleteExpiredTokens


} // end DbUserService

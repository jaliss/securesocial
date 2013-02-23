package securesocial.core.support.pgsql


import _root_.java.util.Date
import play.api.db._
import anorm._

import play.api.Play.current


import play.api.{Logger, Application}
import scala.Error
import scala.Some

import org.joda.time.DateTime
import securesocial.core.{Authenticator, UserId, AuthenticatorStore}

class PgSqlAuthenticatorStore(app: Application) extends AuthenticatorStore(app) {

  // case class Authenticator(id: String, userId: UserId, creationDate: DateTime,
  //   lastUsed: DateTime, expirationDate: DateTime)


  def save(authenticator: Authenticator): Either[Error, Unit] = {

    if (Logger.isDebugEnabled) {
      Logger.debug("Save authenticator [%s]".format(authenticator))
    }

    DB.withConnection { implicit c =>

      val sqlSelectQuery = SQL(
        """
          SELECT * FROM authenticator WHERE id = {id};
        """).on("id" -> authenticator.id)

      val authenticators = sqlSelectQuery().map(row =>
        Authenticator(
          row[String]("id"),
          UserId(row[String]("userId"), row[String]("provider")),
          new DateTime(row[Date]("creationDate")),
          new DateTime(row[Date]("lastUsed")),
          new DateTime(row[Date]("expirationDate"))
        )).toList

      val foundAuthenticator = if (authenticators.size == 1) authenticators(0) else None

      if (Logger.isDebugEnabled) {
        Logger.debug("authenticator = %s".format(foundAuthenticator))
      }

      if (foundAuthenticator == None) { // user not exists

        if (Logger.isDebugEnabled) {
          Logger.debug("INSERT")
        }

        // create a new user
        val sqlQuery = SQL(
          """
            INSERT INTO authenticator (id, userId, provider, creationDate, lastUsed, expirationDate)
            VALUES ({id}, {userId}, {provider}, {creationDate}, {lastUsed}, {expirationDate})
          """).on(
          'id -> authenticator.id,
          'userId -> authenticator.userId.id,
          'provider -> authenticator.userId.providerId,
          'creationDate -> authenticator.creationDate.toDate,
          'lastUsed -> authenticator.lastUsed.toDate,
          'expirationDate -> authenticator.expirationDate.toDate
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
            UPDATE authenticator
            SET id = {id},
                userId = {userId},
                provider = {provider},
                creationDate = {creationDate},
                lastUsed = {lastUsed},
                expirationDate = {expirationDate}
            WHERE id = {id}
          """).on(
          'id -> authenticator.id,
          'userId -> authenticator.userId.id,
          'provider -> authenticator.userId.providerId,
          'creationDate -> authenticator.creationDate.toDate,
          'lastUsed -> authenticator.lastUsed.toDate,
          'expirationDate -> authenticator.expirationDate.toDate
        )

        val result: Int = sqlQuery.executeUpdate()

        if (Logger.isDebugEnabled) {
          Logger.debug("result = %s".format(result))
        }

      } // end else

      authenticator
    } // end DB

    Right(())
  } // end save

  def find(id: String): Either[Error, Option[Authenticator]] = {

    if (Logger.isDebugEnabled) {
      Logger.debug("Find Authenticator with Id = '%s' ...".format(id))
    }

    DB.withConnection { implicit c =>

      val sqlQuery = SQL(
        """
          SELECT * FROM authenticator WHERE id = {id};
        """).on("id" -> id)

      // Transform the resulting Stream[Row] to a List[Authenticators]
      val authenticators = sqlQuery().map(row =>
        Authenticator(
          row[String]("id"),
          UserId(row[String]("userId"), row[String]("provider")),
          new DateTime(row[Date]("creationDate")),
          new DateTime(row[Date]("lastUsed")),
          new DateTime(row[Date]("expirationDate"))
        )).toList

      val authenticator = if (authenticators.size == 1) Some(authenticators(0)) else None

      if (Logger.isDebugEnabled) {
        Logger.debug("authenticator = %s".format(authenticator))
      }

      Right((authenticator))
    } // end DB

  } // end find

  def delete(id: String): Either[Error, Unit] = {

    if (Logger.isDebugEnabled) {
      Logger.debug("delete authenticator...")
      Logger.debug("Authenticator Id = %s".format(id))
    }

    DB.withConnection { implicit c =>

    // delete token
      val sqlQuery = SQL(
        """
		      DELETE FROM authenticator WHERE id = {id};
        """).on("id" -> id)

      val result: Int = sqlQuery.executeUpdate()

      if (Logger.isDebugEnabled) {
        Logger.debug("result = %s".format(result))
      }

    } // end DB

    Right(())
  } // end delete user

}
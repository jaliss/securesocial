/**
 * Copyright 2013-2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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
package securesocial.core.authenticator

import org.joda.time.DateTime
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import play.api.Play
import scala.Some
import play.api.mvc.Result

/**
 * A http header based authenticator. This authenticator works using the X-Auth-Token header in the http request
 * to track authenticated users. Since the token is only an id the rest of the user data is stored using an
 * instance of the AuthenticatorStore.
 *
 * @param id the authenticator id
 * @param user the user this authenticator is associated with
 * @param expirationDate the expiration date
 * @param lastUsed the last time the authenticator was used
 * @param creationDate the authenticator creation time
 * @param store the authenticator store where instances of this authenticator are persisted
 * @tparam U the user type (defined by the application using the module)
 *
 * @see AuthenticatorStore
 * @see RuntimeEnvironment
 */
case class HttpHeaderAuthenticator[U](id: String, user: U, expirationDate: DateTime,
                                  lastUsed: DateTime,
                                  creationDate: DateTime, store: AuthenticatorStore[HttpHeaderAuthenticator[U]]) extends Authenticator[U] {
  private val logger = play.api.Logger("securesocial.core.authenticator.HttpHeaderAuthenticator")

  /**
   * Updated the last used timestamp
   *
   * @return a future with the updated authenticator
   */
  override def touch: Future[Authenticator[U]] = {
    val updated = this.copy[U](lastUsed = DateTime.now())
    logger.debug(s"HttpHeaderAuthenticator touched: lastUsed = $lastUsed")
    store.save(updated, HttpHeaderAuthenticator.absoluteTimeoutInSeconds)
  }


  /**
   * Updates the user information associated with this authenticator
   *
   * @param user the user object
   * @return a future with the updated authenticator
   */
  override def updateUser(user: U): Future[Authenticator[U]] = {
    val updated = this.copy[U](user = user)
    logger.debug(s"HttpHeaderAuthenticator updated user: $updated")
    store.save(updated, HttpHeaderAuthenticator.absoluteTimeoutInSeconds)
  }

  /**
   * Checks if the authenticator has expired. This is an absolute timeout since the creation of
   * the authenticator
   *
   * @return true if the authenticator has expired, false otherwise.
   */
  def expired: Boolean = expirationDate.isBeforeNow

  /**
   * Checks if the time elapsed since the last time the authenticator was used is longer than
   * the maximum idle timeout specified in the properties.
   *
   * @return true if the authenticator timed out, false otherwise.
   */
  def timedOut: Boolean = lastUsed.plusMinutes(HttpHeaderAuthenticator.idleTimeout).isBeforeNow

  /**
   * Checks if the authenticator is valid.  For this implementation it means that the
   * authenticator has not expired or timed out.
   *
   * @return true if the authenticator is valid, false otherwise.
   */
  override def isValid: Boolean = !expired && !timedOut

  /**
   * Removes the authenticator from the store.
   *
   * @param result the result that is about to be sent to the client
   * @return the result unaltered in this case.
   */
  override def discarding(result: Result): Future[Result] = {
    import ExecutionContext.Implicits.global
    store.delete(id).map { _ => result }
  }

  /**
   * Starts an authenticated session by returning a json with the authenticator id
   *
   * @param result the result that is about to be sent to the client
   * @return the result with the authenticator header set
   */
  override def starting(result: Result): Future[Result] = {
    Future.successful { result }
  }

  /**
   * Adds a touched authenticator to the result (for Scala).  In this implementation there's no need
   * to do anything with the result
   *
   * @param result
   * @return
   */
  override def touching(result: Result): Future[Result] = {
    Future.successful(result)
  }

  /**
   * Adds a touched authenticator to the result(for Java).  In this implementation there's no need
   * to do anything with the result
   *
   * @param javaContext
   * @return
   */
  def touching(javaContext: play.mvc.Http.Context): Future[Unit] = {
    Future.successful(())
  }

  /**
   *
   * @param javaContext
   * @return
   */
  override def discarding(javaContext: play.mvc.Http.Context): Future[Unit] = {
    import ExecutionContext.Implicits.global
    store.delete(id).map { _ => () }
  }
}

/**
 * An authenticator builder. It can create an Authenticator instance from an http request or from a user object
 *
 * @param store the store where instances of the HttpHeaderAuthenticator class are persisted.
 * @param generator a session id generator
 * @tparam U the user object type
 */
class HttpHeaderAuthenticatorBuilder[U](store: AuthenticatorStore[HttpHeaderAuthenticator[U]], generator: IdGenerator) extends AuthenticatorBuilder[U] {
  val id = HttpHeaderAuthenticator.Id

  /**
   * Creates an instance of a HttpHeaderAuthenticator from the http request
   *
   * @param request the incoming request
   * @return an optional HttpHeaderAuthenticator instance.
   */
  override def fromRequest(request: RequestHeader): Future[Option[HttpHeaderAuthenticator[U]]] = {
    request.headers.get("X-Auth-Token") match {
      case Some(value) => store.find(value)
      case None => Future.successful(None)
    }
  }

  /**
   * Creates an instance of a HttpHeaderAuthenticator from a user object.
   *
   * @param user the user
   * @return a HttpHeaderAuthenticator instance.
   */
  override def fromUser(user: U): Future[HttpHeaderAuthenticator[U]] = {
    import ExecutionContext.Implicits.global
    generator.generate.flatMap {
      id =>
        val now = DateTime.now()
        val expirationDate = now.plusMinutes(HttpHeaderAuthenticator.absoluteTimeout)
        val authenticator = HttpHeaderAuthenticator(id, user, expirationDate, now, now, store)
        store.save(authenticator, HttpHeaderAuthenticator.absoluteTimeoutInSeconds)
    }
  }
}

object HttpHeaderAuthenticator {
  import play.api.Play.current
  // todo: create settings object

  val Id = "token"
  val HeaderNameKey = "securesocial.auth-header.name"

  // default values
  val DefaultHeaderName = "X-Auth-Token"
  val DefaultIdleTimeout = 30
  val DefaultAbsoluteTimeout = 12 * 60

  lazy val headerName = Play.application.configuration.getString(HeaderNameKey).getOrElse(DefaultHeaderName)
  // using the same properties that the CookieBased authenticator for now.
  lazy val idleTimeout = CookieAuthenticator.idleTimeout
  lazy val absoluteTimeout = CookieAuthenticator.absoluteTimeout
  lazy val absoluteTimeoutInSeconds = CookieAuthenticator.absoluteTimeoutInSeconds
}

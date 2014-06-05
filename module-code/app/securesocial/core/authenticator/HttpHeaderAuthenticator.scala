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
import play.api.mvc.SimpleResult

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
                                  creationDate: DateTime,
                                  @transient
                                  store: AuthenticatorStore[HttpHeaderAuthenticator[U]]) extends StoreBackedAuthenticator[U, HttpHeaderAuthenticator[U]] {

  override val idleTimeoutInMinutes = HttpHeaderAuthenticator.idleTimeout
  override val absoluteTimeoutInSeconds = HttpHeaderAuthenticator.absoluteTimeoutInSeconds
  /**
   * Returns a copy of this authenticator with the given last used time
   *
   * @param time the new time
   * @return the modified authenticator
   */
  def withLastUsedTime(time: DateTime): HttpHeaderAuthenticator[U] = this.copy[U](lastUsed = time)

  /**
   * Returns a copy of this Authenticator with the given user
   *
   * @param user the new user
   * @return the modified authenticator
   */
  def withUser(user: U): HttpHeaderAuthenticator[U] = this.copy[U](user = user)

  /**
   * Starts an authenticated session by returning a json with the authenticator id
   *
   * @param result the result that is about to be sent to the client
   * @return the result with the authenticator header set
   */
  override def starting(result: SimpleResult): Future[SimpleResult] = {
    Future.successful { result }
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
    import ExecutionContext.Implicits.global
    request.headers.get("X-Auth-Token") match {
      case Some(value) => store.find(value).map { retrieved =>
        retrieved.map { _.copy(store = store) }
      }
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

  lazy val headerName = Play.application.configuration.getString(HeaderNameKey).getOrElse(DefaultHeaderName)
  // using the same properties than the CookieBased authenticator for now.
  lazy val idleTimeout = CookieAuthenticator.idleTimeout
  lazy val absoluteTimeout = CookieAuthenticator.absoluteTimeout
  lazy val absoluteTimeoutInSeconds = CookieAuthenticator.absoluteTimeoutInSeconds
}

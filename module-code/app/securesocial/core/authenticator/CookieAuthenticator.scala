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
import play.api.mvc.{Cookie, SimpleResult, DiscardingCookie, RequestHeader}
import securesocial.core.IdentityProvider
import scala.concurrent.{ExecutionContext, Future}
import play.api.Play

/**
 * A Cookie based authenticator. This authenticator puts an id an a cookie that is then used to track authenticated
 * users.  Since the cookie only has the id for this authenticator the rest of the data is stored using an
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
case class CookieAuthenticator[U](id: String, user: U, expirationDate: DateTime,
                                  lastUsed: DateTime,
                                  creationDate: DateTime,
                                  @transient
                                  store: AuthenticatorStore[CookieAuthenticator[U]]) extends StoreBackedAuthenticator[U, CookieAuthenticator[U]] {
  @transient
  override val idleTimeoutInMinutes =  CookieAuthenticator.idleTimeout

  @transient
  override val absoluteTimeoutInSeconds = CookieAuthenticator.absoluteTimeoutInSeconds

  /**
   * Returns a copy of this authenticator with the given last used time
   *
   * @param time the new time
   * @return the modified authenticator
   */
  def withLastUsedTime(time: DateTime): CookieAuthenticator[U] = this.copy[U](lastUsed = time)

  /**
   * Returns a copy of this Authenticator with the given user
   *
   * @param user the new user
   * @return the modified authenticator
   */
  def withUser(user: U): CookieAuthenticator[U] = this.copy[U](user = user)

  /**
   * Ends an authenticator session.  This is invoked when the user logs out or if the
   * authenticator becomes invalid (maybe due to a timeout)
   *
   * @param result the result that is about to be sent to the client.
   * @return the result modified to signal the authenticator is no longer valid
   */
  override def discarding(result: SimpleResult): Future[SimpleResult] = {
    import ExecutionContext.Implicits.global
    store.delete(id).map { _ =>
      result.discardingCookies(CookieAuthenticator.discardingCookie)
    }
  }

  /**
   * Starts an authenticated session by placing a cookie in the result
   *
   * @param result the result that is about to be sent to the client
   * @return the result with the authenticator cookie set
   */
  override def starting(result: SimpleResult): Future[SimpleResult] = {
    Future.successful {
      result.withCookies(
        Cookie(
          CookieAuthenticator.cookieName,
          id,
          if (CookieAuthenticator.makeTransient)
            CookieAuthenticator.Transient
          else Some(CookieAuthenticator.absoluteTimeoutInSeconds),
          CookieAuthenticator.cookiePath,
          CookieAuthenticator.cookieDomain,
          secure = CookieAuthenticator.cookieSecure,
          httpOnly = CookieAuthenticator.cookieHttpOnly
        )
      )
    }
  }

  /**
   * Removes the authenticator from the store and discards the cookie associated with it.
   *
   * @param javaContext the current invocation context
   */
  override def discarding(javaContext: play.mvc.Http.Context): Future[Unit] = {
    import ExecutionContext.Implicits.global
    store.delete(id).map { _ =>
      javaContext.response().discardCookie(
        CookieAuthenticator.cookieName,
        CookieAuthenticator.cookiePath,
        CookieAuthenticator.cookieDomain.getOrElse(null),
        CookieAuthenticator.cookieSecure
      )
    }
  }
}

/**
 * An authenticator builder. It can create an Authenticator instance from an http request or from a user object
 *
 * @param store the store where instances of the CookieAuthenticator class are persisted.
 * @param generator a session id generator
 * @tparam U the user object type
 */
class CookieAuthenticatorBuilder[U](store: AuthenticatorStore[CookieAuthenticator[U]], generator: IdGenerator) extends AuthenticatorBuilder[U] {
  val id = CookieAuthenticator.Id

  /**
   * Creates an instance of a CookieAuthenticator from the http request
   *
   * @param request the incoming request
   * @return an optional CookieAuthenticator instance.
   */
  override def fromRequest(request: RequestHeader): Future[Option[CookieAuthenticator[U]]] = {
    import ExecutionContext.Implicits.global
    request.cookies.get(CookieAuthenticator.cookieName) match {
      case Some(cookie) => store.find(cookie.value).map { retrieved =>
        retrieved.map { _.copy(store = store) }
      }
      case None => Future.successful(None)
    }
  }

  /**
   * Creates an instance of a CookieAuthenticator from a user object.
   *
   * @param user the user
   * @return a CookieAuthenticator instance.
   */
  override def fromUser(user: U): Future[CookieAuthenticator[U]] = {
    import ExecutionContext.Implicits.global
    generator.generate.flatMap {
      id =>
        val now = DateTime.now()
        val expirationDate = now.plusMinutes(CookieAuthenticator.absoluteTimeout)
        val authenticator = CookieAuthenticator(id, user, expirationDate, now, now, store)
        store.save(authenticator, CookieAuthenticator.absoluteTimeoutInSeconds)
    }
  }
}

object CookieAuthenticator {
  import play.api.Play.current
  // todo: create settings object

  val Id = "cookie"

  // property keys
  val CookieNameKey = "securesocial.cookie.name"
  val CookiePathKey = "securesocial.cookie.path"
  val CookieDomainKey = "securesocial.cookie.domain"
  val CookieHttpOnlyKey = "securesocial.cookie.httpOnly"
  val ApplicationContext = "application.context"
  val IdleTimeoutKey = "securesocial.cookie.idleTimeoutInMinutes"
  val AbsoluteTimeoutKey = "securesocial.cookie.absoluteTimeoutInMinutes"
  val TransientKey = "securesocial.cookie.makeTransient"

  // default values
  val DefaultCookieName = "id"
  val DefaultCookiePath = "/"
  val DefaultCookieHttpOnly = true
  val Transient = None
  val DefaultIdleTimeout = 30
  val DefaultAbsoluteTimeout = 12 * 60


  lazy val cookieName = Play.application.configuration.getString(CookieNameKey).getOrElse(DefaultCookieName)
  lazy val cookiePath = Play.application.configuration.getString(CookiePathKey).getOrElse(
    Play.configuration.getString(ApplicationContext).getOrElse(DefaultCookiePath)
  )
  lazy val cookieDomain = Play.application.configuration.getString(CookieDomainKey)
  lazy val cookieSecure = IdentityProvider.sslEnabled
  lazy val cookieHttpOnly = Play.application.configuration.getBoolean(CookieHttpOnlyKey).getOrElse(DefaultCookieHttpOnly)
  lazy val idleTimeout = Play.application.configuration.getInt(IdleTimeoutKey).getOrElse(DefaultIdleTimeout)
  lazy val absoluteTimeout = Play.application.configuration.getInt(AbsoluteTimeoutKey).getOrElse(DefaultAbsoluteTimeout)
  lazy val absoluteTimeoutInSeconds = absoluteTimeout * 60
  lazy val makeTransient = Play.application.configuration.getBoolean(TransientKey).getOrElse(true)

  val discardingCookie: DiscardingCookie = {
    DiscardingCookie(cookieName, cookiePath, cookieDomain, cookieSecure)
  }
}
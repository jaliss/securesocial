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

import io.methvin.play.autoconfig.AutoConfig
import org.joda.time.DateTime
import play.api.mvc.{ Result, _ }
import play.api.{ ConfigLoader, Configuration }

import scala.concurrent.Future

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
  config: HttpHeaderConfig,
  @transient store: AuthenticatorStore[HttpHeaderAuthenticator[U]])
  extends StoreBackedAuthenticator[U, HttpHeaderAuthenticator[U]] {

  override val idleTimeoutInMinutes = config.idleTimeoutInMinutes
  override val absoluteTimeoutInSeconds = config.absoluteTimeoutInSeconds
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
  override def starting(result: Result): Future[Result] = {
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
class HttpHeaderAuthenticatorBuilder[U](
  store: AuthenticatorStore[HttpHeaderAuthenticator[U]],
  generator: IdGenerator,
  config: HttpHeaderConfig)
  extends AuthenticatorBuilder[U] {

  import store.executionContext

  val id = HttpHeaderAuthenticator.Id

  /**
   * Creates an instance of a HttpHeaderAuthenticator from the http request
   *
   * @param request the incoming request
   * @return an optional HttpHeaderAuthenticator instance.
   */
  override def fromRequest(request: RequestHeader): Future[Option[HttpHeaderAuthenticator[U]]] = {
    request.headers.get(config.name) match {
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
    generator.generate.flatMap {
      id =>
        val now = DateTime.now()
        val expirationDate = now.plusMinutes(config.absoluteTimeoutInMinutes)
        val authenticator = HttpHeaderAuthenticator(id, user, expirationDate, now, now, config, store)
        store.save(authenticator, config.absoluteTimeoutInSeconds)
    }
  }
}

case class HttpHeaderConfig(
  name: String,
  idleTimeoutInMinutes: Int,
  absoluteTimeoutInMinutes: Int) {
  def absoluteTimeoutInSeconds: Int = absoluteTimeoutInMinutes * 60
}
object HttpHeaderConfig {
  implicit val configLoader: ConfigLoader[HttpHeaderConfig] = AutoConfig.loader

  def fromConfiguration(configuration: Configuration): HttpHeaderConfig =
    configuration.get[HttpHeaderConfig]("securesocial.auth-header")
}

object HttpHeaderAuthenticator {
  val Id = "token"
}

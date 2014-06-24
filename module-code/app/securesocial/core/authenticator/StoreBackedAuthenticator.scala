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
import scala.annotation.meta.getter
import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc.SimpleResult

/**
 * Base trait for the Cookie and Http Header based authenticators
 *
 * @tparam U the user object type
 * @tparam T the authenticator type
 */
trait StoreBackedAuthenticator[U, T <: Authenticator[U]] extends Authenticator[U] {
  @transient
  protected val logger = play.api.Logger(this.getClass.getName)

  @(transient @getter)
  val store: AuthenticatorStore[T]

  /**
   * The time an authenticator is allowed to live in the store 
   */
  val absoluteTimeoutInSeconds: Int

  /**
   * The inactivity period after which an authenticator is considered invalid
   */
  val idleTimeoutInMinutes: Int

  /**
   * Returns a copy of this authenticator with the given last used time
   *
   * @param time the new time
   * @return the modified authenticator
   */
  def withLastUsedTime(time: DateTime): T

  /**
   * Returns a copy of this Authenticator with the given user
   *
   * @param user the new user
   * @return the modified authenticator
   */
  def withUser(user: U): T

  /**
   * Updated the last used timestamp
   *
   * @return a future with the updated authenticator
   */
  override def touch: Future[T] = {
    val updated = withLastUsedTime(DateTime.now())
    logger.debug(s"touched: lastUsed = $lastUsed")
    store.save(updated, absoluteTimeoutInSeconds)
  }


  /**
   * Updates the user information associated with this authenticator
   *
   * @param user the user object
   * @return a future with the updated authenticator
   */
  override def updateUser(user: U): Future[T] = {
    val updated = withUser(user)
    logger.debug(s"updated user: $updated")
    store.save(updated, absoluteTimeoutInSeconds)
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
  def timedOut: Boolean = lastUsed.plusMinutes(CookieAuthenticator.idleTimeout).isBeforeNow

  /**
   * Checks if the authenticator is valid.  For this implementation it means that the
   * authenticator has not expired or timed out.
   *
   * @return true if the authenticator is valid, false otherwise.
   */
  override def isValid: Boolean = !expired && !timedOut

  /////// Result handling methods
  /**
   * Adds a touched authenticator to the result (for Scala).  In this implementation there's no need
   * to do anything with the result
   *
   * @param result
   * @return
   */
  override def touching(result: SimpleResult): Future[SimpleResult] = {
    Future.successful(result)
  }

  /**
   * Adds a touched authenticator to the result(for Java).  In this implementation there's no need
   * to do anything with the result
   *
   * @param javaContext the current invocation context
   */
  def touching(javaContext: play.mvc.Http.Context): Future[Unit] = {
    Future.successful(())
  }

  /**
   * Ends an authenticator session.  This is invoked when the user logs out or if the
   * authenticator becomes invalid (maybe due to a timeout)
   *
   * @param result the result that is about to be sent to the client.
   * @return the result modified to signal the authenticator is no longer valid
   */
  override def discarding(result: SimpleResult): Future[SimpleResult] = {
    import ExecutionContext.Implicits.global
    store.delete(id).map { _ => result }
  }

  /**
   * Ends an authenticator session.  This is invoked when the authenticator becomes invalid (for Java actions)
   *
   * @param javaContext the current http context
   * @return the current http context modified to signal the authenticator is no longer valid
   */
  override def discarding(javaContext: play.mvc.Http.Context): Future[Unit] = {
    import ExecutionContext.Implicits.global
    store.delete(id).map { _ => () }
  }
}
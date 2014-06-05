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
import play.api.mvc.{SimpleResult, RequestHeader}
import scala.concurrent.Future

/**
 * An Authenticator.  Instances of this trait are used to track authenticated users
 *
 * @tparam U the user object type
 */
trait Authenticator[U] {
  /**
   * An id for this authenticator
   */
  val id: String

  /**
   * The user this authenticator represents
   */
  val user: U

  /**
   * The creation time
   */
  val creationDate: DateTime

  /**
   * The last used time
   */
  val lastUsed: DateTime

  /**
   * The expiration date
   */
  val expirationDate: DateTime

  /**
   * Checks if this authenticator is valid.
   *
   * @return true if the authenticator is valid, false otherwise
   */
  def isValid: Boolean

  /**
   * Touches the authenticator. This is invoked every time a protected action is
   * executed.  Depending on the implementation this can be used to update the last time
   * used timestamp
   *
   * @return an updated instance
   */
  def touch: Future[Authenticator[U]]

  /**
   * Updated the user associated with this authenticator. This method can be used
   * by authenticators that store user information on the client side.
   *
   * @param user the user object
   * @return an updated instance
   */
  def updateUser(user: U): Future[Authenticator[U]]

  /**
   * Starts an authenticator session. This is invoked when the user logs in.
   *
   * @param result the result that is about to be sent to the client
   * @return the result modified to signal a new session has been created.
   */
  def starting(result: SimpleResult): Future[SimpleResult]

  /**
   * Ends an authenticator session.  This is invoked when the user logs out or if the
   * authenticator becomes invalid (maybe due to a timeout)
   *
   * @param result the result that is about to be sent to the client.
   * @return the result modified to signal the authenticator is no longer valid
   */
  def discarding(result: SimpleResult): Future[SimpleResult]

  /**
   * Invoked after a protected action is executed.  This can be used to
   * alter the result in implementations that need to update the information sent to the client
   * after the authenticator is used.
   *
   * @param result the result that is about to be sent to the client.
   * @return the result modified with the updated authenticator
   */
  def touching(result: SimpleResult): Future[SimpleResult]

  // java results
  /**
   * Invoked after a protected Java action is executed.  This can be used to
   * alter the result in implementations that need to update the information sent to the client
   * after the authenticator is used.
   *
   * @param javaContext the current http context
   * @return the http context modified with the updated authenticator
   */
  def touching(javaContext: play.mvc.Http.Context): Future[Unit]

  /**
   * Ends an authenticator session.  This is invoked when the authenticator becomes invalid (for Java actions)
   *
   * @param javaContext the current http context
   * @return the current http context modified to signal the authenticator is no longer valid
   */
  def discarding(javaContext: play.mvc.Http.Context): Future[Unit]
}

/**
 * An AuthenticatorBuilder. It helps to create instances of an Authenticator and to parse them
 * from incoming requests.
 *
 * @tparam U the user object type
 */
trait AuthenticatorBuilder[U] {
  val id: String

  /**
   * Parses a request and returns an optional authenticator instance
   *
   * @param request the incoming request
   * @return an instance of an authenticator if the user is authenticated or None otherwise.
   */
  def fromRequest(request: RequestHeader): Future[Option[Authenticator[U]]]

  /**
   * Creates an authenticator for the given user
   *
   * @param user the user object
   * @return an Authenticator associated with the user
   */
  def fromUser(user: U): Future[Authenticator[U]]
}

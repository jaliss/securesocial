/**
 * Copyright 2012-2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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
package securesocial.controllers

import securesocial.core.SecureSocial
import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc.RequestHeader
import play.api.i18n.Messages
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import securesocial.core.providers.MailToken
import scala.Some
import play.api.mvc.SimpleResult
import java.util.UUID
import org.joda.time.DateTime
import play.api.Play

/**
 * The base controller for password reset and password change operations
 *
 */
abstract class MailTokenBasedOperations[U] extends SecureSocial[U] {
  val Success = "success"
  val Error = "error"
  val Email = "email"
  val TokenDurationKey = "securesocial.userpass.tokenDuration"
  val DefaultDuration = 60
  val TokenDuration = Play.current.configuration.getInt(TokenDurationKey).getOrElse(DefaultDuration)

  val startForm = Form (
    Email -> email.verifying( nonEmpty )
  )

  /**
   * Creates a token for mail based operations
   *
   * @param email the email address
   * @param isSignUp a boolean indicating if the token is used for a signup or password reset operation
   * @return a MailToken instance
   */
  def createToken(email: String, isSignUp: Boolean): Future[MailToken] = {
    val uuid = UUID.randomUUID().toString
    val now = DateTime.now

    val token = MailToken(
      uuid, email.toLowerCase,
      now,
      now.plusMinutes(TokenDuration),
      isSignUp = isSignUp
    )
    import ExecutionContext.Implicits.global
    env.userService.saveToken(token).map(_ => token)
  }

  /**
   * Helper method to execute actions where a token needs to be retrieved from
   * the backing store
   *
   * @param token the token id
   * @param isSignUp a boolean indicating if the token is used for a signup or password reset operation
   * @param f the function that gets invoked if the token exists
   * @param request the current request
   * @return the action result
   */
  protected def executeForToken(token: String, isSignUp: Boolean,
                                f: MailToken => Future[SimpleResult])
                               (implicit request: RequestHeader): Future[SimpleResult] =
  {
    import ExecutionContext.Implicits.global
    env.userService.findToken(token).flatMap {
      case Some(t) if !t.isExpired && t.isSignUp == isSignUp => f(t)
      case _ =>
        val to = if ( isSignUp ) env.routes.signUpUrl else env.routes.resetPasswordUrl
        Future.successful(Redirect(to).flashing(Error -> Messages(BaseRegistration.InvalidLink)))
    }
  }

  /**
   * The result sent after the start page is handled
   *
   * @param request the current request
   * @return the action result
   */
  protected def handleStartResult()(implicit request: RequestHeader): SimpleResult = Redirect(env.routes.loginPageUrl)

  /**
   * The result sent after the operation has been completed by the user
   *
   * @param request the current request
   * @return the action result
   */
  protected def confirmationResult()(implicit request: RequestHeader): SimpleResult = Redirect(env.routes.loginPageUrl)
}

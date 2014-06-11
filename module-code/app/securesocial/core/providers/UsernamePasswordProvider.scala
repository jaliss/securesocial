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
package securesocial.core.providers

import org.joda.time.DateTime
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import securesocial.controllers.ViewTemplates
import securesocial.core.AuthenticationResult.{Authenticated, NavigationFlow}
import securesocial.core._
import securesocial.core.providers.utils.PasswordHasher
import securesocial.core.services.{AvatarService, UserService}

import scala.concurrent.Future


/**
 * A username password provider
 */
class UsernamePasswordProvider[U](userService: UserService[U],
                                  avatarService: Option[AvatarService],
                                  viewTemplates: ViewTemplates,
                                  passwordHashers: Map[String, PasswordHasher])
  extends IdentityProvider with ApiSupport
{

  override val id = UsernamePasswordProvider.UsernamePassword

  def authMethod = AuthenticationMethod.UserPassword

  val InvalidCredentials = "securesocial.login.invalidCredentials"


  def authenticateForApi(implicit request: Request[AnyContent]): Future[AuthenticationResult] = {
    doAuthentication(apiMode = true)
  }

  def authenticate()(implicit request: Request[AnyContent]): Future[AuthenticationResult] = {
    doAuthentication()
  }

  private def doAuthentication[A](apiMode: Boolean = false)(implicit request: Request[A]): Future[AuthenticationResult] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val form = UsernamePasswordProvider.loginForm.bindFromRequest()
    form.fold(
      errors => Future.successful {
        if ( apiMode )
          AuthenticationResult.Failed("Invalid credentials")
        else
          AuthenticationResult.NavigationFlow(badRequest(errors)(request))
      },
      credentials => {
        val userId = credentials._1.toLowerCase
        userService.find(id, userId).flatMap { maybeUser =>
            val loggedIn = for (
              user <- maybeUser;
              pinfo <- user.passwordInfo;
              hasher <- passwordHashers.get(pinfo.hasher) if hasher.matches(pinfo, credentials._2)
            ) yield {
              user
            }

            val authenticatedAndUpdated = for (
              u <- loggedIn ;
              service <- avatarService ;
              email <- u.email
            ) yield {
              service.urlFor(email).map {
                case avatar if avatar != u.avatarUrl => u.copy(avatarUrl = avatar)
                case _ => u
              } map {
                Authenticated
              }
            }

            authenticatedAndUpdated.getOrElse {
              Future.successful {
                if ( apiMode )
                  AuthenticationResult.Failed("Invalid credentials")
                else
                NavigationFlow(badRequest(UsernamePasswordProvider.loginForm, Some(InvalidCredentials)))
              }
            }
        }
      })
  }

  private def badRequest[A](f: Form[(String,String)], msg: Option[String] = None)(implicit request: Request[A]): SimpleResult = {
    Results.BadRequest(viewTemplates.getLoginPage(f, msg))
  }
}

object UsernamePasswordProvider {
  val UsernamePassword = "userpass"
  private val Key = "securesocial.userpass.withUserNameSupport"
  private val SendWelcomeEmailKey = "securesocial.userpass.sendWelcomeEmail"
  private val Hasher = "securesocial.userpass.hasher"
  private val EnableTokenJob = "securesocial.userpass.enableTokenJob"
  private val SignupSkipLogin = "securesocial.userpass.signupSkipLogin"

  val loginForm = Form(
    tuple(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )
  )

  lazy val withUserNameSupport = current.configuration.getBoolean(Key).getOrElse(false)
  lazy val sendWelcomeEmail = current.configuration.getBoolean(SendWelcomeEmailKey).getOrElse(true)
  lazy val hasher = current.configuration.getString(Hasher).getOrElse(PasswordHasher.id)
  lazy val enableTokenJob = current.configuration.getBoolean(EnableTokenJob).getOrElse(true)
  lazy val signupSkipLogin = current.configuration.getBoolean(SignupSkipLogin).getOrElse(false)
}

/**
  * A token used for reset password and sign up operations
 *
  * @param uuid the token id
  * @param email the user email
  * @param creationTime the creation time
  * @param expirationTime the expiration time
  * @param isSignUp a boolean indicating wether the token was created for a sign up action or not
  */
case class MailToken(uuid: String, email: String, creationTime: DateTime, expirationTime: DateTime, isSignUp: Boolean) {
  def isExpired = expirationTime.isBeforeNow
}

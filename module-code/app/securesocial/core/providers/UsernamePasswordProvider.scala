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

import io.methvin.play.autoconfig.AutoConfig
import org.joda.time.DateTime
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc._
import play.api.{ ConfigLoader, Configuration }
import securesocial.controllers.ViewTemplates
import securesocial.core.AuthenticationResult.{ Authenticated, NavigationFlow }
import securesocial.core._
import securesocial.core.providers.utils.PasswordHasher
import securesocial.core.services.{ AvatarService, UserService }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * A username password provider
 */
class UsernamePasswordProvider[U](
  userService: UserService[U],
  avatarService: Option[AvatarService],
  viewTemplates: ViewTemplates,
  passwordHashers: Map[String, PasswordHasher],
  val messagesApi: MessagesApi)(implicit val executionContext: ExecutionContext)
  extends IdentityProvider with ApiSupport with I18nSupport {

  override val id = UsernamePasswordProvider.UsernamePassword

  def authMethod = AuthenticationMethod.UserPassword

  val InvalidCredentials = "securesocial.login.invalidCredentials"

  def authenticateForApi(implicit request: Request[AnyContent]): Future[AuthenticationResult] = {
    doAuthentication(apiMode = true)
  }

  def authenticate()(implicit request: Request[AnyContent]): Future[AuthenticationResult] = {
    doAuthentication()
  }

  private def profileForCredentials(userId: String, password: String): Future[Option[BasicProfile]] = {
    userService.find(id, userId).map { maybeUser =>
      for (
        user <- maybeUser;
        pinfo <- user.passwordInfo;
        hasher <- passwordHashers.get(pinfo.hasher) if hasher.matches(pinfo, password)
      ) yield {
        user
      }
    }
  }

  protected def authenticationFailedResult[A](apiMode: Boolean)(implicit request: Request[A]) = Future.successful {
    if (apiMode)
      AuthenticationResult.Failed("Invalid credentials")
    else
      NavigationFlow(badRequest(UsernamePasswordProvider.loginForm, Some(InvalidCredentials)))
  }

  protected def withUpdatedAvatar(profile: BasicProfile): Future[BasicProfile] = {
    (avatarService, profile.email) match {
      case (Some(service), Some(e)) => service.urlFor(e).map {
        case url if url != profile.avatarUrl => profile.copy(avatarUrl = url)
        case _ => profile
      }
      case _ => Future.successful(profile)
    }
  }

  private def doAuthentication[A](apiMode: Boolean = false)(implicit request: Request[A]): Future[AuthenticationResult] = {
    val form = UsernamePasswordProvider.loginForm.bindFromRequest()
    form.fold(
      errors => Future.successful {
        if (apiMode)
          AuthenticationResult.Failed("Invalid credentials")
        else
          AuthenticationResult.NavigationFlow(badRequest(errors)(request))
      },
      credentials => {
        val userId = credentials._1.toLowerCase
        val password = credentials._2

        profileForCredentials(userId, password).flatMap {
          case Some(profile) => withUpdatedAvatar(profile).map(Authenticated)
          case None => authenticationFailedResult(apiMode)
        }
      })
  }

  private def badRequest[A](f: Form[(String, String)], msg: Option[String] = None)(implicit request: Request[A]): Result = {
    Results.BadRequest(viewTemplates.getLoginPage(f, msg))
  }
}

case class UsernamePasswordConfig(
  withUserNameSupport: Boolean,
  sendWelcomeEmail: Boolean,
  hasher: String,
  enableTokenJob: Boolean,
  signupSkipLogin: Boolean,
  minimumPasswordLength: Int)
object UsernamePasswordConfig {
  implicit val configLoader: ConfigLoader[UsernamePasswordConfig] = AutoConfig.loader
  def fromConfiguration(configuration: Configuration): UsernamePasswordConfig =
    configuration.get[UsernamePasswordConfig]("securesocial.userpass")
}

object UsernamePasswordProvider {
  val UsernamePassword = "userpass"

  val loginForm = Form(
    tuple(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText))
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

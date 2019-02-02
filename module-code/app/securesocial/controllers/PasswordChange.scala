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

import javax.inject.Inject

import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{ I18nSupport, Messages }
import play.api.mvc.{ ControllerComponents, Result }
import play.filters.csrf._
import securesocial.core.SecureSocial._
import securesocial.core._
import securesocial.core.providers.utils.PasswordValidator

import scala.concurrent.{ Await, Future }

/**
 * A default PasswordChange controller that uses the BasicProfile as the user type
 *
 * @param env An environment
 */
class PasswordChange @Inject() (
  override implicit val env: RuntimeEnvironment,
  val csrfAddToken: CSRFAddToken,
  val csrfCheck: CSRFCheck,
  val controllerComponents: ControllerComponents) extends BasePasswordChange

/**
 * A trait that defines the password change functionality
 *
 */
trait BasePasswordChange extends SecureSocial with I18nSupport {
  val CurrentPassword = "currentPassword"
  val InvalidPasswordMessage = "securesocial.passwordChange.invalidPassword"
  val NewPassword = "newPassword"
  val Password1 = "password1"
  val Password2 = "password2"
  val Success = "success"
  val Error = "error"
  val OkMessage = "securesocial.passwordChange.ok"

  val csrfAddToken: CSRFAddToken
  val csrfCheck: CSRFCheck
  val configuration: Configuration = env.configuration

  /**
   * The property that specifies the page the user is redirected to after changing the password.
   */
  val onPasswordChangeGoTo = "securesocial.onPasswordChangeGoTo"

  /** The redirect target of the handlePasswordChange action. */
  def onHandlePasswordChangeGoTo = configuration.get[Option[String]](onPasswordChangeGoTo).getOrElse(
    securesocial.controllers.routes.PasswordChange.page().url)

  /**
   * checks if the supplied password matches the stored one
   * @param suppliedPassword the password entered in the form
   * @param request the current request
   * @tparam A the type of the user object
   * @return a future boolean
   */
  def checkCurrentPassword[A](suppliedPassword: String)(implicit request: SecuredRequest[A, env.U]): Future[Boolean] = {
    env.userService.passwordInfoFor(request.user).map {
      case Some(info) =>
        env.passwordHashers.get(info.hasher).exists {
          _.matches(info, suppliedPassword)
        }
      case None => false
    }
  }

  private def execute[A](f: Form[ChangeInfo] => Future[Result])(implicit request: SecuredRequest[A, env.U]): Future[Result] = {
    val form = Form[ChangeInfo](
      mapping(
        CurrentPassword ->
          nonEmptyText.verifying(Messages(InvalidPasswordMessage), { suppliedPassword =>
            import scala.concurrent.duration._
            Await.result(checkCurrentPassword(suppliedPassword), 10.seconds)
          }),
        NewPassword ->
          tuple(
            Password1 -> nonEmptyText.verifying(PasswordValidator.constraint),
            Password2 -> nonEmptyText).verifying(Messages(BaseRegistration.PasswordsDoNotMatch), passwords => passwords._1 == passwords._2))((currentPassword, newPassword) => ChangeInfo(currentPassword, newPassword._1))((changeInfo: ChangeInfo) => Some(("", ("", "")))))

    env.userService.passwordInfoFor(request.user).flatMap {
      case Some(info) =>
        f(form)
      case None =>
        Future.successful(Forbidden)
    }
  }

  /**
   * Renders the password change page
   *
   * @return
   */
  def page = csrfAddToken {
    SecuredAction.async { implicit request =>
      execute { form: Form[ChangeInfo] =>
        Future.successful {
          Ok(env.viewTemplates.getPasswordChangePage(form))
        }
      }
    }
  }

  /**
   * Handles form submission from the password change page
   *
   * @return
   */
  def handlePasswordChange = csrfCheck {
    SecuredAction.async { implicit request =>
      execute { form: Form[ChangeInfo] =>
        form.bindFromRequest()(request).fold(
          errors => Future.successful(BadRequest(env.viewTemplates.getPasswordChangePage(errors))),
          info => {
            val newPasswordInfo = env.currentHasher.hash(info.newPassword)
            env.userService.updatePasswordInfo(request.user, newPasswordInfo).map {
              case Some(u) =>
                env.mailer.sendPasswordChangedNotice(u)(request, messagesApi.preferred(request))
                val result = Redirect(onHandlePasswordChangeGoTo).flashing(Success -> Messages(OkMessage))
                Events.fire(PasswordChangeEvent(request.user)).map(result.withSession).getOrElse(result)
              case None =>
                Redirect(onHandlePasswordChangeGoTo).flashing(Error -> Messages("securesocial.password.error"))
            }
          })
      }
    }
  }
}

/**
 * The class used in the form
 *
 * @param currentPassword the user's current password
 * @param newPassword the new password
 */
case class ChangeInfo(currentPassword: String, newPassword: String)

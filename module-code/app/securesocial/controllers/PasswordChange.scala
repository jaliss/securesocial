/**
 * Copyright 2012 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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

import securesocial.core._
import play.api.mvc.{AnyContent, Result, Controller}
import com.typesafe.plugin._
import play.api.Play
import Play.current
import play.api.data.Form
import play.api.data.Forms._
import securesocial.core.providers.utils.{Mailer, RoutesHelper, PasswordHasher, PasswordValidator}
import play.api.i18n.Messages
import securesocial.core.SecuredRequest
import scala.Some

/**
 * A controller to provide password change functionality
 */
object PasswordChange extends Controller with SecureSocial {
  val CurrentPassword = "currentPassword"
  val InvalidPasswordMessage = "securesocial.passwordChange.invalidPassword"
  val NewPassword = "newPassword"
  val Password1 = "password1"
  val Password2 = "password2"
  val Success = "success"
  val OkMessage = "securesocial.passwordChange.ok"


  case class ChangeInfo(currentPassword: String, newPassword: String)


  def checkCurrentPassword[A](currentPassword: String)(implicit request: SecuredRequest[A]):Boolean = {
    use[PasswordHasher].matches(request.user.passwordInfo.get, currentPassword)
  }

  private def execute[A](f: (SecuredRequest[A], Form[ChangeInfo]) => Result)(implicit request: SecuredRequest[A]): Result = {
    val form = Form[ChangeInfo](
      mapping(
        CurrentPassword -> nonEmptyText.verifying(
          Messages(InvalidPasswordMessage), checkCurrentPassword(_)),
        (NewPassword ->
          tuple(
            Password1 -> nonEmptyText.verifying( use[PasswordValidator].errorMessage,
              p => use[PasswordValidator].isValid(p)),
            Password2 -> nonEmptyText
          ).verifying(Messages(Registration.PasswordsDoNotMatch), passwords => passwords._1 == passwords._2)
          )

      )((currentPassword, newPassword) => ChangeInfo(currentPassword, newPassword._1))
        ((changeInfo: ChangeInfo) => Some("", ("", "")))
    )

    if ( request.user.authMethod != AuthenticationMethod.UserPassword) {
      Forbidden
    } else {
      f(request, form)
    }
  }

  def page = SecuredAction { implicit request =>
    execute { (request: SecuredRequest[AnyContent], form: Form[ChangeInfo]) =>
      Ok(use[TemplatesPlugin].getPasswordChangePage(request, form))
    }
  }

  def handlePasswordChange = SecuredAction { implicit request =>
    execute { (request: SecuredRequest[AnyContent], form: Form[ChangeInfo]) =>
      form.bindFromRequest()(request).fold (
        errors => BadRequest(use[TemplatesPlugin].getPasswordChangePage(request, errors)),
        info =>  {
          val newPasswordInfo = use[PasswordHasher].hash(info.newPassword)
          val u = SocialUser(request.user).copy( passwordInfo = Some(newPasswordInfo))
          UserService.save( u )
          Mailer.sendPasswordChangedNotice(u)(request)
          Redirect(RoutesHelper.changePasswordPage()).flashing(Success -> Messages(OkMessage))
        }
      )
    }
  }
}

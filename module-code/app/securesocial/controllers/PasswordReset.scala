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

import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.mvc.Action
import securesocial.core._
import securesocial.core.providers.UsernamePasswordProvider
import securesocial.core.providers.utils.PasswordValidator
import securesocial.core.services.SaveMode

import scala.concurrent.Future

/**
 * A default controller the uses the BasicProfile as the user type
 *
 * @param env an environment
 */
class PasswordReset(override implicit val env: RuntimeEnvironment[BasicProfile]) extends BasePasswordReset[BasicProfile]

/**
 * The trait that provides the Password Reset functionality
 *
 * @tparam U the user type
 */
trait BasePasswordReset[U] extends MailTokenBasedOperations[U] {
  private val logger = play.api.Logger("securesocial.controllers.BasePasswordReset")

  val PasswordUpdated = "securesocial.password.passwordUpdated"
  val ErrorUpdatingPassword = "securesocial.password.error"

  val changePasswordForm = Form (
    BaseRegistration.Password ->
      tuple(
        BaseRegistration.Password1 -> nonEmptyText.verifying( PasswordValidator.constraint ),
        BaseRegistration.Password2 -> nonEmptyText
      ).verifying(Messages(BaseRegistration.PasswordsDoNotMatch), passwords => passwords._1 == passwords._2)
  )

  /**
   * Renders the page that starts the password reset flow
   */
  def startResetPassword = Action {
    implicit request =>
      Ok(env.viewTemplates.getStartResetPasswordPage(startForm))
  }

  /**
   * Handles form submission for the start page
   */
  def handleStartResetPassword = Action.async {
    implicit request =>
      import scala.concurrent.ExecutionContext.Implicits.global
      startForm.bindFromRequest.fold(
        errors => Future.successful(BadRequest(env.viewTemplates.getStartResetPasswordPage(errors))),
        email => env.userService.findByEmailAndProvider(email, UsernamePasswordProvider.UsernamePassword).map {
          maybeUser =>
            maybeUser match {
              case Some(user) =>
                createToken(email, isSignUp = false).map {
                  token =>
                    env.mailer.sendPasswordResetEmail(user, token.uuid)
                }
              case None =>
                env.mailer.sendUnkownEmailNotice(email)
            }
            handleStartResult().flashing(Success -> Messages(BaseRegistration.ThankYouCheckEmail))
        }
      )
  }

  /**
   * Renders the reset password page
   *
   * @param token the token that identifies the user request
   */
  def resetPassword(token: String) = Action.async {
    implicit request =>
      executeForToken(token, false, {
        t =>
          Future.successful(Ok(env.viewTemplates.getResetPasswordPage(changePasswordForm, token)))
      })
  }

  /**
   * Handles the reset password page submission
   *
   * @param token the token that identifies the user request
   */
  def handleResetPassword(token: String) = Action.async { implicit request =>
    import scala.concurrent.ExecutionContext.Implicits.global
    executeForToken(token, false, {
      t => changePasswordForm.bindFromRequest.fold(errors =>
          Future.successful(BadRequest(env.viewTemplates.getResetPasswordPage(errors, token))),
      p =>
          env.userService.findByEmailAndProvider(t.email, UsernamePasswordProvider.UsernamePassword).flatMap {
            case Some(profile) =>
              val hashed = env.currentHasher.hash(p._1)
              for (
                updated <- env.userService.save(profile.copy(passwordInfo = Some(hashed)), SaveMode.PasswordChange);
                deleted <- env.userService.deleteToken(token)
              ) yield {
                env.mailer.sendPasswordChangedNotice(profile)
                val eventSession = Events.fire(new PasswordResetEvent(updated)).getOrElse(request.session)
                confirmationResult().withSession(eventSession).flashing(Success -> Messages(PasswordUpdated))
              }
            case _ =>
              logger.error("[securesocial] could not find user with email %s during password reset".format(t.email))
              Future.successful(confirmationResult().flashing(Error -> Messages(ErrorUpdatingPassword)))
          }
      )
    })
  }
}

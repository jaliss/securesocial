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

import play.api.data.Forms._
import play.api.data._
import play.api.i18n.Messages
import play.api.mvc.{Request, Action, Result, RequestHeader}
import securesocial.core._
import securesocial.core.authenticator.CookieAuthenticator
import securesocial.core.providers.UsernamePasswordProvider
import securesocial.core.providers.utils._
import securesocial.core.services.SaveMode
import scala.concurrent.{ Await, ExecutionContext, Future }

/**
 * A default Registration controller that uses the BasicProfile as the user type
 *
 * @param env the environment
 */
class Registration(override implicit val env: RuntimeEnvironment[BasicProfile]) extends BaseRegistration[BasicProfile]

/**
 * A trait that provides the means to handle user registration
 *
 * @tparam U the user type
 */
trait BaseRegistration[U <: GenericProfile] extends MailTokenBasedOperations[U] {

  import securesocial.controllers.BaseRegistration._

  private val logger = play.api.Logger("securesocial.controllers.Registration")

  val providerId = UsernamePasswordProvider.UsernamePassword

  val UserName = "userName"
  val FirstName = "firstName"
  val LastName = "lastName"

  val formWithUsername = Form[RegistrationInfo](
    mapping(
      UserName -> nonEmptyText.verifying(Messages(UserNameAlreadyTaken), userName => {
        env.userService.find(providerId, userName).isEmpty
      }),
      FirstName -> nonEmptyText,
      LastName -> nonEmptyText,
      Password ->
        tuple(
          Password1 -> nonEmptyText.verifying(PasswordValidator.constraint),
          Password2 -> nonEmptyText).verifying(Messages(PasswordsDoNotMatch), passwords => passwords._1 == passwords._2)) // binding
          ((userName, firstName, lastName, password) => RegistrationInfo(Some(userName), firstName, lastName, password._1)) // unbinding
          (info => Some(info.userName.getOrElse(""), info.firstName, info.lastName, ("", ""))))

  val formWithoutUsername = Form[RegistrationInfo](
    mapping(
      FirstName -> nonEmptyText,
      LastName -> nonEmptyText,
      Password ->
        tuple(
          Password1 -> nonEmptyText.verifying(PasswordValidator.constraint),
          Password2 -> nonEmptyText).verifying(Messages(PasswordsDoNotMatch), passwords => passwords._1 == passwords._2)) // binding
          ((firstName, lastName, password) => RegistrationInfo(None, firstName, lastName, password._1)) // unbinding
          (info => Some(info.firstName, info.lastName, ("", ""))))

  val form = if (UsernamePasswordProvider.withUserNameSupport) formWithUsername else formWithoutUsername

  /**
   * Starts the sign up process
   */
  def startSignUp = Action {
    implicit request =>
      if (SecureSocial.enableRefererAsOriginalUrl) {
        SecureSocial.withRefererAsOriginalUrl(Ok(env.viewTemplates.getStartSignUpPage(startForm)))
      } else {
        Ok(env.viewTemplates.getStartSignUpPage(startForm))
      }
  }

  def handleStartSignUp = Action { implicit request =>
    startForm.bindFromRequest.fold(
      errors => {
        BadRequest(env.viewTemplates.getStartSignUpPage(errors))
      },
      e => {
        val email = e.toLowerCase
        val maybeUser = env.userService.findByEmailAndProvider(email, UsernamePasswordProvider.UsernamePassword);
        maybeUser match {
          case Some(user) =>
            // user signed up already, send an email offering to login/recover password
            env.mailer.sendAlreadyRegisteredEmail(user)
          case None =>
            import scala.concurrent.ExecutionContext.Implicits.global
            val token = createToken(email, isSignUp = true)
              env.mailer.sendSignUpEmail(email, token.uuid)
              env.userService.saveToken(token)

        }
        handleStartResult().flashing(Success -> Messages(ThankYouCheckEmail), Email -> email)
      })
  }

  /**
   * Renders the sign up page
   * @return
   */
  def signUp(token: String) = Action{
    implicit request =>
      logger.debug("[securesocial] trying sign up with token %s".format(token))
      executeForToken(token, true, {
        _ =>
          Ok(env.viewTemplates.getSignUpPage(form, token))
      })
  }

  /**
   * Handles posts from the sign up page
   */
  def handleSignUp(token: String) = Action {   implicit request =>
      executeForToken(token, true, {
        t =>
          form.bindFromRequest.fold(
            errors => {
              logger.debug("[securesocial] errors " + errors)
              BadRequest(env.viewTemplates.getSignUpPage(errors, t.uuid))
            },
            info => {
              val id = if (UsernamePasswordProvider.withUserNameSupport) info.userName.get else t.email
              val newUser = BasicProfile(
                providerId,
                id,
                Some(info.firstName),
                Some(info.lastName),
                Some("%s %s".format(info.firstName, info.lastName)),
                Some(t.email),
                None,
                AuthenticationMethod.UserPassword,
                passwordInfo = Some(env.currentHasher.hash(info.password)))

              val withAvatar = env.avatarService.map { avatarService =>
                  val avatarUrl = avatarService.urlFor(t.email)
                  if (avatarUrl != newUser.avatarUrl) {
                    newUser.copy(avatarUrl = avatarUrl)
                  } else {
                    newUser
                  }
                }.getOrElse(newUser)

              val saved = env.userService.save(withAvatar, SaveMode.SignUp);
              val deleted = env.userService.deleteToken(t.uuid);

              if (UsernamePasswordProvider.sendWelcomeEmail) {
                env.mailer.sendWelcomeEmail(newUser)
              }

              val eventSession = Events.fire(new SignUpEvent(saved)).getOrElse(request.session)

              val result : Result = if (UsernamePasswordProvider.signupSkipLogin) {
                skipLogin(saved)
              } else {
                confirmationResult().flashing(Success -> Messages(SignUpDone)).withSession(eventSession)
              }
              result;
            })
      })
  }

  private def skipLogin(user : U)(implicit request : RequestHeader): Result = {
    import securesocial.core.utils._

    env.authenticatorService.find(CookieAuthenticator.Id).map {authBuilder =>
      val  authenticator = authBuilder.fromUser(user)
      confirmationResult().flashing(Success -> Messages(SignUpDone)).startingAuthenticator(authenticator)
    } getOrElse {
      logger.error(s"[securesocial] There isn't CookieAuthenticator registered in the RuntimeEnvironment")
      confirmationResult().flashing(Error -> Messages("There was an error signing you up"))
    }
  }
}

object BaseRegistration {
  val UserNameAlreadyTaken = "securesocial.signup.userNameAlreadyTaken"
  val ThankYouCheckEmail = "securesocial.signup.thankYouCheckEmail"
  val InvalidLink = "securesocial.signup.invalidLink"
  val SignUpDone = "securesocial.signup.signUpDone"
  val Password = "password"
  val Password1 = "password1"
  val Password2 = "password2"

  val PasswordsDoNotMatch = "securesocial.signup.passwordsDoNotMatch"
}

/**
 * The data collected during the registration process
 *
 * @param userName the username
 * @param firstName the first name
 * @param lastName the last name
 * @param password the password
 */
case class RegistrationInfo(userName: Option[String], firstName: String, lastName: String, password: String)

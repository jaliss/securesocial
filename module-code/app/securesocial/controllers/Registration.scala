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

import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{ Messages, MessagesApi }
import play.api.mvc.{ BaseController, ControllerComponents, RequestHeader }
import play.filters.csrf._
import securesocial.core._
import securesocial.core.authenticator.CookieAuthenticator
import securesocial.core.providers.UsernamePasswordProvider
import securesocial.core.providers.utils._
import securesocial.core.services.SaveMode

import scala.concurrent.{ Await, Future }

/**
 * A default Registration controller that uses the BasicProfile as the user type
 *
 * @param env the environment
 */
class Registration @Inject() (
  override implicit val env: RuntimeEnvironment,
  val csrfAddToken: CSRFAddToken,
  val csrfCheck: CSRFCheck,
  val controllerComponents: ControllerComponents) extends BaseRegistration

/**
 * A trait that provides the means to handle user registration
 *
 */
trait BaseRegistration extends MailTokenBasedOperations with BaseController {

  import securesocial.controllers.BaseRegistration._

  private val logger = play.api.Logger("securesocial.controllers.Registration")

  val providerId = UsernamePasswordProvider.UsernamePassword

  val UserName = "userName"
  val FirstName = "firstName"
  val LastName = "lastName"

  override def messagesApi: MessagesApi = env.messagesApi

  def formWithUsername(implicit request: RequestHeader) = Form[RegistrationInfo](
    mapping(
      UserName -> nonEmptyText.verifying(Messages(UserNameAlreadyTaken), userName => {
        // todo: see if there's a way to avoid waiting here :-\
        import scala.concurrent.duration._
        Await.result(env.userService.find(providerId, userName), 20.seconds).isEmpty
      }),
      FirstName -> nonEmptyText,
      LastName -> nonEmptyText,
      Password ->
        tuple(
          Password1 -> nonEmptyText.verifying(PasswordValidator.constraint),
          Password2 -> nonEmptyText).verifying(Messages(PasswordsDoNotMatch), passwords => passwords._1 == passwords._2)) // binding
          ((userName, firstName, lastName, password) => RegistrationInfo(Some(userName), firstName, lastName, password._1)) // unbinding
          (info => Some((info.userName.getOrElse(""), info.firstName, info.lastName, ("", "")))))

  def formWithoutUsername(implicit request: RequestHeader) = Form[RegistrationInfo](
    mapping(
      FirstName -> nonEmptyText,
      LastName -> nonEmptyText,
      Password ->
        tuple(
          Password1 -> nonEmptyText.verifying(PasswordValidator.constraint),
          Password2 -> nonEmptyText).verifying(Messages(PasswordsDoNotMatch), passwords => passwords._1 == passwords._2)) // binding
          ((firstName, lastName, password) => RegistrationInfo(None, firstName, lastName, password._1)) // unbinding
          (info => Some((info.firstName, info.lastName, ("", "")))))

  def form(implicit request: RequestHeader) =
    if (env.usernamePasswordConfig.withUserNameSupport) formWithUsername else formWithoutUsername

  val csrfAddToken: CSRFAddToken
  val csrfCheck: CSRFCheck

  /**
   * Starts the sign up process
   */
  def startSignUp = csrfAddToken {
    Action {
      implicit request =>
        if (env.enableRefererAsOriginalUrl.value) {
          SecureSocial.withRefererAsOriginalUrl(Ok(env.viewTemplates.getStartSignUpPage(startForm)))
        } else {
          Ok(env.viewTemplates.getStartSignUpPage(startForm))
        }
    }
  }

  def handleStartSignUp = csrfCheck {
    Action.async {
      implicit request =>
        startForm.bindFromRequest.fold(
          errors => {
            Future.successful(BadRequest(env.viewTemplates.getStartSignUpPage(errors)))
          },
          e => {
            val email = e.toLowerCase
            // check if there is already an account for this email address
            env.userService.findByEmailAndProvider(email, UsernamePasswordProvider.UsernamePassword).map {
              maybeUser =>
                maybeUser match {
                  case Some(user) =>
                    // user signed up already, send an email offering to login/recover password
                    env.mailer.sendAlreadyRegisteredEmail(user)
                  case None =>
                    createToken(email, isSignUp = true).flatMap { token =>
                      env.mailer.sendSignUpEmail(email, token.uuid)
                      env.userService.saveToken(token)
                    }
                }
                handleStartResult().flashing(Success -> Messages(ThankYouCheckEmail), Email -> email)
            }
          })
    }
  }

  /**
   * Renders the sign up page
   * @return
   */
  def signUp(token: String) = csrfAddToken {
    Action.async {
      implicit request =>
        logger.debug("[securesocial] trying sign up with token %s".format(token))
        executeForToken(token, true, {
          _ =>
            Future.successful(Ok(env.viewTemplates.getSignUpPage(form, token)))
        })
    }
  }

  /**
   * Handles posts from the sign up page
   */
  def handleSignUp(token: String) = csrfCheck {
    Action.async {
      implicit request =>
        executeForToken(token, true, {
          t =>
            form.bindFromRequest.fold(
              errors => {
                logger.debug("[securesocial] errors " + errors)
                Future.successful(BadRequest(env.viewTemplates.getSignUpPage(errors, t.uuid)))
              },
              info => {
                val id = if (env.usernamePasswordConfig.withUserNameSupport) info.userName.get else t.email
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

                val withAvatar = env.avatarService.map {
                  _.urlFor(t.email).map { url =>
                    if (url != newUser.avatarUrl) newUser.copy(avatarUrl = url) else newUser
                  }
                }.getOrElse(Future.successful(newUser))

                import securesocial.core.utils._
                val result = for (
                  toSave <- withAvatar;
                  saved <- env.userService.save(toSave, SaveMode.SignUp);
                  deleted <- env.userService.deleteToken(t.uuid)
                ) yield {
                  if (env.usernamePasswordConfig.sendWelcomeEmail)
                    env.mailer.sendWelcomeEmail(newUser)
                  val eventSession = Events.fire(new SignUpEvent(saved)).getOrElse(request.session)
                  if (env.usernamePasswordConfig.signupSkipLogin) {
                    env.authenticatorService.find(CookieAuthenticator.Id).map {
                      _.fromUser(saved).flatMap { authenticator =>
                        confirmationResult()
                          .flashing(Success -> Messages(SignUpDone))
                          .withSession(eventSession - SecureSocial.OriginalUrlKey - IdentityProvider.SessionId)
                          .startingAuthenticator(authenticator)
                      }
                    } getOrElse {
                      logger.error("[securesocial] There isn't CookieAuthenticator registered in the RuntimeEnvironment")
                      Future.successful(confirmationResult().flashing(Error -> Messages("There was an error signing you up")))
                    }
                  } else {
                    Future.successful(confirmationResult().flashing(Success -> Messages(SignUpDone)).withSession(eventSession))
                  }
                }
                result.flatMap(f => f)
              })
        })
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

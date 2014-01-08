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

import play.api.mvc.{RequestHeader, Request}
import play.api.templates.{Html, Txt}
import play.api.{Logger, Plugin, Application}
import securesocial.core.{Identity, SecuredRequest, SocialUser}
import play.api.data.Form
import securesocial.controllers.Registration.RegistrationInfo
import securesocial.controllers.PasswordChange.ChangeInfo
import play.api.i18n.Lang


/**
 * A trait that defines methods that return the html pages and emails for SecureSocial.
 *
 * If you need to customise the views just create a new plugin
 * and register it instead of DefaultTemplatesPlugin in the play.plugins file of your app.
 *
 * @see DefaultViewsPlugins
 */
trait TemplatesPlugin extends Plugin {
  override def onStart() {
    Logger.info("[securesocial] loaded templates plugin: %s".format(getClass.getName))
  }

  /**
   * Returns the html for the login page
   * @param request
   * @tparam A
   * @return
   */
  def getLoginPage[A](form: Form[(String, String)], msg: Option[String] = None)(implicit request: Request[A], lang: Lang): Html

  /**
   * Returns the html for the signup page
   *
   * @param request
   * @tparam A
   * @return
   */
  def getSignUpPage[A](form: Form[RegistrationInfo], token: String)(implicit request: Request[A], lang: Lang): Html

  /**
   * Returns the html for the start signup page
   *
   * @param request
   * @tparam A
   * @return
   */
  def getStartSignUpPage[A](form: Form[String])(implicit request: Request[A], lang: Lang): Html

  /**
   * Returns the html for the reset password page
   *
   * @param request
   * @tparam A
   * @return
   */
  def getResetPasswordPage[A](form: Form[(String, String)], token: String)(implicit request: Request[A], lang: Lang): Html

  /**
   * Returns the html for the start reset page
   *
   * @param request
   * @tparam A
   * @return
   */
  def getStartResetPasswordPage[A](form: Form[String])(implicit request: Request[A], lang: Lang): Html

  /**
   * Returns the html for the change password page
   *
   * @param request
   * @param form
   * @tparam A
   * @return
   */
  def getPasswordChangePage[A](form: Form[ChangeInfo])(implicit request: Request[A], lang: Lang): Html

  /**
   * Returns the html for the not authorized page
   *
   * @param request
   * @tparam A
   * @return
   */
  def getNotAuthorizedPage[A](implicit request: Request[A], lang: Lang): Html

  /**
   * Returns the email sent when a user starts the sign up process
   *
   * @param token the token used to identify the request
   * @param request the current http request
   * @return a String with the text and/or html body for the email
   */
  def getSignUpEmail(token: String)(implicit request: RequestHeader, lang: Lang): (Option[Txt], Option[Html])

  /**
   * Returns the email sent when the user is already registered
   *
   * @param user the user
   * @param request the current request
   * @return a tuple with the text and/or html body for the email
   */
  def getAlreadyRegisteredEmail(user: Identity)(implicit request: RequestHeader, lang: Lang): (Option[Txt], Option[Html])

  /**
   * Returns the welcome email sent when the user finished the sign up process
   *
   * @param user the user
   * @param request the current request
   * @return a String with the text and/or html body for the email
   */
  def getWelcomeEmail(user: Identity)(implicit request: RequestHeader, lang: Lang): (Option[Txt], Option[Html])

  /**
   * Returns the email sent when a user tries to reset the password but there is no account for
   * that email address in the system
   *
   * @param request the current request
   * @return a String with the text and/or html body for the email
   */
  def getUnknownEmailNotice()(implicit request: RequestHeader, lang: Lang): (Option[Txt], Option[Html])

  /**
   * Returns the email sent to the user to reset the password
   *
   * @param user the user
   * @param token the token used to identify the request
   * @param request the current http request
   * @return a String with the text and/or html body for the email
   */
  def getSendPasswordResetEmail(user: Identity, token: String)(implicit request: RequestHeader, lang: Lang): (Option[Txt], Option[Html])

  /**
   * Returns the email sent as a confirmation of a password change
   *
   * @param user the user
   * @param request the current http request
   * @return a String with the text and/or html body for the email
   */
  def getPasswordChangedNoticeEmail(user: Identity)(implicit request: RequestHeader, lang: Lang): (Option[Txt], Option[Html])

}

/**
 * The default views plugin.  If you need to customise the views just create a new plugin that
 * extends TemplatesPlugin and register it in the play.plugins file instead of this one.
 *
 * @param application
 */
class DefaultTemplatesPlugin(application: Application) extends TemplatesPlugin {
  def getLoginPage[A](form: Form[(String, String)],msg: Option[String] = None)(implicit request: Request[A], lang: Lang): Html =
  {
    securesocial.views.html.login(form, msg)
  }

  def getSignUpPage[A](form: Form[RegistrationInfo], token: String)(implicit request: Request[A], lang: Lang): Html = {
    securesocial.views.html.Registration.signUp(form, token)
  }

  def getStartSignUpPage[A](form: Form[String])(implicit request: Request[A], lang: Lang): Html = {
    securesocial.views.html.Registration.startSignUp(form)
  }

  def getStartResetPasswordPage[A](form: Form[String])(implicit request: Request[A], lang: Lang): Html = {
    securesocial.views.html.Registration.startResetPassword(form)
  }

  def getResetPasswordPage[A](form: Form[(String, String)], token: String)(implicit request: Request[A], lang: Lang): Html = {
    securesocial.views.html.Registration.resetPasswordPage(form, token)
  }

  def getPasswordChangePage[A](form: Form[ChangeInfo])(implicit request: Request[A], lang: Lang):Html = {
    securesocial.views.html.passwordChange(form)
  }

  def getNotAuthorizedPage[A](implicit request: Request[A], lang: Lang): Html = {
    securesocial.views.html.notAuthorized()
  }

  def getSignUpEmail(token: String)(implicit request: RequestHeader, lang: Lang): (Option[Txt], Option[Html]) = {
    (None, Some(securesocial.views.html.mails.signUpEmail(token)))
  }

  def getAlreadyRegisteredEmail(user: Identity)(implicit request: RequestHeader, lang: Lang): (Option[Txt], Option[Html]) = {
    (None, Some(securesocial.views.html.mails.alreadyRegisteredEmail(user)))
  }

  def getWelcomeEmail(user: Identity)(implicit request: RequestHeader, lang: Lang): (Option[Txt], Option[Html]) = {
    (None, Some(securesocial.views.html.mails.welcomeEmail(user)))
  }

  def getUnknownEmailNotice()(implicit request: RequestHeader, lang: Lang): (Option[Txt], Option[Html]) = {
    (None, Some(securesocial.views.html.mails.unknownEmailNotice()))
  }

  def getSendPasswordResetEmail(user: Identity, token: String)(implicit request: RequestHeader, lang: Lang): (Option[Txt], Option[Html]) = {
    (None, Some(securesocial.views.html.mails.passwordResetEmail(user, token)))
  }

  def getPasswordChangedNoticeEmail(user: Identity)(implicit request: RequestHeader, lang: Lang): (Option[Txt], Option[Html]) = {
    (None, Some(securesocial.views.html.mails.passwordChangedNotice(user)))
  }
}

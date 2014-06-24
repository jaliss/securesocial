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

import play.api.mvc.{Controller, RequestHeader}
import play.api.templates.{Html, Txt}
import securesocial.core.{BasicProfile, RuntimeEnvironment}
import play.api.data.Form
import play.api.i18n.Lang

/**
 * A trait that provides the pages for SecureSocial
 *
 * If you need to customise the views just create a class implementing this trait
 * and register it in your RuntimeEnvironment instead of the default one.
 *
 * @see ViewTemplates.Default
 * @see RuntimeEnvironment
 */
trait ViewTemplates extends Controller {
  /**
   * Returns the html for the login page
   */
  def getLoginPage(form: Form[(String, String)], msg: Option[String] = None)(implicit request: RequestHeader, lang: Lang): Html

  /**
   * Returns the html for the signup page
   */
  def getSignUpPage(form: Form[RegistrationInfo], token: String)(implicit request: RequestHeader, lang: Lang): Html

  /**
   * Returns the html for the start signup page
   */
  def getStartSignUpPage(form: Form[String])(implicit request: RequestHeader, lang: Lang): Html

  /**
   * Returns the html for the reset password page
   */
  def getResetPasswordPage(form: Form[(String, String)], token: String)(implicit request: RequestHeader, lang: Lang): Html

  /**
   * Returns the html for the start reset page
   */
  def getStartResetPasswordPage(form: Form[String])(implicit request: RequestHeader, lang: Lang): Html

  /**
   * Returns the html for the change password page
   */
  def getPasswordChangePage(form: Form[ChangeInfo])(implicit request: RequestHeader, lang: Lang): Html

  /**
   * Returns the html for the not authorized page
   */
  def getNotAuthorizedPage(implicit request: RequestHeader, lang: Lang): Html
}

/**
 * A trait that provides the mail content sent by SecureSocial
 */
trait MailTemplates extends Controller {
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
  def getAlreadyRegisteredEmail(user: BasicProfile)(implicit request: RequestHeader, lang: Lang): (Option[Txt], Option[Html])

  /**
   * Returns the welcome email sent when the user finished the sign up process
   *
   * @param user the user
   * @param request the current request
   * @return a String with the text and/or html body for the email
   */
  def getWelcomeEmail(user: BasicProfile)(implicit request: RequestHeader, lang: Lang): (Option[Txt], Option[Html])

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
  def getSendPasswordResetEmail(user: BasicProfile, token: String)(implicit request: RequestHeader, lang: Lang): (Option[Txt], Option[Html])

  /**
   * Returns the email sent as a confirmation of a password change
   *
   * @param user the user
   * @param request the current http request
   * @return a String with the text and/or html body for the email
   */
  def getPasswordChangedNoticeEmail(user: BasicProfile)(implicit request: RequestHeader, lang: Lang): (Option[Txt], Option[Html])

}

object ViewTemplates {
  /**
   * The default views.
   */
  class Default(env: RuntimeEnvironment[_]) extends ViewTemplates {
    implicit val implicitEnv = env

    override def getLoginPage(form: Form[(String, String)],
                              msg: Option[String] = None)(implicit request: RequestHeader, lang: Lang): Html = {
      securesocial.views.html.login(form, msg)(request, lang, env)
    }

    override def getSignUpPage(form: Form[RegistrationInfo], token: String)(implicit request: RequestHeader, lang: Lang): Html = {
      securesocial.views.html.Registration.signUp(form, token)(request, lang, env)
    }

    override def getStartSignUpPage(form: Form[String])(implicit request: RequestHeader, lang: Lang): Html = {
      securesocial.views.html.Registration.startSignUp(form)(request, lang, env)
    }

    override def getStartResetPasswordPage(form: Form[String])(implicit request: RequestHeader, lang: Lang): Html = {
      securesocial.views.html.Registration.startResetPassword(form)(request, lang, env)
    }

    override def getResetPasswordPage(form: Form[(String, String)], token: String)(implicit request: RequestHeader, lang: Lang): Html = {
      securesocial.views.html.Registration.resetPasswordPage(form, token)(request, lang, env)
    }

    override def getPasswordChangePage(form: Form[ChangeInfo])(implicit request: RequestHeader, lang: Lang): Html = {
      securesocial.views.html.passwordChange(form)(request, lang, env)
    }

    def getNotAuthorizedPage(implicit request: RequestHeader, lang: Lang): Html = {
      securesocial.views.html.notAuthorized()(request, lang, env)
    }
  }
}

object MailTemplates {
  /**
   * The default mails.
   */
  class Default(env: RuntimeEnvironment[_]) extends MailTemplates {
    implicit val implicitEnv = env
    def getSignUpEmail(token: String)(implicit request: RequestHeader, lang: Lang): (Option[Txt], Option[Html]) = {
      (None, Some(securesocial.views.html.mails.signUpEmail(token)(request, lang)))
    }

    def getAlreadyRegisteredEmail(user: BasicProfile)(implicit request: RequestHeader, lang: Lang): (Option[Txt], Option[Html]) = {
      (None, Some(securesocial.views.html.mails.alreadyRegisteredEmail(user)(request, lang, env)))
    }

    def getWelcomeEmail(user: BasicProfile)(implicit request: RequestHeader, lang: Lang): (Option[Txt], Option[Html]) = {
      (None, Some(securesocial.views.html.mails.welcomeEmail(user)(request, lang, env)))
    }

    def getUnknownEmailNotice()(implicit request: RequestHeader, lang: Lang): (Option[Txt], Option[Html]) = {
      (None, Some(securesocial.views.html.mails.unknownEmailNotice()(request, lang)))
    }

    def getSendPasswordResetEmail(user: BasicProfile, token: String)(implicit request: RequestHeader, lang: Lang): (Option[Txt], Option[Html]) = {
      (None, Some(securesocial.views.html.mails.passwordResetEmail(user, token)(request, lang, env)))
    }

    def getPasswordChangedNoticeEmail(user: BasicProfile)(implicit request: RequestHeader, lang: Lang): (Option[Txt], Option[Html]) = {
      (None, Some(securesocial.views.html.mails.passwordChangedNotice(user)(request, lang, env)))
    }
  }
}
/**
 * Copyright 2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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
package securesocial.core.services

import play.api.mvc.{ Call, RequestHeader }
import securesocial.core.IdentityProvider

/**
 * A RoutesService that resolves the routes for some of the pages
 */
trait RoutesService {
  /**
   * The login page url
   */
  def loginPageUrl(implicit req: RequestHeader): String

  /**
   * The page that starts the sign up flow
   */
  def startSignUpUrl(implicit req: RequestHeader): String

  /**
   * The url that processes submissions from the start sign up page
   */
  def handleStartSignUpUrl(implicit req: RequestHeader): String

  /**
   * The sign up page
   */
  def signUpUrl(mailToken: String)(implicit req: RequestHeader): String

  /**
   * The url that processes submissions from the sign up page
   */
  def handleSignUpUrl(mailToken: String)(implicit req: RequestHeader): String

  /**
   * The page that starts the reset password flow
   */
  def startResetPasswordUrl(implicit req: RequestHeader): String

  /**
   * The url that processes submissions from the start reset password page
   */
  def handleStartResetPasswordUrl(implicit req: RequestHeader): String

  /**
   * The reset password page
   */
  def resetPasswordUrl(mailToken: String)(implicit req: RequestHeader): String

  /**
   * The url that processes submissions from the reset password page
   */
  def handleResetPasswordUrl(mailToken: String)(implicit req: RequestHeader): String

  /**
   * The password change page
   */
  def passwordChangeUrl(implicit req: RequestHeader): String

  /**
   * The url that processes submissions from the password change page
   */
  def handlePasswordChangeUrl(implicit req: RequestHeader): String

  /**
   * The url to start an authentication flow with the given provider
   */
  def authenticationUrl(provider: String, redirectTo: Option[String] = None)(implicit req: RequestHeader): String
  def faviconPath: Call
  def jqueryPath: Call
  def customCssPath: Option[Call]
}

object RoutesService {
  /**
   * The default RoutesService implementation.  It points to the routes
   * defined by the built in controllers.
   */
  class Default extends RoutesService {
    private val logger = play.api.Logger("securesocial.core.DefaultRoutesService")
    lazy val conf = play.api.Play.current.configuration

    val FaviconKey = "securesocial.faviconPath"
    val JQueryKey = "securesocial.jqueryPath"
    val CustomCssKey = "securesocial.customCssPath"
    val DefaultFaviconPath = "images/favicon.png"
    val DefaultJqueryPath = "javascripts/jquery-1.7.1.min.js"

    protected def absoluteUrl(call: Call)(implicit req: RequestHeader): String = {
      call.absoluteURL(IdentityProvider.sslEnabled)
    }

    override def loginPageUrl(implicit req: RequestHeader): String = {
      absoluteUrl(securesocial.controllers.routes.LoginPage.login())
    }

    override def startSignUpUrl(implicit req: RequestHeader): String = {
      absoluteUrl(securesocial.controllers.routes.Registration.startSignUp())
    }

    override def handleStartSignUpUrl(implicit req: RequestHeader): String = {
      absoluteUrl(securesocial.controllers.routes.Registration.handleStartSignUp())
    }

    override def signUpUrl(mailToken: String)(implicit req: RequestHeader): String = {
      absoluteUrl(securesocial.controllers.routes.Registration.signUp(mailToken))
    }

    override def handleSignUpUrl(mailToken: String)(implicit req: RequestHeader): String = {
      absoluteUrl(securesocial.controllers.routes.Registration.handleSignUp(mailToken))
    }

    override def startResetPasswordUrl(implicit request: RequestHeader): String = {
      absoluteUrl(securesocial.controllers.routes.PasswordReset.startResetPassword())
    }

    override def handleStartResetPasswordUrl(implicit req: RequestHeader): String = {
      absoluteUrl(securesocial.controllers.routes.PasswordReset.handleStartResetPassword())
    }

    override def resetPasswordUrl(mailToken: String)(implicit req: RequestHeader): String = {
      absoluteUrl(securesocial.controllers.routes.PasswordReset.resetPassword(mailToken))
    }

    override def handleResetPasswordUrl(mailToken: String)(implicit req: RequestHeader): String = {
      absoluteUrl(securesocial.controllers.routes.PasswordReset.handleResetPassword(mailToken))
    }

    override def passwordChangeUrl(implicit req: RequestHeader): String = {
      absoluteUrl(securesocial.controllers.routes.PasswordChange.page())
    }

    override def handlePasswordChangeUrl(implicit req: RequestHeader): String = {
      absoluteUrl(securesocial.controllers.routes.PasswordChange.handlePasswordChange)
    }

    override def authenticationUrl(provider: String, redirectTo: Option[String] = None)(implicit req: RequestHeader): String = {
      absoluteUrl(securesocial.controllers.routes.ProviderController.authenticate(provider, redirectTo))
    }

    protected def valueFor(key: String, default: String) = {
      val value = conf.getString(key).getOrElse(default)
      logger.debug(s"[securesocial] $key = $value")
      securesocial.controllers.routes.Assets.at(value)
    }

    /**
     * Loads the Favicon to use from configuration, using a default one if not provided
     * @return the path to Favicon file to use
     */
    override val faviconPath = valueFor(FaviconKey, DefaultFaviconPath)

    /**
     * Loads the Jquery file to use from configuration, using a default one if not provided
     * @return the path to Jquery file to use
     */
    override val jqueryPath = valueFor(JQueryKey, DefaultJqueryPath)

    /**
     * Loads the Custom Css file to use from configuration. If there is none define, none will be used
     * @return Option containing a custom css file or None
     */
    override val customCssPath: Option[Call] = {
      val path = conf.getString(CustomCssKey).map(securesocial.controllers.routes.Assets.at)
      logger.debug("[securesocial] custom css path = %s".format(path))
      path
    }
  }
}
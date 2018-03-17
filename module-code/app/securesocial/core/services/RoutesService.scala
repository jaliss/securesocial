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
import play.api.{ Configuration, Environment }
import securesocial.core.SslEnabled

/**
 * A RoutesService that resolves the routes for some of the pages
 */
trait RoutesService {
  /**
   * The login page url
   */
  def loginPageUrl(implicit req: RequestHeader): String

  /**
   * The page where users get redirected when they deny access to their accounts using
   * oauth logins
   */
  def accessDeniedUrl(implicit req: RequestHeader): String

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
  def bootstrapCssPath: Call
  def customCssPath: Option[Call]
}

object RoutesService {
  /**
   * The default RoutesService implementation.  It points to the routes
   * defined by the built in controllers.
   */
  class Default(environment: Environment, configuration: Configuration) extends RoutesService {
    private val logger = play.api.Logger("securesocial.core.DefaultRoutesService")

    val sslEnabled = SslEnabled(environment, configuration)

    val FaviconKey = "securesocial.faviconPath"
    val JQueryKey = "securesocial.jqueryPath"
    val BootstrapCssKey = "securesocial.bootstrapCssPath"
    val CustomCssKey = "securesocial.customCssPath"
    val ApplicationHostKey = "securesocial.applicationHost"
    val ApplicationPortKey = "securesocial.applicationPort"

    private lazy val applicationHost = configuration.get[Option[String]](ApplicationHostKey).getOrElse {
      throw new RuntimeException(s"Missing property: $ApplicationHostKey")
    }
    private lazy val applicationPort =
      configuration.get[Option[Int]](ApplicationPortKey).map(port => s":$port").getOrElse("")
    private lazy val hostAndPort = s"$applicationHost$applicationPort"

    protected def absoluteUrl(call: Call)(implicit req: RequestHeader): String = {
      call.absoluteURL(sslEnabled.value, hostAndPort)
    }

    override def loginPageUrl(implicit req: RequestHeader): String = {
      absoluteUrl(securesocial.controllers.routes.LoginPage.login())
    }

    override def accessDeniedUrl(implicit req: RequestHeader): String = {
      loginPageUrl
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

    protected def valueFor(key: String) = {
      val value = configuration.get[String](key)
      logger.debug(s"[securesocial] $key = $value")
      securesocial.controllers.routes.Assets.at(value)
    }

    protected def valueFor(key: String, default: String) = {
      val value = configuration.get[Option[String]](key).getOrElse(default)
      logger.debug(s"[securesocial] $key = $value")
      securesocial.controllers.routes.Assets.at(value)
    }

    /**
     * Loads the Favicon to use from configuration, using a default one if not provided
     * @return the path to Favicon file to use
     */
    override val faviconPath = valueFor(FaviconKey)

    /**
     * Loads the Jquery file to use from configuration, using a default one if not provided
     * @return the path to Jquery file to use
     */
    override val jqueryPath = valueFor(JQueryKey)

    /**
     * Loads the Bootstrap CSS file to use from configuration, using a default one if not provided
     * @return the path to Bootstrap CSS file to use
     */
    override val bootstrapCssPath = valueFor(BootstrapCssKey)
    /**
     * Loads the Custom Css file to use from configuration. If there is none define, none will be used
     * @return Option containing a custom css file or None
     */
    override val customCssPath: Option[Call] = {
      val path = configuration.get[Option[String]](CustomCssKey).map(securesocial.controllers.routes.Assets.at)
      logger.debug("[securesocial] custom css path = %s".format(path))
      path
    }
  }
}
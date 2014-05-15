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

import play.api.mvc.{Call, RequestHeader}
import securesocial.core.IdentityProvider

/**
 * A RoutesService that resolves the routes for some of the pages
 */
trait RoutesService {
  def loginPageUrl(implicit req: RequestHeader): String
  def signUpUrl(implicit req: RequestHeader): String
  def resetPasswordUrl(implicit req: RequestHeader): String
  def authenticationUrl(provider:String, redirectTo: Option[String] = None)(implicit req: RequestHeader): String
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

    def signUpUrl(implicit req: RequestHeader): String = {
      absoluteUrl(securesocial.controllers.routes.Registration.startSignUp())
    }

    override def resetPasswordUrl(implicit request: RequestHeader): String = {
      absoluteUrl(securesocial.controllers.routes.PasswordReset.startResetPassword())
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
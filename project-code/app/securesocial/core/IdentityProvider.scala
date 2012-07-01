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
package securesocial.core

import securesocial.controllers.routes
import play.api.mvc.{Request, Result}
import play.api.{Application, Logger, Plugin}

/**
 * Base class for all Identity Providers.  All providers are plugins and are loaded
 * automatically at application start time.
 *
 *
 */
abstract class IdentityProvider(application: Application) extends Plugin {
  val SecureSocial = "securesocial."
  val Dot = "."


  /**
   * Registers the provider in the Provider Registry
   */
  override def onStart() {
    ProviderRegistry.register(this)
  }

  /**
   * Subclasses need to implement this to specify the provider type
   * @return
   */
  def providerId: String

  /**
   * Subclasses need to implement this to specify the authentication method
   * @return
   */
  def authMethod: AuthenticationMethod

  /**
   * Returns the provider name
   *
   * @return
   */
  override def toString = providerId

  /**
   * Authenticates the user and fills the profile information. Returns either a User if all went
   * ok or a Result that the controller sents to the browser (eg: in the case of OAuth for example
   * where the user needs to be redirected to the service provider)
   *
   * @param request
   * @tparam A
   * @return
   */
  def authenticate[A]()(implicit request: Request[A]):Either[Result, SocialUser] = {
    doAuth().fold(
      result => Left(result),
      u =>
      {
        val user = fillProfile(u)
        UserService.save(user)
        Right(user)
      }
    )
  }

  /**
   * The url for this provider. This is used in the login page template to point each icon
   * to the provider url.
   * @return
   */
  def authenticationUrl:String = routes.LoginPage.authenticate(providerId).url

  /**
   * The property key used for all the provider properties.
   *
   * @return
   */
  def propertyKey = SecureSocial + providerId + Dot

  /**
   * Reads a property from the application.conf
   * @param property
   * @return
   */
  def loadProperty(property: String): Option[String] = {
    val result = application.configuration.getString(propertyKey + property)
    if ( !result.isDefined ) {
      Logger.error("[securesocial] Missing property " + property + " for provider " + providerId)
    }
    result
  }


  /**
   * Subclasses need to implement the authentication logic. This method needs to return
   * a User object that then gets passed to the fillProfile method
   *
   * @param request
   * @tparam A
   * @return Either a Result or a User
   */
  def doAuth[A]()(implicit request: Request[A]):Either[Result, SocialUser]

  /**
   * Subclasses need to implement this method to populate the User object with profile
   * information from the service provider.
   *
   * @param user The user object to be populated
   * @return A copy of the user object with the new values set
   */
  def fillProfile(user: SocialUser):SocialUser
}

object IdentityProvider {
  val SessionId = "securesocial.id"
}

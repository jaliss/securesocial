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

import play.api.mvc.{Session, DiscardingCookie, Action, Controller}
import securesocial.core._
import play.api.Play
import Play.current
import providers.UsernamePasswordProvider
import providers.utils.RoutesHelper
import play.Logger


/**
 * The Login page controller
 */
object LoginPage extends Controller
{
  /**
   * The property that specifies the page the user is redirected to after logging out.
   */
  val onLogoutGoTo = "securesocial.onLogoutGoTo"

  /**
   * Renders the login page
   * @return
   */
  def login = Action { implicit request =>
    val to = ProviderController.landingUrl
    if ( SecureSocial.currentUser.isDefined ) {
      // if the user is already logged in just redirect to the app
      if ( Logger.isDebugEnabled() ) {
        Logger.debug("User already logged in, skipping login page. Redirecting to %s".format(to))
      }
      Redirect( to )
    } else {
      import com.typesafe.plugin._
      import Play.current
      Ok(use[TemplatesPlugin].getLoginPage(request, UsernamePasswordProvider.loginForm))
    }
  }

  /**
   * Logs out the user by clearing the credentials from the session.
   * The browser is redirected either to the login page or to the page specified in the onLogoutGoTo property.
   *
   * @return
   */
  def logout = Action { implicit request =>
    val to = Play.configuration.getString(onLogoutGoTo).getOrElse(RoutesHelper.login().absoluteURL(IdentityProvider.sslEnabled))
    val user = for (
      authenticator <- SecureSocial.authenticatorFromRequest ;
      user <- UserService.find(authenticator.userId)
    ) yield {
      Authenticator.delete(authenticator.id)
      user
    }
    val result = Redirect(to).discardingCookies(Authenticator.discardingCookie)
    user match {
      case Some(u) => result.withSession( Events.fire(new LogoutEvent(u)).getOrElse(session) )
      case None => result
    }
  }
}

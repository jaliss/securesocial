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

import play.api.mvc.{RequestHeader, Action, Controller}
import play.api.i18n.Messages
import securesocial.core._
import play.api.{Play, Logger}
import Play.current
import providers.utils.RoutesHelper


/**
 * A controller to provide the authentication entry point
 */
object ProviderController extends Controller
{
  /**
   * The property that specifies the page the user is redirected to if there is no original URL saved in
   * the session.
   */
  val onLoginGoTo = "securesocial.onLoginGoTo"

  /**
   * The root path
   */
  val Root = "/"

  /**
   * The application context
   */
  val ApplicationContext = "application.context"

  /**
   * Returns the url that the user should be redirected to after login
   *
   * @param request
   * @return
   */
  def toUrl(implicit request: RequestHeader) = session.get(SecureSocial.OriginalUrlKey).getOrElse(
    Play.configuration.getString(onLoginGoTo).getOrElse(
      Play.configuration.getString(ApplicationContext).getOrElse(Root)
    )
  )


  /**
   * Renders a not authorized page if the Authorization object passed to the action does not allow
   * execution.
   *
   * @see Authorization
   */
  def notAuthorized() = Action { implicit request =>
    import com.typesafe.plugin._
    Ok(use[TemplatesPlugin].getNotAuthorizedPage)
  }

  /**
   * The authentication flow for all providers starts here.
   *
   * @param provider The id of the provider that needs to handle the call
   * @return
   */
  def authenticate(provider: String) = handleAuth(provider)
  def authenticateByPost(provider: String) = handleAuth(provider)

  private def handleAuth(provider: String) = Action { implicit request =>
    ProviderRegistry.get(provider) match {
      case Some(p) => {
        try {
          p.authenticate().fold( result => result , {
            user =>
              if ( Logger.isDebugEnabled ) {
                Logger.debug("User logged in : [" + user + "]")
              }
              Redirect(toUrl).withSession { session +
                (SecureSocial.UserKey -> user.id.id) +
                (SecureSocial.ProviderKey -> user.id.providerId) -
                SecureSocial.OriginalUrlKey
              }
          })
        } catch {
          case ex: AccessDeniedException => {
            Redirect(RoutesHelper.login()).flashing("error" -> Messages("securesocial.login.accessDenied"))
          }
        }
      }
      case _ => NotFound
    }
  }
}

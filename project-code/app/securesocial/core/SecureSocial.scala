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

import play.api.mvc._
import securesocial.controllers.routes
import play.api.i18n.Messages
import play.api.Logger
import play.api.libs.json.Json


/**
 * Provides the actions that can be used to protect controllers and retrieve the current user
 * if available.
 *
 * object MyController extends SecureSocial {
 *    def protectedAction = SecuredAction() { implicit request =>
 *      Ok("Hello %s".format(request.user.displayName))
 *    }
 */
trait SecureSocial extends Controller {

  /**
   * A request that adds the User for the current call
   */
  case class SecuredRequest[A](user: SocialUser, request: Request[A]) extends WrappedRequest(request)

  /**
   * A Forbidden response for API clients
   * @param request
   * @tparam A
   * @return
   */
  private def apiClientForbidden[A](implicit request: Request[A]): Result = {
    Forbidden(Json.toJson(Map("error"->"Credentials required"))).withSession {
      session - SecureSocial.UserKey - SecureSocial.ProviderKey
    }.as(JSON)
  }

  /**
   * A secured action.  If there is no user in the session the request is redirected
   * to the login page
   *
   * @param apiClient A boolean specifying if this is request is for an API or not
   * @param p
   * @param f
   * @tparam A
   * @return
   */
  def SecuredAction[A](apiClient: Boolean, p: BodyParser[A])(f: SecuredRequest[A] => Result) = Action(p) {
    implicit request => {
      SecureSocial.userFromSession(request).map { userId =>
        UserService.find(userId).map { user =>
          f(SecuredRequest(user, request))
        }.getOrElse {
          // there is no user in the backing store matching the credentials sent by the client.
          // we need to remove the credentials from the session
          if ( apiClient ) {
            apiClientForbidden(request)
          } else {
            Redirect(routes.LoginPage.logout())
          }
        }
      }.getOrElse {
        if ( Logger.isDebugEnabled ) {
          Logger.debug("Anonymous user trying to access : '%s'".format(request.uri))
        }
        if ( apiClient ) {
          apiClientForbidden(request)
        } else {
          Redirect(routes.LoginPage.login()).flashing("error" -> Messages("securesocial.loginRequired")).withSession(
            session + (SecureSocial.OriginalUrlKey -> request.uri)
          )
        }
      }
    }
  }

  /**
   * A secured action.  If there is no user in the session the request is redirected
   * to the login page.
   * @param f
   * @return
   */
  def SecuredAction(apiClient: Boolean = false)(f: SecuredRequest[AnyContent] => Result): Action[AnyContent] = {
    SecuredAction(apiClient, parse.anyContent)(f)
  }

  /**
   * A request that adds the User for the current call
   */
  case class RequestWithUser[A](user: Option[SocialUser], request: Request[A]) extends WrappedRequest(request)

  /**
   * An action that adds the current user in the request if it's available
   *
   * @param p
   * @param f
   * @tparam A
   * @return
   */
  def UserAwareAction[A](p: BodyParser[A])(f: RequestWithUser[A] => Result) = Action(p) {
    implicit request =>
      f(RequestWithUser(SecureSocial.currentUser, request))
  }

  /**
   * An action that adds the current user in the request if it's available
   * @param f
   * @return
   */
  def UserAwareAction(f: RequestWithUser[AnyContent] => Result): Action[AnyContent] = {
    UserAwareAction(parse.anyContent)(f)
  }
}

object SecureSocial {
  val UserKey = "securesocial.user"
  val ProviderKey = "securesocial.provider"
  val OriginalUrlKey = "securesocial.originalUrl"

  /**
   * Build a UserId object from the session data
   *
   * @param request
   * @tparam A
   * @return
   */
  def userFromSession[A](implicit request: Request[A]):Option[UserId] = {
    for (
      userId <- request.session.get(SecureSocial.UserKey);
      providerId <- request.session.get(SecureSocial.ProviderKey)
    ) yield {
      UserId(userId, providerId)
    }
  }

  /**
   * Get the current logged in user.  This method can be used from public actions that need to
   * access the current user if there's any
   *
   * @param request
   * @tparam A
   * @return
   */
  def currentUser[A](implicit request: Request[A]):Option[SocialUser] = {
    for (
      userId <- userFromSession ;
      user <- UserService.find(userId)
    ) yield {
      fillServiceInfo(user)
    }
  }

  def fillServiceInfo(user: SocialUser): SocialUser = {
    if ( user.authMethod == AuthenticationMethod.OAuth1 ) {
      // if the user is using OAuth1 make sure we're also returning
      // the right service info
      ProviderRegistry.get(user.id.providerId).map { p =>
        val si = p.asInstanceOf[OAuth1Provider].serviceInfo
        val oauthInfo = user.oAuth1Info.get.copy(serviceInfo = si)
        user.copy( oAuth1Info = Some(oauthInfo))
      }.get
    } else {
      user
    }
  }
}

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
import providers.utils.RoutesHelper
import play.api.i18n.Messages
import play.api.Logger
import play.api.libs.json.Json
import scala.Some
import play.api.libs.oauth.ServiceInfo


/**
 * A request that adds the User for the current call
 */
case class SecuredRequest[A](user: Identity, request: Request[A]) extends WrappedRequest(request)

/**
 * Provides the actions that can be used to protect controllers and retrieve the current user
 * if available.
 *
 * object MyController extends SecureSocial {
 *    def protectedAction = SecuredAction { implicit request =>
 *      Ok("Hello %s".format(request.user.displayName))
 *    }
 */
trait SecureSocial extends Controller {
  /**
   * A Forbidden response for ajax clients
   * @param request
   * @tparam A
   * @return
   */
  private def ajaxCallNotAuthenticated[A](implicit request: Request[A]): PlainResult = {
    Unauthorized(Json.toJson(Map("error"->"Credentials required"))).as(JSON)
  }

  private def ajaxCallNotAuthorized[A](implicit request: Request[A]): PlainResult = {
    Forbidden( Json.toJson(Map("error" -> "Not authorized"))).as(JSON)
  }

  /**
   * A secured action.  If there is no user in the session the request is redirected
   * to the login page
   *
   * @param ajaxCall a boolean indicating whether this is an ajax call or not
   * @param authorize an Authorize object that checks if the user is authorized to invoke the action
   * @param p the body parser to use
   * @param f the wrapped action to invoke
   * @tparam A
   * @return
   */
  def SecuredAction[A](ajaxCall: Boolean, authorize: Option[Authorization], p: BodyParser[A])
                      (f: SecuredRequest[A] => Result)
                       = Action(p) {
    implicit request => {

      val result = for (
        authenticator <- SecureSocial.authenticatorFromRequest ;
        user <- UserService.find(authenticator.userId)
      ) yield {
        touch(authenticator)
        if ( authorize.isEmpty || authorize.get.isAuthorized(user)) {
          f(SecuredRequest(user, request))
        } else {
          if ( ajaxCall ) {
            ajaxCallNotAuthorized(request)
          } else {
            Redirect(RoutesHelper.notAuthorized.absoluteURL(IdentityProvider.sslEnabled))
          }
        }
      }

      result.getOrElse({
        if ( Logger.isDebugEnabled ) {
          Logger.debug("[securesocial] anonymous user trying to access : '%s'".format(request.uri))
        }
        val response = if ( ajaxCall ) {
          ajaxCallNotAuthenticated(request)
        } else {
          Redirect(RoutesHelper.login()).flashing("error" -> Messages("securesocial.loginRequired")).withSession(
            session + (SecureSocial.OriginalUrlKey -> request.uri)
          )
        }
        response.discardingCookies(Authenticator.discardingCookie)
      })
    }
  }

  /**
   * A secured action.  If there is no user in the session the request is redirected
   * to the login page.
   *
   * @param ajaxCall a boolean indicating whether this is an ajax call or not
   * @param authorize an Authorize object that checks if the user is authorized to invoke the action
   * @param f the wrapped action to invoke
   * @return
   */
  def SecuredAction(ajaxCall: Boolean, authorize: Authorization)
                   (f: SecuredRequest[AnyContent] => Result): Action[AnyContent] =
    SecuredAction(ajaxCall, Some(authorize), p = parse.anyContent)(f)

  /**
   * A secured action.  If there is no user in the session the request is redirected
   * to the login page.
   *
   * @param authorize an Authorize object that checks if the user is authorized to invoke the action
   * @param f the wrapped action to invoke
   * @return
   */
  def SecuredAction(authorize: Authorization)
                   (f: SecuredRequest[AnyContent] => Result): Action[AnyContent] =
    SecuredAction(false,authorize)(f)

  /**
   * A secured action.  If there is no user in the session the request is redirected
   * to the login page.
   *
   * @param ajaxCall a boolean indicating whether this is an ajax call or not
   * @param f the wrapped action to invoke
   * @return
   */
  def SecuredAction(ajaxCall: Boolean)
                   (f: SecuredRequest[AnyContent] => Result): Action[AnyContent] =
    SecuredAction(ajaxCall, None, parse.anyContent)(f)

  /**
   * A secured action.  If there is no user in the session the request is redirected
   * to the login page.
   *
   * @param f the wrapped action to invoke
   * @return
   */
  def SecuredAction(f: SecuredRequest[AnyContent] => Result): Action[AnyContent] =
    SecuredAction(false)(f)

  /**
   * A request that adds the User for the current call
   */
  case class RequestWithUser[A](user: Option[Identity], request: Request[A]) extends WrappedRequest(request)

  /**
   * An action that adds the current user in the request if it's available
   *
   * @param p
   * @param f
   * @tparam A
   * @return
   */
  def UserAwareAction[A](p: BodyParser[A])(f: RequestWithUser[A] => Result) = Action(p) {
    implicit request => {
      val user = for (
        authenticator <- SecureSocial.authenticatorFromRequest ;
        user <- UserService.find(authenticator.userId)
      ) yield {
        touch(authenticator)
        user
      }
      f(RequestWithUser(user, request))
    }
  }

  /**
   * An action that adds the current user in the request if it's available
   * @param f
   * @return
   */
  def UserAwareAction(f: RequestWithUser[AnyContent] => Result): Action[AnyContent] = {
    UserAwareAction(parse.anyContent)(f)
  }

  def touch(authenticator: Authenticator) {
    Authenticator.save(authenticator.touch)
  }
}

object SecureSocial {
  val OriginalUrlKey = "securesocial.originalUrl"

  def authenticatorFromRequest(implicit request: RequestHeader): Option[Authenticator] = {
    val result = for {
      cookie <- request.cookies.get(Authenticator.cookieName) ;
      maybeAuthenticator <- Authenticator.find(cookie.value).fold(e => None, Some(_)) ;
      authenticator <- maybeAuthenticator
    } yield {
      authenticator
    }

    result match {
      case Some(a) => {
        if ( !a.isValid ) {
          Authenticator.delete(a.id)
          None
        } else {
          Some(a)
        }
      }
      case None => None
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
  def currentUser[A](implicit request: RequestHeader):Option[Identity] = {
    for (
      authenticator <- authenticatorFromRequest ;
      user <- UserService.find(authenticator.userId)
    ) yield {
      user
    }
  }

  /**
   * Returns the ServiceInfo needed to sign OAuth1 requests.
   *
   * @param user the user for which the serviceInfo is needed
   * @return an optional service info
   */
  def serviceInfoFor(user: Identity): Option[ServiceInfo] = {
    Registry.providers.get(user.id.providerId) match {
      case Some(p: OAuth1Provider) if p.authMethod == AuthenticationMethod.OAuth1 => Some(p.serviceInfo)
      case _ => None
    }
  }
}

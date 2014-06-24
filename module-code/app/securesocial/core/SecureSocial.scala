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
package securesocial.core

import play.api.mvc._
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.http.HeaderNames
import scala.concurrent.{ExecutionContext, Future}
import play.api.templates.Html

import securesocial.core.utils._
import securesocial.core.authenticator._
import scala.Some
import play.api.mvc.SimpleResult


/**
 * Provides the actions that can be used to protect controllers and retrieve the current user
 * if available.
 *
 */
trait SecureSocial[U] extends Controller {
  implicit val env: RuntimeEnvironment[U]

  /**
   * A Forbidden response for ajax clients
   * @param request the current request
   * @tparam A
   * @return
   */
  protected val notAuthenticatedJson =  Unauthorized(Json.toJson(Map("error"->"Credentials required"))).as(JSON)
  protected val notAuthorizedJson = Forbidden(Json.toJson(Map("error" -> "Not authorized"))).as(JSON)
  protected def notAuthorizedPage()(implicit request: RequestHeader): Html = securesocial.views.html.notAuthorized()

  protected def notAuthenticatedResult[A](implicit request: Request[A]): Future[SimpleResult] = {
    Future.successful {
      render  {
        case Accepts.Json() => notAuthenticatedJson
        case Accepts.Html() => Redirect(env.routes.loginPageUrl).
          flashing("error" -> Messages("securesocial.loginRequired"))
          .withSession(request.session + (SecureSocial.OriginalUrlKey -> request.uri))
        case _ => Unauthorized("Credentials required")
      }
    }
  }

  protected def notAuthorizedResult[A](implicit request: Request[A]): Future[SimpleResult] = {
    Future.successful {
      render {
        case Accepts.Json() => notAuthorizedJson
        case Accepts.Html() => Forbidden(notAuthorizedPage())
        case _ => Forbidden("Not authorized")
      }
    }
  }

  /**
   * A request that adds the User for the current call
   */
  case class SecuredRequest[A](user: U, authenticator: Authenticator[U], request: Request[A]) extends WrappedRequest(request)

  /**
   * A request that adds the User for the current call
   */
  case class RequestWithUser[A](user: Option[U], authenticator: Option[Authenticator[U]], request: Request[A]) extends WrappedRequest(request)


  /**
   * A secured action.  If there is no user in the session the request is redirected
   * to the login page
   */
  object SecuredAction extends SecuredActionBuilder {
    /**
     * Creates a secured action
     */
    def apply[A]() = new SecuredActionBuilder(None)

    /**
     * Creates a secured action
     * @param authorize an Authorize object that checks if the user is authorized to invoke the action
     */
    def apply[A](authorize: Authorization[U]) = new SecuredActionBuilder(Some(authorize))
  }

/**
   * A builder for secured actions
   *
   * @param authorize an Authorize object that checks if the user is authorized to invoke the action
   */
  class SecuredActionBuilder(authorize: Option[Authorization[U]] = None)
    extends ActionBuilder[({type R[A] = SecuredRequest[A]})#R] {
    private val logger = play.api.Logger("securesocial.core.SecuredActionBuilder")

    def invokeSecuredBlock[A](authorize: Option[Authorization[U]], request: Request[A],
                              block: SecuredRequest[A] => Future[SimpleResult]): Future[SimpleResult] =
    {
      import ExecutionContext.Implicits.global
      env.authenticatorService.fromRequest(request).flatMap {
        case Some(authenticator) if authenticator.isValid =>
          authenticator.touch.flatMap { updatedAuthenticator =>
            val user = updatedAuthenticator.user
            if (authorize.isEmpty || authorize.get.isAuthorized(user, request)) {
              block(SecuredRequest(user, updatedAuthenticator, request)).flatMap {
                _.touchingAuthenticator(updatedAuthenticator)
              }
            } else {
              notAuthorizedResult(request)
            }
          }
        case Some(authenticator) if !authenticator.isValid =>
          logger.debug("[securesocial] user tried to access with invalid authenticator : '%s'".format(request.uri))
          import ExecutionContext.Implicits.global
          notAuthenticatedResult(request).flatMap { _.discardingAuthenticator(authenticator) }
        case None =>
          logger.debug("[securesocial] anonymous user trying to access : '%s'".format(request.uri))
          notAuthenticatedResult(request)
      }
    }

    override def invokeBlock[A](request: Request[A],
                                          block: (SecuredRequest[A]) => Future[SimpleResult]): Future[SimpleResult] =
    {
      invokeSecuredBlock(authorize, request, block)
    }
}


  /**
   * An action that adds the current user in the request if it's available.
   */
  object UserAwareAction extends UserAwareActionBuilder {
    def apply[A]() = new UserAwareActionBuilder()
  }


  /**
   * The UserAwareAction builder
   */
  class UserAwareActionBuilder extends ActionBuilder[({ type R[A] = RequestWithUser[A] })#R] {
   override def invokeBlock[A](request: Request[A],
                                 block: (RequestWithUser[A]) => Future[SimpleResult]): Future[SimpleResult] =
    {
      import ExecutionContext.Implicits.global
      env.authenticatorService.fromRequest(request).flatMap {
        case Some(authenticator) if authenticator.isValid =>
          authenticator.touch.flatMap {
            a => block(RequestWithUser(Some(a.user), Some(a), request))
          }
        case Some(authenticator) if !authenticator.isValid =>
           block(RequestWithUser(None, None, request)).flatMap(_.discardingAuthenticator(authenticator))
        case None =>
          block(RequestWithUser(None, None, request))
      }
    }
  }
}

object SecureSocial {
  val OriginalUrlKey = "original-url"

  /**
   * Returns the ServiceInfo needed to sign OAuth1 requests.
   *
   * @param user the user for which the serviceInfo is needed
   * @return an optional service info
   */
//  def serviceInfoFor(user: Identity)(implicit env: RuntimeEnvironment): Option[ServiceInfo] = {
//    env.providers.get(user.identityId.providerId) match {
//      case Some(p: OAuth1Provider) if p.authMethod == AuthenticationMethod.OAuth1 => Some(p.client.serviceInfo)
//      case _ => None
//    }
//  }

  /**
   * Saves the referer as original url in the session if it's not yet set.
   * @param result the result that maybe enhanced with an updated session
   * @return the result that's returned to the client
   */
  def withRefererAsOriginalUrl[A](result: Result)(implicit request: Request[A]): Result = {
    request.session.get(OriginalUrlKey) match {
      // If there's already an original url recorded we keep it: e.g. if s.o. goes to
      // login, switches to signup and goes back to login we want to keep the first referer
      case Some(_) => result
      case None => {
        request.headers.get(HeaderNames.REFERER).map { referer =>
          // we don't want to use the ful referer, as then we might redirect from https
          // back to http and loose our session. So let's get the path and query string only
          val idxFirstSlash = referer.indexOf("/", "https://".length())
          val refererUri = if (idxFirstSlash < 0) "/" else referer.substring(idxFirstSlash)
          result.withSession(
            request.session + (OriginalUrlKey -> refererUri))
        }.getOrElse(result)
      }
    }
  }

  val enableRefererAsOriginalUrl = {
    import play.api.Play
    Play.current.configuration.getBoolean("securesocial.enableRefererAsOriginalUrl").getOrElse(false)
  }

  /**
   * Returns the current user. Invoke this only if you are executing code
   * without a SecuredRequest or UserAwareRequest available. For most cases what SecuredAction or UserAwareAction
   * gives you will be enough.
   *
   * @param request the current request
   * @param env the current environment
   * @tparam U the user type
   * @return a future with an option user
   */
  def currentUser[U](implicit request: RequestHeader, env: RuntimeEnvironment[U], ec: ExecutionContext): Future[Option[U]] = {
    env.authenticatorService.fromRequest.map {
      case Some(authenticator) if authenticator.isValid => Some(authenticator.user)
      case _ => None
    }
  }
}

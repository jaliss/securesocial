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
import securesocial.core.SecureSocial.{ RequestWithUser, SecuredRequest }
import scala.concurrent.{ ExecutionContext, Future }
import play.twirl.api.Html

import securesocial.core.utils._
import securesocial.core.authenticator._
import play.api.mvc.Result
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

/**
 * Provides the actions that can be used to protect controllers and retrieve the current user
 * if available.
 *
 */
trait SecureSocial extends Controller {
  implicit val env: RuntimeEnvironment
  implicit def executionContext: ExecutionContext = env.executionContext

  protected val notAuthenticatedJson = Unauthorized(Json.toJson(Map("error" -> "Credentials required"))).as(JSON)
  protected def notAuthenticatedResult[A](implicit request: Request[A]): Future[Result] = {
    Future.successful {
      render {
        case Accepts.Json() => notAuthenticatedJson
        case Accepts.Html() => Redirect(env.routes.loginPageUrl).
          flashing("error" -> Messages("securesocial.loginRequired"))
          .withSession(request.session + (SecureSocial.OriginalUrlKey -> request.uri))
        case _ => Unauthorized("Credentials required")
      }
    }
  }

  protected val notAuthorizedJson = Forbidden(Json.toJson(Map("error" -> "Not authorized"))).as(JSON)
  protected def notAuthorizedPage()(implicit request: RequestHeader): Html = env.viewTemplates.getNotAuthorizedPage
  protected def notAuthorizedResult[A](implicit request: Request[A]): Future[Result] = {
    Future.successful {
      render {
        case Accepts.Json() => notAuthorizedJson
        case Accepts.Html() => Forbidden(notAuthorizedPage())
        case _ => Forbidden("Not authorized")
      }
    }
  }

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
    def apply[A](authorize: Authorization[env.U]) = new SecuredActionBuilder(Some(authorize))
  }

  /**
   * A builder for secured actions
   *
   * @param authorize an Authorize object that checks if the user is authorized to invoke the action
   */
  class SecuredActionBuilder(authorize: Option[Authorization[env.U]] = None)
      extends ActionBuilder[({ type R[A] = SecuredRequest[A, env.U] })#R] {
    override protected implicit def executionContext: ExecutionContext = env.executionContext

    private val logger = play.api.Logger("securesocial.core.SecuredActionBuilder")

    def invokeSecuredBlock[A](authorize: Option[Authorization[env.U]], request: Request[A],
      block: SecuredRequest[A, env.U] => Future[Result]): Future[Result] =
      {
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
            notAuthenticatedResult(request).flatMap { _.discardingAuthenticator(authenticator) }
          case None =>
            logger.debug("[securesocial] anonymous user trying to access : '%s'".format(request.uri))
            notAuthenticatedResult(request)
        }
      }

    override def invokeBlock[A](request: Request[A],
      block: (SecuredRequest[A, env.U]) => Future[Result]): Future[Result] =
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
  class UserAwareActionBuilder extends ActionBuilder[({ type R[A] = RequestWithUser[A, env.U] })#R] {
    override protected implicit def executionContext: ExecutionContext = env.executionContext

    override def invokeBlock[A](request: Request[A],
      block: (RequestWithUser[A, env.U]) => Future[Result]): Future[Result] =
      {
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
   * A request that adds the User for the current call
   */
  case class SecuredRequest[A, U](user: U, authenticator: Authenticator[U], request: Request[A]) extends WrappedRequest(request)

  /**
   * A request that adds the User for the current call
   */
  case class RequestWithUser[A, U](user: Option[U], authenticator: Option[Authenticator[U]], request: Request[A]) extends WrappedRequest(request)

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
        refererPathAndQuery.map { referer =>
          result.withSession(
            request.session + (OriginalUrlKey -> referer))
        }.getOrElse(result)
      }
    }
  }

  /**
   * Gets the referer URI from the implicit request
   * @return the path and query string of the referer path and query
   */
  def refererPathAndQuery[A](implicit request: Request[A]): Option[String] = {
    request.headers.get(HeaderNames.REFERER).map { referer =>
      // we don't want to use the full referer, as then we might redirect from https
      // back to http and loose our session. So let's get the path and query string only
      val idxFirstSlash = referer.indexOf("/", "https://".length())
      val refererUri = if (idxFirstSlash < 0) "/" else referer.substring(idxFirstSlash)
      refererUri
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
   * @return a future with an option user
   */
  def currentUser(implicit request: RequestHeader, env: RuntimeEnvironment, executionContext: ExecutionContext): Future[Option[env.U]] = {
    env.authenticatorService.fromRequest.map {
      case Some(authenticator) if authenticator.isValid => Some(authenticator.user)
      case _ => None
    }
  }
}

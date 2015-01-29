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
package securesocial.controllers

import play.api.Play
import play.api.Play.current
import play.api.i18n.Messages
import play.api.mvc._
import securesocial.core._
import securesocial.core.authenticator.CookieAuthenticator
import securesocial.core.services.SaveMode
import securesocial.core.utils._

import scala.concurrent.Future

/**
 * A default controller that uses the BasicProfile as the user type
 */
class ProviderController(override implicit val env: RuntimeEnvironment[BasicProfile])
  extends BaseProviderController[BasicProfile]

/**
 * A trait that provides the means to authenticate users for web applications
 *
 * @tparam U the user type
 */
trait BaseProviderController[U <: GenericProfile] extends SecureSocial[U] {
  import securesocial.controllers.ProviderControllerHelper.{ logger, toUrl }

  /**
   * The authentication entry point for GET requests
   *
   * @param provider The id of the provider that needs to handle the call
   */
  def authenticate(provider: String, redirectTo: Option[String] = None) = handleAuth(provider, redirectTo)

  /**
   * The authentication entry point for POST requests
   *
   * @param provider The id of the provider that needs to handle the call
   */
  def authenticateByPost(provider: String, redirectTo: Option[String] = None) = handleAuth(provider, redirectTo)

  /**
   * Overrides the original url if neded
   *
   * @param session the current session
   * @param redirectTo the url that overrides the originalUrl
   * @return a session updated with the url
   */
  private def overrideOriginalUrl(session: Session, redirectTo: Option[String]) = redirectTo match {
    case Some(url) =>
      session + (SecureSocial.OriginalUrlKey -> url)
    case _ =>
      session
  }

  /**
   * Find the AuthenticatorBuilder needed to start the authenticated session
   */
  private def builder() = {
    //todo: this should be configurable maybe
    env.authenticatorService.find(CookieAuthenticator.Id).getOrElse {
      val m = "missing CookieAuthenticatorBuilder"
      logger.error(m)
      throw new AuthenticationException(m)
    }
  }

  /**
   * Common method to handle GET and POST authentication requests
   *
   * @param providerId the provider that needs to handle the flow
   * @param redirectTo the url the user needs to be redirected to after being authenticated
   */
  private def handleAuth(providerId: String, redirectTo: Option[String]) = UserAwareAction.async { implicit request =>
    import scala.concurrent.ExecutionContext.Implicits.global
    val authenticationFlow = request.user.isEmpty


    env.providers.get(providerId).map { identityProvider =>
      val futureAuth : Future[AuthenticationResult] = identityProvider.authenticate();
      futureAuth.flatMap {

        case denied: AuthenticationResult.AccessDenied => Future.successful {
          Redirect(env.routes.loginPageUrl).flashing("error" -> Messages("securesocial.login.accessDenied"))
        }

        case failed: AuthenticationResult.Failed =>
          val m = s"authentication failed, reason: ${failed.error}"
          logger.error(m)
          throw new AuthenticationException(m)

        case flow: AuthenticationResult.NavigationFlow => Future.successful {
          redirectTo.map { url =>
            flow.result.addToSession(SecureSocial.OriginalUrlKey -> url)
          } getOrElse flow.result
        }

        case authenticated: AuthenticationResult.Authenticated =>
          if (authenticationFlow) {
            Future.successful(byAuthenticateFlow(authenticated))
          } else {
            Future.successful(withoutAuthenticateFlow(authenticated, redirectTo))
          }
      } recover {
        case e =>
          logger.error("Unable to log user in. An exception was thrown", e)
          Redirect(env.routes.loginPageUrl).flashing("error" -> Messages("securesocial.login.errorLoggingIn"))
      }
    } getOrElse {
      Future.successful(NotFound)
    }
  }

  private def byAuthenticateFlow(authenticated : AuthenticationResult.Authenticated)(implicit request : Request[_]) : Result = {
    val profile = authenticated.profile
    val maybeExisting = env.userService.find(profile.providerId, profile.userId)

    val mode = if (maybeExisting.isDefined) SaveMode.LoggedIn else SaveMode.SignUp

    val userForAction = env.userService.save(authenticated.profile, mode)
    logger.debug(s"[securesocial] user completed authentication: provider = ${profile.providerId}, userId: ${profile.userId}, mode = $mode")

    val evt = if (mode == SaveMode.LoggedIn) new LoginEvent(userForAction) else new SignUpEvent(userForAction)

    val sessionAfterEvents = Events.fire(evt).getOrElse(request.session)

    import scala.concurrent.ExecutionContext.Implicits.global
    val authenticator = builder().fromUser(userForAction)
    Redirect(toUrl(sessionAfterEvents)).withSession(sessionAfterEvents -
      SecureSocial.OriginalUrlKey -
      IdentityProvider.SessionId -
      OAuth1Provider.CacheKey).startingAuthenticator(authenticator)

  }

  private def withoutAuthenticateFlow(authenticated : AuthenticationResult.Authenticated, redirectTo: Option[String])(implicit request : RequestWithUser[_]): Result = {
    val modifiedSession = overrideOriginalUrl(request.session, redirectTo)
    request.user match {
      case Some(currentUser) =>
        val linked = env.userService.link(currentUser, authenticated.profile)
        val updatedAuthenticator = request.authenticator.get.updateUser(linked);
        logger.debug(s"[securesocial] linked $currentUser to: providerId = ${authenticated.profile.providerId}")

        Redirect(toUrl(modifiedSession)).withSession(modifiedSession -
                SecureSocial.OriginalUrlKey -
                IdentityProvider.SessionId -
                OAuth1Provider.CacheKey).touchingAuthenticator(updatedAuthenticator)

      case _ =>
        Unauthorized
    }

  }
}

object ProviderControllerHelper {
  val logger = play.api.Logger("securesocial.controllers.ProviderController")

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
   * The url where the user needs to be redirected after succesful authentication.
   *
   * @return
   */
  def landingUrl = Play.configuration.getString(onLoginGoTo).getOrElse(
    Play.configuration.getString(ApplicationContext).getOrElse(Root)
  )

  /**
   * Returns the url that the user should be redirected to after login
   *
   * @param session
   * @return
   */
  def toUrl(session: Session) = session.get(SecureSocial.OriginalUrlKey).getOrElse(ProviderControllerHelper.landingUrl)
}

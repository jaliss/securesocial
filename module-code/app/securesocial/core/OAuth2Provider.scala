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

import _root_.java.net.URLEncoder
import _root_.java.util.UUID
import play.api.{Logger, Play, Application}
import play.api.cache.Cache
import Play.current
import play.api.mvc.{Results, Result, Request}
import securesocial.controllers.routes
import play.api.libs.ws.{Response, WS}

/**
 * Base class for all OAuth2 providers
 */
abstract class OAuth2Provider(application: Application) extends IdentityProvider(application) {
  val settings = createSettings()

  def authMethod = AuthenticationMethod.OAuth2

  private def createSettings(): OAuth2Settings = {
    val result = for {
      authorizationUrl <- loadProperty(OAuth2Settings.AuthorizationUrl) ;
      accessToken <- loadProperty(OAuth2Settings.AccessTokenUrl) ;
      clientId <- loadProperty(OAuth2Settings.ClientId) ;
      clientSecret <- loadProperty(OAuth2Settings.ClientSecret)
    } yield {
      val scope = application.configuration.getString(propertyKey + OAuth2Settings.Scope)
      OAuth2Settings(authorizationUrl, accessToken, clientId, clientSecret, scope)
    }
    if ( !result.isDefined ) {
      throw new RuntimeException("Missing properties for provider " + providerId)
    }
    result.get
  }

  private def getAccessToken[A](code: String)(implicit request: Request[A]):OAuth2Info = {
    val params = Map(
      OAuth2Constants.ClientId -> Seq(settings.clientId),
      OAuth2Constants.ClientSecret -> Seq(settings.clientSecret),
      OAuth2Constants.GrantType -> Seq(OAuth2Constants.AuthorizationCode),
      OAuth2Constants.Code -> Seq(code),
      OAuth2Constants.RedirectUri -> Seq(routes.LoginPage.authenticate(providerId).absoluteURL())
    )
    WS.url(settings.accessTokenUrl).post(params).await(10000).fold( onError =>
      {
        Logger.error("Timed out trying to get an access token for provider " + providerId)
        throw new AuthenticationException()
      },
      response =>  buildInfo(response)
    )
  }

  protected def buildInfo(response: Response): OAuth2Info = {
      val json = response.json
      Logger.debug("Got json back [" + json + "]")
      OAuth2Info(
        (json \ OAuth2Constants.AccessToken).as[String],
        (json \ OAuth2Constants.TokenType).asOpt[String],
        (json \ OAuth2Constants.ExpiresIn).asOpt[Int],
        (json \ OAuth2Constants.RefreshToken).asOpt[String]
      )
  }

  def doAuth[A]()(implicit request: Request[A]): Either[Result, SocialUser] = {
    request.queryString.get(OAuth2Constants.Error).flatMap(_.headOption).map( error => {
      Logger.error(providerId + " error = [" + error + "]")
      error match {
        case OAuth2Constants.AccessDenied => throw new AccessDeniedException()
        case _ =>
          Logger.error("Error '" + error + "' returned by the authorization server. Provider type is " + providerId)
          throw new AuthenticationException()
      }
      throw new AuthenticationException()
    })

    request.queryString.get(OAuth2Constants.Code).flatMap(_.headOption) match {
      case Some(code) =>
        // we're being redirected back from the authorization server with the access code.
        val user = for (
          // check if the state we sent is equal to the one we're receiving now before continuing the flow.
          sessionId <- request.session.get(IdentityProvider.SessionId) ;
          originalState <- Cache.getAs[String](sessionId) ;
          currentState <- request.queryString.get(OAuth2Constants.State).flatMap(_.headOption) if originalState == currentState
        ) yield {
          val accessToken = getAccessToken(code)
          val oauth2Info = Some(
            OAuth2Info(accessToken.accessToken, accessToken.tokenType, accessToken.expiresIn, accessToken.refreshToken)
          )
          SocialUser(UserId("", providerId), "", None, None, authMethod, None, oauth2Info)
        }
        if ( Logger.isDebugEnabled ) {
          Logger.debug("user = " + user)
        }
        user match  {
          case Some(u) => Right(u)
          case _ => throw new AuthenticationException()
        }
      case None =>
        // There's no code in the request, this is the first step in the oauth flow
        val state = UUID.randomUUID().toString
        val sessionId = request.session.get(IdentityProvider.SessionId).getOrElse(UUID.randomUUID().toString)
        Cache.set(sessionId, state)
        var params = List(
          (OAuth2Constants.ClientId, settings.clientId),
          (OAuth2Constants.RedirectUri, routes.LoginPage.authenticate(providerId).absoluteURL()),
          (OAuth2Constants.ResponseType, OAuth2Constants.Code),
          (OAuth2Constants.State, state))
        settings.scope.foreach( s => { params = (OAuth2Constants.Scope, s) :: params })
        val url = settings.authorizationUrl +
          params.map( p => p._1 + "=" + URLEncoder.encode(p._2, "UTF-8")).mkString("?", "&", "")
        if ( Logger.isDebugEnabled ) {
          Logger.debug("authorizationUrl = " + settings.authorizationUrl)
          Logger.debug("Redirecting to : [" + url + "]")
        }
        Left(Results.Redirect( url ).withSession(request.session + (IdentityProvider.SessionId, sessionId)))
    }
  }
}

case class OAuth2Settings(authorizationUrl: String, accessTokenUrl: String, clientId: String,
                          clientSecret: String, scope: Option[String]
                           )

object OAuth2Settings {
  val AuthorizationUrl = "authorizationUrl"
  val AccessTokenUrl = "accessTokenUrl"
  val ClientId = "clientId"
  val ClientSecret = "clientSecret"
  val Scope = "scope"
}

object OAuth2Constants {
  val ClientId = "client_id"
  val ClientSecret = "client_secret"
  val RedirectUri = "redirect_uri"
  val Scope = "scope"
  val ResponseType = "response_type"
  val State = "state"
  val GrantType = "grant_type"
  val AuthorizationCode = "authorization_code"
  val AccessToken = "access_token"
  val Error = "error"
  val Code = "code"
  val TokenType = "token_type"
  val ExpiresIn = "expires_in"
  val RefreshToken = "refresh_token"
  val AccessDenied = "access_denied"
}

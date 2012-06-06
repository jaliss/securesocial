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

import _root_.java.util.UUID
import play.api.cache.Cache
import play.api.libs.oauth.{RequestToken, ConsumerKey, OAuth, ServiceInfo}
import play.api.{Application, Logger, Play}
import securesocial.controllers.routes
import play.api.mvc.{Request, Result}
import play.api.mvc.Results.Redirect
import Play.current


/**
 * Base class for all OAuth1 providers
 */
abstract class OAuth1Provider(application: Application) extends IdentityProvider(application)  {
  val serviceInfo = createServiceInfo(propertyKey)
  val service = OAuth(serviceInfo, true)

  def authMethod = AuthenticationMethod.OAuth1

  def createServiceInfo(key: String): ServiceInfo = {
    val result = for {
      requestTokenUrl <- loadProperty(OAuth1Provider.RequestTokenUrl) ;
      accessTokenUrl <- loadProperty(OAuth1Provider.AccessTokenUrl) ;
      authorizationUrl <- loadProperty(OAuth1Provider.AuthorizationUrl) ;
      consumerKey <- loadProperty(OAuth1Provider.ConsumerKey) ;
      consumerSecret <- loadProperty(OAuth1Provider.ConsumerSecret)
    } yield {
      ServiceInfo(requestTokenUrl, accessTokenUrl, authorizationUrl, ConsumerKey(consumerKey, consumerSecret))
    }

    if ( result.isEmpty ) {
      throw new RuntimeException("Missing properties for provider " + providerId)
    }
    result.get
  }


  def doAuth[A]()(implicit request: Request[A]):Either[Result, SocialUser] = {
    if ( request.queryString.get("denied").isDefined ) {
      // the user did not grant access to the account
      throw new AccessDeniedException()
    }

    request.queryString.get("oauth_verifier").map { seq =>
      val verifier = seq.head
      // 2nd step in the oauth flow, we have the access token in the cache, we need to
      // swap it for the access token
      val user = for {
        cacheKey <- request.session.get("cacheKey")
        requestToken <- Cache.getAs[RequestToken](cacheKey)
      } yield {
        service.retrieveAccessToken(RequestToken(requestToken.token, requestToken.secret), verifier) match {
          case Right(token) =>
            // the Cache api does not have a remove method.  Just set the cache key and expire it after 1 second for
            // now.
            Cache.set(cacheKey, Unit, 1)
            Right(
              SocialUser(
                UserId("", providerId), "", None, None, authMethod,
                Some(OAuth1Info(serviceInfo, token.token, token.secret))
              )
            )
          case Left(oauthException) =>
            Logger.error("Error retrieving access token", oauthException)
            throw new AuthenticationException()
        }
      }
      user.getOrElse( throw new AuthenticationException() )
    }.getOrElse {
      // the oauth_verifier field is not in the request, this is the 1st step in the auth flow.
      // we need to get the request tokens
      val callbackUrl = routes.LoginPage.authenticate(providerId).absoluteURL()
      if ( Logger.isDebugEnabled ) {
        Logger.debug("callback url = " + callbackUrl)
      }
      service.retrieveRequestToken(callbackUrl) match {
        case Right(accessToken) =>
          val cacheKey = UUID.randomUUID().toString
          val redirect = Redirect(service.redirectUrl(accessToken.token)).withSession("cacheKey" -> cacheKey)
          Cache.set(cacheKey, accessToken, 600) // set it for 10 minutes, plenty of time to log in
          Left(redirect)
        case Left(e) =>
          Logger.error("Error retrieving request token", e)
          throw new AuthenticationException()
      }
    }
  }
}

object OAuth1Provider {
  val RequestTokenUrl = "requestTokenUrl"
  val AccessTokenUrl = "accessTokenUrl"
  val AuthorizationUrl = "authorizationUrl"
  val ConsumerKey = "consumerKey"
  val ConsumerSecret = "consumerSecret"
}

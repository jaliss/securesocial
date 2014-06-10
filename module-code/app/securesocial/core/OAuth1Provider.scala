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

import _root_.java.util.UUID
import play.api.libs.oauth._
import play.api.mvc.{AnyContent, Request}
import play.api.mvc.Results.Redirect
import oauth.signpost.exception.OAuthException
import scala.concurrent.{ExecutionContext, Future}
import securesocial.core.services.{HttpService, RoutesService, CacheService}
import play.api.libs.oauth.OAuth
import play.api.libs.oauth.ServiceInfo
import play.api.libs.oauth.RequestToken
import play.api.libs.oauth.ConsumerKey
import play.api.libs.json.JsValue


/**
 * A trait that allows mocking the OAuth 1 client
 */
trait OAuth1Client {

  def retrieveRequestToken(callbackURL: String)(implicit ec:ExecutionContext): Future[RequestToken]

  def retrieveOAuth1Info(token: RequestToken, verifier: String)(implicit ec:ExecutionContext): Future[OAuth1Info]

  def redirectUrl(token: String): String

  def retrieveProfile(url:String, info:OAuth1Info)(implicit ec:ExecutionContext):Future[JsValue]
}

object OAuth1Client {
  /**
   * A default implementation based on the Play client
   * @param serviceInfo
   */
  class Default(val serviceInfo: ServiceInfo, val httpService: HttpService) extends OAuth1Client {
    private[core] val client = OAuth(serviceInfo, use10a = true)
    override def redirectUrl(token: String): String = client.redirectUrl(token)

    private def withFuture(call: => Either[OAuthException, RequestToken])(implicit ec:ExecutionContext): Future[RequestToken] = Future {
      call match {
        case Left(error) => throw error
        case Right(token) => token
      }
    }

    override def retrieveOAuth1Info(token: RequestToken, verifier: String)(implicit ec:ExecutionContext) = withFuture {
      client.retrieveAccessToken(token, verifier)
    }.map(accessToken=>OAuth1Info(accessToken.token, accessToken.secret))

    override def retrieveRequestToken(callbackURL: String)(implicit ec:ExecutionContext) = withFuture {
      client.retrieveRequestToken(callbackURL)
    }

    override def retrieveProfile(url: String, info: OAuth1Info)(implicit ec:ExecutionContext):Future[JsValue] =
      httpService.url(url).sign(OAuthCalculator(serviceInfo.key, RequestToken(info.token, info.secret))).get().map(_.json)
  }
}

object ServiceInfoHelper {
  import IdentityProvider._

  /**
   * A helper method to create a service info from the properties file
   * @param id
   * @return
   */
  def forProvider(id: String): ServiceInfo = {
    val result = for {
      requestTokenUrl <- loadProperty(id, OAuth1Provider.RequestTokenUrl) ;
      accessTokenUrl <- loadProperty(id, OAuth1Provider.AccessTokenUrl) ;
      authorizationUrl <- loadProperty(id, OAuth1Provider.AuthorizationUrl) ;
      consumerKey <- loadProperty(id, OAuth1Provider.ConsumerKey) ;
      consumerSecret <- loadProperty(id, OAuth1Provider.ConsumerSecret)
    } yield {
      ServiceInfo(requestTokenUrl, accessTokenUrl, authorizationUrl, ConsumerKey(consumerKey, consumerSecret))
    }

    if ( result.isEmpty ) {
      throwMissingPropertiesException(id)
    }
    result.get

  }
}

/**
 * Base class for all OAuth1 providers
 */
abstract class OAuth1Provider(routesService: RoutesService,
                              cacheService: CacheService, val client: OAuth1Client) extends IdentityProvider  {
  protected val logger = play.api.Logger(this.getClass.getName)

  def authMethod = AuthenticationMethod.OAuth1

  def authenticate()(implicit request: Request[AnyContent]): Future[AuthenticationResult] = {
    import ExecutionContext.Implicits.global
    if (request.queryString.get("denied").isDefined) {
      // the user did not grant access to the account
      Future.successful(AuthenticationResult.AccessDenied())
    }
    val verifier = request.queryString.get("oauth_verifier").map(_.head)
    if (verifier.isEmpty) {
      // this is the 1st step in the auth flow. We need to get the request tokens
      val callbackUrl = routesService.authenticationUrl(id)
      logger.debug("[securesocial] callback url = " + callbackUrl)
      client.retrieveRequestToken(callbackUrl).flatMap {
        case accessToken =>
          val cacheKey = UUID.randomUUID().toString
          val redirect = Redirect(client.redirectUrl(accessToken.token)).withSession(request.session +
            (OAuth1Provider.CacheKey -> cacheKey))
          // set the cache key timeoutfor 5 minutes, plenty of time to log in
          cacheService.set(cacheKey, accessToken, 300).map {
            u =>
              AuthenticationResult.NavigationFlow(redirect)
          }
      } recover {
        case e =>
          logger.error("[securesocial] error retrieving request token", e)
          throw new AuthenticationException()
      }
    } else {
      // 2nd step in the oauth flow
      val cacheKey = request.session.get(OAuth1Provider.CacheKey).getOrElse {
        logger.error("[securesocial] missing cache key in session during OAuth1 flow")
        throw new AuthenticationException()
      }
      for (
        requestToken <- cacheService.getAs[RequestToken](cacheKey).recover {
          case e => logger.error("[securesocial] error retrieving entry from cache", e)
            throw new AuthenticationException()
        };
        accessToken <- client.retrieveOAuth1Info(
          RequestToken(requestToken.get.token, requestToken.get.secret), verifier.get
        ).recover {
          case e => logger.error("[securesocial] error retrieving access token", e)
            throw new AuthenticationException()
        };
        result <- fillProfile(accessToken)
      ) yield {
        AuthenticationResult.Authenticated(result)
      }
    }
  }

  def fillProfile(info: OAuth1Info): Future[BasicProfile]
}

object OAuth1Provider {
  val CacheKey = "cacheKey"
  val RequestTokenUrl = "requestTokenUrl"
  val AccessTokenUrl = "accessTokenUrl"
  val AuthorizationUrl = "authorizationUrl"
  val ConsumerKey = "consumerKey"
  val ConsumerSecret = "consumerSecret"
}

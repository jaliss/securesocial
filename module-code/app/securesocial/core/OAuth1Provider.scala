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

import com.typesafe.config.ConfigObject
import io.methvin.play.autoconfig.AutoConfig
import play.api.libs.json.JsValue
import play.api.libs.oauth.{ ConsumerKey, OAuth, RequestToken, ServiceInfo, _ }
import play.api.mvc.Results.Redirect
import play.api.mvc.{ AnyContent, Request }
import play.api.{ ConfigLoader, Configuration }
import play.shaded.oauth.oauth.signpost.exception.OAuthException
import securesocial.core.services.{ CacheService, HttpService, RoutesService }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * A trait that allows mocking the OAuth 1 client
 */
trait OAuth1Client {

  def retrieveRequestToken(callbackURL: String): Future[RequestToken]

  def retrieveOAuth1Info(token: RequestToken, verifier: String): Future[OAuth1Info]

  def redirectUrl(token: String): String

  def retrieveProfile(url: String, info: OAuth1Info): Future[JsValue]

  implicit def executionContext: ExecutionContext
}

object OAuth1Client {
  /**
   * A default implementation based on the Play client
   * @param serviceInfo
   */
  class Default(val serviceInfo: ServiceInfo, val httpService: HttpService)(implicit val executionContext: ExecutionContext) extends OAuth1Client {
    private[core] val client = OAuth(serviceInfo, use10a = true)
    override def redirectUrl(token: String): String = client.redirectUrl(token)

    private def withFuture(call: => Either[OAuthException, RequestToken]): Future[RequestToken] = Future {
      call match {
        case Left(error) => throw error
        case Right(token) => token
      }
    }

    override def retrieveOAuth1Info(token: RequestToken, verifier: String) = withFuture {
      client.retrieveAccessToken(token, verifier)
    }.map(accessToken => OAuth1Info(accessToken.token, accessToken.secret))

    override def retrieveRequestToken(callbackURL: String) = withFuture {
      client.retrieveRequestToken(callbackURL)
    }

    override def retrieveProfile(url: String, info: OAuth1Info): Future[JsValue] =
      httpService.url(url).sign(OAuthCalculator(serviceInfo.key, RequestToken(info.token, info.secret))).get().map(_.json)
  }
}

case class OAuth1Settings(
  requestTokenUrl: String,
  accessTokenUrl: String,
  authorizationUrl: String,
  consumerKey: String,
  consumerSecret: String)
object OAuth1Settings {
  implicit val configLoader: ConfigLoader[OAuth1Settings] = AutoConfig.loader
  def forProvider(configuration: Configuration, id: String): OAuth1Settings = {
    val path = s"securesocial.$id"
    val defaultPath = "securesocial.oauth1Settings"
    (Configuration(path -> configuration.get[ConfigObject](defaultPath)) ++ configuration).get[OAuth1Settings](path)
  }
}

object ServiceInfoHelper {
  /**
   * A helper method to create a service info from the properties file
   * @param id
   * @return
   */
  def forProvider(configuration: Configuration, id: String): ServiceInfo = {
    val config = configuration.get[OAuth1Settings](s"securesocial.$id")
    import config._
    ServiceInfo(requestTokenUrl, accessTokenUrl, authorizationUrl, ConsumerKey(consumerKey, consumerSecret))
  }
}

/**
 * Base class for all OAuth1 providers
 */
abstract class OAuth1Provider(
  routesService: RoutesService,
  cacheService: CacheService,
  val client: OAuth1Client)
  extends IdentityProvider {

  protected implicit val executionContext = client.executionContext
  protected val logger = play.api.Logger(this.getClass.getName)

  def authMethod = AuthenticationMethod.OAuth1

  def authenticate()(implicit request: Request[AnyContent]): Future[AuthenticationResult] = {
    if (request.queryString.get("denied").isDefined) {
      // the user did not grant access to the account
      Future.successful(AuthenticationResult.AccessDenied())
    } else {
      val verifier = request.queryString.get("oauth_verifier").map(_.head)
      if (verifier.isEmpty) {
        // this is the 1st step in the auth flow. We need to get the request tokens
        val callbackUrl = routesService.authenticationUrl(id)
        logger.debug("[securesocial] callback url = " + callbackUrl)
        client.retrieveRequestToken(callbackUrl).flatMap { accessToken =>
          val cacheKey = UUID.randomUUID().toString
          val redirect = Redirect(client.redirectUrl(accessToken.token))
            .withSession(request.session + (OAuth1Provider.CacheKey -> cacheKey))
          // set the cache key timeoutfor 5 minutes, plenty of time to log in
          cacheService
            .set(cacheKey, accessToken, 300)
            .map(_ => AuthenticationResult.NavigationFlow(redirect))
        } recover {
          case e =>
            logger.error("[securesocial] error retrieving request token", e)
            throw AuthenticationException()
        }
      } else {
        // 2nd step in the oauth flow
        val cacheKey = request.session.get(OAuth1Provider.CacheKey).getOrElse {
          logger.error("[securesocial] missing cache key in session during OAuth1 flow")
          throw AuthenticationException()
        }
        for (
          requestToken <- cacheService.getAs[RequestToken](cacheKey).recover {
            case e =>
              logger.error("[securesocial] error retrieving entry from cache", e)
              throw AuthenticationException()
          };
          accessToken <- client.retrieveOAuth1Info(
            RequestToken(requestToken.get.token, requestToken.get.secret), verifier.get).recover {
              case e =>
                logger.error("[securesocial] error retrieving access token", e)
                throw AuthenticationException()
            };
          result <- fillProfile(accessToken)
        ) yield {
          AuthenticationResult.Authenticated(result)
        }
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

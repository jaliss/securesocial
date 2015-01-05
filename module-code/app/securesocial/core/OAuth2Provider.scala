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

import _root_.java.net.URLEncoder
import _root_.java.util.UUID

import play.api.Play
import play.api.libs.json.{ JsError, JsSuccess, JsValue, Json }
import play.api.libs.ws.WSResponse
import play.api.mvc._
import securesocial.core.services.{ CacheService, HttpService, RoutesService }

import scala.collection.JavaConversions._
import scala.concurrent.{ ExecutionContext, Future }

trait OAuth2Client {
  val settings: OAuth2Settings
  val httpService: HttpService

  def exchangeCodeForToken(code: String, callBackUrl: String, builder: OAuth2InfoBuilder): Future[OAuth2Info]

  def retrieveProfile(profileUrl: String): Future[JsValue]

  type OAuth2InfoBuilder = WSResponse => OAuth2Info

  implicit def executionContext: ExecutionContext
}

object OAuth2Client {

  class Default(val httpService: HttpService, val settings: OAuth2Settings)(implicit val executionContext: ExecutionContext)
      extends OAuth2Client {

    override def exchangeCodeForToken(code: String, callBackUrl: String, builder: OAuth2InfoBuilder): Future[OAuth2Info] = {
      val params = Map(
        OAuth2Constants.ClientId -> Seq(settings.clientId),
        OAuth2Constants.ClientSecret -> Seq(settings.clientSecret),
        OAuth2Constants.GrantType -> Seq(OAuth2Constants.AuthorizationCode),
        OAuth2Constants.Code -> Seq(code),
        OAuth2Constants.RedirectUri -> Seq(callBackUrl)
      ) ++ settings.accessTokenUrlParams.mapValues(Seq(_))
      httpService.url(settings.accessTokenUrl).post(params).map(builder)
    }

    override def retrieveProfile(profileUrl: String): Future[JsValue] =
      httpService.url(profileUrl).get().map(_.json)
  }
}
/**
 * Base class for all OAuth2 providers
 */
abstract class OAuth2Provider(
  routesService: RoutesService,
  client: OAuth2Client,
  cacheService: CacheService)
    extends IdentityProvider with ApiSupport {

  protected implicit val executionContext: ExecutionContext = client.executionContext
  protected val logger = play.api.Logger(this.getClass.getName)

  val settings = client.settings
  def authMethod = AuthenticationMethod.OAuth2

  protected def getAccessToken[A](code: String)(implicit request: Request[A]): Future[OAuth2Info] = {
    val callbackUrl = routesService.authenticationUrl(id)
    client.exchangeCodeForToken(code, callbackUrl, buildInfo)
      .recover {
        case e =>
          logger.error("[securesocial] error trying to get an access token for provider %s".format(id), e)
          throw new AuthenticationException()
      }
  }

  protected def buildInfo(response: WSResponse): OAuth2Info = {
    val json = response.json
    logger.debug("[securesocial] got json back [" + json + "]")
    OAuth2Info(
      (json \ OAuth2Constants.AccessToken).as[String],
      (json \ OAuth2Constants.TokenType).asOpt[String],
      (json \ OAuth2Constants.ExpiresIn).asOpt[Int],
      (json \ OAuth2Constants.RefreshToken).asOpt[String]
    )
  }

  def authenticate()(implicit request: Request[AnyContent]): Future[AuthenticationResult] = {
    val maybeError = request.queryString.get(OAuth2Constants.Error).flatMap(_.headOption).map {
      case OAuth2Constants.AccessDenied => Future.successful(AuthenticationResult.AccessDenied())
      case error =>
        Future.failed {
          logger.error(s"[securesocial] error '$error' returned by the authorization server. Provider is $id")
          throw new AuthenticationException()
        }
    }
    maybeError.getOrElse {
      request.queryString.get(OAuth2Constants.Code).flatMap(_.headOption) match {
        case Some(code) =>
          // we're being redirected back from the authorization server with the access code.
          val result = for (
            // check if the state we sent is equal to the one we're receiving now before continuing the flow.
            // todo: review this -> clustered environments
            stateOk <- request.session.get(IdentityProvider.SessionId).map(cacheService.getAs[String](_).map {
              originalState =>
                val stateInQueryString = request.queryString.get(OAuth2Constants.State).flatMap(_.headOption)
                originalState == stateInQueryString
            })
              .getOrElse {
                Future.failed {
                  logger.error("[securesocial] missing sid in session.")
                  throw new AuthenticationException()
                }
              };
            accessToken <- getAccessToken(code) if stateOk;
            user <- fillProfile(OAuth2Info(accessToken.accessToken, accessToken.tokenType, accessToken.expiresIn, accessToken.refreshToken))
          ) yield {
            logger.debug(s"[securesocial] user loggedin using provider $id = $user")
            AuthenticationResult.Authenticated(user)
          }
          result recover {
            case e =>
              logger.error("[securesocial] error authenticating user", e)
              throw e
          }
        case None =>
          // There's no code in the request, this is the first step in the oauth flow
          val state = UUID.randomUUID().toString
          val sessionId = request.session.get(IdentityProvider.SessionId).getOrElse(UUID.randomUUID().toString)
          cacheService.set(sessionId, state, 300).map {
            unit =>
              var params = List(
                (OAuth2Constants.ClientId, settings.clientId),
                (OAuth2Constants.RedirectUri, routesService.authenticationUrl(id)),
                (OAuth2Constants.ResponseType, OAuth2Constants.Code),
                (OAuth2Constants.State, state))
              settings.scope.foreach(s => {
                params = (OAuth2Constants.Scope, s) :: params
              })
              settings.authorizationUrlParams.foreach(e => {
                params = e :: params
              })
              val url = settings.authorizationUrl +
                params.map(p => URLEncoder.encode(p._1, "UTF-8") + "=" + URLEncoder.encode(p._2, "UTF-8")).mkString("?", "&", "")
              logger.debug("[securesocial] authorizationUrl = %s".format(settings.authorizationUrl))
              logger.debug("[securesocial] redirecting to: [%s]".format(url))
              AuthenticationResult.NavigationFlow(Results.Redirect(url).withSession(request.session + (IdentityProvider.SessionId -> sessionId)))
          }
      }
    }
  }

  def fillProfile(info: OAuth2Info): Future[BasicProfile]

  /**
   * Defines the request format for api authentication requests
   * @param email the user email
   * @param info the OAuth2Info as returned by some Oauth2 service on the client side (eg: JS app)
   */
  case class LoginJson(email: String, info: OAuth2Info)

  /**
   * A Reads instance for the OAuth2Info case class
   */
  implicit val OAuth2InfoReads = Json.reads[OAuth2Info]

  /**
   * A Reads instance for the LoginJson case class
   */
  implicit val LoginJsonReads = Json.reads[LoginJson]

  /**
   * The error returned for malformed requests
   */
  val malformedJson = Json.obj("error" -> "Malformed json").toString()

  def authenticateForApi(implicit request: Request[AnyContent]): Future[AuthenticationResult] = {
    val maybeCredentials = request.body.asJson flatMap {
      _.validate[LoginJson] match {
        case ok: JsSuccess[LoginJson] =>
          Some(ok.get)
        case error: JsError =>
          val e = JsError.toFlatJson(error).toString()
          logger.error(s"[securesocial] error parsing json: $e")
          None
      }
    }

    maybeCredentials.map { credentials =>
      fillProfile(credentials.info).map { profile =>
        if (profile.email.isDefined && profile.email.get == credentials.email.toLowerCase) {
          AuthenticationResult.Authenticated(profile)
        } else {
          AuthenticationResult.Failed("wrong credentials")
        }
      } recover {
        case e: Throwable =>
          logger.error(s"[securesocial] error authenticating user via api", e)
          throw e
      }
    } getOrElse {
      Future.successful(AuthenticationResult.Failed(malformedJson))
    }
  }
}

/**
 * The settings for OAuth2 providers.
 */
case class OAuth2Settings(authorizationUrl: String, accessTokenUrl: String, clientId: String,
  clientSecret: String, scope: Option[String],
  authorizationUrlParams: Map[String, String], accessTokenUrlParams: Map[String, String])

object OAuth2Settings {
  val AuthorizationUrl = "authorizationUrl"
  val AccessTokenUrl = "accessTokenUrl"
  val AuthorizationUrlParams = "authorizationUrlParams"
  val AccessTokenUrlParams = "accessTokenUrlParams"
  val ClientId = "clientId"
  val ClientSecret = "clientSecret"
  val Scope = "scope"

  /**
   * Helper method to create an OAuth2Settings instance from the properties file.
   *
   * @param id the provider id
   * @return an OAuth2Settings instance
   */
  def forProvider(id: String): OAuth2Settings = {
    import securesocial.core.IdentityProvider.loadProperty
    val propertyKey = s"securesocial.$id."

    val result = for {
      authorizationUrl <- loadProperty(id, OAuth2Settings.AuthorizationUrl);
      accessToken <- loadProperty(id, OAuth2Settings.AccessTokenUrl);
      clientId <- loadProperty(id, OAuth2Settings.ClientId);
      clientSecret <- loadProperty(id, OAuth2Settings.ClientSecret)
    } yield {
      val config = Play.current.configuration
      val scope = loadProperty(id, OAuth2Settings.Scope, optional = true)
      val authorizationUrlParams: Map[String, String] =
        config.getObject(propertyKey + OAuth2Settings.AuthorizationUrlParams).map { o =>
          o.unwrapped.toMap.mapValues(_.toString)
        }.getOrElse(Map())

      val accessTokenUrlParams: Map[String, String] = config.getObject(propertyKey + OAuth2Settings.AccessTokenUrlParams).map { o =>
        o.unwrapped.toMap.mapValues(_.toString)
      }.getOrElse(Map())
      OAuth2Settings(authorizationUrl, accessToken, clientId, clientSecret, scope, authorizationUrlParams, accessTokenUrlParams)
    }
    if (!result.isDefined) {
      IdentityProvider.throwMissingPropertiesException(id)
    }
    result.get
  }
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

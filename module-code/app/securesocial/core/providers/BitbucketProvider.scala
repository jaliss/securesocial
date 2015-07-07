/**
 * This file is based on GitHubProvider.scala
 * Original work: Copyright 2012-2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 * Modifcations: Copyright 2015 KASHIMA Kazuo (k4200 at kazu dot tv) - twitter: @k4200
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
package securesocial.core.providers

import play.api.libs.ws.{ WSResponse, WSAuthScheme }
import play.api.libs.json.{ Reads, Json, JsValue }
import securesocial.core._
import securesocial.core.services.{ CacheService, HttpService, RoutesService }

import scala.concurrent.{ ExecutionContext, Future }

import BitbucketProvider.{ ErrorResponse, UserResponse }

class BitbucketOAuth2Client(
    httpService: HttpService, settings: OAuth2Settings)(implicit executionContext: ExecutionContext) extends OAuth2Client.Default(httpService, settings)(executionContext) {
  override def exchangeCodeForToken(code: String, callBackUrl: String, builder: OAuth2InfoBuilder): Future[OAuth2Info] = {
    val params = Map(
      OAuth2Constants.GrantType -> Seq(OAuth2Constants.AuthorizationCode),
      OAuth2Constants.Code -> Seq(code),
      OAuth2Constants.RedirectUri -> Seq(callBackUrl)
    ) ++ settings.accessTokenUrlParams.mapValues(Seq(_))
    httpService.url(settings.accessTokenUrl)
      .withAuth(settings.clientId, settings.clientSecret, WSAuthScheme.BASIC)
      .post(params).map(builder)
  }

}

/**
 * A Bitbucket provider
 */
class BitbucketProvider(routesService: RoutesService,
  cacheService: CacheService,
  client: OAuth2Client)
    extends OAuth2Provider(routesService, client, cacheService) {
  val GetAuthenticatedUser = "https://api.bitbucket.org/2.0/user?access_token=%s"

  implicit val errorResponseReads: Reads[ErrorResponse] = Json.reads[ErrorResponse]
  implicit val userTestReads: Reads[UserResponse] = Json.reads[UserResponse]

  override val id = BitbucketProvider.Bitbucket

  override protected def buildInfo(response: WSResponse): OAuth2Info = {
    val error = (response.json \ "error").asOpt[ErrorResponse]
    if (error.isDefined) {
      logger.error("[securesocial] An error occurred while getting an access token: " + error.get.message)
      throw new AuthenticationException()
    }
    super.buildInfo(response)
  }

  def fillProfile(info: OAuth2Info): Future[BasicProfile] = {
    client.retrieveProfile(GetAuthenticatedUser.format(info.accessToken)).map { me =>
      val optError = (me \ "error").asOpt[ErrorResponse]
      optError match {
        case Some(error) =>
          logger.error(s"[securesocial] error retrieving profile information from Bitbucket. Message = ${error.message}")
          throw new AuthenticationException()
        case _ =>
          val userInfo = me.as[UserResponse]
          BasicProfile(id, userInfo.uuid, None, None, Some(userInfo.display_name), None, None, authMethod, oAuth2Info = Some(info))
      }
    } recover {
      case e: AuthenticationException => throw e
      case e =>
        logger.error("[securesocial] error retrieving profile information from github", e)
        throw new AuthenticationException()
    }
  }
}

object BitbucketProvider {
  val Bitbucket = "bitbucket"
  case class ErrorResponse(
    message: String,
    detail: String,
    id: Option[String])
  case class UserResponse(
    uuid: String,
    display_name: String,
    username: String)

  def apply(routesService: RoutesService, cacheService: CacheService, dummyClient: OAuth2Client)(implicit executionContext: ExecutionContext): BitbucketProvider = {
    val client = new BitbucketOAuth2Client(dummyClient.httpService, dummyClient.settings)
    new BitbucketProvider(routesService, cacheService, client)
  }
}

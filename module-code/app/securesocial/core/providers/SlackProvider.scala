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

import play.api.libs.ws.WSResponse
import play.api.libs.json.{ Reads, Json, JsValue }
import securesocial.core._
import securesocial.core.services.{ CacheService, RoutesService }

import scala.concurrent.Future

import SlackProvider.{ CommonResponse, AuthTestResponse }

/**
 * A Slack provider
 */
class SlackProvider(routesService: RoutesService,
  cacheService: CacheService,
  client: OAuth2Client)
    extends OAuth2Provider(routesService, client, cacheService) {
  val GetAuthenticatedUser = "https://slack.com/api/auth.test?token=%s"
  val AccessToken = "token"

  implicit val commonResponseReads: Reads[CommonResponse] = Json.reads[CommonResponse]
  implicit val authTestReads: Reads[AuthTestResponse] = Json.reads[AuthTestResponse]

  override val id = SlackProvider.Slack

  override protected def buildInfo(response: WSResponse): OAuth2Info = {
    val cr = response.json.as[CommonResponse]
    if (!cr.ok) {
      logger.error("[securesocial] An error occurred while getting an access token: " + cr.error.getOrElse("(no error message)"))
      throw new AuthenticationException()
    }
    super.buildInfo(response)
  }

  def fillProfile(info: OAuth2Info): Future[BasicProfile] = {
    client.retrieveProfile(GetAuthenticatedUser.format(info.accessToken)).map { me =>
      val response = me.as[CommonResponse]
      response.error match {
        case Some(msg) =>
          logger.error(s"[securesocial] error retrieving profile information from Slack. Message = $msg")
          throw new AuthenticationException()
        case _ =>
          val userInfo = me.as[AuthTestResponse]
          BasicProfile(id, userInfo.user_id, None, None, Some(userInfo.user), None, None, authMethod, oAuth2Info = Some(info))
      }
    } recover {
      case e: AuthenticationException => throw e
      case e =>
        logger.error("[securesocial] error retrieving profile information from github", e)
        throw new AuthenticationException()
    }
  }
}

object SlackProvider {
  val Slack = "slack"
  case class CommonResponse(
    ok: Boolean,
    error: Option[String])
  case class AuthTestResponse(
    team: String,
    user: String,
    team_id: String,
    user_id: String)
}

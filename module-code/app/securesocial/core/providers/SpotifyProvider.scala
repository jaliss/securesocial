/**
 * Copyright 2015 Mikael Vallerie
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
import securesocial.core._
import securesocial.core.services.{ CacheService, RoutesService }
import scala.concurrent.Future
import SpotifyProvider._

/**
 * A Spotify provider
 *
 */
class SpotifyProvider(routesService: RoutesService,
  cacheService: CacheService,
  client: OAuth2Client)
    extends OAuth2Provider(routesService, client, cacheService) {
  override val id = SpotifyProvider.Spotify

  def fillProfile(info: OAuth2Info): Future[BasicProfile] = {
    client.retrieveProfile(SpotifyProvider.Api.format(info.accessToken)).map { me =>
      (me \ Message).asOpt[String] match {
        case Some(msg) =>
          logger.error(s"[securesocial] error retrieving profile information from Spotify. Message = $msg")
          throw new AuthenticationException()
        case _ =>
          val userId = (me \ Id).as[String]
          val displayName = (me \ Name).asOpt[String]
          val uri = (me \ Uri).asOpt[String]
          val email = (me \ Email).asOpt[String].filter(!_.isEmpty)
          BasicProfile(id, userId.toString, None, None, displayName, email, uri, authMethod, oAuth2Info = Some(info))
      }
    } recover {
      case e: AuthenticationException => throw e
      case e =>
        logger.error("[securesocial] error retrieving profile information from Spotify", e)
        throw new AuthenticationException()
    }
  }
}

object SpotifyProvider {
  val Api = "https://api.spotify.com/v1/me?access_token=%s"
  val Spotify = "spotify"
  val Message = "message"
  val Id = "id"
  val Name = "display_name"
  val Uri = "uri"
  val Email = "email"
}

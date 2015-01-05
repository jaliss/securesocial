/**
 * Copyright 2014 @amertum
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

import play.api.libs.json.JsObject
import securesocial.core._
import securesocial.core.services.{ CacheService, RoutesService }

import scala.concurrent.Future

/**
 * A SoundcloudProvider OAuth2 Provider
 */
class SoundcloudProvider(routesService: RoutesService,
  cacheService: CacheService,
  client: OAuth2Client)
    extends OAuth2Provider(routesService, client, cacheService) {
  val UserInfoApi = "https://api.soundcloud.com/me.json?oauth_token="
  val Error = "error"
  val Message = "message"
  val Code = "code"
  val Id = "id"
  val Username = "username"
  val FullName = "full_name"
  val AvatarUrl = "avatar_url"
  val Account = "account"

  override val id = SoundcloudProvider.Soundcloud

  def fillProfile(info: OAuth2Info): Future[BasicProfile] = {
    val accessToken = info.accessToken
    client.retrieveProfile(UserInfoApi + accessToken).map { me =>
      (me \ Error).asOpt[JsObject] match {
        case Some(error) =>
          val message = (error \ Message).as[String]
          val errorCode = (error \ Code).as[String]
          logger.error(s"[securesocial] error retrieving profile information from Soundcloud. Error type = $errorCode, message = $message")
          throw new AuthenticationException()
        case _ =>
          val userId = (me \ Id).as[Int]
          val username = (me \ Username).asOpt[String]
          val fullName = (me \ FullName).asOpt[String]
          val avatarUrl = (me \ AvatarUrl).asOpt[String]
          BasicProfile(id, userId.toString, None, username, fullName, None, avatarUrl, authMethod, oAuth2Info = Some(info))
      }
    } recover {
      case e: AuthenticationException => throw e
      case e =>
        logger.error("[securesocial] error retrieving profile information from Soundcloud", e)
        throw new AuthenticationException()
    }
  }
}

object SoundcloudProvider {
  val Soundcloud = "soundcloud"
}

/**
 * Copyright 2013 Brian Porter (poornerd at gmail dot com) - twitter: @poornerd
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

import securesocial.core._
import securesocial.core.services.{CacheService, RoutesService}

import scala.concurrent.Future

/**
 * An Instagram provider
 *
 */
class InstagramProvider(routesService: RoutesService,
                        cacheService: CacheService,
                        client: OAuth2Client)
  extends OAuth2Provider(routesService, client, cacheService)
{
  val GetAuthenticatedUser = "https://api.instagram.com/v1/users/self?access_token=%s"
  val AccessToken = "access_token"
  val TokenType = "token_type"
  val Data = "data"
  val Username = "username"
  val FullName ="full_name"
  val ProfilePic = "profile_picture"
  val Id = "id"
  
  override val id = InstagramProvider.Instagram

  def fillProfile(info: OAuth2Info): Future[BasicProfile] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    client.retrieveProfile(GetAuthenticatedUser.format(info.accessToken)).map { me =>
        (me \ "response" \ "user").asOpt[String] match {
          case Some(msg) => {
            logger.error(s"[securesocial] error retrieving profile information from Instagram. Message = $msg")
            throw new AuthenticationException()
          }
          case _ =>
            val userId = (me \ Data \ Id).as[String]
            val fullName = (me \ Data \ FullName).asOpt[String]
            val avatarUrl = (me \ Data \ ProfilePic).asOpt[String]
            BasicProfile(id, userId, None, None, fullName, None, avatarUrl, authMethod, oAuth2Info = Some(info))
        }
    } recover {
      case e: AuthenticationException => throw e
      case e: Exception =>
        logger.error( "[securesocial] error retrieving profile information from Instagram", e)
        throw new AuthenticationException()
    }
  }
}

object InstagramProvider {
  val Instagram = "instagram"
}

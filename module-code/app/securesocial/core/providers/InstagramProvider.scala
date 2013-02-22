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
import play.api.{Logger, Application}
import play.api.libs.ws.WS
import securesocial.core.UserId
import securesocial.core.SocialUser
import securesocial.core.AuthenticationException
import scala.Some

/**
 * An Instagram provider
 *
 */
class InstagramProvider(application: Application) extends OAuth2Provider(application) {

  val GetAuthenticatedUser = "https://api.instagram.com/v1/users/self?access_token=%s"
  val AccessToken = "access_token"
  val TokenType = "token_type"
  val Data = "data"
  val Username = "username"
  val FullName ="full_name"
  val ProfilePic = "profile_picture"
  val Id = "id"
  
  override def id = InstagramProvider.Instagram


  /**
   * Subclasses need to implement this method to populate the User object with profile
   * information from the service provider.
   *
   * @param user The user object to be populated
   * @return A copy of the user object with the new values set
   */
  def fillProfile(user: SocialUser): SocialUser = {
    val promise = WS.url(GetAuthenticatedUser.format(user.oAuth2Info.get.accessToken)).get()

    try {
      val response = awaitResult(promise)
      val me = response.json

      (me \ "response" \ "user").asOpt[String] match {
        case Some(msg) => {
          Logger.error("[securesocial] error retrieving profile information from Instagram. Message = %s".format(msg))
          throw new AuthenticationException()
        }
        case _ => {
          val userId = ( me \ Data \ Id ).as[String]
          val fullName =  ( me \ Data \ FullName ).asOpt[String].getOrElse("")
          val avatarUrl = ( me \ Data \ ProfilePic ).asOpt[String]

          user.copy(
            id = UserId(userId , id),
            fullName = fullName,
            avatarUrl = avatarUrl
          )
        }
      }
    } catch {
      case e: Exception => {
        Logger.error( "[securesocial] error retrieving profile information from Instagram", e)
        throw new AuthenticationException()
      }
    }
  }
}

object InstagramProvider {
  val Instagram = "instagram"
}
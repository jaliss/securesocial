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
package securesocial.core.providers

import securesocial.core._
import play.api.{Logger, Application}
import play.api.libs.ws.{Response, WS}
import securesocial.core.UserId
import securesocial.core.SocialUser
import play.api.libs.ws.Response
import securesocial.core.AuthenticationException
import scala.Some

/**
 * A Weibo provider
 *
 */
class WeiboProvider(application: Application) extends OAuth2Provider(application) {
  val GetAuthenticatedUser = "https://api.weibo.com/2/users/show.json?uid=%s&access_token=%s"
  val AccessToken = "access_token"
  val Message = "error"
  val UId = "uid"
  val Id = "id"
  val Name = "name"
  val AvatarUrl = "profile_image_url"
  val GetUserEmail = "https://api.weibo.com/2/account/profile/email.json?access_token=%s"
  val Email = "email"
  var WeiboUserId:Option[String] = None
  
  override def id = WeiboProvider.Weibo

  override protected def buildInfo(response: Response): OAuth2Info = {
      val json = response.json
      if ( Logger.isDebugEnabled ) {
        Logger.debug("[securesocial] got json back [" + json + "]")
      }
      
      WeiboUserId = (json \ UId).asOpt[String]
      
      OAuth2Info(
        (json \ OAuth2Constants.AccessToken).as[String],
        (json \ OAuth2Constants.TokenType).asOpt[String],
        (json \ OAuth2Constants.ExpiresIn).asOpt[Int],
        (json \ OAuth2Constants.RefreshToken).asOpt[String]
      )
  }

  /**
   * Subclasses need to implement this method to populate the User object with profile
   * information from the service provider.
   *
   * @param user The user object to be populated
   * @return A copy of the user object with the new values set
   */
  def fillProfile(user: SocialUser): SocialUser = {
    val promise = WS.url(GetAuthenticatedUser.format(WeiboUserId,user.oAuth2Info.get.accessToken)).get()
    promise.await(10000).fold(
      error => {
        Logger.error( "[securesocial] error retrieving profile information from weibo", error)
        throw new AuthenticationException()
      },
      response => {
        val me = response.json
        (me \ Message).asOpt[String] match {
          case Some(msg) => {
            Logger.error("[securesocial] error retrieving profile information from Weibo. Message = %s".format(msg))
            throw new AuthenticationException()
          }
          case _ => {
            val userId = (me \ Id).as[String]
            val displayName = (me \ Name).asOpt[String].getOrElse("")
            val avatarUrl = (me \ AvatarUrl).asOpt[String]
            val email = getEmail(user)
            
            user.copy(
              id = UserId(userId,id),
              firstName = "",
              lastName = "",
              fullName = displayName,
              avatarUrl = avatarUrl,
              email = email
            )
          }
        }

      }
    )
  }
  
  def getEmail(user: SocialUser):Option[String] = {
    val promise = WS.url(GetUserEmail.format(user.oAuth2Info.get.accessToken)).get()
    promise.await(10000).fold(
      error => {
        Logger.error( "[securesocial] error retrieving email information from weibo", error)
        return None
      },
      response => {
        val me = response.json
        (me \ Message).asOpt[String] match {
          case Some(msg) => {
            Logger.error("[securesocial] error retrieving email information from Weibo. Message = %s".format(msg))
            return None
          }
          case _ => {
            (me \ Email).asOpt[String].filter( !_.isEmpty )
          }
        }

      }
    )
    
  }
}

object WeiboProvider {
  val Weibo = "weibo"
}
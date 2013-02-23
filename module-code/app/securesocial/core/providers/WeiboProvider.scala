/**
 * Copyright 2013 wuhaixing (wuhaixing at gmail dot com) - weibo: @数据水墨
 *                qiuzhanghua (qiuzhanghua at gmail.com) - weibo: qiuzhanghua
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
  val Id = "idstr"
  val Name = "name"
  val AvatarUrl = "profile_image_url"
  val GetUserEmail = "https://api.weibo.com/2/account/profile/email.json?access_token=%s"
  val Email = "email"


  override def id = WeiboProvider.Weibo

  /**
   *
   * According to the weibo.com's OAuth2 implemention,I use TokenType position place UId param
   * So please check http://open.weibo.com/wiki/OAuth2/access_token to ensure they stay weird
   * before you use this.   
   *
   */
  override protected def buildInfo(response: Response): OAuth2Info = {
    val json = response.json
    if (Logger.isDebugEnabled) {
      Logger.debug("[securesocial] got json back [" + json + "]")
    }
    //UId occupied TokenType in the weibo.com provider
    OAuth2Info(
      (json \ OAuth2Constants.AccessToken).as[String],
      (json \ UId).asOpt[String],
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
  override def fillProfile(user: SocialUser): SocialUser = {
    val weiboUserId = user.oAuth2Info.get.tokenType.getOrElse("")

    if (weiboUserId == "") {
      Logger.error("[securesocial] Cann't found weiboUserId")
      throw new AuthenticationException()
    }
    val promise = WS.url(GetAuthenticatedUser.format(weiboUserId, user.oAuth2Info.get.accessToken)).get()

    try {
      val response = awaitResult(promise)
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
            id = UserId(userId, id),
            fullName = displayName,
            avatarUrl = avatarUrl,
            email = email
          )
        }
      }
    } catch {
      case e: Exception => {
        Logger.error("[securesocial] error retrieving profile information from weibo", e)
        throw new AuthenticationException()
      }
    }
  }


  def getEmail(user: SocialUser): Option[String] = {

    val promise = WS.url(GetUserEmail.format(user.oAuth2Info.get.accessToken)).get()

    try {
      val response = awaitResult(promise)
      val me = response.json
      (me \ Message).asOpt[String] match {
        case Some(msg) => {
          Logger.error("[securesocial] error retrieving email information from Weibo. Message = %s".format(msg))
          None
        }
        case _ => {
          (me \ Email).asOpt[String].filter(!_.isEmpty)
        }
      }
    } catch {
      case e: Exception => {
        Logger.error("[securesocial] error retrieving profile information from weibo", e)
        None
      }
    }

  }
}

object WeiboProvider {
  val Weibo = "weibo"

}

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
import play.api.libs.oauth.{RequestToken, OAuthCalculator}
import play.api.libs.ws.WS
import play.api.{Application, Logger}
import XingProvider._

/**
 * A Xing Provider
 */
class XingProvider(application: Application) extends OAuth1Provider(application) {
  override def id = XingProvider.Xing

  override  def fillProfile(user: SocialUser): SocialUser = {
    val oauthInfo = user.oAuth1Info.get
    val call = WS.url(XingProvider.VerifyCredentials).sign(
      OAuthCalculator(SecureSocial.serviceInfoFor(user).get.key,
      RequestToken(oauthInfo.token, oauthInfo.secret))
    ).get()

    try {
      val response = awaitResult(call)
      val me = response.json

      val userId = (me \\ Id ).head.as[String]
      val displayName = (me \\ Name).head.as[String]
      val lastName = (me \\ LastName).head.as[String]
      val firstName = (me \\ FirstName).head.as[String]
      val profileImage = (me \\ Large ).head.as[String]
      val email = (me  \\ ActiveEmail).head.as[String]
      user.copy(id = UserId(userId, id),
        fullName = displayName,
        firstName = firstName,
        lastName = lastName,
        avatarUrl = Some(profileImage),
        email = Some(email)
      )

    } catch {
      case e: Exception => {
        Logger.error("[securesocial] error retrieving profile information from Xing", e)
        throw new AuthenticationException()
      }
    }
  }
}

object XingProvider {
  val VerifyCredentials = "https://api.xing.com/v1/users/me"
  val Xing = "xing"
  val Id = "id"
  val Name = "display_name"
  val FirstName = "first_name"
  val LastName = "last_name"
  val Users = "users"
  val ProfileImage = "photo_urls"
  val Large = "large"
  val ActiveEmail = "active_email"
}

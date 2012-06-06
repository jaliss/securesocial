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
import play.api.mvc.{Request, Results, Result}
import play.api.libs.oauth.{RequestToken, OAuthCalculator}
import play.api.libs.ws.{Response, WS}
import play.api.{Application, Logger}


/**
 * A Twitter Provider
 */
class TwitterProvider(application: Application) extends OAuth1Provider(application) {


  override def providerId = TwitterProvider.Twitter

  override def fillProfile(user: SocialUser): SocialUser = {
    //var result = user
    val oauthInfo = user.oAuth1Info.get
    val call = WS.url(TwitterProvider.VerifyCredentials).sign(
      OAuthCalculator(oauthInfo.serviceInfo.key,
      RequestToken(oauthInfo.token, oauthInfo.secret))).get()
    call.await(10000).fold(
      onError => {
        Logger.error("timed out waiting for Twitter")
        throw new AuthenticationException()
      },
      response =>
      {
        val me = response.json
        val id = (me \ "id").as[Int]
        val name = (me \ "name").as[String]
        val profileImage = (me \ "profile_image_url").asOpt[String]
        user.copy(id = UserId(id.toString, providerId), displayName = name, avatarUrl = profileImage)
      }
    )
  }
}

object TwitterProvider {
  val VerifyCredentials = "https://api.twitter.com/1/account/verify_credentials.json"
  val Twitter = "twitter"
}

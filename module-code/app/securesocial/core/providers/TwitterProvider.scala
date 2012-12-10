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
import play.api.libs.oauth.{RequestToken, OAuthCalculator}
import play.api.libs.ws.WS
import play.api.{Application, Logger}
import TwitterProvider._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}


/**
 * A Twitter Provider
 */
class TwitterProvider(application: Application) extends OAuth1Provider(application) {


  override def providerId = TwitterProvider.Twitter

  override def fillProfile(user: SocialUser): SocialUser = {
    val oauthInfo = user.oAuth1Info.get

    val f = WS.url(TwitterProvider.VerifyCredentials).sign(
      OAuthCalculator(oauthInfo.serviceInfo.key,
        RequestToken(oauthInfo.token, oauthInfo.secret))).get()
    val p = promise[SocialUser]
    f.onComplete {
      case Success(response) => p.success {
        val me = response.json
        val id = (me \ Id).as[Int]
        val name = (me \ Name).as[String]
        val profileImage = (me \ ProfileImage).asOpt[String]
        user.copy(id = UserId(id.toString, providerId), fullName = name, avatarUrl = profileImage)
      }
      case Failure(t) => {
        Logger.error("timed out waiting for Twitter")
        throw new AuthenticationException()
      }
    }

    Await.result(p.future, 10 seconds)
  }
}

object TwitterProvider {
  val VerifyCredentials = "https://api.twitter.com/1.1/account/verify_credentials.json"
  val Twitter = "twitter"
  val Id = "id"
  val Name = "name"
  val ProfileImage = "profile_image_url_https"
}

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
 * A LinkedIn Provider
 */
class LinkedInProvider(application: Application) extends OAuth1Provider(application) {


  override def providerId = LinkedInProvider.LinkedIn

  override def fillProfile(user: SocialUser): SocialUser = {
    val oauthInfo = user.oAuth1Info.get
    WS.url(LinkedInProvider.Api).sign(OAuthCalculator(oauthInfo.serviceInfo.key,
      RequestToken(oauthInfo.token, oauthInfo.secret))).get().await(10000).fold(
      onError => {
        Logger.error("timed out waiting for LinkedIn")
        throw new AuthenticationException()
      },
      response =>
      {
        val me = response.json
        (me \ "errorCode").asOpt[Int] match {
          case Some(error) => {
            val message = (me \ "message").asOpt[String]
            val requestId = (me \ "requestId").asOpt[String]
            val timestamp = (me \ "timestamp").asOpt[String]
            Logger.error(
              "Error retrieving information from LinkedIn. Error code: %s, requestId: %s, message: %s, timestamp: %s"
              format(error, message, requestId, timestamp)
            )
            throw new AuthenticationException()
          }
          case _ => {
            val id = (me \ "id").as[String]
            val first = (me \ "firstName").asOpt[String]
            val last = (me \ "lastName").asOpt[String]
            val fullName = "%s %s".format(first.getOrElse(""), last.getOrElse(""))
            val avatarUrl = (me \ "pictureUrl").asOpt[String]
            user.copy(id = UserId(id, providerId), displayName = fullName, avatarUrl = avatarUrl)
          }
        }
      }
    )
  }
}

object LinkedInProvider {
  val Api = "https://api.linkedin.com/v1/people/~:(id,first-name,last-name,picture-url)?format=json"
  val LinkedIn = "linkedin"
}

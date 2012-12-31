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

import play.api.libs.ws.WS
import play.api.{Application, Logger}
import play.api.libs.json.JsObject
import securesocial.core._


/**
 * A Google OAuth2 Provider
 */
class GoogleProvider(application: Application) extends OAuth2Provider(application) {
  val UserInfoApi = "https://www.googleapis.com/oauth2/v1/userinfo?access_token="
  val Error = "error"
  val Message = "message"
  val Type = "type"
  val Id = "id"
  val Name = "name"
  val GivenName = "given_name"
  val FamilyName = "family_name"
  val Picture = "picture"
  val Email = "email"


  override def id = GoogleProvider.Google

  def fillProfile(user: SocialUser): SocialUser = {
    val accessToken = user.oAuth2Info.get.accessToken
    val promise = WS.url(UserInfoApi + accessToken).get()

    promise.await(10000).fold( error => {
      Logger.error( "[securesocial] error retrieving profile information", error)
      throw new AuthenticationException()
    }, response => {
      val me = response.json
      (me \ Error).asOpt[JsObject] match {
        case Some(error) =>
          val message = (error \ Message).as[String]
          val errorType = ( error \ Type).as[String]
          Logger.error("[securesocial] error retrieving profile information from Google. Error type = %s, message = %s"
            .format(errorType,message))
          throw new AuthenticationException()
        case _ =>
          val userId = (me \ Id).as[String]
          val firstName = (me \ GivenName).as[String]
          val lastName = (me \ FamilyName).as[String]
          val fullName = (me \ Name).as[String]
          val avatarUrl = ( me \ Picture).asOpt[String]
          val email = ( me \ Email).as[String]
          user.copy(
            id = UserId(userId, id),
            firstName = firstName,
            lastName = lastName,
            fullName = fullName,
            avatarUrl = avatarUrl,
            email = Some(email)
          )
      }
    })
  }
}

object GoogleProvider {
  val Google = "google"
}
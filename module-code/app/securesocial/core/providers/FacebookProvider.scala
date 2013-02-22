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

import play.api.{Application, Logger}
import play.api.libs.json.JsObject
import securesocial.core._
import play.api.libs.ws.{ Response, WS }

/**
 * A Facebook Provider
 */
class FacebookProvider(application: Application) extends OAuth2Provider(application) {
  val MeApi = "https://graph.facebook.com/me?fields=name,first_name,last_name,picture,email&return_ssl_resources=1&access_token="
  val Error = "error"
  val Message = "message"
  val Type = "type"
  val Id = "id"
  val FirstName = "first_name"
  val LastName = "last_name"
  val Name = "name"
  val Picture = "picture"
  val Email = "email"
  val AccessToken = "access_token"
  val Expires = "expires"
  val Data = "data"
  val Url = "url"

  override def id = FacebookProvider.Facebook

  // facebook does not follow the OAuth2 spec :-\
  override protected def buildInfo(response: Response): OAuth2Info = {
    response.body.split("&|=") match {
        case Array(AccessToken, token, Expires, expiresIn) => OAuth2Info(token, None, Some(expiresIn.toInt))
        case _ =>
          Logger.error("[securesocial] invalid response format for accessToken")
          throw new AuthenticationException()
    }
  }

  def fillProfile(user: SocialUser) = {
    val accessToken = user.oAuth2Info.get.accessToken
    val call = WS.url(MeApi + accessToken).get()

    try {
      val response = awaitResult(call)
      val me = response.json
      (me \ Error).asOpt[JsObject] match {
        case Some(error) =>
          val message = (error \ Message).as[String]
          val errorType = ( error \ Type).as[String]
          Logger.error(
            "[securesocial] error retrieving profile information from Facebook. Error type: %s, message: %s".
              format(errorType, message)
          )
          throw new AuthenticationException()
        case _ =>
          val userId = ( me \ Id).as[String]
          val name = ( me \ Name).as[String]
          val firstName = ( me \ FirstName).as[String]
          val lastName = ( me \ LastName).as[String]
          val picture = (me \ Picture)
          val avatarUrl = (picture \ Data \ Url).asOpt[String]
          val email = ( me \ Email).as[String]

          user.copy(
            id = UserId(userId, id),
            firstName = firstName,
            lastName = lastName,
            fullName = name,
            avatarUrl = avatarUrl,
            email = Some(email)
          )
      }
    } catch {
      case e: Exception => {
          Logger.error("[securesocial] error retrieving profile information from Facebook",  e)
          throw new AuthenticationException()
      }
    }
  }
}

object FacebookProvider {
  val Facebook = "facebook"
}
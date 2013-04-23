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
import securesocial.core.UserId
import securesocial.core.SocialUser
import play.api.libs.ws.WS
import securesocial.core.AuthenticationException
import scala.Some

/**
 * A Foursquare provider
 *
 */
class FoursquareProvider(application: Application) extends OAuth2Provider(application) {

  val GetAuthenticatedUser = "https://api.foursquare.com/v2/users/self?oauth_token=%s"
  val AccessToken = "access_token"
  val TokenType = "token_type"
  val Message = "message"
  val Id = "id"
  val Response = "response"
  val User = "user"
  val Contact = "contact"
  val LastName = "lastName"
  val FirstName = "firstName"
  val AvatarUrl = "photo"
  val Email = "email"
  val Prefix = "prefix"
  val Suffix = "suffix"

  override def id = FoursquareProvider.Foursquare

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
          Logger.error("[securesocial] error retrieving profile information from Foursquare. Message = %s".format(msg))
          throw new AuthenticationException()
        }
        case _ => {
          val userId = ( me \ Response \ User \ Id).asOpt[String]
          val lastName = (me \ Response \ User \ LastName).asOpt[String].getOrElse("")
          val firstName = (me \ Response \ User \ FirstName).asOpt[String].getOrElse("")
          val avatarUrlPart1  = (me \ Response \ User \ AvatarUrl \ Prefix).asOpt[String]
          val avatarUrlPart2 = (me \ Response \ User \ AvatarUrl \ Suffix).asOpt[String]
          val email = (me \ Response \ User \ Contact \ Email).asOpt[String].filter( !_.isEmpty )

          user.copy(
            id = UserId(userId.get , id),
            lastName = lastName,
            firstName = firstName,
            fullName = firstName + " " + lastName,
            avatarUrl = for (prefix <- avatarUrlPart1; postfix <- avatarUrlPart2) yield prefix + "100x100" + postfix,
            email = email
          )
        }
      }
    } catch {
      case e: Exception => {
        Logger.error( "[securesocial] error retrieving profile information from foursquare", e)
        throw new AuthenticationException()
      }
    }
  }
}

object FoursquareProvider {
  val Foursquare = "foursquare"
}
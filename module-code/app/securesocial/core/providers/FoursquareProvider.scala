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
import securesocial.core.services.{CacheService, RoutesService}

import scala.concurrent.Future

/**
 * A Foursquare provider
 *
 */
class FoursquareProvider(routesService: RoutesService,
                         cacheService: CacheService,
                         client: OAuth2Client)
  extends OAuth2Provider(routesService, client, cacheService)
{
  val GetAuthenticatedUser = "https://api.foursquare.com/v2/users/self?v=20140404oauth_token=%s"
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

  override val id = FoursquareProvider.Foursquare

  def fillProfile(info: OAuth2Info): Future[BasicProfile] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    client.retrieveProfile(GetAuthenticatedUser.format(info.accessToken)).map { me =>
        (me \ "response" \ "user").asOpt[String] match {
          case Some(msg) =>
            logger.error("[securesocial] error retrieving profile information from Foursquare. Message = %s".format(msg))
            throw new AuthenticationException()
          case _ =>
            val userId = (me \ Response \ User \ Id).as[String]
            val lastName = (me \ Response \ User \ LastName).asOpt[String]
            val firstName = (me \ Response \ User \ FirstName).asOpt[String]
            val avatarUrlPart1 = (me \ Response \ User \ AvatarUrl \ Prefix).asOpt[String]
            val avatarUrlPart2 = (me \ Response \ User \ AvatarUrl \ Suffix).asOpt[String]
            val avatarUrl = for (prefix <- avatarUrlPart1; postfix <- avatarUrlPart2) yield prefix + "100x100" + postfix
            val email = (me \ Response \ User \ Contact \ Email).asOpt[String].filter(!_.isEmpty)
            BasicProfile(id, userId, firstName, lastName, None, email, avatarUrl, authMethod, oAuth2Info = Some(info))
        }
    } recover {
      case e: AuthenticationException => throw e
      case e  =>
        logger.error( "[securesocial] error retrieving profile information from Foursquare", e)
        throw new AuthenticationException()
    }
  }
}

object FoursquareProvider {
  val Foursquare = "foursquare"
}

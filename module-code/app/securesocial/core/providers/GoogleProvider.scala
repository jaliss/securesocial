/**
 * Copyright 2012-2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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

import play.api.libs.json.JsObject
import securesocial.core._
import securesocial.core.services.{CacheService, RoutesService}

import scala.concurrent.Future


/**
 * A Google OAuth2 Provider
 */
class GoogleProvider(routesService: RoutesService,
                     cacheService: CacheService,
                     client: OAuth2Client)
  extends OAuth2Provider(routesService, client, cacheService)
{
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


  override val id = GoogleProvider.Google

  def fillProfile(info: OAuth2Info): Future[BasicProfile] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val accessToken = info.accessToken
      client.retrieveProfile(UserInfoApi + accessToken).map { me =>
        (me \ Error).asOpt[JsObject] match {
          case Some(error) =>
            val message = (error \ Message).as[String]
            val errorType = (error \ Type).as[String]
            logger.error("[securesocial] error retrieving profile information from Google. Error type = %s, message = %s"
              .format(errorType, message))
            throw new AuthenticationException()
          case _ =>
            val userId = (me \ Id).as[String]
            val firstName = (me \ GivenName).asOpt[String]
            val lastName = (me \ FamilyName).asOpt[String]
            val fullName = (me \ Name).asOpt[String]
            val avatarUrl = (me \ Picture).asOpt[String]
            val email = (me \ Email).asOpt[String]
            BasicProfile(id, userId, firstName, lastName, fullName, email, avatarUrl, authMethod, oAuth2Info = Some(info))
        }
    } recover {
      case e: AuthenticationException => throw e
      case e =>
        logger.error( "[securesocial] error retrieving profile information from Google", e)
        throw new AuthenticationException()
    }
  }
}

object GoogleProvider {
  val Google = "google"
}

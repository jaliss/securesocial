/**
 * Copyright 2013 Greg Methvin (greg at methvin dot net)
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
import securesocial.core.providers.LinkedInOAuth2Provider._
import securesocial.core.services.{CacheService, RoutesService}

import scala.concurrent.Future

/**
 * A LinkedIn Provider (OAuth2)
 */
class LinkedInOAuth2Provider(routesService: RoutesService,
                             cacheService: CacheService,
                             client: OAuth2Client)
  extends OAuth2Provider(routesService, client, cacheService)
{
  override val id = LinkedInOAuth2Provider.LinkedIn

  override def fillProfile(info: OAuth2Info): Future[BasicProfile] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val accessToken = info.accessToken
    client.retrieveProfile(LinkedInOAuth2Provider.Api + accessToken).map { me =>
        (me \ ErrorCode).asOpt[Int] match {
          case Some(error) => {
            val message = (me \ Message).asOpt[String]
            val requestId = (me \ RequestId).asOpt[String]
            val timestamp = (me \ Timestamp).asOpt[String]
            logger.error(
              s"Error retrieving information from LinkedIn. Error code: $error, requestId: $requestId, message: $message, timestamp: $timestamp"
            )
            throw new AuthenticationException()
          }
          case _ => {
            val userId = (me \ Id).as[String]
            val firstName = (me \ FirstName).asOpt[String]
            val lastName = (me \ LastName).asOpt[String]
            val fullName = (me \ FormattedName).asOpt[String]
            val avatarUrl = (me \ PictureUrl).asOpt[String]
            val emailAddress = (me \ EmailAddress).asOpt[String]
            BasicProfile(id, userId, firstName, lastName, fullName, emailAddress, avatarUrl, authMethod, oAuth2Info = Some(info))
          }
        }
    } recover {
      case e: AuthenticationException => throw e
      case e  =>
        logger.error("[securesocial] error retrieving profile information from LinkedIn", e)
        throw new AuthenticationException()
    }
  }
}

object LinkedInOAuth2Provider {
  val Api = "https://api.linkedin.com/v1/people/~:(id,first-name,last-name,formatted-name,picture-url,email-address)?format=json&oauth2_access_token="
  val LinkedIn = "linkedin"
  val ErrorCode = "errorCode"
  val Message = "message"
  val RequestId = "requestId"
  val Timestamp = "timestamp"
  val Id = "id"
  val FirstName = "firstName"
  val LastName = "lastName"
  val FormattedName = "formattedName"
  val PictureUrl = "pictureUrl"
  val EmailAddress = "emailAddress"
}

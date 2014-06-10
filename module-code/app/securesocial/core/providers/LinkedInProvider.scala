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

import securesocial.core._
import play.api.libs.oauth.{RequestToken, OAuthCalculator}
import play.api.Logger
import LinkedInProvider._
import scala.concurrent.{ExecutionContext, Future}
import securesocial.core.services.{RoutesService, CacheService, HttpService}


/**
 * A LinkedIn Provider
 */
class LinkedInProvider(
        routesService: RoutesService,
        cacheService: CacheService,
        client: OAuth1Client //= new OAuth1Client.Default(ServiceInfoHelper.forProvider(LinkedInProvider.LinkedIn), httpService)
      ) extends OAuth1Provider(
        routesService,
        cacheService,
        client
      )
{
  override val id = LinkedInProvider.LinkedIn

  override  def fillProfile(info: OAuth1Info): Future[BasicProfile] = {
    import ExecutionContext.Implicits.global
      client.retrieveProfile(LinkedInProvider.Api,info).map { me =>
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
          case _ =>
            val userId = (me \ Id).as[String]
            val firstName = (me \ FirstName).asOpt[String]
            val lastName = (me \ LastName).asOpt[String]
            val fullName = (me \ FormattedName).asOpt[String]
            val avatarUrl = (me \ PictureUrl).asOpt[String]
            val emailAddress = (me \ EmailAddress).asOpt[String]
            BasicProfile(id, userId, firstName, lastName, fullName, emailAddress, avatarUrl, authMethod, Some(info))
        }
    } recover {
      case e: AuthenticationException => throw e
      case e =>
        logger.error("[securesocial] error retrieving profile information from LinkedIn", e)
        throw new AuthenticationException()
    }
  }
}

object LinkedInProvider {
  val Api = "https://api.linkedin.com/v1/people/~:(id,first-name,last-name,formatted-name,picture-url,email-address)?format=json"
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

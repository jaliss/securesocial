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

import play.api.libs.json.JsObject
import securesocial.core._
import securesocial.core.providers.XingProvider._
import securesocial.core.services.{CacheService, RoutesService}

import scala.concurrent.Future

/**
 * A Xing Provider
 */
class XingProvider(
        routesService: RoutesService,
        cacheService: CacheService,
        client: OAuth1Client
      ) extends OAuth1Provider(
        routesService,
        cacheService,
        client
      )
{
  override val id = XingProvider.Xing

  override  def fillProfile(info: OAuth1Info): Future[BasicProfile] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    client.retrieveProfile(XingProvider.VerifyCredentials,info).map { json=>
      val me = (json \ Users).as[Seq[JsObject]].head
      val userId = (me \ Id).as[String]
      val displayName = (me \ Name).asOpt[String]
      val lastName = (me \ LastName).asOpt[String]
      val firstName = (me \ FirstName).asOpt[String]
      val profileImage = (me \ ProfileImage \ Large).asOpt[String]
      val email = (me  \ ActiveEmail).asOpt[String]
      BasicProfile(id, userId, displayName, firstName, lastName, email, profileImage, authMethod, Some(info))
    } recover {
      case e =>
        logger.error("[securesocial] error retrieving profile information from Xing", e)
        throw new AuthenticationException()
    }
  }
}

object XingProvider {
  val VerifyCredentials = "https://api.xing.com/v1/users/me"
  val Xing = "xing"
  val Id = "id"
  val Name = "display_name"
  val FirstName = "first_name"
  val LastName = "last_name"
  val Users = "users"
  val ProfileImage = "photo_urls"
  val Large = "large"
  val ActiveEmail = "active_email"
}

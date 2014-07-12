/**
 * Copyright 2014 Deter de Wet
 * 
 * Based on the code for LinkedInOAuth2Provider by Greg Methvin
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
import securesocial.core._
import securesocial.core.providers.WordPressProvider._
import securesocial.core.services.{CacheService, RoutesService}

import scala.concurrent.Future
/**
 * A WordPress Provider (OAuth2)
 */
class WordPressProvider(routesService: RoutesService,
                      cacheService: CacheService,
                      client: OAuth2Client)
  extends OAuth2Provider(routesService, client, cacheService)
{
  private val Logger=play.api.Logger("securesocial.core.providers.WordPressProvider")
  override val id = WordPressProvider.WordPress

  override def fillProfile(info: OAuth2Info): Future[BasicProfile] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val accessToken = info.accessToken
    WS.url(WordPressProvider.Api).withHeaders("Authorization"->s"Bearer $accessToken").get().map { response =>
        response.status match {
          case 200 => 
            val data = response.json
            val userId = (data \ Id).as[Int].toString
			val avatar = (data \ Avatar).asOpt[String]
			val email = (data \ Email).asOpt[String]
            val fullName = (data \ FormattedName).asOpt[String]
            BasicProfile(id, userId, None, None, fullName, email, avatar, authMethod, None, Some(info))
          case _ =>
            Logger.error("[securesocial] WordPress account info request returned error: "+response.body)
            throw new AuthenticationException()
        }
     } recover {
       case e  =>
         Logger.error("[securesocial] error retrieving profile information from WordPress", e)
         throw new AuthenticationException()
     }
  }
}

object WordPressProvider {
  val Api = "https://public-api.wordpress.com/rest/v1/me/"
  val WordPress = "wordpress"
  val Id = "ID"
  val FormattedName = "display_name"
  val Email = "email"
  val Avatar = "avatar_URL"  
  //$d->name, $d->email, $d->user_id
}

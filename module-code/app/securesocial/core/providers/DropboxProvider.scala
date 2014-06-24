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
import securesocial.core.providers.DropboxProvider._
import securesocial.core.services.{CacheService, RoutesService}

import scala.concurrent.Future

/**
 * A Dropbox Provider (OAuth2)
 */
class DropboxProvider(routesService: RoutesService,
                      cacheService: CacheService,
                      client: OAuth2Client)
  extends OAuth2Provider(routesService, client, cacheService)
{
  private val Logger=play.api.Logger("securesocial.core.providers.DropboxProvider")
  override val id = DropboxProvider.Dropbox

  override def fillProfile(info: OAuth2Info): Future[BasicProfile] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    import play.api.Play.current

    val accessToken = info.accessToken
    WS.url(DropboxProvider.Api).withHeaders("Authorization"->s"Bearer $accessToken").get().map { response =>
        response.status match {
          case 200 => 
            val data = response.json
            val userId = (data \ Id).as[Int].toString
            val fullName = (data \ FormattedName).asOpt[String]
            BasicProfile(id, userId, None, None, fullName, None, None, authMethod, None, Some(info))
          case _ =>
            Logger.error("[securesocial] Dropbox account info request returned error: "+response.body)
            throw new AuthenticationException()
        }
     } recover {
       case e  =>
         Logger.error("[securesocial] error retrieving profile information from Dropbox", e)
         throw new AuthenticationException()
     }
  }
}

object DropboxProvider {
  val Api = "https://api.dropbox.com/1/account/info"
  val Dropbox = "dropbox"
  val Id = "uid"
  val FormattedName = "display_name"
}

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

import com.evernote.edam.userstore.UserStore
import com.evernote.thrift.protocol.TBinaryProtocol
import com.evernote.thrift.transport.THttpClient
import securesocial.core._
import securesocial.core.services.{ CacheService, RoutesService }

import scala.concurrent.Future

/**
 * An Evernote Provider
 */
class EvernoteProvider(
  routesService: RoutesService,
  cacheService: CacheService,
  client: OAuth1Client) extends OAuth1Provider(
  routesService,
  cacheService,
  client
) {
  override val id = EvernoteProvider.Evernote

  override def fillProfile(info: OAuth1Info): Future[BasicProfile] = Future.successful {
    val userStoreTrans: THttpClient = new THttpClient(EvernoteProvider.UserInfo)
    val userStoreProt: TBinaryProtocol = new TBinaryProtocol(userStoreTrans)
    val userStore: UserStore.Client = new UserStore.Client(userStoreProt, userStoreProt)
    val user = userStore.getUser(info.token)

    BasicProfile(id, user.getId.toString, None, None, Option(user.getName), None, None, authMethod, Some(info))
  }
}

object EvernoteProvider {
  val Evernote = "evernote"
  val UserInfo = "https://sandbox.evernote.com/edam/user"

}


package service

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

import javax.inject.{ Inject, Singleton }

import akka.actor.ActorSystem
import controllers.CustomRoutesService
import play.api.cache.AsyncCacheApi
import play.api.{ Configuration, Environment }
import play.api.i18n.MessagesApi
import play.api.libs.mailer.MailerClient
import play.api.libs.ws.WSClient
import play.api.mvc.PlayBodyParsers
import securesocial.core.{ IdentityProvider, RuntimeEnvironment }

import scala.collection.immutable.ListMap
import scala.concurrent.ExecutionContext

@Singleton
class MyEnvironment @Inject() (
  override val configuration: Configuration,
  override implicit val messagesApi: MessagesApi,
  override val environment: Environment,
  override val wsClient: WSClient,
  override val cacheApi: AsyncCacheApi,
  override val mailerClient: MailerClient,
  override val executionContext: ExecutionContext,
  override val parsers: PlayBodyParsers,
  override val actorSystem: ActorSystem,
  customProviders: CustomProviders) extends RuntimeEnvironment.Default {
  override type U = DemoUser
  override lazy val routes = new CustomRoutesService(environment, configuration)
  override lazy val userService: InMemoryUserService = new InMemoryUserService()
  override lazy val eventListeners = List(new MyEventListener)
  override lazy val providers: ListMap[String, IdentityProvider] =
    ListMap(customProviders.list.map(include): _*) ++ builtInProviders
}

case class CustomProviders(list: Seq[IdentityProvider]) {
  @Inject def this() = this(Seq.empty)
}

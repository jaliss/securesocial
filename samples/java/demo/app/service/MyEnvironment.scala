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
package service

import javax.inject.{ Inject, Singleton }

import akka.actor.ActorSystem
import play.api.{ Configuration, Environment }
import play.api.cache.AsyncCacheApi
import play.api.i18n.MessagesApi
import play.api.libs.mailer.MailerClient
import play.api.libs.ws.WSClient
import play.api.mvc.PlayBodyParsers
import securesocial.core.RuntimeEnvironment
import securesocial.core.services.UserService

import scala.concurrent.ExecutionContext

@Singleton
class MyEnvironment @Inject() (
  override val configuration: Configuration,
  override val messagesApi: MessagesApi,
  override val environment: Environment,
  override val wsClient: WSClient,
  override val cacheApi: AsyncCacheApi,
  override val mailerClient: MailerClient,
  override val executionContext: ExecutionContext,
  override val parsers: PlayBodyParsers,
  override val actorSystem: ActorSystem) extends RuntimeEnvironment.Default {
  type U = DemoUser
  override val userService: UserService[U] = new InMemoryUserService()
}
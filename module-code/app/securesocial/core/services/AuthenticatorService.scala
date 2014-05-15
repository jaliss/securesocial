/**
 * Copyright 2013-2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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
package securesocial.core.services

import play.api.mvc.RequestHeader
import scala.concurrent.{ExecutionContext, Future}
import securesocial.core.authenticator.{Authenticator, AuthenticatorBuilder}

class AuthenticatorService[U](builders: AuthenticatorBuilder[U]*) {
  val asMap = builders.map { builder => builder.id -> builder }.toMap

  def find(id: String): Option[AuthenticatorBuilder[U]] = {
    asMap.get(id)
  }

  def fromRequest(implicit request: RequestHeader): Future[Option[Authenticator[U]]] = {
    import ExecutionContext.Implicits.global

    def iterateIt(list: List[AuthenticatorBuilder[U]]): Future[Option[Authenticator[U]]] = {
      if ( list.isEmpty )
        Future.successful(None)
      else {
        list.head.fromRequest(request).flatMap {
          case Some(authenticator) => Future.successful(Some(authenticator))
          case None => iterateIt(list.tail)
        }
      }
    }
    iterateIt(builders.toList)
  }
}

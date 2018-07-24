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
import scala.concurrent.{ ExecutionContext, Future }
import securesocial.core.authenticator.{ Authenticator, AuthenticatorBuilder }
import scala.reflect.ClassTag

class AuthenticatorService[U](builders: AuthenticatorBuilder[U]*)(implicit val executionContext: ExecutionContext) {
  private val logger = play.api.Logger(getClass.getName)
  private val asMap = builders.map { builder => builder.id -> builder }.toMap

  def find(id: String): Option[AuthenticatorBuilder[U]] = {
    asMap.get(id)
  }

  def findAs[T <: AuthenticatorBuilder[U]](id: String)(implicit ct: ClassTag[T]): Option[T] = {
    find(id) map {
      case builder if builder.isInstanceOf[T] => builder.asInstanceOf[T]
    }
  }

  def fromRequest(implicit request: RequestHeader): Future[Option[Authenticator[U]]] = {
    def iterateIt(seq: Seq[AuthenticatorBuilder[U]]): Future[Option[Authenticator[U]]] = {
      if (seq.isEmpty)
        Future.successful(None)
      else {
        seq.head.fromRequest(request).flatMap {
          case Some(authenticator) => Future.successful(Some(authenticator))
          case None => iterateIt(seq.tail)
        }
      }
    }
    iterateIt(builders) recover {
      case t: Throwable =>
        logger.error(s"An error occurred while trying to build an authenticator from a request", t)
        None
    }
  }
}

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
package securesocial.core.authenticator

import scala.concurrent.{ExecutionContext, Future}
import securesocial.core.services.CacheService
import scala.reflect.ClassTag

/**
 * Defines a backing store for Authenticator instances
 *
 * @tparam A the Authenticator type the store manages
 */
trait AuthenticatorStore[A <: Authenticator[_]] {
  /**
   * Retrieves an Authenticator from the backing store
   *
   * @param id the authenticator id
   * @param ct the class tag for the Authenticator type
   * @return an optional future Authenticator
   */
  def find(id: String)(implicit ct: ClassTag[A]): Future[Option[A]]

  /**
   * Saves/updates an authenticator in the backing store
   *
   * @param authenticator the istance to save
   * @param timeoutInSeconds the timeout. after this time has passed the backing store needs to remove the entry.
   * @return the saved authenticator
   */
  def save(authenticator: A, timeoutInSeconds: Int): Future[A]

  /**
   * Deletes an Authenticator from the backing store
   *
   * @param id the authenticator id
   * @return a future of Unit
   */
  def delete(id: String): Future[Unit]
}

object AuthenticatorStore {
  /**
   * The default AuthenticatorStore based on a cache service
   *
   * @param cacheService the cache service to use
   * @tparam A the Authenticator type
   */
  class Default[A <: Authenticator[_]](cacheService: CacheService) extends AuthenticatorStore[A] {
    /**
     * Retrieves an Authenticator from the cache
     *
     * @param id the authenticator id
     * @param ct the class tag for the Authenticator type
     * @return an optional future Authenticator
     */
    override def find(id: String)(implicit ct: ClassTag[A]): Future[Option[A]] = {
      cacheService.getAs[A](id)(ct)
    }

    /**
     * Saves/updates an authenticator into the cache
     *
     * @param authenticator the istance to save
     * @param timeoutInSeconds the timeout.
     * @return the saved authenticator
     */
    override def save(authenticator: A, timeoutInSeconds: Int): Future[A] = {
      import ExecutionContext.Implicits.global
      cacheService.set(authenticator.id, authenticator, timeoutInSeconds).map { _ => authenticator }
    }

    /**
     * Deletes an Authenticator from the cache
     *
     * @param id the authenticator id
     * @return a future of Unit
     */
    override def delete(id: String): Future[Unit] ={
      cacheService.remove(id)
    }
  }
}